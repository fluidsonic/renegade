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
import ccr.server.net.ServerFps
import ccr.server.net.SoldierGameObj
import ccr.server.net.Team
import ccr.server.net.WinEvent
import ccr.server.net.BackgroundMgr
import ccr.server.net.NetEvent
import ccr.server.net.NetworkObjectFactories
import ccr.server.net.WeatherMgr
import ccr.server.net.ClientControl
import ccr.server.net.ClientFps
import ccr.server.combat.ArmorWarheadManager
import ccr.server.net.PlayerKill
import ccr.server.net.ResetWinsEvent
import ccr.net.flow.BandwidthBudget
import ccr.net.flow.FlowController
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectFactoryManager
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
import ccr.server.defs.PhysDefClass
import ccr.server.defs.WeaponDefinitionClass
import ccr.server.defs.DoorPhysDefClass
import ccr.server.defs.PowerUpGameObjDef
import ccr.server.defs.RefineryGameObjDef
import ccr.math.OBBox
import ccr.math.Matrix3D as MathMatrix3D
import ccr.physics.static.DoorPhysClass
import ccr.server.net.DoorNetworkObject
import ccr.server.net.PowerUpGameObj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
class Network(internal val config: ServerConfig) {

    private val gameThread = newSingleThreadContext("game-thread")
    private val transport = UdpTransport(config.gamePort)
    internal val connectionManager = ConnectionManager(config.maxPlayers)
    private val localIp = detectLocalIp()
    private val gameData = GameData(config, localIp)

    private val rconServer = RconServer(
        port = config.rconPort,
        password = config.rconPassword,
        remoteAdminAllowed = config.remoteAdminAllowed,
        welcomeMessage = showWelcomeMessage(),
        commandHandler = ::handleRconCommand,
    )

    private val lanResponder = LanBroadcastResponder(config, connectionManager) { gameData.mapNameCrc }

    // Milliseconds per network tick
    private val tickIntervalMs: Long = 1000L / config.netUpdateRate.coerceAtLeast(1)

    // Nicknames for players currently in acceptance: populated in applicationAcceptanceHandler,
    // consumed in BIOEVENT handler so that we can include the name in the Player network object.
    internal val playerNicknames = mutableMapOf<java.net.InetSocketAddress, String>()

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
    // GameObjManager is a Kotlin object (singleton); expose as a property for callers that
    // hold a Network reference (God.kt, ExplosionHelper.kt, etc.).
    internal val gameObjManager: GameObjManager get() = GameObjManager

    // GameContext — session-scoped container for shared game state (lazy to allow gameState init first).
    internal val gameContext: GameContext by lazy {
        GameContext(config = config, gameState = gameState)
    }

    // Periodic GameDataUpdateEvent resend (once per second) to keep clients' timer in sync.
    private var lastGameDataUpdateMs: Long = 0L

    // Score sort timer (once per second, C++: End_Game_Test sort)
    private var lastScoreSortMs: Long = 0L

    // Per-tick outbox: packets buffered during tick, flushed together at end of tick
    private val pendingOutbox = mutableMapOf<Int, MutableList<Pair<InetSocketAddress, ByteArray>>>()
    private val bytesSentThisTick = mutableMapOf<Int, Int>()

    // C++: cConnection bandwidth management
    private val bandwidthBudget = BandwidthBudget(if (config.bandwidthBps > 0) config.bandwidthBps else 1_500_000)
    // Per-host flow controllers (C++: Adjust_Flow_If_Necessary in rhost.cpp)
    internal val flowControllers = mutableMapOf<Int, FlowController>()

    // Tracks rhostIds of clients currently in a loading state (LOADINGEVENT).
    internal val loadingHosts = mutableSetOf<Int>()

    // C++: cServerFps singleton — informs clients of server framerate.
    private val serverFps = ServerFps()

    // FPS tracking state
    private var lastFpsUpdateMs: Long = 0L
    private var fpsFrameCount: Int = 0
    private var frameDeltaSeconds: Float = 0f

    // Client FPS tracking: rhostId → last reported fps value.
    internal val clientFpsMap = mutableMapOf<Int, Int>()

    // VendorClass — handles purchase terminal requests.
    internal val vendor by lazy { VendorClass(this) }

