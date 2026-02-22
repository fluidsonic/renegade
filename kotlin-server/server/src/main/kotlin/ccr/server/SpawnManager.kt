package ccr.server

import ccr.math.Vector3
import ccr.server.defs.combat.SpawnerDefClass
import ccr.server.level.LoadedLevel
import ccr.server.level.ldd.LoadedSpawner

/**
 * Encapsulates spawn location logic for multiplayer spawning.
 * Matches C++ SpawnManager::Get_Multiplayer_Spawn_Location() (spawn.cpp:796-880).
 *
 * Filters spawners by: !isPrimary && isSoldierStartup && playerType == effectiveTeam.
 * No collision checks or furthest-from-enemy logic (no physics engine yet).
 */
class SpawnManager(level: LoadedLevel) {

    private data class ResolvedSpawner(
        val spawner: LoadedSpawner,
        val def: SpawnerDefClass,
    )

    private val spawners: List<ResolvedSpawner>

    init {
        val rawSpawners = level.dynamicData.spawners
        println("[SPAWN] ${rawSpawners.size} spawner instances in LDD")

        spawners = rawSpawners.mapNotNull { spawner ->
            val def = level.definitions.findById(spawner.definitionId.toUInt())
            when {
                def == null ->
                    null.also { println("[SPAWN]   defId=${spawner.definitionId}: NOT FOUND in registry") }
                def !is SpawnerDefClass ->
                    null.also { println("[SPAWN]   defId=${spawner.definitionId}/${def.classId} name='${def.name}': found but is ${def::class.simpleName} (not SpawnerDefClass)") }
                else -> {
                    println("[SPAWN]   defId=${spawner.definitionId} name='${def.name}': " +
                        "isPrimary=${def.isPrimary} isSoldierStartup=${def.isSoldierStartup} playerType=${def.playerType}")
                    ResolvedSpawner(spawner, def)
                }
            }
        }
        val nodSoldier = spawners.count { !it.def.isPrimary && it.def.isSoldierStartup && it.def.playerType == 0 }
        val gdiSoldier = spawners.count { !it.def.isPrimary && it.def.isSoldierStartup && it.def.playerType == 1 }
        val weapon = spawners.count { it.def.isMultiplayWeaponSpawner }
        println("[SPAWN] resolved ${spawners.size} spawners: $nodSoldier NOD soldier, $gdiSoldier GDI soldier, $weapon weapon")
    }

    /**
     * Returns a random spawn position for the given playerType.
     * C++: SpawnManager::Get_Multiplayer_Spawn_Location(playerType) — spawn.cpp:796-880.
     *
     * @param playerType  0=NOD, 1=GDI, -1=RENEGADE (neutral/unassigned → remapped to RENEGADE)
     */
    fun getMultiplayerSpawnLocation(playerType: Int): Vector3 {
        val effectiveType = if (playerType == PLAYERTYPE_NEUTRAL) PLAYERTYPE_RENEGADE else playerType

        val candidates = spawners.filter { (_, def) ->
            !def.isPrimary && def.isSoldierStartup && def.playerType == effectiveType
        }

        if (candidates.isEmpty()) {
            println("[SPAWN] WARNING: no spawners for playerType=$effectiveType, using origin fallback")
            return Vector3(0f, 0f, 5f)
        }

        val selected = candidates.random()
        val levelPos = selected.spawner.transform.position
        println("[SPAWN] selected spawner defId=${selected.spawner.definitionId} " +
            "pos=(${levelPos.x}, ${levelPos.y}, ${levelPos.z})")
        return Vector3(levelPos.x, levelPos.y, levelPos.z)
    }

    companion object {
        private const val PLAYERTYPE_NEUTRAL  = -2
        private const val PLAYERTYPE_RENEGADE = -1
    }
}
