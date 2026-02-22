package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of SpecialEffectsGameObjDef (Combat/specialeffectsgameobj.cpp).
 *
 * C++ hierarchy: SpecialEffectsGameObjDef : PhysicalGameObjDef : DamageableGameObjDef :
 *   ScriptableGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (specialeffectsgameobj.cpp constructor):
 *   SoundDefID(0), AnimationName("")
 */
class SpecialEffectsGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val animationName: String = "",
    val soundDefId: Int = 0,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040130u  // CHUNKID_GAME_OBJECT_DEF_SPECIAL_EFFECTS
    }
}

// Chunk IDs from specialeffectsgameobj.cpp local enum (CHUNKID_DEF_PARENT = 0x09010212)
private const val CHUNKID_DEF_VARIABLES = 0x09010213u

// Micro-chunk IDs
private const val VARID_DEF_ANIMATION_NAME = 1
private const val VARID_DEF_SOUNDID = 2

/**
 * Parses a SpecialEffectsGameObjDef from the OBJDATA chunk.
 * [name], [id], and [chunkId] are already extracted by the definition DB reader.
 */
fun parseSpecialEffectsGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): SpecialEffectsGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return SpecialEffectsGameObjDef(name = name, id = id, chunkId = chunkId)

    return SpecialEffectsGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        animationName = vars.readMicroString(VARID_DEF_ANIMATION_NAME) ?: "",
        soundDefId = vars.readMicroInt(VARID_DEF_SOUNDID) ?: 0,
    )
}
