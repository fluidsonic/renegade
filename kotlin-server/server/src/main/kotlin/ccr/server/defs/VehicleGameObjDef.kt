package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vehicle type enum matching C++ VehicleType in vehicle.h.
 */
enum class VehicleType(val value: Int) {
    CAR(0),
    TANK(1),
    BIKE(2),
    FLYING(3),
    TURRET(4);

    companion object {
        fun fromValue(value: Int): VehicleType = entries.firstOrNull { it.value == value } ?: CAR
    }
}

/**
 * Parsed transition definition from TransitionDataClass (transition.h/cpp).
 *
 * Each transition defines a trigger zone, animation, and end transform for
 * soldier enter/exit behavior on vehicles and ladders.
 */
data class TransitionData(
    /** TransitionDataClass::StyleType enum value. */
    val type: Int,
    /** OBBoxClass: center(3f) + extent(3f) + basis(9f) = 15 floats. Null if data was missing/short. */
    val zone: FloatArray?,
    /** Animation to play during the transition. */
    val animationName: String,
    /** Matrix3D: 12 floats (3 rows x 4 columns). Null if data was missing/short. */
    val endingTM: FloatArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransitionData) return false
        return type == other.type &&
            animationName == other.animationName &&
            (zone contentEqualsNullable other.zone) &&
            (endingTM contentEqualsNullable other.endingTM)
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + animationName.hashCode()
        result = 31 * result + (zone?.contentHashCode() ?: 0)
        result = 31 * result + (endingTM?.contentHashCode() ?: 0)
        return result
    }
}

private infix fun FloatArray?.contentEqualsNullable(other: FloatArray?): Boolean =
    if (this == null) other == null else other != null && contentEquals(other)

