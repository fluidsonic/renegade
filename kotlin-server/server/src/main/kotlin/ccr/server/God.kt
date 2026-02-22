package ccr.server

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_REMOTE
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_TIMED
import ccr.server.defs.SoldierGameObjDefWrapper
import ccr.server.defs.VehicleGameObjDefWrapper
import ccr.server.defs.WeaponDefinitionClass
import ccr.server.defs.combat.BeaconGameObjDef
import ccr.server.net.BeaconGameObj
import ccr.server.net.C4GameObj
import ccr.server.net.Player
import ccr.server.net.SoldierGameObj
import ccr.server.net.VehicleGameObj
import ccr.server.net.WeaponEntry

/**
 * Port of C++ cGod (god.cpp) — owns the player/soldier lifecycle.
 *
 * Responsibilities:
 *  - Player creation and team assignment (cGod::Create_Player)
 *  - Soldier spawning for connected players (cGod::Create_Commando)
 *  - Respawn loop — each tick spawns soldiers for soldierless in-game players (cGod::Think)
 *  - Disconnect/suicide cleanup (deleteSoldier, removePlayer)
 *  - Vehicle entry/exit tracking (enterVehicle, exitVehicle)
 *
 * Single-player stubs are included for structural parity with C++ but are no-ops in this MP server.
 */
open class God(private val server: GameServer) {

    companion object {
        const val RESPAWN_DELAY_SECONDS = 3.0f
    }

    // C++ GodState enum
    enum class State {
        UNINITIALIZED, MULTIPLAYER, EXITING,
        SINGLE_INIT, SINGLE_RUNNING, SINGLE_DEAD,
    }

    var state: State = State.UNINITIALIZED

    // Player/soldier state (moved from GameServer)
    val playersByHost = mutableMapOf<Int, Player>()
    val soldiersByHost = mutableMapOf<Int, SoldierGameObj>()
    val vehiclesByNetId = mutableMapOf<Int, VehicleGameObj>()  // networkId → VehicleGameObj
    val playerVehicles = mutableMapOf<Int, VehicleGameObj>()   // rhostId → vehicle being driven
    val playerTeams = mutableMapOf<Int, Int>()   // rhostId → team (0=NOD, 1=GDI)
    val playerInGame = mutableSetOf<Int>()        // rhostIds where IsInGame=true
    val playerNetIds = mutableMapOf<Int, Int>()   // rhostId → networkId of cPlayer object

    // C4 tracking
    val c4Objects = mutableListOf<C4GameObj>()
    private val lastC4PlaceMs = mutableMapOf<Int, Long>()  // rhostId → last placement time

    // Beacon tracking (parallel to C4 tracking)
    val beaconObjects = mutableListOf<BeaconGameObj>()
    private val lastBeaconPlaceMs = mutableMapOf<Int, Long>()  // rhostId → last placement time

    // Respawn cooldown timers: rhostId → remaining seconds before next spawn is permitted
    private val respawnTimers = mutableMapOf<Int, Float>()

    // ---- cGod::Think (god.cpp:91-168) ----

    /**
     * Called every network tick. Transitions state machine and spawns soldiers
     * for in-game players that currently have no soldier.
     *
     * @param deltaSeconds elapsed time since the last tick, in seconds
     */
    fun think(deltaSeconds: Float) {
        // Return early if no players (C++: player list head == NULL)
        if (playerInGame.isEmpty()) return

        // C++: cGod::Think returns early during intermission
        if (server.gameState.isIntermission) return

        // State machine transition on first call
        if (state == State.UNINITIALIZED) {
            // Server is always MP (IS_MISSION = false)
            state = State.MULTIPLAYER
        }

        // SP stubs — no-op for MP server
        if (state == State.SINGLE_INIT) {
            // Create commando for single player, add start script, transition to SINGLE_RUNNING
            state = State.SINGLE_RUNNING
        }

        if (state == State.SINGLE_RUNNING) {
            // Fix player-soldier links after load — stub
        }

        // MP respawn loop: create a commando for every in-game player without a soldier
        if (state == State.MULTIPLAYER) {
            // Decrement respawn timers
            val timerIter = respawnTimers.iterator()
            while (timerIter.hasNext()) {
                val entry = timerIter.next()
                entry.setValue(entry.value - deltaSeconds)
                if (entry.value <= 0f) timerIter.remove()
            }

            for (rhostId in playerInGame.toList()) {  // toList() to avoid ConcurrentModificationException
                if (rhostId !in soldiersByHost) {
                    // Skip if still in respawn cooldown
                    if (respawnTimers.containsKey(rhostId)) continue
                    val player = playersByHost[rhostId] ?: continue
                    createCommando(player)
                }
            }
        }
    }

