package ccr.server

/**
 * Immutable map rotation tracker.
 *
 * Single-map servers: maps.size == 1 → nextName() always returns null → same-map restart.
 * Multi-map looping:  nextName() wraps to maps[0] after last map; isMapCycleOver always false.
 * Multi-map no loop:  nextName() returns null at last map; isMapCycleOver = true at last map.
 */
data class MapRotation(
    val maps: List<String>,
    val loops: Boolean,
    val currentIndex: Int = 0,
) {
    /** Name of the currently active map. */
    val currentName: String get() = maps.getOrElse(currentIndex) { maps.firstOrNull() ?: "" }

    /**
     * Name of the next map after the current one, or null if the rotation should stay put.
     * Null means same-map restart (single-map server, or end of non-looping cycle).
     */
    fun nextName(): String? {
        if (maps.size <= 1) return null          // single-map: never rotate
        val nextIndex = currentIndex + 1
        return when {
            nextIndex < maps.size -> maps[nextIndex]   // next map in list
            loops -> maps[0]                           // wrap around
            else -> null                               // end of non-looping cycle
        }
    }

    /**
     * True only when there are multiple maps, looping is disabled, and we're on the last map.
     * When true, the WinEvent.isMapCycleOver flag should be set — clients return to lobby.
     */
    val isMapCycleOver: Boolean get() =
        maps.size > 1 && !loops && currentIndex >= maps.size - 1

    /**
     * Returns a new MapRotation advanced to the next position.
     * For a single-map server this is a no-op (returns self).
     * At end of non-looping list, wraps to index 0 (ready for next cycle when new clients arrive).
     */
    fun advance(): MapRotation {
        if (maps.size <= 1) return this
        val nextIndex = (currentIndex + 1) % maps.size
        return copy(currentIndex = nextIndex)
    }
}
