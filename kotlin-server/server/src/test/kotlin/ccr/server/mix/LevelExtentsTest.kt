package ccr.server.mix

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals

/**
 * Tests for extractLevelExtents — navigates the .lsd chunk hierarchy to find AABTree root bounds.
 *
 * Chunk ID constants (matching LevelExtents.kt):
 *   0x00020000  PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM
 *   0x04433220  PSDSSC_CHUNKID_SCENE
 *   0x00004500  PSCENE_SD_CHUNK_STATIC_OBJECT_AABTREE
 *   0x00000104  STATICAABTREE_CHUNK_AABTREE_CLASS_DATA
 *   0x00000001  AABTREE_CHUNK_VERSION
 *   0x00000101  AABTREE_CHUNK_AABNODE
 *   0x00000104  AABTREE_CHUNK_AABNODE_VARIABLES (new format)
 *   0x00000102  AABTREE_CHUNK_AABNODE_INFO      (legacy format)
 *   micro 0x00  AABTREE_VARIABLE_NODESTRUCT
 */
class LevelExtentsTest {

    // IOAABNodeStruct = Center(3×float) + Extent(3×float) + Attributes(uint32) = 28 bytes
    private fun buildNodeStruct(
        cx: Float, cy: Float, cz: Float,
        ex: Float, ey: Float, ez: Float,
        attrs: Int = 0,
    ): ByteArray {
        val buf = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(cx); buf.putFloat(cy); buf.putFloat(cz)
        buf.putFloat(ex); buf.putFloat(ey); buf.putFloat(ez)
        buf.putInt(attrs)
        return buf.array()
    }

    // ---- Byte-building helpers (same as ChunkReaderTest) ----

    private fun leInt(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte(),
    )

    private fun leUInt(v: Long) = leInt(v.toInt())

    private fun chunkHeader(type: Long, dataSize: Int, isContainer: Boolean = false): ByteArray {
        val sizeWord = if (isContainer) (dataSize.toLong() or 0x80000000L) else dataSize.toLong()
        return leUInt(type) + leUInt(sizeWord)
    }

    private fun chunk(type: Long, data: ByteArray, isContainer: Boolean = false): ByteArray =
        chunkHeader(type, data.size, isContainer) + data

    private fun microChunk(type: Int, data: ByteArray): ByteArray =
        byteArrayOf(type.toByte(), data.size.toByte()) + data

    /**
     * Builds a minimal .lsd byte array with the new (AABNODE_VARIABLES) format.
     *
     * Path:
     *   [0x00020000] static-data subsystem
     *     [0x04433220] scene
     *       [0x00004500] static-object aabtree
     *         [0x00000104] aabtree class-data
     *           [0x00000001] version → 0x00010000
     *           [0x00000101] aabnode (root)
     *             [0x00000104] aabnode-variables (container with micro-chunks)
     *               micro[0x00] → IOAABNodeStruct (28 bytes)
     */
    private fun buildLsd(
        cx: Float, cy: Float, cz: Float,
        ex: Float, ey: Float, ez: Float,
        useLegacyFormat: Boolean = false,
    ): ByteArray {
        val nodeStruct = buildNodeStruct(cx, cy, cz, ex, ey, ez)

        val nodeDescChunk = if (!useLegacyFormat) {
            // New format: AABNODE_VARIABLES container with micro-chunk
            val microNodeStruct = microChunk(0x00, nodeStruct)
            chunk(0x00000104L, microNodeStruct, isContainer = false) // micro-chunks, but NOT set as container
        } else {
            // Legacy format: AABNODE_INFO with raw bytes
            chunk(0x00000102L, nodeStruct)
        }

        // AABNODE also needs AABNODE_CONTENTS (even if empty, we include it for spec compliance)
        val nodeContents = chunk(0x00000103L, ByteArray(0))

        // AABNODE wrapper
        val aabnodeData = nodeDescChunk + nodeContents
        val aabnodeChunk = chunk(0x00000101L, aabnodeData, isContainer = true)

        // AABTREE_VERSION chunk
        val versionData = leInt(0x00010000) // AABTREE_CURRENT_VERSION
        val versionChunk = chunk(0x00000001L, versionData)

        // AABTREE class-data
        val classData = versionChunk + aabnodeChunk
        val classDataChunk = chunk(0x00000104L, classData, isContainer = true)

        // STATIC_OBJECT_AABTREE
        val aabtreeChunk = chunk(0x00004500L, classDataChunk, isContainer = true)

        // PSDSSC_SCENE
        val sceneChunk = chunk(0x04433220L, aabtreeChunk, isContainer = true)

        // STATIC_DATA_SUBSYSTEM
        return chunk(0x00020000L, sceneChunk, isContainer = true)
    }

