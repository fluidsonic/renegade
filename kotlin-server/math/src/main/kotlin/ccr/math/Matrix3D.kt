package ccr.math

import kotlin.math.cos
import kotlin.math.sin

// C++: Matrix3D in WWMath/matrix3d.h
// Row-major 3x4 transformation matrix (3 rows of 4 floats: rotation + translation)
data class Matrix3D(
    // Row 0: right vector + tx
    var m00: Float = 1f, var m01: Float = 0f, var m02: Float = 0f, var m03: Float = 0f,
    // Row 1: forward vector + ty
    var m10: Float = 0f, var m11: Float = 1f, var m12: Float = 0f, var m13: Float = 0f,
    // Row 2: up vector + tz
    var m20: Float = 0f, var m21: Float = 0f, var m22: Float = 1f, var m23: Float = 0f,
) {
    val translation: Vector3 get() = Vector3(m03, m13, m23)

    fun transformPoint(v: Vector3) = Vector3(
        x = m00 * v.x + m01 * v.y + m02 * v.z + m03,
        y = m10 * v.x + m11 * v.y + m12 * v.z + m13,
        z = m20 * v.x + m21 * v.y + m22 * v.z + m23,
    )

    companion object {
        val IDENTITY = Matrix3D()

        fun fromQuaternion(q: Quaternion): Matrix3D {
            val x2 = q.x + q.x
            val y2 = q.y + q.y
            val z2 = q.z + q.z
            val xx = q.x * x2; val xy = q.x * y2; val xz = q.x * z2
            val yy = q.y * y2; val yz = q.y * z2; val zz = q.z * z2
            val wx = q.w * x2; val wy = q.w * y2; val wz = q.w * z2
            return Matrix3D(
                m00 = 1f - (yy + zz), m01 = xy - wz, m02 = xz + wy,
                m10 = xy + wz, m11 = 1f - (xx + zz), m12 = yz - wx,
                m20 = xz - wy, m21 = yz + wx, m22 = 1f - (xx + yy),
            )
        }

        fun translation(t: Vector3) = Matrix3D(m03 = t.x, m13 = t.y, m23 = t.z)
    }
}
