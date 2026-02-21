package ccr.server.level.pathfind

import ccr.server.level.ChunkIds
import ccr.server.level.Vector3
import ccr.server.level.toInt32
import ccr.server.level.toVector3
import ccr.server.mix.ChunkReader

/**
 * Loads pathfinding data from the PSDSSC_CHUNKID_PATHFIND chunk in an LSD file.
 *
 * Binary layout (Pathfind.cpp):
 * ```
 * PATHFIND_DATABASE (0x01060635)
 *   ├─ SECTOR_CULLING_SYSTEM (0x01060638)
 *   │   ├─ SECTOR_CULL_TREE (binary, skipped)
 *   │   └─ SECTOR (0x01060639) …
 *   │       ├─ SECTOR_OBJECT (0x0106063C) or PATHFIND_SECTOR_OBJECT (0x01060640)
 *   │       │   └─ VARIABLES (0x01060643): micro-chunks BOUNDING_BOX, PORTAL_ID*
 *   │       └─ SECTOR_LINKAGE (skipped)
 *   ├─ PORTAL (0x01060637) …
 *   │   └─ VARIABLES (0x01060654): micro-chunks BOUNDING_BOX, DEST_SECTOR1/2, ID
 *   ├─ ACTION_PORTAL (0x0106063E) …
 *   │   ├─ PARENT (0x01060655) → base portal data
 *   │   └─ VARIABLES (action-specific, skipped)
 *   ├─ WAYPATH_PORTAL (0x0106063F) …
 *   │   ├─ PARENT → base portal data
 *   │   └─ VARIABLES (waypath-specific, skipped)
 *   ├─ WAYPATH (0x00020110) …
 *   │   └─ VARIABLES (0x04290219): micro-chunks FLAGS, ID
 *   ├─ WAYPOINT (0x00020111) …
 *   │   └─ VARIABLES (0x04290112): micro-chunks FLAGS, POSITION, ID, ACTION_ID
 *   └─ HEIGHTDB (0x0106063D, skipped)
 * ```
 */
object PathfindLoader {

    // Sector micro-chunk container (PathfindSector.cpp)
    private const val SECTOR_VARIABLES_ID = 0x01060643u
    // Portal micro-chunk container (PathfindPortal.cpp)
    private const val PORTAL_VARIABLES_ID = 0x01060654u
    // Portal parent chunk for sub-classes (PathfindPortal.cpp: CHUNKID_PARENT)
    private const val PORTAL_PARENT_ID = 0x01060655u
    // Waypath micro-chunk container (waypath.cpp)
    private const val WAYPATH_VARIABLES_ID = 0x04290219u
    // Waypoint micro-chunk container (waypoint.cpp)
    private const val WAYPOINT_VARIABLES_ID = 0x04290112u

    /**
     * Parse the pathfind chunk (PSDSSC_CHUNKID_PATHFIND).
     * The first child chunk should be PATHFIND_CHUNK_DATABASE.
     */
    fun load(pathfindChunk: ChunkReader): PathfindData {
        val databaseChunk = pathfindChunk.findChunk(ChunkIds.PATHFIND_CHUNK_DATABASE)
            ?: return PathfindData()

        val sectors = mutableListOf<PathSector>()
        val portals = mutableListOf<PathPortal>()
        val waypaths = mutableListOf<Waypath>()

        var currentWaypathId = -1
        var currentWaypathFlags = 0
        val currentWaypoints = mutableListOf<Waypoint>()

        databaseChunk.forEachChunk { chunkId, _, reader ->
            when (chunkId) {
                ChunkIds.PATHFIND_CHUNK_SECTOR_CULLING_SYSTEM ->
                    loadSectorsFromCullingSystem(reader, sectors)

                ChunkIds.PATHFIND_CHUNK_PORTAL,
                ChunkIds.PATHFIND_CHUNK_ACTION_PORTAL,
                ChunkIds.PATHFIND_CHUNK_WAYPATH_PORTAL ->
                    parsePortal(reader)?.let { portals.add(it) }

                ChunkIds.PHYSICS_CHUNKID_WAYPATH -> {
                    finishCurrentWaypath(currentWaypathId, currentWaypathFlags, currentWaypoints, waypaths)
                    currentWaypoints.clear()
                    val (id, flags) = parseWaypathHeader(reader)
                    currentWaypathId = id
                    currentWaypathFlags = flags
                }

                ChunkIds.PHYSICS_CHUNKID_WAYPOINT ->
                    parseWaypoint(reader)?.let { currentWaypoints.add(it) }
            }
        }

        finishCurrentWaypath(currentWaypathId, currentWaypathFlags, currentWaypoints, waypaths)

        return PathfindData(
            sectors = sectors,
            portals = portals,
            waypaths = waypaths,
        )
    }

    private fun finishCurrentWaypath(
        id: Int,
        flags: Int,
        waypoints: MutableList<Waypoint>,
        waypaths: MutableList<Waypath>,
    ) {
        if (id < 0) return
        waypaths.add(Waypath(id = id, flags = flags, waypoints = waypoints.toList()))
    }

    // ─── Sectors ────────────────────────────────────────────────────────────────

