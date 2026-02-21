package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass

// C++: VehiclePhysClass in wwphys/vehphys.h
abstract class VehiclePhysClass : RigidBodyClass() {

    val wheels = mutableListOf<SuspensionElement>()
    var springConstant: Float = 5000f
    var dampingConstant: Float = 500f
    var springLength: Float = 0.5f
    var tractionMultiplier: Float = 1.0f
    var isFake: Boolean = false
    var isEngineOn: Boolean = true
    var lastGoodPosition: Vector3 = Vector3.ZERO

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
