package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaneTest {
    @Test
    fun `distanceTo point on plane is zero`() {
        val plane = Plane(Vector3(0f, 0f, 1f), 0f)
        assertTrue(abs(plane.distanceTo(Vector3(5f, 3f, 0f))) < 1e-6f)
    }

    @Test
    fun `distanceTo point above is positive`() {
        val plane = Plane(Vector3(0f, 0f, 1f), 0f)
        val d = plane.distanceTo(Vector3(0f, 0f, 5f))
        assertTrue(d > 0f, "expected positive but got $d")
        assertTrue(abs(d - 5f) < 1e-6f)
    }

    @Test
    fun `distanceTo point below is negative`() {
        val plane = Plane(Vector3(0f, 0f, 1f), 0f)
        val d = plane.distanceTo(Vector3(0f, 0f, -3f))
        assertTrue(d < 0f)
    }

    @Test
    fun `intersect midway returns 0_5`() {
        val plane = Plane(Vector3(0f, 0f, 1f), 0f)
        val t = plane.intersect(Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
        assertNotNull(t)
        assertTrue(abs(t - 0.5f) < 1e-6f, "expected 0.5 but got $t")
    }

    @Test
    fun `intersect parallel returns null`() {
        val plane = Plane(Vector3(0f, 0f, 1f), 0f)
        val t = plane.intersect(Vector3(0f, 0f, 1f), Vector3(1f, 0f, 1f))
        assertNull(t)
    }

    @Test
    fun `fromNormalAndPoint`() {
        val plane = Plane.fromNormalAndPoint(Vector3(0f, 0f, 1f), Vector3(0f, 0f, 5f))
        assertTrue(abs(plane.dist - 5f) < 1e-6f)
        assertTrue(abs(plane.distanceTo(Vector3(0f, 0f, 5f))) < 1e-6f)
    }
}
