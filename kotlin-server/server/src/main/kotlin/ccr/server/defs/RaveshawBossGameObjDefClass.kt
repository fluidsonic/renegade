package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of RaveshawBossGameObjDefClass (Combat/raveshawbossgameobj.cpp).
 *
 * C++ hierarchy: RaveshawBossGameObjDefClass : SoldierGameObjDef : SmartGameObjDef :
 *   ArmedGameObjDef : PhysicalGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * RaveshawBossGameObjDefClass adds NO persisted fields of its own; Save_Variables is empty.
 *
 * Chunk layout inside OBJDATA:
 *   [CHUNKID_DEF_PARENT = 0x09230242]  -> SoldierGameObjDef::Save (parent chain)
 *   [CHUNKID_DEF_VARIABLES = 0x09230243] -> (empty — no Raveshaw-specific variables)
 */
class RaveshawBossGameObjDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    soldierDef: SoldierGameObjDef,
) : SoldierGameObjDefWrapper(name, id, chunkId, soldierDef) {

    companion object {
        const val CHUNK_ID: UInt = 0x0004014Cu  // CHUNKID_GAME_OBJECT_DEF_RAVESHAW_BOSS

        private const val CHUNKID_DEF_PARENT = 0x09230242u

        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): RaveshawBossGameObjDefClass? {
            val soldierParentChunk = objDataReader.findChunk(CHUNKID_DEF_PARENT) ?: return null
            val soldierDef = SoldierGameObjDef.load(soldierParentChunk) ?: return null
            return RaveshawBossGameObjDefClass(name = name, id = id, chunkId = chunkId, soldierDef = soldierDef)
        }
    }
}
