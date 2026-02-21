package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class Matrix3Test {
    @Test
    fun `identity times vector returns same vector`() {
        val v = Vector3(1f, 2f, 3f)
        val result = Matrix3.IDENTITY * v
        assertTrue(abs(result.x - v.x) < 1e-6f)
        assertTrue(abs(result.y - v.y) < 1e-6f)
        assertTrue(abs(result.z - v.z) < 1e-6f)
    }

    @Test
    fun `fromQuaternion identity produces identity matrix`() {
        val m = Matrix3.fromQuaternion(Quaternion.IDENTITY)
        assertTrue(abs(m.m00 - 1f) < 1e-6f)
        assertTrue(abs(m.m11 - 1f) < 1e-6f)
        assertTrue(abs(m.m22 - 1f) < 1e-6f)
        assertTrue(abs(m.m01) < 1e-6f)
        assertTrue(abs(m.m02) < 1e-6f)
        assertTrue(abs(m.m10) < 1e-6f)
        assertTrue(abs(m.m12) < 1e-6f)
        assertTrue(abs(m.m20) < 1e-6f)
        assertTrue(abs(m.m21) < 1e-6f)
    }

    @Test
    fun `transpose of identity is identity`() {
        val t = Matrix3.IDENTITY.transpose()
        assertTrue(abs(t.m00 - 1f) < 1e-6f)
        assertTrue(abs(t.m11 - 1f) < 1e-6f)
        assertTrue(abs(t.m22 - 1f) < 1e-6f)
        assertTrue(abs(t.m01) < 1e-6f)
        assertTrue(abs(t.m10) < 1e-6f)
    }

    @Test
    fun `inverse of identity is identity`() {
        val inv = Matrix3.IDENTITY.inverse()
        assertTrue(abs(inv.m00 - 1f) < 1e-6f)
        assertTrue(abs(inv.m11 - 1f) < 1e-6f)
        assertTrue(abs(inv.m22 - 1f) < 1e-6f)
        assertTrue(abs(inv.m01) < 1e-6f)
        assertTrue(abs(inv.m10) < 1e-6f)
    }

    @Test
    fun `identity times identity is identity`() {
        val result = Matrix3.IDENTITY * Matrix3.IDENTITY
        assertTrue(abs(result.m00 - 1f) < 1e-6f)
        assertTrue(abs(result.m11 - 1f) < 1e-6f)
        assertTrue(abs(result.m22 - 1f) < 1e-6f)
        assertTrue(abs(result.m01) < 1e-6f)
        assertTrue(abs(result.m10) < 1e-6f)
    }

    @Test
    fun `scalar multiplication`() {
        val m = Matrix3.IDENTITY * 2f
        assertTrue(abs(m.m00 - 2f) < 1e-6f)
        assertTrue(abs(m.m11 - 2f) < 1e-6f)
        assertTrue(abs(m.m22 - 2f) < 1e-6f)
    }
}
