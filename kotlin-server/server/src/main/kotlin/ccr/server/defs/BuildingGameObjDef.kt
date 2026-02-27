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

    // DamageableGameObjDef (via super)
    defenseObjectDef: DefenseObjectDefClass = DefenseObjectDefClass(),
    infoIconTextureFilename: String = "",
    translatedNameId: Int = 0,
    notTargetable: Boolean = false,
    defaultPlayerType: Int = PLAYERTYPE_NEUTRAL,
    encyclopediaType: Int = 0,
    encyclopediaId: Int = 0,

    // ScriptableGameObjDef (forwarded through DamageableGameObjDef)
    scriptNameList: List<String> = emptyList(),
    scriptParameterList: List<String> = emptyList(),

    // BuildingGameObjDef own fields
    val meshPrefix: String = "",                                      // C++: StringClass MeshPrefix
    val mctSkin: Int = 0,                                             // C++: ArmorType MCTSkin
    val buildingType: Int = BUILDING_TYPE_NONE,                       // C++: BuildingConstants::BuildingType Type
    val gdiDamageReportId: Int = 0,                                   // C++: int GDIDamageReportID
    val nodDamageReportId: Int = 0,                                   // C++: int NodDamageReportID
    val gdiDestroyReportId: Int = 0,                                  // C++: int GDIDestroyReportID
    val nodDestroyReportId: Int = 0,                                  // C++: int NodDestroyReportID
) : DamageableGameObjDef(
    name = name,
    id = id,
    chunkId = chunkId,
    defenseObjectDef = defenseObjectDef,
    infoIconTextureFilename = infoIconTextureFilename,
    translatedNameId = translatedNameId,
    notTargetable = notTargetable,
    defaultPlayerType = defaultPlayerType,
    encyclopediaType = encyclopediaType,
    encyclopediaId = encyclopediaId,
    scriptNameList = scriptNameList,
    scriptParameterList = scriptParameterList,
) {

    // C++: int Get_Damage_Report(int team) const
    fun getDamageReport(team: Int): Int = when (team) {
        PLAYERTYPE_GDI -> gdiDamageReportId
        PLAYERTYPE_NOD -> nodDamageReportId
        else           -> 0
    }

    // C++: int Get_Destroy_Report(int team) const
    fun getDestroyReport(team: Int): Int = when (team) {
        PLAYERTYPE_GDI -> gdiDestroyReportId
        PLAYERTYPE_NOD -> nodDestroyReportId
        else           -> 0
    }

    companion object {
        /** Persist factory chunk ID for BuildingGameObjDef in the DDB. */
        const val CHUNK_ID: UInt = 0x00040134u  // CHUNKID_GAME_OBJECT_DEF_BUILDING

        // --- BuildingConstants::BuildingType (building.h) ---
        const val BUILDING_TYPE_NONE           = -1
        const val BUILDING_TYPE_POWER_PLANT    = 0
        const val BUILDING_TYPE_SOLDIER_FACTORY = 1
        const val BUILDING_TYPE_VEHICLE_FACTORY = 2
        const val BUILDING_TYPE_REFINERY       = 3
        const val BUILDING_TYPE_COM_CENTER     = 4
        const val BUILDING_TYPE_REPAIR_BAY     = 5
        const val BUILDING_TYPE_SHRINE         = 6
        const val BUILDING_TYPE_HELIPAD        = 7
        const val BUILDING_TYPE_CONYARD        = 8
        const val BUILDING_TYPE_BASE_DEFENSE   = 9

        // --- PlayerType constants (playertype.h) ---
        const val PLAYERTYPE_NOD     = 0
        const val PLAYERTYPE_GDI     = 1
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

        // DefenseObjectDef (damage.cpp)
        private const val DEFENSEOBJECTDEF_CHUNK_VARIABLES = 7311607u
        private const val MCID_DEF_HEALTH = 0x00
        private const val MCID_DEF_HEALTH_MAX = 0x01
        private const val MCID_DEF_SKIN = 0x02
        private const val MCID_DEF_SHIELD_STRENGTH = 0x03
        private const val MCID_DEF_SHIELD_STRENGTH_MAX = 0x04
        private const val MCID_DEF_SHIELD_TYPE = 0x05
        private const val MCID_DEF_DAMAGE_POINTS = 0x06
        private const val MCID_DEF_DEATH_POINTS = 0x07

        // BuildingGameObjDef (building.cpp)
        // enum { CHUNKID_DEF_PARENT = 207011030, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_BUILDING_DEF_PARENT    = 207011030u
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

            // --- DamageableGameObjDef ---
            val dmgVars = objDataChunk.findChunkRecursive(CHUNKID_DAMAGEABLE_DEF_VARIABLES)
            val translatedNameId = dmgVars?.mcInt(MCID_DMG_TRANSLATED_NAME_ID) ?: 0
            val infoIconTexture = dmgVars?.mcString(MCID_DMG_INFO_ICON_TEXTURE_FILENAME) ?: ""
            val encyclopediaType = dmgVars?.mcInt(MCID_DMG_ENCY_TYPE) ?: 0
            val encyclopediaId = dmgVars?.mcInt(MCID_DMG_ENCY_ID) ?: 0
            val notTargetable = dmgVars?.mcBool(MCID_DMG_NOT_TARGETABLE) ?: false
            var defaultPlayerType = dmgVars?.mcInt(MCID_DMG_DEFAULT_PLAYER_TYPE) ?: PLAYERTYPE_NEUTRAL

            // DefenseObjectDef (embedded in DamageableGameObjDef)
            val defenseChunk = objDataChunk.findChunkRecursive(CHUNKID_DAMAGEABLE_DEF_DEFENSEOBJECTDEF)
            val defenseObjectDef = if (defenseChunk != null) {
                loadDefenseObjectDef(defenseChunk)
            } else {
                DefenseObjectDefClass()
            }

            // --- ScriptableGameObjDef ---
            val scriptVars = objDataChunk.findChunkRecursive(CHUNKID_SCRIPTABLE_DEF_VARIABLES)
            val scriptNames = mutableListOf<String>()
            val scriptParams = mutableListOf<String>()
            if (scriptVars != null) {
                var pendingName: String? = null
                scriptVars.forEachMicroChunk { id, bytes ->
                    when (id) {
                        MCID_SCRIPT_NAME -> {
                            pendingName = readNullTermString(bytes)
                        }
                        MCID_SCRIPT_PARAMETERS -> {
                            scriptNames.add(pendingName ?: "")
                            scriptParams.add(readNullTermString(bytes))
                            pendingName = null
                        }
                    }
                }
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
            // C++: if (legacy_team != -1) DefaultPlayerType = PLAYERTYPE_GDI/NOD
            val legacyTeam = bldgVars?.mcInt(MCID_LEGACY_BUILDING_TEAM)
            if (legacyTeam != null && legacyTeam != -1) {
                defaultPlayerType = if (legacyTeam == LEGACY_TEAM_NOD) PLAYERTYPE_NOD else PLAYERTYPE_GDI
            }

            return BuildingGameObjDef(
                name = name,
                id = definitionId,
                chunkId = chunkId,
                defenseObjectDef = defenseObjectDef,
                infoIconTextureFilename = infoIconTexture,
                translatedNameId = translatedNameId,
                notTargetable = notTargetable,
                defaultPlayerType = defaultPlayerType,
                encyclopediaType = encyclopediaType,
                encyclopediaId = encyclopediaId,
                scriptNameList = scriptNames,
                scriptParameterList = scriptParams,
                meshPrefix = meshPrefix,
                mctSkin = mctSkin,
                buildingType = buildingType,
                gdiDamageReportId = gdiDamageReportId,
                nodDamageReportId = nodDamageReportId,
                gdiDestroyReportId = gdiDestroyReportId,
                nodDestroyReportId = nodDestroyReportId,
            )
        }

        private fun loadDefenseObjectDef(defenseChunk: ChunkReader): DefenseObjectDefClass {
            val vars = defenseChunk.findChunk(DEFENSEOBJECTDEF_CHUNK_VARIABLES)
                ?: return DefenseObjectDefClass()
            return DefenseObjectDefClass(
                health              = vars.mcFloat(MCID_DEF_HEALTH) ?: 100f,
                healthMax           = vars.mcFloat(MCID_DEF_HEALTH_MAX) ?: 100f,
                skin                = vars.mcInt(MCID_DEF_SKIN) ?: 0,
                shieldStrength      = vars.mcFloat(MCID_DEF_SHIELD_STRENGTH) ?: 0f,
                shieldStrengthMax   = vars.mcFloat(MCID_DEF_SHIELD_STRENGTH_MAX) ?: 0f,
                shieldType          = vars.mcInt(MCID_DEF_SHIELD_TYPE) ?: 0,
                damagePoints        = vars.mcFloat(MCID_DEF_DAMAGE_POINTS) ?: 0f,
                deathPoints         = vars.mcFloat(MCID_DEF_DEATH_POINTS) ?: 0f,
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
