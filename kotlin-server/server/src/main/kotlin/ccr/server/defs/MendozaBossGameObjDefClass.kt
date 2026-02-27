package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of MendozaBossGameObjDefClass (Combat/mendozabossgameobj.cpp).
 *
 * C++ hierarchy: MendozaBossGameObjDefClass : SoldierGameObjDef : SmartGameObjDef :
 *   ArmedGameObjDef : PhysicalGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * MendozaBossGameObjDefClass adds NO persisted fields of its own; Save_Variables is empty.
 *
 * Chunk layout inside OBJDATA:
 *   [CHUNKID_DEF_PARENT = 0x09230242]  -> SoldierGameObjDef::Save (parent chain)
 *   [CHUNKID_DEF_VARIABLES = 0x09230243] -> (empty — no Mendoza-specific variables)
 */
class MendozaBossGameObjDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    soldierDef: SoldierGameObjDef,
) : SoldierGameObjDefWrapper(name, id, chunkId, soldierDef) {

    companion object {
        const val CHUNK_ID: UInt = 0x0004014Au  // CHUNKID_GAME_OBJECT_DEF_MENDOZA_BOSS

        private const val CHUNKID_DEF_PARENT = 0x09230242u

        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): MendozaBossGameObjDefClass? {
            val soldierParentChunk = objDataReader.findChunk(CHUNKID_DEF_PARENT) ?: return null
            val soldierDef = SoldierGameObjDef.load(soldierParentChunk) ?: return null
            return MendozaBossGameObjDefClass(name = name, id = id, chunkId = chunkId, soldierDef = soldierDef)
        }
    }
}
