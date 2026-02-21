package ccr.server.level.lsd

import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.level.toMatrix3D
import ccr.server.mix.ChunkReader

/**
 * Loads static sound emitters from the WWAudio subsystem chunk in the LSD file.
 *
 * Chunk hierarchy (AudioSaveLoad.cpp, SoundScene.cpp, Sound3D.cpp, AudibleSound.cpp):
 *   WWAUDIO_STATIC_SAVELOAD (0x00030005) ← subsystem top-level
 *     STATIC_SCENE (0x10291220)
 *       SCENE_VARIABLES (0x00000100) ← min/max extents (skipped)
 *       STATIC_SOUNDS (0x00000101) ← list of persist-factory sound objects
 *         SOUND3D (0x00030003) ← per-sound via SimplePersistFactoryClass
 *           BASE_CLASS (0x11090956) ← AudibleSoundClass
 *             BASE_CLASS (0x00000101) ← SoundSceneObjClass (skipped)
 *             VARIABLES (0x00000100) ← transform, volume, dropoff, filename
 *           VARIABLES (0x11090955) ← maxVolRadius, isStatic
 */
object SoundLoader {

    // AudioSaveLoad.cpp — static scene wrapper
    private const val CHUNKID_STATIC_SCENE = 0x10291220u

    // SoundScene.cpp — sub-chunks within the static scene
    private const val CHUNKID_SCENE_STATIC_SOUNDS = 0x00000101u

    // Sound3D.cpp — chunk IDs inside each Sound3D persist block
    private const val CHUNKID_S3D_VARIABLES = 0x11090955u
    private const val CHUNKID_S3D_BASE_CLASS = 0x11090956u

    // AudibleSound.cpp AUDIBLE_SOUND_SAVELOAD namespace
    private const val CHUNKID_ASC_VARIABLES = 0x00000100u

    // AudibleSoundClass micro-chunk variable IDs (AUDIBLE_SOUND_SAVELOAD)
    private const val VARID_VOLUME = 0x04
    private const val VARID_LOOP_COUNT = 0x06
    private const val VARID_TRANSFORM = 0x0A
    private const val VARID_DROP_OFF = 0x0E
    private const val VARID_FILENAME = 0x0F

    // Sound3D micro-chunk variable IDs
    private const val VARID_MAX_VOL_RADIUS = 0x05

    // Known persist factory chunk IDs for sound types (SoundChunkIDs.h)
    private const val CHUNKID_SOUND3D = 0x00030003u

    fun load(audioChunk: ChunkReader): List<StaticSoundEmitter> {
        val emitters = mutableListOf<StaticSoundEmitter>()

        audioChunk.forEachChunk { chunkId, _, reader ->
            if (chunkId == CHUNKID_STATIC_SCENE) {
                reader.forEachChunk { subId, _, subReader ->
                    if (subId == CHUNKID_SCENE_STATIC_SOUNDS) {
                        loadStaticSounds(subReader, emitters)
                    }
                }
            }
        }

        return emitters
    }

    private fun loadStaticSounds(reader: ChunkReader, emitters: MutableList<StaticSoundEmitter>) {
        reader.forEachChunk { chunkId, _, soundReader ->
            if (chunkId == CHUNKID_SOUND3D) {
                parseSound3D(soundReader)?.let { emitters.add(it) }
            }
            // Other factory types (pseudo-3D, filtered) are rare for static map sounds; skip them.
        }
    }

    private fun parseSound3D(reader: ChunkReader): StaticSoundEmitter? {
        var emitter = StaticSoundEmitter()

        reader.forEachChunk { chunkId, _, subReader ->
            when (chunkId) {
                CHUNKID_S3D_BASE_CLASS -> {
                    emitter = parseAudibleSoundChunks(subReader, emitter)
                }
                CHUNKID_S3D_VARIABLES -> {
                    val maxVol = subReader.readMicroFloat(VARID_MAX_VOL_RADIUS)
                    if (maxVol != null) {
                        emitter = emitter.copy(innerRadius = maxVol)
                    }
                }
            }
        }

        return emitter
    }

    private fun parseAudibleSoundChunks(reader: ChunkReader, base: StaticSoundEmitter): StaticSoundEmitter {
        var result = base

        reader.forEachChunk { chunkId, _, subReader ->
            if (chunkId == CHUNKID_ASC_VARIABLES) {
                val volume = subReader.readMicroFloat(VARID_VOLUME)
                val loopCount = subReader.readMicroInt(VARID_LOOP_COUNT)
                val dropOff = subReader.readMicroFloat(VARID_DROP_OFF)
                val filename = subReader.readMicroString(VARID_FILENAME)

                // Extract position from the 48-byte Matrix3D transform.
                // Translation = elements[3], elements[7], elements[11] (column 3 of each row).
                val transformBytes = subReader.findMicroChunk(VARID_TRANSFORM)
                val position = if (transformBytes != null && transformBytes.size >= 48) {
                    transformBytes.toMatrix3D().position
                } else {
                    null
                }

                result = result.copy(
                    volume = volume ?: result.volume,
                    loop = (loopCount ?: 1) != 1,
                    outerRadius = dropOff ?: result.outerRadius,
                    filename = filename ?: result.filename,
                    position = position ?: result.position,
                )
            }
            // CHUNKID_ASC_BASE_CLASS (0x00000101) = SoundSceneObjClass data — skip
        }

        return result
    }
}
