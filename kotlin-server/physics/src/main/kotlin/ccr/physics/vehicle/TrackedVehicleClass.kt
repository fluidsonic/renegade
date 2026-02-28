package ccr.physics.vehicle

import ccr.math.Vector3

// C++: TrackedVehicleClass in wwphys/trackedvehicle.h
open class TrackedVehicleClass : VehiclePhysClass() {

    companion object {
        // Chunk IDs sourced from trackedvehicle.cpp Save()
        const val TRACKEDVEHICLE_CHUNK_VEHICLEPHYS: Int = 0x00119801  // wraps parent VehiclePhysClass
        const val TRACKEDVEHICLE_CHUNK_VARIABLES: Int = 0x00119802    // own data (no micro-chunks written)
    }

    // Definition-like fields (C++: in TrackedVehicleDefClass)
    var maxEngineTorque: Float = 2000f
    var turnTorqueScaleFactor: Float = 1.0f

    // Instance fields (C++: TrackedVehicleClass)
    var leftTrackTorque: Float = 0f
    var rightTrackTorque: Float = 0f
    var leftTrackMovement: Float = 0f
    var rightTrackMovement: Float = 0f
    var leftTrackLastPosition: Vector3 = Vector3.ZERO
    var rightTrackLastPosition: Vector3 = Vector3.ZERO

    override fun asTrackedVehicleClass(): TrackedVehicleClass = this

    override fun allocSuspensionElement(): SuspensionElement = TrackWheel()

    override fun timestep(dt: Float) {
        if (isFake) return

        val ctrl = controller
        val forward = ctrl?.moveForward?.coerceIn(-1f, 1f) ?: 0f
        val left = ctrl?.let {
            if (kotlin.math.abs(it.turnLeft) > kotlin.math.abs(it.moveLeft)) it.turnLeft
            else it.moveLeft
        }?.coerceIn(-1f, 1f) ?: 0f

        // Compute differential steering torques (matches C++ Compute_Force_And_Torque)
        val turnScale = 1.0f - kotlin.math.abs(left) * (1.0f - turnTorqueScaleFactor)
        leftTrackTorque = (forward - left) * maxEngineTorque * turnScale
        rightTrackTorque = (forward + left) * maxEngineTorque * turnScale

        for (w in wheels.filterIsInstance<TrackWheel>()) {
            w.axleTorque = when {
                w.hasFlag(SuspensionElement.LEFT_TRACK) -> leftTrackTorque
                w.hasFlag(SuspensionElement.RIGHT_TRACK) -> rightTrackTorque
                else -> 0f
            }
        }

        super.timestep(dt)
    }
}
