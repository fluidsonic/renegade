package ccr.server.net

import ccr.math.Vector3

// C++: RefineryGameObj (refinery.cpp) — extends BuildingGameObj.
// Manages passive credit trickle to teammates.
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
    // Passive credit trickle rate (credits per second, per teammate)
    var fundsDistributedPerSec: Float = 2f

    // Harvester management — set from RefineryGameObjDef.harvesterDefId at init
    var harvesterDefId: Int = 0
    var harvesterVehicle: VehicleGameObj? = null

    private var distributionTimer: Float = 1f

    // C++: RefineryGameObj::Think — request harvester if needed, then distribute passive trickle
    override fun think(deltaSeconds: Float) {
        if (!isDestroyed) {
            // Clear stale harvester reference if the vehicle was destroyed
            if (harvesterVehicle != null && harvesterVehicle!!.isDeletePending) {
                harvesterVehicle = null
            }

            // Request a new harvester if we don't have one
            if (harvesterVehicle == null && harvesterDefId != 0) {
                baseController?.requestHarvester(harvesterDefId)
            }

            distributionTimer -= deltaSeconds
            if (distributionTimer <= 0f) {
                distributionTimer = 1f
                val ctrl = baseController ?: return
                val opFactor = ctrl.operationTimeFactor
                val funds = (fundsDistributedPerSec / opFactor).toInt()
                val ctx = gameContext ?: return
                ctrl.distributeFundsToEachTeammate(funds, ctx.gameObjManager.getStarList())
            }
        }
        super.think(deltaSeconds)
    }

    // C++: RefineryGameObj::On_Destroyed — kill harvester when refinery goes down
    override fun onDestroyed() {
        super.onDestroyed()
        harvesterVehicle?.let { harv ->
            if (!harv.isDeletePending) {
                harv.applyDamage(harv.health + harv.shieldStrength + 1f)
            }
        }
        harvesterVehicle = null
    }
}
