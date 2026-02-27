package ccr.server.defs

// C++: PhysicalGameObjDef : public DamageableGameObjDef (physicalgameobj.h)
open class PhysicalGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val type: Int = 0,                            // C++: int Type
    val radarBlipType: Int = 0,                   // C++: int RadarBlipType
    val bullseyeOffsetZ: Float = 0f,              // C++: float BullseyeOffsetZ
    val animation: String = "",                   // C++: StringClass Animation
    val physDefId: Int = 0,                       // C++: int PhysDefID
    val killedExplosion: Int = 0,                 // C++: int KilledExplosion
    val defaultHibernationEnable: Boolean = false,// C++: bool DefaultHibernationEnable
    val allowInnateConversations: Boolean = false, // C++: bool AllowInnateConversations
    val oratorType: Int = 0,                      // C++: int OratorType
    val useCreationEffect: Boolean = false,       // C++: bool UseCreationEffect
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
) : DamageableGameObjDef(
    name, id, chunkId,
    defenseObjectDef, infoIconTextureFilename, translatedNameId,
    notTargetable, defaultPlayerType, encyclopediaType, encyclopediaId,
    scriptNameList, scriptParameterList,
)