    suspend fun run() = coroutineScope {
        // C++: cNetwork::Init_Server() — first thing it does is Set_Is_Server(true) so that
        // NetworkObjectClass constructors auto-register with a new dynamic ID.
        NetworkObject.isServer = true

        // C++: factory classes registered via static constructors; here we do it explicitly at startup
        NetworkObjectFactories.register()
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

        connectionManager.applicationAcceptanceHandler = ::applicationAcceptanceHandler
        connectionManager.connHandler = { id, host ->
            println("[CONNECT] client $id connected from ${host.address} bps=${host.maximumBps}")
            connectionHandler(id, host)
        }
        connectionManager.disconnectHandler = { id ->
            println("[CONNECT] client $id disconnected")
            serverBrokenConnectionHandler(id)
        }
        connectionManager.serverPacketHandler = ::serverPacketHandler
        println("[SERVER] listening on UDP port ${config.gamePort} (RCON: ${config.rconPort})")

        launch(Dispatchers.IO)   { transport.ioLoop() }
        launch(gameThread)       { processInbound() }
        launch(gameThread)       { serverThink() }
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

    // ---- Network tick — C++: cNetwork::Update() → Server_Think() ----

    // C++: cNetwork::Update() drives the server loop. Server_Think() is the server-side body
    // called from within Update(). Here these are unified into a single suspend loop matching
    // the same call order: cGod::Think → End_Game_Test → Server_Update_Dynamic_Objects →
    // Server_Send_Delete_Notifications → Delete_Pending.
    // Kotlin note: `serverThink` merges C++ cNetwork::Update() + Server_Think() into one coroutine.
    private suspend fun serverThink() {
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

            // Sort players by score once per second (C++: End_Game_Test sort)
            if (nowMs - lastScoreSortMs >= 1000L) {
                lastScoreSortMs = nowMs
                // Note: In the full C++ implementation this sorts teams and players
                // by score for display. Full sort implementation would update rankingByScore maps here.
            }

            // Re-send GameDataUpdateEvent once per second to keep clients' timer in sync
            if (nowMs - lastGameDataUpdateMs >= 1000L && god.playerInGame.isNotEmpty()) {
                lastGameDataUpdateMs = nowMs
                for (clientId in god.playerInGame) {
                    val clientHost = connectionManager.getHost(clientId) ?: continue
                    sendGameDataUpdateEvent(clientHost)
                }
            }

            // C++: cNetwork::End_Game_Test() — game-over detection
            endGameTest()

            // C++: cNetwork::Intermission_Over_Processing() — core restart after intermission
            if (gameState.pendingCoreRestart) {
                gameState.pendingCoreRestart = false
                intermissionOverProcessing()
            }

            // C++: Server_Think() step 1 — cGod::Think()
            god.think(frameDeltaSeconds)

            // C++: CombatManager::Think() step — GameObjManager::Think()
            gameObjManager.think(frameDeltaSeconds)

            // C++: CombatManager::Think() step — COMBAT_SCENE->Update() (physics)
            physicsScene?.update(frameDeltaSeconds)

            // C++: CombatManager::Think() step — GameObjManager::Post_Think()
            gameObjManager.postThink()

            // C++: CombatManager::Think() step — SpawnManager::Update()
            spawnManager?.think(frameDeltaSeconds)

            // C++: cNetwork::Update() — Update_Fps()
            updateFps(nowMs)

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

            // C++: Server_Think() step 3 — Receiver->Server_Update_Dynamic_Objects()
            // Pushes dirty object state to all in-game clients.
            // For delete-pending objects the isDeletePending flag is combined with dirty bits
            // in the same packet, matching C++ Send_Object_Update.
            for (clientId in god.playerInGame) {
                tellClientAboutDynamicObjects(clientId)
            }

            // C++: Server_Think() step 4 — Receiver->Server_Send_Delete_Notifications()
            // Sends header-only deletion packets for all objects marked delete-pending.
            for (clientId in god.playerInGame) {
                tellClientAboutDeleteNotifications(clientId)
            }

            // C++: cNetwork::Update() — NetworkObjectMgrClass::Delete_Pending()
            NetworkObjectManager.deletePending()

            // Clean up C4 objects that have been marked for deletion
            god.c4Objects.removeAll { it.isDeletePending }
            god.beaconObjects.removeAll { it.isDeletePending }

            // C++: cNetwork::Flush() — flush per-tick packet outbox
            flush()

            // Adjust per-host flow controllers with bytes sent this tick
            val connectedCount = connectionManager.getConnectedCount()
            val targetBps = bandwidthBudget.perHostBps(connectedCount)
            for ((rhostId, bytesSent) in bytesSentThisTick) {
                flowControllers[rhostId]?.adjust(targetBps, bytesSent, tickDeltaMs.toFloat())
            }

            delay(tickIntervalMs)
        }
    }

