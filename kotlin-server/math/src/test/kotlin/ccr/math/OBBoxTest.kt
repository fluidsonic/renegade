package ccr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class OBBoxTest {
    @Test
    fun `axisAlignedExtent with identity basis returns same extent`() {
        val extent = Vector3(2f, 3f, 4f)
        val obb = OBBox(Vector3.ZERO, extent, Matrix3D.IDENTITY)
        val aae = obb.computeAxisAlignedExtent()
        assertTrue(abs(aae.x - extent.x) < 1e-5f, "expected ${extent.x} but got ${aae.x}")
        assertTrue(abs(aae.y - extent.y) < 1e-5f, "expected ${extent.y} but got ${aae.y}")
        assertTrue(abs(aae.z - extent.z) < 1e-5f, "expected ${extent.z} but got ${aae.z}")
    }

    @Test
    fun `projectToAxis with axis-aligned box`() {
        val obb = OBBox(Vector3.ZERO, Vector3(2f, 3f, 4f), Matrix3D.IDENTITY)
        val proj = obb.projectToAxis(Vector3(1f, 0f, 0f))
        assertTrue(abs(proj - 2f) < 1e-5f, "expected 2.0 but got $proj")
    }

    @Test
    fun `projectToAxis Y`() {
        val obb = OBBox(Vector3.ZERO, Vector3(2f, 3f, 4f), Matrix3D.IDENTITY)
        val proj = obb.projectToAxis(Vector3(0f, 1f, 0f))
        assertTrue(abs(proj - 3f) < 1e-5f, "expected 3.0 but got $proj")
    }
}
