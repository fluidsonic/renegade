package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.math.abs
import kotlin.math.min

// C++: MotorVehicleClass in wwphys/motorvehicle.h
abstract class MotorVehicleClass : VehiclePhysClass() {

    companion object {
        private const val GEAR_SHIFT_DELAY = 1.0f
        private const val MIN_BRAKING_SPEED = 1.5f

        // Chunk IDs sourced from motorvehicle.cpp Save()
        const val MOTO_CHUNK_RIGIDBODY: Int = 0x01234500       // obsolete parent chunk (was RigidBody)
        const val MOTO_CHUNK_VARIABLES: Int = 0x01234501       // own data
        const val MOTO_CHUNK_VEHICLEPHYS: Int = 0x01234502     // current parent chunk (VehiclePhys)

        // Micro-chunk IDs within MOTO_CHUNK_VARIABLES:
        // 0x00 = OBSOLETE_MOTO_VARIABLE_CURENGINETORQUE
        const val MOTO_VARIABLE_ENGINEANGULARVELOCITY: Int = 0x01
        const val MOTO_VARIABLE_CURRENTGEAR: Int = 0x02
        const val MOTO_VARIABLE_SHIFTIMER: Int = 0x03
    }

    // Definition-like fields (C++: in MotorVehicleDefClass)
    var maxEngineTorque: Float = 1000f
    var gearCount: Int = 5
    var gearRatios: FloatArray = floatArrayOf(3.5f, 2.5f, 1.7f, 1.2f, 0.9f)
    var finalDriveGearRatio: Float = 3.7f
    var shiftUpRpm: Float = 5000f
    var shiftDownRpm: Float = 2000f
    var driveTrainInertia: Float = 0.5f

    // Instance fields (C++: MotorVehicleClass)
    var engineAngularVelocity: Float = 0f   // C++: EngineAngularVelocity (rad/s)
    var currentGear: Int = 0                // C++: CurrentGear (0-indexed)
    var shiftTimer: Float = 0f              // C++: ShiftTimer
    var acceleratorFraction: Float = 0f     // C++: AcceleratorFraction
    var isBraking: Boolean = false          // C++: IsBraking

    // Non-C++ helpers for Kotlin logic
    var idleRPM: Float = 800f
    var maxRPM: Float = 6000f

    override fun asMotorVehicleClass(): MotorVehicleClass = this

    override fun timestep(dt: Float) {
        if (isFake) return

        // Read controller input
        val ctrl = controller
        if (ctrl != null && isEngineOn) {
            acceleratorFraction = ctrl.moveForward.coerceIn(-1f, 1f)
            isBraking = false

            // Determine braking vs accelerating (matches C++ Timestep logic)
            val objSpeed = velocity.length()
            if (ctrl.moveForward < 0f && objSpeed > MIN_BRAKING_SPEED) {
                isBraking = true
                acceleratorFraction = 0f
            } else if (ctrl.moveForward > 0f && objSpeed > MIN_BRAKING_SPEED) {
                // Forward driving, not braking
            }
        } else {
            acceleratorFraction = 0f
            isBraking = false
        }

        // Update engine RPM based on vehicle speed
        val speed = velocity.length()
        val gearRatio = if (currentGear in gearRatios.indices)
            gearRatios[currentGear] else 1f
        val totalRatio = gearRatio * finalDriveGearRatio
        // Find an engine-flagged wheel for radius
        val engineWheel = wheels.filterIsInstance<Wheel>().firstOrNull { it.hasFlag(SuspensionElement.ENGINE) }
        val wheelRadius = engineWheel?.radius ?: 0.3f
        if (wheelRadius > 0f && totalRatio > 0f) {
            engineAngularVelocity = (speed / wheelRadius * totalRatio)
                .coerceIn(0f, maxRPM)
        }

        // Auto-shift (matches C++ logic with shift timer)
        if (shiftTimer <= 0f) {
            if (engineAngularVelocity > shiftUpRpm && currentGear < gearCount - 1) {
                currentGear++
                shiftTimer = GEAR_SHIFT_DELAY
            } else if (engineAngularVelocity < shiftDownRpm && currentGear > 0) {
                currentGear--
                shiftTimer = GEAR_SHIFT_DELAY
            }
        } else {
            shiftTimer -= dt
        }

        // Compute axle torque and distribute to engine wheels
        val engineTorque = acceleratorFraction * maxEngineTorque
        val axleTorque = engineTorque * gearRatio * finalDriveGearRatio
        val engineWheels = wheels.filterIsInstance<Wheel>().filter { it.hasFlag(SuspensionElement.ENGINE) }
        val perWheel = if (engineWheels.isNotEmpty()) axleTorque / engineWheels.size else 0f
        for (w in engineWheels) {
            w.axleTorque = perWheel
        }

        // Apply brakes
        if (isBraking) {
            for (w in wheels.filterIsInstance<Wheel>()) {
                w.setFlag(SuspensionElement.BRAKING)
            }
        } else {
            for (w in wheels.filterIsInstance<Wheel>()) {
                w.clearFlag(SuspensionElement.BRAKING)
            }
        }

        super.timestep(dt)
    }
}
