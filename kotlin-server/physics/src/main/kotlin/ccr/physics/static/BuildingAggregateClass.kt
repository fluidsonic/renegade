package ccr.physics.static

/**
 * C++: BuildingAggregateClass (BAG) : StaticAnimPhysClass (Combat/buildingaggregate.h/.cpp)
 *
 * Animated static objects which display segments of their animation depending on
 * the building's health/power state. Used by building logic in Renegade.
 */
open class BuildingAggregateClass : StaticAnimPhysClass() {

    override val factoryChunkId: UInt = 0x00020A03u // PHYSICS_CHUNKID_BUILDINGAGGREGATE from wwphysids.h

    // C++: int CurrentState (initialized to BuildingStateClass::HEALTH100_POWERON = 0)
    var currentState: Int = HEALTH100_POWERON

    /** C++: bool Is_MCT() — returns true if this aggregate is a Master Control Terminal. */
    // Determined by definition; server sets this from the def during construction.
    var isMCT: Boolean = false

    companion object {
        // BuildingStateClass states (buildingstate.h)
        const val HEALTH100_POWERON: Int = 0
        const val HEALTH75_POWERON: Int = 1
        const val HEALTH50_POWERON: Int = 2
        const val HEALTH25_POWERON: Int = 3
        const val DESTROYED_POWERON: Int = 4
        const val HEALTH100_POWEROFF: Int = 5
        const val HEALTH75_POWEROFF: Int = 6
        const val HEALTH50_POWEROFF: Int = 7
        const val HEALTH25_POWEROFF: Int = 8
        const val DESTROYED_POWEROFF: Int = 9
        const val STATE_COUNT: Int = 10

        // Chunk IDs from buildingaggregate.cpp — BuildingAggregateClass Save/Load
        const val BAG_CHUNK_STATICANIMPHYS: Int = 8281529
        const val BAG_CHUNK_VARIABLES: Int = 8281530

        const val BAG_VARIABLE_CURRENTSTATE: Int = 0
    }
}
