package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of CinematicGameObjDef (Combat/cinematicgameobj.cpp).
 *
 * C++ hierarchy: CinematicGameObjDef : ArmedGameObjDef
 * C++ defaults: SoundDefID(0), AutoFireWeapon(false), DestroyAfterAnimation(true), CameraRelative(false)
 */
class CinematicGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val soundDefId: Int = 0,
    val soundBoneName: String = "",
    val animationName: String = "",
    val autoFireWeapon: Boolean = false,
    val destroyAfterAnimation: Boolean = true,
    val cameraRelative: Boolean = false,
) : DefinitionClass(name, id, chunkId) {
    companion object {
        const val CHUNK_ID: UInt = 0x0004012Cu  // CHUNKID_GAME_OBJECT_DEF_CINEMATIC
    }
}

// Chunk IDs from cinematicgameobj.cpp local enum
private const val CHUNKID_DEF_VARIABLES = 418001958u

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_SOUND_DEF_ID = 1
private const val MICROCHUNKID_DEF_SOUND_BONE_NAME = 2
private const val MICROCHUNKID_DEF_ANIMATION_NAME = 3 // XXX_ prefix in C++ but still read/written
private const val MICROCHUNKID_DEF_AUTO_FIRE_WEAPON = 4
private const val MICROCHUNKID_DEF_DESTROY_AFTER_ANIMATION = 5
private const val MICROCHUNKID_DEF_CAMERA_RELATIVE = 6

fun parseCinematicGameObjDef(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): CinematicGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return CinematicGameObjDef(name = name, id = id, chunkId = chunkId)

    return CinematicGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        soundDefId = vars.readMicroInt(MICROCHUNKID_DEF_SOUND_DEF_ID) ?: 0,
        soundBoneName = vars.readMicroString(MICROCHUNKID_DEF_SOUND_BONE_NAME) ?: "",
        animationName = vars.readMicroString(MICROCHUNKID_DEF_ANIMATION_NAME) ?: "",
        autoFireWeapon = vars.readMicroBool(MICROCHUNKID_DEF_AUTO_FIRE_WEAPON) ?: false,
        destroyAfterAnimation = vars.readMicroBool(MICROCHUNKID_DEF_DESTROY_AFTER_ANIMATION) ?: true,
        cameraRelative = vars.readMicroBool(MICROCHUNKID_DEF_CAMERA_RELATIVE) ?: false,
    )
}
