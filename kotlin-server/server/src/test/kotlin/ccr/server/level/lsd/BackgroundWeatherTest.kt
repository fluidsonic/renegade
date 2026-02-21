package ccr.server.level.lsd

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundWeatherTest {

    @Test
    fun `BackgroundData has sensible defaults`() {
        val bg = BackgroundData()
        assertEquals(1.0f, bg.skyTintFactor)
        assertEquals(0.0f, bg.cloudCover)
        assertEquals(0.0f, bg.cloudGloominess)
        assertEquals(0.0f, bg.lightningIntensity)
        assertEquals(12, bg.timeHours)
        assertEquals(0, bg.timeMinutes)
        assertEquals(0, bg.lightSourceType)
        assertEquals(0, bg.moonType)
    }

    @Test
    fun `WeatherData has sensible defaults`() {
        val w = WeatherData()
        assertEquals(0.0f, w.windHeading)
        assertEquals(0.0f, w.windSpeed)
        assertEquals(0.0f, w.rainDensity)
        assertEquals(0.0f, w.snowDensity)
        assertEquals(0.0f, w.ashDensity)
        assertFalse(w.fogEnabled)
        assertEquals(100.0f, w.fogStart)
        assertEquals(500.0f, w.fogEnd)
    }

    @Test
    fun `BackgroundLoader returns defaults for empty chunk`() {
        val result = BackgroundLoader.load(ChunkReader(ByteArray(0)))
        assertEquals(BackgroundData(), result)
    }

    @Test
    fun `WeatherLoader returns defaults for empty chunk`() {
        val result = WeatherLoader.load(ChunkReader(ByteArray(0)))
        assertEquals(WeatherData(), result)
    }

    @Test
    fun `BackgroundLoader parses static micro-chunks`() {
        // Build a BackgroundMgr chunk containing CHUNKID_MICRO_CHUNKS (0x11080732)
        // with micro-chunks for hours=18, minutes=30, lightSourceType=1, moonType=2
        val microChunkData = buildMicroChunks(
            0x14 to intBytes(18),     // VARID_TIME_HOURS
            0x15 to intBytes(30),     // VARID_TIME_MINUTES
            0x16 to intBytes(1),      // VARID_LIGHT_SOURCE_TYPE
            0x17 to intBytes(2),      // VARID_MOON_TYPE
        )
        val bgChunkData = buildChunk(0x11080732u, microChunkData)
        val result = BackgroundLoader.load(ChunkReader(bgChunkData))
        assertEquals(18, result.timeHours)
        assertEquals(30, result.timeMinutes)
        assertEquals(1, result.lightSourceType)
        assertEquals(2, result.moonType)
    }

    @Test
    fun `BackgroundLoader parses dynamic micro-chunks`() {
        // Build CHUNKID_DYNAMIC_MICRO_CHUNKS (0x11020216) with skyTintFactor and cloudCover
        val dynamicData = buildMicroChunks(
            0x24 to floatBytes(0.75f),  // SKY_TINT_FACTOR CurrentValue
            0x18 to floatBytes(0.5f),   // CLOUD_COVER CurrentValue
            0x1E to floatBytes(0.3f),   // CLOUD_GLOOMINESS CurrentValue
        )
        val bgChunkData = buildChunk(0x11020216u, dynamicData)
        val result = BackgroundLoader.load(ChunkReader(bgChunkData))
        assertEquals(0.75f, result.skyTintFactor)
        assertEquals(0.5f, result.cloudCover)
        assertEquals(0.3f, result.cloudGloominess)
    }

    @Test
    fun `WeatherLoader parses dynamic micro-chunks`() {
        // Build CHUNKID_DYNAMIC_MICRO_CHUNKS (0x11020245) with weather params
        val dynamicData = buildMicroChunks(
            0x0A to floatBytes(1.5f),    // WIND_HEADING CurrentValue
            0x10 to floatBytes(10.0f),   // WIND_SPEED CurrentValue
            0x1C to floatBytes(0.8f),    // RAIN_DENSITY CurrentValue
            0x30 to intBytes(1),         // FOG_ENABLED (bool as int)
            0x31 to floatBytes(50.0f),   // FOG_START_DISTANCE CurrentValue
            0x37 to floatBytes(300.0f),  // FOG_END_DISTANCE CurrentValue
        )
        val weatherChunkData = buildChunk(0x11020245u, dynamicData)
        val result = WeatherLoader.load(ChunkReader(weatherChunkData))
        assertEquals(1.5f, result.windHeading)
        assertEquals(10.0f, result.windSpeed)
        assertEquals(0.8f, result.rainDensity)
        assertTrue(result.fogEnabled)
        assertEquals(50.0f, result.fogStart)
        assertEquals(300.0f, result.fogEnd)
    }

    // --- Helpers ---

    /** Builds a chunk: 4-byte type LE + 4-byte size LE (bit 31 = 0, no sub-chunks) + data. */
    private fun buildChunk(type: UInt, data: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(type.toInt())
        buf.putInt(data.size)  // bit 31 = 0 (no sub-chunks flag)
        buf.put(data)
        return buf.array()
    }

    /** Builds a sequence of micro-chunks: each is 1-byte type + 1-byte size + data. */
    private fun buildMicroChunks(vararg entries: Pair<Int, ByteArray>): ByteArray {
        val totalSize = entries.sumOf { 2 + it.second.size }
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        for ((id, data) in entries) {
            buf.put(id.toByte())
            buf.put(data.size.toByte())
            buf.put(data)
        }
        return buf.array()
    }

    private fun intBytes(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun floatBytes(value: Float): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
}