    private fun loadSectorsFromCullingSystem(
        cullingChunk: ChunkReader,
        sectors: MutableList<PathSector>,
    ) {
        cullingChunk.forEachChunk { chunkId, _, reader ->
            if (chunkId == ChunkIds.PATHFIND_CHUNK_SECTOR) {
                parseSector(reader)?.let { sectors.add(it) }
            }
        }
    }

    /**
     * Parse a SECTOR container chunk.
     * Contains SECTOR_OBJECT (or PATHFIND_SECTOR_OBJECT) + optional SECTOR_LINKAGE.
     * The actual data is in micro-chunks inside SECTOR_VARIABLES_ID (0x01060643).
     */
    private fun parseSector(sectorChunk: ChunkReader): PathSector? {
        val variablesChunk = sectorChunk.findChunkRecursive(SECTOR_VARIABLES_ID) ?: return null

        var center = Vector3.ZERO
        var extent = Vector3.ZERO
        val portalIds = mutableListOf<Int>()

        variablesChunk.forEachMicroChunk { id, data ->
            when (id) {
                1 -> { // VARID_BOUNDING_BOX — AABoxClass: center(3f) + extent(3f) = 24 bytes
                    if (data.size >= 24) {
                        center = data.toVector3(0)
                        extent = data.toVector3(12)
                    }
                }
                2 -> { // VARID_PORTAL_ID — uint32 (repeating)
                    if (data.size >= 4) {
                        portalIds.add(data.toInt32())
                    }
                }
            }
        }

        return PathSector(center = center, extent = extent, portalIds = portalIds)
    }

    // ─── Portals ────────────────────────────────────────────────────────────────

    /**
     * Parse a portal chunk (regular, action, or waypath).
     *
     * Regular portal: PORTAL_VARIABLES_ID is a direct child.
     * Action/waypath portal: base data is inside PORTAL_PARENT_ID → PORTAL_VARIABLES_ID.
     */
    private fun parsePortal(portalChunk: ChunkReader): PathPortal? {
        // For action/waypath portals, the base portal data is inside a PARENT chunk.
        // For regular portals, VARIABLES is a direct child.
        val parentChunk = portalChunk.findChunk(PORTAL_PARENT_ID)
        val variablesChunk = if (parentChunk != null) {
            parentChunk.findChunk(PORTAL_VARIABLES_ID)
        } else {
            portalChunk.findChunk(PORTAL_VARIABLES_ID)
        } ?: return null

        var id = 0
        var destSector1 = 0
        var destSector2 = 0
        var center = Vector3.ZERO
        var extent = Vector3.ZERO

        variablesChunk.forEachMicroChunk { microId, data ->
            when (microId) {
                1 -> { // VARID_BOUNDING_BOX — AABoxClass (24 bytes)
                    if (data.size >= 24) {
                        center = data.toVector3(0)
                        extent = data.toVector3(12)
                    }
                }
                3 -> { // VARID_DEST_SECTOR1 — uint16
                    if (data.size >= 2) {
                        destSector1 = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
                    }
                }
                4 -> { // VARID_DEST_SECTOR2 — uint16
                    if (data.size >= 2) {
                        destSector2 = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
                    }
                }
                5 -> { // VARID_ID — uint32
                    if (data.size >= 4) id = data.toInt32()
                }
                // 6 = VARID_OLD_PTR — skip (pointer remapping)
            }
        }

        return PathPortal(
            id = id,
            destSector1 = destSector1,
            destSector2 = destSector2,
            center = center,
            extent = extent,
        )
    }

    // ─── Waypaths ───────────────────────────────────────────────────────────────

    /** Returns (id, flags) from the waypath's VARIABLES chunk. */
    private fun parseWaypathHeader(waypathChunk: ChunkReader): Pair<Int, Int> {
        val variablesChunk = waypathChunk.findChunk(WAYPATH_VARIABLES_ID) ?: return Pair(0, 0)

        var id = 0
        var flags = 0

        variablesChunk.forEachMicroChunk { microId, data ->
            when (microId) {
                // 1 = VARID_OLD_PTR — skip
                2 -> if (data.size >= 4) flags = data.toInt32()   // VARID_FLAGS
                3 -> if (data.size >= 4) id = data.toInt32()      // VARID_ID
                // 4 = VARID_WAYPOINT_PTR — skip (pointer remapping)
            }
        }

        return Pair(id, flags)
    }

    // ─── Waypoints ──────────────────────────────────────────────────────────────

    private fun parseWaypoint(waypointChunk: ChunkReader): Waypoint? {
        val variablesChunk = waypointChunk.findChunk(WAYPOINT_VARIABLES_ID) ?: return null

        var id = 0
        var flags = 0
        var position = Vector3.ZERO
        var actionPortalId = -1

        variablesChunk.forEachMicroChunk { microId, data ->
            when (microId) {
                // 1 = VARID_OLD_PTR — skip
                2 -> if (data.size >= 4) flags = data.toInt32()              // VARID_FLAGS
                3 -> if (data.size >= 12) position = data.toVector3()        // VARID_POSITION
                4 -> if (data.size >= 4) id = data.toInt32()                 // VARID_ID
                5 -> if (data.size >= 4) actionPortalId = data.toInt32()     // VARID_ACTION_ID
            }
        }

        return Waypoint(id = id, position = position, flags = flags, actionPortalId = actionPortalId)
    }
}
