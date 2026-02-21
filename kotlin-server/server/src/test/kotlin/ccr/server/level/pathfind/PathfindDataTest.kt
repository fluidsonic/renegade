package ccr.server.level.pathfind

import ccr.server.level.ChunkIds
import ccr.server.level.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathfindDataTest {

    @Test
    fun `empty PathfindData has zero counts`() {
        val data = PathfindData()
        assertEquals(0, data.sectorCount)
        assertEquals(0, data.portalCount)
        assertEquals(0, data.waypathCount)
        assertTrue(data.sectors.isEmpty())
        assertTrue(data.portals.isEmpty())
        assertTrue(data.waypaths.isEmpty())
    }

    @Test
    fun `PathSector has sensible defaults`() {
        val sector = PathSector()
        assertEquals(Vector3.ZERO, sector.center)
        assertEquals(Vector3.ZERO, sector.extent)
        assertTrue(sector.portalIds.isEmpty())
    }

    @Test
    fun `PathPortal has sensible defaults`() {
        val portal = PathPortal()
        assertEquals(0, portal.id)
        assertEquals(0, portal.destSector1)
        assertEquals(0, portal.destSector2)
        assertEquals(Vector3.ZERO, portal.center)
        assertEquals(Vector3.ZERO, portal.extent)
    }

    @Test
    fun `Waypath flags match C++ constants`() {
        assertEquals(0x01, Waypath.FLAG_TWO_WAY)
        assertEquals(0x02, Waypath.FLAG_LOOPING)
        assertEquals(0x04, Waypath.FLAG_HUMAN)
        assertEquals(0x08, Waypath.FLAG_GROUND_VEHICLE)
        assertEquals(0x10, Waypath.FLAG_FLYING_VEHICLE)
        assertEquals(0x20, Waypath.FLAG_INNATE_PATHFIND)
    }

    @Test
    fun `Waypoint has sensible defaults`() {
        val wp = Waypoint()
        assertEquals(0, wp.id)
        assertEquals(Vector3.ZERO, wp.position)
        assertEquals(0, wp.flags)
        assertEquals(-1, wp.actionPortalId)
    }

    @Test
    fun `PathfindData counts match list sizes`() {
        val data = PathfindData(
            sectors = listOf(PathSector(), PathSector()),
            portals = listOf(PathPortal()),
            waypaths = listOf(Waypath(), Waypath(), Waypath()),
        )
        assertEquals(2, data.sectorCount)
        assertEquals(1, data.portalCount)
        assertEquals(3, data.waypathCount)
    }

    @Test
    fun `PathfindLoader returns empty data for empty chunk`() {
        val emptyReader = ChunkReader(ByteArray(0))
        val data = PathfindLoader.load(emptyReader)
        assertEquals(0, data.sectorCount)
        assertEquals(0, data.portalCount)
        assertEquals(0, data.waypathCount)
    }

    @Test
    fun `PathfindLoader parses portal from synthetic chunk`() {
        // Build a minimal pathfind database with one portal.
        // Portal micro-chunks: BOUNDING_BOX(1), DEST_SECTOR1(3), DEST_SECTOR2(4), ID(5)
        val portalVariables = buildMicroChunks {
            // VARID_BOUNDING_BOX = 1, AABoxClass: center(10,20,30) + extent(5,5,5)
            microChunk(1, floats(10f, 20f, 30f, 5f, 5f, 5f))
            // VARID_DEST_SECTOR1 = 3, uint16 = 0
            microChunk(3, uint16(0))
            // VARID_DEST_SECTOR2 = 4, uint16 = 1
            microChunk(4, uint16(1))
            // VARID_ID = 5, uint32 = 42
            microChunk(5, int32(42))
        }

        // Wrap in PORTAL_VARIABLES chunk (0x01060654)
        val portalChunkData = wrapChunk(0x01060654u, portalVariables)
        // Wrap in PORTAL chunk (0x01060637)
        val portalChunk = wrapChunk(ChunkIds.PATHFIND_CHUNK_PORTAL, portalChunkData, isContainer = true)
        // Wrap in DATABASE chunk (0x01060635)
        val databaseChunk = wrapChunk(ChunkIds.PATHFIND_CHUNK_DATABASE, portalChunk, isContainer = true)

        val reader = ChunkReader(databaseChunk)
        val data = PathfindLoader.load(reader)

        assertEquals(1, data.portalCount)
        val portal = data.portals.first()
        assertEquals(42, portal.id)
        assertEquals(0, portal.destSector1)
        assertEquals(1, portal.destSector2)
        assertEquals(10f, portal.center.x)
        assertEquals(20f, portal.center.y)
        assertEquals(30f, portal.center.z)
        assertEquals(5f, portal.extent.x)
    }

    @Test
    fun `PathfindLoader parses sector from synthetic chunk`() {
        // Build sector micro-chunks: BOUNDING_BOX + two PORTAL_IDs
        val sectorVariables = buildMicroChunks {
            microChunk(1, floats(1f, 2f, 3f, 4f, 5f, 6f)) // AABoxClass
            microChunk(2, int32(10)) // portal ID 10
            microChunk(2, int32(20)) // portal ID 20
        }

        // SECTOR_VARIABLES (0x01060643) chunk
        val sectorVarsChunk = wrapChunk(0x01060643u, sectorVariables)
        // SECTOR_OBJECT (0x0106063C) — container holding the variables chunk
        val sectorObject = wrapChunk(ChunkIds.PATHFIND_CHUNK_SECTOR_OBJECT, sectorVarsChunk, isContainer = true)
        // SECTOR (0x01060639) — container holding sector object
        val sector = wrapChunk(ChunkIds.PATHFIND_CHUNK_SECTOR, sectorObject, isContainer = true)
        // SECTOR_CULLING_SYSTEM (0x01060638) — container holding sectors
        val culling = wrapChunk(ChunkIds.PATHFIND_CHUNK_SECTOR_CULLING_SYSTEM, sector, isContainer = true)
        // DATABASE
        val database = wrapChunk(ChunkIds.PATHFIND_CHUNK_DATABASE, culling, isContainer = true)

        val data = PathfindLoader.load(ChunkReader(database))

        assertEquals(1, data.sectorCount)
        val s = data.sectors.first()
        assertEquals(1f, s.center.x)
        assertEquals(2f, s.center.y)
        assertEquals(3f, s.center.z)
        assertEquals(4f, s.extent.x)
        assertEquals(listOf(10, 20), s.portalIds)
    }

    @Test
    fun `PathfindLoader parses waypath with waypoints from synthetic chunk`() {
        // Build a waypath followed by two waypoints
        val waypathVars = buildMicroChunks {
            microChunk(2, int32(0x0D)) // flags: TWO_WAY | HUMAN | GROUND_VEHICLE
            microChunk(3, int32(7))    // id = 7
        }
        val waypathChunk = wrapChunk(0x04290219u, waypathVars) // WAYPATH_VARIABLES

        val wp1Vars = buildMicroChunks {
            microChunk(3, floats(100f, 200f, 300f)) // position
            microChunk(4, int32(1))                  // id = 1
        }
        val wp1Chunk = wrapChunk(0x04290112u, wp1Vars) // WAYPOINT_VARIABLES

        val wp2Vars = buildMicroChunks {
            microChunk(3, floats(400f, 500f, 600f)) // position
            microChunk(4, int32(2))                  // id = 2
        }
        val wp2Chunk = wrapChunk(0x04290112u, wp2Vars) // WAYPOINT_VARIABLES

        // Wrap in persist factory chunks
        val waypathPersist = wrapChunk(ChunkIds.PHYSICS_CHUNKID_WAYPATH, waypathChunk, isContainer = true)
        val wp1Persist = wrapChunk(ChunkIds.PHYSICS_CHUNKID_WAYPOINT, wp1Chunk, isContainer = true)
        val wp2Persist = wrapChunk(ChunkIds.PHYSICS_CHUNKID_WAYPOINT, wp2Chunk, isContainer = true)

        val databaseContent = waypathPersist + wp1Persist + wp2Persist
        val database = wrapChunk(ChunkIds.PATHFIND_CHUNK_DATABASE, databaseContent, isContainer = true)

        val data = PathfindLoader.load(ChunkReader(database))

        assertEquals(1, data.waypathCount)
        val waypath = data.waypaths.first()
        assertEquals(7, waypath.id)
        assertEquals(0x0D, waypath.flags)
        assertEquals(2, waypath.waypoints.size)
        assertEquals(100f, waypath.waypoints[0].position.x)
        assertEquals(1, waypath.waypoints[0].id)
        assertEquals(400f, waypath.waypoints[1].position.x)
        assertEquals(2, waypath.waypoints[1].id)
    }

    @Test
    fun `ChunkIds pathfind values match C++ enum`() {
        // Pathfind.cpp enum starting at 0x01060635
        assertEquals(0x01060635u, ChunkIds.PATHFIND_CHUNK_DATABASE)
        assertEquals(0x01060637u, ChunkIds.PATHFIND_CHUNK_PORTAL)
        assertEquals(0x01060638u, ChunkIds.PATHFIND_CHUNK_SECTOR_CULLING_SYSTEM)
        assertEquals(0x01060639u, ChunkIds.PATHFIND_CHUNK_SECTOR)
        assertEquals(0x0106063Cu, ChunkIds.PATHFIND_CHUNK_SECTOR_OBJECT)
        assertEquals(0x0106063Du, ChunkIds.PATHFIND_CHUNK_HEIGHTDB)
        assertEquals(0x0106063Eu, ChunkIds.PATHFIND_CHUNK_ACTION_PORTAL)
        assertEquals(0x0106063Fu, ChunkIds.PATHFIND_CHUNK_WAYPATH_PORTAL)
        assertEquals(0x01060640u, ChunkIds.PATHFIND_CHUNK_PATHFIND_SECTOR_OBJECT)

        // wwphysids.h: CHUNKID_WWPHYS_BEGIN(0x00020000) + 0x100 + offset
        assertEquals(0x00020110u, ChunkIds.PHYSICS_CHUNKID_WAYPATH)
        assertEquals(0x00020111u, ChunkIds.PHYSICS_CHUNKID_WAYPOINT)
    }

    // ─── Test helpers ───────────────────────────────────────────────────────────

    /** Build a W3D chunk: 8-byte header (type LE, size LE with bit 31 for container) + data. */
    private fun wrapChunk(type: UInt, data: ByteArray, isContainer: Boolean = false): ByteArray {
        val size = data.size.toLong() or (if (isContainer) 0x80000000L else 0L)
        val buf = ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(type.toInt())
        buf.putInt(size.toInt())
        buf.put(data)
        return buf.array()
    }

    private class MicroChunkBuilder {
        val bytes = mutableListOf<Byte>()

        fun microChunk(id: Int, data: ByteArray) {
            bytes.add(id.toByte())
            bytes.add(data.size.toByte())
            data.forEach { bytes.add(it) }
        }
    }

    private fun buildMicroChunks(block: MicroChunkBuilder.() -> Unit): ByteArray {
        val builder = MicroChunkBuilder()
        builder.block()
        return builder.bytes.toByteArray()
    }

    private fun floats(vararg values: Float): ByteArray {
        val buf = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { buf.putFloat(it) }
        return buf.array()
    }

    private fun int32(value: Int): ByteArray {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(value)
        return buf.array()
    }

    private fun uint16(value: Int): ByteArray {
        val buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(value.toShort())
        return buf.array()
    }
}
