package ccr.math

import kotlin.math.sqrt

// C++: Quaternion in WWMath/quat.h
data class Quaternion(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var w: Float = 1f,
) {
    fun isValid(): Boolean = x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()

    fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

    fun normalized(): Quaternion {
        val len = length()
        return if (len > 0f) Quaternion(x / len, y / len, z / len, w / len) else IDENTITY
    }

    // Hamilton product
    operator fun times(other: Quaternion) = Quaternion(
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
        w = w * other.w - x * other.x - y * other.y - z * other.z,
    )

    fun conjugate() = Quaternion(-x, -y, -z, w)

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
    }
}
