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
    name: String,
    id: UInt,
    chunkId: UInt,
) : BuildingGameObjDef(name, id, chunkId) {

    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_SOLDIER_FACTORY (combatchunkid.h)
        const val CHUNK_ID: UInt = 0x0004013Cu

        // C++: enum { CHUNKID_DEF_PARENT = 0x02211153, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT    = 0x02211153u
        private const val CHUNKID_DEF_VARIABLES = 0x02211154u

        // C++: MICROCHUNKID_DEF_UNUSED = 1 (no micro-chunks actually used)
        private const val MICROCHUNKID_DEF_UNUSED = 1

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SoldierFactoryGameObjDef {
            return SoldierFactoryGameObjDef(name = name, id = id, chunkId = chunkId)
        }
    }
}
