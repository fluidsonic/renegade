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

    @Test
    fun `times scalar`() {
        val q = Quaternion(1f, 2f, 3f, 4f)
        val result = q * 2f
        assertTrue(abs(result.x - 2f) < 1e-6f)
        assertTrue(abs(result.y - 4f) < 1e-6f)
        assertTrue(abs(result.z - 6f) < 1e-6f)
        assertTrue(abs(result.w - 8f) < 1e-6f)
    }

    @Test
    fun `plus quaternion`() {
        val a = Quaternion(1f, 2f, 3f, 4f)
        val b = Quaternion(5f, 6f, 7f, 8f)
        val result = a + b
        assertTrue(abs(result.x - 6f) < 1e-6f)
        assertTrue(abs(result.y - 8f) < 1e-6f)
        assertTrue(abs(result.z - 10f) < 1e-6f)
        assertTrue(abs(result.w - 12f) < 1e-6f)
    }

    @Test
    fun `dot product`() {
        val a = Quaternion(1f, 0f, 0f, 0f)
        val b = Quaternion(1f, 0f, 0f, 0f)
        assertTrue(abs(a.dot(b) - 1f) < 1e-6f)
    }

    @Test
    fun `dot product identity with itself is 1`() {
        val q = Quaternion.IDENTITY
        assertTrue(abs(q.dot(q) - 1f) < 1e-6f)
    }

    @Test
    fun `slerp at 0 returns this`() {
        val a = Quaternion.IDENTITY
        val b = Quaternion(0f, 0f, 0.7071068f, 0.7071068f).normalized()
        val result = a.slerp(b, 0f)
        assertTrue(abs(result.x - a.x) < 1e-5f)
        assertTrue(abs(result.y - a.y) < 1e-5f)
        assertTrue(abs(result.z - a.z) < 1e-5f)
        assertTrue(abs(result.w - a.w) < 1e-5f)
    }

    @Test
    fun `slerp at 1 returns other`() {
        val a = Quaternion.IDENTITY
        val b = Quaternion(0f, 0f, 0.7071068f, 0.7071068f).normalized()
        val result = a.slerp(b, 1f)
        assertTrue(abs(result.x - b.x) < 1e-5f, "x: expected ${b.x} but got ${result.x}")
        assertTrue(abs(result.y - b.y) < 1e-5f, "y: expected ${b.y} but got ${result.y}")
        assertTrue(abs(result.z - b.z) < 1e-5f, "z: expected ${b.z} but got ${result.z}")
        assertTrue(abs(result.w - b.w) < 1e-5f, "w: expected ${b.w} but got ${result.w}")
    }

    @Test
    fun `slerp identity with itself returns identity`() {
        val q = Quaternion.IDENTITY
        val result = q.slerp(q, 0.5f)
        assertTrue(abs(result.x) < 1e-5f)
        assertTrue(abs(result.y) < 1e-5f)
        assertTrue(abs(result.z) < 1e-5f)
        assertTrue(abs(result.w - 1f) < 1e-5f)
    }
}
