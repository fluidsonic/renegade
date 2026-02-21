package ccr.server.level.lsd

import ccr.server.mix.ChunkReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SoundLoaderTest {

    @Test
    fun `StaticSoundEmitter has sensible defaults`() {
        val emitter = StaticSoundEmitter()
        assertTrue(emitter.outerRadius > 0f)
        assertTrue(emitter.volume > 0f)
        assertEquals("", emitter.filename)
    }

    @Test
    fun `SoundLoader returns empty list for empty data`() {
        val result = SoundLoader.load(ChunkReader(ByteArray(0)))
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `SoundLoader returns empty list for unrelated chunk data`() {
        // Build a chunk with an unrelated ID (not CHUNKID_STATIC_SCENE)
        val data = buildChunk(0x12345678u, ByteArray(0))
        val result = SoundLoader.load(ChunkReader(data))
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    /** Helper: builds a single chunk (8-byte header + payload). */
    private fun buildChunk(id: UInt, payload: ByteArray, isContainer: Boolean = false): ByteArray {
        val sizeField = payload.size or (if (isContainer) 0x80000000.toInt() else 0)
        val header = ByteArray(8)
        header[0] = (id.toInt() and 0xFF).toByte()
        header[1] = ((id.toInt() shr 8) and 0xFF).toByte()
        header[2] = ((id.toInt() shr 16) and 0xFF).toByte()
        header[3] = ((id.toInt() shr 24) and 0xFF).toByte()
        header[4] = (sizeField and 0xFF).toByte()
        header[5] = ((sizeField shr 8) and 0xFF).toByte()
        header[6] = ((sizeField shr 16) and 0xFF).toByte()
        header[7] = ((sizeField shr 24) and 0xFF).toByte()
        return header + payload
    }
}
