package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of ComCenterGameObjDef (Combat/comcentergameobj.cpp).
 *
 * C++ hierarchy: ComCenterGameObjDef : BuildingGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * ComCenterGameObjDef adds NO persisted fields of its own. Its Save() wraps
 * BuildingGameObjDef::Save() in CHUNKID_DEF_PARENT and writes an empty
 * CHUNKID_DEF_VARIABLES.
 */
class ComCenterGameObjDef(
    name: String,
    id: UInt,
    classId: UInt,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_COMCENTER = CLASSID_BUILDINGS + 8 = 0xD008 */
        const val CLASS_ID: UInt = 0xD008u
    }
}

/**
 * Parses a ComCenterGameObjDef from the OBJDATA chunk.
 * No definition-specific fields to extract.
 */
fun parseComCenterGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): ComCenterGameObjDef {
    return ComCenterGameObjDef(name = name, id = id, classId = classId)
}
