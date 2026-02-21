package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of MoveablePhysDefClass (wwphys/movephys.h).
 * Inherits from DynamicPhysDefClass.
 *
 * C++ defaults: Mass(1.0f), GravScale(1.0f), Elasticity(0.1f),
 *   CinematicCollisionMode(CINEMATIC_COLLISION_PUSH = 2)
 */
open class MoveablePhysDefClass(
    name: String,
    id: UInt,
    classId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    val mass: Float = 1f,
    val gravScale: Float = 1f,
    val elasticity: Float = 0.1f,
    val cinematicCollisionMode: Int = CINEMATIC_COLLISION_PUSH,
) : DynamicPhysDefClass(name, id, classId, modelName, isPreLit) {

    internal data class ParsedFields(
        val modelName: String,
        val isPreLit: Boolean,
        val mass: Float,
        val gravScale: Float,
        val elasticity: Float,
        val cinematicCollisionMode: Int,
    )

    companion object {
        const val CINEMATIC_COLLISION_NONE = 0
        const val CINEMATIC_COLLISION_STOP = 1
        const val CINEMATIC_COLLISION_PUSH = 2
        const val CINEMATIC_COLLISION_KILL = 3

        // Chunk IDs from movephys.cpp local enum
        internal const val MOVEABLEPHYSDEF_CHUNK_PHYSDEF = 0x04486000u         // obsolete parent
        internal const val MOVEABLEPHYSDEF_CHUNK_VARIABLES = 0x04486001u
        internal const val MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF = 0x04486002u  // current parent

        // Micro-chunk IDs
        internal const val MOVEABLEPHYSDEF_VARIABLE_MASS = 0x00
        internal const val MOVEABLEPHYSDEF_VARIABLE_GRAVSCALE = 0x01
        internal const val MOVEABLEPHYSDEF_VARIABLE_ELASTICITY = 0x02
        internal const val MOVEABLEPHYSDEF_VARIABLE_CINEMATICCOLLISIONMODE = 0x03

        internal fun parseFields(moveableChunk: ChunkReader): ParsedFields {
            // Navigate to DynamicPhysDef → PhysDef for parent fields
            val dynamicChunk = moveableChunk.findChunk(MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF)
                ?: moveableChunk.findChunk(MOVEABLEPHYSDEF_CHUNK_PHYSDEF) // obsolete fallback
            val physDefChunk = dynamicChunk?.findChunk(DynamicPhysDefClass.DYNAMICPHYSDEF_CHUNK_PHYSDEF)
                ?: dynamicChunk // if old format, dynamicChunk IS the physDef

            val (modelName, isPreLit) = if (physDefChunk != null) {
                PhysDefClass.parseFields(physDefChunk)
            } else {
                "NULL" to false
            }

            // Own fields
            val vars = moveableChunk.findChunk(MOVEABLEPHYSDEF_CHUNK_VARIABLES)
            val mass = vars?.let { PhysDefClass.microChunkFloat(it, MOVEABLEPHYSDEF_VARIABLE_MASS) } ?: 1f
            val gravScale = vars?.let { PhysDefClass.microChunkFloat(it, MOVEABLEPHYSDEF_VARIABLE_GRAVSCALE) } ?: 1f
            val elasticity = vars?.let { PhysDefClass.microChunkFloat(it, MOVEABLEPHYSDEF_VARIABLE_ELASTICITY) } ?: 0.1f
            val cinematicCollisionMode = vars?.let { PhysDefClass.microChunkInt(it, MOVEABLEPHYSDEF_VARIABLE_CINEMATICCOLLISIONMODE) } ?: CINEMATIC_COLLISION_PUSH

            return ParsedFields(
                modelName = modelName,
                isPreLit = isPreLit,
                mass = mass,
                gravScale = gravScale,
                elasticity = elasticity,
                cinematicCollisionMode = cinematicCollisionMode,
            )
        }
    }
}
