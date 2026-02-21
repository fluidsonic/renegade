package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val DEG_TO_RAD: Float = (Math.PI / 180.0).toFloat()

/**
 * Kotlin representation of VTOLVehicleDefClass (wwphys/vtolvehicle.h).
 * Inherits from VehiclePhysDefClass.
 *
 * Chunk layout inside OBJDATA:
 *   [VTOLVEHICLEDEF_CHUNK_VEHICLEPHYSDEF = 408000936]  → parent chain
 *   [VTOLVEHICLEDEF_CHUNK_VARIABLES = 408000937]        → VTOL-specific micro-chunks (all floats)
 */
class VTOLVehicleDefClass(
    name: String,
    id: UInt,
    classId: UInt,
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
    // VTOL-specific fields — engine thrust (units of acceleration)
    val maxVerticalAcceleration: Float = 0f,
    val maxHorizontalAcceleration: Float = 0f,
    // Vehicle behavior controls (angles stored in radians)
    val maxFuselagePitch: Float = 15f * DEG_TO_RAD,
    val maxFuselageRoll: Float = 20f * DEG_TO_RAD,
    val pitchControllerGain: Float = 45.5f,
    val pitchControllerDamping: Float = 12.75f,
    val rollControllerGain: Float = 45.5f,
    val rollControllerDamping: Float = 12.75f,
    val maxYawVelocity: Float = 180f * DEG_TO_RAD,
    val yawControllerGain: Float = 5f,
    // Engine graphical behavior
    val maxEngineRotation: Float = 25f * DEG_TO_RAD,
    // Rotor graphical behavior
    val rotorSpeed: Float = 360f * DEG_TO_RAD,
    val rotorAcceleration: Float = 180f * DEG_TO_RAD,
    val rotorDeceleration: Float = 180f * DEG_TO_RAD,
) : VehiclePhysDefClass(
    name, id, classId, modelName, isPreLit, mass, gravScale, elasticity,
    cinematicCollisionMode, aerodynamicDragCoefficient, collisionDisabled,
    springConstant, dampingConstant, springLength, tractionMultiplier,
    lateralMomentArm, tractiveMomentArm, engineFlameLength, isFake,
) {

    companion object {
        /** CLASSID_VTOLVEHICLEDEF = CLASSID_PHYSICS + 13 = 0x900D */
        const val CLASS_ID: UInt = 0x900Du

        // Chunk IDs from vtolvehicle.cpp local enum
        private const val VTOLVEHICLEDEF_CHUNK_VEHICLEPHYSDEF = 408000936u
        private const val VTOLVEHICLEDEF_CHUNK_VARIABLES = 408000937u

        // VTOL micro-chunk IDs (from vtolvehicle.cpp enum, starting at 0x00)
        private const val MCID_MAX_VERTICAL_ACCELERATION = 0x00
        private const val MCID_MAX_HORIZONTAL_ACCELERATION = 0x01
        private const val MCID_MAX_FUSELAGE_PITCH = 0x02
        private const val MCID_MAX_FUSELAGE_ROLL = 0x03
        private const val MCID_PITCH_CONTROLLER_GAIN = 0x04
        private const val MCID_PITCH_CONTROLLER_DAMPING = 0x05
        private const val MCID_ROLL_CONTROLLER_GAIN = 0x06
        private const val MCID_ROLL_CONTROLLER_DAMPING = 0x07
        private const val MCID_MAX_YAW_VELOCITY = 0x08
        private const val MCID_YAW_CONTROLLER_GAIN = 0x09
        private const val MCID_MAX_ENGINE_ROTATION = 0x0A
        private const val MCID_ROTOR_SPEED = 0x0B
        private const val MCID_ROTOR_ACCELERATION = 0x0C
        private const val MCID_ROTOR_DECELERATION = 0x0D

        /**
         * Loads a VTOLVehicleDefClass from the OBJDATA chunk.
         *
         * Chunk nesting:
         * ```
         * [VTOLVEHICLEDEF_CHUNK_VEHICLEPHYSDEF]
         *   [VEHICLEPHYSDEF_CHUNK_RIGIDBODYDEF]
         *     [RIGIDBODYDEF_CHUNK_MOVEABLEPHYSDEF]
         *       [MOVEABLEPHYSDEF_CHUNK_DYNAMICPHYSDEF]
         *         [DYNAMICPHYSDEF_CHUNK_PHYSDEF]
         *           [PHYSDEF_CHUNK_DEFINITION] → DefinitionClass
         *           [PHYSDEF_CHUNK_VARIABLES]  → modelName, isPreLit
         *       [MOVEABLEPHYSDEF_CHUNK_VARIABLES] → mass, gravScale, etc.
         *     [RIGIDBODYDEF_CHUNK_VARIABLES] → aerodynamicDragCoefficient, collisionDisabled
         *   [VEHICLEPHYSDEF_CHUNK_VARIABLES] → springConstant, dampingConstant, etc.
         * [VTOLVEHICLEDEF_CHUNK_VARIABLES] → VTOL-specific floats
         * ```
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, classId: UInt): VTOLVehicleDefClass? {
            // Parse parent VehiclePhysDefClass fields
            val vehiclePhysChunk = objDataChunk.findChunk(VTOLVEHICLEDEF_CHUNK_VEHICLEPHYSDEF)
                ?: return null
            val parentFields = VehiclePhysDefClass.parseFields(vehiclePhysChunk)

            // Parse VTOL-specific fields
            val varsChunk = objDataChunk.findChunk(VTOLVEHICLEDEF_CHUNK_VARIABLES)

            var maxVerticalAcceleration = 0f
            var maxHorizontalAcceleration = 0f
            var maxFuselagePitch = 15f * DEG_TO_RAD
            var maxFuselageRoll = 20f * DEG_TO_RAD
            var pitchControllerGain = 45.5f
            var pitchControllerDamping = 12.75f
            var rollControllerGain = 45.5f
            var rollControllerDamping = 12.75f
            var maxYawVelocity = 180f * DEG_TO_RAD
            var yawControllerGain = 5f
            var maxEngineRotation = 25f * DEG_TO_RAD
            var rotorSpeed = 360f * DEG_TO_RAD
            var rotorAcceleration = 180f * DEG_TO_RAD
            var rotorDeceleration = 180f * DEG_TO_RAD

            varsChunk?.forEachMicroChunk { microId, data ->
                if (data.size < 4) return@forEachMicroChunk
                val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                when (microId) {
                    MCID_MAX_VERTICAL_ACCELERATION -> maxVerticalAcceleration = bb.float
                    MCID_MAX_HORIZONTAL_ACCELERATION -> maxHorizontalAcceleration = bb.float
                    MCID_MAX_FUSELAGE_PITCH -> maxFuselagePitch = bb.float
                    MCID_MAX_FUSELAGE_ROLL -> maxFuselageRoll = bb.float
                    MCID_PITCH_CONTROLLER_GAIN -> pitchControllerGain = bb.float
                    MCID_PITCH_CONTROLLER_DAMPING -> pitchControllerDamping = bb.float
                    MCID_ROLL_CONTROLLER_GAIN -> rollControllerGain = bb.float
                    MCID_ROLL_CONTROLLER_DAMPING -> rollControllerDamping = bb.float
                    MCID_MAX_YAW_VELOCITY -> maxYawVelocity = bb.float
                    MCID_YAW_CONTROLLER_GAIN -> yawControllerGain = bb.float
                    MCID_MAX_ENGINE_ROTATION -> maxEngineRotation = bb.float
                    MCID_ROTOR_SPEED -> rotorSpeed = bb.float
                    MCID_ROTOR_ACCELERATION -> rotorAcceleration = bb.float
                    MCID_ROTOR_DECELERATION -> rotorDeceleration = bb.float
                }
            }

            return VTOLVehicleDefClass(
                name = name,
                id = id,
                classId = classId,
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
                maxVerticalAcceleration = maxVerticalAcceleration,
                maxHorizontalAcceleration = maxHorizontalAcceleration,
                maxFuselagePitch = maxFuselagePitch,
                maxFuselageRoll = maxFuselageRoll,
                pitchControllerGain = pitchControllerGain,
                pitchControllerDamping = pitchControllerDamping,
                rollControllerGain = rollControllerGain,
                rollControllerDamping = rollControllerDamping,
                maxYawVelocity = maxYawVelocity,
                yawControllerGain = yawControllerGain,
                maxEngineRotation = maxEngineRotation,
                rotorSpeed = rotorSpeed,
                rotorAcceleration = rotorAcceleration,
                rotorDeceleration = rotorDeceleration,
            )
        }
    }
}
