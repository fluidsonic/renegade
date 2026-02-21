package ccr.physics.moveable

import ccr.math.Vector3
import kotlin.math.abs
import kotlin.test.*

class ConcreteMoveablePhys : MoveablePhysClass()

class MoveablePhysClassTest {
    @Test
    fun massDefaultPositive() {
        val obj = ConcreteMoveablePhys()
        assertTrue(obj.mass > 0f)
    }

    @Test
    fun massSetterEnforcesPositive() {
        val obj = ConcreteMoveablePhys()
        assertFailsWith<IllegalArgumentException> { obj.mass = 0f }
        assertFailsWith<IllegalArgumentException> { obj.mass = -1f }
    }

    @Test
    fun massInvIsReciprocal() {
        val obj = ConcreteMoveablePhys()
        obj.mass = 2f
        assertTrue(abs(obj.massInv - 0.5f) < 1e-6f)
    }

    @Test
    fun velocityDefault() {
        val obj = ConcreteMoveablePhys()
        assertEquals(Vector3.ZERO, obj.velocity)
    }

    @Test
    fun needsTimestepTrue() {
        assertTrue(ConcreteMoveablePhys().needsTimestep())
    }

    @Test
    fun positionLinkedToTransform() {
        val obj = ConcreteMoveablePhys()
        obj.position = Vector3(1f, 2f, 3f)
        assertEquals(1f, obj.transform.translation.x)
        assertEquals(2f, obj.transform.translation.y)
        assertEquals(3f, obj.transform.translation.z)
    }
}
