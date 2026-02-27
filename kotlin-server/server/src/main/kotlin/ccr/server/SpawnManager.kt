package ccr.server

import ccr.math.Vector3
import ccr.server.defs.PowerUpGameObjDef
import ccr.server.defs.SpawnerDefClass
import ccr.server.level.LoadedLevel
import ccr.server.level.ldd.LoadedSpawner
import ccr.server.net.PowerUpGameObj

/**
 * Encapsulates spawn location logic for multiplayer spawning.
 * Matches C++ SpawnManager::Get_Multiplayer_Spawn_Location() (spawn.cpp:796-880).
 *
 * Also owns the powerup-spawner timer logic that matches C++ SpawnManager::Update()
 * and SpawnerClass::Check_Auto_Spawn() (spawn.cpp).
 *
 * Filters soldier spawners by: !isPrimary && isSoldierStartup && playerType == effectiveTeam.
 * No collision checks or furthest-from-enemy logic (no physics engine yet).
 */
class SpawnManager(level: LoadedLevel) {

    private data class ResolvedSpawner(
        val spawner: LoadedSpawner,
        val def: SpawnerDefClass,
    )

    private data class PowerUpSpawnerState(
        val spawner: LoadedSpawner,
        val def: SpawnerDefClass,
        val powerUpDef: PowerUpGameObjDef,
        val spawnDelay: Float,
        var timer: Float,
        var liveObject: PowerUpGameObj? = null,
    )

    private val spawners: List<ResolvedSpawner>
    private val powerUpSpawners: List<PowerUpSpawnerState>

    // Callback invoked when a powerup spawner's timer fires.
    // Arguments: spawn position, PowerUpGameObjDef to create.
    // GameServer sets this after constructing SpawnManager.
    var onCreatePowerUp: ((position: Vector3, def: PowerUpGameObjDef) -> PowerUpGameObj?)? = null

    init {
        val rawSpawners = level.dynamicData.spawners
        println("[SPAWN] ${rawSpawners.size} spawner instances in LDD")

        spawners = rawSpawners.mapNotNull { spawner ->
            val def = level.definitions.findById(spawner.definitionId.toUInt())
            when {
                def == null ->
                    null.also { println("[SPAWN]   defId=${spawner.definitionId}: NOT FOUND in registry") }
                def !is SpawnerDefClass ->
                    null.also { println("[SPAWN]   defId=${spawner.definitionId}/${def.chunkId} name='${def.name}': found but is ${def::class.simpleName} (not SpawnerDefClass)") }
                else -> {
                    println("[SPAWN]   defId=${spawner.definitionId} name='${def.name}': " +
                        "isPrimary=${def.isPrimary} isSoldierStartup=${def.isSoldierStartup} playerType=${def.playerType}")
                    ResolvedSpawner(spawner, def)
                }
            }
        }

        // Build powerup spawner list — enabled weapon spawners with a known PowerUpGameObjDef
        powerUpSpawners = spawners.mapNotNull { (spawner, def) ->
            if (!spawner.enabled) return@mapNotNull null
            if (!def.isMultiplayWeaponSpawner) return@mapNotNull null

            val objectDefId = def.spawnDefinitionIdList.firstOrNull() ?: return@mapNotNull null
            val powerUpDef = level.definitions.findById(objectDefId.toUInt()) as? PowerUpGameObjDef
                ?: return@mapNotNull null.also {
                    println("[SPAWN]   weapon spawner defId=${spawner.definitionId} has objectDefId=$objectDefId but not a PowerUpGameObjDef, skipping")
                }

            val delay = if (def.spawnDelay > 0f) def.spawnDelay else SPAWN_DELAY_DEFAULT
            val pos = spawner.transform.position
            println("[SPAWN]   powerup spawner: spawnerDefId=${spawner.definitionId} " +
                "powerUpName='${powerUpDef.name}' delay=${delay}s pos=(${pos.x}, ${pos.y}, ${pos.z})")
            PowerUpSpawnerState(spawner, def, powerUpDef, delay, timer = delay)
        }

        val nodSoldier = spawners.count { !it.def.isPrimary && it.def.isSoldierStartup && it.def.playerType == 0 }
        val gdiSoldier = spawners.count { !it.def.isPrimary && it.def.isSoldierStartup && it.def.playerType == 1 }
        val weapon = spawners.count { it.def.isMultiplayWeaponSpawner }
        println("[SPAWN] resolved ${spawners.size} spawners: $nodSoldier NOD soldier, $gdiSoldier GDI soldier, $weapon weapon (${powerUpSpawners.size} with PowerUpGameObjDef)")
    }

    /**
     * Returns a random spawn position for the given playerType.
     * C++: SpawnManager::Get_Multiplayer_Spawn_Location(playerType) — spawn.cpp:796-880.
     *
     * @param playerType  0=NOD, 1=GDI, -1=RENEGADE (neutral/unassigned → remapped to RENEGADE)
     */
    fun getMultiplayerSpawnLocation(playerType: Int): Pair<Vector3, Float> {
        val effectiveType = if (playerType == PLAYERTYPE_NEUTRAL) PLAYERTYPE_RENEGADE else playerType

        val candidates = spawners.filter { (_, def) ->
            !def.isPrimary && def.isSoldierStartup && def.playerType == effectiveType
        }

        if (candidates.isEmpty()) {
            println("[SPAWN] WARNING: no spawners for playerType=$effectiveType, using origin fallback")
            return Pair(Vector3(0f, 0f, 5f), 0f)
        }

        val selected = candidates.random()
        val levelPos = selected.spawner.transform.position
        println("[SPAWN] selected spawner defId=${selected.spawner.definitionId} " +
            "pos=(${levelPos.x}, ${levelPos.y}, ${levelPos.z})")
        return Pair(Vector3(levelPos.x, levelPos.y, levelPos.z), 0f)
    }

    /**
     * Ticks all powerup spawner timers.
     * C++: SpawnManager::Update() (spawn.cpp) accumulates time and calls Check_Auto_Spawn().
     *
     * @param deltaSeconds  time elapsed since last tick
     */
    fun think(deltaSeconds: Float) {
        if (powerUpSpawners.isEmpty()) return

        for (state in powerUpSpawners) {
            // C++: timer only counts down when no live object exists
            val live = state.liveObject
            if (live != null && !live.isDeletePending) continue  // still alive → freeze timer

            state.liveObject = null  // clear stale reference

            state.timer -= deltaSeconds
            if (state.timer > 0f) continue

            val pos = state.spawner.transform.position
            println("[SPAWN] powerup spawner fired: '${state.powerUpDef.name}' at (${pos.x}, ${pos.y}, ${pos.z})")
            val spawned = onCreatePowerUp?.invoke(Vector3(pos.x, pos.y, pos.z), state.powerUpDef)
            if (spawned != null) {
                state.liveObject = spawned
                state.timer = state.spawnDelay  // reset timer only after successful spawn
            }
            // if spawned == null: timer stays ≤ 0, retry next tick immediately
        }
    }

    companion object {
        private const val PLAYERTYPE_NEUTRAL  = -2
        private const val PLAYERTYPE_RENEGADE = -1

        /** Fallback spawn delay if SpawnerDefClass.spawnDelay is 0 or missing. */
        const val SPAWN_DELAY_DEFAULT = 10.0f
    }
}
