package ccr.server.defs.phys

import ccr.server.defs.MoveablePhysDefClass
import ccr.server.defs.PhysDefClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of MotorcycleDefClass (wwphys/motorcycle.h).
 *
 * Inheritance: MotorcycleDefClass -> WheeledVehicleDefClass -> MotorVehicleDefClass ->
 *   VehiclePhysDefClass -> RigidBodyDefClass -> MoveablePhysDefClass ->
 *   DynamicPhysDefClass -> PhysDefClass -> DefinitionClass
 *
 * C++ defaults: LeanK0(18.0f), LeanK1(5.0f)
 *
 * Chunk layout inside OBJDATA:
 *   [MOTORCYCLEDEF_CHUNK_WHEELEDVEHICLEDEF = 0x00516000]  -> parent WheeledVehicleDefClass data
 *   [MOTORCYCLEDEF_CHUNK_VARIABLES         = 0x00516001]  -> LeanK0, LeanK1
 */
class MotorcycleDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    mass: Float = 1f,
    gravScale: Float = 1f,
    elasticity: Float = 0.1f,
    cinematicCollisionMode: Int = MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH,
    aerodynamicDragCoefficient: Float = 0f,
    collisionDisabled: Boolean = false,
    springConstant: Float = 3.0f,
    dampingConstant: Float = 0.75f,
    springLength: Float = 1.0f,
    tractionMultiplier: Float = 2.0f,
    lateralMomentArm: Float = 0.0f,
    tractiveMomentArm: Float = 0.0f,
    engineFlameLength: Float = 1.0f,
    isFake: Boolean = false,
    maxEngineTorque: Float = 5.0f,
    engineTorqueCurveFilename: String = "Vehicles\\PhysicsTables\\DefaultEngineTorque.tbl",
    gearCount: Int = 4,
    gearRatio0: Float = 12.01f,
    gearRatio1: Float = 7.82f,
    gearRatio2: Float = 5.16f,
    gearRatio3: Float = 3.81f,
    gearRatio4: Float = 2.79f,
    gearRatio5: Float = 1.0f,
    finalDriveGearRatio: Float = 2.92f,
    driveTrainInertia: Float = 0.1f,
    shiftUpRpm: Float = 7000f,
    shiftDownRpm: Float = 2000f,
    maxSteeringAngle: Float = 0.7853982f,
    val leanK0: Float = 18.0f,
    val leanK1: Float = 5.0f,
) : WheeledVehicleDefClass(
    name, id, chunkId, modelName, isPreLit,
    mass, gravScale, elasticity, cinematicCollisionMode,
    aerodynamicDragCoefficient, collisionDisabled,
    springConstant, dampingConstant, springLength, tractionMultiplier,
    lateralMomentArm, tractiveMomentArm, engineFlameLength, isFake,
    maxEngineTorque, engineTorqueCurveFilename,
    gearCount, gearRatio0, gearRatio1, gearRatio2, gearRatio3, gearRatio4, gearRatio5,
    finalDriveGearRatio, driveTrainInertia, shiftUpRpm, shiftDownRpm,
    maxSteeringAngle,
) {

    companion object {
        /** CLASSID_MOTORCYCLEDEF = CLASSID_PHYSICS(0x9000) + 2 */
        const val CHUNK_ID: UInt = 0x00020503u  // PHYSICS_CHUNKID_MOTORCYCLEDEF

        // Chunk IDs from motorcycle.cpp local enum
        const val CHUNK_WHEELEDVEHICLEDEF: UInt = 0x00516000u
        const val CHUNK_VARIABLES: UInt = 0x00516001u

        // Micro-chunk IDs
        const val VARIABLE_LEANK0: Int = 0x00
        const val VARIABLE_LEANK1: Int = 0x01

        /**
         * Loads a MotorcycleDefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [CHUNK_WHEELEDVEHICLEDEF]
         *   [WheeledVehicle CHUNK_MOTORVEHICLEDEF]
         *     [MotorVehicle CHUNK_VEHICLEPHYSDEF]
         *       [VehiclePhys CHUNK_RIGIDBODYDEF]
         *         ... → parent fields
         *     [MotorVehicle CHUNK_VARIABLES] → engine/gear fields
         *   [WheeledVehicle CHUNK_VARIABLES] → maxSteeringAngle
         * [CHUNK_VARIABLES] → leanK0, leanK1
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, chunkId: UInt): MotorcycleDefClass {
            // Parse parent chain via WheeledVehicleDefClass
            val wheeledVehicleChunk = objDataChunk.findChunk(CHUNK_WHEELEDVEHICLEDEF)
            val parentFields = if (wheeledVehicleChunk != null) {
                WheeledVehicleDefClass.parseFields(wheeledVehicleChunk)
            } else {
                WheeledVehicleDefClass.ParsedFields(
                    "NULL", false, 1f, 1f, 0.1f, MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH,
                    0f, false, 3f, 0.75f, 1f, 2f, 0f, 0f, 1f, false,
                    5.0f, "Vehicles\\PhysicsTables\\DefaultEngineTorque.tbl",
                    4, 12.01f, 7.82f, 5.16f, 3.81f, 2.79f, 1.0f,
                    2.92f, 0.1f, 7000f, 2000f,
                    0.7853982f,
                )
            }

            // Own fields — use findChunk (direct child only, not recursive!)
            val vars = objDataChunk.findChunk(CHUNK_VARIABLES)
            val leanK0 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_LEANK0) } ?: 18.0f
            val leanK1 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_LEANK1) } ?: 5.0f

            return MotorcycleDefClass(
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
                maxEngineTorque = parentFields.maxEngineTorque,
                engineTorqueCurveFilename = parentFields.engineTorqueCurveFilename,
                gearCount = parentFields.gearCount,
                gearRatio0 = parentFields.gearRatio0,
                gearRatio1 = parentFields.gearRatio1,
                gearRatio2 = parentFields.gearRatio2,
                gearRatio3 = parentFields.gearRatio3,
                gearRatio4 = parentFields.gearRatio4,
                gearRatio5 = parentFields.gearRatio5,
                finalDriveGearRatio = parentFields.finalDriveGearRatio,
                driveTrainInertia = parentFields.driveTrainInertia,
                shiftUpRpm = parentFields.shiftUpRpm,
                shiftDownRpm = parentFields.shiftDownRpm,
                maxSteeringAngle = parentFields.maxSteeringAngle,
                leanK0 = leanK0,
                leanK1 = leanK1,
            )
        }
    }
}
