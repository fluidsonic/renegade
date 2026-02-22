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

fun ChunkReader.readMicroVector3(id: Int): ccr.server.level.Vector3? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 12) return null
    val bb = ByteBuffer.wrap(bytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
    return ccr.server.level.Vector3(bb.float, bb.float, bb.float)
}

fun ChunkReader.readMicroMatrix3D(id: Int): ccr.server.level.Matrix3D? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 48) return null
    val bb = ByteBuffer.wrap(bytes, 0, 48).order(ByteOrder.LITTLE_ENDIAN)
    val elems = FloatArray(12) { bb.float }
    return ccr.server.level.Matrix3D(elems)
}

fun ChunkReader.readMicroOBBox(id: Int): ccr.server.level.OBBox? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 60) return null
    val bb = ByteBuffer.wrap(bytes, 0, 60).order(ByteOrder.LITTLE_ENDIAN)
    val basis = FloatArray(9) { bb.float }
    val cx = bb.float; val cy = bb.float; val cz = bb.float
    val ex = bb.float; val ey = bb.float; val ez = bb.float
    return ccr.server.level.OBBox(basis, ccr.server.level.Vector3(cx, cy, cz), ccr.server.level.Vector3(ex, ey, ez))
}

fun ChunkReader.readMicroSphere(id: Int): ccr.server.level.Sphere? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 16) return null
    val bb = ByteBuffer.wrap(bytes, 0, 16).order(ByteOrder.LITTLE_ENDIAN)
    val cx = bb.float; val cy = bb.float; val cz = bb.float
    val radius = bb.float
    return ccr.server.level.Sphere(ccr.server.level.Vector3(cx, cy, cz), radius)
}

fun ChunkReader.readMicroWideString(id: Int): String? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 2) return ""
    // UTF-16LE: find null terminator (two zero bytes)
    var endIdx = bytes.size
    var i = 0
    while (i + 1 < bytes.size) {
        if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()) { endIdx = i; break }
        i += 2
    }
    return String(bytes, 0, endIdx, Charsets.UTF_16LE)
}
