package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass
import kotlin.math.abs

// C++: WheelClass in wwphys/wheel.h
abstract class Wheel : SuspensionElement() {

    var radius: Float = 0.3f
    var rotation: Float = 0f
    var axleTorque: Float = 0f
    var slipFactor: Float = 0f
    var contactPoint: Vector3 = Vector3.ZERO
    var contactNormal: Vector3 = Vector3(0f, 0f, 1f)
    var inContact: Boolean = false

    fun computeSuspensionForce(vehicle: RigidBodyClass): Float {
        if (!inContact) return 0f
        // World-space wheel position
        val worldPos = vehicle.transform.transformPoint(suspensionPosition)
        // Compression = rest length - current distance to contact
        val toContact = contactPoint - worldPos
        val currentLength = abs(toContact.dot(contactNormal))
        val compression = springRestLength - currentLength
        if (compression <= 0f) return 0f
        // Spring force + damping
        val pointVel = vehicle.computePointVelocity(worldPos)
        val compressionVelocity = -pointVel.dot(contactNormal)
        return springConstant * compression + dampingConstant * compressionVelocity
    }

    abstract fun computeTractionForces(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce)

    override fun computeForceAndTorque(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce) {
        val suspForce = computeSuspensionForce(vehicle)
        if (suspForce > 0f) {
            val f = contactNormal * suspForce
            force.add(f)
            // Torque from suspension force applied at wheel position
            val worldPos = vehicle.transform.transformPoint(suspensionPosition)
            val r = worldPos - vehicle.state.position
            torque.add(r.cross(f))
        }
        if (inContact) {
            computeTractionForces(vehicle, force, torque)
        }
    }
}
