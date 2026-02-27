package ccr.server

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_REMOTE
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_TIMED
import ccr.server.defs.PhysDefClass
import ccr.server.defs.SoldierGameObjDef
import ccr.server.defs.VehicleGameObjDef
import ccr.server.defs.WeaponDefinitionClass
import ccr.server.defs.BeaconGameObjDef
import ccr.server.defs.PowerUpGameObjDef
import ccr.server.net.BeaconGameObj
import ccr.server.net.C4GameObj
import ccr.server.net.Player
import ccr.server.net.SoldierGameObj
import ccr.server.net.VehicleGameObj
import ccr.server.net.WeaponBagClass

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
open class God(private val server: Network) {

    companion object {
        /**
         * Adds a weapon to a soldier's weapon bag, or tops up rounds if already owned.
         * C++: WeaponBagClass::Add_Weapon — new weapon = add entry; already owned = add rounds.
         * When grantWeapon=false and weapon is not owned, nothing is added — this matches
         * C++ WeaponBagClass::Add_Weapon which returns early when give_weapon=false and
         * the weapon is not found in the bag.
         *
         * @param maxRounds round cap (from WeaponDefinitionClass.maxInventoryRounds); default = uncapped.
         */
        internal fun addWeaponToSoldier(
            soldier: SoldierGameObj,
            weaponDefId: Int,
            rounds: Int,
            grantWeapon: Boolean,
            maxRounds: Int = Int.MAX_VALUE,
        ) {
            val bag = soldier.weaponBag
            val existing = (1 until bag.getCount()).mapNotNull { bag.peekWeapon(it) }
                .firstOrNull { it.definitionId == weaponDefId }
            if (existing != null) {
                existing.setTotalRounds((existing.getTotalRounds() + rounds).coerceAtMost(maxRounds))
            } else if (grantWeapon) {
                bag.addWeapon(weaponDefId, rounds.coerceAtMost(maxRounds))
            }
        }
    }

    // C++ GodState enum
    enum class State {
        UNINITIALIZED, MULTIPLAYER, EXITING,
        SINGLE_INIT, SINGLE_RUNNING, SINGLE_DEAD,
    }

    var state: State = State.UNINITIALIZED

