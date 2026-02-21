package ccr.server.net

import ccr.math.Vector3

// C++: SimpleGameObj (simplegameobj.cpp) — extends PhysicalGameObj.
// Sets AppPacketType = APPPACKETTYPE_SIMPLE.
// No additional Export_Creation/Rare/Occasional/Frequent beyond PhysicalGameObj.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj
open class SimpleGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
) : PhysicalGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType)
