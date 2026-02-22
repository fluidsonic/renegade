package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: VehicleFactoryGameObj (vehiclefactory.cpp) — extends BuildingGameObj.
// Export_Rare: calls super.exportRare(packet) then writes isBusy.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj → VehicleFactoryGameObj
open class VehicleFactoryGameObj(
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
    val isBusy: Boolean = false,
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
    // C++: VehicleFactoryGameObj::Export_Rare — calls super then appends isBusy.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // BuildingGameObj: isDestroyed + isPowerOn + currentState
        packet.addBool(isBusy)
    }
}
