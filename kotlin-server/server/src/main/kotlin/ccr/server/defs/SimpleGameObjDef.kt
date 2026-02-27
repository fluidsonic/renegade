package ccr.server.defs

import ccr.server.mix.ChunkReader

// C++: SimpleGameObjDef : public PhysicalGameObjDef (simplegameobj.h)
open class SimpleGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val isEditorObject: Boolean = false,     // C++: bool IsEditorObject (initialized to false)
    val isHiddenObject: Boolean = false,     // C++: bool IsHiddenObject (initialized to false)
    val playerTerminalType: Int = -1,        // C++: PlayerTerminalClass::TYPE PlayerTerminalType (TYPE_NONE = -1)
    physDefId: Int = 0,                      // C++: int PhysDefID (inherited from PhysicalGameObjDef)
) : PhysicalGameObjDef(name, id, chunkId, physDefId = physDefId) {

    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_SIMPLE = CHUNKID_GAME_OBJECTS_BEGIN + 11 = 0x0004010B
        const val CHUNK_ID: UInt = 0x0004010Bu

        // C++: simplegameobj.cpp local enum (line 61)
        private const val CHUNKID_DEF_VARIABLES = 930991657u

        // C++: micro-chunk IDs from simplegameobj.cpp local enum (line 65-67)
        private const val MICROCHUNKID_DEF_IS_EDITOR_OBJECT = 1
        private const val MICROCHUNKID_DEF_IS_HIDDEN_OBJECT = 2
        private const val MICROCHUNKID_DEF_PLAYER_TERM_TYPE = 3

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SimpleGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
                ?: return SimpleGameObjDef(name = name, id = id, chunkId = chunkId)
            return SimpleGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                isEditorObject = vars.readMicroBool(MICROCHUNKID_DEF_IS_EDITOR_OBJECT) ?: false,
                isHiddenObject = vars.readMicroBool(MICROCHUNKID_DEF_IS_HIDDEN_OBJECT) ?: false,
                playerTerminalType = vars.readMicroInt(MICROCHUNKID_DEF_PLAYER_TERM_TYPE) ?: -1,
            )
        }
    }
}
