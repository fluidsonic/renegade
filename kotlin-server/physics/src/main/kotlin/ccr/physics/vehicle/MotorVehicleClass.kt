package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.math.abs
import kotlin.math.min

// C++: MotorVehicleClass in wwphys/motorvehicle.h
abstract class MotorVehicleClass : VehiclePhysClass() {

    var maxEngineTorque: Float = 1000f
    var engineRPM: Float = 800f
    var idleRPM: Float = 800f
    var maxRPM: Float = 6000f
    var currentGear: Int = 1
    var gearCount: Int = 5
    var gearRatios: FloatArray = floatArrayOf(3.5f, 2.5f, 1.7f, 1.2f, 0.9f)
    var finalDriveGearRatio: Float = 3.7f
    var shiftUpRpm: Float = 5000f
    var shiftDownRpm: Float = 2000f
    var driveTrainInertia: Float = 0.5f

    private var throttle: Float = 0f
    private var brake: Float = 0f

    override fun timestep(dt: Float) {
        if (isFake) return

        // Read controller input
        val ctrl = controller
        if (ctrl != null && isEngineOn) {
            throttle = ctrl.moveForward.coerceIn(0f, 1f)
            brake = (-ctrl.moveForward).coerceIn(0f, 1f)
        } else {
            throttle = 0f
            brake = 0f
        }

        // Update engine RPM based on vehicle speed
        val speed = velocity.length()
        val gearRatio = if (currentGear in 1..gearRatios.size)
            gearRatios[currentGear - 1] else 1f
        val totalRatio = gearRatio * finalDriveGearRatio
        // Find an engine-flagged wheel for radius
        val engineWheel = wheels.filterIsInstance<Wheel>().firstOrNull { it.hasFlag(SuspensionElement.ENGINE) }
        val wheelRadius = engineWheel?.radius ?: 0.3f
        if (wheelRadius > 0f && totalRatio > 0f) {
            engineRPM = (speed / wheelRadius * totalRatio * 60f / (2f * kotlin.math.PI.toFloat()))
                .coerceIn(idleRPM, maxRPM)
        }

        // Auto-shift
        if (engineRPM >= shiftUpRpm && currentGear < gearCount) {
            currentGear++
        } else if (engineRPM <= shiftDownRpm && currentGear > 1) {
            currentGear--
        }

        // Compute axle torque and distribute to engine wheels
        val engineTorque = throttle * maxEngineTorque
        val axleTorque = engineTorque * gearRatio * finalDriveGearRatio
        val engineWheels = wheels.filterIsInstance<Wheel>().filter { it.hasFlag(SuspensionElement.ENGINE) }
        val perWheel = if (engineWheels.isNotEmpty()) axleTorque / engineWheels.size else 0f
        for (w in engineWheels) {
            w.axleTorque = perWheel
        }

        // Apply brakes
        if (brake > 0f) {
            for (w in wheels.filterIsInstance<Wheel>()) {
                w.setFlag(SuspensionElement.BRAKING)
                w.axleTorque = -brake * maxEngineTorque * 0.5f
            }
        } else {
            for (w in wheels.filterIsInstance<Wheel>()) {
                w.clearFlag(SuspensionElement.BRAKING)
            }
        }

        super.timestep(dt)
    }
}
