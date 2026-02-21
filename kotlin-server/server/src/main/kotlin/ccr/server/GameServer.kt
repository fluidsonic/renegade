package ccr.server

import ccr.net.bitstream.*
import ccr.net.connection.ConnectionManager
import ccr.net.connection.RemoteHost
import ccr.net.protocol.Packet
import ccr.net.protocol.PacketCombiner
import ccr.net.protocol.PacketType
import ccr.net.protocol.WrapperCrc
import ccr.net.rcon.RconServer
import ccr.net.transport.UdpTransport
import ccr.server.defs.readDefinitions
import ccr.server.mix.MixReader
import ccr.server.mix.WorldExtents
import ccr.server.mix.extractLevelExtents
import ccr.math.Vector3
import ccr.server.net.GameData
import ccr.server.net.GameDataUpdateEvent
import ccr.server.net.GameOptionsEvent
import ccr.server.net.NetworkObjectPacketWriter
import ccr.server.net.Player
import ccr.server.net.ScPingResponseEvent
import ccr.server.net.SoldierGameObj
import ccr.server.net.Team
import ccr.server.net.WeaponEntry
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.File

/**
 * Main server orchestrator. Wires together UdpTransport, ConnectionManager,
 * RconServer, and LanBroadcastResponder into a single coroutine scope.
 *
 * Thread safety: All ConnectionManager / RemoteHost state is confined to
 * [gameThread] (single-thread dispatcher). UdpTransport communicates via
 * its own channels. RconServer and LanBroadcastResponder own their sockets.
 */
class GameServer(private val config: ServerConfig) {

    private val gameThread = newSingleThreadContext("game-thread")
    private val transport = UdpTransport(config.gamePort)
    private val connectionManager = ConnectionManager(config.maxPlayers)
    private val localIp = detectLocalIp()
    private val gameData = GameData(config, localIp)

    private val rconServer = RconServer(
        port = config.rconPort,
        password = config.rconPassword,
        remoteAdminAllowed = config.remoteAdminAllowed,
        welcomeMessage = buildWelcomeMessage(),
        commandHandler = ::handleRconCommand,
    )

    private val lanResponder = LanBroadcastResponder(config, connectionManager)

    // Milliseconds per network tick
    private val tickIntervalMs: Long = 1000L / config.netUpdateRate.coerceAtLeast(1)

    // Nicknames for players currently in acceptance: populated in checkApplication,
    // consumed in connHandler so that we can include the name in the Player network object.
    private val playerNicknames = mutableMapOf<java.net.InetSocketAddress, String>()

    // Tracks the dynamic network ID allocated for each player's cPlayer network object.
    // Needed so that BIT_RARE updates reference the same networkId as the original creation.
    private val playerNetIds = mutableMapOf<Int, Int>()  // rhostId -> networkId

    // C++: cGameData::HostedGameNumber — increments each time a new game starts on this server.
    private var hostedGameNumber = 1

    // Per-player state tracked by the server.
    // C++: cPlayer::PlayerType (team 0=NOD, 1=GDI, -1=unassigned) and IsInGame.
    private val playerTeams = mutableMapOf<Int, Int>()   // rhostId → team (0/1)
    private val playerInGame = mutableSetOf<Int>()       // rhostIds where IsInGame=true

    // Registered game objects: players and soldiers, keyed by rhostId for lifecycle management.
    private val playersByHost = mutableMapOf<Int, Player>()
    private val soldiersByHost = mutableMapOf<Int, SoldierGameObj>()

    // Singleton team objects registered at startup (stable static IDs).
    private val teamNod = Team(teamNumber = 0)
    private val teamGdi = Team(teamNumber = 1)

    // World extents loaded from the map's .lsd file at startup (null if not available).
    private var worldExtents: WorldExtents? = null

    // Soldier definition IDs loaded from always.dat at startup; fall back to config values.
    private var nodSoldierDefId: Int = config.nodSoldierDefId
    private var gdiSoldierDefId: Int = config.gdiSoldierDefId
    private var pistolWeaponDefId: Int = 0

    suspend fun run() = coroutineScope {
        loadDefinitions()  // Load definition IDs from always.dat before accepting clients
        initEncoders()     // Load world extents and configure all BITPACK_* encoders before accepting clients

        // Register team singletons with static IDs. These persist for the lifetime of the server.
        NetworkObjectManager.registerObject(teamNod, NET_ID_NOD_TEAM)
        NetworkObjectManager.registerObject(teamGdi, NET_ID_GDI_TEAM)

        connectionManager.applicationAcceptanceHandler = ::checkApplication
        connectionManager.connHandler = { id, host ->
            println("[CONNECT] client $id connected from ${host.address} bps=${host.maximumBps}")
            sendConnectionObjects(id, host)
        }
        connectionManager.disconnectHandler = { id ->
            println("[CONNECT] client $id disconnected")
            // Send deletion packets to all other in-game hosts before unregistering
            val disconnectedPlayer = playersByHost[id]
            val disconnectedSoldier = soldiersByHost[id]
            for (otherId in playerInGame) {
                if (otherId == id) continue
                val otherHost = connectionManager.getHost(otherId) ?: continue
                disconnectedSoldier?.also { s ->
                    sendGameNetObj(otherHost) { bs -> NetworkObjectPacketWriter.writeDeletion(bs, s.networkId) }
                }
                disconnectedPlayer?.also { p ->
                    val netId = playerNetIds[id] ?: return@also
                    sendGameNetObj(otherHost) { bs -> NetworkObjectPacketWriter.writeDeletion(bs, netId) }
                }
            }
            playersByHost.remove(id)?.also { NetworkObjectManager.unregisterObject(it) }
            soldiersByHost.remove(id)?.also { NetworkObjectManager.unregisterObject(it) }
            playerTeams.remove(id)
            playerInGame.remove(id)
            playerNetIds.remove(id)
        }
        connectionManager.serverPacketHandler = ::handleGamePacket
        println("[SERVER] listening on UDP port ${config.gamePort} (RCON: ${config.rconPort})")

        launch(Dispatchers.IO)   { transport.ioLoop() }
        launch(gameThread)       { processInbound() }
        launch(gameThread)       { networkTickLoop() }
        launch(gameThread)       { physicsTickLoop() }
        launch(Dispatchers.IO)   { rconServer.run() }
        launch(Dispatchers.IO)   { lanResponder.broadcastLoop() }
    }

