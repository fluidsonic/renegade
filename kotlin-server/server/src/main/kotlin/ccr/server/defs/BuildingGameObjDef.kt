package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of BuildingGameObjDef (Combat/building.h).
 *
 * Inheritance chain (C++ definition classes):
 *   DefinitionClass → BaseGameObjDef → ScriptableGameObjDef →
 *   DamageableGameObjDef → BuildingGameObjDef
 *
 * Chunk layout inside OBJDATA:
 *   [207011030] Building DEF_PARENT → DamageableGameObjDef::Save
 *     [207011205] Damageable DEF_PARENT → ScriptableGameObjDef::Save
 *       [627001056] Scriptable DEF_PARENT → BaseGameObjDef::Save
 *         [1111991123] Base DEF_PARENT → DefinitionClass::Save
 *           [0x100] VARIABLES → micro 0x01=m_ID, 0x03=m_Name
 *       [627001057] Scriptable DEF_VARIABLES → micro 2=ScriptName*, 3=ScriptParams*
 *     [207011206] Damageable DEF_VARIABLES → micros 1..6
 *     [207011207] Damageable DEF_DEFENSEOBJECTDEF
 *       [7311607] DefenseObjectDef VARIABLES → micros 0x00..0x07
 *   [207011031] Building DEF_VARIABLES → micros 1..8
 *
 * This class is open so building subtypes (PowerPlant, Refinery, etc.) can extend it.
 */
