package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Vector3Test {
    @Test
    fun `dot product`() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        assertEquals(32f, a.dot(b))
    }

    @Test
    fun `cross product`() {
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)
        assertEquals(Vector3(0f, 0f, 1f), a.cross(b))
    }

    @Test
    fun `arithmetic operators`() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        assertEquals(Vector3(5f, 7f, 9f), a + b)
        assertEquals(Vector3(-3f, -3f, -3f), a - b)
        assertEquals(Vector3(2f, 4f, 6f), a * 2f)
    }

    @Test
    fun `isValid rejects NaN and Infinity`() {
        assertTrue(Vector3(1f, 2f, 3f).isValid())
        assertTrue(!Vector3(Float.NaN, 0f, 0f).isValid())
        assertTrue(!Vector3(0f, Float.POSITIVE_INFINITY, 0f).isValid())
    }

    @Test
    fun `normalized returns unit vector`() {
        val v = Vector3(3f, 4f, 0f)
        val n = v.normalized()
        assertTrue(abs(n.length() - 1f) < 1e-6f)
        assertTrue(abs(n.x - 0.6f) < 1e-6f)
        assertTrue(abs(n.y - 0.8f) < 1e-6f)
    }

    @Test
    fun `normalized of zero returns zero`() {
        val n = Vector3.ZERO.normalized()
        assertEquals(Vector3.ZERO, n)
    }

    @Test
    fun `lengthSquared`() {
        val v = Vector3(3f, 4f, 0f)
        assertTrue(abs(v.lengthSquared() - 25f) < 1e-6f)
    }

    @Test
    fun `unary minus`() {
        val v = Vector3(1f, -2f, 3f)
        val neg = -v
        assertEquals(Vector3(-1f, 2f, -3f), neg)
    }

    @Test
    fun `division by scalar`() {
        val v = Vector3(2f, 4f, 6f)
        assertEquals(Vector3(1f, 2f, 3f), v / 2f)
    }

    @Test
    fun `lerp at 0 returns this`() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        assertEquals(a, a.lerp(b, 0f))
    }

    @Test
    fun `lerp at 1 returns other`() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        assertEquals(b, a.lerp(b, 1f))
    }

    @Test
    fun `lerp at 0_5 returns midpoint`() {
        val a = Vector3(0f, 0f, 0f)
        val b = Vector3(2f, 4f, 6f)
        val mid = a.lerp(b, 0.5f)
        assertTrue(abs(mid.x - 1f) < 1e-6f)
        assertTrue(abs(mid.y - 2f) < 1e-6f)
        assertTrue(abs(mid.z - 3f) < 1e-6f)
    }
}
