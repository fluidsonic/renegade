package ccr.server.defs.phys

import ccr.server.defs.MoveablePhysDefClass
import ccr.server.defs.PhysDefClass
import ccr.server.defs.VehiclePhysDefClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of MotorVehicleDefClass (wwphys/motorvehicle.h).
 *
 * Inheritance: MotorVehicleDefClass -> VehiclePhysDefClass -> RigidBodyDefClass ->
 *   MoveablePhysDefClass -> DynamicPhysDefClass -> PhysDefClass -> DefinitionClass
 *
 * C++ defaults: MaxEngineTorque(5.0f), GearCount(4), FinalDriveGearRatio(2.92f),
 *   ShiftUpRpm(7000), ShiftDownRpm(2000), DriveTrainInertia(0.1f)
 *
 * Chunk layout inside OBJDATA:
 *   [MOTORVEHICLEDEF_CHUNK_RIGIDBODYDEF    = 0x00516000]  -> old parent (obsolete)
 *   [MOTORVEHICLEDEF_CHUNK_VARIABLES       = 0x00516001]  -> motor-vehicle-specific micro-chunks
 *   [MOTORVEHICLEDEF_CHUNK_VEHICLEPHYSDEF  = 0x00516002]  -> current parent VehiclePhysDefClass data
 */
open class MotorVehicleDefClass(
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
    val maxEngineTorque: Float = 5.0f,
    val engineTorqueCurveFilename: String = "Vehicles\\PhysicsTables\\DefaultEngineTorque.tbl",
    val gearCount: Int = 4,
    val gearRatio0: Float = 12.01f,
    val gearRatio1: Float = 7.82f,
    val gearRatio2: Float = 5.16f,
    val gearRatio3: Float = 3.81f,
    val gearRatio4: Float = 2.79f,
    val gearRatio5: Float = 1.0f,
    val finalDriveGearRatio: Float = 2.92f,
    val driveTrainInertia: Float = 0.1f,
    val shiftUpRpm: Float = 7000f,
    val shiftDownRpm: Float = 2000f,
) : VehiclePhysDefClass(
    name, id, chunkId, modelName, isPreLit,
    mass, gravScale, elasticity, cinematicCollisionMode,
    aerodynamicDragCoefficient, collisionDisabled,
    springConstant, dampingConstant, springLength, tractionMultiplier,
    lateralMomentArm, tractiveMomentArm, engineFlameLength, isFake,
) {

    internal data class ParsedFields(
        // VehiclePhysDefClass parent fields
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
        // MotorVehicleDefClass own fields
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
    )

    companion object {
        /** CLASSID_MOTORVEHICLEDEF = CLASSID_PHYSICS(0x9000) + 3 */
        const val CHUNK_ID: UInt = 0x00020504u  // PHYSICS_CHUNKID_MOTORVEHICLEDEF

        // Chunk IDs from motorvehicle.cpp local enum
        const val CHUNK_RIGIDBODYDEF: UInt = 0x00516000u    // old parent (obsolete)
        const val CHUNK_VARIABLES: UInt = 0x00516001u
        const val CHUNK_VEHICLEPHYSDEF: UInt = 0x00516002u  // current parent

        // Micro-chunk IDs
        const val VARIABLE_MAXENGINETORQUE: Int = 0x00
        const val VARIABLE_ENGINETORQUECURVEFILENAME: Int = 0x01
        const val VARIABLE_GEARCOUNT: Int = 0x02
        const val VARIABLE_GEARRATIO1: Int = 0x03
        const val VARIABLE_GEARRATIO2: Int = 0x04
        const val VARIABLE_GEARRATIO3: Int = 0x05
        const val VARIABLE_GEARRATIO4: Int = 0x06
        const val VARIABLE_GEARRATIO5: Int = 0x07
        const val VARIABLE_GEARRATIO6: Int = 0x08
        const val VARIABLE_FINALDRIVEGEARRATIO: Int = 0x09
        const val VARIABLE_SHIFTUPRPM: Int = 0x0D
        const val VARIABLE_SHIFTDOWNRPM: Int = 0x0E
        const val VARIABLE_DRIVETRAININERTIA: Int = 0x0F

        internal fun parseFields(motorVehicleChunk: ChunkReader): ParsedFields {
            // Try current parent first, fall back to old parent
            val vehiclePhysChunk = motorVehicleChunk.findChunk(CHUNK_VEHICLEPHYSDEF)
                ?: motorVehicleChunk.findChunk(CHUNK_RIGIDBODYDEF)
            val parentFields = if (vehiclePhysChunk != null) {
                VehiclePhysDefClass.parseFields(vehiclePhysChunk)
            } else {
                VehiclePhysDefClass.ParsedFields(
                    "NULL", false, 1f, 1f, 0.1f, MoveablePhysDefClass.CINEMATIC_COLLISION_PUSH,
                    0f, false, 3f, 0.75f, 1f, 2f, 0f, 0f, 1f, false,
                )
            }

            val vars = motorVehicleChunk.findChunk(CHUNK_VARIABLES)
            val maxEngineTorque = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_MAXENGINETORQUE) } ?: 5.0f
            val engineTorqueCurveFilename = vars?.let { PhysDefClass.microChunkString(it, VARIABLE_ENGINETORQUECURVEFILENAME) }
                ?: "Vehicles\\PhysicsTables\\DefaultEngineTorque.tbl"
            val gearCount = vars?.let { PhysDefClass.microChunkInt(it, VARIABLE_GEARCOUNT) } ?: 4
            val gearRatio0 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_GEARRATIO1) } ?: 12.01f
            val gearRatio1 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_GEARRATIO2) } ?: 7.82f
            val gearRatio2 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_GEARRATIO3) } ?: 5.16f
            val gearRatio3 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_GEARRATIO4) } ?: 3.81f
            val gearRatio4 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_GEARRATIO5) } ?: 2.79f
            val gearRatio5 = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_GEARRATIO6) } ?: 1.0f
            val finalDriveGearRatio = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_FINALDRIVEGEARRATIO) } ?: 2.92f
            val driveTrainInertia = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_DRIVETRAININERTIA) } ?: 0.1f
            val shiftUpRpm = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_SHIFTUPRPM) } ?: 7000f
            val shiftDownRpm = vars?.let { PhysDefClass.microChunkFloat(it, VARIABLE_SHIFTDOWNRPM) } ?: 2000f

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
                maxEngineTorque = maxEngineTorque,
                engineTorqueCurveFilename = engineTorqueCurveFilename,
                gearCount = gearCount,
                gearRatio0 = gearRatio0,
                gearRatio1 = gearRatio1,
                gearRatio2 = gearRatio2,
                gearRatio3 = gearRatio3,
                gearRatio4 = gearRatio4,
                gearRatio5 = gearRatio5,
                finalDriveGearRatio = finalDriveGearRatio,
                driveTrainInertia = driveTrainInertia,
                shiftUpRpm = shiftUpRpm,
                shiftDownRpm = shiftDownRpm,
            )
        }
    }
}
