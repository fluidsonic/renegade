package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of Phys3DefClass (wwphys/phys3.h).
 * Inherits from MoveablePhysDefClass.
 *
 * C++ defaults:
 *   NormSpeed(10.0f), SlideAngle(DEG_TO_RADF(45)), StepHeight(0.25f)
 */
open class Phys3DefClass(
    name: String,
    id: UInt,
    classId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    mass: Float = 1f,
    gravScale: Float = 1f,
    elasticity: Float = 0.1f,
    cinematicCollisionMode: Int = CINEMATIC_COLLISION_PUSH,
    val normSpeed: Float = 10f,
    val slideAngle: Float = 0.7853982f, // DEG_TO_RADF(45)
    val stepHeight: Float = 0.25f,
) : MoveablePhysDefClass(name, id, classId, modelName, isPreLit, mass, gravScale, elasticity, cinematicCollisionMode) {

    internal data class ParsedFields(
        val modelName: String,
        val isPreLit: Boolean,
        val mass: Float,
        val gravScale: Float,
        val elasticity: Float,
        val cinematicCollisionMode: Int,
        val normSpeed: Float,
        val slideAngle: Float,
        val stepHeight: Float,
    )

    companion object {
        const val CLASS_ID: UInt = 0x9004u

        // Chunk IDs from phys3.cpp local enum
        internal const val PHYS3DEF_CHUNK_MOVEABLEPHYSDEF = 0x04486000u
        internal const val PHYS3DEF_CHUNK_VARIABLES = 0x04486001u

        // Micro-chunk IDs (starting at 0x00)
        internal const val PHYS3DEF_VARIABLE_NORMSPEED = 0x00
        internal const val PHYS3DEF_VARIABLE_SLIDEANGLE = 0x01
        internal const val PHYS3DEF_VARIABLE_STEPHEIGHT = 0x02

        internal fun parseFields(phys3Chunk: ChunkReader): ParsedFields {
            // Parse parent (MoveablePhysDefClass) from its wrapper chunk
            val moveableChunk = phys3Chunk.findChunk(PHYS3DEF_CHUNK_MOVEABLEPHYSDEF)
            val parentFields = if (moveableChunk != null) {
                MoveablePhysDefClass.parseFields(moveableChunk)
            } else {
                MoveablePhysDefClass.ParsedFields("NULL", false, 1f, 1f, 0.1f, MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH)
            }

            // Own fields
            val vars = phys3Chunk.findChunk(PHYS3DEF_CHUNK_VARIABLES)
            val normSpeed = vars?.let { PhysDefClass.microChunkFloat(it, PHYS3DEF_VARIABLE_NORMSPEED) } ?: 10f
            val slideAngle = vars?.let { PhysDefClass.microChunkFloat(it, PHYS3DEF_VARIABLE_SLIDEANGLE) } ?: 0.7853982f
            val stepHeight = vars?.let { PhysDefClass.microChunkFloat(it, PHYS3DEF_VARIABLE_STEPHEIGHT) } ?: 0.25f

            return ParsedFields(
                modelName = parentFields.modelName,
                isPreLit = parentFields.isPreLit,
                mass = parentFields.mass,
                gravScale = parentFields.gravScale,
                elasticity = parentFields.elasticity,
                cinematicCollisionMode = parentFields.cinematicCollisionMode,
                normSpeed = normSpeed,
                slideAngle = slideAngle,
                stepHeight = stepHeight,
            )
        }

        /**
         * Loads a Phys3DefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [PHYS3DEF_CHUNK_MOVEABLEPHYSDEF]
         *   [MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF]
         *     [DYNAMICPHYSDEF_CHUNK_PHYSDEF]
         *       [PHYSDEF_CHUNK_DEFINITION] → DefinitionClass
         *       [PHYSDEF_CHUNK_VARIABLES]  → modelName, isPreLit
         *   [MOVEABLEPHYSDEF_CHUNK_VARIABLES] → mass, gravScale, elasticity, cinematicCollisionMode
         * [PHYS3DEF_CHUNK_VARIABLES] → normSpeed, slideAngle, stepHeight
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, classId: UInt): Phys3DefClass {
            val fields = parseFields(objDataChunk)

            return Phys3DefClass(
                name = name,
                id = id,
                classId = classId,
                modelName = fields.modelName,
                isPreLit = fields.isPreLit,
                mass = fields.mass,
                gravScale = fields.gravScale,
                elasticity = fields.elasticity,
                cinematicCollisionMode = fields.cinematicCollisionMode,
                normSpeed = fields.normSpeed,
                slideAngle = fields.slideAngle,
                stepHeight = fields.stepHeight,
            )
        }
    }
}
