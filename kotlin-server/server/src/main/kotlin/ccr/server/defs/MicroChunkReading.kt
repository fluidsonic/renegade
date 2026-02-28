package ccr.server.defs

import ccr.physics.defs.readMicroBool as physReadMicroBool
import ccr.physics.defs.readMicroFloat as physReadMicroFloat
import ccr.physics.defs.readMicroFloatAt as physReadMicroFloatAt
import ccr.physics.defs.readMicroInt as physReadMicroInt
import ccr.physics.defs.readMicroString as physReadMicroString
import ccr.physics.defs.readMicroVector3 as physReadMicroVector3
import ccr.server.core.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Re-export the basic micro-chunk readers so server defs files in this package
// don't need explicit imports from ccr.physics.defs.
fun ChunkReader.readMicroInt(id: Int): Int? = physReadMicroInt(id)
fun ChunkReader.readMicroFloat(id: Int): Float? = physReadMicroFloat(id)
fun ChunkReader.readMicroBool(id: Int): Boolean? = physReadMicroBool(id)
fun ChunkReader.readMicroString(id: Int): String? = physReadMicroString(id)
fun ChunkReader.readMicroFloatAt(id: Int, byteOffset: Int): Float? = physReadMicroFloatAt(id, byteOffset)
fun ChunkReader.readMicroVector3(id: Int): ccr.math.Vector3? = physReadMicroVector3(id)

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

fun ChunkReader.readMicroSphere(id: Int): ccr.math.Sphere? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 16) return null
    val bb = ByteBuffer.wrap(bytes, 0, 16).order(ByteOrder.LITTLE_ENDIAN)
    val cx = bb.float; val cy = bb.float; val cz = bb.float
    val radius = bb.float
    return ccr.math.Sphere(ccr.math.Vector3(cx, cy, cz), radius)
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
