package ccr.physics.dynamic

open class DecorationPhysClass : DynamicPhysClass() {
    override fun timestep(dt: Float) {} // inert, no-op — matches C++ empty Timestep

    override fun asDecorationPhysClass(): DecorationPhysClass? = this

    companion object {
        // Chunk IDs sourced from decophys.cpp Save()
        const val DECOPHYS_CHUNK_PHYS: Int = 0x05060000        // old parent class data (legacy)
        const val DECOPHYS_CHUNK_DYNAMICPHYS: Int = 0x05060001 // current parent class data
    }
}
