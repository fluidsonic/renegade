package ccr.physics.static

import ccr.math.OBBox
import ccr.math.Vector3
import ccr.math.transformOBBox
import ccr.physics.collision.CollisionMath

class DoorPhysClass(
    val definitionId: Int,
    val closeDelay: Float,
    val triggerZone1: OBBox?,
    val triggerZone2: OBBox?,
    val lockCode: Int,
    val doorOpensForVehicles: Boolean,
) : AccessiblePhysClass() {

    var state: Int = STATE_CLOSED_DOOR
        private set
    var timer: Float = 0f
    var checkTimer: Float = 0f

    companion object {
        const val STATE_CLOSED_DOOR   = 0
        const val STATE_OPENED_DOOR   = 1
        const val STATE_OPENING_DOOR  = 2
        const val STATE_CLOSING_DOOR  = 3
        const val STATE_ACCESS_DENIED = 4

        private const val ANIM_OPEN_CLOSE_TIME = 1.5f  // seconds to open/close
        private const val CHECK_INTERVAL       = 0.3f  // proximity check interval
    }

    override fun open() {
        if (state == STATE_CLOSED_DOOR || state == STATE_CLOSING_DOOR || state == STATE_ACCESS_DENIED) {
            state = STATE_OPENING_DOOR
            timer = ANIM_OPEN_CLOSE_TIME
        }
    }

    override fun close() {
        if (state == STATE_OPENED_DOOR || state == STATE_OPENING_DOOR) {
            state = STATE_CLOSING_DOOR
            timer = ANIM_OPEN_CLOSE_TIME
        }
    }

    /**
     * Advance the door state machine.
     * @param dt delta time in seconds
     * @param soldierPositions world-space positions of all soldiers
     */
    fun updateState(dt: Float, soldierPositions: List<Vector3>) {
        timer -= dt
        checkTimer -= dt

        when (state) {
            STATE_OPENING_DOOR -> {
                if (timer <= 0f) {
                    state = STATE_OPENED_DOOR
                    timer = closeDelay
                    isOpen = true
                }
            }
            STATE_OPENED_DOOR -> {
                if (timer <= 0f) {
                    if (checkTimer <= 0f) {
                        checkTimer = CHECK_INTERVAL
                        if (!anyPlayerInTriggerZone(soldierPositions)) {
                            close()
                        } else {
                            timer = closeDelay  // reset delay, player still nearby
                        }
                    }
                }
            }
            STATE_CLOSING_DOOR -> {
                if (timer <= 0f) {
                    state = STATE_CLOSED_DOOR
                    isOpen = false
                }
            }
            STATE_CLOSED_DOOR, STATE_ACCESS_DENIED -> {
                if (checkTimer <= 0f) {
                    checkTimer = CHECK_INTERVAL
                    val playerNearby = anyPlayerInTriggerZone(soldierPositions)
                    if (playerNearby) {
                        if (lockCode != 0) {
                            state = STATE_ACCESS_DENIED
                        } else {
                            open()
                        }
                    } else if (state == STATE_ACCESS_DENIED) {
                        state = STATE_CLOSED_DOOR
                    }
                }
            }
        }
    }

    private fun anyPlayerInTriggerZone(positions: List<Vector3>): Boolean {
        val z1 = triggerZone1?.let { transformOBBox(it, transform) }
        val z2 = triggerZone2?.let { transformOBBox(it, transform) }
        return positions.any { pos ->
            (z1 != null && CollisionMath.pointInOBBox(pos, z1)) ||
            (z2 != null && CollisionMath.pointInOBBox(pos, z2))
        }
    }
}
