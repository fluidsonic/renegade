package ccr.physics.moveable

import ccr.math.Vector3
import ccr.physics.PhysController
import ccr.physics.dynamic.DynamicPhysClass

abstract class MoveablePhysClass : DynamicPhysClass() {
    var mass: Float = 1.0f
        set(v) {
            require(v > 0f) { "mass must be positive" }
            field = v
        }
    val massInv: Float get() = 1f / mass
    var gravScale: Float = 1.0f
    var elasticity: Float = 0.3f
    var controller: PhysController? = null
    var carrier: ccr.physics.PhysClass? = null
    var velocity: Vector3 = Vector3()

    var position: Vector3
        get() = transform.translation
        set(v) {
            transform = transform.withTranslation(v)
        }

    override fun needsTimestep(): Boolean = true
}
