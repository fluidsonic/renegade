package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of AirStripGameObjDef (Combat/airstripgameobj.h / airstripgameobj.cpp).
 *
 * C++ hierarchy: AirStripGameObjDef : VehicleFactoryGameObjDef : BuildingGameObjDef :
 *   DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (airstripgameobj.cpp constructor):
 *   CinematicDefID(0), CinematicSlotIndex(0),
 *   CinematicLengthToDropOff(0), CinematicLengthToVehicleDisplay(0)
 */
class AirStripGameObjDef(
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
    // C++: int CinematicDefID (initialized to 0)
    val cinematicDefId: Int = 0,
    // C++: int CinematicSlotIndex (initialized to 0)
    val cinematicSlotIndex: Int = 0,
    // C++: float CinematicLengthToDropOff (initialized to 0)
    val cinematicLengthToDropOff: Float = 0f,
    // C++: float CinematicLengthToVehicleDisplay (initialized to 0)
    val cinematicLengthToVehicleDisplay: Float = 0f,
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
        // C++: CHUNKID_GAME_OBJECT_DEF_AIRSTRIP — persist factory chunk ID for AirStripGameObjDef.
        // CLASSID_BUILDINGS + 8 area — verified from combatchunkid.h sequence.
        const val CHUNK_ID: UInt = 0x00040140u  // CHUNKID_GAME_OBJECT_DEF_AIRSTRIP

        // AirStripGameObjDef Save/Load chunk IDs (airstripgameobj.cpp local enum)
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, ... }
        // NOTE: AirStrip uses the SAME chunk IDs as VehicleFactory — 0x02200638/0x02200639.
        // AirStrip's own vars are at top-level CHUNKID_DEF_VARIABLES (0x02200639),
        // while VehicleFactory's vars are INSIDE DEF_PARENT (0x02200638).
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        // AirStripGameObjDef micro-chunk IDs (airstripgameobj.cpp local enum)
        private const val MICROCHUNKID_DEF_CINEMATIC_DEFID                = 1
        private const val MICROCHUNKID_DEF_CINEMATIC_LENGTH_TO_DROPOFF    = 2
        private const val MICROCHUNKID_DEF_CINEMATIC_SLOT_INDEX           = 3
        private const val MICROCHUNKID_DEF_DISPLAY_VEHICLE_TIME           = 4

        /**
         * Parses an AirStripGameObjDef from the OBJDATA chunk.
         * Delegates VehicleFactory/Building chain parsing to VehicleFactoryGameObjDef.load(),
         * then reads AirStrip's own vars from the top-level CHUNKID_DEF_VARIABLES.
         *
         * C++: AirStripGameObjDef::Load / Load_Variables
         */
        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): AirStripGameObjDef {
            // Delegate to VehicleFactoryGameObjDef for all parent chain fields
            val base = VehicleFactoryGameObjDef.load(objDataReader, chunkId)

            // Read AirStrip's own vars from top-level CHUNKID_DEF_VARIABLES
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
            val cinematicDefId = vars?.readMicroInt(MICROCHUNKID_DEF_CINEMATIC_DEFID) ?: 0
            val cinematicLengthToDropOff = vars?.readMicroFloat(MICROCHUNKID_DEF_CINEMATIC_LENGTH_TO_DROPOFF) ?: 0f
            val cinematicSlotIndex = vars?.readMicroInt(MICROCHUNKID_DEF_CINEMATIC_SLOT_INDEX) ?: 0
            val cinematicLengthToVehicleDisplay = vars?.readMicroFloat(MICROCHUNKID_DEF_DISPLAY_VEHICLE_TIME) ?: 0f

            if (base == null) {
                return AirStripGameObjDef(
                    name = name, id = id, chunkId = chunkId,
                    cinematicDefId = cinematicDefId,
                    cinematicLengthToDropOff = cinematicLengthToDropOff,
                    cinematicSlotIndex = cinematicSlotIndex,
                    cinematicLengthToVehicleDisplay = cinematicLengthToVehicleDisplay,
                )
            }

            return AirStripGameObjDef(
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
                cinematicDefId = cinematicDefId,
                cinematicLengthToDropOff = cinematicLengthToDropOff,
                cinematicSlotIndex = cinematicSlotIndex,
                cinematicLengthToVehicleDisplay = cinematicLengthToVehicleDisplay,
            )
        }
    }
}
