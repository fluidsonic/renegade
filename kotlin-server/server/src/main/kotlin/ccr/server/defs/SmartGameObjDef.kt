package ccr.server.defs

// C++: SmartGameObjDef : public ArmedGameObjDef (smartgameobj.h)
open class SmartGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val sightRange: Float = 0f,           // C++: float SightRange
    val sightArc: Float = 0f,             // C++: float SightArc
    val listenerScale: Float = 1.0f,      // C++: float ListenerScale
    val isStealthUnit: Boolean = false,   // C++: bool IsStealthUnit
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
    type: Int = 0,
    radarBlipType: Int = 0,
    bullseyeOffsetZ: Float = 0f,
    animation: String = "",
    physDefId: Int = 0,
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
) : ArmedGameObjDef(
    name, id, chunkId,
    weaponDefId, secondaryWeaponDefId, weaponRounds,
    weaponTiltRate, weaponTiltMin, weaponTiltMax,
    weaponTurnRate, weaponTurnMin, weaponTurnMax,
    weaponError,
    type, radarBlipType, bullseyeOffsetZ, animation, physDefId,
    killedExplosion, defaultHibernationEnable, allowInnateConversations, oratorType, useCreationEffect,
    defenseObjectDef, infoIconTextureFilename, translatedNameId,
    notTargetable, defaultPlayerType, encyclopediaType, encyclopediaId,
    scriptNameList, scriptParameterList,
)
