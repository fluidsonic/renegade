package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of RigidBodyDefClass (wwphys/rbody.h).
 * Inherits from MoveablePhysDefClass.
 *
 * C++ defaults: AerodynamicDragCoefficient(0.0f), CollisionDisabled(false)
 */
open class RigidBodyDefClass(
    name: String,
    id: UInt,
    classId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    mass: Float = 1f,
    gravScale: Float = 1f,
    elasticity: Float = 0.1f,
    cinematicCollisionMode: Int = CINEMATIC_COLLISION_PUSH,
    val aerodynamicDragCoefficient: Float = 0f,
    val collisionDisabled: Boolean = false,
) : MoveablePhysDefClass(name, id, classId, modelName, isPreLit, mass, gravScale, elasticity, cinematicCollisionMode) {

    internal data class ParsedFields(
        val modelName: String,
        val isPreLit: Boolean,
        val mass: Float,
        val gravScale: Float,
        val elasticity: Float,
        val cinematicCollisionMode: Int,
        val aerodynamicDragCoefficient: Float,
        val collisionDisabled: Boolean,
    )

    companion object {
        const val CLASS_ID: UInt = 0x9005u

        // Chunk IDs from rbody.cpp local enum
        internal const val RIGIDBODYDEF_CHUNK_MOVEABLEPHYSDEF = 0x01106650u
        internal const val RIGIDBODYDEF_CHUNK_VARIABLES = 0x01106651u

        // Micro-chunk IDs (starting at 0x00)
        internal const val RIGIDBODYDEF_VARIABLE_AERODYNAMICDRAGCOEFFICIENT = 0x00
        internal const val RIGIDBODYDEF_VARIABLE_COLLISIONDISABLED = 0x01

        internal fun parseFields(rigidBodyChunk: ChunkReader): ParsedFields {
            val moveableChunk = rigidBodyChunk.findChunk(RIGIDBODYDEF_CHUNK_MOVEABLEPHYSDEF)
            val parentFields = if (moveableChunk != null) {
                MoveablePhysDefClass.parseFields(moveableChunk)
            } else {
                MoveablePhysDefClass.ParsedFields("NULL", false, 1f, 1f, 0.1f, CINEMATIC_COLLISION_PUSH)
            }

            val vars = rigidBodyChunk.findChunk(RIGIDBODYDEF_CHUNK_VARIABLES)
            val aerodynamicDragCoefficient = vars?.let { PhysDefClass.microChunkFloat(it, RIGIDBODYDEF_VARIABLE_AERODYNAMICDRAGCOEFFICIENT) } ?: 0f
            val collisionDisabled = vars?.let { PhysDefClass.microChunkBool(it, RIGIDBODYDEF_VARIABLE_COLLISIONDISABLED) } ?: false

            return ParsedFields(
                modelName = parentFields.modelName,
                isPreLit = parentFields.isPreLit,
                mass = parentFields.mass,
                gravScale = parentFields.gravScale,
                elasticity = parentFields.elasticity,
                cinematicCollisionMode = parentFields.cinematicCollisionMode,
                aerodynamicDragCoefficient = aerodynamicDragCoefficient,
                collisionDisabled = collisionDisabled,
            )
        }

        /**
         * Loads a RigidBodyDefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [RIGIDBODYDEF_CHUNK_MOVEABLEPHYSDEF]
         *   [MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF]
         *     [DYNAMICPHYSDEF_CHUNK_PHYSDEF]
         *       [PHYSDEF_CHUNK_DEFINITION] → DefinitionClass
         *       [PHYSDEF_CHUNK_VARIABLES]  → modelName, isPreLit
         *   [MOVEABLEPHYSDEF_CHUNK_VARIABLES] → mass, gravScale, elasticity, cinematicCollisionMode
         * [RIGIDBODYDEF_CHUNK_VARIABLES] → aerodynamicDragCoefficient, collisionDisabled
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, classId: UInt): RigidBodyDefClass {
            val fields = parseFields(objDataChunk)

            return RigidBodyDefClass(
                name = name,
                id = id,
                classId = classId,
                modelName = fields.modelName,
                isPreLit = fields.isPreLit,
                mass = fields.mass,
                gravScale = fields.gravScale,
                elasticity = fields.elasticity,
                cinematicCollisionMode = fields.cinematicCollisionMode,
                aerodynamicDragCoefficient = fields.aerodynamicDragCoefficient,
                collisionDisabled = fields.collisionDisabled,
            )
        }
    }
}
