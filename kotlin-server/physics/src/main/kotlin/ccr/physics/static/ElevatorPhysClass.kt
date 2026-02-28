package ccr.physics.static

/**
 * C++: ElevatorPhysClass : AccessiblePhysClass (Combat/elevator.h/.cpp)
 *
 * Two-floor elevator with call zones, door states, and a movement state machine.
 * Extends AccessiblePhysClass (inherits LockCode for locked elevator doors).
 */
open class ElevatorPhysClass : AccessiblePhysClass() {

    // C++: int State (initialized to STATE_UP in constructor)
    var state: Int = STATE_UP

    // C++: int DoorStates[2] — TOP=0, BOTTOM=1
    var topDoorState: Int = DOOR_STATE_NORMAL
    var bottomDoorState: Int = DOOR_STATE_NORMAL

    // C++: float CheckTimer — rate-limits state evaluation (resets to 0.5f)
    var checkTimer: Float = 0f

    // C++: float PrevFrame — previous animation frame for sound effect triggers
    var prevFrame: Float = 0f

    // C++: bool IsCallTimerSet — whether the call timer is active
    var isCallTimerSet: Boolean = false

    // C++: float CallTimer — delay before responding to a call
    var callTimer: Float = 0f

    // C++: int TriggerRequest — bitmask of AI-requested trigger zones
    var triggerRequest: Int = 0

    override fun asElevatorPhysClass(): ElevatorPhysClass? = this

    /** C++: bool Is_Moving() */
    fun isMoving(): Boolean = state == STATE_MOVING_UP || state == STATE_MOVING_DOWN

    /** C++: int Get_Floor() — 0=bottom, 1=top, 13=moving */
    fun getFloor(): Int = when (state) {
        STATE_DOWN -> 0
        STATE_UP -> 1
        else -> 13
    }

    // C++: elevator state enum (elevator.h, inside ElevatorPhysClass)
    companion object {
        const val STATE_DOWN: Int = 0
        const val STATE_MOVING_UP: Int = 1
        const val STATE_UP: Int = 2
        const val STATE_MOVING_DOWN: Int = 3
        const val STATE_MAX: Int = 4

        const val DOOR_STATE_NORMAL: Int = 0
        const val DOOR_STATE_UNLOCKED: Int = 1
        const val DOOR_STATE_ACCESS_DENIED: Int = 2
        const val DOOR_STATE_MAX: Int = 3

        // C++: door position enum (TOP=0, BOTTOM=1)
        const val TOP: Int = 0
        const val BOTTOM: Int = 1

        // Chunk IDs from elevator.cpp — ElevatorPhysClass Save/Load
        const val CHUNKID_PARENT: Int = 714001438
        const val CHUNKID_VARIABLES: Int = 714001439
        const val CHUNKID_RIDER_OBJECT: Int = 714001440

        const val MICROCHUNKID_STATE: Int = 2
        const val MICROCHUNKID_TOP_DOOR_STATE: Int = 3
        const val MICROCHUNKID_BOTTOM_DOOR_STATE: Int = 4
    }
}
