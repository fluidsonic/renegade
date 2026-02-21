package ccr.physics.dynamic

import ccr.math.Matrix3D
import ccr.math.Vector3
import kotlin.test.*

class DecorationPhysClassTest {
    @Test fun needsTimestepFalse() {
        assertFalse(DecorationPhysClass().needsTimestep())
    }

    @Test fun transformGetSet() {
        val obj = DecorationPhysClass()
        val t = Matrix3D.IDENTITY.withTranslation(Vector3(1f, 2f, 3f))
        obj.transform = t
        assertEquals(1f, obj.transform.translation.x)
        assertEquals(2f, obj.transform.translation.y)
        assertEquals(3f, obj.transform.translation.z)
    }
}
