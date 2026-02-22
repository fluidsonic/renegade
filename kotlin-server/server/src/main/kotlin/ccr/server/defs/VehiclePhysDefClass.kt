package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of VehiclePhysDefClass (wwphys/vehiclephys.h).
 * Inherits from RigidBodyDefClass.
 *
 * C++ defaults: SpringConstant(3.0f), DampingConstant(0.75f), SpringLength(1.0f),
 *   TractionMultiplier(2.0f), LateralMomentArm(0.0f), TractiveMomentArm(0.0f),
 *   EngineFlameLength(1.0f), IsFake(false)
 */
open class VehiclePhysDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    mass: Float = 1f,
    gravScale: Float = 1f,
    elasticity: Float = 0.1f,
    cinematicCollisionMode: Int = CINEMATIC_COLLISION_PUSH,
    aerodynamicDragCoefficient: Float = 0f,
    collisionDisabled: Boolean = false,
    val springConstant: Float = 3f,
    val dampingConstant: Float = 0.75f,
    val springLength: Float = 1f,
    val tractionMultiplier: Float = 2f,
    val lateralMomentArm: Float = 0f,
    val tractiveMomentArm: Float = 0f,
    val engineFlameLength: Float = 1f,
    val isFake: Boolean = false,
) : RigidBodyDefClass(
    name, id, chunkId, modelName, isPreLit, mass, gravScale, elasticity,
    cinematicCollisionMode, aerodynamicDragCoefficient, collisionDisabled,
) {

    internal data class ParsedFields(
        val modelName: String,
        val isPreLit: Boolean,
        val mass: Float,
        val gravScale: Float,
        val elasticity: Float,
        val cinematicCollisionMode: Int,
        val aerodynamicDragCoefficient: Float,
        val collisionDisabled: Boolean,
        val springConstant: Float,
        val dampingConstant: Float,
        val springLength: Float,
        val tractionMultiplier: Float,
        val lateralMomentArm: Float,
        val tractiveMomentArm: Float,
        val engineFlameLength: Float,
        val isFake: Boolean,
    )

    companion object {
        /** CLASSID_VEHICLEPHYSDEF = CLASSID_PHYSICS + 11 = 0x900B */
        const val CHUNK_ID: UInt = 0x0002050Cu  // PHYSICS_CHUNKID_VEHICLEPHYSDEF

        // Chunk IDs from vehiclephys.cpp local enum
        internal const val VEHICLEPHYSDEF_CHUNK_RIGIDBODYDEF = 405001519u  // 0x1823D52F
        internal const val VEHICLEPHYSDEF_CHUNK_VARIABLES = 405001520u     // 0x1823D530

        // Micro-chunk IDs (starting at 0x00)
        internal const val VEHICLEPHYSDEF_VARIABLE_SPRINGCONSTANT = 0x00
        internal const val VEHICLEPHYSDEF_VARIABLE_DAMPINGCONSTANT = 0x01
        internal const val VEHICLEPHYSDEF_VARIABLE_SPRINGLENGTH = 0x02
        internal const val VEHICLEPHYSDEF_VARIABLE_TRACTIONMULTIPLIER = 0x03
        internal const val VEHICLEPHYSDEF_VARIABLE_LATERALMOMENTARM = 0x04
        internal const val VEHICLEPHYSDEF_VARIABLE_TRACTIVEMOMENTARM = 0x05
        internal const val VEHICLEPHYSDEF_VARIABLE_ENGINEFLAMELENGTH = 0x06
        internal const val VEHICLEPHYSDEF_VARIABLE_ISFAKE = 0x07

        internal fun parseFields(vehiclePhysChunk: ChunkReader): ParsedFields {
            val rigidBodyChunk = vehiclePhysChunk.findChunk(VEHICLEPHYSDEF_CHUNK_RIGIDBODYDEF)
            val parentFields = if (rigidBodyChunk != null) {
                RigidBodyDefClass.parseFields(rigidBodyChunk)
            } else {
                RigidBodyDefClass.ParsedFields("NULL", false, 1f, 1f, 0.1f, CINEMATIC_COLLISION_PUSH, 0f, false)
            }

            val vars = vehiclePhysChunk.findChunk(VEHICLEPHYSDEF_CHUNK_VARIABLES)
            val springConstant = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_SPRINGCONSTANT) } ?: 3f
            val dampingConstant = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_DAMPINGCONSTANT) } ?: 0.75f
            val springLength = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_SPRINGLENGTH) } ?: 1f
            val tractionMultiplier = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_TRACTIONMULTIPLIER) } ?: 2f
            val lateralMomentArm = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_LATERALMOMENTARM) } ?: 0f
            val tractiveMomentArm = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_TRACTIVEMOMENTARM) } ?: 0f
            val engineFlameLength = vars?.let { PhysDefClass.microChunkFloat(it, VEHICLEPHYSDEF_VARIABLE_ENGINEFLAMELENGTH) } ?: 1f
            val isFake = vars?.let { PhysDefClass.microChunkBool(it, VEHICLEPHYSDEF_VARIABLE_ISFAKE) } ?: false

            return ParsedFields(
                modelName = parentFields.modelName,
                isPreLit = parentFields.isPreLit,
                mass = parentFields.mass,
                gravScale = parentFields.gravScale,
                elasticity = parentFields.elasticity,
                cinematicCollisionMode = parentFields.cinematicCollisionMode,
                aerodynamicDragCoefficient = parentFields.aerodynamicDragCoefficient,
                collisionDisabled = parentFields.collisionDisabled,
                springConstant = springConstant,
                dampingConstant = dampingConstant,
                springLength = springLength,
                tractionMultiplier = tractionMultiplier,
                lateralMomentArm = lateralMomentArm,
                tractiveMomentArm = tractiveMomentArm,
                engineFlameLength = engineFlameLength,
                isFake = isFake,
            )
        }

        /**
         * Loads a VehiclePhysDefClass from the OBJDATA chunk.
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, chunkId: UInt): VehiclePhysDefClass {
            val fields = parseFields(objDataChunk)

            return VehiclePhysDefClass(
                name = name,
                id = id,
                chunkId = chunkId,
                modelName = fields.modelName,
                isPreLit = fields.isPreLit,
                mass = fields.mass,
                gravScale = fields.gravScale,
                elasticity = fields.elasticity,
                cinematicCollisionMode = fields.cinematicCollisionMode,
                aerodynamicDragCoefficient = fields.aerodynamicDragCoefficient,
                collisionDisabled = fields.collisionDisabled,
                springConstant = fields.springConstant,
                dampingConstant = fields.dampingConstant,
                springLength = fields.springLength,
                tractionMultiplier = fields.tractionMultiplier,
                lateralMomentArm = fields.lateralMomentArm,
                tractiveMomentArm = fields.tractiveMomentArm,
                engineFlameLength = fields.engineFlameLength,
                isFake = fields.isFake,
            )
        }
    }
}
