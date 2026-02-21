package ccr.math

import kotlin.math.sqrt

// C++: Vector3 in WWMath/vector3.h
data class Vector3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
) {
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z
    fun normalized(): Vector3 { val len = length(); return if (len > 0f) Vector3(x / len, y / len, z / len) else ZERO }

    fun isValid(): Boolean = x.isFinite() && y.isFinite() && z.isFinite()

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Vector3(-x, -y, -z)

    fun lerp(other: Vector3, t: Float) = Vector3(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t)

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
    }
}
