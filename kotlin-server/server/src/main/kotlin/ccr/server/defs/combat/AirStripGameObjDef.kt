package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of AirStripGameObjDef (Combat/airstripgameobj.cpp).
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
    val cinematicDefId: Int = 0,
    val cinematicSlotIndex: Int = 0,
    val cinematicLengthToDropOff: Float = 0f,
    val cinematicLengthToVehicleDisplay: Float = 0f,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_AIRSTRIP = CLASSID_BUILDINGS + 6 = 0xD006 */
        const val CHUNK_ID: UInt = 0x00040140u  // CHUNKID_GAME_OBJECT_DEF_AIRSTRIP
    }
}

// Chunk IDs from airstripgameobj.cpp local enum (CHUNKID_DEF_PARENT = 0x02200638)
private const val CHUNKID_DEF_VARIABLES = 0x02200639u

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_CINEMATIC_DEFID = 1
private const val MICROCHUNKID_DEF_CINEMATIC_LENGTH_TO_DROPOFF = 2
private const val MICROCHUNKID_DEF_CINEMATIC_SLOT_INDEX = 3
private const val MICROCHUNKID_DEF_DISPLAY_VEHICLE_TIME = 4

/**
 * Parses an AirStripGameObjDef from the OBJDATA chunk.
 * [name], [id], and [chunkId] are already extracted by the definition DB reader.
 */
fun parseAirStripGameObjDef(
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
