package ccr.physics.dynamic

open class TimedDecorationPhysClass(lifetime: Float) : DecorationPhysClass() {
    var lifetime: Float = lifetime
    var elapsed: Float = 0f
    var onExpired: (() -> Unit)? = null
    private var expired = false

    override fun needsTimestep(): Boolean = true

    override fun timestep(dt: Float) {
        super.timestep(dt)
        if (expired) return
        elapsed += dt
        if (elapsed >= lifetime) {
            expired = true
            observer?.objectExpired()
            onExpired?.invoke()
        }
    }
}
