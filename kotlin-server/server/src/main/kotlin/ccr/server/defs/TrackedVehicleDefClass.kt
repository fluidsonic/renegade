package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of TrackedVehicleDefClass (wwphys/trackedvehicle.h).
 * Inherits from VehiclePhysDefClass.
 *
 * Inheritance chain:
 *   TrackedVehicleDefClass → VehiclePhysDefClass → RigidBodyDefClass →
 *   MoveablePhysDefClass → DynamicPhysDefClass → PhysDefClass → DefinitionClass
 *
 * Note: TrackedVehicle extends VehiclePhysDefClass directly (NOT MotorVehicleDefClass).
 *
 * C++ defaults: MaxEngineTorque(0.0f), TrackUScaleFactor(25.0f),
 *   TrackVScaleFactor(0.0f), TurnTorqueScaleFactor(1.0f)
 */
class TrackedVehicleDefClass(
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
    springConstant: Float = 3f,
    dampingConstant: Float = 0.75f,
    springLength: Float = 1f,
    tractionMultiplier: Float = 2f,
    lateralMomentArm: Float = 0f,
    tractiveMomentArm: Float = 0f,
    engineFlameLength: Float = 1f,
    isFake: Boolean = false,
    val maxEngineTorque: Float = 0.0f,
    val trackUScaleFactor: Float = 25.0f,
    val trackVScaleFactor: Float = 0.0f,
    val turnTorqueScaleFactor: Float = 1.0f,
) : VehiclePhysDefClass(
    name, id, chunkId, modelName, isPreLit, mass, gravScale, elasticity,
    cinematicCollisionMode, aerodynamicDragCoefficient, collisionDisabled,
    springConstant, dampingConstant, springLength, tractionMultiplier,
    lateralMomentArm, tractiveMomentArm, engineFlameLength, isFake,
) {

    companion object {
        /** CLASSID_TRACKEDVEHICLEDEF = CLASSID_PHYSICS + 12 = 0x900C */
        const val CHUNK_ID: UInt = 0x0002050Du  // PHYSICS_CHUNKID_TRACKEDVEHICLEDEF

        // Chunk IDs from trackedvehicle.cpp local enum (C++ octal 0406001454 = 0x0418032C)
        private const val TRACKEDVEHICLEDEF_CHUNK_VEHICLEPHYSDEF = 0x0418032Cu
        private const val TRACKEDVEHICLEDEF_CHUNK_VARIABLES = 0x0418032Du

        // Micro-chunk IDs (starting at 0x00)
        private const val TRACKEDVEHICLEDEF_VARIABLE_MAXENGINETORQUE = 0x00
        private const val TRACKEDVEHICLEDEF_VARIABLE_TRACKUSCALEFACTOR = 0x01
        private const val TRACKEDVEHICLEDEF_VARIABLE_TRACKVSCALEFACTOR = 0x02
        private const val TRACKEDVEHICLEDEF_VARIABLE_TURNTORQUESCALEFACTOR = 0x03

        /**
         * Loads a TrackedVehicleDefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [TRACKEDVEHICLEDEF_CHUNK_VEHICLEPHYSDEF]
         *   [VEHICLEPHYSDEF_CHUNK_RIGIDBODYDEF]
         *     [RIGIDBODYDEF_CHUNK_MOVEABLEPHYSDEF]
         *       [MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF]
         *         [DYNAMICPHYSDEF_CHUNK_PHYSDEF]
         *           [PHYSDEF_CHUNK_DEFINITION] → DefinitionClass (name, id)
         *           [PHYSDEF_CHUNK_VARIABLES]  → modelName, isPreLit
         *       [MOVEABLEPHYSDEF_CHUNK_VARIABLES] → mass, gravScale, elasticity, cinematicCollisionMode
         *     [RIGIDBODYDEF_CHUNK_VARIABLES] → aerodynamicDragCoefficient, collisionDisabled
         *   [VEHICLEPHYSDEF_CHUNK_VARIABLES] → springConstant, dampingConstant, etc.
         * [TRACKEDVEHICLEDEF_CHUNK_VARIABLES] → maxEngineTorque, trackUScaleFactor, etc.
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, chunkId: UInt): TrackedVehicleDefClass {
            // Parse parent chain via VehiclePhysDefClass
            val vehiclePhysChunk = objDataChunk.findChunk(TRACKEDVEHICLEDEF_CHUNK_VEHICLEPHYSDEF)
            val parentFields = if (vehiclePhysChunk != null) {
                VehiclePhysDefClass.parseFields(vehiclePhysChunk)
            } else {
                VehiclePhysDefClass.ParsedFields(
                    "NULL", false, 1f, 1f, 0.1f, CINEMATIC_COLLISION_PUSH, 0f, false,
                    3f, 0.75f, 1f, 2f, 0f, 0f, 1f, false,
                )
            }

            // Own fields
            val vars = objDataChunk.findChunk(TRACKEDVEHICLEDEF_CHUNK_VARIABLES)
            val maxEngineTorque = vars?.let { PhysDefClass.microChunkFloat(it, TRACKEDVEHICLEDEF_VARIABLE_MAXENGINETORQUE) } ?: 0.0f
            val trackUScaleFactor = vars?.let { PhysDefClass.microChunkFloat(it, TRACKEDVEHICLEDEF_VARIABLE_TRACKUSCALEFACTOR) } ?: 25.0f
            val trackVScaleFactor = vars?.let { PhysDefClass.microChunkFloat(it, TRACKEDVEHICLEDEF_VARIABLE_TRACKVSCALEFACTOR) } ?: 0.0f
            val turnTorqueScaleFactor = vars?.let { PhysDefClass.microChunkFloat(it, TRACKEDVEHICLEDEF_VARIABLE_TURNTORQUESCALEFACTOR) } ?: 1.0f

            return TrackedVehicleDefClass(
                name = name,
                id = id,
                chunkId = chunkId,
                modelName = parentFields.modelName,
                isPreLit = parentFields.isPreLit,
                mass = parentFields.mass,
                gravScale = parentFields.gravScale,
                elasticity = parentFields.elasticity,
                cinematicCollisionMode = parentFields.cinematicCollisionMode,
                aerodynamicDragCoefficient = parentFields.aerodynamicDragCoefficient,
                collisionDisabled = parentFields.collisionDisabled,
                springConstant = parentFields.springConstant,
                dampingConstant = parentFields.dampingConstant,
                springLength = parentFields.springLength,
                tractionMultiplier = parentFields.tractionMultiplier,
                lateralMomentArm = parentFields.lateralMomentArm,
                tractiveMomentArm = parentFields.tractiveMomentArm,
                engineFlameLength = parentFields.engineFlameLength,
                isFake = parentFields.isFake,
                maxEngineTorque = maxEngineTorque,
                trackUScaleFactor = trackUScaleFactor,
                trackVScaleFactor = trackVScaleFactor,
                turnTorqueScaleFactor = turnTorqueScaleFactor,
            )
        }
    }
}
