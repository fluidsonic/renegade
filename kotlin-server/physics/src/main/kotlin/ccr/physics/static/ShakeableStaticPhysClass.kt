package ccr.physics.static

/**
 * C++: ShakeableStaticPhysClass (wwphys/shakeablestaticphys.h/.cpp)
 *
 * A static animated physics object which plays its animation as a "shaking" effect
 * when it is collided with. No additional fields beyond StaticAnimPhysClass.
 */
open class ShakeableStaticPhysClass : StaticAnimPhysClass() {

    companion object {
        // Chunk IDs from shakeablestaticphys.cpp
        const val SHAKEABLESTATICPHYS_CHUNK_STATICANIMPHYS: Int = 7311740
    }
}
