package ccr.physics.moveable

import ccr.math.Vector3
import ccr.physics.PhysObserver
import ccr.physics.collision.CollisionReactionType
import ccr.physics.collision.CollisionEvent
import kotlin.math.abs
import kotlin.test.*

class ProjectileClassTest {
    @Test
    fun ballisticTrajectory() {
        val proj = ProjectileClass()
        proj.position = Vector3(0f, 0f, 0f)
        proj.velocity = Vector3(0f, 0f, 0f)
        proj.gravity = 1.0f
        repeat(100) { proj.timestep(0.01f) }
        assertTrue(abs(proj.position.z - (-4.9f)) < 0.5f, "z=${proj.position.z}")
    }

    @Test
    fun expiresAfterLifetime() {
        val proj = ProjectileClass()
        proj.lifetime = 0.5f
        var expired = false
        proj.observer = object : PhysObserver {
            override fun objectExpired() {
                expired = true
            }
        }
        repeat(60) { proj.timestep(0.01f) }
        assertTrue(expired)
    }

    @Test
    fun defaultValues() {
        val proj = ProjectileClass()
        assertEquals(5.0f, proj.lifetime)
        assertEquals(0, proj.bounceCount)
        assertEquals(0, proj.maxBounces)
        assertTrue(proj.needsTimestep())
    }
}
