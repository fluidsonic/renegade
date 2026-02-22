package ccr.server.defs.combat

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of C4GameObjDef (Combat/c4.cpp).
 *
 * C++ default: ThrowVelocity(5)
 */
data class C4GameObjDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val throwVelocity: Float = 5f,
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040103u  // CHUNKID_GAME_OBJECT_DEF_C4
    }
}

// Chunk IDs from c4.cpp local enum
private const val CHUNKID_DEF_VARIABLES = 930991701u

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_THROW_VELOCITY = 1

fun parseC4GameObjDef(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): C4GameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)

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
