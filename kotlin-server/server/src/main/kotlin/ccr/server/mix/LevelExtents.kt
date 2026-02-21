package ccr.server.mix

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WorldExtents(
    val minX: Float, val minY: Float, val minZ: Float,
    val maxX: Float, val maxY: Float, val maxZ: Float,
)

/**
 * Extracts world extents from an .lsd file (level static data).
 *
 * Navigate chunk hierarchy:
 *   [PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM = 0x00020000]  (saveloadids.h + wwphysids.h)
 *     [PSDSSC_CHUNKID_SCENE = 0x04433220]                 (physstaticsavesystem.h)
 *       [PSCENE_SD_CHUNK_STATIC_OBJECT_AABTREE = 0x4500]  (pscene_saveload.cpp)
 *         [STATICAABTREE_CHUNK_AABTREE_CLASS_DATA = 0x104] (staticaabtreecull.cpp)
 *           [AABTREE_CHUNK_VERSION = 0x1]                 skipped (aabtreecull.cpp)
 *           [AABTREE_CHUNK_AABNODE = 0x101]               root node
 *             [AABTREE_CHUNK_AABNODE_VARIABLES = 0x104]   new format
 *               micro-chunk 0x00 → IOAABNodeStruct (28 bytes)
 *             — or —
 *             [AABTREE_CHUNK_AABNODE_INFO = 0x102]        legacy format
 *               raw IOAABNodeStruct (28 bytes)
 *
 * IOAABNodeStruct (WWMath/aabtreecull.cpp):
 *   float  Center.X   offset  0
 *   float  Center.Y   offset  4
 *   float  Center.Z   offset  8
 *   float  Extent.X   offset 12
 *   float  Extent.Y   offset 16
 *   float  Extent.Z   offset 20
 *   uint32 Attributes offset 24
 *   (total: 28 bytes, little-endian)
 *
 * World bounds: min = Center - Extent, max = Center + Extent
 */
