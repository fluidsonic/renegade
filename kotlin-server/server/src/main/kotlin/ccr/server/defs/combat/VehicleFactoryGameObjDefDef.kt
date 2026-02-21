package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of VehicleFactoryGameObjDef (Combat/vehiclefactorygameobj.cpp).
 *
 * C++ hierarchy: VehicleFactoryGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (vehiclefactorygameobj.cpp constructor):
 *   PadClearingWarhead(25), TotalBuildingTime(12.0f)
 */
open class VehicleFactoryGameObjDef(
    name: String,
    id: UInt,
    classId: UInt,
    val padClearingWarhead: Int = 25,
    val totalBuildingTime: Float = 12f,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_VEHICLE_FACTORY = CLASSID_BUILDINGS + 5 = 0xD005 */
        const val CLASS_ID: UInt = 0xD005u
    }
}

// Chunk IDs from vehiclefactorygameobj.cpp local enum (CHUNKID_DEF_PARENT = 0x02200638)
private const val CHUNKID_DEF_VARIABLES = 0x02200639u

// Micro-chunk IDs (1 = unused)
private const val MICROCHUNKID_DEF_PADCLEARINGWARHEAD = 2
private const val MICROCHUNKID_DEF_TOTALBUILDINGTIME = 3

/**
 * Parses a VehicleFactoryGameObjDef from the OBJDATA chunk.
 * [name], [id], and [classId] are already extracted by the definition DB reader.
 */
fun parseVehicleFactoryGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): VehicleFactoryGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return VehicleFactoryGameObjDef(name = name, id = id, classId = classId)

    return VehicleFactoryGameObjDef(
        name = name,
        id = id,
        classId = classId,
        padClearingWarhead = vars.readMicroInt(MICROCHUNKID_DEF_PADCLEARINGWARHEAD) ?: 25,
        totalBuildingTime = vars.readMicroFloat(MICROCHUNKID_DEF_TOTALBUILDINGTIME) ?: 12f,
    )
}
