package ccr.server.mix

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for ChunkReader — reads W3D/LSD chunk-based data.
 *
 * Chunk header (8 bytes, little-endian):
 *   chunkType: uint32
 *   chunkSize: uint32  — bit 31 = has_sub_chunks, bits 30-0 = data size
 *
 * MicroChunk (2-byte header):
 *   type: uint8
 *   size: uint8
 */
class ChunkReaderTest {

    // ---- Byte-building helpers ----

    private fun leInt(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )

    private fun leUInt(v: Long) = leInt(v.toInt())

    /** Build a chunk header: [type:4][size:4] with optional has_sub_chunks flag. */
    private fun chunkHeader(type: Int, dataSize: Int, isContainer: Boolean = false): ByteArray {
        val sizeWord = if (isContainer) (dataSize.toLong() or 0x80000000L) else dataSize.toLong()
        return leInt(type) + leUInt(sizeWord)
    }

    /** Wrap [data] in a chunk with the given type. */
    private fun chunk(type: Int, data: ByteArray, isContainer: Boolean = false): ByteArray =
        chunkHeader(type, data.size, isContainer) + data

    /** Build a micro-chunk: [type:1][size:1][data]. */
    private fun microChunk(type: Int, data: ByteArray): ByteArray =
        byteArrayOf(type.toByte(), data.size.toByte()) + data

    // ---- Tests ----

    @Test
    fun `iterates a single flat chunk`() {
        val payload = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
        val raw = chunk(0x12345678, payload)
        val reader = ChunkReader(raw)
        var count = 0
        reader.forEachChunk { id, _, c ->
            count++
            assertEquals(0x12345678u, id)
            assertEquals(payload.toList(), c.readBytes().toList())
        }
        assertEquals(1, count)
    }

    @Test
    fun `iterates multiple sequential chunks`() {
        val a = chunk(0x0001, byteArrayOf(1))
        val b = chunk(0x0002, byteArrayOf(2))
        val c = chunk(0x0003, byteArrayOf(3))
        val raw = a + b + c
        val reader = ChunkReader(raw)
        val ids = mutableListOf<UInt>()
        reader.forEachChunk { id, _, _ -> ids += id }
        assertEquals(listOf(0x0001u, 0x0002u, 0x0003u), ids)
    }

    @Test
    fun `findChunk returns null for missing chunk`() {
        val raw = chunk(0x0001, byteArrayOf(1))
        assertNull(ChunkReader(raw).findChunk(0x9999u))
    }

    @Test
    fun `findChunk finds by ID`() {
        val raw = chunk(0x0001, byteArrayOf(0xAA.toByte())) +
                  chunk(0x0002, byteArrayOf(0xBB.toByte())) +
                  chunk(0x0003, byteArrayOf(0xCC.toByte()))
        val reader = ChunkReader(raw)
        val found = reader.findChunk(0x0002u)
        assertNotNull(found)
        assertEquals(listOf(0xBB.toByte()), found.readBytes().toList())
    }

    @Test
    fun `nested chunk navigation`() {
        val inner = byteArrayOf(0x42)
        val innerChunk = chunk(0xBBBBu.toInt(), inner)
        val outerData = chunk(0xAAAAu.toInt(), innerChunk, isContainer = true)
        val root = ChunkReader(outerData)

        val outer = root.findChunk(0xAAAAu)
        assertNotNull(outer)
        val innerResult = outer.findChunk(0xBBBBu)
        assertNotNull(innerResult)
        assertEquals(listOf(0x42.toByte()), innerResult.readBytes().toList())
    }

    @Test
    fun `deeply nested chunk navigation`() {
        // depth 3: 0x1000 > 0x2000 > 0x3000 with value 99
        val level3 = chunk(0x3000, byteArrayOf(99), isContainer = false)
        val level2 = chunk(0x2000, level3, isContainer = true)
        val level1 = chunk(0x1000, level2, isContainer = true)
        val root = ChunkReader(level1)

        val l1 = root.findChunk(0x1000u)
        val l2 = l1?.findChunk(0x2000u)
        val l3 = l2?.findChunk(0x3000u)
        assertNotNull(l3)
        assertEquals(99.toByte(), l3.readBytes().first())
    }

    @Test
    fun `micro-chunk extraction`() {
        val mc0 = microChunk(0x00, byteArrayOf(0x11, 0x22))
        val mc1 = microChunk(0x01, byteArrayOf(0x33, 0x44, 0x55))
        val mc2 = microChunk(0x02, byteArrayOf(0x66))
        val data = mc0 + mc1 + mc2
        val reader = ChunkReader(data)

        val found0 = reader.findMicroChunk(0x00)
        val found1 = reader.findMicroChunk(0x01)
        val found2 = reader.findMicroChunk(0x02)
        assertNotNull(found0); assertEquals(listOf(0x11.toByte(), 0x22.toByte()), found0.toList())
        assertNotNull(found1); assertEquals(listOf(0x33.toByte(), 0x44.toByte(), 0x55.toByte()), found1.toList())
        assertNotNull(found2); assertEquals(listOf(0x66.toByte()), found2.toList())
    }

    @Test
    fun `micro-chunk returns null for missing id`() {
        val mc = microChunk(0x01, byteArrayOf(0xFF.toByte()))
        assertNull(ChunkReader(mc).findMicroChunk(0x99))
    }

    @Test
    fun `readInt reads little-endian int32 from chunk data`() {
        // Value 0x01020304 in LE bytes: 04 03 02 01
        val data = byteArrayOf(0x04, 0x03, 0x02, 0x01)
        val raw = chunk(0x0001, data)
        val reader = ChunkReader(raw).findChunk(0x0001u)
        assertNotNull(reader)
        assertEquals(0x01020304, reader.readInt())
    }

    @Test
    fun `readFloat reads little-endian float32 from chunk data`() {
        val value = 3.14f
        val bits = java.lang.Float.floatToRawIntBits(value)
        val data = byteArrayOf(
            (bits and 0xFF).toByte(),
            ((bits shr 8) and 0xFF).toByte(),
            ((bits shr 16) and 0xFF).toByte(),
            ((bits shr 24) and 0xFF).toByte(),
        )
        val raw = chunk(0x0001, data)
        val reader = ChunkReader(raw).findChunk(0x0001u)
        assertNotNull(reader)
        assertEquals(value, reader.readFloat())
    }

    @Test
    fun `isContainer flag is reported correctly`() {
        val containerData = chunk(0x0001, byteArrayOf(1), isContainer = true)
        val dataChunk = chunk(0x0002, byteArrayOf(2), isContainer = false)
        val raw = containerData + dataChunk
        val reader = ChunkReader(raw)
        val flags = mutableMapOf<UInt, Boolean>()
        reader.forEachChunk { id, isContainer, _ -> flags[id] = isContainer }
        assertEquals(true, flags[0x0001u])
        assertEquals(false, flags[0x0002u])
    }
}
