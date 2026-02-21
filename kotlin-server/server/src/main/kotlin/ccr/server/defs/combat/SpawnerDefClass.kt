package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of SpawnerDefClass (Combat/spawn.h).
 *
 * C++ hierarchy: SpawnerDefClass : DefinitionClass
 *
 * Chunk layout inside OBJDATA:
 *   [CHUNKID_DEF_PARENT = 1013991542]    -> DefinitionClass base (name + id) — caller provides them
 *   [CHUNKID_DEF_VARIABLES = 1013991543] -> spawner-specific micro-chunks
 */
class SpawnerDefClass(
    name: String,
    id: UInt,
    classId: UInt,
    // Spawner fields — defaults match C++ constructor (spawn.cpp SpawnerDefClass::SpawnerDefClass)
    val spawnDefinitionIdList: List<Int> = emptyList(),
    val playerType: Int = -2,                  // PLAYERTYPE_NEUTRAL
    val spawnMax: Int = -1,                    // unlimited
    val spawnDelay: Float = 10f,
    val spawnDelayVariation: Float = 0f,
    val isPrimary: Boolean = false,
    val isSoldierStartup: Boolean = false,
    val postVisualSpawnDelay: Float = 0f,
    val specialEffectsObjId: Int = 0,
    val gotoSpawnerPos: Boolean = false,
    val gotoSpawnerPosPriority: Float = 30f,
    val teleportFirstSpawn: Boolean = true,
    val startsDisabled: Boolean = false,
    val killHibernatingSpawn: Boolean = true,
    val applySpawnMaterialEffect: Boolean = true,
    val isMultiplayWeaponSpawner: Boolean = false,
    val scriptNames: List<String> = emptyList(),
    val scriptParameters: List<String> = emptyList(),
) : DefinitionClass(name, id, classId) {

    companion object {
        const val CLASS_ID: UInt = 0x300Du // CLASSID_GAME_OBJECTS(0x3000) + 13
    }
}

// Chunk IDs from spawn.cpp local enum (starting at 1013991542)
private const val CHUNKID_DEF_VARIABLES = 1013991543u

// Micro-chunk IDs (from spawn.cpp local enum, starting at 1)
private const val MCID_DEFINITION_ID = 1
// 2 = obsolete (IS_COMMANDO_STARTING_POINT)
private const val MCID_PLAYER_TYPE = 3
private const val MCID_SPAWN_MAX = 4
private const val MCID_SPAWN_DELAY = 5
// 6 = obsolete (AUTO_SPAWN_RADIUS)
// 7 = obsolete (ONE_AT_A_TIME)
private const val MCID_SPAWN_DELAY_VARIATION = 8
private const val MCID_IS_PRIMARY = 9
// 10, 11, 12 = obsolete (EFFECT_MODEL_NAME, EFFECT_ANIMATION_NAME, SOUND_ID)
private const val MCID_POST_EFFECT_SPAWN_DELAY = 13
private const val MCID_SPECIAL_EFFECTS_OBJ_ID = 14
private const val MCID_IS_SOLDIER_STARTUP = 15
private const val MCID_GOTO_SPAWNER_POS = 16
private const val MCID_TELEPORT_FIRST_SPAWN = 17
private const val MCID_SCRIPT_NAME = 18
private const val MCID_SCRIPT_PARAMETERS = 19
private const val MCID_STARTS_DISABLED = 20
private const val MCID_KILL_HIBERNATING_SPAWN = 21
private const val MCID_GOTO_SPAWNER_POS_PRIORITY = 22
private const val MCID_APPLY_SPAWN_MATERIAL_EFFECT = 23
private const val MCID_IS_MULTIPLAY_WEAPON_SPAWNER = 24

fun parseSpawnerDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): SpawnerDefClass? {
    val varsReader = objDataReader.findChunk(CHUNKID_DEF_VARIABLES) ?: return null

    // Repeated micro-chunks for lists
    val spawnDefIds = varsReader.findAllMicroChunks(MCID_DEFINITION_ID).mapNotNull { bytes ->
        if (bytes.size < 4) null
        else ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }
    val scriptNames = varsReader.findAllMicroChunks(MCID_SCRIPT_NAME).map { readString(it) }
    val scriptParams = varsReader.findAllMicroChunks(MCID_SCRIPT_PARAMETERS).map { readString(it) }

    return SpawnerDefClass(
        name = name,
        id = id,
        classId = classId,
        spawnDefinitionIdList = spawnDefIds,
        playerType = varsReader.readMicroInt(MCID_PLAYER_TYPE) ?: -2,
        spawnMax = varsReader.readMicroInt(MCID_SPAWN_MAX) ?: -1,
        spawnDelay = varsReader.readMicroFloat(MCID_SPAWN_DELAY) ?: 10f,
        spawnDelayVariation = varsReader.readMicroFloat(MCID_SPAWN_DELAY_VARIATION) ?: 0f,
        isPrimary = varsReader.readMicroBool(MCID_IS_PRIMARY) ?: false,
        isSoldierStartup = varsReader.readMicroBool(MCID_IS_SOLDIER_STARTUP) ?: false,
        postVisualSpawnDelay = varsReader.readMicroFloat(MCID_POST_EFFECT_SPAWN_DELAY) ?: 0f,
        specialEffectsObjId = varsReader.readMicroInt(MCID_SPECIAL_EFFECTS_OBJ_ID) ?: 0,
        gotoSpawnerPos = varsReader.readMicroBool(MCID_GOTO_SPAWNER_POS) ?: false,
        gotoSpawnerPosPriority = varsReader.readMicroFloat(MCID_GOTO_SPAWNER_POS_PRIORITY) ?: 30f,
        teleportFirstSpawn = varsReader.readMicroBool(MCID_TELEPORT_FIRST_SPAWN) ?: true,
        startsDisabled = varsReader.readMicroBool(MCID_STARTS_DISABLED) ?: false,
        killHibernatingSpawn = varsReader.readMicroBool(MCID_KILL_HIBERNATING_SPAWN) ?: true,
        applySpawnMaterialEffect = varsReader.readMicroBool(MCID_APPLY_SPAWN_MATERIAL_EFFECT) ?: true,
        isMultiplayWeaponSpawner = varsReader.readMicroBool(MCID_IS_MULTIPLAY_WEAPON_SPAWNER) ?: false,
        scriptNames = scriptNames,
        scriptParameters = scriptParams,
    )
}

private fun readString(bytes: ByteArray): String {
    val nullIndex = bytes.indexOfFirst { it == 0.toByte() }
    val len = if (nullIndex < 0) bytes.size else nullIndex
    return String(bytes, 0, len, Charsets.ISO_8859_1)
}