    @Test
    fun `extracts world extents using new AABNODE_VARIABLES format`() {
        val lsd = buildLsd(cx = 100f, cy = 200f, cz = 50f, ex = 150f, ey = 250f, ez = 80f)
        val extents = extractLevelExtents(lsd)

        assertEquals(-50f, extents.minX)   // 100 - 150
        assertEquals(-50f, extents.minY)   // 200 - 250
        assertEquals(-30f, extents.minZ)   // 50 - 80
        assertEquals(250f, extents.maxX)   // 100 + 150
        assertEquals(450f, extents.maxY)   // 200 + 250
        assertEquals(130f, extents.maxZ)   // 50 + 80
    }

    @Test
    fun `extracts world extents using legacy AABNODE_INFO format`() {
        val lsd = buildLsd(cx = 0f, cy = 0f, cz = 0f, ex = 500f, ey = 500f, ez = 100f, useLegacyFormat = true)
        val extents = extractLevelExtents(lsd)

        assertEquals(-500f, extents.minX)
        assertEquals(-500f, extents.minY)
        assertEquals(-100f, extents.minZ)
        assertEquals(500f, extents.maxX)
        assertEquals(500f, extents.maxY)
        assertEquals(100f, extents.maxZ)
    }

    @Test
    fun `WorldExtents min and max are derived from Center and Extent`() {
        val cx = 10f; val cy = 20f; val cz = 30f
        val ex = 5f;  val ey = 8f;  val ez = 15f
        val lsd = buildLsd(cx, cy, cz, ex, ey, ez)
        val extents = extractLevelExtents(lsd)

        assertEquals(cx - ex, extents.minX)
        assertEquals(cy - ey, extents.minY)
        assertEquals(cz - ez, extents.minZ)
        assertEquals(cx + ex, extents.maxX)
        assertEquals(cy + ey, extents.maxY)
        assertEquals(cz + ez, extents.maxZ)
    }

    @Test
    fun `throws on empty data`() {
        assertThrows<Exception> { extractLevelExtents(ByteArray(0)) }
    }

    @Test
    fun `throws when top-level chunk is missing`() {
        // Use wrong chunk type at top level
        val wrongChunk = chunk(0xDEADBEEFL, ByteArray(4))
        assertThrows<Exception> { extractLevelExtents(wrongChunk) }
    }

    @Test
    fun `throws on wrong AABTree version`() {
        // Build lsd with wrong version number
        val nodeStruct = buildNodeStruct(0f, 0f, 0f, 1f, 1f, 1f)
        val microNodeStruct = microChunk(0x00, nodeStruct)
        val variablesChunk = chunk(0x00000104L, microNodeStruct)
        val nodeContents = chunk(0x00000103L, ByteArray(0))
        val aabnodeChunk = chunk(0x00000101L, variablesChunk + nodeContents, isContainer = true)

        val badVersion = leInt(0x00009999) // wrong version
        val versionChunk = chunk(0x00000001L, badVersion)
        val classData = versionChunk + aabnodeChunk
        val classDataChunk = chunk(0x00000104L, classData, isContainer = true)
        val aabtreeChunk = chunk(0x00004500L, classDataChunk, isContainer = true)
        val sceneChunk = chunk(0x04433220L, aabtreeChunk, isContainer = true)
        val lsd = chunk(0x00020000L, sceneChunk, isContainer = true)

        assertThrows<Exception> { extractLevelExtents(lsd) }
    }
}