    // ---- cGod::Create_Player (god.cpp:172-220) ----

    /**
     * Creates (or re-activates) a cPlayer network object for a connecting client.
     * Uses the team already stored in [playerTeams] (assigned at connection time).
     * Registers with [NetworkObjectManager] and stores in [playersByHost].
     * Callers (GameServer) are responsible for sending the creation packet over the network.
     *
     * @return the created [Player] object
     */
    fun createPlayer(rhostId: Int, name: String): Player {
        // Check for an existing inactive player with the same name (reconnect)
        val existing = playersByHost.values.find { !it.isActive && it.name == name }
        if (existing != null) {
            val existingRhostId = playersByHost.entries.find { it.value === existing }?.key
            if (existingRhostId != null && existingRhostId != rhostId) {
                // Reactivate with new rhostId
                playersByHost.remove(existingRhostId)
                val netId = playerNetIds.remove(existingRhostId)
                if (netId != null) {
                    playerNetIds[rhostId] = netId
                    playerTeams[rhostId] = playerTeams.remove(existingRhostId) ?: choosePlayerType()
                }
            }
            // Re-register with NetworkObjectManager (setDeletePending on disconnect caused unregistration)
            val reconnectNetId = playerNetIds[rhostId]
            if (reconnectNetId != null) {
                NetworkObjectManager.registerObject(existing, reconnectNetId)
                existing.setObjectDirtyBit(NetworkObject.BIT_CREATION, true)
            }
            existing.isActive = true
            existing.isInGame = true
            playersByHost[rhostId] = existing
            println("[GOD] reconnected player '${existing.name}' as rhostId=$rhostId")
            return existing
        }

        // Create new player
        val team = playerTeams[rhostId] ?: choosePlayerType().also { playerTeams[rhostId] = it }
        val player = Player(id = rhostId, name = name, team = team, isInGame = true)
        val playerNetId = NetworkObjectManager.getNewDynamicId()
        playerNetIds[rhostId] = playerNetId
        NetworkObjectManager.registerObject(player, playerNetId)
        playersByHost[rhostId] = player
        return player
    }

    // ---- cGod::Create_Commando(cPlayer*) (god.cpp:342-350) ----

    /**
     * Spawns a soldier for the given player. Convenience overload that extracts
     * clientId and playerType from the [Player] object.
     */
    fun createCommando(player: Player): SoldierGameObj? =
        createCommando(player.id, playerTeams[player.id] ?: player.team)

    // ---- cGod::Create_Commando(int, int) (god.cpp:240-339) ----

    /**
     * Spawns a soldier for the given client. Picks the CnC preset based on [playerType],
     * resolves a spawn position via [SpawnManager], creates [SoldierGameObj],
     * registers it, and sends creation packets to all in-game hosts.
     *
     * Declared `open` so tests can override it to verify spawn suppression logic
     * without actually spawning network objects.
     *
     * @param rhostId   remote host / client ID
     * @param playerType  0=NOD, 1=GDI
     * @return the spawned [SoldierGameObj], or null if no definition ID is available
     */
    open fun createCommando(rhostId: Int, playerType: Int): SoldierGameObj? {
        // C++: Is_Cnc() == true → use CnC minigunner presets
        val defId = if (playerType == 0) server.nodSoldierDefId else server.gdiSoldierDefId
        if (defId == 0) {
            println("[GOD] createCommando: no soldier defId for rhostId=$rhostId team=$playerType, skipping")
            return null
        }

        val (position, facing) = server.spawnManager?.getMultiplayerSpawnLocation(playerType)
            ?: Pair(Vector3(0f, 0f, 5f), 0f).also {
                println("[GOD] WARNING: spawnManager is null, spawning at fallback origin (0, 0, 5)")
            }

        println("[GOD] spawned soldier for rhostId=$rhostId team=${if (playerType == 0) "NOD" else "GDI"} " +
            "pos=(${position.x}, ${position.y}, ${position.z})")

        val modelName = if (playerType == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
        val weapons = buildWeaponsForSoldier(defId)

        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = rhostId,
            team         = playerType,
            modelName    = modelName,
            animName     = "S_A_HUMAN.H_A_AINM",
            position     = position,
            facing       = facing,
            weapons      = weapons,
        )
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(soldier, netId)
        soldiersByHost[rhostId] = soldier
        server.gameObjManager.addStar(soldier)

        // Bind the player's data object so buildings (Refinery) can award money
        soldier.playerData = playersByHost[rhostId]

        // Give starting credits on first spawn if configured
        if (server.config.startingCredits > 0 && server.gameState.gameDurationSeconds < 5f) {
            playersByHost[rhostId]?.replaceMoney(server.config.startingCredits.toFloat())
        }

        return soldier
    }