    // Player/soldier state (moved from Network)
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
        // C++: cGod::Think spawns immediately — respawn delay screen is handled client-side
        if (state == State.MULTIPLAYER) {
            for (rhostId in playerInGame.toList()) {  // toList() to avoid ConcurrentModificationException
                if (rhostId !in soldiersByHost) {
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
     * Callers (Network) are responsible for sending the creation packet over the network.
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

        val fallbackModel = if (playerType == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
        // FIXME: replace with W3D HLod name lookup once W3D loading is implemented
        val soldierDef = server.loadedLevel?.definitions?.findById(defId.toUInt()) as? SoldierGameObjDef
        val physDefId = soldierDef?.physical?.physDefId ?: 0
        val rawModel = if (physDefId != 0) {
            (server.loadedLevel?.definitions?.findById(physDefId.toUInt()) as? PhysDefClass)?.modelName ?: ""
        } else ""
        val modelName = rawModel.substringAfterLast('\\').substringAfterLast('/').substringBeforeLast('.')
            .ifEmpty { fallbackModel }

        val soldier = SoldierGameObj()
        soldierDef?.let { soldier.definition = it }
        soldier.controlOwner = rhostId
        soldier.playerType   = playerType
        soldier.modelName    = modelName
        soldier.animName     = "S_A_HUMAN.H_A_AINM"
        soldier.position     = position
        soldier.facing       = facing
        buildWeaponsForSoldier(defId, soldier.weaponBag)
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(soldier, netId)
        soldiersByHost[rhostId] = soldier
        server.gameObjManager.addStar(soldier)

        // C++: cGod::Create_Commando — Set_Player_Data links the soldier back to the player
        soldier.setPlayerData(playersByHost[rhostId])

        // Give starting credits on first spawn if configured
        if (server.config.startingCredits > 0 && server.gameState.gameDurationSeconds < 5f) {
            playersByHost[rhostId]?.replaceMoney(server.config.startingCredits.toFloat())
        }

        return soldier
    }

    // ---- cGod::Create_Commando with explicit definition (for purchased characters) ----

    /**
     * Spawns a soldier using an explicit definition ID (used by purchase terminal).
     * Same as [createCommando] but skips starting credits (purchase already costs money).
     * The model name is resolved from the SoldierGameObjDef's physDefId chain, with a
     * team-based fallback if the def is not found.
     *
     * @param rhostId    remote host / client ID
     * @param playerType 0=NOD, 1=GDI
     * @param defId      soldier definition ID from PurchaseSettingsDefClass
     * @return the spawned [SoldierGameObj], or null if spawn fails
     */
    fun createCommandoWithDef(rhostId: Int, playerType: Int, defId: Int): SoldierGameObj? {
        if (defId == 0) {
            println("[GOD] createCommandoWithDef: defId=0 for rhostId=$rhostId, skipping")
            return null
        }

        val (position, facing) = server.spawnManager?.getMultiplayerSpawnLocation(playerType)
            ?: Pair(Vector3(0f, 0f, 5f), 0f).also {
                println("[GOD] WARNING: spawnManager is null, spawning at fallback origin (0, 0, 5)")
            }

        val fallbackModel = if (playerType == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
        // FIXME: replace with W3D HLod name lookup once W3D loading is implemented
        val soldierDef2 = server.loadedLevel?.definitions?.findById(defId.toUInt()) as? SoldierGameObjDef
        val physDefId2 = soldierDef2?.physical?.physDefId ?: 0
        val rawModel2 = if (physDefId2 != 0) {
            (server.loadedLevel?.definitions?.findById(physDefId2.toUInt()) as? PhysDefClass)?.modelName ?: ""
        } else ""
        val modelName = rawModel2.substringAfterLast('\\').substringAfterLast('/').substringBeforeLast('.')
            .ifEmpty { fallbackModel }

        println("[GOD] spawned purchased soldier for rhostId=$rhostId team=${if (playerType == 0) "NOD" else "GDI"} " +
            "defId=$defId model=$modelName pos=(${position.x}, ${position.y}, ${position.z})")

        val soldier = SoldierGameObj()
        soldierDef2?.let { soldier.definition = it }
        soldier.controlOwner = rhostId
        soldier.playerType   = playerType
        soldier.modelName    = modelName
        soldier.animName     = "S_A_HUMAN.H_A_AINM"
        soldier.position     = position
        soldier.facing       = facing
        buildWeaponsForSoldier(defId, soldier.weaponBag)
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(soldier, netId)
        soldiersByHost[rhostId] = soldier
        server.gameObjManager.addStar(soldier)

        // C++: cGod::Create_Commando — Set_Player_Data links the soldier back to the player
        soldier.setPlayerData(playersByHost[rhostId])

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
    /**
     * Populates [bag] with weapons for a newly spawned soldier from its definition.
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
    private fun buildWeaponsForSoldier(defId: Int, bag: WeaponBagClass) {
        val registry = server.loadedLevel?.definitions
        val wrapper = registry?.findById(defId.toUInt()) as? SoldierGameObjDef

        data class WeaponEntry(val defId: Int, val rounds: Int)
        val entries = mutableListOf<WeaponEntry>()

        if (wrapper != null) {
            val armed = wrapper.armed

            // Primary weapon
            if (armed.weaponDefId != 0) {
                val rounds = if (armed.weaponRounds >= 0) {
                    armed.weaponRounds
                } else {
                    (registry.findById(armed.weaponDefId.toUInt()) as? WeaponDefinitionClass)
                        ?.maxInventoryRounds ?: 100
                }
                entries.add(WeaponEntry(armed.weaponDefId, rounds))
            }

            // Secondary weapon
            if (armed.secondaryWeaponDefId != 0) {
                val rounds = (registry.findById(armed.secondaryWeaponDefId.toUInt()) as? WeaponDefinitionClass)
                    ?.maxInventoryRounds ?: 100
                entries.add(WeaponEntry(armed.secondaryWeaponDefId, rounds))
            }
        } else {
            // Fallback when def not found: give pistol
            if (server.pistolWeaponDefId != 0) {
                entries.add(WeaponEntry(server.pistolWeaponDefId, 100))
            }
        }

        // Timed C4 is granted by scripts in C++; hardcode it here until scripts are executed
        if (server.timedC4WeaponDefId != 0 && entries.none { it.defId == server.timedC4WeaponDefId }) {
            entries.add(WeaponEntry(server.timedC4WeaponDefId, 1))
        }

        // Sort by keyNumber ascending — matches C++ WeaponBagClass::Add_Weapon sorted insertion
        if (registry != null) {
            entries.sortBy { entry ->
                (registry.findById(entry.defId.toUInt()) as? WeaponDefinitionClass)?.keyNumber
                    ?: Float.MAX_VALUE
            }
        }

        for (entry in entries) {
            bag.addWeapon(entry.defId, entry.rounds)
        }
    }

    // ---- Vehicle spawning ----

    // FIXME: resolveModelName should be replaced by a real W3D implementation — the model name
    // should come from the loaded W3D model's HLod chunk name (via Peek_Physical_Object()->Peek_Model()->Get_Name()),
    // not by string-manipulating the DDB definition path.
    private fun resolveModelName(wrapper: VehicleGameObjDef?): String {
        if (wrapper == null) return ""
        val physDefId = wrapper.physical.physDefId
        if (physDefId == 0) return ""
        val physDef = server.loadedLevel?.definitions?.findById(physDefId.toUInt()) as? PhysDefClass
        val raw = physDef?.modelName ?: return ""
        // C++: Peek_Physical_Object()->Peek_Model()->Get_Name() returns the W3D HLod chunk name,
        // e.g. "V_NOD_TURRET" — not the full DDB path "vehicles\nod turret\v_nod_turret.w3d".
        // Extract the base filename, strip the extension, and uppercase to match C++ behaviour.
        return raw.substringAfterLast('\\').substringAfterLast('/').substringBeforeLast('.').uppercase()
    }

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
            as? VehicleGameObjDef
        val vehicleType = wrapper?.type?.value ?: VehicleGameObj.VEHICLE_TYPE_CAR
        val seatCount   = wrapper?.numSeats ?: 2  // C++ default: NumSeats = 2
        val modelName   = resolveModelName(wrapper)
        val playerType  = playerTeams[buyerRhostId] ?: 0

        val vehicle = VehicleGameObj()
        wrapper?.let { vehicle.definition = it }
        vehicle.modelName       = modelName
        vehicle.position        = spawnPosition
        vehicle.playerType      = playerType
        vehicle.vehicleDelivered = true
        // Resize seat list to match definition
        vehicle.seatOccupants.clear()
        repeat(seatCount) { vehicle.seatOccupants.add(null) }
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(vehicle, netId)
        vehiclesByNetId[netId] = vehicle
        server.gameObjManager.add(vehicle)

        println("[GOD] vehicle spawned: defId=$defId netId=$netId " +
            "def=${if (wrapper != null) "FOUND" else "MISSING"} " +
            "model='$modelName' type=$vehicleType seats=$seatCount " +
            "team=${if (playerType == 0) "NOD" else "GDI"} " +
            "pos=(${spawnPosition.x}, ${spawnPosition.y}, ${spawnPosition.z}) buyer=$buyerRhostId")
        return vehicle
    }

    /**
     * Spawns an unoccupied harvester vehicle for the given [team] at [spawnPosition].
     * Unlike [createVehicle], the harvester has no driver (controlOwner=0).
     */
    fun createHarvester(team: Int, defId: Int, spawnPosition: Vector3): VehicleGameObj? {
        if (defId == 0) return null
        val wrapper = server.loadedLevel?.definitions?.findById(defId.toUInt())
            as? VehicleGameObjDef
        val vehicleType = wrapper?.type?.value ?: VehicleGameObj.VEHICLE_TYPE_CAR
        val seatCount   = wrapper?.numSeats ?: 1
        val vehicle = VehicleGameObj()
        wrapper?.let { vehicle.definition = it }
        vehicle.modelName       = resolveModelName(wrapper)
        vehicle.position        = spawnPosition
        vehicle.playerType      = team
        vehicle.vehicleDelivered = true
        vehicle.controlOwner    = 0
        vehicle.seatOccupants.clear()
        repeat(seatCount) { vehicle.seatOccupants.add(null) }
        val netId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(vehicle, netId)
        vehiclesByNetId[netId] = vehicle
        server.gameObjManager.add(vehicle)
        println("[GOD] harvester spawned: defId=$defId netId=$netId team=${if (team == 0) "NOD" else "GDI"} " +
            "pos=(${spawnPosition.x}, ${spawnPosition.y}, ${spawnPosition.z})")
        return vehicle
    }

    // ---- Equipment grants (PowerUpGameObjDef::Grant) ----

    /**
     * Applies a PowerUpGameObjDef grant to the buyer's existing soldier.
     * C++: PowerUpGameObjDef::Grant() — called on equipment purchases and powerup pickups.
     * Updates in-place and marks BIT_OCCASIONAL dirty so all clients see the change.
     */
    fun grantPowerUp(rhostId: Int, powerUpDefId: Int) {
        val soldier = soldiersByHost[rhostId] ?: return
        val def = server.loadedLevel?.definitions?.findById(powerUpDefId.toUInt())
            as? PowerUpGameObjDef ?: return

        // grantShieldType — upgrades soldier's armor type when grant type is higher than current
        // C++: powerup.cpp line 268
        if (def.grantShieldType > 0 && def.grantShieldType > soldier.shieldType) {
            soldier.shieldType = def.grantShieldType
            soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
        }

        // grantShieldStrengthMax — increases max shield by (grantShieldStrengthMax * baseDef.shieldStrengthMax)
        // C++: powerup.cpp line 277; rounds up via (int)(add + 0.95f)
        if (def.grantShieldStrengthMax > 0f) {
            val baseDef = (soldier.definition as? SoldierGameObjDef)?.damageable?.defenseObjectDef
            if (baseDef != null) {
                val add = (def.grantShieldStrengthMax * baseDef.shieldStrengthMax + 0.95f).toInt().toFloat()
                soldier.shieldStrengthMax += add
                soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
            }
        }

        if (def.grantShieldStrength != 0f && soldier.shieldStrength < soldier.shieldStrengthMax) {
            soldier.shieldStrength = (soldier.shieldStrength + def.grantShieldStrength)
                .coerceAtMost(soldier.shieldStrengthMax)
            soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
        }

        // grantHealthMax — increases max health by (grantHealthMax * baseDef.healthMax)
        // C++: powerup.cpp line 321; rounds up via (int)(add + 0.95f)
        if (def.grantHealthMax > 0f) {
            val baseDef = (soldier.definition as? SoldierGameObjDef)?.damageable?.defenseObjectDef
            if (baseDef != null) {
                val add = (def.grantHealthMax * baseDef.healthMax + 0.95f).toInt().toFloat()
                soldier.healthMax += add
                soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
            }
        }

        if (def.grantHealth != 0f && soldier.health < soldier.healthMax) {
            soldier.health = (soldier.health + def.grantHealth).coerceAtMost(soldier.healthMax)
            soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
        }

        if (def.grantWeaponId != 0) {
            // C++: WeaponClass::maxInventoryRounds caps total ammo; Apply_Damage/Add_Rounds respect it
            val wDef = server.loadedLevel?.definitions?.findById(def.grantWeaponId.toUInt()) as? WeaponDefinitionClass
            val maxRounds = wDef?.maxInventoryRounds ?: Int.MAX_VALUE
            addWeaponToSoldier(soldier, def.grantWeaponId, def.grantWeaponRounds, def.grantWeapon, maxRounds)
            soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
            println("[GOD] equipment grant: rhostId=$rhostId weapon=${def.grantWeaponId} " +
                "rounds=${def.grantWeaponRounds} grantWeapon=${def.grantWeapon}")
        } else if (def.grantWeaponClips && def.grantWeaponRounds > 0) {
            val definitions = server.loadedLevel?.definitions
            var refilled = false
            val bag = soldier.weaponBag
            for (i in 1 until bag.getCount()) {
                val weapon = bag.peekWeapon(i) ?: continue  // index 0 is null sentinel
                val wDef = definitions?.findById(weapon.definitionId.toUInt()) as? WeaponDefinitionClass
                if (wDef != null && wDef.canReceiveGenericCnCAmmo && wDef.clipSize > 0) {
                    weapon.setTotalRounds(
                        (weapon.getTotalRounds() + wDef.clipSize * def.grantWeaponRounds)
                            .coerceAtMost(wDef.maxInventoryRounds)
                    )
                    refilled = true
                }
            }
            if (refilled) {
                soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
                println("[GOD] equipment grant: rhostId=$rhostId weapon clips refilled")
            }
        }
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
        // Set first seat occupant (driver seat = 0)
        if (vehicle.seatOccupants.isEmpty()) vehicle.seatOccupants.add(null)
        vehicle.seatOccupants[0] = soldier
        vehicle.controlOwner = rhostId
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

        if (vehicle.seatOccupants.isNotEmpty()) vehicle.seatOccupants[0] = null
        vehicle.controlOwner = 0
        vehicle.enableEngine(false)
        vehicle.setVelocity(Vector3(0f, 0f, 0f))

        if (soldier != null) {
            // Place the soldier 2 units to the side of the vehicle
            soldier.position = Vector3(
                vehicle.position.x + 2f,
                vehicle.position.y + 2f,
                vehicle.position.z,
            )

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

        val ammoDef = server.getAmmoDefForWeapon(soldier.getWeapon()?.definitionId ?: 0) ?: return null
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
        c4.detonationMode = 1
        c4.timer          = if (ammoDef.ammoType == AMMO_TYPE_C4_TIMED) ammoDef.c4TriggerTime1 else 0f
        // FIXME: c4.ownerRhostId — not a field on new C4GameObj; track per-owner C4 via c4Objects list only
        // FIXME: c4.stuckBuilding / c4.serverRef — not fields on new C4GameObj; building damage handled by stuckObject + detonate()

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
            (playerTeams[c4.getOwner()?.controlOwner ?: -1] ?: -1) == team
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

        val beacon = BeaconGameObj()
        beacon.definition    = beaconDef
        beacon.position      = soldier.position.copy()
        beacon.modelName     = ammoDef.modelFilename
        beacon.state         = BeaconGameObj.STATE_ARMING
        beacon.stateTimer    = beaconDef.armTime
        // Link owner so BeaconGameObj can read owner's controlOwner for identification
        beacon.owner.set(soldier)
        // FIXME: beacon.serverRef / beacon.ownerRhostId / beacon.beaconDef / beacon.armTimer — not fields on new BeaconGameObj
        // State and timer set above directly; definition accessed via beacon.getDefinition() after registration

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
        // Identify ownership via the C4's owner soldier's controlOwner (rhostId)
        c4Objects.filter { !it.isDeletePending && it.getOwner()?.controlOwner == rhostId && it.ammoDefinition?.ammoType == AMMO_TYPE_C4_REMOTE }
            .forEach { it.defuse() }
        // Cancel all beacons owned by this player.
        // Identify ownership via the owner soldier's controlOwner (live path) or
        // ownerRhostId fallback (test / secondary-constructor path where owner ref is null).
        beaconObjects.filter {
            !it.isDeletePending &&
            (it.getOwner()?.controlOwner == rhostId ||
             (it.getOwner() == null && it.ownerRhostId == rhostId))
        }.forEach { it.setDeletePending() }
        val soldier = soldiersByHost.remove(rhostId) ?: return
        server.gameObjManager.removeStar(soldier)
        soldier.setDeletePending()
        println("[GOD] marked soldier delete-pending for host $rhostId netId=${soldier.networkId}")
    }

    /**
     * Clears round-specific tracking state in God after all soldiers have been deleted.
     * Called by handleCoreRestart() and unloadLevel() to prevent stale references
     * from carrying over into the next round / next map.
     */
    internal fun clearRoundState() {
        // Defuse/cancel any C4 and beacons not already cleaned up by deleteSoldier()
        // (e.g. timed C4 owned by no live player, or anything that slipped through)
        c4Objects.filter { !it.isDeletePending }.forEach { it.defuse() }
        beaconObjects.filter { !it.isDeletePending }.forEach { it.setDeletePending() }
        c4Objects.clear()
        beaconObjects.clear()

        // Rate-limit maps — clear so old timestamps don't block first placements
        lastC4PlaceMs.clear()
        lastBeaconPlaceMs.clear()

        // Player vehicle references — clear stale associations
        // (vehicles themselves are cleaned up separately by unloadLevel/handleCoreRestart)
        playerVehicles.clear()
    }

    /**
     * Full cleanup when a client disconnects:
     *  1. Delete the soldier (with deletion packets to peers)
     *  2. Send player-object deletion to peers
     *  3. Unregister and clean up all maps
     */
    fun removePlayer(rhostId: Int) {
        deleteSoldier(rhostId)

        // Clean up any vehicles still assigned to this player (controlOwner == rhostId)
        // that are not currently being driven (already cleaned up by deleteSoldier → exitVehicle)
        val ownedVehicles = vehiclesByNetId.values.filter {
            it.controlOwner == rhostId && rhostId !in playerVehicles
        }
        ownedVehicles.forEach { it.setDeletePending() }

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
