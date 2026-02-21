package ccr.server.level

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Parse 12 bytes (3 × float32 LE) into a Vector3. */
fun ByteArray.toVector3(offset: Int = 0): Vector3 {
    require(size >= offset + 12) { "Need at least ${offset + 12} bytes for Vector3, got $size" }
    val bb = ByteBuffer.wrap(this, offset, 12).order(ByteOrder.LITTLE_ENDIAN)
    return Vector3(bb.float, bb.float, bb.float)
}

/** Parse 48 bytes (12 × float32 LE) into a Matrix3D. */
fun ByteArray.toMatrix3D(offset: Int = 0): Matrix3D {
    require(size >= offset + 48) { "Need at least ${offset + 48} bytes for Matrix3D, got $size" }
    val bb = ByteBuffer.wrap(this, offset, 48).order(ByteOrder.LITTLE_ENDIAN)
    val elems = FloatArray(12) { bb.float }
    return Matrix3D(elems)
}

/** Parse 4 bytes as a little-endian int32. */
fun ByteArray.toInt32(offset: Int = 0): Int {
    require(size >= offset + 4) { "Need at least ${offset + 4} bytes for Int32, got $size" }
    return ByteBuffer.wrap(this, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
}

/** Parse 4 bytes as a little-endian float32. */
fun ByteArray.toFloat32(offset: Int = 0): Float {
    require(size >= offset + 4) { "Need at least ${offset + 4} bytes for Float32, got $size" }
    return ByteBuffer.wrap(this, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
}
