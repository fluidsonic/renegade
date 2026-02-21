package ccr.server.net

import ccr.math.Vector3

// C++: PowerUpGameObj — extends SimpleGameObj.
// Is_Always_Dirty returns false — no Export_Creation/Rare/Occasional/Frequent overrides.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → PowerUpGameObj
class PowerUpGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType)
