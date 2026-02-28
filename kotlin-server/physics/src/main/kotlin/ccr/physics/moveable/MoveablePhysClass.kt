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
    var elasticity: Float = 0.5f  // C++ default: 0.5f (movephys.cpp:52)
    var controller: PhysController? = null
    var carrier: ccr.physics.PhysClass? = null
    var velocity: Vector3 = Vector3()

    var position: Vector3
        get() = transform.translation
        set(v) {
            transform = transform.withTranslation(v)
        }

    override fun asMoveablePhysClass(): MoveablePhysClass = this

    override fun needsTimestep(): Boolean = true

    companion object {
        // Chunk IDs sourced from movephys.cpp Save()
        const val MOVEABLE_CHUNK_PHYS: Int = 0x00803000            // obsolete parent class
        const val MOVEABLE_CHUNK_VARIABLES: Int = 0x00803001
        const val MOVEABLE_CHUNK_DYNAMICPHYS: Int = 0x00803002     // current parent class

        // Micro-chunk IDs
        const val MOVEABLE_VARIABLE_MASS: Int = 0x00
        const val MOVEABLE_VARIABLE_GRAVSCALE: Int = 0x01
        const val MOVEABLE_VARIABLE_ELASTICITY: Int = 0x02
        const val MOVEABLE_VARIABLE_CONTROLLER: Int = 0x03
        const val MOVEABLE_VARIABLE_CARRIER: Int = 0x04
    }
}
