package ccr.server.defs

// C++: ScriptableGameObjDef : public BaseGameObjDef (scriptablegameobj.h)
open class ScriptableGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt = 0u,
    val scriptNameList: List<String> = emptyList(),
    val scriptParameterList: List<String> = emptyList(),
) : BaseGameObjDef(name, id, chunkId)
