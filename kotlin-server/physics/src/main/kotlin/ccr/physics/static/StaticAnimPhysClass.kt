package ccr.physics.static

abstract class StaticAnimPhysClass : StaticPhysClass() {
    var animFrame: Float = 0f
    var animSpeed: Float = 1f
    var animLooping: Boolean = true
    var animLength: Float = 1f // total animation length in seconds

    override fun needsTimestep(): Boolean = true

    override fun timestep(dt: Float) {
        super.timestep(dt)
        animFrame += dt * animSpeed
        if (animLooping && animLength > 0f) {
            while (animFrame >= animLength) animFrame -= animLength
            while (animFrame < 0f) animFrame += animLength
        }
    }
}