    // ---- Packet dispatch ----

    /** Enqueues a datagram after prepending the WRAPPER_CRC (packetmgr.cpp:894-914). */
    private fun enqueueWithCrc(datagrams: List<ccr.net.protocol.OutgoingDatagram>) {
        for (dg in datagrams) {
            transport.enqueue(dg.copy(data = WrapperCrc.prepend(dg.data)))
        }
    }

    private suspend fun processInbound() {
        for (datagram in transport.receiveChannel) {
            // Every datagram has a 4-byte WRAPPER_CRC prefix (packetmgr.cpp:894-914).
            if (!WrapperCrc.verify(datagram.data, datagram.length)) {
                println("[NET] datagram from ${datagram.source} (${datagram.length}b): CRC mismatch, discarding")
                continue
            }
            val packets = PacketCombiner.split(datagram.data, datagram.length, offset = 4, deltaFormat = true)
            if (packets.isEmpty()) {
                println("[NET] datagram from ${datagram.source} (${datagram.length}b): split → 0 packets (bad combiner header?)")
            }
            for (incoming in packets) {
                val rawHex = incoming.data.copyOf(incoming.length).joinToString(" ") { "%02x".format(it) }
                println("[NET] ← (${incoming.length}b) $rawHex")
                val packet = try {
                    Packet.parseWirePacket(incoming.data, incoming.length)
                } catch (e: Exception) {
                    println("[NET] datagram from ${datagram.source}: parse failed: $e (raw: ${incoming.data.take(16).joinToString(" ") { "%02x".format(it) }})")
                    continue
                }
                packet.sourceAddress = datagram.source
                dispatchPacket(packet)
            }
        }
    }

    private fun dispatchPacket(packet: Packet) {
        val source = packet.sourceAddress ?: return
        when (packet.type) {
            PacketType.CONNECT_CS -> {
                println("[CONNECT] CONNECT_CS from $source id=${packet.id} senderId=${packet.senderId} payloadBits=${packet.bitLength}")

                // ACK the reliable CONNECT_CS first (C++: Send_Ack before Process_Connection_Request)
                val ack = connectionManager.buildAck(packet.id)
                enqueueWithCrc(PacketCombiner.combine(listOf(source to Packet.buildWirePacket(ack))))
                println("[CONNECT] → sent ACK id=${packet.id} to $source")

                val result = connectionManager.processConnectionRequest(packet, source)
                if (result == null) {
                    println("[CONNECT] → duplicate CONNECT_CS from $source, ignored")
                    return
                }

                // Send ACCEPT_SC (or REFUSAL_SC) first — client needs its rhostId before
                // it can process any game objects. C++: Send_Accept_Sc before Connection_Handler.
                val acceptWire = Packet.buildWirePacket(result.packet)
                val acceptHex = acceptWire.joinToString(" ") { "%02x".format(it) }
                println("[CONNECT] → sending ${result.packet.type} id=${result.packet.id} (${acceptWire.size}B wire, ${result.packet.bitLength}b payload): $acceptHex")
                enqueueWithCrc(PacketCombiner.combine(listOf(source to acceptWire)))

                // If accepted, now send connection objects (Teams + GameOptionsEvent).
                val host = result.host
                if (host != null) {
                    connectionManager.connHandler(result.rhostId, host)
                }
            }
            PacketType.ACK -> {
                val host = connectionManager.processAck(packet)
                println("[NET] ACK id=${packet.id} from ${packet.senderId} (host=${host?.address})")
            }
            PacketType.KEEPALIVE -> {
                val host = connectionManager.processIncoming(packet) ?: run {
                    println("[NET] KEEPALIVE from unknown senderId=${packet.senderId} (source=$source)")
                    return
                }
                val ack = connectionManager.buildAck(packet.id)
                enqueueWithCrc(PacketCombiner.combine(listOf(host.address to Packet.buildWirePacket(ack))))
                println("[NET] KEEPALIVE from host ${host.id} (${host.address}) id=${packet.id} → ACKed")
            }
            PacketType.RELIABLE -> {
                val host = connectionManager.processIncoming(packet) ?: run {
                    println("[NET] RELIABLE from unknown senderId=${packet.senderId} (source=$source)")
                    return
                }
                val ack = connectionManager.buildAck(packet.id)
                enqueueWithCrc(PacketCombiner.combine(listOf(host.address to Packet.buildWirePacket(ack))))
                println("[NET] RELIABLE from host ${host.id} id=${packet.id} payloadBits=${packet.bitLength} ${peekGameEvent(packet)} → ACKed")
                connectionManager.serverPacketHandler(packet, host.id)
            }
            PacketType.UNRELIABLE -> {
                val host = connectionManager.processIncoming(packet) ?: run {
                    println("[NET] UNRELIABLE from unknown senderId=${packet.senderId} (source=$source)")
                    return
                }
                if (host.unreliable.receive(packet)) {
                    println("[NET] UNRELIABLE from host ${host.id} id=${packet.id} payloadBits=${packet.bitLength} ${peekGameEvent(packet)}")
                    connectionManager.serverPacketHandler(packet, host.id)
                }
            }
            else -> {
                println("[NET] unexpected packet type ${packet.type} from $source id=${packet.id}")
            }
        }
    }

