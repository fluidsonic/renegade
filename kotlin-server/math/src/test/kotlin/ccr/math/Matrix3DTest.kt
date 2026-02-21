package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class Matrix3DTest {
    @Test
    fun `rotateVector with identity returns same vector`() {
        val v = Vector3(1f, 2f, 3f)
        val result = Matrix3D.IDENTITY.rotateVector(v)
        assertTrue(abs(result.x - v.x) < 1e-6f)
        assertTrue(abs(result.y - v.y) < 1e-6f)
        assertTrue(abs(result.z - v.z) < 1e-6f)
    }

    @Test
    fun `rotateVector ignores translation`() {
        val m = Matrix3D(m03 = 100f, m13 = 200f, m23 = 300f)
        val v = Vector3(1f, 0f, 0f)
        val result = m.rotateVector(v)
        assertTrue(abs(result.x - 1f) < 1e-6f)
        assertTrue(abs(result.y) < 1e-6f)
        assertTrue(abs(result.z) < 1e-6f)
    }

    @Test
    fun `withTranslation replaces translation`() {
        val m = Matrix3D.IDENTITY
        val t = Vector3(5f, 6f, 7f)
        val result = m.withTranslation(t)
        assertTrue(abs(result.m03 - 5f) < 1e-6f)
        assertTrue(abs(result.m13 - 6f) < 1e-6f)
        assertTrue(abs(result.m23 - 7f) < 1e-6f)
        // rotation part unchanged
        assertTrue(abs(result.m00 - 1f) < 1e-6f)
        assertTrue(abs(result.m11 - 1f) < 1e-6f)
        assertTrue(abs(result.m22 - 1f) < 1e-6f)
    }

    @Test
    fun `getZRotation of identity is zero`() {
        assertTrue(abs(Matrix3D.IDENTITY.getZRotation()) < 1e-6f)
    }
}
