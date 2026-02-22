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
import ccr.server.level.LevelLoader
import ccr.server.level.LoadedLevel
import ccr.server.mix.MixReader
import ccr.server.mix.WorldExtents
import ccr.server.mix.extractLevelExtents
import ccr.math.Vector3
import ccr.server.net.EvictionEvent
import ccr.server.net.GameData
import ccr.server.net.GameDataUpdateEvent
import ccr.server.net.GameOptionsEvent
import ccr.server.net.NetworkObjectPacketWriter
import ccr.server.net.Player
import ccr.server.net.ScPingResponseEvent
import ccr.server.net.ServerFps
import ccr.server.net.SoldierGameObj
import ccr.server.net.Team
import ccr.server.net.WeaponEntry
import ccr.server.net.WinEvent
import ccr.server.net.BackgroundMgr
import ccr.server.net.WeatherMgr
import ccr.server.net.CsAnnouncement
import ccr.server.net.ScAnnouncement
import ccr.server.net.CsTextObj
import ccr.server.net.ScTextObj
import ccr.server.net.CsDamageEvent
import ccr.server.net.DonateEvent
import ccr.server.combat.ArmorWarheadManager
import ccr.server.net.PlayerKill
import ccr.server.net.PurchaseRequestEvent
import ccr.server.net.PurchaseResponseEvent
import ccr.server.net.RequestKillEvent
import ccr.server.net.SuicideEvent
import ccr.net.flow.BandwidthBudget
import ccr.net.flow.FlowController
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import ccr.server.level.ChunkIds
import ccr.server.level.ldd.LoadedBuildingGameObj
import ccr.server.level.ldd.LoadedVehicleGameObj
import ccr.server.net.BaseControllerClass
import ccr.server.net.BuildingGameObj
import ccr.server.net.ComCenterGameObj
import ccr.server.net.PowerPlantGameObj
import ccr.server.net.RefineryGameObj
import ccr.server.net.SoldierFactoryGameObj
import ccr.server.net.VehicleFactoryGameObj
import ccr.server.net.WarFactoryGameObj
import ccr.physics.scene.PhysicsScene
import ccr.server.level.PhysicsSceneBuilder
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.BuildingGameObjDef
import ccr.server.defs.WeaponDefinitionClass
import ccr.server.defs.combat.DoorPhysDefClass
import ccr.server.defs.combat.PowerUpGameObjDef
import ccr.server.defs.combat.RefineryGameObjDef
import ccr.math.OBBox
import ccr.math.Matrix3D as MathMatrix3D
import ccr.physics.static.DoorPhysClass
import ccr.server.net.DoorNetworkObject
import ccr.server.net.PowerUpGameObj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.File
import java.net.InetSocketAddress

/**
 * Main server orchestrator. Wires together UdpTransport, ConnectionManager,
 * RconServer, and LanBroadcastResponder into a single coroutine scope.
 *
 * Thread safety: All ConnectionManager / RemoteHost state is confined to
 * [gameThread] (single-thread dispatcher). UdpTransport communicates via
 * its own channels. RconServer and LanBroadcastResponder own their sockets.
 */
class GameServer(internal val config: ServerConfig) {

    private val gameThread = newSingleThreadContext("game-thread")
    private val transport = UdpTransport(config.gamePort)
    internal val connectionManager = ConnectionManager(config.maxPlayers)
    private val localIp = detectLocalIp()
    private val gameData = GameData(config, localIp)

    private val rconServer = RconServer(
        port = config.rconPort,
        password = config.rconPassword,
        remoteAdminAllowed = config.remoteAdminAllowed,
        welcomeMessage = buildWelcomeMessage(),
        commandHandler = ::handleRconCommand,
    )

    private val lanResponder = LanBroadcastResponder(config, connectionManager) { gameData.mapNameCrc }

    // Milliseconds per network tick
    private val tickIntervalMs: Long = 1000L / config.netUpdateRate.coerceAtLeast(1)

    // Nicknames for players currently in acceptance: populated in checkApplication,
    // consumed in BIOEVENT handler so that we can include the name in the Player network object.
    private val playerNicknames = mutableMapOf<java.net.InetSocketAddress, String>()

    // C++: cGameData::HostedGameNumber — increments each time a new game starts on this server.
    private var hostedGameNumber = 1

    // Singleton team objects registered at startup (stable static IDs).
    internal val teamNod = Team(teamNumber = 0)
    internal val teamGdi = Team(teamNumber = 1)

    // World extents loaded from the map's .lsd file at startup (null if not available).
    private var worldExtents: WorldExtents? = null

    // Loaded level data (definitions, static/dynamic data, spawners).
    internal var loadedLevel: LoadedLevel? = null

    // Physics scene built from static collision geometry during level load.
    var physicsScene: PhysicsScene? = null

    // Map rotation state (advanced at each round end)
    private var mapRotation: MapRotation = MapRotation(
        maps = config.effectiveMapList,
        loops = config.mapCycleLoops,
    )

    // Runtime name of the currently active map (may differ from config.mapName after rotation)
    private var currentMapName: String = mapRotation.currentName

    // Soldier definition IDs loaded from always.dat at startup; fall back to config values.
    internal var nodSoldierDefId: Int = config.nodSoldierDefId
    internal var gdiSoldierDefId: Int = config.gdiSoldierDefId
    internal var pistolWeaponDefId: Int = 0

    // C4 weapon and object definition IDs (populated by loadLevel / loadDefinitions).
    internal var timedC4WeaponDefId: Int = 0
    internal var tossedC4DefId: Int = 0
    internal var beaconWeaponDefId: Int = 0  // used to give soldiers beacon weapons in createCommando (like timedC4WeaponDefId)

    // SpawnManager resolves multiplayer spawn locations from loaded spawners.
    internal var spawnManager: SpawnManager? = null

    // Base controllers for NOD (playerType=0) and GDI (playerType=1) teams.
    internal var baseControllerNod: BaseControllerClass? = null
    internal var baseControllerGdi: BaseControllerClass? = null

    // Door physics + network objects from LSD static data.
    private val doorObjects = mutableListOf<Pair<DoorPhysClass, DoorNetworkObject>>()

    // God owns the player/soldier lifecycle (port of C++ cGod).
    internal val god = God(this)

    // Game state machine — timer, intermission, game-over (port of cGameData::Think).
    internal val gameState = GameState(config)

    // GameObjManager — owns all BaseGameObj instances and drives their Think() loops.
    internal val gameObjManager = GameObjManager()

    // GameContext — session-scoped container for shared game state (lazy to allow gameState init first).
    internal val gameContext: GameContext by lazy {
        GameContext(config = config, gameObjManager = gameObjManager, gameState = gameState)
    }

    // Periodic GameDataUpdateEvent resend (once per second) to keep clients' timer in sync.
    private var lastGameDataUpdateMs: Long = 0L

    // Per-tick outbox: packets buffered during tick, flushed together at end of tick
    private val pendingOutbox = mutableMapOf<Int, MutableList<Pair<InetSocketAddress, ByteArray>>>()
    private val bytesSentThisTick = mutableMapOf<Int, Int>()

    // C++: cConnection bandwidth management
    private val bandwidthBudget = BandwidthBudget(if (config.bandwidthBps > 0) config.bandwidthBps else 1_500_000)
    // Per-host flow controllers (C++: Adjust_Flow_If_Necessary in rhost.cpp)
    private val flowControllers = mutableMapOf<Int, FlowController>()

    // Tracks rhostIds of clients currently in a loading state (LOADINGEVENT).
    private val loadingHosts = mutableSetOf<Int>()

    // C++: cServerFps singleton — informs clients of server framerate.
    private val serverFps = ServerFps()

    // FPS tracking state
    private var lastFpsUpdateMs: Long = 0L
    private var fpsFrameCount: Int = 0
    private var frameDeltaSeconds: Float = 0f

    // Client FPS tracking: rhostId → last reported fps value.
    private val clientFpsMap = mutableMapOf<Int, Int>()

    // VendorClass — handles purchase terminal requests.
    private val vendor by lazy { VendorClass(this) }

