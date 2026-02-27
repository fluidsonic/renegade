package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of RepairBayGameObjDef (Combat/repairbaygameobj.h).
 *
 * C++ hierarchy: RepairBayGameObjDef : BuildingGameObjDef :
 *   DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * Save layout inside persist chunk:
 *   [CHUNKID_DEF_PARENT = 0x02200638] → BuildingGameObjDef::Save
 *   [CHUNKID_DEF_VARIABLES = 0x02200639]
 *     micro 1 = RepairPerSec (float)
 *     micro 2 = RepairingStaticAnimDefID (int)
 */
// C++: RepairBayGameObjDef : public BuildingGameObjDef
class RepairBayGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    // BuildingGameObjDef fields
    defenseObjectDef: DefenseObjectDefClass = DefenseObjectDefClass(),
    infoIconTextureFilename: String = "",
    translatedNameId: Int = 0,
    notTargetable: Boolean = false,
    defaultPlayerType: Int = BuildingGameObjDef.PLAYERTYPE_NEUTRAL,
    meshPrefix: String = "",
    mctSkin: Int = 0,
    buildingType: Int = BuildingGameObjDef.BUILDING_TYPE_REPAIR_BAY,
    gdiDamageReportId: Int = 0,
    nodDamageReportId: Int = 0,
    gdiDestroyReportId: Int = 0,
    nodDestroyReportId: Int = 0,
    // RepairBayGameObjDef own fields
    val repairPerSec: Float = 0f,               // C++: float RepairPerSec (initialized to 0)
    val repairingStaticAnimDefId: Int = 0,       // C++: int RepairingStaticAnimDefID (initialized to 0)
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

    companion object {
        /** CHUNKID_GAME_OBJECT_DEF_REPAIR_BAY (combatchunkid.h) */
        const val CHUNK_ID: UInt = 0x00040146u

        // C++: local enum in repairbaygameobj.cpp
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        private const val MICROCHUNKID_DEF_REPAIR_PER_SEC               = 1
        private const val MICROCHUNKID_DEF_REPARING_STATICANIM_DEFID    = 2

        /**
         * Loads a RepairBayGameObjDef from the OBJDATA chunk of a definition entry.
         * Returns null if required base fields (id, name) are missing.
         */
        fun load(objDataChunk: ChunkReader, chunkId: UInt): RepairBayGameObjDef? {
            // Parse the base BuildingGameObjDef first via its own loader
            val buildingDef = BuildingGameObjDef.load(objDataChunk, chunkId)
                ?: return null

            // RepairBayGameObjDef own variables
            val vars = objDataChunk.findChunk(CHUNKID_DEF_VARIABLES)
            val repairPerSec = vars?.readMicroFloat(MICROCHUNKID_DEF_REPAIR_PER_SEC) ?: 0f
            val repairingStaticAnimDefId = vars?.readMicroInt(MICROCHUNKID_DEF_REPARING_STATICANIM_DEFID) ?: 0

            return RepairBayGameObjDef(
                name = buildingDef.name,
                id = buildingDef.id,
                chunkId = chunkId,
                defenseObjectDef = buildingDef.defenseObjectDef,
                infoIconTextureFilename = buildingDef.infoIconTextureFilename,
                translatedNameId = buildingDef.translatedNameId,
                notTargetable = buildingDef.notTargetable,
                defaultPlayerType = buildingDef.defaultPlayerType,
                meshPrefix = buildingDef.meshPrefix,
                mctSkin = buildingDef.mctSkin,
                buildingType = buildingDef.buildingType,
                gdiDamageReportId = buildingDef.gdiDamageReportId,
                nodDamageReportId = buildingDef.nodDamageReportId,
                gdiDestroyReportId = buildingDef.gdiDestroyReportId,
                nodDestroyReportId = buildingDef.nodDestroyReportId,
                repairPerSec = repairPerSec,
                repairingStaticAnimDefId = repairingStaticAnimDefId,
            )
        }

        /** Convenience overload — name/id already extracted by caller; delegates to full load(). */
        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): RepairBayGameObjDef {
            return load(objDataReader, chunkId) ?: RepairBayGameObjDef(name = name, id = id, chunkId = chunkId)
        }
    }

    override fun toString(): String =
        "RepairBayGameObjDef(id=$id, name='$name', repairPerSec=$repairPerSec, repairingStaticAnimDefId=$repairingStaticAnimDefId)"
}
