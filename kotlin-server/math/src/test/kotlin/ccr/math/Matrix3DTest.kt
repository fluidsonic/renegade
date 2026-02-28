package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
    assertTrue(abs(actual - expected) < eps, "Expected $expected but got $actual (eps=$eps)")
}

class Matrix3DTest {
    // -------------------------------------------------------------------------
    // Matrix3D * Matrix3D multiplication
    // -------------------------------------------------------------------------

    @Test
    fun `identity times identity is identity`() {
        val result = Matrix3D.IDENTITY * Matrix3D.IDENTITY
        assertEquals(Matrix3D.IDENTITY, result)
    }

    @Test
    fun `identity times matrix returns same matrix`() {
        val m = Matrix3D(
            m00 = 1f, m01 = 2f, m02 = 3f, m03 = 4f,
            m10 = 5f, m11 = 6f, m12 = 7f, m13 = 8f,
            m20 = 9f, m21 = 10f, m22 = 11f, m23 = 12f,
        )
        val result = Matrix3D.IDENTITY * m
        assertEquals(m, result)
    }

    @Test
    fun `matrix times identity returns same matrix`() {
        val m = Matrix3D(
            m00 = 1f, m01 = 2f, m02 = 3f, m03 = 4f,
            m10 = 5f, m11 = 6f, m12 = 7f, m13 = 8f,
            m20 = 9f, m21 = 10f, m22 = 11f, m23 = 12f,
        )
        val result = m * Matrix3D.IDENTITY
        assertEquals(m, result)
    }

    @Test
    fun `translation times translation adds translations`() {
        val t1 = Matrix3D.translation(Vector3(1f, 2f, 3f))
        val t2 = Matrix3D.translation(Vector3(4f, 5f, 6f))
        val result = t1 * t2
        assertNear(5f, result.m03)
        assertNear(7f, result.m13)
        assertNear(9f, result.m23)
        // Rotation part should still be identity
        assertNear(1f, result.m00); assertNear(0f, result.m01); assertNear(0f, result.m02)
        assertNear(0f, result.m10); assertNear(1f, result.m11); assertNear(0f, result.m12)
        assertNear(0f, result.m20); assertNear(0f, result.m21); assertNear(1f, result.m22)
    }

    @Test
    fun `matrix multiplication satisfies known numeric result`() {
        // A: rotation-like matrix
        val a = Matrix3D(
            m00 = 0f, m01 = -1f, m02 = 0f, m03 = 1f,
            m10 = 1f, m11 =  0f, m12 = 0f, m13 = 2f,
            m20 = 0f, m21 =  0f, m22 = 1f, m23 = 3f,
        )
        // B: translation only
        val b = Matrix3D.translation(Vector3(10f, 20f, 30f))
        // result.translation = a * (10,20,30) + (1,2,3)
        // a * (10,20,30) = (0*10 + -1*20 + 0*30, 1*10 + 0*20 + 0*30, 0*10 + 0*20 + 1*30) = (-20, 10, 30)
        // + (1,2,3) = (-19, 12, 33)
        val result = a * b
        assertNear(-19f, result.m03)
        assertNear(12f,  result.m13)
        assertNear(33f,  result.m23)
        // Rotation part: a.rotation * identity = a.rotation
        assertNear(0f,  result.m00); assertNear(-1f, result.m01); assertNear(0f, result.m02)
        assertNear(1f,  result.m10); assertNear( 0f, result.m11); assertNear(0f, result.m12)
        assertNear(0f,  result.m20); assertNear( 0f, result.m21); assertNear(1f, result.m22)
    }


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
