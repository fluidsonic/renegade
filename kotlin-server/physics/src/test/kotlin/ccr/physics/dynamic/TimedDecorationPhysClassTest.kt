package ccr.physics.dynamic

import kotlin.test.*

class TimedDecorationPhysClassTest {
    @Test fun expiresAfterLifetime() {
        val obj = TimedDecorationPhysClass(1.0f)
        var expired = false
        obj.onExpired = { expired = true }
        obj.timestep(0.5f); assertFalse(expired)
        obj.timestep(0.6f); assertTrue(expired)
    }

    @Test fun onExpiredCalledOnce() {
        val obj = TimedDecorationPhysClass(0.5f)
        var count = 0
        obj.onExpired = { count++ }
        obj.timestep(0.6f); obj.timestep(0.6f) // second step should not re-trigger
        assertEquals(1, count)
    }

    @Test fun needsTimestepTrue() {
        assertTrue(TimedDecorationPhysClass(1f).needsTimestep())
    }
}
