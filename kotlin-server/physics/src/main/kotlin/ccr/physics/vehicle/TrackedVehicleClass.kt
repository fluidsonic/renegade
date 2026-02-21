package ccr.physics.vehicle

import ccr.math.Vector3

// C++: TrackedVehicleClass in wwphys/tvehicle.h
open class TrackedVehicleClass : VehiclePhysClass() {

    var maxEngineTorque: Float = 2000f

    override fun allocSuspensionElement(): SuspensionElement = TrackWheel()

    override fun timestep(dt: Float) {
        if (isFake) return

        val ctrl = controller
        val throttle = ctrl?.moveForward?.coerceIn(-1f, 1f) ?: 0f
        val turn = ctrl?.turnLeft?.coerceIn(-1f, 1f) ?: 0f

        // Differential steering: distribute torque to left/right tracks
        val baseTorque = throttle * maxEngineTorque
        val turnTorque = turn * maxEngineTorque * 0.5f

        for (w in wheels.filterIsInstance<TrackWheel>()) {
            w.axleTorque = when {
                w.hasFlag(SuspensionElement.LEFT_TRACK) -> baseTorque + turnTorque
                w.hasFlag(SuspensionElement.RIGHT_TRACK) -> baseTorque - turnTorque
                else -> baseTorque
            }
        }

        super.timestep(dt)
    }
}
