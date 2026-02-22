package ccr.server.net

import ccr.math.Vector3

// C++: WarFactoryGameObj (warfactory.cpp) — extends VehicleFactoryGameObj.
// No additional Export methods beyond VehicleFactoryGameObj.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj
//                         → VehicleFactoryGameObj → WarFactoryGameObj
class WarFactoryGameObj(
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
    isBusy: Boolean = false,
    playerType: Int = 0,
) : VehicleFactoryGameObj(
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
    isBusy         = isBusy,
    playerType     = playerType,
)