    suspend fun run() = coroutineScope {
        loadLevel()        // Load level data (definitions, world extents, spawners) from MIX files
        initializeLevel()  // Set up encoders, SpawnManager, buildings and base controllers

        // Register WeatherMgr and BackgroundMgr singletons with well-known static IDs.
        val weatherMgr = WeatherMgr()
        val backgroundMgr = BackgroundMgr()
        NetworkObjectManager.registerObject(weatherMgr, NET_ID_SERVER_WEATHER)
        NetworkObjectManager.registerObject(backgroundMgr, NET_ID_SERVER_BACKGROUND)
        weatherMgr.setObjectDirtyBit(NetworkObject.BIT_RARE, true)
        backgroundMgr.setObjectDirtyBit(NetworkObject.BIT_RARE, true)

        // Register team singletons with static IDs. These persist for the lifetime of the server.
        // Dirty bits are set by Team.init — no explicit call needed here.
        NetworkObjectManager.registerObject(teamNod, NET_ID_NOD_TEAM)
        NetworkObjectManager.registerObject(teamGdi, NET_ID_GDI_TEAM)

        // Register ServerFps singleton (C++: cServerFps uses a static network ID)
        NetworkObjectManager.registerObject(serverFps, NET_ID_SERVER_FPS)

        connectionManager.applicationAcceptanceHandler = ::checkApplication
        connectionManager.connHandler = { id, host ->
            println("[CONNECT] client $id connected from ${host.address} bps=${host.maximumBps}")
            sendConnectionObjects(id, host)
        }
        connectionManager.disconnectHandler = { id ->
            println("[CONNECT] client $id disconnected")
            loadingHosts.remove(id)
            flowControllers.remove(id)
            god.removePlayer(id)
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
        var lastTickMs = System.currentTimeMillis()
        while (true) {
            val nowMs = System.currentTimeMillis()
            val tickDeltaMs = (nowMs - lastTickMs).coerceAtMost(1000L)
            lastTickMs = nowMs

            // Update frame delta seconds for use by think() loops
            frameDeltaSeconds = tickDeltaMs / 1000f
            gameContext.frameDeltaSeconds = frameDeltaSeconds

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

            // Advance game timer
            gameState.think(tickDeltaMs)
            gameState.currentPlayers = connectionManager.getConnectedCount()

            // Re-send GameDataUpdateEvent once per second to keep clients' timer in sync
            if (nowMs - lastGameDataUpdateMs >= 1000L && god.playerInGame.isNotEmpty()) {
                lastGameDataUpdateMs = nowMs
                for (clientId in god.playerInGame) {
                    val clientHost = connectionManager.getHost(clientId) ?: continue
                    sendGameDataUpdateEvent(clientHost)
                }
            }

            // Game-over detection (only check when players are in game and not already in intermission)
            if (!gameState.isIntermission && god.playerInGame.isNotEmpty()) {
                val (gameOver, winType) = gameState.checkGameOver(
                    isNodBaseDestroyed = baseControllerNod?.areAllBuildingsDestroyed() ?: false,
                    isGdiBaseDestroyed = baseControllerGdi?.areAllBuildingsDestroyed() ?: false,
                )
                if (gameOver) {
                    handleGameOver(winType)
                }
            }

            // Core restart after intermission: rotate to next map or same-map restart
            if (gameState.pendingCoreRestart) {
                gameState.pendingCoreRestart = false
                val nextMap = mapRotation.nextName()
                if (nextMap != null) {
                    mapRotation = mapRotation.advance()
                    handleMapRotation(nextMap)
                } else {
                    mapRotation = mapRotation.advance()  // reset index to 0 for next cycle
                    handleCoreRestart()
                }
            }

            // GameObjManager.think() — drives building Think() loops (refinery trickle, war factory timer, etc.)
            gameObjManager.think(frameDeltaSeconds)

            // SpawnManager.think() — ticks powerup spawner countdown timers
            spawnManager?.think(frameDeltaSeconds)

            // Update measured FPS and push to clients
            updateFps(nowMs)

            // God.think() — handles respawn loop (creates soldiers for soldierless in-game players)
            god.think(frameDeltaSeconds)

            // Tick door state machines and detect state changes for network replication
            if (doorObjects.isNotEmpty()) {
                val soldierPositions = god.soldiersByHost.values.map { it.position }
                for ((door, doorNet) in doorObjects) {
                    door.updateState(frameDeltaSeconds, soldierPositions)
                    doorNet.networkThink()
                }
            }

            // Mark driven vehicles BIT_FREQUENT dirty — gameObjManager.think() already advanced
            // their position via VehicleGameObj.think(). All clients need the updated position.
            for ((_, vehicle) in god.playerVehicles) {
                for (clientId in god.playerInGame) {
                    vehicle.setObjectDirtyBit(clientId, NetworkObject.BIT_FREQUENT, true)
                }
            }

            // Push dirty object state to all in-game clients
            replicationTick()

            // Centralized delete-pending: broadcast deletion + unregister any objects marked for deletion
            NetworkObjectManager.getAllObjects()
                .filter { it.isDeletePending }
                .forEach { obj ->
                    for (clientId in god.playerInGame) {
                        val clientHost = connectionManager.getHost(clientId) ?: continue
                        sendGameNetObj(clientHost) { bs ->
                            NetworkObjectPacketWriter.writeDeletion(bs, obj.networkId)
                        }
                    }
                    NetworkObjectManager.unregisterObject(obj)
                }

            // Clean up C4 objects that have been marked for deletion
            god.c4Objects.removeAll { it.isDeletePending }
            god.beaconObjects.removeAll { it.isDeletePending }

            // Flush per-tick packet outbox: combines buffered packets into fewer datagrams per host
            flushOutbox()

            // Adjust per-host flow controllers with bytes sent this tick
            val connectedCount = connectionManager.getConnectedCount()
            val targetBps = bandwidthBudget.perHostBps(connectedCount)
            for ((rhostId, bytesSent) in bytesSentThisTick) {
                flowControllers[rhostId]?.adjust(targetBps, bytesSent, tickDeltaMs.toFloat())
            }

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
            for (clientId in god.playerInGame) {
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
                    // BIT_FREQUENT only — skip own soldier; gate others through FlowController
                    if (!isOwnSoldier) {
                        val fc = flowControllers[clientId]
                        if (fc == null || fc.shouldSend(50.0f)) {
                            sendUnreliable(host) { bs -> NetworkObjectPacketWriter.writeFrequentUpdate(bs, obj, obj.networkId) }
                        }
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
        pendingOutbox.getOrPut(host.id) { mutableListOf() }.add(host.address to wireData)
    }

    private fun flushOutbox() {
        bytesSentThisTick.clear()
        for ((rhostId, packets) in pendingOutbox) {
            val datagrams = PacketCombiner.combine(packets)
            enqueueWithCrc(datagrams)
            bytesSentThisTick[rhostId] = datagrams.sumOf { it.data.size }
        }
        pendingOutbox.clear()
    }

    // ---- Physics tick ----

    // C++: wwphys PhysicsSceneClass runs at ~30 Hz
    private suspend fun physicsTickLoop() {
        val intervalMs = 1000L / 30
        var lastTickMs = System.currentTimeMillis()
        while (true) {
            delay(intervalMs)
            val nowMs = System.currentTimeMillis()
            val dt = ((nowMs - lastTickMs) / 1000f).coerceAtMost(0.1f)
            lastTickMs = nowMs
            physicsScene?.update(dt)
        }
    }

    // ---- Game event peek (for logging) ----

    // Reads the network object header from a payload without consuming it.
    // Wire layout: [networkId:32][dirtyBits:8 (BYTE)][isDeletePending:1 (compressed bool)]
    //              [networkClassId:32 if BIT_CREATION set]
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
                val networkClassId = snap.getInt()
                sb.append(" class=$networkClassId(${NetClassIds.name(networkClassId)})")
            }
            sb.toString()
        } catch (e: Exception) {
            "(peek: $e)"
        }
    }

    // ---- Connection objects ----

    // C++: cNetwork::Connection_Handler — sends initial game state to a newly connected client.
    // Sends ONLY Teams and GameOptionsEvent (matching C++ Connection_Handler).
    // All other objects (buildings, base controllers, ServerFps, players) are sent by
    // replicationTick() after the client sends BIOEVENT and enters the game.
    private fun sendConnectionObjects(rhostId: Int, host: RemoteHost) {
        // Auto-assign team to balance NOD/GDI. CHANGETEAMEVENT toggles NOD↔GDI.
        val assignedTeam = god.choosePlayerType()
        god.playerTeams[rhostId] = assignedTeam
        println("[CONNECT] sending connection objects to host $rhostId (team=${if (assignedTeam == 0) "NOD" else "GDI"})")

        // NOD team (teamNumber=0) and GDI team (teamNumber=1)
        // C++: cTeam dirty=BIT_CREATION(0x0F) — all 4 tiers sent on initial creation
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, teamNod, NET_ID_NOD_TEAM) }
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, teamGdi, NET_ID_GDI_TEAM) }

        // Clear team dirty bits for this client — already sent manually above;
        // restoreDirtyBits() in BIOEVENT would otherwise re-mark them.
        teamNod.setObjectDirtyBits(rhostId, 0)
        teamGdi.setObjectDirtyBits(rhostId, 0)

        // GameOptionsEvent — one-time creation event; tells the client what game/map is running.
        // C++: gameoptionsevent.cpp Export_Creation calls Export_Tier_1_Data + Export_Tier_2_Data.
        // cNetEvent subclass has no RARE/OCCASIONAL/FREQUENT state, so those tiers write nothing.
        gameData.currentPlayers = connectionManager.getConnectedCount()
        val gameOptionsEvent = GameOptionsEvent(gameData)
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, gameOptionsEvent, NetworkObjectManager.getNewDynamicId()) }

        // Player creation is NOT sent here. C++ sends it in cBioEvent::Act() after the client
        // finishes loading. See BIOEVENT handler (networkClassId=1026).
        // Buildings, base controllers, and ServerFps are sent by replicationTick() after BIOEVENT.
    }

    // ---- Game event handlers ----

    // C++: pkthandlers.cpp / neteventhandlers.cpp — dispatches incoming game events by networkClassId.
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

        val networkClassId = snap.getInt()
        val host = connectionManager.getHost(rhostId) ?: return

        when (networkClassId) {
            1019 -> {  // NETCLASSID_CSTEXTOBJ (chat, header=1018, wire=1019)
                try {
                    val msg = CsTextObj()
                    msg.importCreation(snap)
                    println("[GAME] CHAT from rhostId=$rhostId type=${msg.type} text='${msg.text}'")
                    // TEXT_MESSAGE_PUBLIC=0, TEXT_MESSAGE_TEAM=1, TEXT_MESSAGE_PRIVATE=2
                    val relay = ScTextObj(
                        type = msg.type,
                        senderId = msg.senderId,
                        recipientId = msg.recipientId,
                        isHostAdminMessage = msg.isHostAdminMessage,
                        text = msg.text,
                    )
                    when (msg.type) {
                        0 -> {  // PUBLIC — broadcast to all in-game
                            for (clientId in god.playerInGame) {
                                val clientHost = connectionManager.getHost(clientId) ?: continue
                                sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                            }
                        }
                        1 -> {  // TEAM — send to same team only
                            val senderTeam = god.playerTeams[rhostId] ?: -1
                            for (clientId in god.playerInGame) {
                                if ((god.playerTeams[clientId] ?: -1) != senderTeam) continue
                                val clientHost = connectionManager.getHost(clientId) ?: continue
                                sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                            }
                        }
                        2 -> {  // PRIVATE — send to sender and recipient only
                            val recipientRhostId = god.playersByHost.entries.find { it.value.id == msg.recipientId }?.key
                            for (clientId in listOfNotNull(rhostId, recipientRhostId)) {
                                val clientHost = connectionManager.getHost(clientId) ?: continue
                                sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                            }
                        }
                        else -> {
                            sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                        }
                    }
                } catch (e: Exception) {
                    println("[GAME] CSTEXTOBJ parse error from rhostId=$rhostId: $e")
                }
            }
            1018 -> {  // NETCLASSID_CLIENTCONTROL — sent BEFORE loading; do not spawn here
                // Import_Creation reads ClientId. Spawning here would send a game-object
                // packet to a client that hasn't loaded the world yet.  Wait for BioEvent.
                val clientId = snap.getInt()  // Import_Creation: ClientId
                println("[GAME] CLIENTCONTROL from rhostId=$rhostId clientId=$clientId")
            }
            1021 -> {  // NETCLASSID_CHANGETEAMEVENT (changeteamevent.cpp Import_Creation)
                // Client sends only SenderId. Act() toggles NOD(0)↔GDI(1).
                val senderId = snap.getInt()
                if (!config.isTeamChangingAllowed) {
                    println("[GAME] CHANGETEAMEVENT from rhostId=$rhostId: team changing is disabled, ignored")
                    return
                }
                val currentTeam = god.playerTeams[rhostId] ?: 0
                val newTeam = if (currentTeam == 0) 1 else 0
                god.playerTeams[rhostId] = newTeam
                god.playersByHost[rhostId]?.team = newTeam  // sync stored player object
                println("[GAME] CHANGETEAMEVENT from rhostId=$rhostId senderId=$senderId: ${if (currentTeam == 0) "NOD" else "GDI"} → ${if (newTeam == 0) "NOD" else "GDI"}")
                // Kill existing soldier so god.think() respawns with the new team
                god.deleteSoldier(rhostId)
                sendPlayerRareUpdate(host, rhostId)
            }
            1026 -> {  // NETCLASSID_BIOEVENT — sent after Load_Level() in gameinitmgr.cpp
                // cBioEvent::Act() on the original server: creates cPlayer (Is_In_Game defaults true),
                // then sends cGameDataUpdateEvent.  cGod::Think() then spawns the soldier because
                // Is_Active && Is_In_Game are both true.  This is the correct post-load trigger.
                if (rhostId !in god.playerInGame) {
                    println("[GAME] BIOEVENT from rhostId=$rhostId → entering game (post-load)")
                    god.playerInGame.add(rhostId)
                    flowControllers[rhostId] = FlowController()

                    // Mark all registered objects dirty for this new client.
                    // C++: Tell_Client_About_Dynamic_Objects sets per-client dirty bits for all objects.
                    // replicationTick() will send everything on the next tick.
                    NetworkObjectManager.restoreDirtyBits(rhostId)

                    // Teams were already sent in sendConnectionObjects — clear their dirty bits
                    teamNod.setObjectDirtyBits(rhostId, 0)
                    teamGdi.setObjectDirtyBits(rhostId, 0)

                    // Create player — sets BIT_CREATION for all clients via setObjectDirtyBit
                    val nickname = playerNicknames.remove(host.address) ?: "Player$rhostId"
                    god.createPlayer(rhostId, nickname)

                    // One-shot event to signal the client that gameplay can proceed
                    sendGameDataUpdateEvent(host)
                    // Soldier spawning happens via god.think() on the next tick
                    // All object creation packets sent by replicationTick() on the next tick
                }
            }
            1025 -> {  // NETCLASSID_CLIENTGOODBYEEVENT (header=1024, wire=1025) — graceful disconnect
                val senderId = snap.getInt()
                println("[GAME] CLIENTGOODBYE from rhostId=$rhostId senderId=$senderId — removing player")
                flowControllers.remove(rhostId)
                god.removePlayer(rhostId)
            }
            1027 -> {  // NETCLASSID_LOADINGEVENT (loadingevent.cpp Import_Creation)
                val senderId = snap.getInt()
                val isLoading = snap.getBool()
                if (isLoading) loadingHosts.add(rhostId) else loadingHosts.remove(rhostId)
                println("[GAME] LOADINGEVENT from rhostId=$rhostId senderId=$senderId isLoading=$isLoading")
            }
            1032 -> {  // NETCLASSID_CLIENTFPS — Import_Creation reads ClientId only; fps is in frequent updates
                val clientId = snap.getInt()
                println("[GAME] CLIENTFPS from rhostId=$rhostId clientId=$clientId")
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
            1020 -> {  // NETCLASSID_SUICIDEEVENT (wire 1019+1)
                val senderId = snap.getInt()
                println("[GAME] SUICIDEEVENT from rhostId=$rhostId senderId=$senderId")
                if (!gameState.isGameplayPermitted) return
                if (rhostId in god.soldiersByHost) {
                    broadcastPlayerKill(-1, rhostId)
                    god.deleteSoldier(rhostId)
                }
            }
            1024 -> {  // NETCLASSID_PURCHASEREQUESTEVENT (wire 1023+1)
                val event = PurchaseRequestEvent()
                event.importCreation(snap)
                println("[GAME] PURCHASEREQUESTEVENT from rhostId=$rhostId senderId=${event.senderId} " +
                    "type=${event.purchaseType} item=${event.itemIndex} altSkin=${event.altSkinIndex}")
                if (!gameState.isGameplayPermitted) {
                    val response = PurchaseResponseEvent(purchaserId = event.senderId, responseId = 2)
                    sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, NetworkObjectManager.getNewDynamicId()) }
                    return
                }
                val result = vendor.handlePurchase(rhostId, event.purchaseType, event.itemIndex, event.altSkinIndex)
                val response = PurchaseResponseEvent(purchaserId = event.senderId, responseId = result.responseId)
                sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, NetworkObjectManager.getNewDynamicId()) }
                if (result.responseId == VendorClass.RESPONSE_SUCCESS) {
                    if (result.isVehiclePurchase) {
                        // Route to vehicle factory — find the team's available factory and start the timer
                        val playerTeam = god.playerTeams[rhostId] ?: 0
                        val baseController = if (playerTeam == 0) baseControllerNod else baseControllerGdi
                        val factory = gameObjManager.getAllObjects()
                            .filterIsInstance<VehicleFactoryGameObj>()
                            .find { it.baseController === baseController && !it.isBusy && !it.isDestroyed }
                        if (factory != null) {
                            factory.requestVehicle(result.purchasedDefId, 12.0f, rhostId)
                            println("[GAME] vehicle order queued: defId=${result.purchasedDefId} factory=${factory.networkId} buyer=$rhostId")
                        } else {
                            // Factory became unavailable between vendor check and now — log only
                            // (VendorClass already checked canGenerateVehicles so this is very rare)
                            println("[GAME] no available vehicle factory for team=$playerTeam (race condition)")
                        }
                    } else {
                        // Kill current soldier and respawn as purchased character
                        god.deleteSoldier(rhostId)
                        val playerTeam = god.playerTeams[rhostId] ?: 0
                        val purchasedModelName = if (playerTeam == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
                        god.createCommandoWithDef(rhostId, playerTeam, result.purchasedDefId, purchasedModelName)
                    }
                }
            }
            1034 -> {  // NETCLASSID_CSDAMAGEEVENT (header=1033, wire=1034) — client reports damage dealt
                if (!gameState.isGameplayPermitted) return
                val event = CsDamageEvent()
                event.importCreation(snap)
                println("[GAME] CSDAMAGEEVENT from rhostId=$rhostId damagee=${event.damageeGoid} damage=${event.damage} warhead=${event.warhead}")
                val target = gameObjManager.findObject(event.damageeGoid)
                if (target != null) {
                    val scaledDamage = ArmorWarheadManager.scaleDamage(event.damage, event.warhead, target.shieldType)
                    target.applyDamage(scaledDamage)
                    println("[GAME] applied damage=$scaledDamage to netId=${event.damageeGoid} health=${target.health}")
                    if (target.isDead) {
                        val victimRhostId = god.soldiersByHost.entries.find { it.value.networkId == event.damageeGoid }?.key
                        if (victimRhostId != null) {
                            broadcastPlayerKill(rhostId, victimRhostId)
                            god.deleteSoldier(victimRhostId)
                        }
                    }
                } else {
                    println("[GAME] CSDAMAGEEVENT: target netId=${event.damageeGoid} not found in gameObjManager")
                }
            }
            1035 -> {  // NETCLASSID_REQUESTKILLEVENT (wire 1034+1)
                val event = RequestKillEvent()
                event.importCreation(snap)
                println("[GAME] REQUESTKILLEVENT from rhostId=$rhostId objectId=${event.objectId}")
                // Only allow self-kill: check if the requested object is this player's soldier
                val soldier = god.soldiersByHost[rhostId]
                if (soldier != null && soldier.networkId == event.objectId) {
                    broadcastPlayerKill(-1, rhostId)
                    god.deleteSoldier(rhostId)
                }
            }
            1038 -> {  // NETCLASSID_CSANNOUNCEMENT (wire 1037+1)
                try {
                    val announcement = CsAnnouncement()
                    announcement.importCreation(snap)
                    println("[GAME] CSANNOUNCEMENT from rhostId=$rhostId fromId=${announcement.fromId} " +
                        "toId=${announcement.toId} announcementId=${announcement.announcementId} " +
                        "radioCmdId=${announcement.radioCmdId} type=${announcement.type}")
                    // Relay to all in-game clients as ScAnnouncement
                    val relay = ScAnnouncement(
                        toId = announcement.toId,
                        fromId = announcement.fromId,
                        announcementId = announcement.announcementId,
                        radioCmdId = announcement.radioCmdId,
                        type = announcement.type,
                    )
                    for (clientId in god.playerInGame) {
                        val clientHost = connectionManager.getHost(clientId) ?: continue
                        sendGameNetObj(clientHost) { bs ->
                            NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId())
                        }
                    }
                } catch (e: Exception) {
                    println("[GAME] CSANNOUNCEMENT parse error from rhostId=$rhostId: $e")
                }
            }
            1039 -> {  // NETCLASSID_DONATEEVENT (wire 1038+1)
                if (!gameState.isGameplayPermitted) return
                val event = DonateEvent()
                event.importCreation(snap)
                val sender = god.playersByHost.values.find { it.id == event.senderId }
                val recipient = god.playersByHost.values.find { it.id == event.recipientId }
                if (sender != null && recipient != null) {
                    val amount = event.amount.toFloat().coerceAtMost(sender.money)
                    if (amount > 0) {
                        sender.addMoney(-amount)
                        recipient.addMoney(amount)
                        println("[GAME] DONATEEVENT from rhostId=$rhostId: ${sender.name} donated $amount credits to ${recipient.name}")
                    } else {
                        println("[GAME] DONATEEVENT from rhostId=$rhostId: insufficient funds (has ${sender.money}, tried ${event.amount})")
                    }
                } else {
                    println("[GAME] DONATEEVENT from rhostId=$rhostId: sender=${event.senderId} or recipient=${event.recipientId} not found")
                }
            }
            else -> println("[GAME] unhandled networkClassId=$networkClassId netId=$networkId from rhostId=$rhostId")
        }
    }

    // Broadcasts a PlayerKill event to all in-game clients and updates scoring.
    // C++: cPlayerKill (playerkill.cpp) — S→C event sent when a player dies.
    // killerId = rhostId of killer (-1 = no killer / suicide).
    // victimId = rhostId of victim.
    private fun broadcastPlayerKill(killerId: Int, victimId: Int) {
        // Update player scores
        if (killerId >= 0) {
            god.playersByHost[killerId]?.incrementScore(1f)
            god.playersByHost[killerId]?.incrementKills()
            val killerTeam = god.playerTeams[killerId]
            if (killerTeam == 0) teamNod.incrementKills()
            if (killerTeam == 1) teamGdi.incrementKills()
        }
        god.playersByHost[victimId]?.incrementDeaths()
        val victimTeam = god.playerTeams[victimId]
        if (victimTeam == 0) teamNod.incrementDeaths()
        if (victimTeam == 1) teamGdi.incrementDeaths()

        // Resolve player network IDs for the kill event (PlayerKill uses player IDs, not host IDs)
        val killerPlayerId = if (killerId >= 0) god.playerNetIds[killerId] ?: killerId else 0
        val victimPlayerId = god.playerNetIds[victimId] ?: victimId
        val event = PlayerKill(killerId = killerPlayerId, victimId = victimPlayerId)

        for (clientId in god.playerInGame) {
            val clientHost = connectionManager.getHost(clientId) ?: continue
            sendGameNetObj(clientHost) { bs ->
                NetworkObjectPacketWriter.writeCreation(bs, event, NetworkObjectManager.getNewDynamicId())
            }
        }
        println("[GAME] broadcastPlayerKill: killer=$killerId victim=$victimId")
    }

    // Sends a PLAYER BIT_RARE update (no networkClassId — not a creation packet).
    // C++: cPlayer::Export_Rare + Export_Occasional + Export_Frequent.
    // dirtyBits=0x07 = BIT_RARE|BIT_OCCASIONAL|BIT_FREQUENT (not BIT_CREATION).
    private fun sendPlayerRareUpdate(host: RemoteHost, rhostId: Int) {
        val netId = god.playerNetIds[rhostId] ?: run {
            println("[GAME] sendPlayerRareUpdate: no playerNetId for rhostId=$rhostId, skipping")
            return
        }
        val player = god.playersByHost[rhostId] ?: run {
            println("[GAME] sendPlayerRareUpdate: no player object for rhostId=$rhostId, skipping")
            return
        }
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeRareUpdate(bs, player, netId) }
        println("[GAME] sent PLAYER BIT_RARE to host $rhostId: team=${if (player.team == 0) "NOD" else "GDI"} inGame=${player.isInGame} netId=$netId")
    }

    // Handles an UNRELIABLE game packet that is NOT a creation (no BIT_CREATION).
    // These are CClientControl frequent updates: the client sends its soldier's position/state
    // every tick. C++: clientcontrol.cpp:114-134 reads SmartObjId → Import_Control_Cs + Import_State_Cs.
    //
    // On-foot wire format (after header consumed by handleGamePacket):
    //   smartObjId(32), in_vehicle(bool)=false, has_weapon(bool), [weaponDefId(32)+rounds(32)],
    //   position(3×BITPACK_WORLD), humanState, humanSubState, isSpecialDamage(bool),
    //   onHostBone(bool), targeting(3×BITPACK_WORLD), continuousBoolBits, 4×analog
    //
    // In-vehicle wire format:
    //   smartObjId(32), in_vehicle(bool)=true,
    //   onHostBone(bool), targeting(3×BITPACK_WORLD), continuousBoolBits, 4×analog
    private fun handleFrequentUpdate(snap: BitStream, rhostId: Int) {
        val smartObjId = try { snap.getInt() } catch (e: Exception) { return }
        if (smartObjId == -1) return  // no controlled object

        val soldier = god.soldiersByHost[rhostId] ?: return
        if (soldier.networkId != smartObjId) {
            // SmartObjId doesn't match — client may not be controlling their soldier yet
            return
        }

        try {
            val inVehicle = snap.getBool()

            if (inVehicle) {
                // ---- In-vehicle path ----
                // C++: SoldierGameObj::Import_Frequent when in_vehicle=true:
                //   calls SmartGameObj::Import_Frequent (on_host_bone + targeting + control)
                snap.getBool()  // on_host_bone (discard)
                val tx = snap.getFloat(BITPACK_WORLD_POSITION_X)
                val ty = snap.getFloat(BITPACK_WORLD_POSITION_Y)
                val tz = snap.getFloat(BITPACK_WORLD_POSITION_Z)
                val contBits = snap.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toInt() and 0xFF
                val fwd  = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_FORWARD
                val left = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_LEFT
                val up   = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_UP
                val turn = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_TURN_LEFT

                soldier.targeting          = Vector3(tx, ty, tz)
                soldier.continuousBoolBits = contBits
                soldier.analogMoveForward  = fwd
                soldier.analogMoveLeft     = left
                soldier.analogMoveUp       = up
                soldier.analogTurnLeft     = turn
                soldier.inVehicle          = true
                // Player can detonate remote C4 while riding in a vehicle
                soldier.detonateC4 = (contBits and 2) != 0 && isC4Weapon(soldier.currentWeaponDefId)

                // Entry detection: client reports in_vehicle=true but we haven't recorded entry yet
                if (rhostId !in god.playerVehicles) {
                    val nearest = god.vehiclesByNetId.values
                        .filter { it.seatOccupantIds.getOrElse(0) { -1 } == -1 }
                        .minByOrNull { v ->
                            val dx = v.position.x - soldier.position.x
                            val dy = v.position.y - soldier.position.y
                            val dz = v.position.z - soldier.position.z
                            dx * dx + dy * dy + dz * dz
                        }
                    if (nearest != null) {
                        val dx = nearest.position.x - soldier.position.x
                        val dy = nearest.position.y - soldier.position.y
                        val dz = nearest.position.z - soldier.position.z
                        val distSq = dx * dx + dy * dy + dz * dz
                        if (distSq <= 100f) {  // 10 m radius
                            god.enterVehicle(rhostId, nearest)
                        }
                    }
                }

                // Forward soldier in-vehicle state to other clients
                for (otherId in god.playerInGame) {
                    if (otherId != rhostId) {
                        soldier.setObjectDirtyBit(otherId, NetworkObject.BIT_FREQUENT, true)
                    }
                }
                return
            }

            // ---- Exit detection: was in a vehicle, now on foot ----
            if (rhostId in god.playerVehicles) {
                god.exitVehicle(rhostId)
            }

            // ---- On-foot path ----
            val hasWeapon = snap.getBool()
            if (hasWeapon) {
                val weaponDefId = snap.getInt()
                snap.getInt()  // totalRounds
                soldier.currentWeaponDefId = weaponDefId
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
            val contBits = snap.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toInt() and 0xFF
            val fwd  = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_FORWARD
            val left = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_LEFT
            val up   = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_MOVE_UP
            val turn = snap.getFloat(BITPACK_ANALOG_VALUES)  // ANALOG_TURN_LEFT

            // Update soldier's authoritative state on the server
            soldier.position           = Vector3(x, y, z)
            soldier.targeting          = Vector3(tx, ty, tz)
            soldier.continuousBoolBits = contBits
            soldier.analogMoveForward  = fwd
            soldier.analogMoveLeft     = left
            soldier.analogMoveUp       = up
            soldier.analogTurnLeft     = turn

            // C4 detonation: bit 1 = BOOLEAN_WEAPON_FIRE_SECONDARY (alt-fire / remote trigger)
            val weaponFirePrimary   = (contBits and 1) != 0
            val weaponFireSecondary = (contBits and 2) != 0
            soldier.detonateC4 = weaponFireSecondary && isC4Weapon(soldier.currentWeaponDefId)
            if (weaponFirePrimary && isC4Weapon(soldier.currentWeaponDefId)) {
                god.createC4(rhostId, soldier, System.currentTimeMillis())
            }
            if (weaponFirePrimary && isBeaconWeapon(soldier.currentWeaponDefId)) {
                val ammoDef = getAmmoDefForWeapon(soldier.currentWeaponDefId)
                if (ammoDef != null) {
                    god.createBeacon(rhostId, soldier, ammoDef, System.currentTimeMillis())
                }
            }

            // Mark BIT_FREQUENT dirty for all other in-game clients so the replication tick
            // will forward the position update unreliably
            for (otherId in god.playerInGame) {
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
    // C++ server sends timeRemainingSeconds and hostedGameNumber.
    private fun sendGameDataUpdateEvent(host: RemoteHost) {
        val timeRemaining = gameState.timeRemainingSeconds.toInt()
        val event = GameDataUpdateEvent(
            timeRemainingSeconds = timeRemaining,
            hostedGameNumber = hostedGameNumber,
        )
        sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, event, NetworkObjectManager.getNewDynamicId()) }
    }

    // Measures actual server tick rate and updates serverFps once per second.
    private fun updateFps(nowMs: Long) {
        fpsFrameCount++
        val interval = nowMs - lastFpsUpdateMs
        if (interval > 1000L) {
            val measuredFps = (fpsFrameCount * 1000f / interval + 0.5f).toInt()
            lastFpsUpdateMs = nowMs
            fpsFrameCount = 0
            serverFps.setFps(measuredFps)
        }
    }

    // Builds a RELIABLE packet, enqueues it in the host's reliable channel, and sends it immediately.
    // The packet ID is pre-assigned from host.reliable.nextSendId so the wire bytes are consistent.
    internal fun sendGameNetObj(host: RemoteHost, writePayload: (BitStream) -> Unit) {
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
        pendingOutbox.getOrPut(host.id) { mutableListOf() }.add(host.address to wireData)
    }

    // ---- Game-over / intermission ----

    private fun handleGameOver(winType: Int) {
        println("[GAME] game over winType=$winType")
        // Determine winner by team score
        val nodScore = teamNod.score
        val gdiScore = teamGdi.score
        val (winner, loser) = when {
            winType == 2 -> {
                // Base destruction: the surviving team wins
                // For now use score as tiebreak
                if (nodScore >= gdiScore) Pair(0, 1) else Pair(1, 0)
            }
            nodScore > gdiScore -> Pair(0, 1)
            gdiScore > nodScore -> Pair(1, 0)
            else -> Pair(-1, -1)  // draw
        }

        // Determine MVP (highest-scoring player)
        val mvp = god.playersByHost.values.maxByOrNull { it.score }
        val mvpName = mvp?.name ?: ""

        // Send WinEvent to all in-game clients
        val winEvent = WinEvent(
            winner = winner,
            loser = loser,
            hostedGameNumber = hostedGameNumber,
            isMapCycleOver = mapRotation.isMapCycleOver,
            winType = winType,
            gameDuration = gameState.gameDurationSeconds.toInt(),
            mvpName = mvpName,
            mvpCount = if (mvp != null) mvp.kills else 0,
            modNameCrc = gameData.modNameCrc,
            mapNameCrc = gameData.mapNameCrc,
        )
        for (clientId in god.playerInGame) {
            val clientHost = connectionManager.getHost(clientId) ?: continue
            sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, winEvent, NetworkObjectManager.getNewDynamicId()) }
        }

        // Start intermission
        gameState.startIntermission()
        println("[GAME] intermission started (${config.intermissionTimeSeconds}s)")
    }

    private fun handleCoreRestart() {
        println("[GAME] core restart — resetting scores and game state")
        hostedGameNumber++
        teamNod.reset()
        teamGdi.reset()
        gameState.reset()

        // Reset all player scores
        for (player in god.playersByHost.values) {
            player.resetStats()
        }

        // Delete all soldiers (they will be re-spawned by god.think() on next tick)
        for (rhostId in god.playerInGame.toList()) {
            god.deleteSoldier(rhostId)
        }

        // Reset buildings and base controllers for new round
        baseControllerNod?.reset()
        baseControllerGdi?.reset()
        for (building in gameObjManager.getBuildingList()) {
            building.resetToFull()
        }

        println("[GAME] core restart complete — hostedGameNumber=$hostedGameNumber")
    }

    private fun initializeLevel() {
        initEncoders()  // must run before building encoders

        loadedLevel?.also { level ->
            if (level.dynamicData.spawners.isNotEmpty()) {
                spawnManager = SpawnManager(level).also { sm ->
                    sm.onCreatePowerUp = { position, def -> createPowerUp(position, def) }
                }
            }
        }

        // Force gameContext initialisation so baseControllers array is ready before buildings use it.
        val ctx = gameContext
        loadedLevel?.also { level ->
            val loadedBuildings = level.dynamicData.gameObjects.filterIsInstance<LoadedBuildingGameObj>()
            if (loadedBuildings.isNotEmpty()) {
                val controllerNod = BaseControllerClass(playerType = 0)
                val controllerGdi = BaseControllerClass(playerType = 1)
                NetworkObjectManager.registerObject(controllerNod, NET_ID_BASE_CONTROLLER_NOD)
                NetworkObjectManager.registerObject(controllerGdi, NET_ID_BASE_CONTROLLER_GDI)
                controllerNod.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
                controllerGdi.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
                baseControllerNod = controllerNod
                baseControllerGdi = controllerGdi
                ctx.baseControllers[0] = controllerNod
                ctx.baseControllers[1] = controllerGdi

                println("[BUILDING] found ${loadedBuildings.size} buildings in LDD")
                for (lb in loadedBuildings) {
                    val building = createBuilding(lb) ?: continue
                    NetworkObjectManager.registerObject(building, lb.networkId)
                    building.gameContext = ctx
                    val controller = when (lb.playerType) {
                        0 -> { controllerNod.addBuilding(building); controllerNod }
                        1 -> { controllerGdi.addBuilding(building); controllerGdi }
                        else -> null
                    }
                    if (controller != null) building.cncInitialize(controller)
                    // Wire vehicle factory delivery callback so purchased vehicles spawn in-world
                    if (building is VehicleFactoryGameObj) {
                        val baseCtrl = when (lb.playerType) {
                            0 -> controllerNod
                            1 -> controllerGdi
                            else -> null
                        }
                        building.onVehicleReady = { defId, buyerRhostId ->
                            if (buyerRhostId == ccr.server.net.BaseControllerClass.HARVESTER_BUYER_ID) {
                                // Harvester delivery — find the refinery that requested it
                                val refinery = baseCtrl?.getBuildings()
                                    ?.filterIsInstance<ccr.server.net.RefineryGameObj>()
                                    ?.firstOrNull { !it.isDestroyed && it.harvesterVehicle == null && it.harvesterDefId == defId }
                                if (refinery != null) {
                                    val vehicle = god.createHarvester(building.playerType, defId, refinery.position)
                                    if (vehicle != null) refinery.harvesterVehicle = vehicle
                                }
                            } else {
                                god.createVehicle(buyerRhostId, defId, building.position)
                            }
                        }
                    }
                    gameObjManager.add(building)
                    gameObjManager.addBuilding(building)
                    println("[BUILDING] registered ${building::class.simpleName} networkId=${lb.networkId} defId=${lb.definitionId} playerType=${lb.playerType}")
                }
                println("[BUILDING] registered ${loadedBuildings.size} buildings, 2 base controllers")
            }

            // Instantiate pre-placed vehicles from LDD (harvesters, decorative vehicles, etc.)
            // C++: cGod loads all VehicleGameObj save-data entries during level init
            val loadedVehicles = level.dynamicData.gameObjects.filterIsInstance<LoadedVehicleGameObj>()
            for (lv in loadedVehicles) {
                god.createLevelVehicle(lv)
            }
            println("[LEVEL] ${loadedVehicles.size} level vehicles instantiated")
        }

        // Register doors from LSD static objects
        loadedLevel?.also { level ->
            val doors = level.staticData.staticObjects.filter { it.factoryChunkId == ChunkIds.PHYSICS_CHUNKID_DOORPHYS }
            if (doors.isNotEmpty()) {
                println("[DOOR] found ${doors.size} doors in LSD")
                for (obj in doors) {
                    val def = level.definitions.findById(obj.definitionId.toUInt()) as? DoorPhysDefClass ?: continue
                    val zone1 = def.triggerZone1?.let { OBBox.fromFloatArray(it) }
                    val zone2 = def.triggerZone2?.let { OBBox.fromFloatArray(it) }
                    val doorPhys = DoorPhysClass(
                        definitionId = obj.definitionId,
                        closeDelay = def.closeDelay,
                        triggerZone1 = zone1,
                        triggerZone2 = zone2,
                        lockCode = def.lockCode,
                        doorOpensForVehicles = def.doorOpensForVehicles,
                    )
                    doorPhys.transform = obj.transform.let { tm ->
                        val e = tm.elements
                        ccr.math.Matrix3D(
                            m00 = e[0], m01 = e[1], m02 = e[2], m03 = e[3],
                            m10 = e[4], m11 = e[5], m12 = e[6], m13 = e[7],
                            m20 = e[8], m21 = e[9], m22 = e[10], m23 = e[11],
                        )
                    }
                    val doorNet = DoorNetworkObject(doorPhys)
                    NetworkObjectManager.registerObject(doorNet, obj.instanceId)
                    doorNet.setObjectDirtyBit(NetworkObject.BIT_RARE, true)
                    doorObjects.add(Pair(doorPhys, doorNet))
                    println("[DOOR] registered door instanceId=${obj.instanceId} defId=${obj.definitionId} name=${def.name}")
                }
            }
        }
    }

    private fun unloadLevel() {
        println("[SERVER] unloading level '$currentMapName'")

        // Unregister door network objects
        for ((_, doorNet) in doorObjects) {
            NetworkObjectManager.unregisterObject(doorNet)
        }
        doorObjects.clear()

        // Unregister buildings from NetworkObjectManager
        for (building in gameObjManager.getBuildingList().toList()) {
            NetworkObjectManager.unregisterObject(building)
        }

        // Unregister base controllers
        baseControllerNod?.let { NetworkObjectManager.unregisterObject(it) }
        baseControllerGdi?.let { NetworkObjectManager.unregisterObject(it) }
        baseControllerNod = null
        baseControllerGdi = null
        gameContext.baseControllers[0] = null
        gameContext.baseControllers[1] = null

        // Unregister vehicles from NetworkObjectManager and clear God's vehicle tracking
        for (vehicle in god.vehiclesByNetId.values.toList()) {
            NetworkObjectManager.unregisterObject(vehicle)
        }
        god.vehiclesByNetId.clear()

        // Clear all ticking game objects (buildings, any remaining C4/beacons)
        gameObjManager.destroyAll()

        // Clear level-specific state
        spawnManager = null
        loadedLevel = null

        // Reset weapon/soldier def IDs (repopulated by loadLevel)
        nodSoldierDefId = config.nodSoldierDefId
        gdiSoldierDefId = config.gdiSoldierDefId
        pistolWeaponDefId = 0
        timedC4WeaponDefId = 0
        tossedC4DefId = 0
        beaconWeaponDefId = 0

        println("[SERVER] level unloaded")
    }

    private suspend fun handleMapRotation(nextMapName: String) {
        println("[GAME] map rotation → '$nextMapName'")
        hostedGameNumber++
        teamNod.reset()
        teamGdi.reset()
        gameState.reset()

        // Reset all player scores
        for (player in god.playersByHost.values) {
            player.resetStats()
        }

        // Delete all soldiers (re-spawned by god.think() after level init)
        for (rhostId in god.playerInGame.toList()) {
            god.deleteSoldier(rhostId)
        }

        // Unload the current level
        unloadLevel()

        // Update runtime map name and gameData CRC
        currentMapName = nextMapName
        gameData.mapNameCrc = if (nextMapName.isEmpty()) 0 else crcStringi(nextMapName)

        // Load and initialize the new level
        loadLevel(nextMapName)
        initializeLevel()

        println("[GAME] map rotation complete — now on '$nextMapName', hostedGameNumber=$hostedGameNumber")
    }

    private fun createBuilding(lb: LoadedBuildingGameObj): BuildingGameObj? {
        val pos = Vector3(lb.transform.position.x, lb.transform.position.y, lb.transform.position.z)
        val sphereCenter = Vector3(lb.collectionSphere.center.x, lb.collectionSphere.center.y, lb.collectionSphere.center.z)
        val radius = lb.collectionSphere.radius

        if (!ChunkIds.isBuilding(lb.factoryChunkId)) return null

        val health = lb.defense.healthMax.takeIf { it > 0f } ?: 5000f
        val shieldType = lb.defense.skinSaveId
        val mctSkinSaveId = (loadedLevel?.definitions?.findById(lb.definitionId.toUInt())
            as? BuildingGameObjDef)?.mctSkin ?: 0

        val building = when (lb.factoryChunkId) {
            ChunkIds.GAMEOBJ_BUILDING_POWERPLANT ->
                PowerPlantGameObj(lb.definitionId, pos, sphereCenter, radius,
                    health = health, shieldType = shieldType, isPowerOn = lb.isPowerOn, playerType = lb.playerType)

            ChunkIds.GAMEOBJ_BUILDING_REFINERY -> {
                val refinery = RefineryGameObj(lb.definitionId, pos, sphereCenter, radius,
                    health = health, shieldType = shieldType, playerType = lb.playerType)
                val refDef = loadedLevel?.definitions?.findById(lb.definitionId.toUInt())
                    as? RefineryGameObjDef
                refinery.harvesterDefId = refDef?.harvesterDefId ?: 0
                refinery
            }

            ChunkIds.GAMEOBJ_BUILDING_SOLDIERFACTORY ->
                SoldierFactoryGameObj(lb.definitionId, pos, sphereCenter, radius,
                    health = health, shieldType = shieldType, playerType = lb.playerType)

            ChunkIds.GAMEOBJ_BUILDING_WARFACTORY ->
                WarFactoryGameObj(lb.definitionId, pos, sphereCenter, radius,
                    health = health, shieldType = shieldType, playerType = lb.playerType)

            ChunkIds.GAMEOBJ_BUILDING_AIRSTRIP,
            ChunkIds.GAMEOBJ_BUILDING_VEHICLEFACTORY ->
                VehicleFactoryGameObj(lb.definitionId, pos, sphereCenter, radius,
                    health = health, shieldType = shieldType, playerType = lb.playerType)

            ChunkIds.GAMEOBJ_BUILDING_COMCENTER ->
                ComCenterGameObj(lb.definitionId, pos, sphereCenter, radius,
                    health = health, shieldType = shieldType, playerType = lb.playerType)

            else -> BuildingGameObj(lb.definitionId, pos, sphereCenter, radius,
                health = health, shieldType = shieldType, playerType = lb.playerType)
        }
        building.mctSkinSaveId = mctSkinSaveId
        return building
    }

    companion object {
        // C++ networkobjectmgr.h ID ranges:
        //   DYNAMIC: 1,500,000,000 - 2,100,000,000 (server-created objects: events, soldiers, etc.)
        //   STATIC:  2,100,000,001 - 2,110,000,000 (level static objects incl. teams)
        //   CLIENT:  2,110,000,001 - 2,122,800,001 (100K per client)

        // Stable network IDs for server-created singleton objects in the STATIC range.
        // C++: cTeam uses NETID_STATIC_OBJECT_MIN + offset; these values match C++ server logs exactly.
        internal const val NET_ID_NOD_TEAM           = 2_100_000_004
        internal const val NET_ID_GDI_TEAM           = 2_100_000_005
        internal const val NET_ID_SERVER_FPS         = 2_100_000_006

        // BaseControllerClass static IDs (C++: CNCGameMgr uses 2100000002 / 2100000003)
        internal const val NET_ID_BASE_CONTROLLER_NOD = 2_100_000_002
        internal const val NET_ID_BASE_CONTROLLER_GDI = 2_100_000_003

        // WeatherMgr and BackgroundMgr static IDs
        internal const val NET_ID_SERVER_WEATHER    = 2_100_000_007
        internal const val NET_ID_SERVER_BACKGROUND = 2_100_000_008
    }

    // ---- Encoder setup ----

    // Loads level data from MIX files using LevelLoader. Populates loadedLevel, and extracts
    // soldier/weapon definition IDs for spawning. Falls back to the legacy loadDefinitions() path
    // if the map MIX is not available.
    private suspend fun loadLevel(mapName: String = currentMapName) {
        val dataDir = if (config.dataPath.isNotEmpty()) File(config.dataPath) else File(".")

        // Find always MIX (Renegade loads Always2.dat, Always.dbs, Always.dat in init.cpp)
        val alwaysMix = listOf("always.dbs", "always2.dat", "always.dat").firstNotNullOfOrNull { fileName ->
            val file = File(dataDir, fileName)
            if (!file.exists()) return@firstNotNullOfOrNull null
            try {
                MixReader(file.readBytes()).also {
                    println("[SERVER] opened $fileName (${it.fileCount()} files in archive)")
                }
            } catch (e: Exception) {
                println("[SERVER] $fileName: ${e.message}")
                null
            }
        }

        if (mapName.isEmpty()) {
            println("[SERVER] no MapName configured, skipping level load")
            loadDefinitions()  // fall back to legacy path for definitions only
            return
        }

        val baseName = if (mapName.endsWith(".mix", ignoreCase = true))
            mapName.dropLast(4) else mapName
        val mixFile = File(dataDir, "$baseName.mix")
        if (!mixFile.exists()) {
            println("[SERVER] map MIX not found: ${mixFile.absolutePath}, falling back to legacy loading")
            loadDefinitions()
            return
        }

        val mapMix = MixReader(mixFile.readBytes())
        println("[SERVER] opened $baseName.mix (${mapMix.fileCount()} files in archive)")

        alwaysMix?.readFile("armor.ini")?.also { ArmorWarheadManager.init(it) }

        val level = LevelLoader(alwaysMix, mapMix, baseName).load()
        loadedLevel = level

        physicsScene = PhysicsSceneBuilder.build(level.staticData.staticObjects, mapMix, alwaysMix)

        // Extract soldier/weapon definition IDs from the loaded registry
        val defs = level.definitions
        println("[SERVER] loaded ${defs.size} definitions via LevelLoader")

        defs.findByName("CnC_Nod_Minigunner_0")?.let {
            nodSoldierDefId = it.id.toInt()
            println("[SERVER] NOD soldier: ${it.name} defId=0x${nodSoldierDefId.toUInt().toString(16)}")
        }
        defs.findByName("CnC_GDI_MiniGunner_0")?.let {
            gdiSoldierDefId = it.id.toInt()
            println("[SERVER] GDI soldier: ${it.name} defId=0x${gdiSoldierDefId.toUInt().toString(16)}")
        }
        defs.findByName("Weapon_Pistol_Player")?.let {
            pistolWeaponDefId = it.id.toInt()
            println("[SERVER] Using pistol: ${it.name} defId=0x${pistolWeaponDefId.toUInt().toString(16)}")
        }
        defs.all().filterIsInstance<WeaponDefinitionClass>().find { it.style == 0 }?.let {
            timedC4WeaponDefId = it.id.toInt()
            println("[SERVER] C4 weapon: ${it.name} defId=0x${timedC4WeaponDefId.toUInt().toString(16)}")
        }
        defs.findByName("Tossed C4")?.let {
            tossedC4DefId = it.id.toInt()
            println("[SERVER] Tossed C4 preset: ${it.name} defId=0x${tossedC4DefId.toUInt().toString(16)}")
        }
        // Beacon weapon: find the weapon whose primary ammo has beaconDefId != 0
        defs.all().filterIsInstance<AmmoDefinitionClass>().find { it.beaconDefId != 0 }?.let { ammoDef ->
            defs.all().filterIsInstance<WeaponDefinitionClass>()
                .find { it.primaryAmmoDefID == ammoDef.id.toInt() }
                ?.let { weaponDef ->
                    beaconWeaponDefId = weaponDef.id.toInt()
                    println("[SERVER] beacon weapon: ${weaponDef.name} defId=0x${beaconWeaponDefId.toUInt().toString(16)}")
                }
        }

        // Restore nextDynamicId so dynamically created objects (soldiers, vehicles)
        // get IDs that don't collide with pre-placed LDD objects.
        val nextDynId = level.dynamicData.nextDynamicNetworkId
        if (nextDynId > 0) {
            NetworkObjectManager.setNewDynamicId(nextDynId)
            println("[SERVER] restored nextDynamicId=$nextDynId from LDD")
        }

        val spawnerCount = level.dynamicData.spawners.size
        val objectCount = level.dynamicData.gameObjects.size
        println("[SERVER] level '$baseName': ${spawnerCount} spawners, ${objectCount} game objects, " +
            "extents=${level.worldExtents ?: "none"}")
    }

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

        // Also load armor multiplier table if not already loaded via loadLevel()
        candidates.firstNotNullOfOrNull { fileName ->
            val file = File(dataDir, fileName)
            if (!file.exists()) return@firstNotNullOfOrNull null
            try { MixReader(file.readBytes()).readFile("armor.ini") } catch (e: Exception) { null }
        }?.also { ArmorWarheadManager.init(it) }

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
        weaponDefs.forEach { println("[SERVER]   ${it.name} id=0x${it.id.toString(16)} chunkId=${it.chunkId}") }

        val pistolDef = definitions.find { it.name.equals("Weapon_Pistol_Player", ignoreCase = true) }
        if (pistolDef != null) {
            pistolWeaponDefId = pistolDef.id.toInt()
            println("[SERVER] Using pistol: ${pistolDef.name} defId=0x${pistolWeaponDefId.toUInt().toString(16)}")
        } else {
            println("[SERVER] No pistol weapon found in objects.ddb")
        }
        definitions.filterIsInstance<WeaponDefinitionClass>().find { it.style == 0 }?.let {
            timedC4WeaponDefId = it.id.toInt()
            println("[SERVER] C4 weapon: ${it.name} defId=0x${timedC4WeaponDefId.toUInt().toString(16)}")
        }
        definitions.find { it.name.equals("Tossed C4", ignoreCase = true) }?.let {
            tossedC4DefId = it.id.toInt()
            println("[SERVER] Tossed C4 preset: ${it.name} defId=0x${tossedC4DefId.toUInt().toString(16)}")
        }
        // Beacon weapon: find the weapon whose primary ammo has beaconDefId != 0
        definitions.filterIsInstance<AmmoDefinitionClass>().find { it.beaconDefId != 0 }?.let { ammoDef ->
            definitions.filterIsInstance<WeaponDefinitionClass>()
                .find { it.primaryAmmoDefID == ammoDef.id.toInt() }
                ?.let { weaponDef ->
                    beaconWeaponDefId = weaponDef.id.toInt()
                    println("[SERVER] beacon weapon: ${weaponDef.name} defId=0x${beaconWeaponDefId.toUInt().toString(16)}")
                }
        }
    }

    // ---- C4 definition lookup helpers ----

    // Returns true if the given weapon definition ID corresponds to a C4 weapon (style == 0).
    fun isC4Weapon(weaponDefId: Int): Boolean {
        val def = loadedLevel?.definitions?.findById(weaponDefId.toUInt()) ?: return false
        return (def as? WeaponDefinitionClass)?.style == 0
    }

    // Returns true if the weapon's primary ammo has a non-zero beaconDefId.
    fun isBeaconWeapon(weaponDefId: Int): Boolean {
        val ammoDef = getAmmoDefForWeapon(weaponDefId) ?: return false
        return ammoDef.beaconDefId != 0
    }

    // Returns the AmmoDefinitionClass for the primary ammo of the given weapon, or null.
    fun getAmmoDefForWeapon(weaponDefId: Int): AmmoDefinitionClass? {
        val wDef = loadedLevel?.definitions?.findById(weaponDefId.toUInt()) as? WeaponDefinitionClass ?: return null
        return loadedLevel?.definitions?.findById(wDef.primaryAmmoDefID.toUInt()) as? AmmoDefinitionClass
    }

    /**
     * Creates a PowerUpGameObj at the given position and registers it with the server.
     * Called by SpawnManager when a powerup spawner's timer fires.
     * C++: SpawnerClass::Spawn_Object() creates the object and calls Add_Network_Object().
     *
     * @param position  world position from the spawner's transform
     * @param def       the PowerUpGameObjDef describing what to grant
     */
    internal fun createPowerUp(position: Vector3, def: PowerUpGameObjDef) {
        val powerUp = PowerUpGameObj(
            definitionId  = def.id.toInt(),
            position      = position,
            modelName     = "",   // C++: model comes from physics def; stub for Phase 6
        )
        powerUp.powerUpDef = def
        powerUp.serverRef  = this

        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(powerUp, netId)
        gameObjManager.add(powerUp)

        println("[POWERUP] spawned '${def.name}' netId=$netId at (${position.x}, ${position.y}, ${position.z})")
    }

    private fun initEncoders() {
        val extents = loadedLevel?.worldExtents ?: loadWorldExtents()
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
        EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE,     0.0, 19.0,  1.0)    // humanstate.h: LOCKED_ANIMATION=19
        EncoderRegistry.setPrecision(BITPACK_HUMAN_SUB_STATE, 0.0, 511.0, 1.0)    // humanstate.cpp: (1<<9)-1=511
        EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_CS, 8)                 // control.cpp: CONTROL_TURN_RIGHT+1=8
        EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_SC, 6)                 // control.cpp: CONTROL_MOVE_DOWN+1=6
        EncoderRegistry.setPrecision(BITPACK_BUILDING_RADIUS, 0.0, 50.0, 0.1)     // building.cpp
        EncoderRegistry.setPrecision(BITPACK_BUILDING_STATE, -1.0, 10.0, 1.0)     // building.cpp
        // Vehicle encoders — vehicle.cpp: VehicleGameObj::Set_Precision()
        EncoderRegistry.setPrecision(BITPACK_VEHICLE_VELOCITY,         -90.0,  90.0, 0.01)
        EncoderRegistry.setPrecision(BITPACK_VEHICLE_ANGULAR_VELOCITY, -20.0,  20.0, 0.01)
        EncoderRegistry.setPrecision(BITPACK_VEHICLE_QUATERNION,        -1.0,   1.0, 0.0005)
        EncoderRegistry.setPrecision(BITPACK_VEHICLE_LOCK_TIMER,         0.0,  16.0, 0.25)
        // Door encoder — doors.cpp: DoorPhysClass::Set_Precision(), STATE_MAX=5
        EncoderRegistry.setPrecision(BITPACK_DOOR_STATE, 0.0, 5.0, 1.0)
        // Elevator encoders — elevator.cpp: ElevatorPhysClass::Set_Precision()
        EncoderRegistry.setPrecision(BITPACK_ELEVATOR_STATE,             0.0, 4.0, 1.0) // STATE_MAX=4
        EncoderRegistry.setPrecision(BITPACK_ELEVATOR_TOP_DOOR_STATE,    0.0, 3.0, 1.0) // DOOR_STATE_MAX=3
        EncoderRegistry.setPrecision(BITPACK_ELEVATOR_BOTTOM_DOOR_STATE, 0.0, 3.0, 1.0) // DOOR_STATE_MAX=3
    }

    // Reads the map .mix file and extracts world extents from the embedded .lsd file.
    // Returns null only if no MapName is configured. Throws if the map file cannot be loaded.
    private fun loadWorldExtents(mapName: String = currentMapName): WorldExtents? {
        if (mapName.isEmpty()) {
            println("[SERVER] no MapName configured, skipping LSD load")
            return null
        }
        // Strip .mix extension if already present (config may include it or not)
        val baseName = if (mapName.endsWith(".mix", ignoreCase = true))
            mapName.dropLast(4) else mapName
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
                  help      - show this message
                  status    - show server status
                  players   - list connected players
                  score     - show team and player scores
                  gameover  - trigger game over
                  kick <id> - kick a player by host ID
            """.trimIndent()

            "status" -> """
                Server: ${config.serverName}
                Map: $currentMapName
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

            "gameover" -> {
                gameState.manualGameOver = true
                "Game over triggered."
            }

            "kick" -> {
                val targetId = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (targetId == null) "Usage: kick <playerId>"
                else {
                    val targetHost = connectionManager.getHost(targetId)
                    if (targetHost == null) "Player $targetId not found."
                    else {
                        val eviction = EvictionEvent(evictionCode = 0)
                        sendGameNetObj(targetHost) { bs ->
                            NetworkObjectPacketWriter.writeCreation(bs, eviction, NetworkObjectManager.getNewDynamicId())
                        }
                        flowControllers.remove(targetId)
                        god.removePlayer(targetId)
                        "Kicked player $targetId."
                    }
                }
            }

            "score" -> {
                buildString {
                    appendLine("NOD score=${teamNod.score} kills=${teamNod.kills}")
                    appendLine("GDI score=${teamGdi.score} kills=${teamGdi.kills}")
                    for ((id, player) in god.playersByHost) {
                        appendLine("  [$id] ${player.name} score=${player.score} kills=${player.kills} money=${player.money}")
                    }
                }.trimEnd()
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
