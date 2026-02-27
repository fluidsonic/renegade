package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of SAMSiteGameObjDef (Combat/samsite.h).
 *
 * C++ hierarchy: SAMSiteGameObjDef : SmartGameObjDef : ArmedGameObjDef :
 *   PhysicalGameObjDef : DamageableGameObjDef : ScriptableGameObjDef :
 *   BaseGameObjDef : DefinitionClass
 *
 * SAMSiteGameObjDef adds NO persisted fields of its own. Its Save() simply
 * wraps SmartGameObjDef::Save() in CHUNKID_DEF_PARENT (930991800).
 */
class SAMSiteGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    // SmartGameObjDef fields (forwarded)
    sightRange: Float = 0f,
    sightArc: Float = 0f,
    listenerScale: Float = 1.0f,
    isStealthUnit: Boolean = false,
    // ArmedGameObjDef fields (forwarded)
    weaponDefId: Int = 0,
    secondaryWeaponDefId: Int = 0,
    weaponRounds: Int = -1,
    weaponTiltRate: Float = 1.0f,
    weaponTiltMin: Float = -10000f,
    weaponTiltMax: Float = 10000f,
    weaponTurnRate: Float = 1.0f,
    weaponTurnMin: Float = -10000f,
    weaponTurnMax: Float = 10000f,
    weaponError: Float = 0f,
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
    translatedNameId: Int = 0,
    infoIconTextureFilename: String = "",
    notTargetable: Boolean = false,
    defaultPlayerType: Int = -2,
    encyclopediaType: Int = 0,
    encyclopediaId: Int = 0,
    // ScriptableGameObjDef fields (forwarded)
    scriptNameList: List<String> = emptyList(),
    scriptParameterList: List<String> = emptyList(),
) : SmartGameObjDef(
    name, id, chunkId,
    sightRange, sightArc, listenerScale, isStealthUnit,
    weaponDefId, secondaryWeaponDefId, weaponRounds,
    weaponTiltRate, weaponTiltMin, weaponTiltMax,
    weaponTurnRate, weaponTurnMin, weaponTurnMax,
    weaponError,
    type, radarBlipType, bullseyeOffsetZ, animation, physDefId,
    killedExplosion, defaultHibernationEnable, allowInnateConversations, oratorType, useCreationEffect,
    defenseObjectDef, infoIconTextureFilename, translatedNameId,
    notTargetable, defaultPlayerType, encyclopediaType, encyclopediaId,
    scriptNameList, scriptParameterList,
) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040109u  // CHUNKID_GAME_OBJECT_DEF_SAMSITE

        // SAMSiteGameObjDef CHUNKID_DEF_PARENT wraps SmartGameObjDef::Save
        private const val CHUNKID_DEF_PARENT = 930991800u

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): SAMSiteGameObjDef {
            val samParent = objDataReader.findChunk(CHUNKID_DEF_PARENT)
                ?: return SAMSiteGameObjDef(name = name, id = id, chunkId = chunkId)
            val (smartData, armedParentChunk) = SmartGameObjDefData.load(samParent)
                ?: return SAMSiteGameObjDef(name = name, id = id, chunkId = chunkId)
            val (armedData, physParentChunk) = armedParentChunk?.let { ArmedGameObjDefData.load(it) }
                ?: (ArmedGameObjDefData() to null)
            val (physData, dmgParentChunk) = physParentChunk?.let { PhysicalGameObjDefData.load(it) }
                ?: (PhysicalGameObjDefData() to null)
            val (dmgData, scriptParentChunk) = dmgParentChunk?.let { DamageableGameObjDefData.load(it) }
                ?: (DamageableGameObjDefData() to null)
            val (scriptableData, _) = scriptParentChunk?.let { ScriptableGameObjDefData.load(it) }
                ?: (ScriptableGameObjDefData(emptyList()) to null)

            return SAMSiteGameObjDef(
                name = name, id = id, chunkId = chunkId,
                // SmartGameObjDef fields
                sightRange = smartData.sightRange,
                sightArc = smartData.sightArc,
                listenerScale = smartData.listenerScale,
                isStealthUnit = smartData.isStealthUnit,
                // ArmedGameObjDef fields
                weaponDefId = armedData.weaponDefId,
                secondaryWeaponDefId = armedData.secondaryWeaponDefId,
                weaponRounds = armedData.weaponRounds,
                weaponTiltRate = armedData.weaponTiltRate,
                weaponTiltMin = armedData.weaponTiltMin,
                weaponTiltMax = armedData.weaponTiltMax,
                weaponTurnRate = armedData.weaponTurnRate,
                weaponTurnMin = armedData.weaponTurnMin,
                weaponTurnMax = armedData.weaponTurnMax,
                weaponError = armedData.weaponError,
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
