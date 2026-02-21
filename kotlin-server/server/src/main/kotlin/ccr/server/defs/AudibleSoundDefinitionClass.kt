package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of AudibleSoundDefinitionClass (WWAudio/AudibleSound.h).
 *
 * C++ hierarchy: AudibleSoundDefinitionClass : DefinitionClass
 *
 * Parsed from the AUDIBLE_SOUND_DEF_SAVELOAD micro-chunks inside the
 * CHUNKID_VARIABLES (0x100) chunk written by Save_Variables().
 */
class AudibleSoundDefinitionClass(
    name: String,
    id: UInt,
    classId: UInt,
    val priority: Float = 0.5f,
    val volume: Float = 1.0f,
    val volumeRandomizer: Float = 0.0f,
    val pan: Float = 0.5f,
    val loopCount: Int = 1,
    val virtualChannel: Int = 0,
    val dropOffRadius: Float = 40.0f,
    val maxVolRadius: Float = 20.0f,
    val is3D: Boolean = true,
    val filename: String = "",
    val type: Int = TYPE_SOUND_EFFECT,
    val displayText: String = "",
    val startOffset: Float = 0.0f,
    val pitchFactor: Float = 1.0f,
    val pitchFactorRandomizer: Float = 0.0f,
    // Logical sound fields
    val logicalTypeMask: Int = 0,
    val logicalNotifyDelay: Float = 2.0f,
    val logicalDropOffRadius: Float = -1.0f,
    val createLogical: Boolean = false,
    // Misc UI info
    val sphereColorR: Float = 0.0f,
    val sphereColorG: Float = 0.75f,
    val sphereColorB: Float = 0.75f,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_SOUND = NEXT_SUPER_CLASSID(4) = 0x1000 + 4*0x1000 */
        const val CLASS_ID: UInt = 0x5000u

        // AudibleSoundClass::SOUND_TYPE enum values
        const val TYPE_MUSIC: Int = 0
        const val TYPE_SOUND_EFFECT: Int = 1
        const val TYPE_DIALOG: Int = 2
        const val TYPE_CINEMATIC: Int = 3
    }
}

// Chunk / micro-chunk IDs from AUDIBLE_SOUND_DEF_SAVELOAD namespace
// (AudibleSound.cpp lines 68-101)
private const val CHUNKID_VARIABLES = 0x00000100u

// Micro-chunk variable IDs (enum starting at 0x01)
private const val VARID_PRIORITY = 0x03
private const val VARID_VOLUME = 0x04
private const val VARID_PAN = 0x05
private const val VARID_LOOP_COUNT = 0x06
private const val VARID_DROP_OFF = 0x07
private const val VARID_MAX_VOL = 0x08
private const val VARID_TYPE = 0x09
private const val VARID_IS3D = 0x0A
private const val VARID_FILENAME = 0x0B
private const val VARID_DISPLAY_TEXT = 0x0C
private const val VARID_LOGICAL_MASK = 0x0D
private const val VARID_LOGICAL_DELAY = 0x0E
private const val VARID_CREATE_LOGICAL = 0x0F
private const val VARID_LOGICAL_DROP_OFF = 0x10
private const val VARID_SPHERE_COLOR = 0x11
private const val VARID_START_OFFSET = 0x12
private const val VARID_PITCH_FACTOR = 0x13
private const val VARID_PITCH_FACTOR_RND = 0x14
private const val VARID_VOLUME_RND = 0x15
private const val VARID_VIRTUAL_CHANNEL = 0x16

fun parseAudibleSoundDefinitionClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): AudibleSoundDefinitionClass? {
    val vars = objDataReader.findChunk(CHUNKID_VARIABLES)
        ?: return null

    return AudibleSoundDefinitionClass(
        name = name,
        id = id,
        classId = classId,
        priority = vars.readMicroFloat(VARID_PRIORITY) ?: 0.5f,
        volume = vars.readMicroFloat(VARID_VOLUME) ?: 1.0f,
        volumeRandomizer = vars.readMicroFloat(VARID_VOLUME_RND) ?: 0.0f,
        pan = vars.readMicroFloat(VARID_PAN) ?: 0.5f,
        loopCount = vars.readMicroInt(VARID_LOOP_COUNT) ?: 1,
        virtualChannel = vars.readMicroInt(VARID_VIRTUAL_CHANNEL) ?: 0,
        dropOffRadius = vars.readMicroFloat(VARID_DROP_OFF) ?: 40.0f,
        maxVolRadius = vars.readMicroFloat(VARID_MAX_VOL) ?: 20.0f,
        is3D = vars.readMicroBool(VARID_IS3D) ?: true,
        filename = vars.readMicroString(VARID_FILENAME) ?: "",
        type = vars.readMicroInt(VARID_TYPE) ?: AudibleSoundDefinitionClass.TYPE_SOUND_EFFECT,
        displayText = vars.readMicroString(VARID_DISPLAY_TEXT) ?: "",
        startOffset = vars.readMicroFloat(VARID_START_OFFSET) ?: 0.0f,
        pitchFactor = vars.readMicroFloat(VARID_PITCH_FACTOR) ?: 1.0f,
        pitchFactorRandomizer = vars.readMicroFloat(VARID_PITCH_FACTOR_RND) ?: 0.0f,
        logicalTypeMask = vars.readMicroInt(VARID_LOGICAL_MASK) ?: 0,
        logicalNotifyDelay = vars.readMicroFloat(VARID_LOGICAL_DELAY) ?: 2.0f,
        logicalDropOffRadius = vars.readMicroFloat(VARID_LOGICAL_DROP_OFF) ?: -1.0f,
        createLogical = vars.readMicroBool(VARID_CREATE_LOGICAL) ?: false,
        sphereColorR = vars.readMicroFloatAt(VARID_SPHERE_COLOR, 0) ?: 0.0f,
        sphereColorG = vars.readMicroFloatAt(VARID_SPHERE_COLOR, 4) ?: 0.75f,
        sphereColorB = vars.readMicroFloatAt(VARID_SPHERE_COLOR, 8) ?: 0.75f,
    )
}
