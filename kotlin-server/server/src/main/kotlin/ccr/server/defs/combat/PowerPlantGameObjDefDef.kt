package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of PowerPlantGameObjDef (Combat/powerplantgameobj.cpp).
 *
 * C++ hierarchy: PowerPlantGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * PowerPlantGameObjDef adds NO persisted fields of its own.
 */
class PowerPlantGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_POWERPLANT = CLASSID_BUILDINGS + 3 = 0xD003 */
        const val CHUNK_ID: UInt = 0x0004013Au  // CHUNKID_GAME_OBJECT_DEF_POWERPLANT
    }
}

/**
 * Parses a PowerPlantGameObjDef from the OBJDATA chunk.
 * No definition-specific fields to extract.
 */
fun parsePowerPlantGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): PowerPlantGameObjDef {
    return PowerPlantGameObjDef(name = name, id = id, chunkId = chunkId)
}
