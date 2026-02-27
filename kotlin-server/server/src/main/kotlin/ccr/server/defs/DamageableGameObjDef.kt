package ccr.server.defs

// C++: DamageableGameObjDef : public ScriptableGameObjDef (damageablegameobj.h)
open class DamageableGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val defenseObjectDef: DefenseObjectDefClass = DefenseObjectDefClass(),  // C++: DefenseObjectDefClass DefenseObjectDef
    val infoIconTextureFilename: String = "",                               // C++: StringClass InfoIconTextureFilename
    val translatedNameId: Int = 0,                                         // C++: int TranslatedNameID
    val notTargetable: Boolean = false,                                     // C++: bool NotTargetable
    val defaultPlayerType: Int = -2,                                        // C++: int DefaultPlayerType (PLAYERTYPE_NEUTRAL)
    val encyclopediaType: Int = 0,                                          // C++: int EncyclopediaType
    val encyclopediaId: Int = 0,                                            // C++: int EncyclopediaID
) : ScriptableGameObjDef(name, id, chunkId)
