package ccr.physics

import kotlin.test.*

class PhysicsConstantsTest {
    @Test fun gravityZ() {
        assertEquals(-9.8f, PhysicsConstants.gravityAcceleration.z)
        assertEquals(0f, PhysicsConstants.gravityAcceleration.x)
        assertEquals(0f, PhysicsConstants.gravityAcceleration.y)
    }

    @Test fun frictionTableIndexed() {
        val f = PhysicsConstants.frictionTable[DynamicObjType.TIRE][SurfaceType.CONCRETE]
        assertEquals(PhysicsConstants.defaultContactFriction, f)
    }

    @Test fun dampingValues() {
        assertTrue(PhysicsConstants.linearDamping > 0f)
        assertTrue(PhysicsConstants.angularDamping > 0f)
    }
}
