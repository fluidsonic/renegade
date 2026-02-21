package ccr.server.net

import ccr.math.Vector3

// C++: RefineryGameObj (refinery.cpp) — extends BuildingGameObj.
// Export_Rare body calls super only — no additional fields beyond BuildingGameObj.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj → RefineryGameObj
class RefineryGameObj(
    definitionId: Int,
    position: Vector3,
    sphereCenter: Vector3,
    sphereRadius: Float,
    health: Float = 5000f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    isDestroyed: Boolean = false,
    isPowerOn: Boolean = true,
    currentState: Int = 0,
) : BuildingGameObj(
    definitionId   = definitionId,
    position       = position,
    sphereCenter   = sphereCenter,
    sphereRadius   = sphereRadius,
    health         = health,
    shieldStrength = shieldStrength,
    shieldType     = shieldType,
    isDestroyed    = isDestroyed,
    isPowerOn      = isPowerOn,
    currentState   = currentState,
)
