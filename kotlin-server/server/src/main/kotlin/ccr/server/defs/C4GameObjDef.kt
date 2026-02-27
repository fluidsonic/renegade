package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// C++: C4GameObjDef : public SimpleGameObjDef : public PhysicalGameObjDef (c4.h / c4.cpp)
open class C4GameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    physDefId: Int = 0,
    // C++: float ThrowVelocity (initialized to 5)
    val throwVelocity: Float = 5f,
) : SimpleGameObjDef(
    name      = name,
    id        = id,
    chunkId   = chunkId,
    physDefId = physDefId,
) {
    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_C4 — from combatchunkid.h
        const val CHUNK_ID: UInt = 0x00040103u

        // C++: c4.cpp local enum starting at 930991700
        const val CHUNKID_DEF_PARENT    = 930991700
        const val CHUNKID_DEF_VARIABLES = 930991701

        // C++: micro-chunk IDs inside CHUNKID_DEF_VARIABLES
        const val MICROCHUNKID_DEF_THROW_VELOCITY = 1

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): C4GameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES.toUInt())
            val throwVelocity = vars?.findMicroChunk(MICROCHUNKID_DEF_THROW_VELOCITY)?.let { bytes ->
                if (bytes.size >= 4) ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float else null
            } ?: 5f
            return C4GameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                throwVelocity = throwVelocity,
            )
        }
    }
}
