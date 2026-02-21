package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.rigidbody.RigidBodyClass

// C++: SuspensionElementClass in wwphys/suspension.h
abstract class SuspensionElement {

    companion object {
        const val FAKE = 0x01
        const val STEERING = 0x02
        const val INV_STEERING = 0x04
        const val TILT_STEERING = 0x08
        const val ENGINE = 0x10
        const val LEFT_TRACK = 0x20
        const val RIGHT_TRACK = 0x40
        const val DISABLED = 0x80
        const val IN_CONTACT = 0x100
        const val BRAKING = 0x200
    }

    var flags: Int = 0
    var springRestLength: Float = 0.5f
    var springConstant: Float = 5000f
    var dampingConstant: Float = 500f
    var suspensionPosition: Vector3 = Vector3.ZERO // vehicle body-space

    fun hasFlag(flag: Int): Boolean = (flags and flag) != 0
    fun setFlag(flag: Int) { flags = flags or flag }
    fun clearFlag(flag: Int) { flags = flags and flag.inv() }

    abstract fun computeForceAndTorque(vehicle: RigidBodyClass, force: MutableForce, torque: MutableForce)
}

/** Mutable accumulator for force/torque to avoid allocating Vector3 on every call. */
class MutableForce(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun add(v: Vector3) { x += v.x; y += v.y; z += v.z }
    fun toVector3() = Vector3(x, y, z)
}
