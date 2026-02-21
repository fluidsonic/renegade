package ccr.net.bitstream

import ccr.math.Quaternion
import ccr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BitStreamTest {

    @Test
    fun `bool round trip - true`() {
        val s = BitStream()
        s.addBool(true)
        assertEquals(1, s.bitWritePosition) // 1 bit when compression enabled
        assertEquals(true, s.getBool())
    }

    @Test
    fun `bool round trip - false`() {
        val s = BitStream()
        s.addBool(false)
        assertEquals(false, s.getBool())
    }

    @Test
    fun `byte round trip`() {
        val s = BitStream()
        s.addByte(0x42)
        assertEquals(0x42.toByte(), s.getByte())
    }

    @Test
    fun `short round trip`() {
        val s = BitStream()
        s.addShort(0x1234.toShort())
        assertEquals(0x1234.toShort(), s.getShort())
    }

    @Test
    fun `int round trip`() {
        val s = BitStream()
        s.addInt(0x12345678)
        assertEquals(0x12345678, s.getInt())
    }

    @Test
    fun `int negative round trip`() {
        val s = BitStream()
        s.addInt(-1)
        assertEquals(-1, s.getInt())
    }

    @Test
    fun `float round trip`() {
        val s = BitStream()
        s.addFloat(3.14f)
        assertEquals(3.14f, s.getFloat())
    }

    @Test
    fun `terminated string round trip`() {
        val s = BitStream()
        s.addTerminatedString("hello")
        assertEquals("hello", s.getTerminatedString())
    }

    @Test
    fun `wide string round trip`() {
        val s = BitStream()
        s.addWideString("Player1")
        assertEquals("Player1", s.getWideString())
    }

    @Test
    fun `wide string format - length then chars`() {
        val s = BitStream()
        s.addWideString("AB")
        // Length: 2 as USHORT (16 bits) → 0x0002
        // 'A' = 0x0041 as USHORT
        // 'B' = 0x0042 as USHORT
        assertEquals(2.toShort(), s.getShort()) // length
        assertEquals('A'.code.toShort(), s.getShort()) // A
        assertEquals('B'.code.toShort(), s.getShort()) // B
    }

    @Test
    fun `raw data round trip`() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte())
        val s = BitStream()
        s.addRawData(data)
        val result = s.getRawData(4)
        assertTrue(data.contentEquals(result))
    }

    @Test
    fun `vector3 round trip`() {
        val v = Vector3(1.5f, -2.3f, 100f)
        val s = BitStream()
        s.addVector3(v)
        val result = s.getVector3()
        assertEquals(v.x, result.x)
        assertEquals(v.y, result.y)
        assertEquals(v.z, result.z)
    }

    @Test
    fun `quaternion round trip`() {
        val q = Quaternion(0.5f, 0.5f, 0.5f, 0.5f)
        val s = BitStream()
        s.addQuaternion(q)
        val result = s.getQuaternion()
        assertEquals(q.x, result.x)
        assertEquals(q.y, result.y)
        assertEquals(q.z, result.z)
        assertEquals(q.w, result.w)
    }

    @Test
    fun `mixed types round trip - simulates handshake payload`() {
        val s = BitStream()
        // Simulate CONNECT_CS payload: nickname, password (wide strings), exe_key (int)
        s.addWideString("TestPlayer")
        s.addWideString("", permitEmpty = true)  // password
        s.addInt(0xDEADBEEF.toInt())  // exe_key

        assertEquals("TestPlayer", s.getWideString())
        assertEquals("", s.getWideString(permitEmpty = true))
        assertEquals(0xDEADBEEF.toInt(), s.getInt())
    }

    @Test
    fun `compressed size equals ceil of write position over 8`() {
        val s = BitStream()
        s.addBool(true)   // 1 bit
        s.addBool(false)  // 1 bit
        assertEquals(1, s.compressedSizeBytes) // ceil(2/8) = 1
        s.addByte(0)      // 8 bits
        assertEquals(2, s.compressedSizeBytes) // ceil(10/8) = 2
    }

    @Test
    fun `encoder registry with raw bit count`() {
        EncoderRegistry.setPrecision(0, 4) // encoder 0 = 4 bits
        val s = BitStream()
        s.addByte(7, encoderType = 0) // should encode in 4 bits
        assertEquals(4, s.bitWritePosition)
        assertEquals(7.toByte(), s.getByte(encoderType = 0))
    }
}
