package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of ExplosionDefinitionClass (Combat/explosion.h).
 *
 * C++ hierarchy: ExplosionDefinitionClass : DefinitionClass
 */
class ExplosionDefinitionClass(
    name: String,
    id: UInt,
    classId: UInt,
    val physDefId: Int = 0,
    val soundDefId: Int = 0,
    val damageRadius: Float = 0f,
    val damageStrength: Float = 0f,
    val damageWarhead: Int = 0,
    val damageIsScaled: Boolean = true,
    val decalFilename: String = "",
    val decalSize: Float = 10f,
    val animatedExplosion: Boolean = true,
    val cameraShakeIntensity: Float = 0f,
    val cameraShakeRadius: Float = 25f,
    val cameraShakeDuration: Float = 1.5f,
) : DefinitionClass(name, id, classId) {

    companion object {
        const val CLASS_ID: UInt = 0xB003u // CLASSID_MUNITIONS + 3
    }
}

// Chunk ID from explosion.cpp (octal 0317001525 = 54264661 decimal = 0x033C0355 hex)
private const val CHUNKID_EXPLOSION_DEF_VARIABLES = 54264661u

// Micro-chunk IDs (from explosion.cpp local enum, starting at 1)
private const val MCID_PHYS_DEF_ID = 1
private const val MCID_SOUND_DEF_ID = 2
private const val MCID_DAMAGE_RADIUS = 3
private const val MCID_DAMAGE_STRENGTH = 4
private const val MCID_DAMAGE_WARHEAD = 5
private const val MCID_DAMAGE_IS_SCALED = 6
private const val MCID_DECAL_FILENAME = 7
private const val MCID_DECAL_SIZE = 8
// 9 = XXXMICROCHUNKID_EXPLOSION_DEF_ANIMATION (deprecated)
private const val MCID_ANIMATED_EXPLOSION = 10
private const val MCID_CAMERASHAKE_INTENSITY = 11
private const val MCID_CAMERASHAKE_RADIUS = 12
private const val MCID_CAMERASHAKE_DURATION = 13

fun parseExplosionDefinitionClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): ExplosionDefinitionClass? {
    val varsReader = objDataReader.findChunk(CHUNKID_EXPLOSION_DEF_VARIABLES) ?: return null

    return ExplosionDefinitionClass(
        name = name,
        id = id,
        classId = classId,
        physDefId = varsReader.readMicroInt(MCID_PHYS_DEF_ID) ?: 0,
        soundDefId = varsReader.readMicroInt(MCID_SOUND_DEF_ID) ?: 0,
        damageRadius = varsReader.readMicroFloat(MCID_DAMAGE_RADIUS) ?: 0f,
        damageStrength = varsReader.readMicroFloat(MCID_DAMAGE_STRENGTH) ?: 0f,
        damageWarhead = varsReader.readMicroInt(MCID_DAMAGE_WARHEAD) ?: 0,
        damageIsScaled = varsReader.readMicroBool(MCID_DAMAGE_IS_SCALED) ?: true,
        decalFilename = varsReader.readMicroString(MCID_DECAL_FILENAME) ?: "",
        decalSize = varsReader.readMicroFloat(MCID_DECAL_SIZE) ?: 10f,
        animatedExplosion = varsReader.readMicroBool(MCID_ANIMATED_EXPLOSION) ?: true,
        cameraShakeIntensity = varsReader.readMicroFloat(MCID_CAMERASHAKE_INTENSITY) ?: 0f,
        cameraShakeRadius = varsReader.readMicroFloat(MCID_CAMERASHAKE_RADIUS) ?: 25f,
        cameraShakeDuration = varsReader.readMicroFloat(MCID_CAMERASHAKE_DURATION) ?: 1.5f,
    )
}
