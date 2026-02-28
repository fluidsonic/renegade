package ccr.physics.moveable

import ccr.math.AABox
import ccr.math.Vector3
import ccr.physics.PhysController
import ccr.physics.PhysicsConstants
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.scene.PhysicsScene

abstract class Phys3Class : MoveablePhysClass() {

    companion object {
        // Mirrors C++ phys3.cpp constants (lines 325-326).
        const val GROUND_DISTANCE = 0.1f
        const val GROUND_EPSILON = GROUND_DISTANCE / 5.0f  // = 0.02f
    }

    enum class MovementMode { USER_OVERRIDE, BALLISTIC, SLIDE, NORMAL, COLLIDE }

    // Object-space collision box (mirrors C++ Phys3Class::CollisionBox).
    // collisionBoxCenter is the offset from the physics position to the box center.
    // C++ default: Center=(0,0,1), Extent=(1,1,1).
    // Kotlin: extent=(0.3,0.3,0.9), center=(0,0,0.9) -- box bottom at feet, top 1.8 units up.
    var collisionBoxCenter: Vector3 = Vector3(0f, 0f, 0.9f)
    var collisionBoxExtent: Vector3 = Vector3(0.3f, 0.3f, 0.9f)
    var normSpeed: Float = 3.0f
    /** cos(SlideAngle). C++ default: cos(45deg) = ~0.707. Matches C++ SlideNormalZ. */
    var slideAngleCos: Float = 0.707f
    var stepHeight: Float = 0.3f
    var groundState: GroundState = GroundState.NOT_ON_GROUND
    var currentMode: MovementMode = MovementMode.NORMAL

    /** Compute the world-space collision box. Mirrors C++ Compute_WS_Collision_Box. */
    fun computeWSCollisionBox(): AABox = AABox(position + collisionBoxCenter, collisionBoxExtent)

    /**
     * Mirrors C++ Phys3Class::Timestep.
     *
     * Mode transitions happen HERE based on ground state, NOT inside checkGround().
     * C++ Timestep (lines 1034-1067):
     *   gs = Get_Ground_State()
     *   if (!gs.OnGround) -> BALLISTIC
     *   else if (gs.Normal.Z < SlideNormalZ) -> SLIDE
     *   else -> NORMAL
     *
     * Gravity is only applied inside Ballistic_Move (as part of the kinematic equation),
     * NOT as a separate step in Timestep.
     */
    override fun timestep(dt: Float) {
        val sc = scene ?: return

        // C++ calls Get_Ground_State() which lazily calls Check_Ground.
        // We call checkGround() directly (no dirty-flag cache).
        checkGround(sc)

        // Mode transitions — mirrors C++ Timestep lines 1048-1066.
        if (!groundState.onGround) {
            currentMode = MovementMode.BALLISTIC
            ballisticMove(dt, sc)
        } else if (groundState.normal.z < slideAngleCos) {
            currentMode = MovementMode.SLIDE
            slideMove(dt, sc)
        } else {
            currentMode = MovementMode.NORMAL
            normalMove(dt, sc)
        }
    }

    /**
     * Mirrors C++ Phys3Class::Check_Ground EXACTLY.
     *
     * Pure ground-state-gathering function. NO mode transitions, NO velocity changes.
     *
     * C++ signature: Check_Ground(const AABoxClass & box, GroundStateStruct * gs, float check_dist)
     * Called with: Compute_WS_Collision_Box(State, &box); Check_Ground(box, &GroundState, GROUND_DISTANCE);
     *
     * 1. Cast wsBox downward by GROUND_DISTANCE (0.1f)
     * 2. If hit (fraction < 1.0): Init_From_Collision_Result(test, fraction * check_dist)
     *    - OnGround = true, Height = fraction * check_dist, Normal = result.Normal
     *    - Down = cross(cross(Normal, (0,0,-1)), Normal).normalize()
     * 3. If no hit: OnGround = false, SurfaceType = SURFACE_TYPE_DEFAULT
     */
    fun checkGround(scene: PhysicsScene) {
        val box = computeWSCollisionBox()
        val checkDist = GROUND_DISTANCE
        val move = Vector3(0f, 0f, -checkDist)
        val test = AABoxCollisionTest(box, move)
        val hitAny = scene.castAABox(test)

        if (hitAny && test.result.fraction < 1f) {
            // Mirrors Init_From_Collision_Result(test, result.Fraction * check_dist)
            val normal = test.result.normal
            val height = test.result.fraction * checkDist

            // Compute Down vector: N x -Z x N, normalized.
            // Matches C++ Init_From_Collision_Result lines 2352-2355.
            val negZ = Vector3(0f, 0f, -1f)
            val tmp = normal.cross(negZ)
            val down = tmp.cross(normal).let { d ->
                val len = d.length()
                if (len > 0f) d / len else Vector3(1f, 0f, 0f) // fallback matches C++ Reset() default
            }

            groundState = GroundState(
                onGround = true,
                surfaceType = test.result.surfaceType,
                height = height,
                normal = normal,
                down = down,
            )
        } else {
            groundState = GroundState.NOT_ON_GROUND
        }
    }

