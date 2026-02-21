package ccr.server.level.lsd

import ccr.server.level.toFloat32
import ccr.server.level.toInt32
import ccr.server.mix.ChunkReader

/**
 * Parses BackgroundMgrClass data from a CHUNKID_BACKGROUND_MGR (0x00040126) chunk.
 *
 * Structure (backgroundmgr.h / backgroundmgr.cpp):
 *   Sub-chunk MICRO_CHUNKS (0x11080732):
 *     Micro-chunk 0x14 → hours (uint32)
 *     Micro-chunk 0x15 → minutes (uint32)
 *     Micro-chunk 0x16 → lightSourceType (uint32)
 *     Micro-chunk 0x17 → moonType (uint32)
 *   Sub-chunk DYNAMIC_MICRO_CHUNKS (0x11020216):
 *     Each parameter has 6 micro-chunks (CurrentValue, NormalValue, NormalTarget, NormalDuration,
 *     OverrideTarget, OverrideDuration). We only read the CurrentValue for each.
 *     Micro-chunk 0x24 → skyTintFactor.CurrentValue (float)
 *     Micro-chunk 0x18 → cloudCover.CurrentValue (float)
 *     Micro-chunk 0x1E → cloudGloominess.CurrentValue (float)
 *     Micro-chunk 0x2A → lightningIntensity.CurrentValue (float)
 *     Micro-chunk 0x30 → lightningStartDistance.CurrentValue (float)
 *     Micro-chunk 0x36 → lightningEndDistance.CurrentValue (float)
 *     Micro-chunk 0x3C → lightningHeading.CurrentValue (float)
 *     Micro-chunk 0x42 → lightningDistribution.CurrentValue (float)
 */
object BackgroundLoader {

    private const val CHUNKID_MICRO_CHUNKS = 0x11080732u
    private const val CHUNKID_DYNAMIC_MICRO_CHUNKS = 0x11020216u

    // Static micro-chunk IDs (backgroundmgr.h enum, starting at 0x14)
    private const val VARID_TIME_HOURS = 0x14
    private const val VARID_TIME_MINUTES = 0x15
    private const val VARID_LIGHT_SOURCE_TYPE = 0x16
    private const val VARID_MOON_TYPE = 0x17

    // Dynamic parameter CurrentValue micro-chunk IDs
    // Each VARID_PARAMETER(X) generates 6 sequential IDs; _CURRENT_VALUE is the first.
    private const val VARID_CLOUD_COVER_CURRENT = 0x18
    private const val VARID_CLOUD_GLOOMINESS_CURRENT = 0x1E
    private const val VARID_SKY_TINT_FACTOR_CURRENT = 0x24
    private const val VARID_LIGHTNING_INTENSITY_CURRENT = 0x2A
    private const val VARID_LIGHTNING_START_DISTANCE_CURRENT = 0x30
    private const val VARID_LIGHTNING_END_DISTANCE_CURRENT = 0x36
    private const val VARID_LIGHTNING_HEADING_CURRENT = 0x3C
    private const val VARID_LIGHTNING_DISTRIBUTION_CURRENT = 0x42

    fun load(bgChunk: ChunkReader): BackgroundData {
        var timeHours = 12
        var timeMinutes = 0
        var lightSourceType = 0
        var moonType = 0
        var skyTintFactor = 1.0f
        var cloudCover = 0.0f
        var cloudGloominess = 0.0f
        var lightningIntensity = 0.0f
        var lightningStartDistance = 0.0f
        var lightningEndDistance = 0.0f
        var lightningHeading = 0.0f
        var lightningDistribution = 0.0f

        bgChunk.forEachChunk { id, _, reader ->
            when (id) {
                CHUNKID_MICRO_CHUNKS -> {
                    reader.findMicroChunk(VARID_TIME_HOURS)?.let { timeHours = it.toInt32() }
                    reader.findMicroChunk(VARID_TIME_MINUTES)?.let { timeMinutes = it.toInt32() }
                    reader.findMicroChunk(VARID_LIGHT_SOURCE_TYPE)?.let { lightSourceType = it.toInt32() }
                    reader.findMicroChunk(VARID_MOON_TYPE)?.let { moonType = it.toInt32() }
                }
                CHUNKID_DYNAMIC_MICRO_CHUNKS -> {
                    reader.findMicroChunk(VARID_SKY_TINT_FACTOR_CURRENT)?.let { skyTintFactor = it.toFloat32() }
                    reader.findMicroChunk(VARID_CLOUD_COVER_CURRENT)?.let { cloudCover = it.toFloat32() }
                    reader.findMicroChunk(VARID_CLOUD_GLOOMINESS_CURRENT)?.let { cloudGloominess = it.toFloat32() }
                    reader.findMicroChunk(VARID_LIGHTNING_INTENSITY_CURRENT)?.let { lightningIntensity = it.toFloat32() }
                    reader.findMicroChunk(VARID_LIGHTNING_START_DISTANCE_CURRENT)?.let { lightningStartDistance = it.toFloat32() }
                    reader.findMicroChunk(VARID_LIGHTNING_END_DISTANCE_CURRENT)?.let { lightningEndDistance = it.toFloat32() }
                    reader.findMicroChunk(VARID_LIGHTNING_HEADING_CURRENT)?.let { lightningHeading = it.toFloat32() }
                    reader.findMicroChunk(VARID_LIGHTNING_DISTRIBUTION_CURRENT)?.let { lightningDistribution = it.toFloat32() }
                }
            }
        }

        return BackgroundData(
            skyTintFactor = skyTintFactor,
            cloudCover = cloudCover,
            cloudGloominess = cloudGloominess,
            lightningIntensity = lightningIntensity,
            lightningStartDistance = lightningStartDistance,
            lightningEndDistance = lightningEndDistance,
            lightningHeading = lightningHeading,
            lightningDistribution = lightningDistribution,
            timeHours = timeHours,
            timeMinutes = timeMinutes,
            lightSourceType = lightSourceType,
            moonType = moonType,
        )
    }
}
