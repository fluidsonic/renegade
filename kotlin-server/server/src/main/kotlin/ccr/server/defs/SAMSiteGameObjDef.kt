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
    classId: UInt,
) : DefinitionClass(name, id, classId) {

    companion object {
        const val CLASS_ID: UInt = 0x3007u // CLASSID_GAME_OBJECT_DEF_SAMSITE
    }
}

/**
 * Parses a SAMSiteGameObjDef from the OBJDATA chunk.
 * No definition-specific fields to extract.
 */
fun parseSAMSiteGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): SAMSiteGameObjDef {
    return SAMSiteGameObjDef(name = name, id = id, classId = classId)
}
