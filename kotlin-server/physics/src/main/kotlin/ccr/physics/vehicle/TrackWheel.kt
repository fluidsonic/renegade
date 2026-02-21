package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass

// C++: TrackedVehicleWheelClass — differential steering for tracked vehicles
class TrackWheel : Wheel() {

    var trackFriction: Float = 2.0f

    override fun computeTractionForces(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce) {
        val worldPos = vehicle.transform.transformPoint(suspensionPosition)
        val pointVel = vehicle.computePointVelocity(worldPos)

        // Ground-plane velocity
        val normalComponent = pointVel.dot(contactNormal)
        val groundVel = pointVel - contactNormal * normalComponent

        // Forward and lateral
        val vehicleForward = vehicle.transform.rotateVector(Vector3(0f, 1f, 0f))
        val forwardOnGround = vehicleForward - contactNormal * vehicleForward.dot(contactNormal)
        val forwardLen = forwardOnGround.length()
        if (forwardLen < 1e-6f) return
        val forward = forwardOnGround / forwardLen
        val lateral = contactNormal.cross(forward)

        // Track force from axle torque
        val longitudinalVel = groundVel.dot(forward)
        val trackForce = if (radius > 0f) axleTorque / radius else 0f

        // Lateral friction (tracks have high lateral friction)
        val lateralVel = groundVel.dot(lateral)
        val latForce = -lateralVel * trackFriction

        val f = forward * trackForce + lateral * latForce
        force.add(f)
        val r = worldPos - vehicle.state.position
        torque.add(r.cross(f))
    }
}
