package ccr.physics.collision

import ccr.math.*
import kotlin.test.*

class AABoxCollisionTestTest {
    @Test
    fun sweepBoundsCorrect() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val move = Vector3(5f, 0f, 0f)
        val test = AABoxCollisionTest(box, move)
        // box min.x=-1, max.x=1, move.x=5 → sweepMax.x = max(1, 1+5) = 6
        assertTrue(test.sweepMax.x >= 6f, "sweepMax.x=${test.sweepMax.x}")
        // sweepMin.x = min(-1, -1+5) = -1
        assertTrue(test.sweepMin.x <= -1f, "sweepMin.x=${test.sweepMin.x}")
    }

    @Test
    fun cullFalseForDistantBox() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val test = AABoxCollisionTest(box, Vector3(1f, 0f, 0f))
        val far = AABox(Vector3(100f, 100f, 100f), Vector3(1f, 1f, 1f))
        assertFalse(test.cull(far))
    }

    @Test
    fun cullTrueForNearBox() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val test = AABoxCollisionTest(box, Vector3(3f, 0f, 0f))
        val near = AABox(Vector3(2f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertTrue(test.cull(near))
    }
}