    // ---- Network tick ----

    private suspend fun networkTickLoop() {
        while (true) {
            val nowMs = System.currentTimeMillis()

            // Send keepalives
            for ((host, kp) in connectionManager.getKeepalives(nowMs)) {
                val wire = Packet.buildWirePacket(kp)
                enqueueWithCrc(PacketCombiner.combine(listOf(host.address to wire)))
            }

            // Resend unacknowledged reliable packets
            for ((host, pending) in connectionManager.getResendPackets(nowMs)) {
                println("[NET] resending ${pending.packet.type} id=${pending.packet.id} to host ${host.id} (attempt ${pending.resendCount + 1})")
                enqueueWithCrc(PacketCombiner.combine(listOf(host.address to pending.rawData)))
            }

            // Check timeouts
            connectionManager.checkTimeouts(nowMs)

            // Push dirty object state to all in-game clients
            replicationTick()

            delay(tickIntervalMs)
        }
    }

    // ---- Replication tick ----

    // Scans all registered NetworkObjects and pushes dirty state to connected in-game clients.
    // C++: messages.cpp Send_Object_Update loop in cGod/cNetwork::Service.
    // Reliable for RARE/OCCASIONAL; unreliable for FREQUENT-only.
    // Never sends a soldier's FREQUENT update to its own controlling player (client is authoritative).
    private fun replicationTick() {
        val objects = NetworkObjectManager.getAllObjects()
        for (obj in objects) {
            for (clientId in playerInGame) {
                val bits = obj.getObjectDirtyBits(clientId).toInt() and 0xFF
                if (bits == 0) continue

                val host = connectionManager.getHost(clientId) ?: continue

                // Determine the soldier's controlling client to skip self-updates
                val isOwnSoldier = (obj is SoldierGameObj) && (obj.controlOwner == clientId)

                if ((bits and 0x08) != 0) {
                    // BIT_CREATION — send full creation reliably
                    sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, obj, obj.networkId) }
                    obj.setObjectDirtyBits(clientId, 0)
                } else if ((bits and 0x04) != 0) {
                    // BIT_RARE — send rare+occasional+frequent reliably
                    sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeRareUpdate(bs, obj, obj.networkId) }
                    obj.setObjectDirtyBits(clientId, 0)
                } else if ((bits and 0x02) != 0) {
                    // BIT_OCCASIONAL — send occasional+frequent reliably
                    sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeOccasionalUpdate(bs, obj, obj.networkId) }
                    obj.setObjectDirtyBits(clientId, 0)
                } else if ((bits and 0x01) != 0) {
                    // BIT_FREQUENT only — skip own soldier; send unreliably to others
                    if (!isOwnSoldier) {
                        sendUnreliable(host) { bs -> NetworkObjectPacketWriter.writeFrequentUpdate(bs, obj, obj.networkId) }
                    }
                    obj.setObjectDirtyBits(clientId, 0)
                }
            }
        }
    }

    // Sends an UNRELIABLE packet to the given host. No retransmission, no ACK.
    // C++: packetmgr.cpp Send_Object_Update for unreliable objects.
    private fun sendUnreliable(host: RemoteHost, writePayload: (BitStream) -> Unit) {
        val p = Packet()
        p.type = PacketType.UNRELIABLE
        p.id = host.unreliable.nextOutgoingId()
        p.senderId = 0
        writePayload(p.payload)
        val wireData = Packet.buildWirePacket(p)
        enqueueWithCrc(PacketCombiner.combine(listOf(host.address to wireData)))
    }

    // ---- Physics tick (stub) ----

    private suspend fun physicsTickLoop() {
        val intervalMs = 1000L / 120
        while (true) {
            delay(intervalMs)
        }
    }

    // ---- Game event peek (for logging) ----

    // Reads the network object header from a payload without consuming it.
    // Wire layout: [networkId:32][dirtyBits:8 (BYTE)][isDeletePending:1 (compressed bool)]
    //              [classId:32 if BIT_CREATION set]
    // C++: messages.cpp Send_Object_Update, pkthandlers.cpp BYTE dirty_bits = packet.Get(...)
    private fun peekGameEvent(packet: Packet): String {
        val bs = packet.payload
        if (bs.bitWritePosition < 41) return "(${bs.bitWritePosition}b)"  // 32+8+1 minimum
        return try {
            val snap = BitStream()
            val payloadBytes = (bs.bitWritePosition + 7) / 8
            System.arraycopy(bs.buffer, 0, snap.buffer, 0, payloadBytes)
            snap.setBitWritePosition(bs.bitWritePosition)

            val networkId = snap.getInt()
            val dirtyBits = snap.getByte().toInt() and 0xFF   // BYTE, not int
            val isDeletePending = snap.getBool()              // 1 bit (compression on)

            val sb = StringBuilder("netId=$networkId dirty=0x${dirtyBits.toString(16).uppercase()}")
            if (isDeletePending) sb.append(" DELETE")

            if ((dirtyBits and 0x08) != 0) {  // BIT_CREATION
                val classId = snap.getInt()
                sb.append(" class=$classId(${NetClassIds.name(classId)})")
            }
            sb.toString()
        } catch (e: Exception) {
            "(peek: $e)"
        }
    }

    // ---- Connection objects ----

    // C++: cNetwork::Connection_Handler — sends initial game state to a newly connected client.
    // Sends Teams and GameOptionsEvent. Player creation is deferred to BIOEVENT handler.
    private fun sendConnectionObjects(rhostId: Int, host: RemoteHost) {
        // Auto-assign team to balance NOD/GDI. CHANGETEAMEVENT toggles NOD↔GDI and requires
        // the player to already have a real team (asserts team==0||1 in C++).
        val nodCount = playerTeams.values.count { it == 0 }
        val gdiCount = playerTeams.values.count { it == 1 }
        val assignedTeam = if (nodCount <= gdiCount) 0 else 1  // 0=NOD, 1=GDI
        playerTeams[rhostId] = assignedTeam
        println("[CONNECT] sending connection objects to host $rhostId (team=${if (assignedTeam == 0) "NOD" else "GDI"})")

        // NOD team (teamNumber=0) and GDI team (teamNumber=1)
        // C++: cTeam dirty=BIT_CREATION(0x0F) — all 4 tiers sent on initial creation
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, teamNod, NET_ID_NOD_TEAM) }
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, teamGdi, NET_ID_GDI_TEAM) }

        // GameOptionsEvent — one-time creation event; tells the client what game/map is running.
        // C++: gameoptionsevent.cpp Export_Creation calls Export_Tier_1_Data + Export_Tier_2_Data.
        // cNetEvent subclass has no RARE/OCCASIONAL/FREQUENT state, so those tiers write nothing.
        gameData.currentPlayers = connectionManager.getConnectedCount()
        val gameOptionsEvent = GameOptionsEvent(gameData)
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, gameOptionsEvent, NetworkObjectManager.getNewDynamicId()) }

        // Player creation is NOT sent here. C++ sends it in cBioEvent::Act() after the client
        // finishes loading. See BIOEVENT handler (classId=1026) which sends Player + GameDataUpdateEvent.
    }

    // ---- Game event handlers ----

    // C++: pkthandlers.cpp / neteventhandlers.cpp — dispatches incoming game events by classId.
    private fun handleGamePacket(packet: Packet, rhostId: Int) {
        val bs = packet.payload
        if (bs.bitWritePosition < 41) {
            println("[GAME] handleGamePacket: rhostId=$rhostId too short (${bs.bitWritePosition} bits), skipping")
            return
        }

        val snap = BitStream()
        val payloadBytes = (bs.bitWritePosition + 7) / 8
        System.arraycopy(bs.buffer, 0, snap.buffer, 0, payloadBytes)
        snap.setBitWritePosition(bs.bitWritePosition)

        val networkId = snap.getInt()
        val dirtyBits = snap.getByte().toInt() and 0xFF
        val isDeletePending = snap.getBool()
        if ((dirtyBits and 0x08) == 0) {
            // Not a creation packet — handle as a frequent update (CClientControl position data)
            handleFrequentUpdate(snap, rhostId)
            return
        }

        val classId = snap.getInt()
        val host = connectionManager.getHost(rhostId) ?: return

        when (classId) {
            1018 -> {  // NETCLASSID_CLIENTCONTROL — sent BEFORE loading; do not spawn here
                // Import_Creation reads ClientId. Spawning here would send a game-object
                // packet to a client that hasn't loaded the world yet.  Wait for BioEvent.
                val clientId = snap.getInt()  // Import_Creation: ClientId
                println("[GAME] CLIENTCONTROL from rhostId=$rhostId clientId=$clientId")
            }
            1021 -> {  // NETCLASSID_CHANGETEAMEVENT (changeteamevent.cpp Import_Creation)
                // Client sends only SenderId. Act() toggles NOD(0)↔GDI(1).
                val senderId = snap.getInt()
                val currentTeam = playerTeams[rhostId] ?: 0
                val newTeam = if (currentTeam == 0) 1 else 0
                playerTeams[rhostId] = newTeam
                playersByHost[rhostId]?.team = newTeam  // sync stored player object
                println("[GAME] CHANGETEAMEVENT from rhostId=$rhostId senderId=$senderId: ${if (currentTeam == 0) "NOD" else "GDI"} → ${if (newTeam == 0) "NOD" else "GDI"}")
                sendPlayerRareUpdate(host, rhostId)
            }
            1026 -> {  // NETCLASSID_BIOEVENT — sent after Load_Level() in gameinitmgr.cpp
                // cBioEvent::Act() on the original server: creates cPlayer (Is_In_Game defaults true),
                // then sends cGameDataUpdateEvent.  cGod::Think() then spawns the soldier because
                // Is_Active && Is_In_Game are both true.  This is the correct post-load trigger.
                if (rhostId !in playerInGame) {
                    println("[GAME] BIOEVENT from rhostId=$rhostId → entering game (post-load)")
                    playerInGame.add(rhostId)

                    // C++: cBioEvent::Act() loops cPlayerManager::Get_Player_Object_List() and calls
                    // Send_Object_Update for every existing player before creating the new one.
                    // Send all existing players and soldiers to the new joiner first.
                    for ((existingId, existingPlayer) in playersByHost) {
                        val existingNetId = playerNetIds[existingId] ?: continue
                        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, existingPlayer, existingNetId) }
                    }
                    for ((_, existingSoldier) in soldiersByHost) {
                        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, existingSoldier, existingSoldier.networkId) }
                    }

                    // C++: cBioEvent::Act() creates the cPlayer object. Send BIT_CREATION here.
                    val nickname = playerNicknames.remove(host.address) ?: "Player$rhostId"
                    val team = playerTeams[rhostId] ?: 0
                    val player = Player(id = rhostId, name = nickname, team = team, isInGame = true)
                    val playerNetId = NetworkObjectManager.getNewDynamicId()
                    playerNetIds[rhostId] = playerNetId
                    NetworkObjectManager.registerObject(player, playerNetId)
                    playersByHost[rhostId] = player
                    sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, player, playerNetId) }

                    // Broadcast new player creation to all other already-in-game hosts.
                    for (otherId in playerInGame) {
                        if (otherId == rhostId) continue
                        val otherHost = connectionManager.getHost(otherId) ?: continue
                        sendGameNetObj(otherHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, player, playerNetId) }
                    }

                    sendGameDataUpdateEvent(host)
                    spawnSoldier(host, rhostId)
                }
            }
            1027 -> {  // NETCLASSID_LOADINGEVENT (loadingevent.cpp Import_Creation)
                val senderId = snap.getInt()
                val isLoading = snap.getBool()
                println("[GAME] LOADINGEVENT from rhostId=$rhostId senderId=$senderId isLoading=$isLoading → ignored")
            }
            1032 -> {  // NETCLASSID_CLIENTFPS — Import_Creation reads ClientId
                val clientId = snap.getInt()
                println("[GAME] CLIENTFPS creation from rhostId=$rhostId clientId=$clientId")
            }
            1033 -> {  // NETCLASSID_CSPINGREQUESTEVENT — Act() sends ScPingResponseEvent back
                // C++: cCsPingRequestEvent::Import_Creation reads SenderId + PingNumber, then Act().
                // Act() creates cScPingResponseEvent(PingNumber) and sends it to the requesting client.
                val senderId = snap.getInt()
                val pingNumber = snap.getInt()
                println("[GAME] CSPINGREQUESTEVENT from rhostId=$rhostId senderId=$senderId pingNumber=$pingNumber → ScPingResponseEvent")
                val response = ScPingResponseEvent(pingNumber)
                sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, NetworkObjectManager.getNewDynamicId()) }
            }
            else -> println("[GAME] unhandled classId=$classId netId=$networkId from rhostId=$rhostId")
        }
    }

    // Sends a PLAYER BIT_RARE update (no classId — not a creation packet).
    // C++: cPlayer::Export_Rare + Export_Occasional + Export_Frequent.
    // dirtyBits=0x07 = BIT_RARE|BIT_OCCASIONAL|BIT_FREQUENT (not BIT_CREATION).
    private fun sendPlayerRareUpdate(host: RemoteHost, rhostId: Int) {
        val netId = playerNetIds[rhostId] ?: run {
            println("[GAME] sendPlayerRareUpdate: no playerNetId for rhostId=$rhostId, skipping")
            return
        }
        val player = playersByHost[rhostId] ?: run {
            println("[GAME] sendPlayerRareUpdate: no player object for rhostId=$rhostId, skipping")
            return
        }
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeRareUpdate(bs, player, netId) }
        println("[GAME] sent PLAYER BIT_RARE to host $rhostId: team=${if (player.team == 0) "NOD" else "GDI"} inGame=${player.isInGame} netId=$netId")
    }

    // Handles an UNRELIABLE game packet that is NOT a creation (no BIT_CREATION).
    // These are CClientControl frequent updates: the client sends its soldier's position/state
    // every tick. C++: clientcontrol.cpp:114-134 reads SmartObjId → Import_Control_Cs + Import_State_Cs.
    // Wire format (after the header was already consumed by handleGamePacket):
    //   smartObjId: int
    //   if smartObjId != -1:
    //     in_vehicle: bool
    //     has_weapon: bool; if true: weaponDefId(32) + totalRounds(32)
    //     position: x,y,z floats (BITPACK_WORLD_POSITION)
    //     human_state: int (BITPACK_HUMAN_STATE)
    //     human_sub_state: int (BITPACK_HUMAN_SUB_STATE)
    //     is_special_damage: bool
    //     [PhysicalGameObj] on_host_bone: bool
    //     [ArmedGameObj] targeting: x,y,z floats (BITPACK_WORLD_POSITION)
    //     [SmartGameObj] continuousBoolBits: byte (BITPACK_CONTINUOUS_BOOLEAN_BITS)
    //     [SmartGameObj] 4x analog floats (BITPACK_ANALOG_VALUES)
    private fun handleFrequentUpdate(snap: BitStream, rhostId: Int) {
        val smartObjId = try { snap.getInt() } catch (e: Exception) { return }
        if (smartObjId == -1) return  // no controlled object

        val soldier = soldiersByHost[rhostId] ?: return
        if (soldier.networkId != smartObjId) {
            // SmartObjId doesn't match — client may not be controlling their soldier yet
            return
        }

        try {
            val inVehicle = snap.getBool()
            if (inVehicle) return  // vehicle control not yet handled

            val hasWeapon = snap.getBool()
            if (hasWeapon) {
                snap.getInt()  // weaponDefId (discard — we don't trust client weapon data)
                snap.getInt()  // totalRounds
            }

            val x = snap.getFloat(BITPACK_WORLD_POSITION_X)
            val y = snap.getFloat(BITPACK_WORLD_POSITION_Y)
            val z = snap.getFloat(BITPACK_WORLD_POSITION_Z)
            snap.getInt(BITPACK_HUMAN_STATE)      // human_state (not yet used server-side)
            snap.getInt(BITPACK_HUMAN_SUB_STATE)  // human_sub_state
            snap.getBool()                         // is_special_damage

            // ArmedGameObj via super chain: on_host_bone + targeting
            snap.getBool()  // on_host_bone (PhysicalGameObj)
            val tx = snap.getFloat(BITPACK_WORLD_POSITION_X)
            val ty = snap.getFloat(BITPACK_WORLD_POSITION_Y)
            val tz = snap.getFloat(BITPACK_WORLD_POSITION_Z)

            // SmartGameObj control: continuousBoolBits + 4 analog floats
            snap.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS)
            snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_FORWARD
            snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_LEFT
            snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_UP
            snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_TURN_LEFT

            // Update soldier's authoritative state on the server
            soldier.position  = Vector3(x, y, z)
            soldier.targeting = Vector3(tx, ty, tz)

            // Mark BIT_FREQUENT dirty for all other in-game clients so the replication tick
            // will forward the position update unreliably
            for (otherId in playerInGame) {
                if (otherId != rhostId) {
                    soldier.setObjectDirtyBit(otherId, NetworkObject.BIT_FREQUENT, true)
                }
            }
        } catch (e: Exception) {
            // Malformed packet — discard silently
        }
    }

    // C++: gamedataupdateevent.cpp Export_Creation — sent after client finishes loading.
    // Signals the client that gameplay can proceed (activates combat mode via Act()).
    // C++ server sends timeRemainingSeconds = timeLimitMinutes * 60 and hostedGameNumber = 1+.
    private fun sendGameDataUpdateEvent(host: RemoteHost) {
        val timeRemaining = config.timeLimitMinutes * 60  // C++: gamedataupdateevent.h:27 — INT
        val event = GameDataUpdateEvent(
            timeRemainingSeconds = timeRemaining,
            hostedGameNumber = hostedGameNumber,
        )
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, event, NetworkObjectManager.getNewDynamicId()) }
        println("[GAME] sent GAMEDATAUPDATEEVENT to host ${host.id}: timeRemaining=${timeRemaining}s hostedGame=$hostedGameNumber")
    }

    // Builds a RELIABLE packet, enqueues it in the host's reliable channel, and sends it immediately.
    // The packet ID is pre-assigned from host.reliable.nextSendId so the wire bytes are consistent.
    private fun sendGameNetObj(host: RemoteHost, writePayload: (BitStream) -> Unit) {
        val p = Packet()
        p.type = PacketType.RELIABLE
        p.id = host.reliable.nextSendId  // enqueue() will assign this same ID
        p.senderId = 0                   // server ID
        writePayload(p.payload)
        val wireData = Packet.buildWirePacket(p)
        val payloadBits = p.payload.bitWritePosition
        val hexDump = wireData.joinToString(" ") { "%02x".format(it) }
        println("[GAME] → RELIABLE id=${p.id} to host ${host.id} (${wireData.size}B wire, ${payloadBits}b payload): $hexDump")
        host.reliable.enqueue(p, wireData)
        enqueueWithCrc(PacketCombiner.combine(listOf(host.address to wireData)))
    }

    companion object {
        // C++ networkobjectmgr.h ID ranges:
        //   DYNAMIC: 1,500,000,000 - 2,100,000,000 (server-created objects: events, soldiers, etc.)
        //   STATIC:  2,100,000,001 - 2,110,000,000 (level static objects incl. teams)
        //   CLIENT:  2,110,000,001 - 2,122,800,001 (100K per client)

        // Stable network IDs for server-created singleton objects in the STATIC range.
        // C++: cTeam uses NETID_STATIC_OBJECT_MIN + offset; these values match C++ server logs exactly.
        private const val NET_ID_NOD_TEAM = 2_100_000_004
        private const val NET_ID_GDI_TEAM = 2_100_000_005
    }

    // ---- Encoder setup ----

    // Configures all BITPACK_* encoders required by soldier/game-object packets.
    // Position encoders use world extents from the map's .lsd file; others use fixed ranges.
    // C++: combatgmode.cpp:1063-1076, control.cpp:454-466, humanstate.cpp:1388-1389, damage.cpp:1321-1325
    // Loads definition IDs for NOD/GDI soldiers from always.dat → objects.ddb.
    // Falls back to config values if the file is missing or the presets aren't found.
    private fun loadDefinitions() {
        val dataDir = if (config.dataPath.isNotEmpty()) File(config.dataPath) else File(".")
        // Renegade loads definitions from objects.ddb which lives inside Always.dbs
        // (init.cpp loads Always2.dat, Always.dbs, Always.dat in that order).
        val candidates = listOf("always.dbs", "always2.dat", "always.dat")
        val ddbData = candidates.firstNotNullOfOrNull { fileName ->
            val file = File(dataDir, fileName)
            if (!file.exists()) return@firstNotNullOfOrNull null
            try {
                val mix = MixReader(file.readBytes())
                val ddb = mix.readFile("objects.ddb")
                if (ddb != null) {
                    println("[SERVER] found objects.ddb in $fileName (${mix.fileCount()} files in archive)")
                    ddb
                } else null
            } catch (e: Exception) {
                println("[SERVER] $fileName: ${e.message}")
                null
            }
        }
        if (ddbData == null) {
            println("[SERVER] objects.ddb not found in any of $candidates under ${dataDir.absolutePath}, using config definition IDs")
            return
        }
        val definitions = readDefinitions(ddbData)
        println("[SERVER] loaded ${definitions.size} definitions from always.dat/objects.ddb")

        val nodDef = definitions.find { it.name.equals("CnC_Nod_Minigunner_0", ignoreCase = true) }
        val gdiDef = definitions.find { it.name.equals("CnC_GDI_MiniGunner_0", ignoreCase = true) }

        if (nodDef != null) {
            nodSoldierDefId = nodDef.id.toInt()
            println("[SERVER] NOD soldier: ${nodDef.name} defId=0x${nodSoldierDefId.toUInt().toString(16)}")
        } else {
            println("[SERVER] CnC_Nod_Minigunner_0 not found in objects.ddb, using config value 0x${nodSoldierDefId.toUInt().toString(16)}")
        }
        if (gdiDef != null) {
            gdiSoldierDefId = gdiDef.id.toInt()
            println("[SERVER] GDI soldier: ${gdiDef.name} defId=0x${gdiSoldierDefId.toUInt().toString(16)}")
        } else {
            println("[SERVER] CnC_GDI_MiniGunner_0 not found in objects.ddb, using config value 0x${gdiSoldierDefId.toUInt().toString(16)}")
        }

        val weaponDefs = definitions.filter { it.name.contains("weapon", ignoreCase = true) || it.name.contains("pistol", ignoreCase = true) }
        println("[SERVER] Weapon definitions found (${weaponDefs.size}):")
        weaponDefs.forEach { println("[SERVER]   ${it.name} id=0x${it.id.toString(16)} classId=${it.classId}") }

        val pistolDef = definitions.find { it.name.equals("Weapon_Pistol_Player", ignoreCase = true) }
        if (pistolDef != null) {
            pistolWeaponDefId = pistolDef.id.toInt()
            println("[SERVER] Using pistol: ${pistolDef.name} defId=0x${pistolWeaponDefId.toUInt().toString(16)}")
        } else {
            println("[SERVER] No pistol weapon found in objects.ddb")
        }
    }

    private fun initEncoders() {
        val extents = loadWorldExtents()
        worldExtents = extents

        if (extents != null) {
            val margin = 1.0
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X,
                extents.minX - margin, extents.maxX + margin, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y,
                extents.minY - margin, extents.maxY + margin, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z,
                extents.minZ - margin, extents.maxZ + margin, 0.2)
            println("[SERVER] world position encoders configured from LSD extents: " +
                "X[${extents.minX}..${extents.maxX}] " +
                "Y[${extents.minY}..${extents.maxY}] " +
                "Z[${extents.minZ}..${extents.maxZ}]")
        } else {
            // Fallback: wide range so any position can be represented
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            println("[SERVER] using default world extents (±500), no map LSD loaded")
        }

        // Fixed-range encoders (identical on client and server, from vanilla Renegade source)
        EncoderRegistry.setPrecision(BITPACK_ONE_TIME_BOOLEAN_BITS,  23)           // control.cpp: NUM_BOOLEAN_ONE_TIME
        EncoderRegistry.setPrecision(BITPACK_CONTINUOUS_BOOLEAN_BITS, 4)           // control.cpp: NUM_BOOLEAN_CONTINUOUS
        EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)      // control.cpp
        EncoderRegistry.setPrecision(BITPACK_HEALTH,          0.0, 2000.0, 1.0)   // damage.cpp
        EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)   // damage.cpp
        EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE,     0.0, config.armorTypeCount.toDouble(), 1.0) // damage.cpp
        EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE,     0.0, 12.0,  1.0)    // humanstate.cpp: HIGHEST_HUMAN_STATE=12
        EncoderRegistry.setPrecision(BITPACK_HUMAN_SUB_STATE, 0.0, 511.0, 1.0)    // humanstate.cpp: (1<<9)-1=511
        EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_CS, 8)                 // control.cpp: CONTROL_TURN_RIGHT+1=8
        EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_SC, 6)                 // control.cpp: CONTROL_MOVE_DOWN+1=6
    }

    // Reads the map .mix file and extracts world extents from the embedded .lsd file.
    // Returns null only if no MapName is configured. Throws if the map file cannot be loaded.
    private fun loadWorldExtents(): WorldExtents? {
        if (config.mapName.isEmpty()) {
            println("[SERVER] no MapName configured, skipping LSD load")
            return null
        }
        // Strip .mix extension if already present (config may include it or not)
        val baseName = if (config.mapName.endsWith(".mix", ignoreCase = true))
            config.mapName.dropLast(4) else config.mapName
        val mixName = "$baseName.mix"
        val lsdName = "$baseName.lsd"
        val dataDir = if (config.dataPath.isNotEmpty()) File(config.dataPath) else File(".")
        val mixFile = File(dataDir, mixName)
        if (!mixFile.exists()) {
            error("[SERVER] map MIX not found: ${mixFile.absolutePath}")
        }
        val mixData = mixFile.readBytes()
        val mix = MixReader(mixData)
        val lsdData = mix.readFile(lsdName)
            ?: error("[SERVER] $lsdName not found inside $mixName")
        val extents = extractLevelExtents(lsdData)
        println("[SERVER] loaded world extents from $lsdName in $mixName")
        return extents
    }

    // ---- Soldier spawn ----

    // Sends a NETCLASSID_GAMEOBJ (1000) creation packet to spawn a soldier for the given player.
    // This sets COMBAT_STAR on the client (SmartGameObj::Import_Creation) and clears "Gameplay is pending".
    // C++: cGod::Create_Commando creates a SoldierGameObj with preset CnC_Nod_Minigunner_0 / CnC_GDI_MiniGunner_0.
    // Wire format: all 4 dirty-bit tiers (BIT_CREATION|RARE|OCCASIONAL|FREQUENT = 0x0F).
    private fun spawnSoldier(host: RemoteHost, rhostId: Int) {
        println("[GAME] spawnSoldier: rhostId=$rhostId team=${playerTeams[rhostId]}")
        val team = playerTeams[rhostId] ?: run {
            println("[GAME] spawnSoldier: no team for rhostId=$rhostId, skipping")
            return
        }
        val defId = if (team == 0) nodSoldierDefId else gdiSoldierDefId
        if (defId == 0) {
            println("[GAME] soldier spawn skipped for host $rhostId " +
                "(no soldier definition ID available)")
            return
        }

        // Hardcoded spawn position for Phase 2A. Proper spawn-point lookup is Phase 2B.
        // Model names match the C++ CnC_Nod_Minigunner_0 / CnC_GDI_MiniGunner_0 presets.
        // animName must be non-empty: C++ always sends a real animation; an empty string
        // causes the client to fail loading the soldier's animation state and never ACK.
        val modelName = if (team == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
        val weapons = if (pistolWeaponDefId != 0) {
            listOf(WeaponEntry(pistolWeaponDefId, 100))
        } else emptyList()
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = rhostId,
            team         = team,
            modelName    = modelName,
            animName     = "S_A_HUMAN.H_A_AINM",
            position     = Vector3(0f, 0f, 5f),
            weapons      = weapons,
        )
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(soldier, netId)
        soldiersByHost[rhostId] = soldier
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, soldier, netId) }
        println("[GAME] sent soldier spawn to host $rhostId: team=${if (team == 0) "NOD" else "GDI"} " +
            "defId=$defId netId=$netId")

        // Broadcast new soldier creation to all other already-in-game hosts.
        for (otherId in playerInGame) {
            if (otherId == rhostId) continue
            val otherHost = connectionManager.getHost(otherId) ?: continue
            sendGameNetObj(otherHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, soldier, netId) }
            println("[GAME] sent soldier spawn (broadcast) to host $otherId: netId=$netId")
        }
    }

    // ---- Application acceptance ----

    private fun checkApplication(
        packet: ccr.net.protocol.Packet,
        address: java.net.InetSocketAddress,
    ): ccr.net.protocol.RefusalCode {
        // Payload layout (cnetwork.cpp:213-216):
        //   [nickname: wideString][password: wideString, permitEmpty][exeKey: int][bbo: int ← read by wwnet]
        // Must consume all 3 app fields so ConnectionManager reads bbo at the correct position.
        return try {
            val readStart = packet.payload.bitReadPosition
            val nickname = packet.payload.getWideString()
            val clientPassword = packet.payload.getWideString(permitEmpty = true)
            val clientExeKey = packet.payload.getInt()
            val bitsConsumed = packet.payload.bitReadPosition - readStart
            println("[CONNECT] checkApplication from $address: nickname='$nickname' password='$clientPassword' exeKey=0x${clientExeKey.toString(16)} bitsConsumed=$bitsConsumed payloadBits=${packet.bitLength}")

            if (nickname.isEmpty()) {
                println("[CONNECT] → REFUSED: empty nickname")
                return ccr.net.protocol.RefusalCode.VERSION_MISMATCH
            }

            if (config.password.isNotEmpty() && clientPassword != config.password) {
                println("[CONNECT] → REFUSED: bad password (expected='${config.password}' got='$clientPassword')")
                return ccr.net.protocol.RefusalCode.BAD_PASSWORD
            }

            if (config.versionNumber != 0 && clientExeKey != config.versionNumber) {
                println("[CONNECT] → REFUSED: exe key mismatch (expected=0x${Integer.toUnsignedString(config.versionNumber, 16)} got=0x${Integer.toUnsignedString(clientExeKey, 16)})")
                return ccr.net.protocol.RefusalCode.VERSION_MISMATCH
            }

            println("[CONNECT] → ACCEPTED: nickname='$nickname' exeKey=0x${clientExeKey.toString(16)}")
            playerNicknames[address] = nickname
            ccr.net.protocol.RefusalCode.CLIENT_ACCEPTED
        } catch (e: Exception) {
            println("[CONNECT] → REFUSED: exception reading payload from $address: $e (payloadBits=${packet.bitLength} readPos=${packet.payload.bitReadPosition})")
            ccr.net.protocol.RefusalCode.VERSION_MISMATCH
        }
    }

    // ---- RCON commands ----

    private fun handleRconCommand(command: String): String {
        val parts = command.trim().split(Regex("\\s+"), limit = 2)
        return when (parts[0].lowercase()) {
            "help" -> """
                Available commands:
                  help     - show this message
                  status   - show server status
                  players  - list connected players
            """.trimIndent()

            "status" -> """
                Server: ${config.serverName}
                Map: ${config.mapName}
                Players: ${connectionManager.getConnectedCount()} / ${config.maxPlayers}
                Port: ${config.gamePort}
                Objects: ${NetworkObjectManager.getObjectCount()}
            """.trimIndent()

            "players" -> {
                val count = connectionManager.getConnectedCount()
                if (count == 0) "No players connected."
                else (1..config.maxPlayers).mapNotNull { id ->
                    connectionManager.getHost(id)?.let { "[$id] ${it.address}" }
                }.joinToString("\n")
            }

            else -> "Unknown command: ${parts[0]}. Type 'help' for a list of commands."
        }
    }

    private fun buildWelcomeMessage(): String = buildString {
        append("=== ${config.serverName} ===\n")
        if (config.gameTitle.isNotEmpty()) append("${config.gameTitle}\n")
        append("Type 'help' for available commands.\n")
    }
}
