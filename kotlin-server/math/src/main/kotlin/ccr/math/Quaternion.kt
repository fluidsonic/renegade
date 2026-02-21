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

    operator fun times(scalar: Float) = Quaternion(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun plus(other: Quaternion) = Quaternion(x + other.x, y + other.y, z + other.z, w + other.w)
    fun dot(other: Quaternion): Float = x * other.x + y * other.y + z * other.z + w * other.w

    fun slerp(other: Quaternion, t: Float): Quaternion {
        var cosHalfAngle = dot(other)
        val target = if (cosHalfAngle < 0f) {
            cosHalfAngle = -cosHalfAngle
            Quaternion(-other.x, -other.y, -other.z, -other.w)
        } else other
        if (cosHalfAngle >= 1.0f) return this
        val halfAngle = kotlin.math.acos(cosHalfAngle)
        val sinHalfAngle = kotlin.math.sqrt(1f - cosHalfAngle * cosHalfAngle)
        return if (kotlin.math.abs(sinHalfAngle) < 0.001f) {
            Quaternion(x * 0.5f + target.x * 0.5f, y * 0.5f + target.y * 0.5f, z * 0.5f + target.z * 0.5f, w * 0.5f + target.w * 0.5f)
        } else {
            val ratioA = kotlin.math.sin((1f - t) * halfAngle) / sinHalfAngle
            val ratioB = kotlin.math.sin(t * halfAngle) / sinHalfAngle
            Quaternion(x * ratioA + target.x * ratioB, y * ratioA + target.y * ratioB, z * ratioA + target.z * ratioB, w * ratioA + target.w * ratioB)
        }
    }

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
    }
}
