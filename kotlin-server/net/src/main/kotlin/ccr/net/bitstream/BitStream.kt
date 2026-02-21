package ccr.net.bitstream

import ccr.math.Quaternion
import ccr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder

// C++: BitStreamClass in wwbitpack/bitstream.h/.cpp
// Typed add/get operations on top of BitPacker.
// Compression uses EncoderRegistry when an encoder type is specified.

class BitStream : BitPacker() {

    // Uncompressed size for statistics (not used for wire protocol)
    var uncompressedSizeBytes: Int = 0
        private set

    val compressedSizeBytes: Int get() = (bitWritePosition + 7) / 8

    // ---- Bool (1 bit when compression enabled) ----

    fun addBool(value: Boolean) {
        if (EncoderRegistry.isCompressionEnabled) {
            addBits(if (value) 1L else 0L, 1)
        } else {
            addBits(if (value) 1L else 0L, 8) // sizeof(bool) = 1 byte in C++
        }
        uncompressedSizeBytes += 1
    }

    fun getBool(): Boolean {
        val bits = if (EncoderRegistry.isCompressionEnabled) getBits(1) else getBits(8)
        return bits == 1L
    }

    // ---- Byte (8 bits) ----

    fun addByte(value: Byte, encoderType: Int = NO_ENCODER) {
        internalAdd(value.toLong() and 0xFF, 8, encoderType, 1)
    }

    fun getByte(encoderType: Int = NO_ENCODER): Byte {
        return internalGet(8, encoderType, 1).toByte()
    }

    // ---- Short / UShort (16 bits) ----

    fun addShort(value: Short, encoderType: Int = NO_ENCODER) {
        internalAdd(value.toLong() and 0xFFFF, 16, encoderType, 2)
    }

    fun getShort(encoderType: Int = NO_ENCODER): Short {
        return internalGet(16, encoderType, 2).toShort()
    }

    // ---- Int (32 bits) ----

    fun addInt(value: Int, encoderType: Int = NO_ENCODER) {
        internalAdd(value.toLong() and 0xFFFFFFFFL, 32, encoderType, 4)
    }

    fun getInt(encoderType: Int = NO_ENCODER): Int {
        return internalGet(32, encoderType, 4).toInt()
    }

    // ---- Float (32 bits, IEEE 754) ----

    fun addFloat(value: Float, encoderType: Int = NO_ENCODER) {
        val bits = java.lang.Float.floatToRawIntBits(value).toLong() and 0xFFFFFFFFL
        if (EncoderRegistry.isCompressionEnabled && encoderType != NO_ENCODER) {
            val entry = EncoderRegistry.getEntry(encoderType)
            val scaled = entry.scale(value.toDouble())
            addBits(scaled, entry.bitPrecision)
        } else {
            addBits(bits, 32)
        }
        uncompressedSizeBytes += 4
    }

    fun getFloat(encoderType: Int = NO_ENCODER): Float {
        return if (EncoderRegistry.isCompressionEnabled && encoderType != NO_ENCODER) {
            val entry = EncoderRegistry.getEntry(encoderType)
            val scaled = getBits(entry.bitPrecision)
            uncompressedSizeBytes += 4
            entry.unscale(scaled).toFloat()
        } else {
            uncompressedSizeBytes += 4
            java.lang.Float.intBitsToFloat(getBits(32).toInt())
        }
    }

    // ---- Raw data (no encoding, byte-aligned) ----
    // C++: Add_Raw_Data / Get_Raw_Data — writes each byte as an unencoded char

    fun addRawData(data: ByteArray, size: Int = data.size) {
        for (i in 0 until size) {
            addByte(data[i])
        }
    }

    fun getRawData(size: Int): ByteArray {
        val buf = ByteArray(size)
        for (i in 0 until size) {
            buf[i] = getByte()
        }
        return buf
    }

