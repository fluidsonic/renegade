package ccr.physics.moveable

import ccr.math.Vector3
import ccr.physics.PhysController
import kotlin.math.cos
import kotlin.math.sin

open class HumanPhysClass : Phys3Class() {
    var jumpSpeed: Float = 6.0f
    var crouchSpeedMultiplier: Float = 0.5f
    var isCrouching: Boolean = false
    var isJumping: Boolean = false
    var heading: Float = 0f // yaw in radians, 0 = +Y axis (forward)

    override fun timestep(dt: Float) {
        val ctrl = controller
        // Handle jump input
        if (ctrl != null && ctrl.moveUp > 0f && groundState.onGround && !isJumping) {
            velocity = Vector3(velocity.x, velocity.y, jumpSpeed)
            isJumping = true
            currentMode = MovementMode.BALLISTIC
        }
        if (groundState.onGround && velocity.z <= 0f) {
            isJumping = false
        }
        super.timestep(dt)
    }

    override fun computeDesiredMoveVector(ctrl: PhysController): Vector3 {
        val fwd = ctrl.moveForward
        val left = ctrl.moveLeft
        val cosH = cos(heading)
        val sinH = sin(heading)
        val worldX = fwd * sinH + left * cosH
        val worldY = fwd * cosH - left * sinH
        val speedMul = if (isCrouching) crouchSpeedMultiplier else 1.0f
        return Vector3(worldX * speedMul, worldY * speedMul, 0f)
    }
}
