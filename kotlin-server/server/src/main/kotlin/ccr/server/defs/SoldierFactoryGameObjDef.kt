package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of SoldierFactoryGameObjDef (Combat/soldierfactorygameobj.h).
 *
 * C++ hierarchy: SoldierFactoryGameObjDef : BuildingGameObjDef :
 *   DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * SoldierFactoryGameObjDef adds NO persisted fields of its own. Its Save() wraps
 * BuildingGameObjDef::Save() in CHUNKID_DEF_PARENT (0x02211153) and writes an
 * empty CHUNKID_DEF_VARIABLES (0x02211154). The Load_Variables switch is entirely
 * commented out in the C++ source — no micro-chunks are read.
 *
 * C++ protected fields UnloadTime, FundsGathered, HarvesterDefID are declared in
 * the header but never saved or loaded — they are not ported.
 *
 * Chunk IDs (soldierfactorygameobj.cpp enum):
 *   CHUNKID_DEF_PARENT    = 0x02211153
 *   CHUNKID_DEF_VARIABLES = 0x02211154
 */
class SoldierFactoryGameObjDef(
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
        // C++: CHUNKID_GAME_OBJECT_DEF_SOLDIER_FACTORY (combatchunkid.h)
        const val CHUNK_ID: UInt = 0x0004013Cu

        // C++: enum { CHUNKID_DEF_PARENT = 0x02211153, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02211153u
        private const val CHUNKID_DEF_VARIABLES = 0x02211154u

        // C++: MICROCHUNKID_DEF_UNUSED = 1 (no micro-chunks actually used)
        private const val MICROCHUNKID_DEF_UNUSED = 1

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SoldierFactoryGameObjDef {
            val base = BuildingGameObjDef.load(objDataReader, chunkId)
                ?: return SoldierFactoryGameObjDef(name = name, id = id, chunkId = chunkId)
            return SoldierFactoryGameObjDef(
                name = base.name,
                id = base.id,
                chunkId = chunkId,
                defenseObjectDef = base.defenseObjectDef,
                infoIconTextureFilename = base.infoIconTextureFilename,
                translatedNameId = base.translatedNameId,
                notTargetable = base.notTargetable,
                defaultPlayerType = base.defaultPlayerType,
                meshPrefix = base.meshPrefix,
                mctSkin = base.mctSkin,
                buildingType = base.buildingType,
                gdiDamageReportId = base.gdiDamageReportId,
                nodDamageReportId = base.nodDamageReportId,
                gdiDestroyReportId = base.gdiDestroyReportId,
                nodDestroyReportId = base.nodDestroyReportId,
            )
        }
    }

    override fun toString(): String =
        "SoldierFactoryGameObjDef(id=$id, name='$name', type=$buildingType, meshPrefix='$meshPrefix')"
}
