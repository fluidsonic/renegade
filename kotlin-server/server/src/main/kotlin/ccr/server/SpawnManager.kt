package ccr.server

import ccr.math.Vector3
import ccr.physics.moveable.Phys3Class
import ccr.server.defs.PowerUpGameObjDef
import ccr.server.defs.SpawnerDefClass
import ccr.server.level.LoadedLevel
import ccr.server.level.ldd.LoadedSpawner
import ccr.server.net.PowerUpGameObj
import kotlin.math.atan2

/**
 * Encapsulates spawn location logic for multiplayer spawning.
 * Matches C++ SpawnManager::Get_Multiplayer_Spawn_Location() (spawn.cpp:762-846).
 *
 * Also owns the powerup-spawner timer logic that matches C++ SpawnManager::Update()
 * and SpawnerClass::Check_Auto_Spawn() (spawn.cpp).
 *
 * Filters soldier spawners by: !isPrimary && isSoldierStartup && playerType == effectiveTeam.
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
    // Network sets this after constructing SpawnManager.
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
     * Returns a spawn position and facing for the given playerType.
     * Matches C++ SpawnManager::Get_Multiplayer_Spawn_Location (spawn.cpp:762-846) exactly.
     *
     * C++ algorithm:
     * 1. NEUTRAL → RENEGADE remap
     * 2. Count suitable spawners (!isPrimary && isSoldierStartup && playerType == team)
     * 3. Randomly select one suitable spawner as fallback
     * 4. Starting from the selected spawner, loop through ALL spawners (wrapping around)
     *    and return the first one where phys_obj->Can_Teleport(tm, true) succeeds
     * 5. Can_Teleport(tm, true) = check_dyn_only — only tests dynamic object overlap
     * 6. If no clear point found, return the randomly selected fallback
     *
     * @param playerType  0=NOD, 1=GDI, -1=RENEGADE (neutral/unassigned -> remapped to RENEGADE)
     * @param physObj     the soldier's Phys3Class for Can_Teleport checks (null = skip collision check)
     * @return (position, facing) pair — facing is the yaw extracted from the spawner's transform matrix
     */
    fun getMultiplayerSpawnLocation(playerType: Int, physObj: Phys3Class? = null): Pair<Vector3, Float> {
        val effectiveType = if (playerType == PLAYERTYPE_NEUTRAL) PLAYERTYPE_RENEGADE else playerType

        // C++ spawn.cpp:774-782 — count suitable spawners
        val suitableIndices = mutableListOf<Int>()
        for (i in spawners.indices) {
            val def = spawners[i].def
            if (!def.isPrimary && def.isSoldierStartup && def.playerType == effectiveType) {
                suitableIndices.add(i)
            }
        }

        if (suitableIndices.isEmpty()) {
            println("[SPAWN] WARNING: no spawners for playerType=$effectiveType, using origin fallback")
            return Pair(Vector3(0f, 0f, 5f), 0f)
        }

        // C++ spawn.cpp:795-816 — randomly choose one of the suitable spawners
        val selectedSuitableIdx = kotlin.random.Random.nextInt(suitableIndices.size)
        val startIndex = suitableIndices[selectedSuitableIdx]
        val fallbackTm = spawners[startIndex].spawner.transform
        val fallbackPos = fallbackTm.position
        val fallbackFacing = extractFacing(fallbackTm)

        println("[SPAWN] randomly selected spawner index=$startIndex defId=${spawners[startIndex].spawner.definitionId} " +
            "pos=(${fallbackPos.x}, ${fallbackPos.y}, ${fallbackPos.z}) facing=$fallbackFacing")

        // C++ spawn.cpp:822-841 — loop through entire list starting from selected index,
        // try Can_Teleport(tm, check_dyn_only=true) on each suitable spawner
        if (physObj != null) {
            for (offset in spawners.indices) {
                val index = (offset + startIndex) % spawners.size
                val (spawner, def) = spawners[index]

                if (!def.isPrimary && def.isSoldierStartup && def.playerType == effectiveType) {
                    val tm = spawner.transform
                    val pos = tm.position
                    val mathPos = Vector3(pos.x, pos.y, pos.z)
                    if (physObj.canTeleport(mathPos)) {
                        println("[SPAWN] clear spawn point found at index=$index defId=${spawner.definitionId} " +
                            "pos=(${pos.x}, ${pos.y}, ${pos.z})")
                        return Pair(mathPos, extractFacing(tm))
                    }
                }
            }
            // C++ spawn.cpp:840 — no clear spawn points found, fall through to fallback
            println("[SPAWN] WARNING: no clear spawn points found, using fallback")
        }

        // C++ spawn.cpp:845 — fallback: return the spawn point we randomly chose
        return Pair(Vector3(fallbackPos.x, fallbackPos.y, fallbackPos.z), fallbackFacing)
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
            if (live != null && !live.isDeletePending) continue  // still alive -> freeze timer

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
            // if spawned == null: timer stays <= 0, retry next tick immediately
        }
    }

    companion object {
        private const val PLAYERTYPE_NEUTRAL  = -2
        private const val PLAYERTYPE_RENEGADE = -1

        /** Fallback spawn delay if SpawnerDefClass.spawnDelay is 0 or missing. */
        const val SPAWN_DELAY_DEFAULT = 10.0f

        /**
         * Extract yaw facing angle from a level Matrix3D transform.
         * C++: Matrix3D::Get_Z_Rotation() = atan2(Row[1][0], Row[0][0])
         * Level Matrix3D layout (row-major 3x4): elements[0..3]=row0, elements[4..7]=row1
         * So Row[1][0] = elements[4], Row[0][0] = elements[0].
         */
        fun extractFacing(tm: ccr.server.level.Matrix3D): Float =
            atan2(tm.elements[4], tm.elements[0])
    }
}
