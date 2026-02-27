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
    name: String,
    id: UInt,
    chunkId: UInt,
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
    padClearingWarhead = padClearingWarhead,
    totalBuildingTime = totalBuildingTime,
) {

    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_AIRSTRIP — persist factory chunk ID for AirStripGameObjDef.
        // CLASSID_BUILDINGS + 8 area — verified from combatchunkid.h sequence.
        const val CHUNK_ID: UInt = 0x00040140u  // CHUNKID_GAME_OBJECT_DEF_AIRSTRIP

        // AirStripGameObjDef Save/Load chunk IDs (airstripgameobj.cpp local enum)
        // enum { CHUNKID_DEF_PARENT = 0x02200638, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02200638u
        private const val CHUNKID_DEF_VARIABLES = 0x02200639u

        // AirStripGameObjDef micro-chunk IDs (airstripgameobj.cpp local enum)
        private const val MICROCHUNKID_DEF_CINEMATIC_DEFID                = 1
        private const val MICROCHUNKID_DEF_CINEMATIC_LENGTH_TO_DROPOFF    = 2
        private const val MICROCHUNKID_DEF_CINEMATIC_SLOT_INDEX           = 3
        private const val MICROCHUNKID_DEF_DISPLAY_VEHICLE_TIME           = 4

        /**
         * Parses an AirStripGameObjDef from the OBJDATA chunk.
         * [name], [id], and [chunkId] are already extracted by the definition DB reader.
         *
         * C++: AirStripGameObjDef::Load / Load_Variables
         */
        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): AirStripGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
                ?: return AirStripGameObjDef(name = name, id = id, chunkId = chunkId)

            return AirStripGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                cinematicDefId = vars.readMicroInt(MICROCHUNKID_DEF_CINEMATIC_DEFID) ?: 0,
                cinematicLengthToDropOff = vars.readMicroFloat(MICROCHUNKID_DEF_CINEMATIC_LENGTH_TO_DROPOFF) ?: 0f,
                cinematicSlotIndex = vars.readMicroInt(MICROCHUNKID_DEF_CINEMATIC_SLOT_INDEX) ?: 0,
                cinematicLengthToVehicleDisplay = vars.readMicroFloat(MICROCHUNKID_DEF_DISPLAY_VEHICLE_TIME) ?: 0f,
            )
        }
    }
}
