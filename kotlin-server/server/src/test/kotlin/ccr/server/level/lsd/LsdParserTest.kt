package ccr.server.level.lsd

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LsdParserTest {

    @Test
    fun `parse empty data returns default LevelStaticData`() {
        val result = LsdParser.parse(ByteArray(0))
        assertNull(result.worldExtents)
        assertNull(result.sunlightColor)
        assertNull(result.ambientLightColor)
        assertEquals(emptyList(), result.staticObjects)
        assertEquals(emptyList(), result.staticLights)
        assertNull(result.pathfinding)
        assertNull(result.background)
        assertNull(result.weather)
        assertEquals(emptyList(), result.staticSounds)
    }

    @Test
    fun `parse too-small data returns default LevelStaticData without crashing`() {
        // 4 bytes is too small for a valid chunk header (8 bytes minimum)
        val result = LsdParser.parse(ByteArray(4))
        assertNull(result.worldExtents)
        assertNull(result.sunlightColor)
        assertNull(result.ambientLightColor)
    }

    @Test
    fun `parse sunlight and ambient from synthetic LSD`() {
        // Build a minimal LSD with sunlight + ambient chunks.
        //
        // Structure:
        //   [0x00020000] PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM (container)
        //     [0x04433220] PSDSSC_CHUNKID_SCENE (container)
        //       [0x00004800] PSCENE_SD_CHUNK_SUNLIGHT (data: IOSunLightStruct 28 bytes)
        //       [0x00004820] PSCENE_SD_CHUNK_VARIABLES (data: micro-chunk 0x00 + Vector3 12 bytes)

        // IOSunLightStruct: Enabled(u32) + Yaw(f32) + Pitch(f32) + Intensity(f32) + Color(3×f32)
        val sunlightData = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(1)           // Enabled
            putFloat(45.0f)     // Yaw
            putFloat(-30.0f)    // Pitch
            putFloat(0.8f)      // Intensity
            putFloat(1.0f)      // Color.X
            putFloat(0.9f)      // Color.Y
            putFloat(0.7f)      // Color.Z
        }.array()

        // Micro-chunk: type(u8) + size(u8) + data(12 bytes for Vector3)
        val ambientVec = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).apply {
            putFloat(0.2f)
            putFloat(0.3f)
            putFloat(0.4f)
        }.array()
        val variablesData = ByteArray(2 + ambientVec.size).also { buf ->
            buf[0] = 0x00  // micro-chunk type = PSCENE_SD_VARIABLE_AMBIENT
            buf[1] = ambientVec.size.toByte()
            ambientVec.copyInto(buf, 2)
        }

        val sunlightChunk = buildChunk(0x00004800u, sunlightData, isContainer = false)
        val variablesChunk = buildChunk(0x00004820u, variablesData, isContainer = false)
        val sceneData = sunlightChunk + variablesChunk
        val sceneChunk = buildChunk(0x04433220u, sceneData, isContainer = true)
        val subsystemChunk = buildChunk(0x00020000u, sceneChunk, isContainer = true)

        val result = LsdParser.parse(subsystemChunk)

        val sun = assertNotNull(result.sunlightColor)
        assertEquals(1.0f, sun.x)
        assertEquals(0.9f, sun.y)
        assertEquals(0.7f, sun.z)

        val ambient = assertNotNull(result.ambientLightColor)
        assertEquals(0.2f, ambient.x)
        assertEquals(0.3f, ambient.y)
        assertEquals(0.4f, ambient.z)
    }

    /** Helper: builds a chunk header + data bytes. */
    private fun buildChunk(chunkType: UInt, data: ByteArray, isContainer: Boolean): ByteArray {
        val sizeBits = data.size or (if (isContainer) 0x80000000.toInt() else 0)
        return ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(chunkType.toInt())
            putInt(sizeBits)
            put(data)
        }.array()
    }
}
