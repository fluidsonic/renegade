package ccr.server.level.pathfind

import ccr.server.level.Vector3

data class Waypoint(
    val id: Int = 0,
    val position: Vector3 = Vector3.ZERO,
    val flags: Int = 0,
    val actionPortalId: Int = -1,
)

data class Waypath(
    val id: Int = 0,
    val flags: Int = 0,
    val waypoints: List<Waypoint> = emptyList(),
) {
    companion object {
        const val FLAG_TWO_WAY = 0x01
        const val FLAG_LOOPING = 0x02
        const val FLAG_HUMAN = 0x04
        const val FLAG_GROUND_VEHICLE = 0x08
        const val FLAG_FLYING_VEHICLE = 0x10
        const val FLAG_INNATE_PATHFIND = 0x20
    }
}

data class PathPortal(
    val id: Int = 0,
    val destSector1: Int = 0,
    val destSector2: Int = 0,
    val center: Vector3 = Vector3.ZERO,
    val extent: Vector3 = Vector3.ZERO,
)

data class PathSector(
    val center: Vector3 = Vector3.ZERO,
    val extent: Vector3 = Vector3.ZERO,
    val portalIds: List<Int> = emptyList(),
)

data class PathfindData(
    val sectors: List<PathSector> = emptyList(),
    val portals: List<PathPortal> = emptyList(),
    val waypaths: List<Waypath> = emptyList(),
    val sectorCount: Int = sectors.size,
    val portalCount: Int = portals.size,
    val waypathCount: Int = waypaths.size,
)
