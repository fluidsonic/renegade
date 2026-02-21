package ccr.physics.vehicle

import ccr.math.Vector3

// C++: MotorcycleClass in wwphys/motorcycle.h
open class MotorcycleClass : WheeledVehicleClass() {

    var leanGain: Float = 5.0f

    override fun computeForceAndTorque() {
        super.computeForceAndTorque()

        // Self-balancing: try to keep roll = 0
        val right = transform.rotateVector(Vector3(1f, 0f, 0f))
        val forward = transform.rotateVector(Vector3(0f, 1f, 0f))
        val worldUp = Vector3(0f, 0f, 1f)
        val rollError = right.dot(worldUp)

        // Restoring torque to stay upright
        addTorque(forward * (-rollError * leanGain))

        // Lean into turns based on steering
        val ctrl = controller
        val turnInput = ctrl?.turnLeft ?: 0f
        val speed = velocity.length()
        val leanTarget = turnInput * (speed * 0.1f).coerceAtMost(1f) // lean more at speed
        addTorque(forward * (leanTarget * leanGain * 0.5f))
    }
}
