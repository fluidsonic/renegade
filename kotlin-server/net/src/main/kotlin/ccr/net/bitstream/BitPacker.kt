package ccr.net.bitstream

// C++: cBitPacker in wwbitpack/BitPacker.h/.cpp
// Ported from the Jani 02-14-2002 optimized rewrite (MSB-first bit ordering).
// Note: old version used LSB-first — the two are not compatible. We use the new version.
//
// MAX_BUFFER_SIZE = 548 (MTU 576 - 20 IP header - 8 UDP header)
const val MAX_BUFFER_SIZE = 548

open class BitPacker {
    val buffer = ByteArray(MAX_BUFFER_SIZE)

    // Independent read and write positions, both in bits
    var bitWritePosition: Int = 0
        private set
    var bitReadPosition: Int = 0
        private set

    fun reset() {
        bitWritePosition = 0
        bitReadPosition = 0
        buffer.fill(0)
    }

    fun flush() {
        bitReadPosition = bitWritePosition
    }

    val isFlushed: Boolean get() = bitReadPosition == bitWritePosition

    // C++: Set_Bit_Write_Position — used when constructing a packet from received raw bytes
    fun setBitWritePosition(position: Int) {
        require(position <= MAX_BUFFER_SIZE * 8) { "position $position exceeds buffer capacity" }
        bitWritePosition = position
    }

    // C++: Add_Bits — MSB-first encoding (Jani's 02-14-2002 optimized rewrite)
    fun addBits(value: Long, numBits: Int) {
        require(numBits in 1..32) { "numBits must be 1..32, was $numBits" }
        require(bitWritePosition + numBits <= MAX_BUFFER_SIZE * 8) {
            "write overflow: pos=$bitWritePosition + bits=$numBits > ${MAX_BUFFER_SIZE * 8}"
        }

        var byteNum = bitWritePosition ushr 3
        val bitOffset = bitWritePosition and 0x7
        bitWritePosition += numBits

        // Shift value to MSB of a 32-bit word
        var remaining = numBits
        var v = (value.toInt()) shl (32 - numBits)

        // Fill the remaining bits of the current byte first
        if (bitOffset != 0) {
            val bitCount = minOf(8 - bitOffset, remaining)
            val bitValue = v.ushr(24 + bitOffset).toByte()
            buffer[byteNum] = (buffer[byteNum].toInt() or bitValue.toInt()).toByte()
            byteNum++
            v = v shl bitCount
            remaining -= bitCount
        }

        // Write remaining full bytes and partial last byte
        while (remaining > 0) {
            buffer[byteNum] = v.ushr(24).toByte()
            byteNum++
            v = v shl 8
            remaining -= 8
        }
    }

    // C++: Get_Bits — reads numBits bits MSB-first
    fun getBits(numBits: Int): Long {
        require(numBits in 1..32) { "numBits must be 1..32, was $numBits" }
        require(bitReadPosition + numBits <= bitWritePosition) {
            "read overflow: readPos=$bitReadPosition + bits=$numBits > writePos=$bitWritePosition"
        }

        val readLen = numBits
        var byteNum = bitReadPosition / 8
        val bitOffset = bitReadPosition and 0x7
        bitReadPosition += numBits

        var bitCount = 8 - bitOffset
        if (bitCount > numBits) bitCount = numBits

        var value = ((buffer[byteNum++].toInt() and 0xFF shl bitOffset) and 0xFF).toLong() shl 24
        var remaining = numBits - bitCount

        var shift = 24 - bitCount
        while (shift > 0) {
            value = value or ((buffer[byteNum++].toInt() and 0xFF).toLong() shl shift)
            shift -= 8
            remaining -= 8
        }
        if (remaining > 0) {
            value = value or ((buffer[byteNum].toInt() and 0xFF).toLong() ushr (-shift))
        }

        value = value ushr (32 - readLen)
        return value and 0xFFFFFFFFL
    }
}
