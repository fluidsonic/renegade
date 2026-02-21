package ccr.net.bitstream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

// Tests verify exact byte-level output against expected sequences derived from
// the C++ cBitPacker (Jani's 02-14-2002 MSB-first rewrite).

class BitPackerTest {

    @Test
    fun `single byte MSB first - write 8 bits of 0xAB`() {
        val p = BitPacker()
        p.addBits(0xABL, 8)
        assertEquals(0xAB.toByte(), p.buffer[0])
        assertEquals(8, p.bitWritePosition)
    }

    @Test
    fun `single nibble high bits - write 4 bits of 0xA`() {
        val p = BitPacker()
        p.addBits(0xAL, 4)
        // 0xA = 1010, written MSB-first into high bits of first byte → 1010_0000 = 0xA0
        assertEquals(0xA0.toByte(), p.buffer[0])
        assertEquals(4, p.bitWritePosition)
    }

    @Test
    fun `two 4-bit nibbles pack into one byte`() {
        val p = BitPacker()
        p.addBits(0xAL, 4)  // high nibble
        p.addBits(0xBL, 4)  // low nibble
        // Should produce 0xAB
        assertEquals(0xAB.toByte(), p.buffer[0])
        assertEquals(8, p.bitWritePosition)
    }

    @Test
    fun `write across byte boundary - 12 bits spanning 2 bytes`() {
        val p = BitPacker()
        p.addBits(0xABCL, 12)
        // 0xABC = 1010_1011_1100
        // Byte 0: 1010_1011 = 0xAB
        // Byte 1: 1100_0000 = 0xC0
        assertEquals(0xAB.toByte(), p.buffer[0])
        assertEquals(0xC0.toByte(), p.buffer[1])
    }

    @Test
    fun `round trip - 1 bit`() {
        val p = BitPacker()
        p.addBits(1L, 1)
        p.addBits(0L, 1)
        p.addBits(1L, 1)
        assertEquals(1L, p.getBits(1))
        assertEquals(0L, p.getBits(1))
        assertEquals(1L, p.getBits(1))
    }

    @Test
    fun `round trip - 32 bit int`() {
        val p = BitPacker()
        val value = 0x12345678L
        p.addBits(value, 32)
        assertEquals(value, p.getBits(32))
    }

    @Test
    fun `round trip - multiple values with different bit widths`() {
        val p = BitPacker()
        p.addBits(0b101L, 3)
        p.addBits(0xFFFF_FFFFL, 32)
        p.addBits(7L, 4)
        p.addBits(0x123456L, 24)

        assertEquals(0b101L, p.getBits(3))
        assertEquals(0xFFFF_FFFFL, p.getBits(32))
        assertEquals(7L, p.getBits(4))
        assertEquals(0x123456L, p.getBits(24))
    }

    @Test
    fun `4-bit type + 28-bit id matches packet header encoding`() {
        // This is the critical packet header encoding test:
        // Type = 4 bits, Id = 28 bits, together = first 4 bytes
        val p = BitPacker()
        val type = 4L   // PACKETTYPE_CONNECT_CS
        val id = 0L     // first packet

        p.addBits(type, 4)
        p.addBits(id, 28)

        // First 4 bytes: 4 bits type + 28 bits id
        // type=4 (0100), id=0 (28 zeros)
        // Result: 0100_0000 0000_0000 0000_0000 0000_0000 = 0x40 0x00 0x00 0x00
        assertEquals(0x40.toByte(), p.buffer[0])
        assertEquals(0x00.toByte(), p.buffer[1])
        assertEquals(0x00.toByte(), p.buffer[2])
        assertEquals(0x00.toByte(), p.buffer[3])

        assertEquals(type, p.getBits(4))
        assertEquals(id, p.getBits(28))
    }

    @Test
    fun `setBitWritePosition allows reading externally received data`() {
        val p = BitPacker()
        p.buffer[0] = 0xAB.toByte()
        p.buffer[1] = 0xCD.toByte()
        p.setBitWritePosition(16)

        assertEquals(0xABL, p.getBits(8))
        assertEquals(0xCDL, p.getBits(8))
    }

    @Test
    fun `flush makes stream appear empty`() {
        val p = BitPacker()
        p.addBits(42L, 8)
        p.flush()
        assert(p.isFlushed)
    }

    @Test
    fun `overflow on write throws`() {
        val p = BitPacker()
        p.setBitWritePosition(MAX_BUFFER_SIZE * 8)
        assertFails { p.addBits(1L, 1) }
    }
}
