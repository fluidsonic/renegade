package ccr.physics.static

import ccr.math.OBBox
import ccr.math.Vector3
import ccr.math.transformOBBox
import ccr.physics.collision.CollisionMath

/**
 * C++: DoorPhysClass : AccessiblePhysClass (Combat/doors.h/.cpp)
 *
 * Mirrors the C++ Update_State state machine exactly, with animation-frame checks
 * replaced by a time-based approximation (server doesn't run real animation frames).
 */
class DoorPhysClass(
    val definitionId: Int,
    val closeDelay: Float,
    val triggerZone1: OBBox?,
    val triggerZone2: OBBox?,
    defLockCode: Int,
    val doorOpensForVehicles: Boolean,
) : AccessiblePhysClass() {

    init {
        lockCode = defLockCode
    }

    // C++: int State (initialized to STATE_CLOSED_DOOR)
    var state: Int = STATE_CLOSED_DOOR
        private set

    // C++: float Timer — used in STATE_OPENED_DOOR to count down before closing
    var timer: Float = 0f

    // C++: float CheckTimer — used in STATE_CLOSED_DOOR / STATE_ACCESS_DENIED to rate-limit trigger zone checks
    var checkTimer: Float = 0f

    // C++: bool OpenRequestPending — set by Request_Door_Open(), cleared each Timestep
    var openRequestPending: Boolean = false

    // C++: bool LockState — set by Lock_Door_Open(), checked in Update_State
    var lockState: Boolean = false

    override fun asDoorPhysClass(): DoorPhysClass? = this

    /** C++: bool Is_Door_Open() — returns true if door is in OPENED state. */
    fun isDoorOpen(): Boolean = state == STATE_OPENED_DOOR

    /** C++: void Request_Door_Open() */
    fun requestDoorOpen() {
        openRequestPending = true
    }

    /** C++: void Lock_Door_Open(bool onoff) */
    fun lockDoorOpen(onoff: Boolean) {
        if (onoff) {
            setState(STATE_OPENING_DOOR)
        }
        lockState = onoff
    }

    companion object {
        // C++: door state enum (doors.h)
        const val STATE_CLOSED_DOOR   = 0
        const val STATE_OPENED_DOOR   = 1
        const val STATE_OPENING_DOOR  = 2
        const val STATE_CLOSING_DOOR  = 3
        const val STATE_ACCESS_DENIED = 4
        const val STATE_MAX           = 5

        // Server-side approximation for animation open/close duration (no real anim frames on server).
        // C++: determined by the W3D animation frame count and playback speed; 1.5s is typical.
        private const val ANIM_OPEN_CLOSE_TIME = 1.5f

        // C++: CheckTimer resets to 0.3f — check every 1/3 second
        private const val CHECK_INTERVAL = 0.3f

        // Result codes from Can_Open_Door / Check_Door_Trigger (doors.cpp local enum)
        private const val DOOR_OPEN_OK     = 0
        private const val DOOR_OPEN_LOCKED = 1
        private const val DOOR_OPEN_NOONE  = 2

        // Chunk IDs from doors.cpp — DoorPhysClass Save/Load
        const val CHUNKID_OLD_PARENT: Int = 320002202
        const val CHUNKID_VARIABLES: Int = 320002203
        const val CHUNKID_PARENT: Int = 320002204

        const val MICROCHUNKID_STATE: Int = 2
        const val MICROCHUNKID_TIMER: Int = 3
        const val MICROCHUNKID_CHECK_TIMER: Int = 4
        const val MICROCHUNKID_OPEN_REQUEST_PENDING: Int = 5
    }

    // C++: bool DoorPhysClass::Set_State(int new_state) — sets state, returns true if changed
    private fun setState(newState: Int): Boolean {
        if (state == newState) return false
        when (newState) {
            STATE_OPENING_DOOR -> {
                // C++: AnimManager.Set_Target_Frame(lastFrame) → door starts opening
                timer = ANIM_OPEN_CLOSE_TIME
            }
            STATE_CLOSING_DOOR -> {
                // C++: AnimManager.Set_Target_Frame(0) → door starts closing
                timer = ANIM_OPEN_CLOSE_TIME
            }
            STATE_OPENED_DOOR -> {
                // C++: Timer = Get_DoorPhysDef()->CloseDelay; anim stays at last frame
                timer = closeDelay
            }
            STATE_CLOSED_DOOR -> {
                // C++: anim stays at frame 0
            }
            STATE_ACCESS_DENIED -> {
                // C++: plays access-denied sound (no-op on server)
            }
        }
        state = newState
        return true
    }

    /**
     * Advance the door state machine.
     * Mirrors C++ DoorPhysClass::Update_State(float dt) exactly.
     *
     * @param dt delta time in seconds
     * @param soldierPositions world-space positions of all human-controlled soldiers
     */
    fun updateState(dt: Float, soldierPositions: List<Vector3>) {
        when (state) {
            STATE_OPENING_DOOR -> {
                // C++: if (AnimManager.Get_Current_Frame() == lastFrame) Set_State(STATE_OPENED_DOOR)
                timer -= dt
                if (timer <= 0f) {
                    setState(STATE_OPENED_DOOR)
                }
            }
            STATE_OPENED_DOOR -> {
                // C++: Timer -= dt; if (Timer < 0 && !LockState && !OpenRequestPending && Can_Open_Door() != OK)
                //        Set_State(STATE_CLOSING_DOOR)
                timer -= dt
                if (timer < 0f
                    && !lockState
                    && !openRequestPending
                    && canOpenDoor(soldierPositions) != DOOR_OPEN_OK
                ) {
                    setState(STATE_CLOSING_DOOR)
                }
            }
            STATE_CLOSING_DOOR -> {
                // C++: if (AnimManager.Get_Current_Frame() == 0) Set_State(STATE_CLOSED_DOOR)
                timer -= dt
                if (timer <= 0f) {
                    setState(STATE_CLOSED_DOOR)
                }
                // Note: fall-through to CLOSED_DOOR case intentional in C++ (switch fall-through)
                checkForOpen(dt, soldierPositions)
            }
            STATE_CLOSED_DOOR, STATE_ACCESS_DENIED -> {
                checkForOpen(dt, soldierPositions)
            }
        }

        // C++: OpenRequestPending = false (reset each Timestep)
        openRequestPending = false
    }

    // C++: STATE_CLOSED_DOOR and STATE_ACCESS_DENIED share a fall-through check block.
    // Also STATE_CLOSING_DOOR falls through to this block in C++ (switch without break).
    private fun checkForOpen(dt: Float, soldierPositions: List<Vector3>) {
        // C++: CheckTimer -= dt; if (CheckTimer <= 0) { CheckTimer = 0.3f; ... }
        checkTimer -= dt
        if (checkTimer <= 0f) {
            checkTimer = CHECK_INTERVAL

            // C++: if (LockState == false) { ... }
            if (!lockState) {
                var updateClients = false
                val result = canOpenDoor(soldierPositions)
                if (openRequestPending || result == DOOR_OPEN_OK) {
                    updateClients = setState(STATE_OPENING_DOOR)
                } else if (result == DOOR_OPEN_LOCKED) {
                    updateClients = setState(STATE_ACCESS_DENIED)
                } else if (state == STATE_ACCESS_DENIED) {
                    updateClients = setState(STATE_CLOSED_DOOR)
                }

                // C++: if (update_clients) Enable_Is_State_Dirty(true)
                if (updateClients) {
                    flags = flags or IS_STATE_DIRTY
                }
            }
        }
    }

    // C++: int DoorPhysClass::Can_Open_Door()
    private fun canOpenDoor(soldierPositions: List<Vector3>): Int {
        val result1 = checkDoorTrigger(triggerZone1, soldierPositions)
        val result2 = if (result1 != DOOR_OPEN_OK) checkDoorTrigger(triggerZone2, soldierPositions) else DOOR_OPEN_NOONE

        return when {
            result1 == DOOR_OPEN_OK || result2 == DOOR_OPEN_OK -> DOOR_OPEN_OK
            result1 == DOOR_OPEN_LOCKED || result2 == DOOR_OPEN_LOCKED -> DOOR_OPEN_LOCKED
            else -> DOOR_OPEN_NOONE
        }
    }

    // C++: int DoorPhysClass::Check_Door_Trigger(const OBBoxClass& trigger_zone)
    private fun checkDoorTrigger(zone: OBBox?, soldierPositions: List<Vector3>): Int {
        if (zone == null) return DOOR_OPEN_NOONE
        val worldZone = transformOBBox(zone, transform)
        for (pos in soldierPositions) {
            if (CollisionMath.pointInOBBox(pos, worldZone)) {
                return if (lockCode == 0) DOOR_OPEN_OK else DOOR_OPEN_LOCKED
            }
        }
        return DOOR_OPEN_NOONE
    }
}