    /**
     * Mirrors C++ Phys3Class::Ballistic_Move EXACTLY.
     *
     * C++ (lines 1223-1253):
     *   accel = GravityAcceleration.Z * GravScale
     *   move.X = Velocity.X * dt
     *   move.Y = Velocity.Y * dt
     *   move.Z = 0.5 * accel * dt * dt + Velocity.Z * dt
     *   Apply_Move(move, dt)
     *   Velocity.X = (Position.X - start_pos.X) / dt
     *   Velocity.Y = (Position.Y - start_pos.Y) / dt
     *   Velocity.Z = start_vel.Z + accel * dt
     */
    fun ballisticMove(dt: Float, scene: PhysicsScene) {
        val accel = PhysicsConstants.gravityAcceleration.z * gravScale
        val startVel = velocity.copy()
        val startPos = position.copy()

        val move = Vector3(
            velocity.x * dt,
            velocity.y * dt,
            0.5f * accel * dt * dt + velocity.z * dt,
        )

        applyMove(move, scene)

        // Compute X,Y velocities from the actual move that was performed
        velocity = Vector3(
            (position.x - startPos.x) / dt,
            (position.y - startPos.y) / dt,
            startVel.z + accel * dt,
        )
    }

    /**
     * Mirrors C++ Phys3Class::Slide_Move EXACTLY.
     *
     * C++ (lines 1267-1278):
     *   start_pos = State.Position
     *   move = NormSpeed * dt * gs.Down
     *   Apply_Move(move, dt, true, false)
     *   Velocity = (Position - start_pos) / dt
     */
    fun slideMove(dt: Float, scene: PhysicsScene) {
        val startPos = position.copy()
        val move = groundState.down * (normSpeed * dt)
        applyMove(move, scene)
        velocity = (position - startPos) / dt
    }

    open fun normalMove(dt: Float, scene: PhysicsScene) {
        val ctrl = controller
        val moveVec = if (ctrl != null) computeDesiredMoveVector(ctrl) else Vector3.ZERO
        val speed = normSpeed
        val move = Vector3(moveVec.x * speed * dt, moveVec.y * speed * dt, 0f)
        applyMove(move, scene)
    }

    open fun computeDesiredMoveVector(ctrl: PhysController): Vector3 {
        val v = Vector3(ctrl.moveLeft, ctrl.moveForward, ctrl.moveUp)
        val len = v.length()
        return if (len > 1f) v / len else v
    }

    fun applyMove(move: Vector3, scene: PhysicsScene) {
        if (move.lengthSquared() < 1e-10f) return
        val pos = position
        val box = computeWSCollisionBox()
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
        val stepBox = AABox(position + collisionBoxCenter + Vector3(0f, 0f, stepHeight), collisionBoxExtent)
        val stepTest = AABoxCollisionTest(stepBox, remaining)
        if (!scene.castAABox(stepTest) || stepTest.result.fraction >= 1f) {
            position = position + Vector3(0f, 0f, stepHeight) + remaining
            // Snap back down
            val snapBox = computeWSCollisionBox()
            val snapTest = AABoxCollisionTest(snapBox, Vector3(0f, 0f, -stepHeight - 0.05f))
            if (scene.castAABox(snapTest) && snapTest.result.fraction < 1f) {
                position = position + Vector3(0f, 0f, -(stepHeight + 0.05f) * snapTest.result.fraction)
            }
            return
        }

        // Slide along wall: project remaining movement onto wall plane
        val dot = remaining.dot(hitNormal)
        val slideMove = remaining - hitNormal * dot
        if (slideMove.lengthSquared() > 1e-6f) {
            val slideBox = computeWSCollisionBox()
            val slideTest = AABoxCollisionTest(slideBox, slideMove)
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
