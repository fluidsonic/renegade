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

        // Chunk IDs sourced from phys3.cpp Save()
        const val PHYS3_CHUNK_MOVEABLEPHYS: Int = 0x00483200
        const val PHYS3_CHUNK_VARIABLES: Int = 0x00483201

        // Micro-chunk IDs
        const val PHYS3_VARIABLE_COLLISION_AABOX: Int = 0x00
        const val PHYS3_VARIABLE_ONGROUND: Int = 0x01
        const val PHYS3_VARIABLE_INCOLLISION: Int = 0x02
        const val PHYS3_VARIABLE_HEADING: Int = 0x03
        const val PHYS3_VARIABLE_NORMSPEED: Int = 0x04
        const val PHYS3_VARIABLE_SLIDEANGLE: Int = 0x05
        const val PHYS3_VARIABLE_SLIDENORMALZ: Int = 0x06
        const val PHYS3_VARIABLE_SLIDEANGLETAN: Int = 0x07
        const val PHYS3_VARIABLE_STEPHEIGHT: Int = 0x08
        const val PHYS3_VARIABLE_MOVEMODE: Int = 0x09
        const val PHYS3_VARIABLE_POSITION: Int = 0x0A
        const val PHYS3_VARIABLE_VELOCITY: Int = 0x0B
    }

    // Matches C++ Phys3Class::MoveModeType enum (phys3.h:184-191)
    enum class MoveMode { USER_OVERRIDE, BALLISTIC_MOVE, SLIDE_MOVE, NORMAL_MOVE, COLLIDE_MOVE }

    // Object-space collision box (mirrors C++ Phys3Class::CollisionBox, AABoxClass with Center+Extent).
    // Kotlin: extent=(0.3,0.3,0.9), center=(0,0,0.9) -- box bottom at feet, top 1.8 units up.
    var collisionBox: AABox = AABox(Vector3(0f, 0f, 0.9f), Vector3(0.3f, 0.3f, 0.9f))
    var onGround: Boolean = false           // C++ Phys3Class::OnGround — Is_In_Contact() flag
    var inCollision: Boolean = false        // C++ Phys3Class::InCollision
    var heading: Float = 0f                 // C++ Phys3Class::Heading — yaw in radians
    var normSpeed: Float = 3.0f             // C++ Phys3Class::NormSpeed
    var slideAngle: Float = 0.7854f         // C++ Phys3Class::SlideAngle — radians (default ~45 deg)
    var slideNormalZ: Float = 0.707f        // C++ Phys3Class::SlideNormalZ — cos(SlideAngle)
    var slideAngleTan: Float = 1.0f         // C++ Phys3Class::SlideAngleTan — tan(SlideAngle)
    var stepHeight: Float = 0.3f            // C++ Phys3Class::StepHeight
    var moveMode: MoveMode = MoveMode.NORMAL_MOVE  // C++ Phys3Class::MoveMode
    var groundState: GroundState = GroundState.NOT_ON_GROUND

    override fun asPhys3Class(): Phys3Class = this

    /** Compute the world-space collision box. Mirrors C++ Compute_WS_Collision_Box. */
    fun computeWSCollisionBox(): AABox = AABox(position + collisionBox.center, collisionBox.extent)

    /** Compute the world-space collision box at an arbitrary position. */
    fun computeWSCollisionBoxAt(pos: Vector3): AABox = AABox(pos + collisionBox.center, collisionBox.extent)

    /**
     * Mirrors C++ Phys3Class::Can_Teleport(test_tm, check_dyn_only=true).
     * Checks if this object can occupy the specified position without overlapping
     * any dynamic objects in the scene.
     *
     * @param testPos the position to test
     * @return true if the position is clear (no dynamic object intersections)
     */
    fun canTeleport(testPos: Vector3): Boolean {
        val sc = scene ?: return true  // no scene = assume safe
        val box = computeWSCollisionBoxAt(testPos)
        return !sc.intersectionTestDynamic(box, this)
    }

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
            moveMode = MoveMode.BALLISTIC_MOVE
            ballisticMove(dt, sc)
        } else if (groundState.normal.z < slideNormalZ) {
            moveMode = MoveMode.SLIDE_MOVE
            slideMove(dt, sc)
        } else {
            moveMode = MoveMode.NORMAL_MOVE
            normalMove(dt, sc)
        }
    }

    /**
     * After a ground move, snap the soldier back down to the ground surface.
     * Mirrors C++ Phys3Class::Snap_To_Ground.
     *
     * The snap distance accounts for the Z displacement of the actual move plus the
     * horizontal distance (to follow slopes) plus GROUND_DISTANCE to ensure we
     * actually touch the ground.
     */
    fun snapToGround(actualMove: Vector3, scene: PhysicsScene) {
        val deltaZ = actualMove.z
        val deltaXY = kotlin.math.sqrt(actualMove.x * actualMove.x + actualMove.y * actualMove.y)

        // Only snap when we moved upward or horizontally (could have stepped or gone over a crest)
        if (deltaZ + deltaXY <= 0f) return

        val snapDist = deltaZ + deltaXY * slideAngleTan + GROUND_DISTANCE

        val box = computeWSCollisionBox()
        val test = AABoxCollisionTest(box, Vector3(0f, 0f, -snapDist))
        val hit = scene.castAABox(test)

        if (!hit || test.result.fraction <= 0f || test.result.fraction > 1f) {
            // Case 3: ground not within snap distance — soldier is airborne
            moveMode = MoveMode.BALLISTIC_MOVE
            return
        }

        if (test.result.normal.z > slideNormalZ) {
            // Case 1: walkable slope — snap down to GROUND_EPSILON above surface
            val snapDown = snapDist * test.result.fraction
            if (snapDown > GROUND_EPSILON) {
                position = Vector3(position.x, position.y, position.z - (snapDown - GROUND_EPSILON))
            }
        }
        // Case 2: non-walkable slope — don't snap (slide mode handles it)
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
        // WORKAROUND: Lift the cast box by GROUND_EPSILON before sweeping.
        // Our CollisionMath.collide(box, move, tri) has a SAT bug at t=0: when the box bottom
        // is flush with a triangle, the contact-center lies outside the box extent and the SAT
        // rejects the hit. C++ doesn't need this because its swept-box test handles t=0 correctly.
        // The lift ensures t > 0, making the SAT check pass. The extra lift is added to the cast
        // distance so the total reach is unchanged.
        val wsBox = computeWSCollisionBox()
        val liftAmount = GROUND_EPSILON
        val checkDist = GROUND_DISTANCE + liftAmount
        val box = AABox(wsBox.center + Vector3(0f, 0f, liftAmount), wsBox.extent)
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

        // Try step-over for low walls: lift box by stepHeight, retry remaining move
        val remaining = move * (1f - frac)
        val stepBox = AABox(position + collisionBox.center + Vector3(0f, 0f, stepHeight), collisionBox.extent)
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
