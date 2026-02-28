package ccr.physics.dynamic

open class DynamicAnimPhysClass : DecorationPhysClass() {

    // C++ has AnimCollisionManagerClass AnimManager and DynamicShadowManagerClass ShadowManager
    // These are complex sub-objects for animated collision — skipped for server-side

    override fun needsTimestep(): Boolean = true

    override fun timestep(dt: Float) {
        // C++ delegates to AnimManager.Timestep(dt) for animation collision management
        // Server-side: no animation collision management needed
    }

    override fun asDynamicAnimPhysClass(): DynamicAnimPhysClass? = this

    companion object {
        // Chunk IDs sourced from dynamicanimphys.cpp Save()
        const val DYNAMICANIMPHYS_CHUNK_DECOPHYS: Int = 0x1F5A20D3     // 526000339 decimal, wraps parent
        const val DYNAMICANIMPHYS_CHUNK_VARIABLES: Int = 0x1F5A20D4    // own data (currently unused)
        const val DYNAMICANIMPHYS_CHUNK_ANIMMANAGER: Int = 0x1F5A20D5  // animation manager data
    }
}
