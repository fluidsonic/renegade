package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

// C++: WheeledVehicleClass in wwphys/wheelvehicle.h
open class WheeledVehicleClass : MotorVehicleClass() {

    companion object {
        // Chunk IDs sourced from wheelvehicle.cpp Save()
        const val WHEELEDVEHICLE_CHUNK_MOTORVEHICLE: Int = 0x00119801  // wraps parent MotorVehicleClass
        const val WHEELEDVEHICLE_CHUNK_VARIABLES: Int = 0x00119802     // own data

        // Micro-chunk IDs within WHEELEDVEHICLE_CHUNK_VARIABLES:
        const val WV_VARIABLE_STEERINGANGLE: Int = 0x00
        // 0x01..0x04 are OBSOLETE (maxSteeringAngle, spring/damping/length moved to VehiclePhysDef)
    }

    // Definition-like fields (C++: in WheeledVehicleDefClass)
    var maxSteeringAngle: Float = 30f // degrees

    // Instance fields (C++: WheeledVehicleClass)
    var steeringAngle: Float = 0f     // C++: SteeringAngle

    override fun asWheeledVehicleClass(): WheeledVehicleClass = this

    override fun allocSuspensionElement(): SuspensionElement = WVWheel()

    override fun timestep(dt: Float) {
        if (isFake) return

        // Apply steering angle to steering-flagged wheels
        val ctrl = controller
        val steerInput = ctrl?.turnLeft ?: 0f
        steeringAngle = Math.toRadians((steerInput * maxSteeringAngle).toDouble()).toFloat()

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
