package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of RaveshawBossGameObjDefClass (Combat/raveshawbossgameobj.cpp).
 *
 * C++ hierarchy: RaveshawBossGameObjDefClass : SoldierGameObjDef : SmartGameObjDef :
 *   ArmedGameObjDef : PhysicalGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * RaveshawBossGameObjDefClass adds NO persisted fields of its own. The C++ enum
 * declares micro-chunk IDs for gatling/rocket defs but Save_Variables is empty.
 */
class RaveshawBossGameObjDefClass(
    name: String,
    id: UInt,
    classId: UInt,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_RAVESHAW_BOSS = CLASSID_GAME_OBJECTS + 24 = 0x3018 */
        const val CLASS_ID: UInt = 0x3018u
    }
}

/**
 * Parses a RaveshawBossGameObjDefClass from the OBJDATA chunk.
 * No additional persisted fields.
 */
fun parseRaveshawBossGameObjDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): RaveshawBossGameObjDefClass {
    return RaveshawBossGameObjDefClass(name = name, id = id, classId = classId)
}
