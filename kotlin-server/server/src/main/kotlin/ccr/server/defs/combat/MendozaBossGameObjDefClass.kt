package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of MendozaBossGameObjDefClass (Combat/mendozabossgameobj.cpp).
 *
 * C++ hierarchy: MendozaBossGameObjDefClass : SoldierGameObjDef : SmartGameObjDef :
 *   ArmedGameObjDef : PhysicalGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * MendozaBossGameObjDefClass adds NO persisted fields of its own. The C++ enum
 * declares micro-chunk IDs for gatling/rocket defs but Save_Variables is empty.
 */
class MendozaBossGameObjDefClass(
    name: String,
    id: UInt,
    classId: UInt,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_MENDOZA_BOSS = CLASSID_GAME_OBJECTS + 23 = 0x3017 */
        const val CLASS_ID: UInt = 0x3017u
    }
}

/**
 * Parses a MendozaBossGameObjDefClass from the OBJDATA chunk.
 * No additional persisted fields.
 */
fun parseMendozaBossGameObjDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): MendozaBossGameObjDefClass {
    return MendozaBossGameObjDefClass(name = name, id = id, classId = classId)
}
