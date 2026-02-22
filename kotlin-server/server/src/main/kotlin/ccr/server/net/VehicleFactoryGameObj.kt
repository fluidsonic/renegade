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
    var isBusy: Boolean = false; protected set
    private var endTimer: Float = UNINITIALIZED_TIMER

    // C++: VehicleFactoryGameObj::CnC_Initialize — register vehicle capability
    override fun cncInitialize(base: BaseControllerClass) {
        super.cncInitialize(base)
        base.setCanGenerateVehicles(true)
    }

    // C++: VehicleFactoryGameObj::On_Destroyed — revoke vehicle capability
    override fun onDestroyed() {
        super.onDestroyed()
        baseController?.setCanGenerateVehicles(false)
    }

    // C++: VehicleFactoryGameObj::Think — countdown generation timer
    override fun think(deltaSeconds: Float) {
        if (endTimer > UNINITIALIZED_TIMER) {
            endTimer -= deltaSeconds
            if (endTimer < 0f) {
                onGenerationComplete()
                endTimer = UNINITIALIZED_TIMER
            }
        }
        super.think(deltaSeconds)
    }

    fun requestVehicle(definitionId: Int, generationTime: Float) {
        if (!isBusy && definitionId != 0) {
            isBusy = true
            endTimer = generationTime  // C++ default: 12.0f * operationTimeFactor
            setObjectDirtyBit(ccr.net.replication.NetworkObject.BIT_RARE, true)
        }
    }

    private fun onGenerationComplete() {
        isBusy = false
        setObjectDirtyBit(ccr.net.replication.NetworkObject.BIT_RARE, true)
    }

    // C++: VehicleFactoryGameObj::Export_Rare — calls super then appends isBusy.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // BuildingGameObj: isDestroyed + isPowerOn + currentState
        packet.addBool(isBusy)
    }

    companion object {
        private const val UNINITIALIZED_TIMER = -100f
    }
}
