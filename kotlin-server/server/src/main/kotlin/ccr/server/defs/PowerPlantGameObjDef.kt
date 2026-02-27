package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// C++: PowerPlantGameObjDef : public BuildingGameObjDef
// Hierarchy: DefinitionClass → BaseGameObjDef → ScriptableGameObjDef → DamageableGameObjDef
//            → BuildingGameObjDef → PowerPlantGameObjDef
//
// PowerPlantGameObjDef adds three protected fields but does NOT persist them
// (the Load_Variables switch block is commented out in the C++ source).
// The fields are declared here for completeness (friend access from PowerPlantGameObj).
open class PowerPlantGameObjDef(
    // BuildingGameObjDef base
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
) : BuildingGameObjDef(
    name                  = name,
    id                    = id,
    chunkId               = chunkId,
    defenseObjectDef      = defenseObjectDef,
    infoIconTextureFilename = infoIconTextureFilename,
    translatedNameId      = translatedNameId,
    notTargetable         = notTargetable,
    defaultPlayerType     = defaultPlayerType,
    meshPrefix            = meshPrefix,
    mctSkin               = mctSkin,
    buildingType          = buildingType,
    gdiDamageReportId     = gdiDamageReportId,
    nodDamageReportId     = nodDamageReportId,
    gdiDestroyReportId    = gdiDestroyReportId,
    nodDestroyReportId    = nodDestroyReportId,
) {

    // C++: float UnloadTime (protected, not persisted)
    protected var unloadTime: Float = 0f

    // C++: float FundsGathered (protected, not persisted)
    protected var fundsGathered: Float = 0f

    // C++: int HarvesterDefID (protected, not persisted)
    protected var harvesterDefId: Int = 0

    companion object {
        /** CHUNKID_GAME_OBJECT_DEF_POWERPLANT — sequential after CHUNKID_GAME_OBJECT_DEF_REFINERY */
        const val CHUNK_ID: UInt = 0x0004013Au  // CHUNKID_GAME_OBJECT_DEF_POWERPLANT

        /** CLASSID_GAME_OBJECT_DEF_POWERPLANT = CLASSID_BUILDINGS + 3 = 0xD003 */
        const val CLASS_ID: UInt = 0xD003u

        // C++: enum { CHUNKID_DEF_PARENT = 0x02211153, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02211153u
        private const val CHUNKID_DEF_VARIABLES = 0x02211154u

        private const val MICROCHUNKID_DEF_UNUSED = 1

        /**
         * Loads a PowerPlantGameObjDef from the OBJDATA chunk of a definition entry.
         * Delegates all base-class parsing to BuildingGameObjDef.load() then wraps the
         * result in a PowerPlantGameObjDef.
         */
        fun load(objDataChunk: ChunkReader, chunkId: UInt): PowerPlantGameObjDef? {
            val base = BuildingGameObjDef.load(objDataChunk, chunkId) ?: return null
            // C++: PowerPlantGameObjDef::Load → CHUNKID_DEF_PARENT calls BuildingGameObjDef::Load;
            // CHUNKID_DEF_VARIABLES calls Load_Variables, whose switch is entirely commented out.
            // No PowerPlant-specific fields are persisted.
            return PowerPlantGameObjDef(
                name                    = base.name,
                id                      = base.id,
                chunkId                 = chunkId,
                defenseObjectDef        = base.defenseObjectDef,
                infoIconTextureFilename = base.infoIconTextureFilename,
                translatedNameId        = base.translatedNameId,
                notTargetable           = base.notTargetable,
                defaultPlayerType       = base.defaultPlayerType,
                meshPrefix              = base.meshPrefix,
                mctSkin                 = base.mctSkin,
                buildingType            = base.buildingType,
                gdiDamageReportId       = base.gdiDamageReportId,
                nodDamageReportId       = base.nodDamageReportId,
                gdiDestroyReportId      = base.gdiDestroyReportId,
                nodDestroyReportId      = base.nodDestroyReportId,
            )
        }

        /** Convenience overload — name/id already extracted by caller; delegates to full load(). */
        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): PowerPlantGameObjDef {
            return load(objDataReader, chunkId) ?: PowerPlantGameObjDef(name = name, id = id, chunkId = chunkId)
        }
    }

    override fun toString(): String =
        "PowerPlantGameObjDef(id=$id, name='$name', type=$buildingType, meshPrefix='$meshPrefix')"
}
