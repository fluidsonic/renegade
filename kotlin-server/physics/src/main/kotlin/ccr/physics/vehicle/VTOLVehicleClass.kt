package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.math.atan2

// C++: VTOLVehicleClass in wwphys/vtolvehicle.h
open class VTOLVehicleClass : VehiclePhysClass() {

    var liftForce: Float = 0f
    var thrustForce: Float = 0f
    var pitchGain: Float = 1.0f
    var rollGain: Float = 1.0f
    var yawGain: Float = 1.0f
    var pitchDamping: Float = 0.5f
    var rollDamping: Float = 0.5f
    var yawDamping: Float = 0.5f

    override fun allocSuspensionElement(): SuspensionElement = VTOLWheel()

    override fun computeForceAndTorque() {
        super.computeForceAndTorque()

        val ctrl = controller
        val throttle = ctrl?.moveUp?.coerceIn(-1f, 1f) ?: 0f
        val pitch = ctrl?.moveForward?.coerceIn(-1f, 1f) ?: 0f
        val yaw = ctrl?.turnLeft?.coerceIn(-1f, 1f) ?: 0f
        val roll = ctrl?.moveLeft?.coerceIn(-1f, 1f) ?: 0f

        // Lift force along vehicle's up axis
        val up = transform.rotateVector(Vector3(0f, 0f, 1f))
        val forward = transform.rotateVector(Vector3(0f, 1f, 0f))
        val right = transform.rotateVector(Vector3(1f, 0f, 0f))

        addForce(up * (liftForce * throttle))
        addForce(forward * (thrustForce * pitch))

        // PD orientation stabilization torques
        // Try to keep level: penalize world-space tilt
        val worldUp = Vector3(0f, 0f, 1f)
        val pitchError = forward.dot(worldUp) // positive = nose up
        val rollError = right.dot(worldUp)    // positive = tilted right

        // Stabilization torques (body-space axes)
        val pitchTorque = -pitchError * pitchGain - angularVelocity.dot(right) * pitchDamping
        val rollTorque = -rollError * rollGain - angularVelocity.dot(forward) * rollDamping
        val yawTorque = yaw * yawGain - angularVelocity.dot(up) * yawDamping

        addTorque(right * pitchTorque + forward * rollTorque + up * yawTorque)
    }
}