    // ---- cGod::Create_Commando with explicit definition (for purchased characters) ----

    /**
     * Spawns a soldier using an explicit definition ID and model name (used by purchase terminal).
     * Same as [createCommando] but skips starting credits (purchase already costs money).
     *
     * @param rhostId    remote host / client ID
     * @param playerType 0=NOD, 1=GDI
     * @param defId      soldier definition ID from PurchaseSettingsDefClass
     * @param modelName  W3D model name for the character
     * @return the spawned [SoldierGameObj], or null if spawn fails
     */
    fun createCommandoWithDef(rhostId: Int, playerType: Int, defId: Int, modelName: String): SoldierGameObj? {
        if (defId == 0) {
            println("[GOD] createCommandoWithDef: defId=0 for rhostId=$rhostId, skipping")
            return null
        }

        val (position, facing) = server.spawnManager?.getMultiplayerSpawnLocation(playerType)
            ?: Pair(Vector3(0f, 0f, 5f), 0f).also {
                println("[GOD] WARNING: spawnManager is null, spawning at fallback origin (0, 0, 5)")
            }

        println("[GOD] spawned purchased soldier for rhostId=$rhostId team=${if (playerType == 0) "NOD" else "GDI"} " +
            "defId=$defId model=$modelName pos=(${position.x}, ${position.y}, ${position.z})")

        val weapons = buildWeaponsForSoldier(defId)

        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = rhostId,
            team         = playerType,
            modelName    = modelName,
            animName     = "S_A_HUMAN.H_A_AINM",
            position     = position,
            facing       = facing,
            weapons      = weapons,
        )
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(soldier, netId)
        soldiersByHost[rhostId] = soldier
        server.gameObjManager.addStar(soldier)

        // Bind player data (same as createCommando)
        soldier.playerData = playersByHost[rhostId]

        // No starting credits — purchase already costs money

