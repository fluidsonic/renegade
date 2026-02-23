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
) : ArmedGameObjDef(name, id, chunkId)
