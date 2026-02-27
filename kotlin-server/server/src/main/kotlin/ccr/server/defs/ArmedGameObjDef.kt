package ccr.server.defs

// C++: ArmedGameObjDef : public PhysicalGameObjDef (armedgameobj.h)
open class ArmedGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val weaponDefId: Int = 0,             // C++: int WeaponDefID
    val secondaryWeaponDefId: Int = 0,    // C++: int SecondaryWeaponDefID
    val weaponRounds: Int = -1,           // C++: safe_int WeaponRounds
    val weaponTiltRate: Float = 1.0f,     // C++: float WeaponTiltRate
    val weaponTiltMin: Float = -10000f,   // C++: float WeaponTiltMin
    val weaponTiltMax: Float = 10000f,    // C++: float WeaponTiltMax
    val weaponTurnRate: Float = 1.0f,     // C++: float WeaponTurnRate
    val weaponTurnMin: Float = -10000f,   // C++: float WeaponTurnMin
    val weaponTurnMax: Float = 10000f,    // C++: float WeaponTurnMax
    val weaponError: Float = 0f,          // C++: float WeaponError
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
) : PhysicalGameObjDef(
    name, id, chunkId,
    type, radarBlipType, bullseyeOffsetZ, animation, physDefId,
    killedExplosion, defaultHibernationEnable, allowInnateConversations, oratorType, useCreationEffect,
    defenseObjectDef, infoIconTextureFilename, translatedNameId,
    notTargetable, defaultPlayerType, encyclopediaType, encyclopediaId,
    scriptNameList, scriptParameterList,
)
