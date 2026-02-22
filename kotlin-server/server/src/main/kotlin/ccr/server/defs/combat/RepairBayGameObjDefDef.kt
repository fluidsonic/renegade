package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of RepairBayGameObjDef (Combat/repairbaygameobj.cpp).
 *
 * C++ hierarchy: RepairBayGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (repairbaygameobj.cpp constructor):
 *   RepairPerSec(0), RepairingStaticAnimDefID(0)
 */
class RepairBayGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val repairPerSec: Float = 0f,
    val repairingStaticAnimDefId: Int = 0,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_REPAIR_BAY = CLASSID_BUILDINGS + 9 = 0xD009 */
        const val CHUNK_ID: UInt = 0x00040146u  // CHUNKID_GAME_OBJECT_DEF_REPAIR_BAY
    }
}

// Chunk IDs from repairbaygameobj.cpp local enum (CHUNKID_DEF_PARENT = 0x02200638)
private const val CHUNKID_DEF_VARIABLES = 0x02200639u

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_REPAIR_PER_SEC = 1
private const val MICROCHUNKID_DEF_REPAIRING_STATICANIM_DEFID = 2

/**
 * Parses a RepairBayGameObjDef from the OBJDATA chunk.
 * [name], [id], and [chunkId] are already extracted by the definition DB reader.
 */
fun parseRepairBayGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): RepairBayGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return RepairBayGameObjDef(name = name, id = id, chunkId = chunkId)

    return RepairBayGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        repairPerSec = vars.readMicroFloat(MICROCHUNKID_DEF_REPAIR_PER_SEC) ?: 0f,
        repairingStaticAnimDefId = vars.readMicroInt(MICROCHUNKID_DEF_REPAIRING_STATICANIM_DEFID) ?: 0,
    )
}
