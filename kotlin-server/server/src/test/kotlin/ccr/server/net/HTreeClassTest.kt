package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.level.w3d.W3dHierarchy
import ccr.server.level.w3d.W3dPivot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.math.abs
import kotlin.test.assertTrue

private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-5f) {
    assertTrue(abs(actual - expected) < eps, "Expected $expected but got $actual (eps=$eps)")
}

class HTreeClassTest {

    // -------------------------------------------------------------------------
    // HTreeClass.baseUpdate + getTransform
    // -------------------------------------------------------------------------

    @Test
    fun `getTransform before baseUpdate returns identity`() {
        // C++: HTreeClass has no initialised Pivot transforms until Base_Update is called
        val hierarchy = W3dHierarchy(
            name = "test",
            centerPos = ccr.server.level.Vector3(0f, 0f, 0f),
            pivots = listOf(
                W3dPivot("root", parentId = -1, translation = ccr.server.level.Vector3(0f, 0f, 0f)),
            ),
        )
        val tree = HTreeClass(hierarchy)
        // Before baseUpdate, cached transforms are null — should return IDENTITY
        assertEquals(Matrix3D.IDENTITY, tree.getTransform(0))
    }

    @Test
    fun `baseUpdate with identity root sets bone 0 to identity`() {
        val hierarchy = W3dHierarchy(
            name = "test",
            centerPos = ccr.server.level.Vector3(0f, 0f, 0f),
            pivots = listOf(
                W3dPivot("root", parentId = -1, translation = ccr.server.level.Vector3(0f, 0f, 0f)),
            ),
        )
        val tree = HTreeClass(hierarchy)
        tree.baseUpdate(Matrix3D.IDENTITY)
        assertEquals(Matrix3D.IDENTITY, tree.getTransform(0))
    }

    @Test
    fun `baseUpdate with translated root propagates to bone 0`() {
        // C++: Pivot[0].Transform = root
        val hierarchy = W3dHierarchy(
            name = "test",
            centerPos = ccr.server.level.Vector3(0f, 0f, 0f),
            pivots = listOf(
                W3dPivot("root", parentId = -1, translation = ccr.server.level.Vector3(0f, 0f, 0f)),
            ),
        )
        val tree = HTreeClass(hierarchy)
        val root = Matrix3D.translation(Vector3(5f, 6f, 7f))
        tree.baseUpdate(root)
        val result = tree.getTransform(0)
        assertNear(5f, result.m03)
        assertNear(6f, result.m13)
        assertNear(7f, result.m23)
    }

    @Test
    fun `baseUpdate two-bone chain applies child offset from identity root`() {
        // C++: world[i] = world[parentId] * localBaseTransform(i)
        // root at identity, child has local translation (1, 0, 0) with identity rotation
        val hierarchy = W3dHierarchy(
            name = "test",
            centerPos = ccr.server.level.Vector3(0f, 0f, 0f),
            pivots = listOf(
                W3dPivot("root", parentId = -1, translation = ccr.server.level.Vector3(0f, 0f, 0f)),
                W3dPivot("child", parentId = 0, translation = ccr.server.level.Vector3(1f, 0f, 0f)),
            ),
        )
        val tree = HTreeClass(hierarchy)
        tree.baseUpdate(Matrix3D.IDENTITY)

        // root at identity
        assertEquals(Matrix3D.IDENTITY, tree.getTransform(0))

        // child: identity * translation(1,0,0) = translation(1,0,0)
        val child = tree.getTransform(1)
        assertNear(1f, child.m03)
        assertNear(0f, child.m13)
        assertNear(0f, child.m23)
        // rotation part must be identity
        assertNear(1f, child.m00); assertNear(0f, child.m01); assertNear(0f, child.m02)
        assertNear(0f, child.m10); assertNear(1f, child.m11); assertNear(0f, child.m12)
        assertNear(0f, child.m20); assertNear(0f, child.m21); assertNear(1f, child.m22)
    }

    @Test
    fun `baseUpdate two-bone chain with translated root`() {
        // root at (5,0,0), child with local translation (1,0,0)
        // expected child world = (5,0,0) + (1,0,0) = (6,0,0)
        val hierarchy = W3dHierarchy(
            name = "test",
            centerPos = ccr.server.level.Vector3(0f, 0f, 0f),
            pivots = listOf(
                W3dPivot("root", parentId = -1, translation = ccr.server.level.Vector3(0f, 0f, 0f)),
                W3dPivot("child", parentId = 0, translation = ccr.server.level.Vector3(1f, 0f, 0f)),
            ),
        )
        val tree = HTreeClass(hierarchy)
        val root = Matrix3D.translation(Vector3(5f, 0f, 0f))
        tree.baseUpdate(root)

        val rootTm = tree.getTransform(0)
        assertNear(5f, rootTm.m03)

        val child = tree.getTransform(1)
        assertNear(6f, child.m03)
        assertNear(0f, child.m13)
        assertNear(0f, child.m23)
    }

    @Test
    fun `getTransform out of range returns identity`() {
        val hierarchy = W3dHierarchy(
            name = "test",
            centerPos = ccr.server.level.Vector3(0f, 0f, 0f),
            pivots = listOf(
                W3dPivot("root", parentId = -1, translation = ccr.server.level.Vector3(0f, 0f, 0f)),
            ),
        )
        val tree = HTreeClass(hierarchy)
        tree.baseUpdate(Matrix3D.IDENTITY)
        // out-of-range bone index → identity
        assertEquals(Matrix3D.IDENTITY, tree.getTransform(99))
    }
}
