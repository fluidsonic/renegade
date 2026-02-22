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
 * empty CHUNKID_DEF_VARIABLES (0x02211154).
 */
class SoldierFactoryGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        const val CHUNK_ID: UInt = 0x0004013Cu  // CHUNKID_GAME_OBJECT_DEF_SOLDIER_FACTORY
    }
}

/**
 * Parses a SoldierFactoryGameObjDef from the OBJDATA chunk.
 * No definition-specific fields to extract.
 */
fun parseSoldierFactoryGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): SoldierFactoryGameObjDef {
    return SoldierFactoryGameObjDef(name = name, id = id, chunkId = chunkId)
}
