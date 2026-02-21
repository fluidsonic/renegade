package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class QuaternionTest {
    @Test
    fun `identity quaternion`() {
        val q = Quaternion.IDENTITY
        assertTrue(q.isValid())
        assertTrue(abs(q.length() - 1f) < 1e-6f)
    }

    @Test
    fun `conjugate of identity is identity`() {
        val q = Quaternion.IDENTITY.conjugate()
        assertTrue(abs(q.x) < 1e-6f)
        assertTrue(abs(q.y) < 1e-6f)
        assertTrue(abs(q.z) < 1e-6f)
        assertTrue(abs(q.w - 1f) < 1e-6f)
    }

    @Test
    fun `multiplication by identity returns same`() {
        val q = Quaternion(0.5f, 0.5f, 0.5f, 0.5f).normalized()
        val result = q * Quaternion.IDENTITY
        assertTrue(abs(result.x - q.x) < 1e-6f)
        assertTrue(abs(result.y - q.y) < 1e-6f)
        assertTrue(abs(result.z - q.z) < 1e-6f)
        assertTrue(abs(result.w - q.w) < 1e-6f)
    }
}
