package ccr.physics

import ccr.math.Matrix3D
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.OBBoxCollisionTest
import ccr.physics.collision.RayCollisionTest

abstract class PhysClass {
    var scene: ccr.physics.scene.PhysicsScene? = null

    companion object {
        const val COLLISION_GROUP_MASK = 0x0F
        const val IMMOVABLE_BIT = 0x10
        const val ASLEEP_BIT = 0x20
        const val DISABLED_BIT = 0x40
        const val FRICTION_DISABLED_BIT = 0x80
        const val SIMULATION_DISABLED_BIT = 0x100
        const val IGNORE_COUNT_SHIFT = 12
        const val IGNORE_COUNT_MASK = 0xF
    }

    var flags: Int = 0
    var observer: PhysObserver? = null

    abstract var transform: Matrix3D

    var collisionGroup: Int
        get() = flags and COLLISION_GROUP_MASK
        set(v) {
            flags = (flags and COLLISION_GROUP_MASK.inv()) or (v and COLLISION_GROUP_MASK)
        }

    var isImmovable: Boolean
        get() = (flags and IMMOVABLE_BIT) != 0
        set(v) {
            flags = if (v) flags or IMMOVABLE_BIT else flags and IMMOVABLE_BIT.inv()
        }

    var isAsleep: Boolean
        get() = (flags and ASLEEP_BIT) != 0
        set(v) {
            flags = if (v) flags or ASLEEP_BIT else flags and ASLEEP_BIT.inv()
        }

    var isDisabled: Boolean
        get() = (flags and DISABLED_BIT) != 0
        set(v) {
            flags = if (v) flags or DISABLED_BIT else flags and DISABLED_BIT.inv()
        }

    var isFrictionDisabled: Boolean
        get() = (flags and FRICTION_DISABLED_BIT) != 0
        set(v) {
            flags = if (v) flags or FRICTION_DISABLED_BIT else flags and FRICTION_DISABLED_BIT.inv()
        }

    var isSimulationDisabled: Boolean
        get() = (flags and SIMULATION_DISABLED_BIT) != 0
        set(v) {
            flags = if (v) flags or SIMULATION_DISABLED_BIT else flags and SIMULATION_DISABLED_BIT.inv()
        }

    var ignoreCount: Int
        get() = (flags ushr IGNORE_COUNT_SHIFT) and IGNORE_COUNT_MASK
        set(v) {
            flags = (flags and (IGNORE_COUNT_MASK shl IGNORE_COUNT_SHIFT).inv()) or
                ((v and IGNORE_COUNT_MASK) shl IGNORE_COUNT_SHIFT)
        }

    open fun timestep(dt: Float) {}
    open fun postTimestepProcess() {}
    open fun needsTimestep(): Boolean = false
    open fun castRay(test: RayCollisionTest): Boolean = false
    open fun castAABox(test: AABoxCollisionTest): Boolean = false
    open fun castOBBox(test: OBBoxCollisionTest): Boolean = false
}
