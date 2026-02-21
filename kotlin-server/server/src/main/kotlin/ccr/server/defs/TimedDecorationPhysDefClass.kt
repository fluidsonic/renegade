package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of TimedDecorationPhysDefClass (wwphys/timeddecophys.cpp).
 *
 * A decoration physics object with a limited lifetime.
 * Extends DecorationPhysDefClass (parent chain handled via nested chunk).
 *
 * C++ default: Lifetime(2.0f)
 *
 * Chunk layout inside OBJDATA:
 *   [TIMEDDECOPHYSDEF_CHUNK_DECOPHYSDEF = 0x01170003]
 *     -> DecorationPhysDefClass::Save() output (modelName, isPreLit nested within)
 *   [TIMEDDECOPHYSDEF_CHUNK_VARIABLES   = 0x01170004]
 *     -> micro-chunk 0x00: lifetime (float)
 */
data class TimedDecorationPhysDefClass(
    val name: String,
    val id: UInt,
    // PhysDefClass fields (via DecorationPhysDefClass -> DynamicPhysDefClass -> PhysDefClass)
    val modelName: String = "NULL",
    val isPreLit: Boolean = false,
    // Own fields
    val lifetime: Float = 2.0f,
) {
    companion object {
        /** CLASSID_TIMEDDECOPHYSDEF (wwphysids.h enum offset 10 from CLASSID_PHYSICS=0x9000) */
        const val CLASS_ID: UInt = 0x900Au

        // Chunk IDs from timeddecophys.cpp
        private const val CHUNKID_PARENT = 0x01170003u
        private const val CHUNKID_VARIABLES = 0x01170004u

        // Micro-chunk variable IDs
        private const val VARID_LIFETIME = 0x00

        fun load(objDataChunk: ChunkReader, name: String, id: UInt): TimedDecorationPhysDefClass {
            // Parse parent DecorationPhysDefClass fields (modelName, isPreLit)
            val parentChunk = objDataChunk.findChunk(CHUNKID_PARENT)
            val (modelName, isPreLit) = if (parentChunk != null) {
                DecorationPhysDefClass.parseParentFields(parentChunk)
            } else {
                "NULL" to false
            }

            // Parse own variables
            val vars = objDataChunk.findChunk(CHUNKID_VARIABLES)
            val lifetime = vars?.readMicroFloat(VARID_LIFETIME) ?: 2.0f

            return TimedDecorationPhysDefClass(
                name = name,
                id = id,
                modelName = modelName,
                isPreLit = isPreLit,
                lifetime = lifetime,
            )
        }
    }
}
