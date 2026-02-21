package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass

// C++: VTOLWheelClass — high-friction landing gear
class VTOLWheel : Wheel() {

    var gearFriction: Float = 3.0f

    override fun computeTractionForces(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce) {
        val worldPos = vehicle.transform.transformPoint(suspensionPosition)
        val pointVel = vehicle.computePointVelocity(worldPos)

        // Ground-plane velocity
        val normalComponent = pointVel.dot(contactNormal)
        val groundVel = pointVel - contactNormal * normalComponent

        // High friction in both directions to stop sliding
        val f = -groundVel * gearFriction
        force.add(f)
        val r = worldPos - vehicle.state.position
        torque.add(r.cross(f))
    }
}
