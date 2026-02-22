package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of RefineryGameObjDef (Combat/refinerygameobj.cpp).
 *
 * C++ hierarchy: RefineryGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (refinerygameobj.cpp constructor):
 *   UnloadTime(0), FundsGathered(0), FundsDistributedPerSec(0), HarvesterDefID(0)
 */
class RefineryGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val unloadTime: Float = 0f,
    val fundsGathered: Float = 0f,
    val fundsDistributedPerSec: Float = 0f,
    val harvesterDefId: Int = 0,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_REFINERY = CLASSID_BUILDINGS + 2 = 0xD002 */
        const val CHUNK_ID: UInt = 0x00040138u  // CHUNKID_GAME_OBJECT_DEF_REFINERY
    }
}

// Chunk IDs from refinerygameobj.cpp local enum (CHUNKID_DEF_PARENT = 0x02200638)
private const val CHUNKID_DEF_VARIABLES = 0x02200639u

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_UNLOAD_TIME = 1
private const val MICROCHUNKID_DEF_FUNDS_GATHERED = 2
private const val MICROCHUNKID_DEF_HARVESTER_DEFID = 3
private const val MICROCHUNKID_DEF_FUNDS_PER_SEC = 4

/**
 * Parses a RefineryGameObjDef from the OBJDATA chunk.
 * [name], [id], and [chunkId] are already extracted by the definition DB reader.
 */
fun parseRefineryGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): RefineryGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return RefineryGameObjDef(name = name, id = id, chunkId = chunkId)

    return RefineryGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        unloadTime = vars.readMicroFloat(MICROCHUNKID_DEF_UNLOAD_TIME) ?: 0f,
        fundsGathered = vars.readMicroFloat(MICROCHUNKID_DEF_FUNDS_GATHERED) ?: 0f,
        fundsDistributedPerSec = vars.readMicroFloat(MICROCHUNKID_DEF_FUNDS_PER_SEC) ?: 0f,
        harvesterDefId = vars.readMicroInt(MICROCHUNKID_DEF_HARVESTER_DEFID) ?: 0,
    )
}
