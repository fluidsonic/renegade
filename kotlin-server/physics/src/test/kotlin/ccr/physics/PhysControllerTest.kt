package ccr.physics

import kotlin.test.*

class PhysControllerTest {
    @Test fun isInactiveWhenZero() {
        val c = PhysController()
        assertTrue(c.isInactive)
    }

    @Test fun notInactiveWhenSet() {
        val c = PhysController()
        c.moveForward = 1f
        assertFalse(c.isInactive)
    }

    @Test fun resetClearsAll() {
        val c = PhysController()
        c.moveForward = 1f; c.moveLeft = -0.5f; c.turnLeft = 0.3f
        c.reset()
        assertTrue(c.isInactive)
    }

    @Test fun moveVector() {
        val c = PhysController()
        c.moveForward = 1f; c.moveLeft = 0.5f; c.moveUp = 0.2f
        assertEquals(0.5f, c.moveVector.x)
        assertEquals(1f, c.moveVector.y)
        assertEquals(0.2f, c.moveVector.z)
    }
}
