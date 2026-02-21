package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

// C++: WheeledVehicleClass in wwphys/wvehicle.h
open class WheeledVehicleClass : MotorVehicleClass() {

    var maxSteeringAngle: Float = 30f // degrees

    override fun allocSuspensionElement(): SuspensionElement = WVWheel()

    override fun timestep(dt: Float) {
        if (isFake) return

        // Apply steering angle to steering-flagged wheels
        val ctrl = controller
        val steerInput = ctrl?.turnLeft ?: 0f
        val steerAngleRad = Math.toRadians((steerInput * maxSteeringAngle).toDouble()).toFloat()

        for (w in wheels.filterIsInstance<Wheel>()) {
            if (w.hasFlag(SuspensionElement.STEERING)) {
                // Steering wheels modify their forward direction via the angle
                // (actual steering is handled in traction force computation)
            }
            if (w.hasFlag(SuspensionElement.INV_STEERING)) {
                // Inverse steering (rear-wheel steer)
            }
        }

        super.timestep(dt)
    }
}
