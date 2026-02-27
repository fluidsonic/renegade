package ccr.server.defs

import ccr.server.mix.ChunkReader

// C++: SpecialEffectsGameObjDef : public PhysicalGameObjDef (specialeffectsgameobj.h)
open class SpecialEffectsGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val animationName: String = "",     // C++: StringClass AnimationName (initialized to "")
    val soundDefId: Int = 0,            // C++: int SoundDefID (initialized to 0)
) : PhysicalGameObjDef(name, id, chunkId) {

    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_SPECIAL_EFFECTS = 0x00040130 (combatchunkid.h)
        const val CHUNK_ID: UInt = 0x00040130u

        // C++: CLASSID_GAME_OBJECT_DEF_SPECIAL_EFFECTS = 0x3013 (combatchunkid.h)
        const val CLASS_ID: UInt = 0x3013u

        // C++: specialeffectsgameobj.cpp local enum starting at CHUNKID_DEF_PARENT = 0x09010212
        const val CHUNKID_DEF_PARENT: UInt    = 0x09010212u
        const val CHUNKID_DEF_VARIABLES: UInt = 0x09010213u

        // C++: micro-chunk IDs for Save_Variables / Load_Variables
        const val VARID_DEF_ANIMATION_NAME = 1
        const val VARID_DEF_SOUNDID        = 2

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SpecialEffectsGameObjDef {
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
    }
}
