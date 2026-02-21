package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass
import kotlin.math.abs
import kotlin.math.sqrt

// C++: WheeledVehicleWheelClass — tire friction model
class WVWheel : Wheel() {

    var lateralFriction: Float = 1.0f
    var longitudinalFriction: Float = 1.0f

    override fun computeTractionForces(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce) {
        val worldPos = vehicle.transform.transformPoint(suspensionPosition)
        val pointVel = vehicle.computePointVelocity(worldPos)

        // Project velocity onto ground plane
        val normalComponent = pointVel.dot(contactNormal)
        val groundVel = pointVel - contactNormal * normalComponent

        // Forward direction (vehicle's Y axis projected onto ground)
        val vehicleForward = vehicle.transform.rotateVector(Vector3(0f, 1f, 0f))
        val forwardOnGround = (vehicleForward - contactNormal * vehicleForward.dot(contactNormal))
        val forwardLen = forwardOnGround.length()
        if (forwardLen < 1e-6f) return
        val forward = forwardOnGround / forwardLen
        val lateral = contactNormal.cross(forward)

        // Longitudinal: traction from engine torque
        val longitudinalVel = groundVel.dot(forward)
        val wheelSpeed = if (radius > 0f) axleTorque / radius else 0f
        val longSlip = wheelSpeed - longitudinalVel
        val longForce = longSlip * longitudinalFriction

        // Lateral: friction resisting sideways sliding
        val lateralVel = groundVel.dot(lateral)
        val latForce = -lateralVel * lateralFriction

        // Friction circle: limit combined force
        val maxFriction = computeSuspensionForce(vehicle) * 1.0f
        val combinedSq = longForce * longForce + latForce * latForce
        val scale = if (combinedSq > maxFriction * maxFriction && combinedSq > 0f)
            maxFriction / sqrt(combinedSq) else 1f

        val f = forward * (longForce * scale) + lateral * (latForce * scale)
        force.add(f)
        val r = worldPos - vehicle.state.position
        torque.add(r.cross(f))

        // Update wheel rotation
        val speed = abs(longitudinalVel)
        if (radius > 0f) rotation += speed / radius * 0.016f // approximate dt
        slipFactor = if (speed > 0.1f) abs(longSlip) / speed else 0f
    }
}
