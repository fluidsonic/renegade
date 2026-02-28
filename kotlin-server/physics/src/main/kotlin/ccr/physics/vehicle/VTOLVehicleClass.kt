package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.math.atan2

// C++: VTOLVehicleClass in wwphys/vtolvehicle.h
open class VTOLVehicleClass : VehiclePhysClass() {

    companion object {
        // Chunk IDs sourced from vtolvehicle.cpp Save()
        const val VTOLVEHICLE_CHUNK_VEHICLEPHYS: Int = 407001941  // wraps parent VehiclePhysClass
        const val VTOLVEHICLE_CHUNK_VARIABLES: Int = 407001942    // own data (no micro-chunks written)
    }

    // Definition-like fields (C++: in VTOLVehicleDefClass)
    var maxVerticalAcceleration: Float = 0f        // C++: MaxVerticalAcceleration
    var maxHorizontalAcceleration: Float = 0f      // C++: MaxHorizontalAcceleration
    var maxFuselagePitch: Float = 0.2618f          // C++: MaxFuselagePitch (DEG_TO_RAD(15))
    var maxFuselageRoll: Float = 0.3491f           // C++: MaxFuselageRoll (DEG_TO_RAD(20))
    var pitchControllerGain: Float = 45.5f         // C++: PitchControllerGain
    var pitchControllerDamping: Float = 12.75f     // C++: PitchControllerDamping
    var rollControllerGain: Float = 45.5f          // C++: RollControllerGain
    var rollControllerDamping: Float = 12.75f      // C++: RollControllerDamping
    var maxYawVelocity: Float = 3.1416f            // C++: MaxYawVelocity (DEG_TO_RAD(180))
    var yawControllerGain: Float = 5.0f            // C++: YawControllerGain

    // Instance fields (C++: VTOLVehicleClass visual state)
    var normalizedEngineRotation: Float = 0f       // C++: NormalizedEngineRotation
    var normalizedEngineThrust: Float = 0f         // C++: NormalizedEngineThrust
    var rotorAngle: Float = 0f                     // C++: RotorAngle
    var rotorAngularVelocity: Float = 0f           // C++: RotorAngularVelocity

    override fun asVTOLVehicleClass(): VTOLVehicleClass = this

    override fun allocSuspensionElement(): SuspensionElement = VTOLWheel()

    override fun computeForceAndTorque() {
        super.computeForceAndTorque()

        val ctrl = controller
        val moveUp = ctrl?.moveUp?.coerceIn(-1f, 1f) ?: 0f
        val moveForward = ctrl?.moveForward?.coerceIn(-1f, 1f) ?: 0f
        val turnLeft = ctrl?.turnLeft?.coerceIn(-1f, 1f) ?: 0f
        val moveLeft = ctrl?.moveLeft?.coerceIn(-1f, 1f) ?: 0f

        if (isEngineOn) {
            // Lift force along vehicle's up axis
            val up = transform.rotateVector(Vector3(0f, 0f, 1f))
            val forward = transform.rotateVector(Vector3(1f, 0f, 0f))
            val right = transform.rotateVector(Vector3(0f, 1f, 0f))

            // PD orientation stabilization torques (matches C++)
            val pitchVal = kotlin.math.atan2(-forward.z, kotlin.math.sqrt(forward.x * forward.x + forward.y * forward.y))
            val rollVal = kotlin.math.atan2(right.z, kotlin.math.sqrt(right.x * right.x + right.y * right.y))
            val dpitch = angularVelocity.dot(right)
            val droll = angularVelocity.dot(forward)

            val targetPitch = moveForward * maxFuselagePitch
            val targetRoll = -(moveLeft + turnLeft) * maxFuselageRoll

            val pitchTorque = right * ((pitchControllerGain * (targetPitch - pitchVal) + pitchControllerDamping * (0f - dpitch)) * iBody.m11)
            val rollTorque = forward * ((rollControllerGain * (targetRoll - rollVal) + rollControllerDamping * (0f - droll)) * iBody.m00)

            // Yaw controller
            val yawVelocity = angularVelocity.dot(up)
            val desiredYawVelocity = turnLeft * maxYawVelocity
            val yawTorque = up * (yawControllerGain * (desiredYawVelocity - yawVelocity) * iBody.m22)

            addTorque(pitchTorque + rollTorque + yawTorque)

            // Translational forces
            val forwardForce = moveForward * maxHorizontalAcceleration
            val leftForce = moveLeft * maxHorizontalAcceleration

            addForce(forward * forwardForce)
            addForce(right * leftForce)

            // Lift
            val upForce = -gravScale * mass * ccr.physics.PhysicsConstants.gravityAcceleration.z + maxVerticalAcceleration * moveUp
            addForce(Vector3(0f, 0f, upForce))

            // Update visual state
            if (maxFuselagePitch > 0f) {
                normalizedEngineRotation = -pitchVal / maxFuselagePitch
            }
            normalizedEngineThrust = 0.25f +
                (if (maxFuselagePitch > 0f) kotlin.math.abs(pitchVal) / maxFuselagePitch else 0f) +
                (if (maxFuselageRoll > 0f) kotlin.math.abs(rollVal) / maxFuselageRoll else 0f) +
                moveUp
            normalizedEngineThrust = normalizedEngineThrust.coerceIn(0f, 1f)
        } else {
            normalizedEngineThrust = 0f
            normalizedEngineRotation = 0f
        }
    }
}
