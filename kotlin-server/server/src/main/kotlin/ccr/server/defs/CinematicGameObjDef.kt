package ccr.server.defs

import ccr.server.mix.ChunkReader

// C++: CinematicGameObjDef : public ArmedGameObjDef (cinematicgameobj.cpp)
// C++ defaults: SoundDefID(0), AutoFireWeapon(false), DestroyAfterAnimation(true), CameraRelative(false)
// CLASSID_GAME_OBJECT_DEF_CINEMATIC = 0x3011
// CHUNKID_GAME_OBJECT_DEF_CINEMATIC = 0x0004012C
open class CinematicGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    weaponDefId: Int = 0,
    secondaryWeaponDefId: Int = 0,
    weaponRounds: Int = -1,
    weaponTiltRate: Float = 1.0f,
    weaponTiltMin: Float = -10000f,
    weaponTiltMax: Float = 10000f,
    weaponTurnRate: Float = 1.0f,
    weaponTurnMin: Float = -10000f,
    weaponTurnMax: Float = 10000f,
    weaponError: Float = 0f,
    // C++: int SoundDefID (initialized to 0)
    val soundDefId: Int = 0,
    // C++: StringClass SoundBoneName (initialized to "")
    val soundBoneName: String = "",
    // C++: StringClass AnimationName (initialized to "")
    val animationName: String = "",
    // C++: bool AutoFireWeapon (initialized to false)
    val autoFireWeapon: Boolean = false,
    // C++: bool DestroyAfterAnimation (initialized to true)
    val destroyAfterAnimation: Boolean = true,
    // C++: bool CameraRelative (initialized to false)
    val cameraRelative: Boolean = false,
) : ArmedGameObjDef(
    name, id, chunkId,
    weaponDefId, secondaryWeaponDefId, weaponRounds,
    weaponTiltRate, weaponTiltMin, weaponTiltMax,
    weaponTurnRate, weaponTurnMin, weaponTurnMax,
    weaponError,
) {
    companion object {
        // CHUNKID_GAME_OBJECT_DEF_CINEMATIC = 0x0004012C
        const val CHUNK_ID: UInt = 0x0004012Cu

        // CLASSID_GAME_OBJECT_DEF_CINEMATIC = 0x3011
        const val CLASS_ID: UInt = 0x3011u

        // CinematicGameObjDef local chunk IDs (cinematicgameobj.cpp enum, base = 418001957)
        const val CHUNKID_DEF_PARENT    = 418001957u
        const val CHUNKID_DEF_VARIABLES = 418001958u

        // CinematicGameObjDef micro-chunk IDs
        const val MICROCHUNKID_DEF_SOUND_DEF_ID              = 1
        const val MICROCHUNKID_DEF_SOUND_BONE_NAME           = 2
        const val XXX_MICROCHUNKID_DEF_ANIMATION_NAME        = 3  // XXX_ prefix in C++ — legacy/still active
        const val MICROCHUNKID_DEF_AUTO_FIRE_WEAPON          = 4
        const val MICROCHUNKID_DEF_DESTROY_AFTER_ANIMATION   = 5
        const val MICROCHUNKID_DEF_CAMERA_RELATIVE           = 6

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): CinematicGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
                ?: return CinematicGameObjDef(name = name, id = id, chunkId = chunkId)
            return CinematicGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                soundDefId = vars.readMicroInt(MICROCHUNKID_DEF_SOUND_DEF_ID) ?: 0,
                soundBoneName = vars.readMicroString(MICROCHUNKID_DEF_SOUND_BONE_NAME) ?: "",
                animationName = vars.readMicroString(XXX_MICROCHUNKID_DEF_ANIMATION_NAME) ?: "",
                autoFireWeapon = vars.readMicroBool(MICROCHUNKID_DEF_AUTO_FIRE_WEAPON) ?: false,
                destroyAfterAnimation = vars.readMicroBool(MICROCHUNKID_DEF_DESTROY_AFTER_ANIMATION) ?: true,
                cameraRelative = vars.readMicroBool(MICROCHUNKID_DEF_CAMERA_RELATIVE) ?: false,
            )
        }
    }
}
