package ccr.physics.vehicle

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass

// C++: VehiclePhysClass in wwphys/vehiclephys.h
abstract class VehiclePhysClass : RigidBodyClass() {

    companion object {
        private const val EXPIRE_SECONDS = 4.0f

        // Chunk IDs sourced from vehiclephys.cpp Save()
        const val VEHICLEPHYS_CHUNK_RIGIDBODY: Int = 405001445       // wraps parent RigidBodyClass
        const val VEHICLEPHYS_CHUNK_VARIABLES: Int = 405001446       // own data

        // Micro-chunk IDs within VEHICLEPHYS_CHUNK_VARIABLES:
        const val VEHICLEPHYS_VARIABLE_ISENGINEON: Int = 0x00
    }

    val wheels = mutableListOf<SuspensionElement>()

    // Definition-like fields (C++: in VehiclePhysDefClass)
    var springConstant: Float = 5000f
    var dampingConstant: Float = 500f
    var springLength: Float = 0.5f
    var tractionMultiplier: Float = 1.0f
    var isFake: Boolean = false

    // Instance fields (C++: VehiclePhysClass)
    var isEngineOn: Boolean = true
    var realWheelCount: Int = 0
    var driveWheelCount: Int = 0
    var lastGoodPosition: Matrix3D = Matrix3D.IDENTITY
    var expireTimer: Float = EXPIRE_SECONDS

    override fun asVehiclePhysClass(): VehiclePhysClass = this

    abstract fun allocSuspensionElement(): SuspensionElement

    fun addWheel(position: Vector3, flags: Int = 0): SuspensionElement {
        val wheel = allocSuspensionElement()
        wheel.suspensionPosition = position
        wheel.springConstant = springConstant
        wheel.dampingConstant = dampingConstant
        wheel.springRestLength = springLength
        wheel.flags = flags
        wheels.add(wheel)
        return wheel
    }

    override fun timestep(dt: Float) {
        if (isFake) return
        super.timestep(dt)
    }

    override fun computeForceAndTorque() {
        super.computeForceAndTorque()
        // Add wheel forces
        val force = MutableForce()
        val torque = MutableForce()
        for (wheel in wheels) {
            if (wheel.hasFlag(SuspensionElement.DISABLED)) continue
            wheel.computeForceAndTorque(this, force, torque)
        }
        addForce(force.toVector3() * tractionMultiplier)
        addTorque(torque.toVector3() * tractionMultiplier)
    }
}
