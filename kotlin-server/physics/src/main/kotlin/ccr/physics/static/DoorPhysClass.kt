package ccr.physics.static

import ccr.math.OBBox
import ccr.math.Vector3
import ccr.math.transformOBBox
import ccr.physics.collision.CollisionMath

// C++: DoorPhysClass : AccessiblePhysClass (Combat/doors.cpp / doors.h)
// Mirrors the C++ Update_State state machine exactly, with animation-frame checks
// replaced by a time-based approximation (server doesn't run real animation frames).
class DoorPhysClass(
    val definitionId: Int,
    val closeDelay: Float,
    val triggerZone1: OBBox?,
    val triggerZone2: OBBox?,
    val lockCode: Int,
    val doorOpensForVehicles: Boolean,
) : AccessiblePhysClass() {

    // C++: int State (initialized to STATE_CLOSED_DOOR)
    var state: Int = STATE_CLOSED_DOOR
        private set

    // C++: float Timer — used in STATE_OPENED_DOOR to count down before closing
    var timer: Float = 0f

    // C++: float CheckTimer — used in STATE_CLOSED_DOOR / STATE_ACCESS_DENIED to rate-limit trigger zone checks
    var checkTimer: Float = 0f

    companion object {
        // C++: door state enum (doors.h)
        const val STATE_CLOSED_DOOR   = 0
        const val STATE_OPENED_DOOR   = 1
        const val STATE_OPENING_DOOR  = 2
        const val STATE_CLOSING_DOOR  = 3
        const val STATE_ACCESS_DENIED = 4

        // Server-side approximation for animation open/close duration (no real anim frames on server).
        // C++: determined by the W3D animation frame count and playback speed; 1.5s is typical.
        private const val ANIM_OPEN_CLOSE_TIME = 1.5f

        // C++: CheckTimer resets to 0.3f — check every 1/3 second
        private const val CHECK_INTERVAL = 0.3f

        // Result codes from Can_Open_Door / Check_Door_Trigger (doors.cpp local enum)
        private const val DOOR_OPEN_OK     = 0
        private const val DOOR_OPEN_LOCKED = 1
        private const val DOOR_OPEN_NOONE  = 2
    }

    // C++: DoorPhysClass::open() — exposed for Lock_Door_Open
    override fun open() {
        setState(STATE_OPENING_DOOR)
    }

    // C++: DoorPhysClass::close() — exposed for external callers
    override fun close() {
        setState(STATE_CLOSING_DOOR)
    }

    // C++: bool DoorPhysClass::Set_State(int new_state) — sets state, returns true if changed
    // On STATE_OPENING_DOOR: set anim target to last frame (approximated by starting open timer)
    // On STATE_CLOSING_DOOR: set anim target to frame 0 (approximated by starting close timer)
    // On STATE_OPENED_DOOR: set Timer = CloseDelay, set anim target to last frame
    // On STATE_CLOSED_DOOR: set anim target to frame 0
    private fun setState(newState: Int): Boolean {
        if (state == newState) return false
        when (newState) {
            STATE_OPENING_DOOR -> {
                // C++: AnimManager.Set_Target_Frame(lastFrame) → door starts opening
                // Server approximation: use a timer to mark when "animation" completes
                timer = ANIM_OPEN_CLOSE_TIME
                isOpen = false
            }
            STATE_CLOSING_DOOR -> {
                // C++: AnimManager.Set_Target_Frame(0) → door starts closing
                timer = ANIM_OPEN_CLOSE_TIME
                isOpen = false
            }
            STATE_OPENED_DOOR -> {
                // C++: Timer = Get_DoorPhysDef()->CloseDelay; anim stays at last frame
                timer = closeDelay
                isOpen = true
            }
            STATE_CLOSED_DOOR -> {
                // C++: anim stays at frame 0
                isOpen = false
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
                // Server approximation: transition when timer expires
                timer -= dt
                if (timer <= 0f) {
                    setState(STATE_OPENED_DOOR)
                }
            }
            STATE_OPENED_DOOR -> {
                // C++: Timer -= dt; if (Timer < 0 && !lockState && !openRequestPending && Can_Open_Door() != OK)
                //        Set_State(STATE_CLOSING_DOOR)
                timer -= dt
                if (timer < 0f && canOpenDoor(soldierPositions) != DOOR_OPEN_OK) {
                    setState(STATE_CLOSING_DOOR)
                }
            }
            STATE_CLOSING_DOOR -> {
                // C++: if (AnimManager.Get_Current_Frame() == 0) Set_State(STATE_CLOSED_DOOR)
                // Server approximation: transition when timer expires
                timer -= dt
                if (timer <= 0f) {
                    setState(STATE_CLOSED_DOOR)
                }
                // Note: fall-through to CLOSED_DOOR case intentional in C++ (switch fall-through) — handled below
                checkForOpen(dt, soldierPositions)
            }
            STATE_CLOSED_DOOR, STATE_ACCESS_DENIED -> {
                checkForOpen(dt, soldierPositions)
            }
        }
    }

    // C++: STATE_CLOSED_DOOR and STATE_ACCESS_DENIED share a fall-through check block.
    // Also STATE_CLOSING_DOOR falls through to this block in C++ (switch without break).
    private fun checkForOpen(dt: Float, soldierPositions: List<Vector3>) {
        // C++: CheckTimer -= dt; if (CheckTimer <= 0) { CheckTimer = 0.3f; ... }
        checkTimer -= dt
        if (checkTimer <= 0f) {
            checkTimer = CHECK_INTERVAL

            // C++: if (LockState == false) — LockCode as proxy; never externally locked here
            val result = canOpenDoor(soldierPositions)
            if (result == DOOR_OPEN_OK) {
                setState(STATE_OPENING_DOOR)
            } else if (result == DOOR_OPEN_LOCKED) {
                setState(STATE_ACCESS_DENIED)
            } else if (state == STATE_ACCESS_DENIED) {
                setState(STATE_CLOSED_DOOR)
            }
        }
    }

    // C++: int DoorPhysClass::Can_Open_Door()
    // Transforms both trigger zones into world space, checks each.
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
    // Checks if any human-controlled soldier is inside the (world-space) trigger zone.
    // lockCode==0 means no lock (anyone can open); lockCode!=0 means door is locked.
    private fun checkDoorTrigger(zone: OBBox?, soldierPositions: List<Vector3>): Int {
        if (zone == null) return DOOR_OPEN_NOONE
        val worldZone = transformOBBox(zone, transform)
        for (pos in soldierPositions) {
            if (CollisionMath.pointInOBBox(pos, worldZone)) {
                // C++: lockCode==0 → DOOR_OPEN_OK; lockCode!=0 → DOOR_OPEN_LOCKED (no key check on server)
                return if (lockCode == 0) DOOR_OPEN_OK else DOOR_OPEN_LOCKED
            }
        }
        return DOOR_OPEN_NOONE
    }
}
