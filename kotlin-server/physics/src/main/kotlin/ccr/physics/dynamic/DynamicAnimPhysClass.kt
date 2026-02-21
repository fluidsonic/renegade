package ccr.physics.dynamic

open class DynamicAnimPhysClass : DecorationPhysClass() {
    var animFrame: Float = 0f
    var animSpeed: Float = 1f
    var animLooping: Boolean = true
    var animLength: Float = 1f

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
