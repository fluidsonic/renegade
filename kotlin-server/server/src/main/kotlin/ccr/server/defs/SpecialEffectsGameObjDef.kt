package ccr.server.defs

import ccr.server.mix.ChunkReader

// C++: SpecialEffectsGameObjDef : public PhysicalGameObjDef (specialeffectsgameobj.h)
open class SpecialEffectsGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val animationName: String = "",     // C++: StringClass AnimationName (initialized to "")
    val soundDefId: Int = 0,            // C++: int SoundDefID (initialized to 0)
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

            // Navigate Special's parent → PhysicalGameObjDef::Save to load physical + damageable + scriptable fields
            val specialParent = objDataReader.findChunk(CHUNKID_DEF_PARENT)
            val (physData, dmgParentChunk) = specialParent?.let { PhysicalGameObjDefData.load(it) }
                ?: (PhysicalGameObjDefData() to null)
            val (dmgData, scriptParentChunk) = dmgParentChunk?.let { DamageableGameObjDefData.load(it) }
                ?: (DamageableGameObjDefData() to null)
            val (scriptableData, _) = scriptParentChunk?.let { ScriptableGameObjDefData.load(it) }
                ?: (ScriptableGameObjDefData(emptyList()) to null)

            return SpecialEffectsGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                animationName = vars?.readMicroString(VARID_DEF_ANIMATION_NAME) ?: "",
                soundDefId = vars?.readMicroInt(VARID_DEF_SOUNDID) ?: 0,
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
