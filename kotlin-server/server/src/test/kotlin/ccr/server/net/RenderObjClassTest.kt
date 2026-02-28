package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderObjClassTest {

    // -------------------------------------------------------------------------
    // RenderObjClass.getBoneTransform(int) — C++ base returns Get_Transform()
    // -------------------------------------------------------------------------

    @Test
    fun `getBoneTransform returns object transform not identity`() {
        val obj = RenderObjClass()
        val tm = Matrix3D.translation(Vector3(1f, 2f, 3f))
        obj.setTransform(tm)
        val result = obj.getBoneTransform(0)
        assertEquals(tm, result)
    }

    @Test
    fun `getBoneTransform with identity transform returns identity`() {
        val obj = RenderObjClass()
        // default transform is identity
        val result = obj.getBoneTransform(5)
        assertEquals(Matrix3D.IDENTITY, result)
    }

    @Test
    fun `getBoneTransform any bone index returns same object transform`() {
        val obj = RenderObjClass()
        val tm = Matrix3D(
            m00 = 0f, m01 = -1f, m02 = 0f, m03 = 10f,
            m10 = 1f, m11 =  0f, m12 = 0f, m13 = 20f,
            m20 = 0f, m21 =  0f, m22 = 1f, m23 = 30f,
        )
        obj.setTransform(tm)
        // C++ base returns Get_Transform() regardless of boneIndex
        assertEquals(tm, obj.getBoneTransform(0))
        assertEquals(tm, obj.getBoneTransform(1))
        assertEquals(tm, obj.getBoneTransform(42))
    }
}
