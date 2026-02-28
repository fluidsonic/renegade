package ccr.physics

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of PhysDefClass (wwphys/phys.h).
 * Standalone class — mirrors C++ PhysDefClass without extending server DefinitionClass.
 *
 * C++ defaults: ModelName("NULL"), IsPreLit(false)
 */
open class PhysDefClass(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val modelName: String = "NULL",
    val isPreLit: Boolean = false,
) {

    companion object {
        // Chunk IDs from phys.cpp local enum
        internal const val PHYSDEF_CHUNK_DEFINITION = 0x055FFE07u
        internal const val PHYSDEF_CHUNK_VARIABLES = 0x055FFE08u
        internal const val CHUNK_DEFINITION: UInt = PHYSDEF_CHUNK_DEFINITION
        internal const val CHUNK_VARIABLES: UInt = PHYSDEF_CHUNK_VARIABLES

        // Micro-chunk IDs (0x00 = FLAGS is obsolete, 0x01 = MODELNAME, 0x02 = ISPRELIT)
        internal const val PHYSDEF_VARIABLE_MODELNAME = 0x01
        internal const val PHYSDEF_VARIABLE_ISPRELIT = 0x02
        internal const val VARIABLE_MODELNAME: Int = PHYSDEF_VARIABLE_MODELNAME
        internal const val VARIABLE_ISPRELIT: Int = PHYSDEF_VARIABLE_ISPRELIT

        // Micro-chunk helpers (internal so child classes can reuse)
        internal fun microChunkInt(bytes: ByteArray): Int? {
            if (bytes.size < 4) return null
            return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        }

        internal fun microChunkFloat(bytes: ByteArray): Float? {
            if (bytes.size < 4) return null
            return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float
        }

        internal fun microChunkBool(bytes: ByteArray): Boolean? {
            if (bytes.isEmpty()) return null
            return bytes[0] != 0.toByte()
        }

        internal fun microChunkString(bytes: ByteArray): String? {
            val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
            val len = if (nullIdx < 0) bytes.size else nullIdx
            return String(bytes, 0, len, Charsets.ISO_8859_1)
        }
    }
}
