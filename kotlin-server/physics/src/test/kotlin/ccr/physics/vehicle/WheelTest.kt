package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass
import kotlin.test.*

class WheelTest {

    private fun createBody(): RigidBodyClass = object : RigidBodyClass() {
        init {
            mass = 1000f
            computeInertia(2f, 4f, 1.5f)
            updateAuxiliaryState()
        }
    }

    @Test
    fun suspensionForceZeroWhenNotInContact() {
        val wheel = WVWheel()
        wheel.inContact = false
        val body = createBody()
        assertEquals(0f, wheel.computeSuspensionForce(body))
    }

    @Test
    fun tractionForceAppliedWhenInContact() {
        val wheel = WVWheel()
        wheel.inContact = true
        wheel.contactNormal = Vector3(0f, 0f, 1f)
        wheel.contactPoint = Vector3(0f, 0f, -0.5f)
        wheel.suspensionPosition = Vector3(0f, 0f, -0.3f)
        wheel.axleTorque = 100f
        wheel.radius = 0.3f

        val body = createBody()
        val force = MutableForce()
        val torque = MutableForce()
        wheel.computeTractionForces(body, force, torque)

        // Should produce some force (direction depends on vehicle orientation)
        val totalForce = Vector3(force.x, force.y, force.z)
        assertTrue(totalForce.length() > 0f, "Expected non-zero traction force")
    }
}
