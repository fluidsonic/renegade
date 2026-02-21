package ccr.server.level.ldd

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelInfoLoaderTest {

    private fun microString(id: Int, value: String): ByteArray {
        val bytes = (value + "\u0000").toByteArray(Charsets.ISO_8859_1)
        return byteArrayOf(id.toByte(), bytes.size.toByte()) + bytes
    }

    private fun microInt(id: Int, value: Int): ByteArray {
        val data = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).also { it.putInt(value) }.array()
        return byteArrayOf(id.toByte(), 4) + data
    }

    @Test
    fun `parse level info micro-chunks`() {
        val payload = microString(1, "C&C_Field") + microInt(2, 42) + microString(3, "Test level")
        val reader = ChunkReader(payload)
        val info = LevelInfoLoader.load(reader)
        assertEquals("C&C_Field", info.mapFilename)
        assertEquals(42, info.missionDescriptionId)
        assertEquals("Test level", info.description)
    }

    @Test
    fun `empty reader returns defaults`() {
        val info = LevelInfoLoader.load(ChunkReader(ByteArray(0)))
        assertEquals("", info.mapFilename)
        assertEquals(0, info.missionDescriptionId)
        assertEquals("", info.description)
    }
}
