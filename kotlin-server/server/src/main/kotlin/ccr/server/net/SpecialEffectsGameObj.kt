package ccr.server.net

import ccr.math.Vector3

// C++: SpecialEffectsGameObj — extends PhysicalGameObj.
// No Export_Creation/Rare override beyond PhysicalGameObj.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SpecialEffectsGameObj
class SpecialEffectsGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
) : PhysicalGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType)
