package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import ccr.server.GameServer
import ccr.server.defs.combat.BeaconGameObjDef

// C++: BeaconGameObj (beacongameobj.cpp) — extends SimpleGameObj.
// Export_Rare: calls super first, then appends state and ownerId.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → BeaconGameObj
class BeaconGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    initialState: Int = 0,
    initialOwnerId: Int = 0,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    // Mutable state fields
    var state: Int = initialState
    var ownerId: Int = initialOwnerId

    // Runtime fields — not serialised in the wire format; set by God after creation
    var beaconDef: BeaconGameObjDef? = null
    var serverRef: GameServer? = null
    var ownerRhostId: Int = 0
    var armTimer: Float = 0f
    var detonateTimer: Float = 0f

    // C++: BeaconGameObj::Think — drives arming countdown then detonation countdown.
    override fun think(deltaSeconds: Float) {
        thinkInternal(deltaSeconds)
    }

    internal fun thinkInternal(deltaSeconds: Float) {
        if (isDeletePending) return
        when (state) {
            STATE_ARMING -> {
                armTimer -= deltaSeconds
                if (armTimer <= 0f) {
                    state = STATE_ARMED
                    // carry overshoot: armTimer is now negative, detonateTime + armTimer = detonateTime - overshoot
                    detonateTimer = (beaconDef?.detonateTime ?: 30f) + armTimer
                    val inGame = serverRef?.god?.playerInGame ?: return
                    for (clientId in inGame) {
                        setObjectDirtyBit(clientId, NetworkObject.BIT_RARE, true)
                    }
                }
            }
            STATE_ARMED -> {
                detonateTimer -= deltaSeconds
                if (detonateTimer <= 0f) {
                    state = STATE_DETONATING
                    detonate()
                }
            }
        }
    }

    // C++: BeaconGameObj::Detonate — applies AoE building damage, broadcasts explosion event, marks for deletion.
    fun detonate() {
        val server = serverRef
        val def = beaconDef
        if (server == null || def == null) {
            cancel()
            return
        }

        // Remove from manager and mark pending BEFORE AoE so this object is not damaged by its own explosion
        server.gameObjManager.remove(this)
        setDeletePending()

        val explosionDefId = def.explosionDefId
        if (explosionDefId == 0) return

        ExplosionHelper.applyExplosionDamage(
            explosionDefId = explosionDefId,
            posX           = position.x,
            posY           = position.y,
            posZ           = position.z,
            ownerId        = ownerId,
            server         = server,
        )
    }

    // C++: BeaconGameObj removed without detonation (owner disconnect, defuse, etc.).
    fun cancel() {
        serverRef?.gameObjManager?.remove(this)
        setDeletePending()
    }

    // C++: BeaconGameObj::Export_Rare — calls super then appends state + ownerId.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addInt(state)
        packet.addInt(ownerId)
    }

    companion object {
        const val STATE_NULL       = 0
        const val STATE_ARMING     = 1
        const val STATE_ARMED      = 2
        const val STATE_DISARMED   = 3
        const val STATE_DETONATING = 4
    }
}