fun extractLevelExtents(lsdData: ByteArray): WorldExtents {
    // CHUNKID_WWPHYS_BEGIN = 0x00020000 (saveloadids.h)
    // PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM = CHUNKID_WWPHYS_BEGIN + 0 (wwphysids.h)
    val CHUNK_STATIC_DATA_SUBSYSTEM = 0x00020000u

    // PSDSSC_CHUNKID_SCENE = 0x04433220 (physstaticsavesystem.h)
    val CHUNK_PSDSSC_SCENE = 0x04433220u

    // PSCENE_SD_CHUNK_STATIC_OBJECT_AABTREE = 0x00004500 (pscene_saveload.cpp)
    val CHUNK_STATIC_OBJECT_AABTREE = 0x00004500u

    // STATICAABTREE_CHUNK_AABTREE_CLASS_DATA = 0x00000104 (staticaabtreecull.cpp)
    val CHUNK_AABTREE_CLASS_DATA = 0x00000104u

    // AABTREE_CHUNK_VERSION = 0x00000001 — version number chunk in class data (aabtreecull.cpp:Load)
    // AABTREE_CURRENT_VERSION = 0x00010000 (aabtreecull.cpp)
    val CHUNK_AABTREE_VERSION = 0x00000001u
    val AABTREE_CURRENT_VERSION = 0x00010000

    // AABTree chunk IDs (WWMath/aabtreecull.cpp):
    //   AABTREE_CHUNK_AABNODE          = 0x00000101
    //   AABTREE_CHUNK_AABNODE_INFO     = 0x00000102  (OBSOLETE legacy: raw IOAABNodeStruct)
    //   AABTREE_CHUNK_AABNODE_CONTENTS = 0x00000103  (skipped — wraps object linkage)
    //   AABTREE_CHUNK_AABNODE_VARIABLES = 0x00000104  (new: micro-chunks with IOAABNodeStruct)
    //
    // NOTE: STATICAABTREE_CHUNK_AABTREE_CLASS_DATA also = 0x00000104. These live in different
    //       nesting levels so there is no collision: CLASS_DATA is inside STATIC_OBJECT_AABTREE,
    //       while AABNODE_VARIABLES is inside AABNODE.
    val CHUNK_AABNODE           = 0x00000101u
    val CHUNK_AABNODE_INFO      = 0x00000102u  // legacy: raw IOAABNodeStruct
    val CHUNK_AABNODE_VARIABLES = 0x00000104u  // new: contains micro-chunk 0x00 → IOAABNodeStruct

    // AABTREE_VARIABLE_NODESTRUCT = 0x00 (aabtreecull.cpp)
    val MICRO_VARIABLE_NODESTRUCT = 0x00

    val lsdReader = ChunkReader(lsdData)

    val staticDataChunk = lsdReader.findChunk(CHUNK_STATIC_DATA_SUBSYSTEM)
        ?: error("CHUNK_STATIC_DATA_SUBSYSTEM (0x${CHUNK_STATIC_DATA_SUBSYSTEM.toString(16)}) not found in LSD data")

    val sceneChunk = staticDataChunk.findChunk(CHUNK_PSDSSC_SCENE)
        ?: error("CHUNK_PSDSSC_SCENE (0x${CHUNK_PSDSSC_SCENE.toString(16)}) not found")

    val staticAabTreeChunk = sceneChunk.findChunk(CHUNK_STATIC_OBJECT_AABTREE)
        ?: error("CHUNK_STATIC_OBJECT_AABTREE (0x${CHUNK_STATIC_OBJECT_AABTREE.toString(16)}) not found")

    val classDataChunk = staticAabTreeChunk.findChunk(CHUNK_AABTREE_CLASS_DATA)
        ?: error("CHUNK_AABTREE_CLASS_DATA (0x${CHUNK_AABTREE_CLASS_DATA.toString(16)}) not found")

    // Validate version before reading node data (aabtreecull.cpp:Load)
    val versionChunk = classDataChunk.findChunk(CHUNK_AABTREE_VERSION)
    if (versionChunk != null) {
        val version = versionChunk.readInt()
        require(version == AABTREE_CURRENT_VERSION) {
            "Unsupported AABTree version: 0x${version.toUInt().toString(16)} (expected 0x${AABTREE_CURRENT_VERSION.toUInt().toString(16)})"
        }
    }

    // The root node is the first AABTREE_CHUNK_AABNODE after AABTREE_CHUNK_VERSION (aabtreecull.cpp:Load)
    val rootNodeChunk = classDataChunk.findChunk(CHUNK_AABNODE)
        ?: error("CHUNK_AABNODE (0x${CHUNK_AABNODE.toString(16)}) not found in AABTree class data")

    // Read IOAABNodeStruct from either new or legacy sub-chunk format
    val nodeStructBytes: ByteArray = run {
        var result: ByteArray? = null

        rootNodeChunk.forEachChunk { id, _, reader ->
            if (result != null) return@forEachChunk
            when (id) {
                CHUNK_AABNODE_VARIABLES -> {
                    // New format (aabtreecull.cpp:765-783): micro-chunk 0x00 holds IOAABNodeStruct
                    result = reader.findMicroChunk(MICRO_VARIABLE_NODESTRUCT)
                        ?: error("AABTREE_VARIABLE_NODESTRUCT micro-chunk (0x00) not found in AABNODE_VARIABLES")
                }
                CHUNK_AABNODE_INFO -> {
                    // Legacy format (aabtreecull.cpp:759-762): raw IOAABNodeStruct bytes
                    result = reader.readBytes(28)
                }
                else -> { /* AABNODE_CONTENTS and others are not needed */ }
            }
        }

        result ?: error(
            "Neither CHUNK_AABNODE_VARIABLES (0x${CHUNK_AABNODE_VARIABLES.toString(16)}) " +
            "nor CHUNK_AABNODE_INFO (0x${CHUNK_AABNODE_INFO.toString(16)}) found in root AABNODE chunk"
        )
    }

    require(nodeStructBytes.size >= 24) {
        "IOAABNodeStruct data too short: ${nodeStructBytes.size} bytes (need at least 24 for Center+Extent)"
    }

    // Parse IOAABNodeStruct: Center(3×float32) + Extent(3×float32) + Attributes(uint32), little-endian
    val bb = ByteBuffer.wrap(nodeStructBytes).order(ByteOrder.LITTLE_ENDIAN)
    val centerX = bb.getFloat(0)
    val centerY = bb.getFloat(4)
    val centerZ = bb.getFloat(8)
    val extentX = bb.getFloat(12)
    val extentY = bb.getFloat(16)
    val extentZ = bb.getFloat(20)

    return WorldExtents(
        minX = centerX - extentX,
        minY = centerY - extentY,
        minZ = centerZ - extentZ,
        maxX = centerX + extentX,
        maxY = centerY + extentY,
        maxZ = centerZ + extentZ,
    )
}
