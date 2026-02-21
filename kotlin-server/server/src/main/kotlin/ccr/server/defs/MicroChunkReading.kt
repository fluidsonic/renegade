package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ChunkReader.readMicroInt(id: Int): Int? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 4) return null
    return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
}

fun ChunkReader.readMicroFloat(id: Int): Float? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 4) return null
    return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float
}

fun ChunkReader.readMicroBool(id: Int): Boolean? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.isEmpty()) return null
    return bytes[0] != 0.toByte()
}

fun ChunkReader.readMicroString(id: Int): String? {
    val bytes = findMicroChunk(id) ?: return null
    val nullIndex = bytes.indexOfFirst { it == 0.toByte() }
    val len = if (nullIndex < 0) bytes.size else nullIndex
    return String(bytes, 0, len, Charsets.ISO_8859_1)
}

fun ChunkReader.readMicroFloatAt(id: Int, byteOffset: Int): Float? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < byteOffset + 4) return null
    return ByteBuffer.wrap(bytes, byteOffset, 4).order(ByteOrder.LITTLE_ENDIAN).float
}
