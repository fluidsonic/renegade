package ccr.physics.moveable

import ccr.math.LineSeg
import ccr.math.Vector3
import ccr.physics.collision.RayCollisionTest

open class ProjectileClass : MoveablePhysClass() {
    enum class OrientationMode { VELOCITY, TUMBLE, FIXED }

    var lifetime: Float = 5.0f
    var elapsed: Float = 0f
    var bounceCount: Int = 0
    var maxBounces: Int = 0
    var gravity: Float = 1.0f
    var tumbleRate: Float = 0f
    var tumbleAxis: Vector3 = Vector3(0f, 0f, 1f)
    var orientationMode: OrientationMode = OrientationMode.VELOCITY
    private var expired = false

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
            OrientationMode.VELOCITY -> {
                // Keep transform oriented along velocity direction
                transform = transform.withTranslation(position)
            }
            OrientationMode.TUMBLE -> {
                transform = transform.withTranslation(position)
            }
            OrientationMode.FIXED -> {}
        }

        elapsed += dt
        if (elapsed >= lifetime) {
            expired = true
            observer?.objectExpired()
        }
    }
}
