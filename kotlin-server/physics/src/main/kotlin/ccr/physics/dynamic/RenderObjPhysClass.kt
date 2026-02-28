package ccr.physics.dynamic

open class RenderObjPhysClass : DynamicPhysClass() {
    override fun timestep(dt: Float) {} // no-op, matching C++

    override fun asRenderObjPhysClass(): RenderObjPhysClass? = this

    companion object {
        // Chunk IDs sourced from renderobjphys.cpp Save()
        const val RENDEROBJPHYS_CHUNK_PHYS: Int = 0x00099900        // obsolete, old parent class
        const val RENDEROBJPHYS_CHUNK_DYNAMICPHYS: Int = 0x00099901 // current parent class data
    }
}
