package ccr.server.defs.phys

import ccr.server.defs.MoveablePhysDefClass
import ccr.server.defs.PhysDefClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of WheeledVehicleDefClass (wwphys/wheelvehicle.h).
 *
 * Inheritance: WheeledVehicleDefClass -> MotorVehicleDefClass -> VehiclePhysDefClass ->
 *   RigidBodyDefClass -> MoveablePhysDefClass -> DynamicPhysDefClass ->
 *   PhysDefClass -> DefinitionClass
 *
 * C++ defaults: MaxSteeringAngle(PI/4 = 0.7853982f)
 *
 * Chunk layout inside OBJDATA:
 *   [WHEELEDVEHICLEDEF_CHUNK_MOTORVEHICLEDEF = 0x00990066]  -> parent MotorVehicleDefClass data
 *   [WHEELEDVEHICLEDEF_CHUNK_VARIABLES       = 0x00990067]  -> MaxSteeringAngle
 */
open class WheeledVehicleDefClass(
    name: String,
    id: UInt,
    classId: UInt,
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
    val maxSteeringAngle: Float = 0.7853982f, // PI/4
) : MotorVehicleDefClass(
    name, id, classId, modelName, isPreLit,
    mass, gravScale, elasticity, cinematicCollisionMode,
    aerodynamicDragCoefficient, collisionDisabled,
    springConstant, dampingConstant, springLength, tractionMultiplier,
    lateralMomentArm, tractiveMomentArm, engineFlameLength, isFake,
    maxEngineTorque, engineTorqueCurveFilename,
    gearCount, gearRatio0, gearRatio1, gearRatio2, gearRatio3, gearRatio4, gearRatio5,
    finalDriveGearRatio, driveTrainInertia, shiftUpRpm, shiftDownRpm,
) {

    internal data class ParsedFields(
        // MotorVehicleDefClass parent fields
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
        val maxEngineTorque: Float,
        val engineTorqueCurveFilename: String,
        val gearCount: Int,
        val gearRatio0: Float,
        val gearRatio1: Float,
        val gearRatio2: Float,
        val gearRatio3: Float,
        val gearRatio4: Float,
        val gearRatio5: Float,
        val finalDriveGearRatio: Float,
        val driveTrainInertia: Float,
        val shiftUpRpm: Float,
        val shiftDownRpm: Float,
        // WheeledVehicleDefClass own fields
        val maxSteeringAngle: Float,
    )

    companion object {
        /** CLASSID_WHEELEDVEHICLEDEF = CLASSID_PHYSICS(0x9000) + 6 */
        const val CLASS_ID: UInt = 0x9006u

        // Chunk IDs from wheelvehicle.cpp local enum
        const val CHUNK_MOTORVEHICLEDEF: UInt = 0x00990066u
        const val CHUNK_VARIABLES: UInt = 0x00990067u

        // Micro-chunk IDs
        const val VARIABLE_MAXSTEERINGANGLE: Int = 0x00

        internal fun parseFields(wheeledVehicleChunk: ChunkReader): ParsedFields {
            val motorVehicleChunk = wheeledVehicleChunk.findChunk(CHUNK_MOTORVEHICLEDEF)
            val parentFields = if (motorVehicleChunk != null) {
                MotorVehicleDefClass.parseFields(motorVehicleChunk)
            } else {
                MotorVehicleDefClass.ParsedFields(
                    "NULL", false, 1f, 1f, 0.1f, MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH,
                    0f, false, 3f, 0.75f, 1f, 2f, 0f, 0f, 1f, false,
                    5.0f, "Vehicles\\PhysicsTables\\DefaultEngineTorque.tbl",
                    4, 12.01f, 7.82f, 5.16f, 3.81f, 2.79f, 1.0f,
                    2.92f, 0.1f, 7000f, 2000f,
                )
            }

            val vars = wheeledVehicleChunk.findChunk(CHUNK_VARIABLES)
            val maxSteeringAngle = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_MAXSTEERINGANGLE) }
                ?: 0.7853982f // PI/4

            return ParsedFields(
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
                maxSteeringAngle = maxSteeringAngle,
            )
        }

        /**
         * Loads a WheeledVehicleDefClass from the OBJDATA chunk.
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, classId: UInt): WheeledVehicleDefClass {
            val fields = parseFields(objDataChunk)

            return WheeledVehicleDefClass(
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
                springConstant = fields.springConstant,
                dampingConstant = fields.dampingConstant,
                springLength = fields.springLength,
                tractionMultiplier = fields.tractionMultiplier,
                lateralMomentArm = fields.lateralMomentArm,
                tractiveMomentArm = fields.tractiveMomentArm,
                engineFlameLength = fields.engineFlameLength,
                isFake = fields.isFake,
                maxEngineTorque = fields.maxEngineTorque,
                engineTorqueCurveFilename = fields.engineTorqueCurveFilename,
                gearCount = fields.gearCount,
                gearRatio0 = fields.gearRatio0,
                gearRatio1 = fields.gearRatio1,
                gearRatio2 = fields.gearRatio2,
                gearRatio3 = fields.gearRatio3,
                gearRatio4 = fields.gearRatio4,
                gearRatio5 = fields.gearRatio5,
                finalDriveGearRatio = fields.finalDriveGearRatio,
                driveTrainInertia = fields.driveTrainInertia,
                shiftUpRpm = fields.shiftUpRpm,
                shiftDownRpm = fields.shiftDownRpm,
                maxSteeringAngle = fields.maxSteeringAngle,
            )
        }
    }
}
