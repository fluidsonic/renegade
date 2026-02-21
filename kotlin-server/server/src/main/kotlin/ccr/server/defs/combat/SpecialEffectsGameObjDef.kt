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
    classId: UInt,
    val animationName: String = "",
    val soundDefId: Int = 0,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_SPECIAL_EFFECTS = CLASSID_GAME_OBJECTS + 19 = 0x3013 */
        const val CLASS_ID: UInt = 0x3013u
    }
}

// Chunk IDs from specialeffectsgameobj.cpp local enum (CHUNKID_DEF_PARENT = 0x09010212)
private const val CHUNKID_DEF_VARIABLES = 0x09010213u

// Micro-chunk IDs
private const val VARID_DEF_ANIMATION_NAME = 1
private const val VARID_DEF_SOUNDID = 2

/**
 * Parses a SpecialEffectsGameObjDef from the OBJDATA chunk.
 * [name], [id], and [classId] are already extracted by the definition DB reader.
 */
fun parseSpecialEffectsGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): SpecialEffectsGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return SpecialEffectsGameObjDef(name = name, id = id, classId = classId)

    return SpecialEffectsGameObjDef(
        name = name,
        id = id,
        classId = classId,
        animationName = vars.readMicroString(VARID_DEF_ANIMATION_NAME) ?: "",
        soundDefId = vars.readMicroInt(VARID_DEF_SOUNDID) ?: 0,
    )
}
