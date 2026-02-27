package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// C++: C4GameObjDef : public SimpleGameObjDef : public PhysicalGameObjDef (c4.h / c4.cpp)
open class C4GameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    // C++: float ThrowVelocity (initialized to 5)
    val throwVelocity: Float = 5f,
    // SimpleGameObjDef fields (forwarded)
    isEditorObject: Boolean = false,
    isHiddenObject: Boolean = false,
    playerTerminalType: Int = -1,
    // PhysicalGameObjDef fields (forwarded)
    physDefId: Int = 0,
    type: Int = 0,
    radarBlipType: Int = 0,
    bullseyeOffsetZ: Float = 0f,
    animation: String = "",
    killedExplosion: Int = 0,
    defaultHibernationEnable: Boolean = false,
    allowInnateConversations: Boolean = false,
    oratorType: Int = 0,
    useCreationEffect: Boolean = false,
    // DamageableGameObjDef fields (forwarded)
    defenseObjectDef: DefenseObjectDefClass = DefenseObjectDefClass(),
    infoIconTextureFilename: String = "",
    translatedNameId: Int = 0,
    notTargetable: Boolean = false,
    defaultPlayerType: Int = -2,
    encyclopediaType: Int = 0,
    encyclopediaId: Int = 0,
    // ScriptableGameObjDef fields (forwarded)
    scriptNameList: List<String> = emptyList(),
    scriptParameterList: List<String> = emptyList(),
) : SimpleGameObjDef(
    name = name,
    id = id,
    chunkId = chunkId,
    isEditorObject = isEditorObject,
    isHiddenObject = isHiddenObject,
    playerTerminalType = playerTerminalType,
    physDefId = physDefId,
    type = type,
    radarBlipType = radarBlipType,
    bullseyeOffsetZ = bullseyeOffsetZ,
    animation = animation,
    killedExplosion = killedExplosion,
    defaultHibernationEnable = defaultHibernationEnable,
    allowInnateConversations = allowInnateConversations,
    oratorType = oratorType,
    useCreationEffect = useCreationEffect,
    defenseObjectDef = defenseObjectDef,
    infoIconTextureFilename = infoIconTextureFilename,
    translatedNameId = translatedNameId,
    notTargetable = notTargetable,
    defaultPlayerType = defaultPlayerType,
    encyclopediaType = encyclopediaType,
    encyclopediaId = encyclopediaId,
    scriptNameList = scriptNameList,
    scriptParameterList = scriptParameterList,
) {
    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_C4 — from combatchunkid.h
        const val CHUNK_ID: UInt = 0x00040103u

        // C++: c4.cpp local enum starting at 930991700
        const val CHUNKID_DEF_PARENT    = 930991700
        const val CHUNKID_DEF_VARIABLES = 930991701

        // C++: micro-chunk IDs inside CHUNKID_DEF_VARIABLES
        const val MICROCHUNKID_DEF_THROW_VELOCITY = 1

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): C4GameObjDef {
            // Load throwVelocity from C4's own vars
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES.toUInt())
            val throwVelocity = vars?.findMicroChunk(MICROCHUNKID_DEF_THROW_VELOCITY)?.let { bytes ->
                if (bytes.size >= 4) ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float else null
            } ?: 5f

            // Navigate into C4 parent to load SimpleGameObjDef chain
            val c4Parent = objDataReader.findChunk(CHUNKID_DEF_PARENT.toUInt())
            val base = if (c4Parent != null) {
                SimpleGameObjDef.load(c4Parent, name, id, chunkId)
            } else {
                SimpleGameObjDef(name = name, id = id, chunkId = chunkId)
            }

            return C4GameObjDef(
                name = name, id = id, chunkId = chunkId,
                throwVelocity = throwVelocity,
                // SimpleGameObjDef own fields
                isEditorObject = base.isEditorObject,
                isHiddenObject = base.isHiddenObject,
                playerTerminalType = base.playerTerminalType,
                // PhysicalGameObjDef fields
                physDefId = base.physDefId,
                type = base.type,
                radarBlipType = base.radarBlipType,
                bullseyeOffsetZ = base.bullseyeOffsetZ,
                animation = base.animation,
                killedExplosion = base.killedExplosion,
                defaultHibernationEnable = base.defaultHibernationEnable,
                allowInnateConversations = base.allowInnateConversations,
                oratorType = base.oratorType,
                useCreationEffect = base.useCreationEffect,
                // DamageableGameObjDef fields
                defenseObjectDef = base.defenseObjectDef,
                infoIconTextureFilename = base.infoIconTextureFilename,
                translatedNameId = base.translatedNameId,
                notTargetable = base.notTargetable,
                defaultPlayerType = base.defaultPlayerType,
                encyclopediaType = base.encyclopediaType,
                encyclopediaId = base.encyclopediaId,
                // ScriptableGameObjDef fields
                scriptNameList = base.scriptNameList,
                scriptParameterList = base.scriptParameterList,
            )
        }
    }
}
