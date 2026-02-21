package ccr.physics.rigidbody

import ccr.math.Matrix3
import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.physics.ode.StateVector
import kotlin.math.abs
import kotlin.test.*

// Concrete implementation for testing
private class TestRigidBody : RigidBodyClass() {
    init {
        mass = 1f
        computeInertia(1f, 1f, 1f)
        updateAuxiliaryState()
    }
}

class RigidBodyClassTest {

    @Test
    fun freeFall() {
        val body = TestRigidBody()
        body.gravScale = 1f
        // Simulate 1 second in 100 steps
        val dt = 0.01f
        for (i in 0 until 100) {
            body.clearContacts()
            body.integrate(dt)
        }
        // After 1s of free fall: z ≈ -0.5 * 9.8 * 1^2 = -4.9
        val z = body.state.position.z
        assertTrue(z < -4.0f, "Expected z < -4.0 after 1s free fall, got $z")
        assertTrue(z > -6.0f, "Expected z > -6.0 after 1s free fall (with damping), got $z")
    }

    @Test
    fun impulseChangesVelocity() {
        val body = TestRigidBody()
        body.gravScale = 0f // disable gravity
        body.applyImpulse(Vector3(1f, 0f, 0f)) // 1 kg*m/s impulse
        body.updateAuxiliaryState()
        // velocity = momentum * massInv = 1 * 1 = 1 m/s
        assertTrue(abs(body.velocity.x - 1f) < 1e-5f, "velocity.x=${body.velocity.x}")
    }

    @Test
    fun angularImpulse() {
        val body = TestRigidBody()
        body.gravScale = 0f
        val worldPos = body.state.position + Vector3(0.5f, 0f, 0f)
        body.applyImpulse(Vector3(0f, 0f, 1f), worldPos)
        body.updateAuxiliaryState()
        // Should have both linear and angular momentum
        assertTrue(body.velocity.z > 0f, "Expected positive velocity.z")
        assertTrue(body.angularVelocity.length() > 0f, "Expected non-zero angular velocity")
    }

    @Test
    fun boxInertia() {
        val body = TestRigidBody()
        body.mass = 12f
        body.computeInertia(1f, 1f, 1f)
        // I_x = 12 * (1+1) / 12 = 2
        assertTrue(abs(body.inertiaBody.m00 - 2f) < 1e-5f, "Ix=${body.inertiaBody.m00}")
        assertTrue(abs(body.inertiaBody.m11 - 2f) < 1e-5f, "Iy=${body.inertiaBody.m11}")
        assertTrue(abs(body.inertiaBody.m22 - 2f) < 1e-5f, "Iz=${body.inertiaBody.m22}")
    }

    @Test
    fun sleepAfterDelay() {
        val body = TestRigidBody()
        body.gravScale = 0f
        // Add enough contacts
        body.addContact(Vector3(0f, 0f, -0.5f), Vector3(0f, 0f, 1f))
        body.addContact(Vector3(0.5f, 0f, -0.5f), Vector3(0f, 0f, 1f))
        body.addContact(Vector3(-0.5f, 0f, -0.5f), Vector3(0f, 0f, 1f))
        // Velocity is zero, angular velocity is zero, 3 contacts
        assertFalse(body.isAsleep)
        // Simulate enough time for sleep
        val dt = 0.1f
        for (i in 0 until 10) {
            body.clearContacts()
            body.addContact(Vector3(0f, 0f, -0.5f), Vector3(0f, 0f, 1f))
            body.addContact(Vector3(0.5f, 0f, -0.5f), Vector3(0f, 0f, 1f))
            body.addContact(Vector3(-0.5f, 0f, -0.5f), Vector3(0f, 0f, 1f))
            body.integrate(dt) // velocity stays near zero with no gravity
            // Sleep check
            if (body.velocity.length() < 0.1f &&
                body.angularVelocity.length() < 0.1f &&
                body.contactCount >= 3
            ) {
                body.goToSleepTimer += dt
                if (body.goToSleepTimer >= RigidBodyClass.SLEEP_DELAY) {
                    body.isAsleep = true
                }
            } else {
                body.goToSleepTimer = 0f
            }
        }
        assertTrue(body.isAsleep, "Expected body to be asleep after sufficient time with contacts")
    }

    @Test
    fun odeRoundTrip() {
        val body = TestRigidBody()
        body.state = RigidBodyState(
            position = Vector3(1f, 2f, 3f),
            orientation = Quaternion.IDENTITY,
            linearMomentum = Vector3(4f, 5f, 6f),
            angularMomentum = Vector3(0.1f, 0.2f, 0.3f),
        )
        val sv1 = StateVector()
        body.getState(sv1)
        assertEquals(13, sv1.count)

        // Modify state, then restore
        body.state = RigidBodyState()
        body.setState(sv1, 0)
        val sv2 = StateVector()
        body.getState(sv2)

        for (i in 0 until 13) {
            assertTrue(abs(sv1[i] - sv2[i]) < 1e-5f, "Mismatch at index $i: ${sv1[i]} != ${sv2[i]}")
        }
    }

    @Test
    fun computePointVelocity() {
        val body = TestRigidBody()
        body.gravScale = 0f
        body.applyImpulse(Vector3(1f, 0f, 0f))
        body.updateAuxiliaryState()
        // Point at body center should have same velocity as body
        val v = body.computePointVelocity(body.state.position)
        assertTrue(abs(v.x - 1f) < 1e-5f)
    }

    @Test
    fun asleepBodyDoesNotMove() {
        val body = TestRigidBody()
        body.isAsleep = true
        val posBefore = body.state.position
        body.timestep(0.1f)
        assertEquals(posBefore, body.state.position)
    }
}
