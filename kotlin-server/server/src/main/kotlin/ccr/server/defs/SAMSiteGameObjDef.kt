package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of SAMSiteGameObjDef (Combat/samsite.h).
 *
 * C++ hierarchy: SAMSiteGameObjDef : SmartGameObjDef : ArmedGameObjDef :
 *   PhysicalGameObjDef : DamageableGameObjDef : ScriptableGameObjDef :
 *   BaseGameObjDef : DefinitionClass
 *
 * SAMSiteGameObjDef adds NO persisted fields of its own. Its Save() simply
 * wraps SmartGameObjDef::Save() in CHUNKID_DEF_PARENT (930991800).
 */
class SAMSiteGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
) : SmartGameObjDef(name, id, chunkId) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040109u  // CHUNKID_GAME_OBJECT_DEF_SAMSITE

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SAMSiteGameObjDef {
            return SAMSiteGameObjDef(name = name, id = id, chunkId = chunkId)
        }
    }
}
