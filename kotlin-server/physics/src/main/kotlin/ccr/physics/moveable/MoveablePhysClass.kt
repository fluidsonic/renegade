package ccr.physics.moveable

import ccr.math.Matrix3D
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

    // Kotlin property — JVM getter is getVelocity() / setter is setVelocity().
    // Annotated with @JvmName to avoid clash with the explicit convenience methods below.
    @get:JvmName("velocityField") @set:JvmName("setVelocityField")
    var velocity: Vector3 = Vector3()

    // Kotlin property — JVM getter is getPosition() / setter is setPosition().
    // Annotated with @JvmName to avoid clash with the convenience methods inherited from PhysClass.
    @get:JvmName("positionField") @set:JvmName("setPositionField")
    var position: Vector3
        get() = transform.translation
        set(v) {
            transform = transform.withTranslation(v)
        }

    override fun asMoveablePhysClass(): MoveablePhysClass = this

    override fun needsTimestep(): Boolean = true

    // C++: Get_Position() — exposes position as the canonical getPosition() method.
    override fun getPosition(): Vector3 = position

    // C++: Set_Position() — delegates to the position property setter.
    override fun setPosition(pos: Vector3) { position = pos }

    // C++: Set_Velocity() / Get_Velocity()
    // Convenience methods matching the C++ PhysClass interface so server code can call them
    // on a ccr.physics.moveable.MoveablePhysClass reference without needing the stub.
    // Note: getAngularVelocity() is intentionally absent — it would clash with the JVM getter
    // of the `angularVelocity` property in RigidBodyClass. Server code uses the property directly.
    open fun setVelocity(vel: Vector3) { velocity = vel }
    open fun getVelocity(): Vector3 = velocity

    // C++: Cinematic_Move_To() — teleports the object to [tm] without physics simulation.
    open fun cinematicMoveTo(tm: Matrix3D) { transform = tm }

    // C++: Remove_Effect_From_Me() — no-op stub (material effect system not yet ported).
    open fun removeEffectFromMe(effect: Any) {}

    // C++: Compute_Approximate_Ride_Height() — stub; overridden by VehiclePhysClass.
    open fun computeApproximateRideHeight(): Float = 0f

    // C++: Get_Squish_Velocity() — stub returns 0.
    open fun getSquishVelocity(): Float = 0f

    // C++: Set_Gravity_Multiplier() — stub no-op.
    open fun setGravityMultiplier(multiplier: Float) {}

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
