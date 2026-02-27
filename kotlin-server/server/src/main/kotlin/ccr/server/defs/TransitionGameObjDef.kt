package ccr.server.defs

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * C++: OBBoxClass in WWMath/obbox.h — oriented bounding box.
 */
data class OBBoxClass(
    val center: Vector3 = Vector3.ZERO,
    val extent: Vector3 = Vector3.ZERO,
    // Basis is a 3x3 rotation matrix stored as 9 floats (row-major)
    val basis: FloatArray = FloatArray(9) { if (it % 4 == 0) 1f else 0f }, // identity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OBBoxClass) return false
        return center == other.center && extent == other.extent && basis.contentEquals(other.basis)
    }

    override fun hashCode(): Int = center.hashCode() * 31 + extent.hashCode()
}

/**
 * C++: TransitionDataClass in Combat/transition.h
 * Saved per-transition inside TransitionGameObjDef.
 */
data class TransitionDataClass(
    val type: Int = 0,          // StyleType enum
    val zone: OBBoxClass = OBBoxClass(),
    val animationName: String = "",
    val endingTM: Matrix3D = Matrix3D(),
) {
    companion object {
        // From transition.cpp namespace TRANSITION_DATA_CLASS_SAVELOAD
        private const val CHUNKID_VARIABLES = 0x11051106u
        private const val MCID_TYPE = 1
        private const val MCID_ZONE = 2
        private const val MCID_ANIMATION_NAME = 3
        private const val MCID_ENDING_TM = 4

        fun load(reader: ChunkReader): TransitionDataClass? {
            val vars = reader.findChunk(CHUNKID_VARIABLES) ?: return null

            val typeBytes = vars.findMicroChunk(MCID_TYPE)
            val rawType = if (typeBytes != null && typeBytes.size >= 4)
                ByteBuffer.wrap(typeBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            else 0
            // C++ On_Post_Load remaps legacy vehicle transition types
            val type = when (rawType) {
                4, 5 -> STYLE_VEHICLE_ENTER   // LEGACY_VEHICLE_ENTER_0/1
                6, 7 -> STYLE_VEHICLE_EXIT    // LEGACY_VEHICLE_EXIT_0/1
                else -> rawType
            }

            val zone = vars.findMicroChunk(MCID_ZONE)?.let { b ->
                if (b.size < 60) return@let null // 15 floats
                val bb = ByteBuffer.wrap(b, 0, 60).order(ByteOrder.LITTLE_ENDIAN)
                val center = Vector3(bb.float, bb.float, bb.float)
                val extent = Vector3(bb.float, bb.float, bb.float)
                val basis = FloatArray(9) { bb.float }
                OBBoxClass(center, extent, basis)
            } ?: OBBoxClass()

            val animName = vars.findMicroChunk(MCID_ANIMATION_NAME)?.let { b ->
                val nullIdx = b.indexOfFirst { it == 0.toByte() }
                val len = if (nullIdx < 0) b.size else nullIdx
                String(b, 0, len, Charsets.ISO_8859_1)
            } ?: ""

            val endingTM = vars.findMicroChunk(MCID_ENDING_TM)?.let { b ->
                if (b.size < 48) return@let null // 12 floats
                val bb = ByteBuffer.wrap(b, 0, 48).order(ByteOrder.LITTLE_ENDIAN)
                Matrix3D(
                    bb.float, bb.float, bb.float, bb.float,
                    bb.float, bb.float, bb.float, bb.float,
                    bb.float, bb.float, bb.float, bb.float,
                )
            } ?: Matrix3D()

            return TransitionDataClass(
                type = type,
                zone = zone,
                animationName = animName,
                endingTM = endingTM,
            )
        }

        // C++ TransitionDataClass::StyleType enum values
        const val STYLE_DISABLED = -1
        const val STYLE_LADDER_EXIT_TOP = 0
        const val STYLE_LADDER_EXIT_BOTTOM = 1
        const val STYLE_LADDER_ENTER_TOP = 2
        const val STYLE_LADDER_ENTER_BOTTOM = 3
        const val STYLE_VEHICLE_ENTER = 8
        const val STYLE_VEHICLE_EXIT = 9
    }
}

/**
 * C++: TransitionGameObjDef in Combat/transitiongameobj.h
 * Hierarchy: DefinitionClass → BaseGameObjDef → TransitionGameObjDef (leaf)
 *
 * Saves a list of TransitionDataClass entries as separate chunks.
 */
class TransitionGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val transitions: List<TransitionDataClass> = emptyList(),
) : BaseGameObjDef(name, id, chunkId) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040125u  // CHUNKID_GAME_OBJECT_DEF_TRANSITION

        // Chunk IDs from transitiongameobj.cpp
        private const val CHUNKID_DEF_PARENT = 1111991201u
        private const val CHUNKID_DEF_TRANSITION = 1111991202u

        // Base DefinitionClass
        private const val CHUNKID_BASE_VARIABLES = 0x00000100u
        private const val VARID_INSTANCEID = 0x01
        private const val VARID_NAME = 0x03

        fun load(chunkId: UInt, objDataChunk: ChunkReader): TransitionGameObjDef? {
            // Navigate parent chain: TransitionGameObjDef -> BaseGameObjDef -> DefinitionClass
            val parentChunk = objDataChunk.findChunk(CHUNKID_DEF_PARENT) ?: return null
            val baseVarsChunk = parentChunk.findChunkRecursive(CHUNKID_BASE_VARIABLES) ?: return null

            val idBytes = baseVarsChunk.findMicroChunk(VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = baseVarsChunk.findMicroChunk(VARID_NAME) ?: return null
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val nameLen = if (nullIdx < 0) nameBytes.size else nullIdx
            val name = String(nameBytes, 0, nameLen, Charsets.ISO_8859_1)

            // Parse transition list from separate CHUNKID_DEF_TRANSITION chunks
            val transitions = mutableListOf<TransitionDataClass>()
            objDataChunk.forEachChunk { cid, _, reader ->
                if (cid == CHUNKID_DEF_TRANSITION) {
                    TransitionDataClass.load(reader)?.let { transitions.add(it) }
                }
            }

            return TransitionGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                transitions = transitions,
            )
        }
    }
}