open class BuildingGameObjDef(
    // DefinitionClass base
    name: String,
    id: UInt,
    chunkId: UInt,

    // ScriptableGameObjDef
    val scriptNames: List<String> = emptyList(),
    val scriptParameters: List<String> = emptyList(),

    // DamageableGameObjDef
    val translatedNameId: Int = 0,
    val infoIconTextureFilename: String = "",
    val encyclopediaType: Int = -1,      // EncyclopediaMgrClass::TYPE_UNKNOWN
    val encyclopediaId: Int = 0,
    val notTargetable: Boolean = false,
    val defaultPlayerType: Int = -2,     // PLAYERTYPE_NEUTRAL
    val defenseObjectDef: DefenseObjectDef = DefenseObjectDef(),

    // BuildingGameObjDef
    val meshPrefix: String = "",
    val mctSkin: Int = 0,
    val buildingType: Int = BUILDING_TYPE_NONE,
    val gdiDamageReportId: Int = 0,
    val nodDamageReportId: Int = 0,
    val gdiDestroyReportId: Int = 0,
    val nodDestroyReportId: Int = 0,
) : DefinitionClass(name, id, chunkId) {

    /**
     * DefenseObjectDefClass (Combat/damage.h).
     * Embedded in DamageableGameObjDef.
     */
    data class DefenseObjectDef(
        val health: Float = 100f,
        val healthMax: Float = 100f,
        val skinSaveId: Int = 0,
        val shieldStrength: Float = 0f,
        val shieldStrengthMax: Float = 0f,
        val shieldTypeSaveId: Int = 0,
        val damagePoints: Float = 0f,
        val deathPoints: Float = 0f,
    ) {
        companion object {
            private const val DEFENSEOBJECTDEF_CHUNK_VARIABLES = 7311607u
            private const val MCID_HEALTH = 0x00
            private const val MCID_HEALTH_MAX = 0x01
            private const val MCID_SKIN = 0x02
            private const val MCID_SHIELD_STRENGTH = 0x03
            private const val MCID_SHIELD_STRENGTH_MAX = 0x04
            private const val MCID_SHIELD_TYPE = 0x05
            private const val MCID_DAMAGE_POINTS = 0x06
            private const val MCID_DEATH_POINTS = 0x07

            fun load(defenseChunk: ChunkReader): DefenseObjectDef {
                val vars = defenseChunk.findChunk(DEFENSEOBJECTDEF_CHUNK_VARIABLES)
                    ?: return DefenseObjectDef()
                return DefenseObjectDef(
                    health = vars.mcFloat(MCID_HEALTH) ?: 100f,
                    healthMax = vars.mcFloat(MCID_HEALTH_MAX) ?: 100f,
                    skinSaveId = vars.mcInt(MCID_SKIN) ?: 0,
                    shieldStrength = vars.mcFloat(MCID_SHIELD_STRENGTH) ?: 0f,
                    shieldStrengthMax = vars.mcFloat(MCID_SHIELD_STRENGTH_MAX) ?: 0f,
                    shieldTypeSaveId = vars.mcInt(MCID_SHIELD_TYPE) ?: 0,
                    damagePoints = vars.mcFloat(MCID_DAMAGE_POINTS) ?: 0f,
                    deathPoints = vars.mcFloat(MCID_DEATH_POINTS) ?: 0f,
                )
            }
        }
    }

    companion object {
        /** Persist factory chunk ID for BuildingGameObjDef in the DDB. */
        const val CHUNK_ID: UInt = 0x00040134u  // CHUNKID_GAME_OBJECT_DEF_BUILDING

        // --- BuildingConstants::BuildingType (building.h) ---
        const val BUILDING_TYPE_NONE = -1
        const val BUILDING_TYPE_POWER_PLANT = 0
        const val BUILDING_TYPE_SOLDIER_FACTORY = 1
        const val BUILDING_TYPE_VEHICLE_FACTORY = 2
        const val BUILDING_TYPE_REFINERY = 3
        const val BUILDING_TYPE_COM_CENTER = 4
        const val BUILDING_TYPE_REPAIR_BAY = 5
        const val BUILDING_TYPE_SHRINE = 6
        const val BUILDING_TYPE_HELIPAD = 7
        const val BUILDING_TYPE_CONYARD = 8
        const val BUILDING_TYPE_BASE_DEFENSE = 9

        // --- PlayerType constants (playertype.h) ---
        const val PLAYERTYPE_NOD = 0
        const val PLAYERTYPE_GDI = 1
        const val PLAYERTYPE_NEUTRAL = -2

        // --- Chunk IDs (per-class, from each .cpp enum) ---

        // DefinitionClass (definition.cpp)
        private const val CHUNKID_DEF_BASE_VARIABLES = 0x00000100u
        private const val VARID_INSTANCEID = 0x01
        private const val VARID_NAME = 0x03

        // ScriptableGameObjDef (scriptablegameobj.cpp)
        private const val CHUNKID_SCRIPTABLE_DEF_VARIABLES = 627001057u
        private const val MCID_SCRIPT_NAME = 2
        private const val MCID_SCRIPT_PARAMETERS = 3

        // DamageableGameObjDef (damageablegameobj.cpp)
        private const val CHUNKID_DAMAGEABLE_DEF_VARIABLES = 207011206u
        private const val CHUNKID_DAMAGEABLE_DEF_DEFENSEOBJECTDEF = 207011207u
        private const val MCID_DMG_TRANSLATED_NAME_ID = 1
        private const val MCID_DMG_INFO_ICON_TEXTURE_FILENAME = 2
        private const val MCID_DMG_ENCY_TYPE = 3
        private const val MCID_DMG_ENCY_ID = 4
        private const val MCID_DMG_NOT_TARGETABLE = 5
        private const val MCID_DMG_DEFAULT_PLAYER_TYPE = 6

        // BuildingGameObjDef (building.cpp)
        private const val CHUNKID_BUILDING_DEF_VARIABLES = 207011031u
        private const val MCID_MESHPREFIX = 1
        private const val MCID_MCTSKIN = 2
        private const val MCID_BUILDING_TYPE = 3
        private const val MCID_LEGACY_BUILDING_TEAM = 4
        private const val MCID_GDI_DAMAGE_REPORT_ID = 5
        private const val MCID_NOD_DAMAGE_REPORT_ID = 6
        private const val MCID_GDI_DESTROY_REPORT_ID = 7
        private const val MCID_NOD_DESTROY_REPORT_ID = 8

        // Legacy building team enum (building.h BuildingConstants::LegacyBuildingTeam)
        private const val LEGACY_TEAM_GDI = 0
        private const val LEGACY_TEAM_NOD = 1

        /**
         * Loads a BuildingGameObjDef from the OBJDATA chunk of a definition entry.
         * Returns null if required base fields (id, name) are missing.
         */
        fun load(objDataChunk: ChunkReader, chunkId: UInt): BuildingGameObjDef? {
            // --- DefinitionClass base (id + name) ---
            val baseVarsChunk = objDataChunk.findChunkRecursive(CHUNKID_DEF_BASE_VARIABLES)
                ?: return null
            val idBytes = baseVarsChunk.findMicroChunk(VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val definitionId = ByteBuffer.wrap(idBytes, 0, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = baseVarsChunk.findMicroChunk(VARID_NAME) ?: return null
            val name = readNullTermString(nameBytes)

            // --- ScriptableGameObjDef (script names + params) ---
            val scriptNames = mutableListOf<String>()
            val scriptParams = mutableListOf<String>()
            val scriptVars = objDataChunk.findChunkRecursive(CHUNKID_SCRIPTABLE_DEF_VARIABLES)
            if (scriptVars != null) {
                scriptVars.forEachMicroChunk { id, data ->
                    when (id) {
                        MCID_SCRIPT_NAME -> scriptNames += readNullTermString(data)
                        MCID_SCRIPT_PARAMETERS -> scriptParams += readNullTermString(data)
                    }
                }
            }

            // --- DamageableGameObjDef ---
            val dmgVars = objDataChunk.findChunkRecursive(CHUNKID_DAMAGEABLE_DEF_VARIABLES)
            val translatedNameId = dmgVars?.mcInt(MCID_DMG_TRANSLATED_NAME_ID) ?: 0
            val infoIconTexture = dmgVars?.mcString(MCID_DMG_INFO_ICON_TEXTURE_FILENAME) ?: ""
            val encyclopediaType = dmgVars?.mcInt(MCID_DMG_ENCY_TYPE) ?: -1
            val encyclopediaId = dmgVars?.mcInt(MCID_DMG_ENCY_ID) ?: 0
            val notTargetable = dmgVars?.mcBool(MCID_DMG_NOT_TARGETABLE) ?: false
            var defaultPlayerType = dmgVars?.mcInt(MCID_DMG_DEFAULT_PLAYER_TYPE) ?: PLAYERTYPE_NEUTRAL

            // DefenseObjectDef (embedded in DamageableGameObjDef)
            val defenseChunk = objDataChunk.findChunkRecursive(CHUNKID_DAMAGEABLE_DEF_DEFENSEOBJECTDEF)
            val defenseObjectDef = if (defenseChunk != null) {
                DefenseObjectDef.load(defenseChunk)
            } else {
                DefenseObjectDef()
            }

            // --- BuildingGameObjDef ---
            val bldgVars = objDataChunk.findChunk(CHUNKID_BUILDING_DEF_VARIABLES)
            val meshPrefix = bldgVars?.mcString(MCID_MESHPREFIX) ?: ""
            val mctSkin = bldgVars?.mcInt(MCID_MCTSKIN) ?: 0
            val buildingType = bldgVars?.mcInt(MCID_BUILDING_TYPE) ?: BUILDING_TYPE_NONE
            val gdiDamageReportId = bldgVars?.mcInt(MCID_GDI_DAMAGE_REPORT_ID) ?: 0
            val nodDamageReportId = bldgVars?.mcInt(MCID_NOD_DAMAGE_REPORT_ID) ?: 0
            val gdiDestroyReportId = bldgVars?.mcInt(MCID_GDI_DESTROY_REPORT_ID) ?: 0
            val nodDestroyReportId = bldgVars?.mcInt(MCID_NOD_DESTROY_REPORT_ID) ?: 0

            // Legacy team handling: if legacy_team micro-chunk is present, override defaultPlayerType
            val legacyTeam = bldgVars?.mcInt(MCID_LEGACY_BUILDING_TEAM)
            if (legacyTeam != null && legacyTeam != -1) {
                defaultPlayerType = if (legacyTeam == LEGACY_TEAM_NOD) PLAYERTYPE_NOD else PLAYERTYPE_GDI
            }

            return BuildingGameObjDef(
                name = name,
                id = definitionId,
                chunkId = chunkId,
                scriptNames = scriptNames,
                scriptParameters = scriptParams,
                translatedNameId = translatedNameId,
                infoIconTextureFilename = infoIconTexture,
                encyclopediaType = encyclopediaType,
                encyclopediaId = encyclopediaId,
                notTargetable = notTargetable,
                defaultPlayerType = defaultPlayerType,
                defenseObjectDef = defenseObjectDef,
                meshPrefix = meshPrefix,
                mctSkin = mctSkin,
                buildingType = buildingType,
                gdiDamageReportId = gdiDamageReportId,
                nodDamageReportId = nodDamageReportId,
                gdiDestroyReportId = gdiDestroyReportId,
                nodDestroyReportId = nodDestroyReportId,
            )
        }

        private fun readNullTermString(bytes: ByteArray): String {
            val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
            val len = if (nullIdx < 0) bytes.size else nullIdx
            return String(bytes, 0, len, Charsets.ISO_8859_1)
        }
    }

    override fun toString(): String =
        "BuildingGameObjDef(id=$id, name='$name', type=$buildingType, meshPrefix='$meshPrefix')"
}

// Micro-chunk reader helpers: uses internal extensions from GlobalSettingsDefs.kt
// (mcInt, mcFloat, mcBool, mcString)
