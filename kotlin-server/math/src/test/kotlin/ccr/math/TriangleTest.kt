package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class TriangleTest {
    @Test
    fun `XY plane triangle normal is Z`() {
        val tri = Triangle(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))
        assertTrue(abs(tri.normal.x) < 1e-6f)
        assertTrue(abs(tri.normal.y) < 1e-6f)
        assertTrue(abs(tri.normal.z - 1f) < 1e-6f)
    }

    @Test
    fun `normal is unit length`() {
        val tri = Triangle(Vector3(0f, 0f, 0f), Vector3(3f, 0f, 0f), Vector3(0f, 4f, 0f))
        assertTrue(abs(tri.normal.length() - 1f) < 1e-6f)
    }

    @Test
    fun `normal is perpendicular to edges`() {
        val tri = Triangle(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f))
        val e1 = tri.v1 - tri.v0
        val e2 = tri.v2 - tri.v0
        assertTrue(abs(tri.normal.dot(e1)) < 1e-6f)
        assertTrue(abs(tri.normal.dot(e2)) < 1e-6f)
    }
}
