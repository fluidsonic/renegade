package ccr.math

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
}
