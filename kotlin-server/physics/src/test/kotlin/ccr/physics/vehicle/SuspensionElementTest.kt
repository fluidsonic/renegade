package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass
import kotlin.math.abs
import kotlin.test.*

private class TestSuspension : SuspensionElement() {
    var lastForce = Vector3.ZERO
    override fun computeForceAndTorque(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce) {
        // Simple spring: push up
        force.add(Vector3(0f, 0f, springConstant * 0.1f))
        lastForce = Vector3(0f, 0f, springConstant * 0.1f)
    }
}

class SuspensionElementTest {

    @Test
    fun springForceProportionalToConstant() {
        val s1 = TestSuspension()
        s1.springConstant = 1000f
        val s2 = TestSuspension()
        s2.springConstant = 2000f

        val body = object : RigidBodyClass() {
            init { mass = 1f; computeInertia(1f, 1f, 1f); updateAuxiliaryState() }
        }

        val f1 = MutableForce(); val t1 = MutableForce()
        s1.computeForceAndTorque(body, f1, t1)
        val f2 = MutableForce(); val t2 = MutableForce()
        s2.computeForceAndTorque(body, f2, t2)

        assertTrue(abs(f2.z / f1.z - 2f) < 0.01f, "Expected double force: ${f1.z} vs ${f2.z}")
    }

    @Test
    fun flagOperations() {
        val s = TestSuspension()
        assertFalse(s.hasFlag(SuspensionElement.ENGINE))
        s.setFlag(SuspensionElement.ENGINE)
        assertTrue(s.hasFlag(SuspensionElement.ENGINE))
        s.clearFlag(SuspensionElement.ENGINE)
        assertFalse(s.hasFlag(SuspensionElement.ENGINE))
    }
}
