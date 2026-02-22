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
    val isAvailable: Boolean get() = !isBusy && !isDestroyed
    private var endTimer: Float = UNINITIALIZED_TIMER

    // Pending order — set by requestVehicle(), consumed by onGenerationComplete()
    var pendingDefId: Int = 0; private set
    var pendingBuyerRhostId: Int = -1; private set

    // Called when vehicle generation completes: (defId, buyerRhostId) → spawn the vehicle
    var onVehicleReady: ((defId: Int, buyerRhostId: Int) -> Unit)? = null

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
                endTimer = UNINITIALIZED_TIMER
                onGenerationComplete()
            }
        }
        super.think(deltaSeconds)
    }

    fun requestVehicle(definitionId: Int, generationTime: Float, buyerRhostId: Int) {
        if (!isBusy && definitionId != 0) {
            isBusy = true
            pendingDefId = definitionId
            pendingBuyerRhostId = buyerRhostId
            endTimer = generationTime  // C++ default: 12.0f * operationTimeFactor
            setObjectDirtyBit(ccr.net.replication.NetworkObject.BIT_RARE, true)
        }
    }

    private fun onGenerationComplete() {
        onVehicleReady?.invoke(pendingDefId, pendingBuyerRhostId)
        pendingDefId = 0
        pendingBuyerRhostId = -1
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