        return soldier
    }

    // ---- Weapon list builder ----

    /**
     * Builds the weapon list for a newly spawned soldier from its definition.
     *
     * Mirrors C++ ArmedGameObj::Copy_Settings(): reads WeaponDefID and
     * SecondaryWeaponDefID from the preset definition and inserts them in
     * ascending keyNumber order (WeaponBagClass::Add_Weapon sorted insertion).
     *
     * Timed C4 is always appended if not already present, since scripts that
     * normally grant it are not yet executed server-side.
     *
     * Falls back to pistol + C4 when the definition cannot be found.
     */
    private fun buildWeaponsForSoldier(defId: Int): MutableList<WeaponEntry> {
        val registry = server.loadedLevel?.definitions
        val wrapper = registry?.findById(defId.toUInt()) as? SoldierGameObjDefWrapper

        val weapons = mutableListOf<WeaponEntry>()

        if (wrapper != null) {
            val armed = wrapper.soldierDef.armed

            // Primary weapon
            if (armed.weaponDefId != 0) {
                val rounds = if (armed.weaponRounds >= 0) {
                    armed.weaponRounds
                } else {
                    (registry.findById(armed.weaponDefId.toUInt()) as? WeaponDefinitionClass)
                        ?.maxInventoryRounds ?: 100
                }
                weapons.add(WeaponEntry(armed.weaponDefId, rounds))
            }

            // Secondary weapon
            if (armed.secondaryWeaponDefId != 0) {
                val rounds = (registry.findById(armed.secondaryWeaponDefId.toUInt()) as? WeaponDefinitionClass)
                    ?.maxInventoryRounds ?: 100
                weapons.add(WeaponEntry(armed.secondaryWeaponDefId, rounds))
            }
        } else {
            // Fallback when def not found: give pistol
            if (server.pistolWeaponDefId != 0) {
                weapons.add(WeaponEntry(server.pistolWeaponDefId, 100))
            }
        }

        // Timed C4 is granted by scripts in C++; hardcode it here until scripts are executed
        if (server.timedC4WeaponDefId != 0 && weapons.none { it.definitionId == server.timedC4WeaponDefId }) {
            weapons.add(WeaponEntry(server.timedC4WeaponDefId, 1))
        }

        // Sort by keyNumber ascending — matches C++ WeaponBagClass::Add_Weapon sorted insertion
        if (registry != null) {
            weapons.sortBy { entry ->
                (registry.findById(entry.definitionId.toUInt()) as? WeaponDefinitionClass)?.keyNumber
                    ?: Float.MAX_VALUE
            }
        }

        return weapons
    }

    // ---- Vehicle spawning ----

    /**
     * Spawns a VehicleGameObj when a vehicle factory completes generation.
     * Registers it with NetworkObjectManager and dirty-bit replication.
     *
     * @param buyerRhostId  host ID of the purchasing player (used for team + logging)
     * @param defId         vehicle definition ID from PurchaseSettingsDefClass
     * @param spawnPosition world position to spawn at (near the factory delivery pad)
     * @return the spawned [VehicleGameObj], or null if defId is 0
     */
    fun createVehicle(buyerRhostId: Int, defId: Int, spawnPosition: Vector3): VehicleGameObj? {
        if (defId == 0) return null

        val wrapper = server.loadedLevel?.definitions?.findById(defId.toUInt())
            as? VehicleGameObjDefWrapper
        val vehicleType = wrapper?.vehicleDef?.type?.value ?: VehicleGameObj.VEHICLE_TYPE_CAR
        val seatCount   = wrapper?.vehicleDef?.numSeats ?: 1
        val playerType  = playerTeams[buyerRhostId] ?: 0

        val vehicle = VehicleGameObj(
            definitionId     = defId,
            position         = spawnPosition,
            vehicleType      = vehicleType,
            seatCount        = seatCount,
            team             = playerType,
            vehicleDelivered = true,
        )
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(vehicle, netId)
        vehiclesByNetId[netId] = vehicle
        server.gameObjManager.add(vehicle)

        println("[GOD] vehicle spawned: defId=$defId netId=$netId team=${if (playerType == 0) "NOD" else "GDI"} " +
            "seats=$seatCount pos=(${spawnPosition.x}, ${spawnPosition.y}, ${spawnPosition.z}) buyer=$buyerRhostId")
        return vehicle
    }

    /**
     * Spawns an unoccupied harvester vehicle for the given [team] at [spawnPosition].
     * Unlike [createVehicle], the harvester has no driver (controlOwner=0).
     */
    fun createHarvester(team: Int, defId: Int, spawnPosition: Vector3): VehicleGameObj? {
        if (defId == 0) return null
        val wrapper = server.loadedLevel?.definitions?.findById(defId.toUInt())
            as? VehicleGameObjDefWrapper
        val vehicleType = wrapper?.vehicleDef?.type?.value ?: VehicleGameObj.VEHICLE_TYPE_CAR
        val seatCount   = wrapper?.vehicleDef?.numSeats ?: 1
        val vehicle = VehicleGameObj(
            definitionId     = defId,
            position         = spawnPosition,
            vehicleType      = vehicleType,
            seatCount        = seatCount,
            team             = team,
            vehicleDelivered = true,
            controlOwner     = 0,
        )
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(vehicle, netId)
        vehiclesByNetId[netId] = vehicle
        server.gameObjManager.add(vehicle)
        println("[GOD] harvester spawned: defId=$defId netId=$netId team=${if (team == 0) "NOD" else "GDI"} " +
            "pos=(${spawnPosition.x}, ${spawnPosition.y}, ${spawnPosition.z})")
        return vehicle
    }

    // ---- Vehicle entry/exit ----

    /**
     * Records that [rhostId]'s soldier has entered [vehicle].
     * Sets the seat occupant, links the driver reference, marks dirty bits.
     * C++: TransitionManager handles this server-side; we detect it from the client's
     * in_vehicle=true flag in the frequent update and respond immediately.
     */
    fun enterVehicle(rhostId: Int, vehicle: VehicleGameObj) {
        val soldier = soldiersByHost[rhostId] ?: return
        vehicle.seatOccupantIds[0] = soldier.networkId
        vehicle.controlOwner = rhostId
        vehicle.driver = soldier
        soldier.inVehicle = true
        playerVehicles[rhostId] = vehicle

        // BIT_RARE for vehicle — seat occupant changed; all clients need to know
        for (clientId in playerInGame) {
            vehicle.setObjectDirtyBit(clientId, NetworkObject.BIT_RARE, true)
        }
        // BIT_FREQUENT for soldier — in_vehicle=true needs to reach other clients
        for (clientId in playerInGame) {
            if (clientId != rhostId) {
                soldier.setObjectDirtyBit(clientId, NetworkObject.BIT_FREQUENT, true)
            }
        }

        println("[GOD] rhostId=$rhostId entered vehicle netId=${vehicle.networkId} soldierNetId=${soldier.networkId}")
    }

    /**
     * Records that the player driving a vehicle has exited.
     * Clears seat occupant, repositions the soldier near the vehicle.
     */
    fun exitVehicle(rhostId: Int) {
        val vehicle = playerVehicles.remove(rhostId) ?: return
        val soldier = soldiersByHost[rhostId]

        vehicle.seatOccupantIds[0] = -1
        vehicle.controlOwner = 0
        vehicle.driver = null
        vehicle.isEngineOn = false
        vehicle.velocity = Vector3(0f, 0f, 0f)

        if (soldier != null) {
            // Place the soldier 2 units to the side of the vehicle
            soldier.position = Vector3(
                vehicle.position.x + 2f,
                vehicle.position.y + 2f,
                vehicle.position.z,
            )
            soldier.inVehicle = false

            // BIT_FREQUENT for soldier — on-foot again; other clients need updated position
            for (clientId in playerInGame) {
                if (clientId != rhostId) {
                    soldier.setObjectDirtyBit(clientId, NetworkObject.BIT_FREQUENT, true)
                }
            }
        }

        // BIT_RARE for vehicle — seat occupant cleared
        for (clientId in playerInGame) {
            vehicle.setObjectDirtyBit(clientId, NetworkObject.BIT_RARE, true)
        }

        println("[GOD] rhostId=$rhostId exited vehicle netId=${vehicle.networkId}")
    }

    // ---- C4 management ----

    /**
     * Creates a C4 object for the given soldier and attaches it to the nearest enemy building.
     * Returns null if rate-limited, wrong weapon type, or no building in range.
     * C++: C4GameObj is created in response to BOOLEAN_WEAPON_FIRE_PRIMARY when weapon style==0.
     */
    fun createC4(rhostId: Int, soldier: SoldierGameObj, nowMs: Long): C4GameObj? {
        // Rate limit: 1 C4 per second per player
        if (nowMs - (lastC4PlaceMs[rhostId] ?: 0L) < 1000L) return null

        val ammoDef = server.getAmmoDefForWeapon(soldier.currentWeaponDefId) ?: return null
        if (ammoDef.ammoType != AMMO_TYPE_C4_REMOTE && ammoDef.ammoType != AMMO_TYPE_C4_TIMED) return null

        // Find nearest alive enemy building within 15m (225 = 15²)
        val enemyTeam = if ((playerTeams[rhostId] ?: 0) == 0) 1 else 0
        val enemyController = if (enemyTeam == 0) server.baseControllerNod else server.baseControllerGdi
        val nearest = enemyController?.getBuildings()
            ?.filter { !it.isDestroyed }
            ?.minByOrNull { b ->
                val dx = b.position.x - soldier.position.x
                val dy = b.position.y - soldier.position.y
                val dz = b.position.z - soldier.position.z
                dx * dx + dy * dy + dz * dz
            }

        if (nearest != null) {
            val dx = nearest.position.x - soldier.position.x
            val dy = nearest.position.y - soldier.position.y
            val dz = nearest.position.z - soldier.position.z
            if (dx * dx + dy * dy + dz * dz > 225f) return null
        } else {
            return null
        }

        val tossedC4DefId = server.tossedC4DefId
        val c4 = C4GameObj(
            definitionId  = tossedC4DefId,
            position      = soldier.position.copy(),
            modelName     = ammoDef.modelFilename,
            ammoDef       = ammoDef.id.toInt(),
            ownerId       = soldier.networkId,
            stuck         = true,
            stuckPosX     = soldier.position.x,
            stuckPosY     = soldier.position.y,
            stuckPosZ     = soldier.position.z,
            stuckMct      = false,
            stuckToObject = true,
            stuckObjectId = nearest.networkId,
        )

        // Set runtime fields
        c4.ammoDefinition = ammoDef
        c4.ownerRhostId   = rhostId
        c4.stuckBuilding  = nearest
        c4.serverRef      = server
        c4.detonationMode = 1
        c4.timer          = if (ammoDef.ammoType == AMMO_TYPE_C4_TIMED) ammoDef.c4TriggerTime1 else 0f

        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(c4, netId)
        server.gameObjManager.add(c4)
        c4Objects.add(c4)
        lastC4PlaceMs[rhostId] = nowMs

        // Enforce per-team C4 limit for non-timed C4 (remote detonation)
        if (ammoDef.ammoType == AMMO_TYPE_C4_REMOTE) {
            maintainC4Limit(playerTeams[rhostId] ?: 0)
        }

        println("[GOD] C4 placed by rhostId=$rhostId netId=$netId type=${if (ammoDef.ammoType == AMMO_TYPE_C4_TIMED) "TIMED" else "REMOTE"} target=${nearest::class.simpleName}")
        return c4
    }

    /**
     * Enforces the per-team non-timed (remote) C4 limit.
     * If the team has more than C4_LIMIT remote C4 active, defuses the oldest by age.
     */
    private fun maintainC4Limit(team: Int) {
        val teamC4 = c4Objects.filter { c4 ->
            !c4.isDeletePending &&
            c4.ammoDefinition?.ammoType == AMMO_TYPE_C4_REMOTE &&
            (playerTeams[c4.ownerRhostId] ?: -1) == team
        }
        if (teamC4.size > C4GameObj.C4_LIMIT) {
            teamC4.maxByOrNull { it.age }?.defuse()
        }
    }

    // ---- Beacon management ----

    /**
     * Creates a beacon object for the given soldier.
     * Returns null if rate-limited or the ammoDef has no associated BeaconGameObjDef.
     * C++: BeaconGameObj is created in response to BOOLEAN_WEAPON_FIRE_PRIMARY for beacon weapons.
     */
    fun createBeacon(rhostId: Int, soldier: SoldierGameObj, ammoDef: AmmoDefinitionClass, nowMs: Long): BeaconGameObj? {
        // Rate limit: 1 beacon per second per player
        if (nowMs - (lastBeaconPlaceMs[rhostId] ?: 0L) < 1000L) return null

        val beaconDef = server.loadedLevel?.definitions?.findById(ammoDef.beaconDefId.toUInt())
            as? BeaconGameObjDef ?: return null

        val beacon = BeaconGameObj(
            definitionId = beaconDef.id.toInt(),
            position     = soldier.position.copy(),
            modelName    = ammoDef.modelFilename,
            initialState = BeaconGameObj.STATE_ARMING,
            initialOwnerId = soldier.networkId,
        )

        // Set runtime fields
        beacon.beaconDef     = beaconDef
        beacon.serverRef     = server
        beacon.ownerRhostId  = rhostId
        beacon.armTimer      = beaconDef.armTime

        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(beacon, netId)
        server.gameObjManager.add(beacon)
        beaconObjects.add(beacon)
        lastBeaconPlaceMs[rhostId] = nowMs

        println("[GOD] beacon placed by rhostId=$rhostId netId=$netId arm=${beaconDef.armTime}s det=${beaconDef.detonateTime}s")
        return beacon
    }

    /**
     * Injects a last-placement timestamp for [rhostId], used by tests to pre-arm the rate limiter.
     * Exposed as `internal` so tests can simulate a recent beacon placement without a full server.
     */
    internal fun injectBeaconPlacementTime(rhostId: Int, ms: Long) {
        lastBeaconPlaceMs[rhostId] = ms
    }

    // ---- cGod cleanup helpers ----

    /**
     * Removes a player's soldier: sends deletion to all other in-game hosts, then unregisters.
     * Starts a [RESPAWN_DELAY_SECONDS]-second cooldown so [think] will not immediately re-spawn.
     * Reused for suicide, team-change, and disconnect.
     */
    fun deleteSoldier(rhostId: Int) {
        // Exit vehicle before deleting the soldier to clear seat occupant state
        if (rhostId in playerVehicles) {
            exitVehicle(rhostId)
        }
        // Defuse all remote C4 owned by this player (timed C4 continues to tick after disconnect)
        c4Objects.filter { !it.isDeletePending && it.ownerRhostId == rhostId && it.ammoDefinition?.ammoType == AMMO_TYPE_C4_REMOTE }
            .forEach { it.defuse() }
        // Cancel all beacons owned by this player
        beaconObjects.filter { !it.isDeletePending && it.ownerRhostId == rhostId }
            .forEach { it.cancel() }
        val soldier = soldiersByHost.remove(rhostId) ?: return
        server.gameObjManager.removeStar(soldier)
        soldier.setDeletePending()
        startRespawnCooldown(rhostId)
        println("[GOD] marked soldier delete-pending for host $rhostId netId=${soldier.networkId}")
    }

    /**
     * Starts (or resets) the respawn cooldown timer for [rhostId].
     * [think] will not call [createCommando] while this timer is active.
     * Exposed as `internal` so tests can inject a cooldown directly.
     */
    internal fun startRespawnCooldown(rhostId: Int) {
        respawnTimers[rhostId] = RESPAWN_DELAY_SECONDS
    }

    /**
     * Returns the remaining respawn cooldown in seconds for [rhostId], or 0 if no cooldown is active.
     * Exposed as `internal` for test inspection.
     */
    internal fun respawnTimerRemaining(rhostId: Int): Float = respawnTimers[rhostId] ?: 0f

    /**
     * Full cleanup when a client disconnects:
     *  1. Delete the soldier (with deletion packets to peers)
     *  2. Send player-object deletion to peers
     *  3. Unregister and clean up all maps
     */
    fun removePlayer(rhostId: Int) {
        deleteSoldier(rhostId)

        // Mark player inactive and delete-pending; centralized loop broadcasts deletion and unregisters
        val player = playersByHost[rhostId]
        if (player != null) {
            player.isActive = false
            player.isInGame = false
            player.setDeletePending()
        }
        playerInGame.remove(rhostId)
    }

    // ---- Team assignment ----

    /**
     * Auto-balance team assignment. Matches C++ The_Game()->Choose_Player_Type().
     * [teamChoice] -1 = auto-balance, 0 = prefer NOD, 1 = prefer GDI.
     */
    fun choosePlayerType(teamChoice: Int = -1): Int {
        if (teamChoice == 0 || teamChoice == 1) return teamChoice
        val nodCount = playerTeams.values.count { it == 0 }
        val gdiCount = playerTeams.values.count { it == 1 }
        return if (nodCount <= gdiCount) 0 else 1
    }

    // ---- SP stubs (no-op in MP server) ----

    fun createAiPlayer() { /* SP only */ }
    fun createGrunt(pos: Vector3) { /* SP only */ }
    fun starKilled() { /* SP only */ }
    fun respawn() { /* SP only */ }
    fun restart() { /* SP only */ }
    fun loadGame() { /* SP only */ }
    fun missionFailed() { /* SP only */ }
    fun storeInventory(soldier: SoldierGameObj) { /* SP only */ }
    fun restoreInventory(soldier: SoldierGameObj) { /* SP only */ }
    fun resetInventory() { /* SP only */ }

    fun exit() { state = State.EXITING }
    fun reset() { state = State.UNINITIALIZED }
}
