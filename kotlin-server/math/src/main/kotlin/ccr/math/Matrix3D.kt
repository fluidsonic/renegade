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

    fun rotateVector(v: Vector3) = Vector3(
        x = m00 * v.x + m01 * v.y + m02 * v.z,
        y = m10 * v.x + m11 * v.y + m12 * v.z,
        z = m20 * v.x + m21 * v.y + m22 * v.z,
    )

    /**
     * C++: Matrix3D::Inverse_Rotate_Vector(const Vector3& vect) const
     *
     * Rotates a vector by the TRANSPOSE of the 3×3 rotation sub-matrix.
     * Equivalent to multiplying by the inverse of the rotation when the matrix is orthonormal.
     *
     * C++ implementation (matrix3d.cpp):
     *   result.x = Row[0][0]*v.x + Row[1][0]*v.y + Row[2][0]*v.z
     *   result.y = Row[0][1]*v.x + Row[1][1]*v.y + Row[2][1]*v.z
     *   result.z = Row[0][2]*v.x + Row[1][2]*v.y + Row[2][2]*v.z
     */
    fun inverseRotateVector(v: Vector3) = Vector3(
        x = m00 * v.x + m10 * v.y + m20 * v.z,
        y = m01 * v.x + m11 * v.y + m21 * v.z,
        z = m02 * v.x + m12 * v.y + m22 * v.z,
    )

    operator fun times(v: Vector3): Vector3 = Vector3(
        m00 * v.x + m01 * v.y + m02 * v.z + m03,
        m10 * v.x + m11 * v.y + m12 * v.z + m13,
        m20 * v.x + m21 * v.y + m22 * v.z + m23,
    )

    // C++: Matrix3D::Multiply(const Matrix3D& A, const Matrix3D& B, Matrix3D* AresMask)
    // 3x4 row-major multiplication (implicit 4th row = [0,0,0,1]).
    operator fun times(b: Matrix3D): Matrix3D = Matrix3D(
        m00 = m00 * b.m00 + m01 * b.m10 + m02 * b.m20,
        m01 = m00 * b.m01 + m01 * b.m11 + m02 * b.m21,
        m02 = m00 * b.m02 + m01 * b.m12 + m02 * b.m22,
        m03 = m00 * b.m03 + m01 * b.m13 + m02 * b.m23 + m03,
        m10 = m10 * b.m00 + m11 * b.m10 + m12 * b.m20,
        m11 = m10 * b.m01 + m11 * b.m11 + m12 * b.m21,
        m12 = m10 * b.m02 + m11 * b.m12 + m12 * b.m22,
        m13 = m10 * b.m03 + m11 * b.m13 + m12 * b.m23 + m13,
        m20 = m20 * b.m00 + m21 * b.m10 + m22 * b.m20,
        m21 = m20 * b.m01 + m21 * b.m11 + m22 * b.m21,
        m22 = m20 * b.m02 + m21 * b.m12 + m22 * b.m22,
        m23 = m20 * b.m03 + m21 * b.m13 + m22 * b.m23 + m23,
    )

    fun withTranslation(t: Vector3) = copy(m03 = t.x, m13 = t.y, m23 = t.z)

    fun getZRotation(): Float = kotlin.math.atan2(m10, m00)

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
