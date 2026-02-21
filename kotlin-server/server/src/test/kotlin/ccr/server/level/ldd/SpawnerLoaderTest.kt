package ccr.server.level.ldd

import ccr.server.level.ChunkIds
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SpawnerLoaderTest {

    /** Build a chunk header: 4-byte chunkId (LE) + 4-byte size (LE, bit31 set if container). */
    private fun chunkHeader(id: UInt, size: Int, isContainer: Boolean): ByteArray {
        val sizeField = if (isContainer) size or (1 shl 31) else size
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(id.toInt())
            .putInt(sizeField)
            .array()
    }

    private fun microInt(id: Int, value: Int): ByteArray {
        val data = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).also { it.putInt(value) }.array()
        return byteArrayOf(id.toByte(), 4) + data
    }

    private fun microBool(id: Int, value: Boolean): ByteArray {
        return byteArrayOf(id.toByte(), 1, if (value) 1 else 0)
    }

    /** Build a 48-byte Matrix3D with given position (x,y,z) and identity rotation. */
    private fun microMatrix(id: Int, x: Float, y: Float, z: Float): ByteArray {
        val bb = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN)
        // Row 0: [1, 0, 0, x]
        bb.putFloat(1f); bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(x)
        // Row 1: [0, 1, 0, y]
        bb.putFloat(0f); bb.putFloat(1f); bb.putFloat(0f); bb.putFloat(y)
        // Row 2: [0, 0, 1, z]
        bb.putFloat(0f); bb.putFloat(0f); bb.putFloat(1f); bb.putFloat(z)
        return byteArrayOf(id.toByte(), 48) + bb.array()
    }

    private fun microString(id: Int, value: String): ByteArray {
        val bytes = (value + "\u0000").toByteArray(Charsets.ISO_8859_1)
        return byteArrayOf(id.toByte(), bytes.size.toByte()) + bytes
    }

    @Test
    fun `parse spawner with id, definitionId, and transform`() {
        // Build VARIABLES micro-chunks
        val vars = microInt(1, 42) +                         // id = 42
            microMatrix(2, 10f, 20f, 30f) +                  // transform at (10,20,30)
            microInt(3, 100) +                                // definitionId = 100
            microInt(4, 5) +                                  // spawnCount = 5
            microBool(6, true) +                              // enabled
            microString(10, "MyScript") +                     // scriptName
            microString(11, "param1=val1")                    // scriptParams

        // Wrap in SPAWNER_CHUNKID_VARIABLES chunk
        val varsChunk = chunkHeader(ChunkIds.SPAWNER_CHUNKID_VARIABLES, vars.size, isContainer = false) + vars

        // Wrap in SPAWNER_CHUNKID_PARENT chunk (container)
        val parentChunk = chunkHeader(ChunkIds.SPAWNER_CHUNKID_PARENT, varsChunk.size, isContainer = true) + varsChunk

        val spawners = SpawnerLoader.load(ChunkReader(parentChunk))

        assertEquals(1, spawners.size)
        val s = spawners[0]
        assertEquals(42, s.id)
        assertEquals(100, s.definitionId)
        assertEquals(5, s.spawnCount)
        assertEquals(true, s.enabled)
        assertEquals(10f, s.transform.position.x)
        assertEquals(20f, s.transform.position.y)
        assertEquals(30f, s.transform.position.z)
        assertEquals(1, s.scripts.size)
        assertEquals("MyScript", s.scripts[0].name)
        assertEquals("param1=val1", s.scripts[0].params)
    }

    @Test
    fun `empty spawners chunk returns empty list`() {
        val spawners = SpawnerLoader.load(ChunkReader(ByteArray(0)))
        assertEquals(0, spawners.size)
    }

    @Test
    fun `spawner with spawn points`() {
        val vars = microInt(1, 1) +
            microMatrix(2, 0f, 0f, 0f) +
            microInt(3, 50) +
            microInt(4, 1) +
            microBool(6, true) +
            microMatrix(7, 5f, 10f, 15f) +       // spawn point 1
            microMatrix(7, 25f, 30f, 35f)         // spawn point 2

        val varsChunk = chunkHeader(ChunkIds.SPAWNER_CHUNKID_VARIABLES, vars.size, isContainer = false) + vars
        val parentChunk = chunkHeader(ChunkIds.SPAWNER_CHUNKID_PARENT, varsChunk.size, isContainer = true) + varsChunk

        val spawners = SpawnerLoader.load(ChunkReader(parentChunk))

        assertEquals(1, spawners.size)
        val s = spawners[0]
        assertEquals(2, s.spawnPoints.size)
        assertEquals(5f, s.spawnPoints[0].position.x)
        assertEquals(10f, s.spawnPoints[0].position.y)
        assertEquals(15f, s.spawnPoints[0].position.z)
        assertEquals(25f, s.spawnPoints[1].position.x)
        assertEquals(30f, s.spawnPoints[1].position.y)
        assertEquals(35f, s.spawnPoints[1].position.z)
    }
}