// ---------------------------------------------------------------------------
// VehicleGameObjDef — top-level definition for vehicle presets
//
// C++ hierarchy: VehicleGameObjDef : SmartGameObjDef : ArmedGameObjDef :
//   PhysicalGameObjDef : DamageableGameObjDef : ScriptableGameObjDef :
//   BaseGameObjDef : DefinitionClass
//
// Chunk IDs (VehicleGameObjDef local enum starting at 930991656):
//   CHUNKID_DEF_PARENT       = 930991656  -> SmartGameObjDef::Save
//   CHUNKID_DEF_VARIABLES    = 930991657  -> vehicle-specific micro-chunks
//   CHUNKID_DEF_TRANSITION   = 930991658  -> TransitionDataClass (0..n)
// ---------------------------------------------------------------------------
data class VehicleGameObjDef(
    // DefinitionClass
    val definition: DefinitionData,

    // ScriptableGameObjDef
    val scriptable: ScriptableGameObjDefData,

    // DamageableGameObjDef
    val damageable: DamageableGameObjDefData,

    // PhysicalGameObjDef
    val physical: PhysicalGameObjDefData,

    // ArmedGameObjDef
    val armed: ArmedGameObjDefData,

    // SmartGameObjDef
    val smart: SmartGameObjDefData,

    // VehicleGameObjDef own fields
    val type: VehicleType = VehicleType.CAR,
    val typeName: String = "",
    val fire0Anim: String = "",
    val fire1Anim: String = "",
    val profile: String = "",
    val turnRadius: Float = 10.0f,
    val occupantsVisible: Boolean = true,
    val engineSoundMaxPitchFactor: Float = 2.0f,
    val engineStartSound: Int = 0,
    val engineRunSound: Int = 0,
    val engineStopSound: Int = 0,
    val engineOffSound: Int = 0,
    val sightDownMuzzle: Boolean = false,
    val aim2D: Boolean = true,
    val squishVelocity: Float = 1.5f,
    val vehicleNameId: Int = 0,
    val numSeats: Int = 2,
    val gdiDamageReportId: Int = 0,
    val nodDamageReportId: Int = 0,
    val gdiDestroyReportId: Int = 0,
    val nodDestroyReportId: Int = 0,
    val transitions: List<TransitionData> = emptyList(),
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040129u  // CHUNKID_GAME_OBJECT_DEF_VEHICLE

        // VehicleGameObjDef chunk IDs (enum starts at 930991656)
        private const val CHUNKID_DEF_PARENT = 930991656u
        private const val CHUNKID_DEF_VARIABLES = 930991657u
        private const val CHUNKID_DEF_TRANSITION = 930991658u

        // Micro-chunk IDs inside CHUNKID_DEF_VARIABLES
        private const val MICROCHUNKID_TYPE = 1                            // int (VehicleType)
        private const val MICROCHUNKID_TYPE_NAME = 2                       // string
        private const val MICROCHUNKID_FIRE0ANIM = 3                       // string
        private const val MICROCHUNKID_FIRE1ANIM = 4                       // string
        private const val MICROCHUNKID_PROFILE = 5                         // string
        private const val MICROCHUNKID_PHYS_ID = 18                        // int (backward compat)
        private const val MICROCHUNKID_TURN_RADIUS = 19                    // float
        private const val MICROCHUNKID_OCCUPANTS_VISIBLE = 20              // bool
        private const val MICROCHUNKID_ENGINE_START_SOUND = 23             // int
        private const val MICROCHUNKID_ENGINE_RUN_SOUND = 24               // int
        private const val MICROCHUNKID_ENGINE_STOP_SOUND = 25              // int
        private const val MICROCHUNKID_ENGINE_OFF_SOUND = 26               // int
        private const val MICROCHUNKID_DEF_SIGHT_DOWN_MUZZLE = 27          // bool
        private const val MICROCHUNKID_DEF_AIM_2D = 28                     // bool
        private const val MICROCHUNKID_DEF_SQUISH_VELOCITY = 29            // float
        private const val MICROCHUNKID_ENGINE_SOUND_MAX_PITCH_FACTOR = 30  // float
        private const val MICROCHUNKID_DEF_VEHICLE_NAME_ID = 31            // int
        private const val MICROCHUNKID_DEF_NUM_SEATS = 32                  // int
        private const val MICROCHUNKID_DEF_GDI_DAMAGE_REPORT_ID = 33       // int
        private const val MICROCHUNKID_DEF_NOD_DAMAGE_REPORT_ID = 34       // int
        private const val MICROCHUNKID_DEF_GDI_DESTROY_REPORT_ID = 35      // int
        private const val MICROCHUNKID_DEF_NOD_DESTROY_REPORT_ID = 36      // int

        /**
         * Loads a VehicleGameObjDef from the OBJDATA chunk for a vehicle definition.
         * [objDataChunk] should be the ChunkReader for the SIMPLEFACTORY_CHUNKID_OBJDATA content.
         */
        fun load(objDataChunk: ChunkReader): VehicleGameObjDef? {
            // --- VehicleGameObjDef layer ---
            var smartParentChunk: ChunkReader? = null
            var type = 0
            var typeName = ""
            var fire0Anim = ""
            var fire1Anim = ""
            var profile = ""
            var turnRadius = 10.0f
            var occupantsVisible = true
            var engineSoundMaxPitchFactor = 2.0f
            var engineStartSound = 0
            var engineRunSound = 0
            var engineStopSound = 0
            var engineOffSound = 0
            var sightDownMuzzle = false
            var aim2D = true
            var squishVelocity = 1.5f
            var vehicleNameId = 0
            var numSeats = 2
            var gdiDamageReportId = 0
            var nodDamageReportId = 0
            var gdiDestroyReportId = 0
            var nodDestroyReportId = 0
            val transitions = mutableListOf<TransitionData>()

            objDataChunk.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> smartParentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                MICROCHUNKID_TYPE -> type = microData.toLeInt()
                                MICROCHUNKID_TYPE_NAME -> typeName = microData.toNullTerminatedString()
                                MICROCHUNKID_FIRE0ANIM -> fire0Anim = microData.toNullTerminatedString()
                                MICROCHUNKID_FIRE1ANIM -> fire1Anim = microData.toNullTerminatedString()
                                MICROCHUNKID_PROFILE -> profile = microData.toNullTerminatedString()
                                MICROCHUNKID_PHYS_ID -> { /* backward compat, physDefId is in PhysicalGameObjDef */ }
                                MICROCHUNKID_TURN_RADIUS -> turnRadius = microData.toLeFloat()
                                MICROCHUNKID_OCCUPANTS_VISIBLE -> occupantsVisible = microData.toBool()
                                MICROCHUNKID_ENGINE_SOUND_MAX_PITCH_FACTOR -> engineSoundMaxPitchFactor = microData.toLeFloat()
                                MICROCHUNKID_ENGINE_START_SOUND -> engineStartSound = microData.toLeInt()
                                MICROCHUNKID_ENGINE_RUN_SOUND -> engineRunSound = microData.toLeInt()
                                MICROCHUNKID_ENGINE_STOP_SOUND -> engineStopSound = microData.toLeInt()
                                MICROCHUNKID_ENGINE_OFF_SOUND -> engineOffSound = microData.toLeInt()
                                MICROCHUNKID_DEF_SIGHT_DOWN_MUZZLE -> sightDownMuzzle = microData.toBool()
                                MICROCHUNKID_DEF_AIM_2D -> aim2D = microData.toBool()
                                MICROCHUNKID_DEF_SQUISH_VELOCITY -> squishVelocity = microData.toLeFloat()
                                MICROCHUNKID_DEF_VEHICLE_NAME_ID -> vehicleNameId = microData.toLeInt()
                                MICROCHUNKID_DEF_NUM_SEATS -> numSeats = microData.toLeInt()
                                MICROCHUNKID_DEF_GDI_DAMAGE_REPORT_ID -> gdiDamageReportId = microData.toLeInt()
                                MICROCHUNKID_DEF_NOD_DAMAGE_REPORT_ID -> nodDamageReportId = microData.toLeInt()
                                MICROCHUNKID_DEF_GDI_DESTROY_REPORT_ID -> gdiDestroyReportId = microData.toLeInt()
                                MICROCHUNKID_DEF_NOD_DESTROY_REPORT_ID -> nodDestroyReportId = microData.toLeInt()
                            }
                        }
                    }
                    CHUNKID_DEF_TRANSITION -> {
                        loadTransitionData(child)?.let { transitions += it }
                    }
                }
            }

            // --- SmartGameObjDef layer ---
            val smartChunk = smartParentChunk ?: return null
            val (smartData, armedParentChunk) = SmartGameObjDefData.load(smartChunk) ?: return null

            // --- ArmedGameObjDef layer ---
            val armedChunk = armedParentChunk ?: return null
            val (armedData, physParentChunk) = ArmedGameObjDefData.load(armedChunk) ?: return null

            // --- PhysicalGameObjDef layer ---
            val physChunk = physParentChunk ?: return null
            val (physData, damageableParentChunk) = PhysicalGameObjDefData.load(physChunk) ?: return null

            // --- DamageableGameObjDef layer ---
            val damageableChunk = damageableParentChunk ?: return null
            val (damageableData, scriptableParentChunk) = DamageableGameObjDefData.load(damageableChunk) ?: return null

            // --- ScriptableGameObjDef layer ---
            val scriptableChunk = scriptableParentChunk ?: return null
            val (scriptableData, baseParentChunk) = ScriptableGameObjDefData.load(scriptableChunk) ?: return null

            // --- BaseGameObjDef layer (wraps DefinitionClass) ---
            val baseChunk = baseParentChunk ?: return null
            val definitionChunk = baseChunk.findChunk(BASEGAMEOBJ_CHUNKID_DEF_PARENT_V) ?: return null

            // --- DefinitionClass layer ---
            val definitionData = DefinitionData.load(definitionChunk) ?: return null

            return VehicleGameObjDef(
                definition = definitionData,
                scriptable = scriptableData,
                damageable = damageableData,
                physical = physData,
                armed = armedData,
                smart = smartData,
                type = VehicleType.fromValue(type),
                typeName = typeName,
                fire0Anim = fire0Anim,
                fire1Anim = fire1Anim,
                profile = profile,
                turnRadius = turnRadius,
                occupantsVisible = occupantsVisible,
                engineSoundMaxPitchFactor = engineSoundMaxPitchFactor,
                engineStartSound = engineStartSound,
                engineRunSound = engineRunSound,
                engineStopSound = engineStopSound,
                engineOffSound = engineOffSound,
                sightDownMuzzle = sightDownMuzzle,
                aim2D = aim2D,
                squishVelocity = squishVelocity,
                vehicleNameId = vehicleNameId,
                numSeats = numSeats,
                gdiDamageReportId = gdiDamageReportId,
                nodDamageReportId = nodDamageReportId,
                gdiDestroyReportId = gdiDestroyReportId,
                nodDestroyReportId = nodDestroyReportId,
                transitions = transitions,
            )
        }

        /**
         * Parses a TransitionDataClass from a CHUNKID_DEF_TRANSITION chunk reader.
         *
         * TransitionDataClass::Save writes:
         *   [CHUNKID_VARIABLES = 0x11051106]
         *     micro[1] = Type (int, StyleType enum)
         *     micro[2] = Zone (OBBoxClass, 60 bytes: center 3f + extent 3f + basis 9f)
         *     micro[3] = AnimationName (string)
         *     micro[4] = EndingTM (Matrix3D, 48 bytes: 12 floats)
         */
        private fun loadTransitionData(reader: ChunkReader): TransitionData? {
            val TRANSITION_CHUNKID_VARIABLES = 0x11051106u
            val varsReader = reader.findChunk(TRANSITION_CHUNKID_VARIABLES) ?: return null

            var type = 0
            var zone: FloatArray? = null
            var animationName = ""
            var endingTM: FloatArray? = null

            varsReader.forEachMicroChunk { microId, microData ->
                when (microId) {
                    1 -> type = microData.toLeInt()
                    2 -> zone = microData.toLeFloatArray(15) // OBBoxClass: 15 floats
                    3 -> animationName = microData.toNullTerminatedString()
                    4 -> endingTM = microData.toLeFloatArray(12) // Matrix3D: 12 floats
                }
            }

            return TransitionData(
                type = type,
                zone = zone,
                animationName = animationName,
                endingTM = endingTM,
            )
        }
    }
}

// --- Helpers (duplicated from SoldierGameObjDef.kt since they are file-private there) ---

private const val BASEGAMEOBJ_CHUNKID_DEF_PARENT_V = 1111991123u

private fun ByteArray.toLeInt(): Int =
    ByteBuffer.wrap(this, 0, 4.coerceAtMost(size)).order(ByteOrder.LITTLE_ENDIAN).int

private fun ByteArray.toLeFloat(): Float =
    ByteBuffer.wrap(this, 0, 4.coerceAtMost(size)).order(ByteOrder.LITTLE_ENDIAN).float

private fun ByteArray.toBool(): Boolean = isNotEmpty() && this[0] != 0.toByte()

private fun ByteArray.toNullTerminatedString(): String {
    val nullIndex = indexOfFirst { it == 0.toByte() }
    val len = if (nullIndex < 0) size else nullIndex
    return String(this, 0, len, Charsets.ISO_8859_1)
}

private fun ByteArray.toLeFloatArray(count: Int): FloatArray? {
    val needed = count * 4
    if (size < needed) return null
    val buf = ByteBuffer.wrap(this, 0, needed).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(count) { buf.float }
}
