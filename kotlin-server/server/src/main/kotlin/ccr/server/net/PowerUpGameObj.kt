package ccr.server.net

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.server.GameServer
import ccr.server.defs.combat.PowerUpGameObjDef

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
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    /** The definition that specifies what this powerup grants. Set by GameServer after creation. */
    var powerUpDef: PowerUpGameObjDef? = null

    /** Reference to the server — used by think() to iterate live soldiers. */
    var serverRef: GameServer? = null

    private var granted = false

    // C++: PowerUpGameObj::Think — checks bounding-box overlap with soldiers, calls Grant() on hit.
    override fun think(deltaSeconds: Float) {
        super.think(deltaSeconds)
        if (isDeletePending || granted) return
        val server = serverRef ?: return
        val soldiers = server.god.soldiersByHost.values.toList()
        val inGameClientIds = server.god.playerInGame.toSet()
        thinkWithSoldiers(soldiers, inGameClientIds, deltaSeconds)
    }

    /**
     * Testable inner logic — separated from GameServer dependency.
     * Scans [soldiers] for any within PICKUP_RADIUS_M and calls [grant] on the first found.
     */
    internal fun thinkWithSoldiers(
        soldiers: List<SoldierGameObj>,
        inGameClientIds: Set<Int>,
        deltaSeconds: Float,
    ) {
        if (isDeletePending || granted) return
        val def = powerUpDef ?: return

        for (soldier in soldiers) {
            val dx = soldier.position.x - position.x
            val dy = soldier.position.y - position.y
            val dz = soldier.position.z - position.z
            if (dx * dx + dy * dy + dz * dz <= PICKUP_RADIUS_SQ) {
                grant(soldier, inGameClientIds)
                return
            }
        }
    }

    /**
     * Grants this powerup to [soldier].
     * C++: PowerUpGameObj::Grant(SoldierGameObj*) — powerup.cpp.
     *
     * @param inGameClientIds  set of client IDs to mark dirty bits for (empty in tests)
     */
    fun grant(soldier: SoldierGameObj, inGameClientIds: Set<Int>) {
        if (granted) return
        granted = true
        val def = powerUpDef ?: run {
            setDeletePending()
            return
        }

        var healthOrShieldChanged = false

        // Health grant
        if (def.grantHealth > 0f) {
            soldier.health = minOf(soldier.health + def.grantHealth, soldier.healthMax)
            healthOrShieldChanged = true
        }

        // Shield grant
        if (def.grantShieldStrength > 0f) {
            soldier.shieldStrength = minOf(soldier.shieldStrength + def.grantShieldStrength, soldier.shieldStrengthMax)
            healthOrShieldChanged = true
        }

        if (healthOrShieldChanged) {
            for (clientId in inGameClientIds) {
                soldier.setObjectDirtyBit(clientId, NetworkObject.BIT_OCCASIONAL, true)
            }
        }

        // Weapon grant
        if (def.grantWeapon && def.grantWeaponId != 0) {
            soldier.weapons.add(WeaponEntry(def.grantWeaponId, def.grantWeaponRounds))
            for (clientId in inGameClientIds) {
                soldier.setObjectDirtyBit(clientId, NetworkObject.BIT_RARE, true)
            }
        }

        println("[POWERUP] granted '${def.name}' to soldier netId=${soldier.networkId}: " +
            "health+=${def.grantHealth} shield+=${def.grantShieldStrength} weapon=${if (def.grantWeapon && def.grantWeaponId != 0) def.grantWeaponId else 0}")

        // Phase 6: always delete after grant (persistent not implemented until Phase 11)
        setDeletePending()
    }

    companion object {
        private const val PICKUP_RADIUS_M  = 3.0f
        private const val PICKUP_RADIUS_SQ = PICKUP_RADIUS_M * PICKUP_RADIUS_M  // 9.0f
    }
}
