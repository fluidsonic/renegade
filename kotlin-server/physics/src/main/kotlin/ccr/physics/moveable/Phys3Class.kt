package ccr.physics.moveable

import ccr.math.AABox
import ccr.math.Vector3
import ccr.physics.PhysController
import ccr.physics.PhysicsConstants
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.scene.PhysicsScene

abstract class Phys3Class : MoveablePhysClass() {

    enum class MovementMode { USER_OVERRIDE, BALLISTIC, SLIDE, NORMAL, COLLIDE }

    var collisionBoxExtent: Vector3 = Vector3(0.3f, 0.3f, 0.9f)
    var normSpeed: Float = 3.0f
    var slideAngleCos: Float = 0.5f
    var stepHeight: Float = 0.3f
    var groundState: GroundState = GroundState.NOT_ON_GROUND
    var currentMode: MovementMode = MovementMode.NORMAL

    override fun timestep(dt: Float) {
        val sc = scene ?: return
        checkGround(sc)

        when (currentMode) {
            MovementMode.BALLISTIC -> ballisticMove(dt, sc)
            MovementMode.SLIDE -> slideMove(dt, sc)
            MovementMode.NORMAL -> normalMove(dt, sc)
            MovementMode.USER_OVERRIDE -> {}
            MovementMode.COLLIDE -> ballisticMove(dt, sc)
        }

        // Apply gravity unless standing on ground with no upward velocity
        if (!groundState.onGround || velocity.z > 0f) {
            velocity = velocity + PhysicsConstants.gravityAcceleration * (gravScale * dt)
        } else if (groundState.onGround && velocity.z < 0f) {
            velocity = Vector3(velocity.x, velocity.y, 0f)
        }
    }

    fun checkGround(scene: PhysicsScene) {
        val pos = position
        val box = AABox(pos, collisionBoxExtent)
        val groundCastDist = stepHeight + 0.1f
        val move = Vector3(0f, 0f, -groundCastDist)
        val test = AABoxCollisionTest(box, move)
        if (scene.castAABox(test) && test.result.fraction < 1f) {
            val normal = test.result.normal
            val downSlope = normal.z < slideAngleCos
            groundState = GroundState(
                onGround = true,
                surfaceType = test.result.surfaceType,
                height = pos.z - groundCastDist * test.result.fraction,
                normal = normal,
                downSlope = downSlope,
            )
            if (downSlope) {
                currentMode = MovementMode.SLIDE
            } else if (currentMode == MovementMode.SLIDE) {
                currentMode = MovementMode.NORMAL
            }
        } else {
            groundState = GroundState.NOT_ON_GROUND
            if (currentMode == MovementMode.SLIDE) currentMode = MovementMode.BALLISTIC
        }
    }

    fun ballisticMove(dt: Float, scene: PhysicsScene) {
        val move = velocity * dt
        applyMove(move, scene)
    }

    fun slideMove(dt: Float, scene: PhysicsScene) {
        val slopeNormal = groundState.normal
        val projVel = velocity - slopeNormal * velocity.dot(slopeNormal)
        val move = projVel * dt
        applyMove(move, scene)
    }

    open fun normalMove(dt: Float, scene: PhysicsScene) {
        val ctrl = controller
        val moveVec = if (ctrl != null) computeDesiredMoveVector(ctrl) else Vector3.ZERO
        val speed = normSpeed
        val horizontalMove = Vector3(moveVec.x * speed * dt, moveVec.y * speed * dt, 0f)
        val vertMove = Vector3(0f, 0f, velocity.z * dt)
        applyMove(horizontalMove + vertMove, scene)
    }

    open fun computeDesiredMoveVector(ctrl: PhysController): Vector3 {
        val v = Vector3(ctrl.moveLeft, ctrl.moveForward, ctrl.moveUp)
        val len = v.length()
        return if (len > 1f) v / len else v
    }

    fun applyMove(move: Vector3, scene: PhysicsScene) {
        if (move.lengthSquared() < 1e-10f) return
        val pos = position
        val box = AABox(pos, collisionBoxExtent)
        val test = AABoxCollisionTest(box, move)
        if (!scene.castAABox(test) || test.result.fraction >= 1f) {
            position = pos + move
            return
        }

        val frac = test.result.fraction
        val hitNormal = test.result.normal

        // Move to collision point with small epsilon pullback
        val safeMove = move * maxOf(0f, frac - 0.001f)
        position = pos + safeMove

        // Try step-over for low walls
        val remaining = move * (1f - frac)
        val stepTest = AABoxCollisionTest(
            AABox(position + Vector3(0f, 0f, stepHeight), collisionBoxExtent),
            remaining,
        )
        if (!scene.castAABox(stepTest) || stepTest.result.fraction >= 1f) {
            position = position + Vector3(0f, 0f, stepHeight) + remaining
            // Snap back down
            val snapTest = AABoxCollisionTest(
                AABox(position, collisionBoxExtent),
                Vector3(0f, 0f, -stepHeight - 0.05f),
            )
            if (scene.castAABox(snapTest) && snapTest.result.fraction < 1f) {
                position = position + Vector3(0f, 0f, -(stepHeight + 0.05f) * snapTest.result.fraction)
            }
            return
        }

        // Slide along wall: project remaining movement onto wall plane
        val dot = remaining.dot(hitNormal)
        val slideMove = remaining - hitNormal * dot
        if (slideMove.lengthSquared() > 1e-6f) {
            val slideTest = AABoxCollisionTest(AABox(position, collisionBoxExtent), slideMove)
            if (!scene.castAABox(slideTest) || slideTest.result.fraction >= 1f) {
                position = position + slideMove
            } else {
                position = position + slideMove * maxOf(0f, slideTest.result.fraction - 0.001f)
            }
        }

        // Cancel velocity component into wall
        val velDot = velocity.dot(hitNormal)
        if (velDot < 0f) {
            velocity = velocity - hitNormal * velDot
        }
    }
}
