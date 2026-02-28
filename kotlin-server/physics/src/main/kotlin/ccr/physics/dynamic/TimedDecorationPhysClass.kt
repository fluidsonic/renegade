package ccr.physics.dynamic

open class TimedDecorationPhysClass(lifetime: Float = 2.0f) : DecorationPhysClass() {

    // Matches C++ Lifetime — countdown timer, decremented each timestep (timeddecophys.h)
    var lifetime: Float = lifetime

    override fun needsTimestep(): Boolean = true

    override fun timestep(dt: Float) {
        super.timestep(dt)
        // Decrement lifetime, matching C++ TimedDecorationPhysClass::Timestep
        lifetime -= dt
        if (lifetime < 0.0f) {
            observer?.objectExpired()
        }
    }

    override fun asTimedDecorationPhysClass(): TimedDecorationPhysClass? = this

    companion object {
        // Chunk IDs sourced from timeddecophys.cpp Save()
        const val TIMEDDECOPHYS_CHUNK_DECOPHYS: Int = 0x05160000   // wraps parent DecorationPhysClass
        const val TIMEDDECOPHYS_CHUNK_VARIABLES: Int = 0x05160001  // own data
        // Micro-chunk IDs within VARIABLES:
        const val TIMEDDECOPHYS_VARIABLE_LIFETIME: Int = 0x00
    }
}
