package ccr.server.level.lsd

import ccr.server.level.toFloat32
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses WeatherMgrClass data from a CHUNKID_WEATHER_MGR (0x00040800) chunk.
 *
 * Structure (WeatherMgr.h / WeatherMgr.cpp):
 *   Sub-chunk MICRO_CHUNKS (0x03020113): empty (no micro-chunks)
 *   Sub-chunk DYNAMIC_MICRO_CHUNKS (0x11020245):
 *     Each weather parameter has 6 micro-chunks; we only read the CurrentValue.
 *     VARID_DUMMY = 0x09 (placeholder, not used)
 *     Micro-chunk 0x0A → windHeading.CurrentValue (float)
 *     Micro-chunk 0x10 → windSpeed.CurrentValue (float)
 *     Micro-chunk 0x16 → windVariability.CurrentValue (float)
 *     Micro-chunk 0x1C → rainDensity.CurrentValue (float)
 *     Micro-chunk 0x22 → snowDensity.CurrentValue (float)
 *     Micro-chunk 0x28 → ashDensity.CurrentValue (float)
 *     Micro-chunk 0x30 → fogEnabled (bool stored as uint32)
 *     Micro-chunk 0x31 → fogStartDistance.CurrentValue (float)
 *     Micro-chunk 0x37 → fogEndDistance.CurrentValue (float)
 */
object WeatherLoader {

    private const val CHUNKID_DYNAMIC_MICRO_CHUNKS = 0x11020245u

    // Dynamic parameter CurrentValue micro-chunk IDs
    // VARID_DUMMY = 0x09, then each VARID_PARAMETER(X) generates 6 sequential IDs.
    private const val VARID_WIND_HEADING_CURRENT = 0x0A
    private const val VARID_WIND_SPEED_CURRENT = 0x10
    private const val VARID_WIND_VARIABILITY_CURRENT = 0x16
    private const val VARID_RAIN_DENSITY_CURRENT = 0x1C
    private const val VARID_SNOW_DENSITY_CURRENT = 0x22
    private const val VARID_ASH_DENSITY_CURRENT = 0x28
    private const val VARID_FOG_ENABLED = 0x30
    private const val VARID_FOG_START_DISTANCE_CURRENT = 0x31
    private const val VARID_FOG_END_DISTANCE_CURRENT = 0x37

    fun load(weatherChunk: ChunkReader): WeatherData {
        var windHeading = 0.0f
        var windSpeed = 0.0f
        var windVariability = 0.0f
        var rainDensity = 0.0f
        var snowDensity = 0.0f
        var ashDensity = 0.0f
        var fogEnabled = false
        var fogStart = 100.0f
        var fogEnd = 500.0f

        weatherChunk.forEachChunk { id, _, reader ->
            if (id == CHUNKID_DYNAMIC_MICRO_CHUNKS) {
                reader.findMicroChunk(VARID_WIND_HEADING_CURRENT)?.let { windHeading = it.toFloat32() }
                reader.findMicroChunk(VARID_WIND_SPEED_CURRENT)?.let { windSpeed = it.toFloat32() }
                reader.findMicroChunk(VARID_WIND_VARIABILITY_CURRENT)?.let { windVariability = it.toFloat32() }
                reader.findMicroChunk(VARID_RAIN_DENSITY_CURRENT)?.let { rainDensity = it.toFloat32() }
                reader.findMicroChunk(VARID_SNOW_DENSITY_CURRENT)?.let { snowDensity = it.toFloat32() }
                reader.findMicroChunk(VARID_ASH_DENSITY_CURRENT)?.let { ashDensity = it.toFloat32() }
                reader.findMicroChunk(VARID_FOG_ENABLED)?.let {
                    if (it.size >= 4) {
                        fogEnabled = ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).int != 0
                    }
                }
                reader.findMicroChunk(VARID_FOG_START_DISTANCE_CURRENT)?.let { fogStart = it.toFloat32() }
                reader.findMicroChunk(VARID_FOG_END_DISTANCE_CURRENT)?.let { fogEnd = it.toFloat32() }
            }
        }

        return WeatherData(
            windHeading = windHeading,
            windSpeed = windSpeed,
            windVariability = windVariability,
            rainDensity = rainDensity,
            snowDensity = snowDensity,
            ashDensity = ashDensity,
            fogEnabled = fogEnabled,
            fogStart = fogStart,
            fogEnd = fogEnd,
        )
    }
}
