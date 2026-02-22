package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of WarFactoryGameObjDef (Combat/warfactorygameobj.cpp).
 *
 * C++ hierarchy: WarFactoryGameObjDef : VehicleFactoryGameObjDef : BuildingGameObjDef :
 *   DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * WarFactoryGameObjDef adds NO persisted fields of its own. Its Save() wraps
 * VehicleFactoryGameObjDef::Save() in CHUNKID_DEF_PARENT and writes an empty
 * CHUNKID_DEF_VARIABLES.
 */
class WarFactoryGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_WARFACTORY = CLASSID_BUILDINGS + 7 = 0xD007 */
        const val CHUNK_ID: UInt = 0x00040142u  // CHUNKID_GAME_OBJECT_DEF_WARFACTORY
    }
}

/**
 * Parses a WarFactoryGameObjDef from the OBJDATA chunk.
 * No definition-specific fields to extract.
 */
fun parseWarFactoryGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): WarFactoryGameObjDef {
    return WarFactoryGameObjDef(name = name, id = id, chunkId = chunkId)
}
