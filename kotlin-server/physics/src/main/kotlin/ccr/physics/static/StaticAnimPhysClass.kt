package ccr.physics.static

open class StaticAnimPhysClass : StaticPhysClass() {

    companion object {
        // Chunk IDs from staticanimphys.cpp
        const val STATICANIMPHYS_CHUNK_STATICPHYS: Int = 0x01170010   // parent StaticPhysClass chunk
        const val STATICANIMPHYS_CHUNK_VARIABLES: Int = 0x01170011    // own variables (legacy collision mode)
        const val STATICANIMPHYS_CHUNK_ANIMMANAGER: Int = 0x01170012  // AnimCollisionManagerClass state

        // Micro-chunk IDs within STATICANIMPHYS_CHUNK_VARIABLES:
        const val STATICANIMPHYS_VARIABLE_DEFID_OBSOLETE: Int = 0x00  // obsolete, ignored on load
        const val STATICANIMPHYS_VARIABLE_COLLISIONMODE: Int = 0x01   // legacy collision mode (int32)
    }

    override fun asStaticAnimPhysClass(): StaticAnimPhysClass? = this

    // C++ Needs_Timestep() returns true for StaticAnimPhysClass.
    // Server-side we don't run animation/projector logic, so timestep is a no-op.
    override fun needsTimestep(): Boolean = true
    override fun timestep(dt: Float) {}
}
