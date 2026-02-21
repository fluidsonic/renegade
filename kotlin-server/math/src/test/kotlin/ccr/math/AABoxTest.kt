package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AABoxTest {
    @Test
    fun `fromMinMax computes center and extent`() {
        val box = AABox.fromMinMax(Vector3(-1f, -2f, -3f), Vector3(1f, 2f, 3f))
        assertTrue(abs(box.center.x) < 1e-6f)
        assertTrue(abs(box.center.y) < 1e-6f)
        assertTrue(abs(box.center.z) < 1e-6f)
        assertTrue(abs(box.extent.x - 1f) < 1e-6f)
        assertTrue(abs(box.extent.y - 2f) < 1e-6f)
        assertTrue(abs(box.extent.z - 3f) < 1e-6f)
    }

    @Test
    fun `min and max computed correctly`() {
        val box = AABox(Vector3(1f, 2f, 3f), Vector3(0.5f, 0.5f, 0.5f))
        assertTrue(abs(box.min.x - 0.5f) < 1e-6f)
        assertTrue(abs(box.max.x - 1.5f) < 1e-6f)
    }

    @Test
    fun `contains center`() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertTrue(box.contains(Vector3(0f, 0f, 0f)))
    }

    @Test
    fun `does not contain far point`() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertFalse(box.contains(Vector3(5f, 5f, 5f)))
    }

    @Test
    fun `overlaps with adjacent box`() {
        val a = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val b = AABox(Vector3(1.5f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertTrue(a.overlaps(b))
    }

    @Test
    fun `does not overlap distant box`() {
        val a = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val b = AABox(Vector3(10f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertFalse(a.overlaps(b))
    }

    @Test
    fun `addPoint expands box`() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val expanded = box.addPoint(Vector3(5f, 0f, 0f))
        assertTrue(abs(expanded.max.x - 5f) < 1e-6f)
        assertTrue(abs(expanded.min.x - (-1f)) < 1e-6f)
    }

    @Test
    fun `translate moves center`() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val moved = box.translate(Vector3(5f, 0f, 0f))
        assertTrue(abs(moved.center.x - 5f) < 1e-6f)
        assertTrue(abs(moved.extent.x - 1f) < 1e-6f)
    }
}
