package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of WarFactoryGameObjDef (Combat/warfactorygameobj.h/.cpp).
 *
 * C++ hierarchy: WarFactoryGameObjDef : VehicleFactoryGameObjDef : BuildingGameObjDef :
 *   DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * WarFactoryGameObjDef adds NO persisted fields of its own. Its Save() wraps
 * VehicleFactoryGameObjDef::Save() in CHUNKID_DEF_PARENT (0x02200638) and writes an
 * empty CHUNKID_DEF_VARIABLES (0x02200639).
 */
// C++: class WarFactoryGameObjDef : public VehicleFactoryGameObjDef
class WarFactoryGameObjDef(
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
    // VehicleFactoryGameObjDef fields
    padClearingWarhead: Int = 25,
    totalBuildingTime: Float = 12f,
) : VehicleFactoryGameObjDef(
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
    padClearingWarhead = padClearingWarhead,
    totalBuildingTime = totalBuildingTime,
) {

    companion object {
        /** CHUNKID_GAME_OBJECT_DEF_WARFACTORY = 0x00040142 (combatchunkid.h) */
        const val CHUNK_ID: UInt = 0x00040142u

        // C++: CLASSID_GAME_OBJECT_DEF_WARFACTORY = 0xD007 (combatchunkid.h)
        const val CLASS_ID: UInt = 0xD007u

        // C++: local enum in warfactorygameobj.cpp
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, MICROCHUNKID_DEF_UNUSED = 1 }
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        /**
         * Parses a WarFactoryGameObjDef from the OBJDATA chunk.
         * Delegates all parsing to VehicleFactoryGameObjDef.load() then wraps the result.
         * No WarFactory-specific fields are persisted.
         */
        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): WarFactoryGameObjDef {
            val base = VehicleFactoryGameObjDef.load(objDataReader, chunkId)
                ?: return WarFactoryGameObjDef(name = name, id = id, chunkId = chunkId)
            return WarFactoryGameObjDef(
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
                padClearingWarhead = base.padClearingWarhead,
                totalBuildingTime = base.totalBuildingTime,
            )
        }
    }

    override fun toString(): String =
        "WarFactoryGameObjDef(id=$id, name='$name', padClearingWarhead=$padClearingWarhead, totalBuildingTime=$totalBuildingTime)"
}
