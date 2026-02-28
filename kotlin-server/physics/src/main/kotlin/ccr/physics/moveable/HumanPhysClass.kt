package ccr.physics.moveable

import ccr.math.Vector3
import ccr.physics.PhysController
import ccr.physics.scene.PhysicsScene
import kotlin.math.cos
import kotlin.math.sin

open class HumanPhysClass : Phys3Class() {
    var jumpSpeed: Float = 6.0f
    var crouchSpeedMultiplier: Float = 0.5f
    var isCrouching: Boolean = false
    var justJumped: Boolean = false              // C++ HumanPhysClass::JustJumped
    var isAIControlledJump: Boolean = false       // C++ HumanPhysClass::IsAIControlledJump

    override fun asHumanPhysClass(): HumanPhysClass = this

    override fun timestep(dt: Float) {
        val ctrl = controller
        // Handle jump input
        if (ctrl != null && ctrl.moveUp > 0f && groundState.onGround && !justJumped) {
            velocity = Vector3(velocity.x, velocity.y, jumpSpeed)
            justJumped = true
            moveMode = MoveMode.BALLISTIC_MOVE
        }
        if (groundState.onGround && velocity.z <= 0f) {
            justJumped = false
        }
        super.timestep(dt)
    }

    /**
     * Override normalMove to add the post-move snap-to-ground step and Z-velocity clamp,
     * mirroring C++ HumanPhysClass::Normal_Move (humanphys.cpp lines 306–346).
     *
     * C++ steps:
     *   1. Compute_Desired_Move_Vector (projects onto ground plane)
     *   2. Apply_Move
     *   3. Snap_To_Ground (if !JustJumped)
     *   4. Clamp State.Velocity.Z to <= 0 (if !JustJumped)
     */
    override fun normalMove(dt: Float, scene: PhysicsScene) {
        val startPos = position
        super.normalMove(dt, scene)
        if (!justJumped) {
            snapToGround(position - startPos, scene)
            // Clamp Z velocity: humans should never launch off slopes (mirrors C++ line 343)
            velocity = Vector3(velocity.x, velocity.y, minOf(velocity.z, 0f))
        }
    }

    override fun computeDesiredMoveVector(ctrl: PhysController): Vector3 {
        val fwd = ctrl.moveForward
        val left = ctrl.moveLeft
        val cosH = cos(heading)
        val sinH = sin(heading)
        // C++ Rotate_Z(Heading): newX = X*cos - Y*sin, newY = X*sin + Y*cos
        // Input vector is (left, fwd, 0) — left is X, fwd is Y in C++
        val worldX = left * cosH - fwd * sinH
        val worldY = left * sinH + fwd * cosH
        val speedMul = if (isCrouching) crouchSpeedMultiplier else 1.0f
        return Vector3(worldX * speedMul, worldY * speedMul, 0f)
    }

    companion object {
        // Chunk IDs sourced from humanphys.cpp Save()
        const val HUMANPHYS_CHUNK_PHYS3: Int = 0x04040404          // parent Phys3Class data
        const val HUMANPHYS_CHUNK_VARIABLES: Int = 0x04040405

        // Micro-chunk IDs
        const val HUMANPHYS_VARIABLE_JUSTJUMPED: Int = 0x00
    }
}
