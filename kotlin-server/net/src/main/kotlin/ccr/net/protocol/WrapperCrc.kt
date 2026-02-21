package ccr.net.protocol

// C++: WRAPPER_CRC in packetmgr.cpp
// Every UDP datagram is prefixed with a 4-byte CRC so corrupt packets can be discarded.
//
// Wire format: [CRC: 4 bytes LE][PacketCombiner data]
//
// CRC computation (packetmgr.cpp lines 896-908):
//   crc = CRC::Memory(payload)      // standard CRC32 (poly 0xEDB88320)
//   crc = bswap(crc)                // reverse byte order
//   prepend crc as LE uint32
//
// Java's java.util.zip.CRC32 matches CRC::Memory exactly.

object WrapperCrc {

    private const val SIZE = 4

    /**
     * Prepends the 4-byte CRC wrapper to [data], returning a new ByteArray.
     */
    fun prepend(data: ByteArray): ByteArray {
        val crc = compute(data, 0, data.size)
        val result = ByteArray(SIZE + data.size)
        result[0] = (crc and 0xFF).toByte()
        result[1] = ((crc ushr 8) and 0xFF).toByte()
        result[2] = ((crc ushr 16) and 0xFF).toByte()
        result[3] = ((crc ushr 24) and 0xFF).toByte()
        System.arraycopy(data, 0, result, SIZE, data.size)
        return result
    }

    /**
     * Returns true if the first 4 bytes of [data] match the CRC of the remaining bytes.
     */
    fun verify(data: ByteArray, length: Int): Boolean {
        if (length < SIZE) return false
        val stored = readLeInt(data, 0)
        val expected = compute(data, SIZE, length - SIZE)
        return stored == expected
    }

    // -------------------------------------------------------------------------

    /** CRC32(data[offset..offset+length-1]), then byte-swapped (bswap). */
    private fun compute(data: ByteArray, offset: Int, length: Int): Int {
        val crc32 = java.util.zip.CRC32()
        crc32.update(data, offset, length)
        return Integer.reverseBytes(crc32.value.toInt())
    }

    private fun readLeInt(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        ((data[offset + 2].toInt() and 0xFF) shl 16) or
        ((data[offset + 3].toInt() and 0xFF) shl 24)
}
