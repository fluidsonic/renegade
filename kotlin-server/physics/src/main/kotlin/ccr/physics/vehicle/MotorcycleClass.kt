package ccr.physics.vehicle

import ccr.math.Vector3

// C++: MotorcycleClass in wwphys/motorcycle.h
open class MotorcycleClass : WheeledVehicleClass() {

    companion object {
        // Chunk IDs sourced from motorcycle.cpp Save()
        const val MOTORCYCLE_CHUNK_WHEELEDVEHICLE: Int = 0x06511110  // wraps parent WheeledVehicleClass
        const val MOTORCYCLE_CHUNK_VARIABLES: Int = 0x06511111       // own data

        // Micro-chunk IDs within MOTORCYCLE_CHUNK_VARIABLES:
        const val MOTORCYCLE_VARIABLE_LEANK0: Int = 0x00
        const val MOTORCYCLE_VARIABLE_LEANK1: Int = 0x01
    }

    // Instance fields (C++: MotorcycleClass)
    var leanK0: Float = 18.0f     // C++: LeanK0 — spring constant for lean correction
    var leanK1: Float = 5.0f      // C++: LeanK1 — damping constant for lean correction
    var leanValue: Float = 0f     // C++: LeanValue — how much the character should lean

    override fun asMotorcycleClass(): MotorcycleClass = this

    override fun computeForceAndTorque() {
        super.computeForceAndTorque()

        // Self-balancing: try to keep roll = 0 (matches C++ Compute_Force_And_Torque)
        val right = transform.rotateVector(Vector3(1f, 0f, 0f))
        val forward = transform.rotateVector(Vector3(0f, 1f, 0f))
        val worldUp = Vector3(0f, 0f, 1f)
        val rollError = right.dot(worldUp)

        // Restoring torque using LeanK0 (spring) and LeanK1 (damping)
        val droll = angularVelocity.dot(forward)
        addTorque(forward * (-rollError * leanK0 - droll * leanK1))

        // Lean into turns based on steering
        val ctrl = controller
        val turnInput = ctrl?.turnLeft ?: 0f
        val speed = velocity.length()
        val leanTarget = turnInput * (speed * 0.1f).coerceAtMost(1f)
        leanValue = leanTarget - rollError
    }
}
