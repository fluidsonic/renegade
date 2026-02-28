package ccr.physics.static

/**
 * C++: DamageableStaticPhysClass : StaticAnimPhysClass (Combat/damageablestaticphys.h/.cpp)
 *
 * A static physics object which tracks its health. When health drops to zero,
 * it plays a death transition animation and enters a dead loop.
 */
open class DamageableStaticPhysClass : StaticAnimPhysClass() {

    // C++: int CurState (initialized to STATE_LIVE_LOOP)
    var curState: Int = STATE_LIVE_LOOP

    override fun asDamageableStaticPhysClass(): DamageableStaticPhysClass? = this

    companion object {
        // C++: state enum (damageablestaticphys.h, inside DamageableStaticPhysClass)
        const val STATE_LIVE_LOOP: Int = 0
        const val STATE_LIVE_TWITCH: Int = 1
        const val STATE_DEATH_TRANSITION: Int = 2
        const val STATE_DEAD_LOOP: Int = 3
        const val STATE_DEAD_TWITCH: Int = 4

        // Chunk IDs from damageablestaticphys.cpp — DamageableStaticPhysClass Save/Load
        const val DAMAGEABLESTATICPHYS_CHUNK_STATICANIMPHYS: Int = 7311740
        const val DAMAGEABLESTATICPHYS_CHUNK_DEFENSEOBJECT: Int = 7311741
        const val DAMAGEABLESTATICPHYS_CHUNK_VARIABLES: Int = 7311742

        const val DAMAGEABLESTATICPHYS_VARIABLE_CURSTATE: Int = 0x00
    }
}
