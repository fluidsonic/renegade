package ccr.server

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectManager
import ccr.server.defs.VehicleGameObjDef
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
 *
 * Single-player stubs are included for structural parity with C++ but are no-ops in this MP server.
 */
class God(private val server: GameServer) {

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
    val playerTeams = mutableMapOf<Int, Int>()   // rhostId → team (0=NOD, 1=GDI)
    val playerInGame = mutableSetOf<Int>()        // rhostIds where IsInGame=true
    val playerNetIds = mutableMapOf<Int, Int>()   // rhostId → networkId of cPlayer object

    // ---- cGod::Think (god.cpp:91-168) ----

    /**
     * Called every network tick. Transitions state machine and spawns soldiers
     * for in-game players that currently have no soldier.
     */
    fun think() {
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
     * @param rhostId   remote host / client ID
     * @param playerType  0=NOD, 1=GDI
     * @return the spawned [SoldierGameObj], or null if no definition ID is available
     */
    fun createCommando(rhostId: Int, playerType: Int): SoldierGameObj? {
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
        val weapons = if (server.pistolWeaponDefId != 0) {
            listOf(WeaponEntry(server.pistolWeaponDefId, 100))
        } else emptyList()

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

        val weapons = if (server.pistolWeaponDefId != 0) {
            listOf(WeaponEntry(server.pistolWeaponDefId, 100))
        } else emptyList()

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

        val def = server.loadedLevel?.definitions?.all()
            ?.filterIsInstance<VehicleGameObjDef>()
            ?.find { it.definition.id.toInt() == defId }

        val vehicleType = def?.type?.value ?: VehicleGameObj.VEHICLE_TYPE_CAR
        val seatCount   = def?.numSeats ?: 1
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

    // ---- cGod cleanup helpers ----

    /**
     * Removes a player's soldier: sends deletion to all other in-game hosts, then unregisters.
     * Reused for suicide, team-change, and disconnect.
     */
    fun deleteSoldier(rhostId: Int) {
        val soldier = soldiersByHost.remove(rhostId) ?: return
        server.gameObjManager.removeStar(soldier)
        soldier.setDeletePending()
        println("[GOD] marked soldier delete-pending for host $rhostId netId=${soldier.networkId}")
    }

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
