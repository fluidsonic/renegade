package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of VehicleFactoryGameObjDef (Combat/vehiclefactorygameobj.h/.cpp).
 *
 * C++ hierarchy: VehicleFactoryGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * Chunk layout inside OBJDATA (vehiclefactorygameobj.cpp):
 *   [0x02200638] VehicleFactoryDef DEF_PARENT → BuildingGameObjDef::Save
 *     (→ BuildingGameObjDef [207011030] DEF_PARENT, [207011031] DEF_VARIABLES, etc.)
 *   [0x02200639] VehicleFactoryDef DEF_VARIABLES
 *     micro 2 = PadClearingWarhead (int)
 *     micro 3 = TotalBuildingTime (float)
 *
 * C++ defaults (vehiclefactorygameobj.cpp constructor):
 *   PadClearingWarhead(25)     — default to "DEATH" warhead
 *   TotalBuildingTime(12.0f)
 */
open class VehicleFactoryGameObjDef(
    // BuildingGameObjDef constructor params (forwarded)
    name: String,
    id: UInt,
    chunkId: UInt,
    defenseObjectDef: DefenseObjectDefClass = DefenseObjectDefClass(),
    infoIconTextureFilename: String = "",
    translatedNameId: Int = 0,
    notTargetable: Boolean = false,
    defaultPlayerType: Int = PLAYERTYPE_NEUTRAL,
    meshPrefix: String = "",
    mctSkin: Int = 0,
    buildingType: Int = BuildingGameObjDef.BUILDING_TYPE_NONE,
    gdiDamageReportId: Int = 0,
    nodDamageReportId: Int = 0,
    gdiDestroyReportId: Int = 0,
    nodDestroyReportId: Int = 0,

    // C++: int PadClearingWarhead — warhead used to destroy objects blocking the construction area
    val padClearingWarhead: Int = 25,

    // C++: float TotalBuildingTime — total time for slowest vehicle to be constructed and driven out
    val totalBuildingTime: Float = 12.0f,
) : BuildingGameObjDef(
    name = name,
    id = id,
    chunkId = chunkId,
    defenseObjectDef = defenseObjectDef,
    infoIconTextureFilename = infoIconTextureFilename,
    translatedNameId = translatedNameId,
    notTargetable = notTargetable,
    defaultPlayerType = defaultPlayerType,
    meshPrefix = meshPrefix,
    mctSkin = mctSkin,
    buildingType = buildingType,
    gdiDamageReportId = gdiDamageReportId,
    nodDamageReportId = nodDamageReportId,
    gdiDestroyReportId = gdiDestroyReportId,
    nodDestroyReportId = nodDestroyReportId,
) {

    // C++: int Get_Pad_Clearing_Warhead(void) const { return PadClearingWarhead; }
    // → covered by val padClearingWarhead

    // C++: float Get_Total_Building_Time(void) const { return TotalBuildingTime; }
    // → covered by val totalBuildingTime

    companion object {
        /**
         * CHUNKID_GAME_OBJECT_DEF_VEHICLE_FACTORY
         * C++: persist factory chunk id = 0x0004013E
         */
        const val CHUNK_ID: UInt = 0x0004013Eu  // CHUNKID_GAME_OBJECT_DEF_VEHICLE_FACTORY

        // Save/Load chunk IDs (vehiclefactorygameobj.cpp local enum)
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        // Micro chunk IDs
        // MICROCHUNKID_DEF_UNUSED = 1 (not persisted)
        private const val MICROCHUNKID_DEF_PADCLEARINGWARHEAD = 2
        private const val MICROCHUNKID_DEF_TOTALBUILDINGTIME  = 3

        // DefinitionClass base chunk IDs (definition.cpp)
        private const val CHUNKID_DEF_BASE_VARIABLES = 0x00000100u
        private const val VARID_INSTANCEID = 0x01
        private const val VARID_NAME = 0x03

        // DamageableGameObjDef chunk IDs (damageablegameobj.cpp)
        private const val CHUNKID_DAMAGEABLE_DEF_VARIABLES      = 207011206u
        private const val CHUNKID_DAMAGEABLE_DEF_DEFENSEOBJECTDEF = 207011207u
        private const val MCID_DMG_TRANSLATED_NAME_ID           = 1
        private const val MCID_DMG_INFO_ICON_TEXTURE_FILENAME   = 2
        private const val MCID_DMG_NOT_TARGETABLE               = 5
        private const val MCID_DMG_DEFAULT_PLAYER_TYPE          = 6

        // DefenseObjectDef chunk IDs (damage.cpp)
        private const val DEFENSEOBJECTDEF_CHUNK_VARIABLES = 7311607u
        private const val MCID_DEF_HEALTH              = 0x00
        private const val MCID_DEF_HEALTH_MAX          = 0x01
        private const val MCID_DEF_SKIN                = 0x02
        private const val MCID_DEF_SHIELD_STRENGTH     = 0x03
        private const val MCID_DEF_SHIELD_STRENGTH_MAX = 0x04
        private const val MCID_DEF_SHIELD_TYPE         = 0x05
        private const val MCID_DEF_DAMAGE_POINTS       = 0x06
        private const val MCID_DEF_DEATH_POINTS        = 0x07

        // BuildingGameObjDef chunk IDs (building.cpp)
        // enum { CHUNKID_DEF_PARENT = 207011030, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_BUILDING_DEF_VARIABLES = 207011031u
        private const val MCID_MESHPREFIX               = 1
        private const val MCID_MCTSKIN                  = 2
        private const val MCID_BUILDING_TYPE            = 3
        private const val MCID_LEGACY_BUILDING_TEAM     = 4
        private const val MCID_GDI_DAMAGE_REPORT_ID     = 5
        private const val MCID_NOD_DAMAGE_REPORT_ID     = 6
        private const val MCID_GDI_DESTROY_REPORT_ID   = 7
        private const val MCID_NOD_DESTROY_REPORT_ID   = 8

        // Legacy building team enum (building.h BuildingConstants::LegacyBuildingTeam)
        private const val LEGACY_TEAM_NOD = 1

        /**
         * Loads a VehicleFactoryGameObjDef from the OBJDATA chunk of a definition entry.
         * Returns null if required base fields (id, name) are missing.
         */
        fun load(objDataChunk: ChunkReader, chunkId: UInt): VehicleFactoryGameObjDef? {
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
            val translatedNameId    = dmgVars?.mcInt(MCID_DMG_TRANSLATED_NAME_ID) ?: 0
            val infoIconTexture     = dmgVars?.mcString(MCID_DMG_INFO_ICON_TEXTURE_FILENAME) ?: ""
            val notTargetable       = dmgVars?.mcBool(MCID_DMG_NOT_TARGETABLE) ?: false
            var defaultPlayerType   = dmgVars?.mcInt(MCID_DMG_DEFAULT_PLAYER_TYPE) ?: PLAYERTYPE_NEUTRAL

            // DefenseObjectDef (embedded in DamageableGameObjDef)
            val defenseChunk = objDataChunk.findChunkRecursive(CHUNKID_DAMAGEABLE_DEF_DEFENSEOBJECTDEF)
            val defenseObjectDef = if (defenseChunk != null) {
                loadDefenseObjectDef(defenseChunk)
            } else {
                DefenseObjectDefClass()
            }

            // --- BuildingGameObjDef ---
            val bldgVars = objDataChunk.findChunk(CHUNKID_BUILDING_DEF_VARIABLES)
            val meshPrefix         = bldgVars?.mcString(MCID_MESHPREFIX) ?: ""
            val mctSkin            = bldgVars?.mcInt(MCID_MCTSKIN) ?: 0
            val buildingType       = bldgVars?.mcInt(MCID_BUILDING_TYPE) ?: BuildingGameObjDef.BUILDING_TYPE_NONE
            val gdiDamageReportId  = bldgVars?.mcInt(MCID_GDI_DAMAGE_REPORT_ID) ?: 0
            val nodDamageReportId  = bldgVars?.mcInt(MCID_NOD_DAMAGE_REPORT_ID) ?: 0
            val gdiDestroyReportId = bldgVars?.mcInt(MCID_GDI_DESTROY_REPORT_ID) ?: 0
            val nodDestroyReportId = bldgVars?.mcInt(MCID_NOD_DESTROY_REPORT_ID) ?: 0

            // Legacy team handling: if legacy_team micro-chunk is present, override defaultPlayerType
            val legacyTeam = bldgVars?.mcInt(MCID_LEGACY_BUILDING_TEAM)
            if (legacyTeam != null && legacyTeam != -1) {
                defaultPlayerType = if (legacyTeam == LEGACY_TEAM_NOD) PLAYERTYPE_NOD else PLAYERTYPE_GDI
            }

            // --- VehicleFactoryGameObjDef own variables ---
            val vfVars             = objDataChunk.findChunkRecursive(CHUNKID_DEF_VARIABLES)
            val padClearingWarhead = vfVars?.mcInt(MICROCHUNKID_DEF_PADCLEARINGWARHEAD) ?: 25
            val totalBuildingTime  = vfVars?.mcFloat(MICROCHUNKID_DEF_TOTALBUILDINGTIME) ?: 12.0f

            return VehicleFactoryGameObjDef(
                name                    = name,
                id                      = definitionId,
                chunkId                 = chunkId,
                defenseObjectDef        = defenseObjectDef,
                infoIconTextureFilename = infoIconTexture,
                translatedNameId        = translatedNameId,
                notTargetable           = notTargetable,
                defaultPlayerType       = defaultPlayerType,
                meshPrefix              = meshPrefix,
                mctSkin                 = mctSkin,
                buildingType            = buildingType,
                gdiDamageReportId       = gdiDamageReportId,
                nodDamageReportId       = nodDamageReportId,
                gdiDestroyReportId      = gdiDestroyReportId,
                nodDestroyReportId      = nodDestroyReportId,
                padClearingWarhead      = padClearingWarhead,
                totalBuildingTime       = totalBuildingTime,
            )
        }

        /** Convenience overload — name/id already extracted by caller; delegates to full load(). */
        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): VehicleFactoryGameObjDef {
            return load(objDataReader, chunkId) ?: VehicleFactoryGameObjDef(name = name, id = id, chunkId = chunkId)
        }

        private fun loadDefenseObjectDef(defenseChunk: ChunkReader): DefenseObjectDefClass {
            val vars = defenseChunk.findChunk(DEFENSEOBJECTDEF_CHUNK_VARIABLES)
                ?: return DefenseObjectDefClass()
            return DefenseObjectDefClass(
                health            = vars.mcFloat(MCID_DEF_HEALTH) ?: 100f,
                healthMax         = vars.mcFloat(MCID_DEF_HEALTH_MAX) ?: 100f,
                skin              = vars.mcInt(MCID_DEF_SKIN) ?: 0,
                shieldStrength    = vars.mcFloat(MCID_DEF_SHIELD_STRENGTH) ?: 0f,
                shieldStrengthMax = vars.mcFloat(MCID_DEF_SHIELD_STRENGTH_MAX) ?: 0f,
                shieldType        = vars.mcInt(MCID_DEF_SHIELD_TYPE) ?: 0,
                damagePoints      = vars.mcFloat(MCID_DEF_DAMAGE_POINTS) ?: 0f,
                deathPoints       = vars.mcFloat(MCID_DEF_DEATH_POINTS) ?: 0f,
            )
        }

        private fun readNullTermString(bytes: ByteArray): String {
            val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
            val len = if (nullIdx < 0) bytes.size else nullIdx
            return String(bytes, 0, len, Charsets.ISO_8859_1)
        }
    }

    override fun toString(): String =
        "VehicleFactoryGameObjDef(id=$id, name='$name', padClearingWarhead=$padClearingWarhead, totalBuildingTime=$totalBuildingTime)"
}