    // ---- Null-terminated string (ASCII) ----
    // C++: Add_Terminated_String / Get_Terminated_String
    // Format: [length:USHORT][chars...]

    fun addTerminatedString(s: String, permitEmpty: Boolean = false) {
        require(permitEmpty || s.isNotEmpty()) { "Empty string not permitted" }
        val len = s.length.toShort()
        addShort(len)
        for (c in s) addByte(c.code.toByte())
    }

    fun getTerminatedString(permitEmpty: Boolean = false): String {
        val len = getShort().toInt() and 0xFFFF
        require(permitEmpty || len > 0) { "Empty string not permitted" }
        val sb = StringBuilder(len)
        repeat(len) { sb.append(getByte().toInt().toChar()) }
        return sb.toString()
    }

    // ---- Null-terminated wide string (UTF-16LE / WCHAR) ----
    // C++: Add_Wide_Terminated_String / Get_Wide_Terminated_String
    // Format: [length:USHORT][wchars as USHORT each]
    // Note: C++ WCHAR is 2 bytes (UTF-16 LE on Windows), transmitted raw via Add(USHORT)

    fun addWideString(s: String, permitEmpty: Boolean = false) {
        require(permitEmpty || s.isNotEmpty()) { "Empty wide string not permitted" }
        val len = s.length.toShort()
        addShort(len)
        for (c in s) {
            addShort(c.code.toShort())
        }
    }

    fun getWideString(permitEmpty: Boolean = false): String {
        val len = getShort().toInt() and 0xFFFF
        require(permitEmpty || len > 0) { "Empty wide string not permitted" }
        val sb = StringBuilder(len)
        repeat(len) { sb.append(getShort().toInt().toChar()) }
        return sb.toString()
    }

    // ---- Vector3 (3 floats) ----
    // C++: cPacket::Add_Vector3 / Get_Vector3

    fun addVector3(v: Vector3, encoderType: Int = NO_ENCODER) {
        addFloat(v.x, encoderType)
        addFloat(v.y, encoderType)
        addFloat(v.z, encoderType)
    }

    fun getVector3(encoderType: Int = NO_ENCODER) = Vector3(
        x = getFloat(encoderType),
        y = getFloat(encoderType),
        z = getFloat(encoderType),
    )

    // ---- Quaternion (4 floats: x, y, z, w) ----
    // C++: cPacket::Add_Quaternion / Get_Quaternion

    fun addQuaternion(q: Quaternion, encoderType: Int = NO_ENCODER) {
        addFloat(q.x, encoderType)
        addFloat(q.y, encoderType)
        addFloat(q.z, encoderType)
        addFloat(q.w, encoderType)
    }

    fun getQuaternion(encoderType: Int = NO_ENCODER) = Quaternion(
        x = getFloat(encoderType),
        y = getFloat(encoderType),
        z = getFloat(encoderType),
        w = getFloat(encoderType),
    )

    // ---- Internal helpers ----

    private fun internalAdd(rawBits: Long, fullBits: Int, encoderType: Int, byteSize: Int) {
        if (EncoderRegistry.isCompressionEnabled && encoderType != NO_ENCODER) {
            val entry = EncoderRegistry.getEntry(encoderType)
            val scaled = entry.scale(rawBits.toDouble())
            addBits(scaled, entry.bitPrecision)
        } else {
            addBits(rawBits, fullBits)
        }
        uncompressedSizeBytes += byteSize
    }

    private fun internalGet(fullBits: Int, encoderType: Int, byteSize: Int): Long {
        uncompressedSizeBytes += byteSize
        return if (EncoderRegistry.isCompressionEnabled && encoderType != NO_ENCODER) {
            val entry = EncoderRegistry.getEntry(encoderType)
            val scaled = getBits(entry.bitPrecision)
            entry.unscale(scaled).toLong()
        } else {
            getBits(fullBits)
        }
    }

    companion object {
        const val NO_ENCODER = -1
    }
}
