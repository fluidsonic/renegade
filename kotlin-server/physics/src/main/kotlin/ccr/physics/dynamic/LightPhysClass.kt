package ccr.physics.dynamic

open class LightPhysClass : DecorationPhysClass() {

    // Matches C++ LightPhysClass fields (lightphys.h)
    var visSectorId: Int = -1  // C++ init: 0xFFFFFFFF (as signed int = -1)
    var groupId: Int = 0

    override fun asLightPhysClass(): LightPhysClass? = this

    companion object {
        // Chunk IDs sourced from lightphys.cpp Save()
        const val LIGHTPHYS_CHUNK_DECOPHYS: Int = 0x00770010   // wraps parent DecorationPhysClass
        const val LIGHTPHYS_CHUNK_VARIABLES: Int = 0x00770011  // own data
        // Micro-chunk IDs within VARIABLES:
        const val LIGHTPHYS_VARIABLE_VISSECTORID: Int = 0x00
        const val LIGHTPHYS_VARIABLE_GROUPID: Int = 0x01
    }
}
