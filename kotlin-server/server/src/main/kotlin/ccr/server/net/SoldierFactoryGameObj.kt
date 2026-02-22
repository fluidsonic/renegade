package ccr.server.net

import ccr.math.Vector3

// C++: SoldierFactoryGameObj (soldierfactory.cpp) — extends BuildingGameObj.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj → SoldierFactoryGameObj
class SoldierFactoryGameObj(
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
    playerType: Int = 0,
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
    playerType     = playerType,
) {
    // C++: SoldierFactoryGameObj::CnC_Initialize — register soldier capability
    override fun cncInitialize(base: BaseControllerClass) {
        super.cncInitialize(base)
        base.setCanGenerateSoldiers(true)
    }

    // C++: SoldierFactoryGameObj::On_Destroyed — revoke soldier capability
    override fun onDestroyed() {
        super.onDestroyed()
        baseController?.setCanGenerateSoldiers(false)
    }
}
