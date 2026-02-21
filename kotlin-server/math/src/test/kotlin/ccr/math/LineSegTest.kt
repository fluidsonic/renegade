package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineSegTest {
    @Test
    fun `pointAt 0 returns p0`() {
        val ls = LineSeg(Vector3(1f, 2f, 3f), Vector3(4f, 5f, 6f))
        assertEquals(ls.p0, ls.pointAt(0f))
    }

    @Test
    fun `pointAt 1 returns p1`() {
        val ls = LineSeg(Vector3(1f, 2f, 3f), Vector3(4f, 5f, 6f))
        assertEquals(ls.p1, ls.pointAt(1f))
    }

    @Test
    fun `pointAt 0_5 returns midpoint`() {
        val ls = LineSeg(Vector3(0f, 0f, 0f), Vector3(2f, 4f, 6f))
        val mid = ls.pointAt(0.5f)
        assertTrue(abs(mid.x - 1f) < 1e-6f)
        assertTrue(abs(mid.y - 2f) < 1e-6f)
        assertTrue(abs(mid.z - 3f) < 1e-6f)
    }

    @Test
    fun `length is correct`() {
        val ls = LineSeg(Vector3(0f, 0f, 0f), Vector3(3f, 4f, 0f))
        assertTrue(abs(ls.length - 5f) < 1e-6f)
    }

    @Test
    fun `dir is normalized`() {
        val ls = LineSeg(Vector3(0f, 0f, 0f), Vector3(3f, 4f, 0f))
        assertTrue(abs(ls.dir.length() - 1f) < 1e-6f)
        assertTrue(abs(ls.dir.x - 0.6f) < 1e-6f)
        assertTrue(abs(ls.dir.y - 0.8f) < 1e-6f)
    }
}