    // ---- Tell_Client_About_Dynamic_Objects ----

    // C++: cNetwork::Tell_Client_About_Dynamic_Objects(int client_id, Vector3& dest_pos)
    // Scans all registered NetworkObjects and pushes dirty state to a single in-game client.
    // Reliable for CREATION/RARE/OCCASIONAL; unreliable for FREQUENT-only.
    // For objects already marked delete-pending (from game logic), isDeletePending is combined with
    // dirty bits in ONE packet (matching C++ Send_Object_Update writing both fields).
    // Objects with zero dirty bits (including delete-pending ones) are skipped — they are handled
    // exclusively by tellClientAboutDeleteNotifications in the next step.
    // Never sends a soldier's FREQUENT update to its own controlling player (client is authoritative).
    private fun tellClientAboutDynamicObjects(clientId: Int) {
        val objects = NetworkObjectManager.getAllObjects()

        for (obj in objects) {
            val delPending = obj.isDeletePending
            val bits = obj.getObjectDirtyBits(clientId).toInt() and 0xFF
            // C++: Tell_Client_About_Dynamic_Objects only sends objects with dirty bits.
            // Delete-pending objects with zero dirty bits are handled exclusively by
            // tellClientAboutDeleteNotifications — do NOT handle them here.
            if (bits == 0) continue

            val host = connectionManager.getHost(clientId) ?: continue
            val isOwnSoldier = (obj is SoldierGameObj) && (obj.controlOwner == clientId)

            if ((bits and 0x08) != 0) {
                // BIT_CREATION — send full creation reliably
                serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, obj, obj.networkId, delPending) }
                obj.setObjectDirtyBits(clientId, (bits and 0x01).toByte())  // preserve BIT_FREQUENT
            } else if ((bits and 0x04) != 0) {
                // BIT_RARE — send rare+occasional+frequent reliably
                serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeRareUpdate(bs, obj, obj.networkId, delPending) }
                obj.setObjectDirtyBits(clientId, (bits and 0x01).toByte())  // preserve BIT_FREQUENT
            } else if ((bits and 0x02) != 0) {
                // BIT_OCCASIONAL — send occasional+frequent reliably
                serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeOccasionalUpdate(bs, obj, obj.networkId, delPending) }
                obj.setObjectDirtyBits(clientId, (bits and 0x01).toByte())  // preserve BIT_FREQUENT
            } else if ((bits and 0x01) != 0) {
                if (delPending) {
                    // BIT_FREQUENT + delete — promote to reliable, combining both in one packet
                    serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeFrequentUpdate(bs, obj, obj.networkId, delPending) }
                } else if (!isOwnSoldier) {
                    // BIT_FREQUENT only — skip own soldier; gate others through FlowController
                    val fc = flowControllers[clientId]
                    if (fc == null || fc.shouldSend(50.0f)) {
                        sendUnreliable(host) { bs -> NetworkObjectPacketWriter.writeFrequentUpdate(bs, obj, obj.networkId) }
                    }
                }
                obj.setObjectDirtyBits(clientId, 0)
            }
        }
    }

    // C++: cNetwork::Tell_Client_About_Delete_Notifications(int client_id)
    // Loops over all NetworkObjects; for each that Is_Delete_Pending(), sends a header-only
    // reliable packet: [networkId:32][dirtyBits:0x00:8][isDeletePending:true:1].
    // This is the SOLE sender of deletion packets for objects with zero dirty bits.
    // Objects with dirty bits + isDeletePending were already sent by tellClientAboutDynamicObjects
    // (with the isDeletePending flag set in the dirty-bit packet), but they also get a header-only
    // deletion packet here — matching C++ which calls Send_Object_Update unconditionally for all
    // delete-pending objects in this second pass.
    private fun tellClientAboutDeleteNotifications(clientId: Int) {
        val host = connectionManager.getHost(clientId) ?: return
        for (obj in NetworkObjectManager.getAllObjects()) {
            if (obj.isDeletePending) {
                serverSendPacket(host) { bs ->
                    bs.addInt(obj.networkId)
                    bs.addByte(0x00.toByte())
                    bs.addBool(true)
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

    // C++: cNetwork::Flush() — force-sends all buffered packets immediately.
    // Combines buffered per-tick packets into fewer datagrams per host and enqueues them.
    private fun flush() {
        bytesSentThisTick.clear()
        for ((rhostId, packets) in pendingOutbox) {
            val datagrams = PacketCombiner.combine(packets)
            if (datagrams.isNotEmpty()) {
                val details = datagrams.joinToString(", ") { dg -> "${dg.data.size}B[${describeDatagramGroups(dg.data)}]" }
                println("[NET] → combined ${packets.size} packets for host $rhostId into ${datagrams.size} datagram(s): $details")
            }
            enqueueWithCrc(datagrams)
            bytesSentThisTick[rhostId] = datagrams.sumOf { it.data.size }
        }
        pendingOutbox.clear()
    }

    // Decodes the group headers in a combined datagram for logging.
    // Format: "N×SzB+" = N packets of Sz bytes, MorePackets=1 (more groups follow in this datagram).
    //         "N×SzB"  = N packets of Sz bytes, MorePackets=0 (last group in datagram).
    private fun describeDatagramGroups(data: ByteArray): String {
        val groups = mutableListOf<String>()
        var pos = 0
        while (pos + 2 <= data.size) {
            val header = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
            val n = header and 0x1F
            val sz = (header shr 5) and 0x3FF
            val more = (header shr 15) and 1 == 1
            if (n <= 0 || sz <= 0) break
            groups.add("${n}×${sz}B${if (more) "+" else ""}")
            pos += 2 + sz + (n - 1) * (1 + sz)
            if (!more) break
        }
        return if (groups.isEmpty()) "?" else groups.joinToString(" ")
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
    // tellClientAboutDynamicObjects() after the client sends BIOEVENT and enters the game.
    private fun connectionHandler(rhostId: Int, host: RemoteHost) {
        // Auto-assign team to balance NOD/GDI. CHANGETEAMEVENT toggles NOD↔GDI.
        val assignedTeam = god.choosePlayerType()
        god.playerTeams[rhostId] = assignedTeam
        println("[CONNECT] sending connection objects to host $rhostId (team=${if (assignedTeam == 0) "NOD" else "GDI"})")

        // NOD team (teamNumber=0) and GDI team (teamNumber=1)
        // C++: cTeam dirty=BIT_CREATION(0x0F) — all 4 tiers sent on initial creation
        serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, teamNod, NET_ID_NOD_TEAM) }
        serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, teamGdi, NET_ID_GDI_TEAM) }

        // Clear team dirty bits for this client — already sent manually above;
        // restoreDirtyBits() in BIOEVENT would otherwise re-mark them.
        teamNod.setObjectDirtyBits(rhostId, 0)
        teamGdi.setObjectDirtyBits(rhostId, 0)

        // GameOptionsEvent — one-time creation event; tells the client what game/map is running.
        // C++: gameoptionsevent.cpp Export_Creation calls Export_Tier_1_Data + Export_Tier_2_Data.
        // cNetEvent subclass has no RARE/OCCASIONAL/FREQUENT state, so those tiers write nothing.
        gameData.currentPlayers = connectionManager.getConnectedCount()
        val gameOptionsEvent = GameOptionsEvent(gameData)
        serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, gameOptionsEvent, gameOptionsEvent.networkId) }

        // Player creation is NOT sent here. C++ sends it in cBioEvent::Act() after the client
        // finishes loading. See BIOEVENT handler (networkClassId=1026).
        // Buildings, base controllers, and ServerFps are sent by tellClientAboutDynamicObjects() after BIOEVENT.
    }

    // ---- Game event handlers ----

    // C++: cNetwork::Server_Packet_Handler (messages.cpp) — generic C→S packet dispatch.
    // Reads the NetworkObject envelope header, creates via factory if BIT_CREATION,
    // imports dirty-bit layers, and calls act() on NetEvent subclasses.
    private fun serverPacketHandler(packet: Packet, rhostId: Int) {
        val bs = packet.payload
        if (bs.bitWritePosition < 41) {
            println("[GAME] serverPacketHandler: rhostId=$rhostId too short (${bs.bitWritePosition} bits), skipping")
            return
        }

        val snap = BitStream()
        val payloadBytes = (bs.bitWritePosition + 7) / 8
        System.arraycopy(bs.buffer, 0, snap.buffer, 0, payloadBytes)
        snap.setBitWritePosition(bs.bitWritePosition)

        val networkId = snap.getInt()
        val dirtyBits = snap.getByte().toInt() and 0xFF
        val isDeletePending = snap.getBool()

        var obj = NetworkObjectManager.findObject(networkId)

        if ((dirtyBits and 0x08) != 0) {
            // BIT_CREATION — read classId, create via factory
            val classId = snap.getInt()
            val factory = NetworkObjectFactoryManager.getFactory(classId)
            if (factory != null) {
                obj = factory.create(snap)
                if (obj != null) {
                    // Wire up server reference for persistent C→S objects
                    if (obj is ClientControl) {
                        obj.server = this
                        obj.rhostId = rhostId
                    }
                    if (obj is ClientFps) {
                        obj.server = this
                        obj.rhostId = rhostId
                    }
                    NetworkObjectManager.registerObject(obj, networkId)
                    obj.importCreation(snap)
                    if (obj is NetEvent) {
                        obj.act(this, rhostId)
                    }
                } else {
                    println("[GAME] factory.create returned null for classId=$classId netId=$networkId")
                }
            } else {
                println("[GAME] no factory for classId=$classId netId=$networkId from rhostId=$rhostId")
            }
        }

        if (obj != null) {
            if ((dirtyBits and 0x04) != 0) obj.importRare(snap)
            if ((dirtyBits and 0x02) != 0) obj.importOccasional(snap)
            if ((dirtyBits and 0x01) != 0) obj.importFrequent(snap)
            if (isDeletePending) obj.setDeletePending()
        }
    }
    // Broadcasts a PlayerKill event to all in-game clients and updates scoring.
    // C++: cPlayerKill (playerkill.cpp) — S→C event sent when a player dies.
    // killerId = rhostId of killer (-1 = no killer / suicide).
    // victimId = rhostId of victim.
    internal fun broadcastPlayerKill(killerId: Int, victimId: Int) {
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
            serverSendPacket(clientHost) { bs ->
                NetworkObjectPacketWriter.writeCreation(bs, event, event.networkId)
            }
        }
        println("[GAME] broadcastPlayerKill: killer=$killerId victim=$victimId")
    }

    // Sends a PLAYER BIT_RARE update (no networkClassId — not a creation packet).
    // C++: cPlayer::Export_Rare + Export_Occasional + Export_Frequent.
    // dirtyBits=0x07 = BIT_RARE|BIT_OCCASIONAL|BIT_FREQUENT (not BIT_CREATION).
    internal fun sendPlayerRareUpdate(host: RemoteHost, rhostId: Int) {
        val netId = god.playerNetIds[rhostId] ?: run {
            println("[GAME] sendPlayerRareUpdate: no playerNetId for rhostId=$rhostId, skipping")
            return
        }
        val player = god.playersByHost[rhostId] ?: run {
            println("[GAME] sendPlayerRareUpdate: no player object for rhostId=$rhostId, skipping")
            return
        }
        serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeRareUpdate(bs, player, netId) }
        println("[GAME] sent PLAYER BIT_RARE to host $rhostId: team=${if (player.team == 0) "NOD" else "GDI"} inGame=${player.isInGame} netId=$netId")
    }

    // C++: gamedataupdateevent.cpp Export_Creation — sent after client finishes loading.
    // Signals the client that gameplay can proceed (activates combat mode via Act()).
    // C++ server sends timeRemainingSeconds and hostedGameNumber.
    internal fun sendGameDataUpdateEvent(host: RemoteHost) {
        val timeRemaining = gameState.timeRemainingSeconds.toInt()
        val event = GameDataUpdateEvent(
            timeRemainingSeconds = timeRemaining,
            hostedGameNumber = hostedGameNumber,
        )
        serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, event, event.networkId) }
    }

    // C++: cNetwork::Update_Fps() — measures actual tick rate and updates cServerFps once per second.
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

    // C++: cNetwork::Server_Send_Packet(packet, mode, recipient) — sends a packet to a specific client.
    // Builds a RELIABLE packet, enqueues it in the host's reliable channel, and sends it immediately.
    // The packet ID is pre-assigned from host.reliable.nextSendId so the wire bytes are consistent.
    internal fun serverSendPacket(host: RemoteHost, writePayload: (BitStream) -> Unit) {
        val p = Packet()
        p.type = PacketType.RELIABLE
        p.id = host.reliable.nextSendId  // enqueue() will assign this same ID
        p.senderId = 0                   // server ID
        writePayload(p.payload)
        val wireData = Packet.buildWirePacket(p)
        println("[GAME] → id=${p.id} host=${host.id} (${wireData.size}B): ${peekGameEvent(p)}")
        host.reliable.enqueue(p, wireData)
        pendingOutbox.getOrPut(host.id) { mutableListOf() }.add(host.address to wireData)
    }

    // C++: cNetwork::Server_Send_Packet_To_All_Connected(packet, mode) — sends a packet to ALL connected rhosts.
    // Unlike Server_Send_Packet(ALL), this sends to every rhost (not just in-game players).
    internal fun serverSendPacketToAllConnected(writePayload: (BitStream) -> Unit) {
        for (rhostId in 1..config.maxPlayers) {
            val host = connectionManager.getHost(rhostId) ?: continue
            serverSendPacket(host, writePayload)
        }
    }

    // ---- End_Game_Test / Intermission_Over_Processing ----

    // C++: cNetwork::End_Game_Test() — checks for game-over conditions and triggers the win event.
    // Called from Server_Think() once per tick.
    private fun endGameTest() {
        // Sort players by score once per second (C++: End_Game_Test sort)
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastScoreSortMs >= 1000L) {
            lastScoreSortMs = nowMs
            // Note: In the full C++ implementation this sorts teams and players
            // by score for display. Full sort implementation would update rankingByScore maps here.
        }

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
    }

    // C++: cNetwork::Intermission_Over_Processing() — called when intermission ends; restarts the
    // game (same map) or rotates to the next map in the cycle.
    private suspend fun intermissionOverProcessing() {
        val nextMap = mapRotation.nextName()
        if (nextMap != null) {
            mapRotation = mapRotation.advance()
            handleMapRotation(nextMap)
        } else {
            mapRotation = mapRotation.advance()  // reset index to 0 for next cycle
            handleCoreRestart()
        }
    }

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
            serverSendPacket(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, winEvent, winEvent.networkId) }
        }

        // Record game result for clients joining during intermission
        gameState.recordGameResult(winType, winner, loser)

        // Start intermission
        gameState.startIntermission()
        println("[GAME] intermission started (${config.intermissionTimeSeconds}s)")
    }

    private fun handleCoreRestart() {
        println("[GAME] core restart — resetting scores and game state")

        // Send ResetWinsEvent to all in-game clients (C++: bioevent.cpp core restart handler)
        val resetWinsEvent = ResetWinsEvent()
        for (clientId in god.playerInGame) {
            val host = connectionManager.getHost(clientId) ?: continue
            serverSendPacket(host) { bs ->
                NetworkObjectPacketWriter.writeCreation(bs, resetWinsEvent, resetWinsEvent.networkId)
            }
        }

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
        god.clearRoundState()           // clear anything deleteSoldier() left behind

        // Reset buildings and base controllers for new round
        baseControllerNod?.reset()
        baseControllerGdi?.reset()
        for (building in gameObjManager.getBuildingList()) {
            building.setNormalizedHealth(1.0f)
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
                    val controller = when (lb.playerType) {
                        0 -> { controllerNod.addBuilding(building); controllerNod }
                        1 -> { controllerGdi.addBuilding(building); controllerGdi }
                        else -> null
                    }
                    if (controller != null) building.cncInitialize(controller)
                    gameObjManager.add(building)
                    gameObjManager.addBuilding(building)
                    println("[BUILDING] registered ${building::class.simpleName} networkId=${lb.networkId} defId=${lb.definitionId} playerType=${lb.playerType}")
                }
                println("[BUILDING] registered ${loadedBuildings.size} buildings, 2 base controllers")
                gameObjManager.updateBuildingCollectionSpheres(level.definitions)
            }

            // Instantiate pre-placed vehicles from LDD (harvesters, decorative vehicles, etc.)
            // C++: cGod loads all VehicleGameObj save-data entries during level init
            val loadedVehicles = level.dynamicData.gameObjects.filterIsInstance<LoadedVehicleGameObj>()
            for (lv in loadedVehicles) {
                god.createLevelVehicle(lv)
            }
            println("[LEVEL] ${loadedVehicles.size} level vehicles instantiated")
        }

        // Post-load validation: check for duplicate network IDs
        val allObjects = NetworkObjectManager.getAllObjects()
        val ids = allObjects.map { it.networkId }
        val seen = mutableSetOf<Int>()
        val duplicates = ids.filter { !seen.add(it) }.toSet()
        if (duplicates.isNotEmpty()) {
            println("[WARN] Duplicate network IDs found after level load: $duplicates")
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
        god.clearRoundState()   // clear tracking lists before objects are destroyed
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

        // Re-sync all still-connected in-game clients for new level objects
        for (rhostId in god.playerInGame) {
            NetworkObjectManager.restoreDirtyBits(rhostId)
        }

        println("[GAME] map rotation complete — now on '$nextMapName', hostedGameNumber=$hostedGameNumber")
    }

    private fun createBuilding(lb: LoadedBuildingGameObj): BuildingGameObj? {
        val pos = Vector3(lb.transform.position.x, lb.transform.position.y, lb.transform.position.z)
        val sphereCenter = Vector3(lb.collectionSphere.center.x, lb.collectionSphere.center.y, lb.collectionSphere.center.z)
        val radius = lb.collectionSphere.radius

        if (!ChunkIds.isBuilding(lb.factoryChunkId)) return null

        val building: BuildingGameObj = when (lb.factoryChunkId) {
            ChunkIds.GAMEOBJ_BUILDING_POWERPLANT    -> PowerPlantGameObj()
            ChunkIds.GAMEOBJ_BUILDING_REFINERY      -> RefineryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_SOLDIERFACTORY -> SoldierFactoryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_WARFACTORY    -> WarFactoryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_AIRSTRIP,
            ChunkIds.GAMEOBJ_BUILDING_VEHICLEFACTORY -> VehicleFactoryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_COMCENTER     -> ComCenterGameObj()
            else                                    -> BuildingGameObj()
        }

        // Apply definition (loads mctSkin, etc.)
        val def = loadedLevel?.definitions?.findById(lb.definitionId.toUInt()) as? BuildingGameObjDef
        if (def != null) building.init(def)

        // Set position (also updates collectionSphere.center)
        building.setPosition(pos)
        building.collectionSphere = ccr.server.level.Sphere(sphereCenter, radius)

        // Defence state from LDD
        val health = lb.defense.healthMax.takeIf { it > 0f } ?: 5000f
        building.health    = health
        building.healthMax = health
        building.shieldType = lb.defense.skinSaveId
        if (lb.defense.shieldStrength > 0f) building.shieldStrength = lb.defense.shieldStrength

        building.isPowerOn  = lb.isPowerOn
        building.playerType = lb.playerType

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
    internal fun createPowerUp(position: Vector3, def: PowerUpGameObjDef): PowerUpGameObj? {
        val modelName = if (def.physDefId != 0)
            (loadedLevel?.definitions?.findById(def.physDefId.toUInt()) as? PhysDefClass)?.modelName ?: ""
        else ""

        val powerUp = PowerUpGameObj()
        powerUp.definition = def
        powerUp.position   = position
        powerUp.modelName  = modelName

        println("[POWERUP] spawned '${def.name}' netId=${powerUp.networkId} at (${position.x}, ${position.y}, ${position.z})")
        return powerUp
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

    // ---- Application_Acceptance_Handler ----

    // C++: cNetwork::Application_Acceptance_Handler(packet) — validates connecting client's credentials.
    // Called by the connection layer before accepting a new client; returns REFUSAL_CLIENT_ACCEPTED
    // if the client is allowed in, or a specific REFUSAL_* code otherwise.
    private fun applicationAcceptanceHandler(
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
            println("[CONNECT] applicationAcceptanceHandler from $address: nickname='$nickname' password='$clientPassword' exeKey=0x${clientExeKey.toString(16)} bitsConsumed=$bitsConsumed payloadBits=${packet.bitLength}")

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

    // ---- Server_Broken_Connection_Handler / Cleanup_After_Client / Remove_Player ----

    // C++: cNetwork::Server_Broken_Connection_Handler(broken_rhost_id) — called by the net library
    // when a reliable packet fails after many attempts. Cleans up the disconnected client.
    private fun serverBrokenConnectionHandler(rhostId: Int) {
        println("[CONNECT] connection broken for host $rhostId")
        loadingHosts.remove(rhostId)
        flowControllers.remove(rhostId)
        cleanupAfterClient(rhostId)
    }

    // C++: cNetwork::Cleanup_After_Client(client_id) — called when a client disconnects (gracefully
    // or via broken connection). Removes all objects and state associated with the client.
    private fun cleanupAfterClient(clientId: Int) {
        removePlayer(clientId)
    }

    // C++: cNetwork::Delete_Player_Objects(client_id) — deletes all network objects owned by the client.
    // In C++ this calls cPlayerManager::Delete_Network_Objects_For_Player.
    private fun deletePlayerObjects(clientId: Int) {
        god.deleteSoldier(clientId)
    }

    // C++: cNetwork::Remove_Player(player_id) — full disconnect cleanup: remove player and their soldier.
    private fun removePlayer(playerId: Int) {
        god.removePlayer(playerId)
    }

    // C++: cNetwork::Server_Kill_Connection(client_id) — forcibly terminates a client connection.
    // Used by kick/eviction logic to remove a misbehaving or kicked player.
    internal fun serverKillConnection(clientId: Int) {
        flowControllers.remove(clientId)
        god.removePlayer(clientId)
    }

    // C++: cNetwork::Test_For_Team_Defaulting(p_player) — if the player has no team assigned,
    // assigns them to the team with fewer players (auto-balance on join).
    private fun testForTeamDefaulting(player: ccr.server.net.Player) {
        // Team assignment is handled by god.choosePlayerType() during connectionHandler.
        // This stub mirrors the C++ method; actual logic lives in God.choosePlayerType().
    }

    // C++: cNetwork::Enable_Waiting_Players() — marks all players waiting for intermission as
    // in-game once intermission ends, and sends GameDataUpdateEvent to each.
    private fun enableWaitingPlayers() {
        for (clientId in god.playerInGame.toList()) {
            val host = connectionManager.getHost(clientId) ?: continue
            sendGameDataUpdateEvent(host)
        }
    }

    // C++: cNetwork::Get_Distance_Priority(pos1, pos2) — computes a [0..1] priority for network
    // object updates based on the distance between the viewer and the object. Closer = higher priority.
    internal fun getDistancePriority(pos1: Vector3, pos2: Vector3): Float {
        val gap = pos2 - pos1
        val d = gap.length()
        val maxDistance = 1000f   // C++: The_Game()->Get_Maximum_World_Distance()
        val range1 = maxDistance / 25.0f
        val range2 = maxDistance / 5.0f
        val range3 = maxDistance + 1
        return when {
            d < range1 -> ((range1 - d) / range1 * 0.499f + 0.50f)
            d < range2 -> ((range2 - d) / (range2 - range1) * 0.40f + 0.10f)
            d < range3 -> ((range3 - d) / (range3 - range2) * 0.10f + 0.00f)
            else       -> 0f
        }
    }

    // C++: cNetwork::Save(csave) — saves player manager state for level handoff.
    // Not needed in the Kotlin server (stateless between rounds), provided for completeness.
    internal fun save(): Boolean = true

    // C++: cNetwork::Load(cload) — restores player manager state after level load.
    // Not needed in the Kotlin server (players re-register via BIOEVENT), provided for completeness.
    internal fun load(): Boolean = true

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
                        serverSendPacket(targetHost) { bs ->
                            NetworkObjectPacketWriter.writeCreation(bs, eviction, eviction.networkId)
                        }
                        serverKillConnection(targetId)
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

    // C++: cNetwork::Show_Welcome_Message() — returns the welcome banner text shown when a player connects.
    private fun showWelcomeMessage(): String = buildString {
        append("=== ${config.serverName} ===\n")
        if (config.gameTitle.isNotEmpty()) append("${config.gameTitle}\n")
        append("Type 'help' for available commands.\n")
    }
}
