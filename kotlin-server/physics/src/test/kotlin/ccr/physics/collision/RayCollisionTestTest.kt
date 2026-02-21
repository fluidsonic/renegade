package ccr.physics.collision

import ccr.math.*
import kotlin.test.*

class RayCollisionTestTest {
    @Test
    fun cullReturnsFalseForNonOverlappingBox() {
        val ray = LineSeg(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val test = RayCollisionTest(ray)
        val farBox = AABox(Vector3(100f, 100f, 100f), Vector3(1f, 1f, 1f))
        assertFalse(test.cull(farBox))
    }

    @Test
    fun cullReturnsTrueForOverlappingBox() {
        val ray = LineSeg(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val test = RayCollisionTest(ray)
        val nearBox = AABox(Vector3(0.5f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertTrue(test.cull(nearBox))
    }
}
