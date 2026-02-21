package ccr.server.level.lsd

import ccr.server.level.ChunkIds
import ccr.server.level.Vector3
import ccr.server.level.toFloat32
import ccr.server.level.toVector3
import ccr.server.mix.ChunkReader

/**
 * Parses the PSDSSC_CHUNKID_SCENE sub-chunks for sunlight and ambient light colors.
 *
 * Sunlight chunk (PSCENE_SD_CHUNK_SUNLIGHT = 0x4800):
 *   IOSunLightStruct (28 bytes, raw read):
 *     uint32 Enabled
 *     float  Yaw
 *     float  Pitch
 *     float  Intensity
 *     float  Color.X, Color.Y, Color.Z  (IOVector3Struct)
 *
 * Ambient chunk (PSCENE_SD_CHUNK_VARIABLES = 0x4820):
 *   Micro-chunk 0x00 (PSCENE_SD_VARIABLE_AMBIENT) → Vector3 (12 bytes)
 *
 * Source: pscene_saveload.cpp (Load_Level_Static_Data, Load_Sun_Light)
 */
object PhysicsSceneLoader {

    fun loadSunlight(sceneChunk: ChunkReader): Vector3? {
        val sunChunk = sceneChunk.findChunk(ChunkIds.PSCENE_SD_CHUNK_SUNLIGHT) ?: return null
        // IOSunLightStruct: Enabled(4) + Yaw(4) + Pitch(4) + Intensity(4) + Color(12) = 28 bytes
        val bytes = sunChunk.readBytes(28)
        if (bytes.size < 28) return null
        // Color starts at offset 16 (after Enabled+Yaw+Pitch+Intensity)
        return bytes.toVector3(offset = 16)
    }

    fun loadSunlightIntensity(sceneChunk: ChunkReader): Float? {
        val sunChunk = sceneChunk.findChunk(ChunkIds.PSCENE_SD_CHUNK_SUNLIGHT) ?: return null
        val bytes = sunChunk.readBytes(28)
        if (bytes.size < 16) return null
        // Intensity is at offset 12
        return bytes.toFloat32(offset = 12)
    }

    fun loadAmbientLight(sceneChunk: ChunkReader): Vector3? {
        val varsChunk = sceneChunk.findChunk(ChunkIds.PSCENE_SD_CHUNK_VARIABLES) ?: return null
        val ambientBytes = varsChunk.findMicroChunk(ChunkIds.PSCENE_SD_VARIABLE_AMBIENT) ?: return null
        if (ambientBytes.size < 12) return null
        return ambientBytes.toVector3()
    }
}
