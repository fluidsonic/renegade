package ccr.physics.moveable

import ccr.math.Vector3
import ccr.physics.PhysController
import kotlin.math.abs
import kotlin.test.*

class HumanPhysClassTest {
    @Test
    fun jumpSetsVelocityZ() {
        val human = HumanPhysClass()
        human.groundState = GroundState(true, 0, 0f, Vector3(0f, 0f, 1f), false)
        human.controller = PhysController().also { it.moveUp = 1f }
        val ctrl = human.controller!!
        if (ctrl.moveUp > 0f && human.groundState.onGround && !human.isJumping) {
            human.velocity = Vector3(human.velocity.x, human.velocity.y, human.jumpSpeed)
            human.isJumping = true
        }
        assertTrue(abs(human.velocity.z - human.jumpSpeed) < 0.01f, "velocity.z=${human.velocity.z}")
        assertTrue(human.isJumping)
    }

    @Test
    fun crouchReducesSpeed() {
        val human = HumanPhysClass()
        val ctrl = PhysController()
        ctrl.moveForward = 1f
        human.controller = ctrl
        human.isCrouching = false
        val fullSpeed = human.computeDesiredMoveVector(ctrl).length()
        human.isCrouching = true
        val crouchSpeed = human.computeDesiredMoveVector(ctrl).length()
        assertTrue(crouchSpeed < fullSpeed, "crouch=$crouchSpeed, full=$fullSpeed")
        assertTrue(abs(crouchSpeed - fullSpeed * human.crouchSpeedMultiplier) < 0.01f)
    }

    @Test
    fun headingRotatesMoveDirection() {
        val human = HumanPhysClass()
        val ctrl = PhysController()
        ctrl.moveForward = 1f
        human.heading = 0f
        val v0 = human.computeDesiredMoveVector(ctrl)
        human.heading = (Math.PI / 2).toFloat()
        val v90 = human.computeDesiredMoveVector(ctrl)
        assertTrue(
            abs(v0.x - v90.x) > 0.5f || abs(v0.y - v90.y) > 0.5f,
            "v0=$v0, v90=$v90 -- heading rotation should change direction",
        )
    }
}
