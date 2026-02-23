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
) : DamageableGameObjDef(name, id, chunkId)
