package ccr.math

data class Matrix3(
    var m00: Float = 1f, var m01: Float = 0f, var m02: Float = 0f,
    var m10: Float = 0f, var m11: Float = 1f, var m12: Float = 0f,
    var m20: Float = 0f, var m21: Float = 0f, var m22: Float = 1f,
) {
    operator fun times(v: Vector3) = Vector3(
        m00 * v.x + m01 * v.y + m02 * v.z,
        m10 * v.x + m11 * v.y + m12 * v.z,
        m20 * v.x + m21 * v.y + m22 * v.z,
    )

    operator fun times(other: Matrix3) = Matrix3(
        m00 * other.m00 + m01 * other.m10 + m02 * other.m20, m00 * other.m01 + m01 * other.m11 + m02 * other.m21, m00 * other.m02 + m01 * other.m12 + m02 * other.m22,
        m10 * other.m00 + m11 * other.m10 + m12 * other.m20, m10 * other.m01 + m11 * other.m11 + m12 * other.m21, m10 * other.m02 + m11 * other.m12 + m12 * other.m22,
        m20 * other.m00 + m21 * other.m10 + m22 * other.m20, m20 * other.m01 + m21 * other.m11 + m22 * other.m21, m20 * other.m02 + m21 * other.m12 + m22 * other.m22,
    )

    operator fun times(s: Float) = Matrix3(
        m00 * s, m01 * s, m02 * s,
        m10 * s, m11 * s, m12 * s,
        m20 * s, m21 * s, m22 * s,
    )

    fun transpose() = Matrix3(m00, m10, m20, m01, m11, m21, m02, m12, m22)

    fun inverse(): Matrix3 {
        val det = m00 * (m11 * m22 - m12 * m21) - m01 * (m10 * m22 - m12 * m20) + m02 * (m10 * m21 - m11 * m20)
        val invDet = if (kotlin.math.abs(det) < 1e-12f) 0f else 1f / det
        return Matrix3(
            (m11 * m22 - m12 * m21) * invDet, (m02 * m21 - m01 * m22) * invDet, (m01 * m12 - m02 * m11) * invDet,
            (m12 * m20 - m10 * m22) * invDet, (m00 * m22 - m02 * m20) * invDet, (m02 * m10 - m00 * m12) * invDet,
            (m10 * m21 - m11 * m20) * invDet, (m01 * m20 - m00 * m21) * invDet, (m00 * m11 - m01 * m10) * invDet,
        )
    }

    companion object {
        val IDENTITY = Matrix3()

        fun fromQuaternion(q: Quaternion): Matrix3 {
            val m = Matrix3D.fromQuaternion(q)
            return Matrix3(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20, m.m21, m.m22)
        }
    }
}
