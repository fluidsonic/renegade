package ccr.server.defs.combat

import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of SimpleGameObjDef (Combat/simplegameobj.cpp).
 *
 * C++ defaults: IsEditorObject(false), IsHiddenObject(false),
 *               PlayerTerminalType(PlayerTerminalClass::TYPE_NONE = -1)
 */
data class SimpleGameObjDef(
    val name: String,
    val id: UInt,
    val classId: UInt,
    val isEditorObject: Boolean = false,
    val isHiddenObject: Boolean = false,
    val playerTerminalType: Int = -1,
) {
    companion object {
        /** CLASSID_GAME_OBJECT_DEF_SIMPLE = CLASSID_GAME_OBJECTS + 4 = 0x3004 */
        const val CLASS_ID: UInt = 0x3004u
    }
}

// Chunk IDs from simplegameobj.cpp local enum (line 61)
private const val CHUNKID_DEF_VARIABLES = 930991657u

// Micro-chunk IDs from simplegameobj.cpp local enum (line 65-67)
private const val MICROCHUNKID_DEF_IS_EDITOR_OBJECT = 1
private const val MICROCHUNKID_DEF_IS_HIDDEN_OBJECT = 2
private const val MICROCHUNKID_DEF_PLAYER_TERM_TYPE = 3

/**
 * Parses a SimpleGameObjDef from the OBJDATA chunk.
 * [name], [id], and [classId] are already extracted by the definition DB reader.
 */
fun parseSimpleGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): SimpleGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return SimpleGameObjDef(name = name, id = id, classId = classId)

    return SimpleGameObjDef(
        name = name,
        id = id,
        classId = classId,
        isEditorObject = vars.readMicroBool(MICROCHUNKID_DEF_IS_EDITOR_OBJECT) ?: false,
        isHiddenObject = vars.readMicroBool(MICROCHUNKID_DEF_IS_HIDDEN_OBJECT) ?: false,
        playerTerminalType = vars.readMicroInt(MICROCHUNKID_DEF_PLAYER_TERM_TYPE) ?: -1,
    )
}
