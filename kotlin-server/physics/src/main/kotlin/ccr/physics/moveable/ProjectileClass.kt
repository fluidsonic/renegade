package ccr.physics.moveable

import ccr.math.LineSeg
import ccr.math.Vector3
import ccr.physics.collision.RayCollisionTest

open class ProjectileClass : MoveablePhysClass() {

    // Matches C++ ProjectileClass orientation enum (projectile.h:98-103)
    enum class OrientationMode { ALIGNED, FIXED, TUMBLING, ALIGNED_FIXED }

    var collidesOnMove: Boolean = true              // C++ ProjectileClass::CollidesOnMove
    var orientationMode: OrientationMode = OrientationMode.ALIGNED  // C++ ProjectileClass::OrientationMode
    var tumbleAxis: Vector3 = Vector3(1f, 0f, 0f)  // C++ ProjectileClass::TumbleAxis (default 1,0,0)
    var tumbleRate: Float = 0.1745f                 // C++ ProjectileClass::TumbleRate (DEG_TO_RADF(10))
    var lifetime: Float = 2.0f                      // C++ ProjectileClass::Lifetime (default 2.0f)
    var bounceCount: Int = 0                        // C++ ProjectileClass::BounceCount
    var elapsed: Float = 0f
    var maxBounces: Int = 0
    var gravity: Float = 1.0f
    private var expired = false

    override fun asProjectileClass(): ProjectileClass = this

    override fun needsTimestep(): Boolean = true

    override fun timestep(dt: Float) {
        if (expired) return

        // Apply gravity
        velocity = velocity + Vector3(0f, 0f, -9.8f * gravity * dt)

        val sc = scene
        if (sc == null) {
            // No scene: just integrate position
            position = position + velocity * dt
            elapsed += dt
            if (elapsed >= lifetime) {
                expired = true
                observer?.objectExpired()
            }
            return
        }

        // Ray cast from current position to new position
        val startPos = position
        val move = velocity * dt
        val endPos = startPos + move
        val ray = LineSeg(startPos, endPos)
        val test = RayCollisionTest(ray)

        if (sc.castRay(test) && test.result.fraction < 1f) {
            val hitPoint = ray.pointAt(test.result.fraction)
            position = hitPoint
            if (bounceCount < maxBounces) {
                // Reflect velocity off hit normal
                val n = test.result.normal
                val velDot = velocity.dot(n)
                velocity = (velocity - n * (2f * velDot)) * elasticity
                bounceCount++
            } else {
                expired = true
                observer?.objectExpired()
                return
            }
        } else {
            position = endPos
        }

        // Update orientation
        when (orientationMode) {
            OrientationMode.ALIGNED -> {
                // Keep transform oriented along velocity direction
                transform = transform.withTranslation(position)
            }
            OrientationMode.TUMBLING -> {
                transform = transform.withTranslation(position)
            }
            OrientationMode.FIXED -> {}
            OrientationMode.ALIGNED_FIXED -> {
                transform = transform.withTranslation(position)
            }
        }

        elapsed += dt
        if (elapsed >= lifetime) {
            expired = true
            observer?.objectExpired()
        }
    }

    companion object {
        // Chunk IDs sourced from projectile.cpp Save()
        const val PROJECTILE_CHUNK_MOVEABLEPHYS: Int = 0x03308765      // parent class data
        const val PROJECTILE_CHUNK_VARIABLES: Int = 0x03308766

        // Micro-chunk IDs
        const val PROJECTILE_VARIABLE_POSITION: Int = 0x00
        const val PROJECTILE_VARIABLE_VELOCITY: Int = 0x01
        const val PROJECTILE_VARIABLE_COLLIDESONMOVE: Int = 0x02
        const val PROJECTILE_VARIABLE_ORIENTATIONMODE: Int = 0x03
        const val PROJECTILE_VARIABLE_TUMBLEAXIS: Int = 0x04
        const val PROJECTILE_VARIABLE_TUMBLERATE: Int = 0x05
        const val PROJECTILE_VARIABLE_LIFETIME: Int = 0x06
        const val PROJECTILE_VARIABLE_BOUNCECOUNT: Int = 0x07
    }
}
