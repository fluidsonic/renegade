package ccr.physics.dynamic

import ccr.physics.PhysObserver
import kotlin.test.*

class TimedDecorationPhysClassTest {
    @Test fun expiresAfterLifetime() {
        val obj = TimedDecorationPhysClass(1.0f)
        var expired = false
        obj.observer = object : PhysObserver {
            override fun objectExpired() { expired = true }
        }
        obj.timestep(0.5f); assertFalse(expired)
        obj.timestep(0.6f); assertTrue(expired)
    }

    @Test fun lifetimeCountsDown() {
        val obj = TimedDecorationPhysClass(2.0f)
        obj.timestep(0.5f)
        assertEquals(1.5f, obj.lifetime, 0.001f)
        obj.timestep(1.0f)
        assertEquals(0.5f, obj.lifetime, 0.001f)
    }

    @Test fun needsTimestepTrue() {
        assertTrue(TimedDecorationPhysClass(1f).needsTimestep())
    }
}
