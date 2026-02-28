package ccr.server.defs

import ccr.server.core.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Basic micro-chunk readers are in ccr.physics.defs (readMicroInt, readMicroFloat,
// readMicroBool, readMicroString, readMicroFloatAt, readMicroVector3).
// The functions below require server.level types and remain in the server module.

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
    return ccr.server.level.OBBox(basis, ccr.math.Vector3(cx, cy, cz), ccr.math.Vector3(ex, ey, ez))
}

fun ChunkReader.readMicroSphere(id: Int): ccr.server.level.Sphere? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 16) return null
    val bb = ByteBuffer.wrap(bytes, 0, 16).order(ByteOrder.LITTLE_ENDIAN)
    val cx = bb.float; val cy = bb.float; val cz = bb.float
    val radius = bb.float
    return ccr.server.level.Sphere(ccr.math.Vector3(cx, cy, cz), radius)
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
