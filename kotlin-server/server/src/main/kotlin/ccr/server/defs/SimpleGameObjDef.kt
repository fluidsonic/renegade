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
) : PhysicalGameObjDef(
    name, id, chunkId,
    type, radarBlipType, bullseyeOffsetZ, animation, physDefId,
    killedExplosion, defaultHibernationEnable, allowInnateConversations, oratorType, useCreationEffect,
    defenseObjectDef, infoIconTextureFilename, translatedNameId,
    notTargetable, defaultPlayerType, encyclopediaType, encyclopediaId,
    scriptNameList, scriptParameterList,
) {

    companion object {
        // C++: CHUNKID_GAME_OBJECT_DEF_SIMPLE = CHUNKID_GAME_OBJECTS_BEGIN + 11 = 0x0004010B
        const val CHUNK_ID: UInt = 0x0004010Bu

        // C++: simplegameobj.cpp local enum (line 61)
        private const val CHUNKID_DEF_PARENT = 930991656u
        private const val CHUNKID_DEF_VARIABLES = 930991657u

        // C++: micro-chunk IDs from simplegameobj.cpp local enum (line 65-67)
        private const val MICROCHUNKID_DEF_IS_EDITOR_OBJECT = 1
        private const val MICROCHUNKID_DEF_IS_HIDDEN_OBJECT = 2
        private const val MICROCHUNKID_DEF_PLAYER_TERM_TYPE = 3

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SimpleGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)

            // Navigate Simple parent → PhysicalGameObjDef::Save to load physical + damageable + scriptable fields
            val simpleParent = objDataReader.findChunk(CHUNKID_DEF_PARENT)
            val (physData, dmgParentChunk) = simpleParent?.let { PhysicalGameObjDefData.load(it) }
                ?: (PhysicalGameObjDefData() to null)
            val (dmgData, scriptParentChunk) = dmgParentChunk?.let { DamageableGameObjDefData.load(it) }
                ?: (DamageableGameObjDefData() to null)
            val (scriptableData, _) = scriptParentChunk?.let { ScriptableGameObjDefData.load(it) }
                ?: (ScriptableGameObjDefData(emptyList()) to null)

            return SimpleGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                isEditorObject = vars?.readMicroBool(MICROCHUNKID_DEF_IS_EDITOR_OBJECT) ?: false,
                isHiddenObject = vars?.readMicroBool(MICROCHUNKID_DEF_IS_HIDDEN_OBJECT) ?: false,
                playerTerminalType = vars?.readMicroInt(MICROCHUNKID_DEF_PLAYER_TERM_TYPE) ?: -1,
                // PhysicalGameObjDef fields
                physDefId = physData.physDefId,
                type = physData.type,
                radarBlipType = physData.radarBlipType,
                bullseyeOffsetZ = physData.bullseyeOffsetZ,
                animation = physData.animation,
                killedExplosion = physData.killedExplosion,
                defaultHibernationEnable = physData.defaultHibernationEnable,
                allowInnateConversations = physData.allowInnateConversations,
                oratorType = physData.oratorType,
                useCreationEffect = physData.useCreationEffect,
                // DamageableGameObjDef fields
                defenseObjectDef = dmgData.defenseObjectDef.toDefenseObjectDefClass(),
                translatedNameId = dmgData.translatedNameId,
                infoIconTextureFilename = dmgData.infoIconTextureFilename,
                notTargetable = dmgData.notTargetable,
                defaultPlayerType = dmgData.defaultPlayerType,
                encyclopediaType = dmgData.encyclopediaType,
                encyclopediaId = dmgData.encyclopediaId,
                // ScriptableGameObjDef fields
                scriptNameList = scriptableData.scripts.map { it.name },
                scriptParameterList = scriptableData.scripts.map { it.parameters },
            )
        }
    }
}
