package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObjectManager
import ccr.server.GameServer
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_REMOTE
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_TIMED
import ccr.server.combat.ArmorWarheadManager
import ccr.server.defs.ExplosionDefinitionClass

// C++: C4GameObj (c4gameobj.cpp) — extends SimpleGameObj.
// Export_Rare appends C4-specific fields after the SimpleGameObj/PhysicalGameObj chain.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → C4GameObj
class C4GameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    // C4-specific rare fields
    var ammoDef: Int = 0,
    var ownerId: Int = 0,
    var velX: Float = 0f,
    var velY: Float = 0f,
    var velZ: Float = 0f,
    var stuck: Boolean = false,
    var stuckPosX: Float = 0f,
    var stuckPosY: Float = 0f,
    var stuckPosZ: Float = 0f,
    var stuckMct: Boolean = false,
    var stuckToObject: Boolean = false,
    var stuckObjectId: Int = 0,
    var stuckOffsetX: Float = 0f,
    var stuckOffsetY: Float = 0f,
    var stuckOffsetZ: Float = 0f,
    var stuckBone: Int = 0,
    var stuckStaticAnim: Boolean = false,
    var stuckStaticAnimObjId: Int = 0,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    // Runtime fields — not serialized in the wire format
    var timer: Float = 0f
    var age: Float = 0f
    var detonationMode: Int = 1
    var ownerRhostId: Int = 0
    var ammoDefinition: AmmoDefinitionClass? = null
    var stuckBuilding: BuildingGameObj? = null
    var serverRef: GameServer? = null

    // C++: C4GameObj::Think — drives timed countdown and remote detonation trigger.
    override fun think(deltaSeconds: Float) {
        if (isDeletePending) return
        age += deltaSeconds
        val ammoDef = ammoDefinition ?: return
        when (ammoDef.ammoType) {
            AMMO_TYPE_C4_TIMED -> {
                timer -= deltaSeconds
                if (timer <= 0f) detonate()
            }
            AMMO_TYPE_C4_REMOTE -> {
                val server = serverRef ?: return
                val soldier = server.god.soldiersByHost[ownerRhostId]
                if (soldier == null) {
                    // Owner disconnected — defuse without explosion
                    defuse()
                } else if (soldier.detonateC4) {
                    detonate()
                }
            }
        }
    }

    // C++: C4GameObj::Detonate — applies building damage, broadcasts explosion event, marks for deletion.
    fun detonate() {
        val server = serverRef
        val ammoDef = ammoDefinition
        val explosionDefId = ammoDef?.explosionDefId ?: 0

        if (server != null && stuckBuilding != null && explosionDefId != 0) {
            val explosionDef = server.loadedLevel?.definitions?.findById(explosionDefId.toUInt())
                as? ExplosionDefinitionClass
            if (explosionDef != null) {
                val warheadSaveId = ammoDefinition?.warhead ?: 0
                val building = stuckBuilding!!
                val effectiveArmorSaveId = if (stuckMct) building.mctSkinSaveId else building.shieldType
                val damage = ArmorWarheadManager.scaleDamage(
                    explosionDef.damageStrength, warheadSaveId, effectiveArmorSaveId)
                building.applyDamage(damage)
            }
        }

        // Broadcast explosion visual/sound to all in-game clients
        if (server != null && explosionDefId != 0) {
            val explosion = ScExplosionEvent(
                defId    = explosionDefId,
                posX     = stuckPosX,
                posY     = stuckPosY,
                posZ     = stuckPosZ,
                ownerId  = ownerId,
            )
            for (clientId in server.god.playerInGame) {
                val host = server.connectionManager.getHost(clientId) ?: continue
                server.sendGameNetObj(host) { bs ->
                    NetworkObjectPacketWriter.writeCreation(bs, explosion, NetworkObjectManager.getNewDynamicId())
                }
            }
        }

        server?.gameObjManager?.remove(this)
        setDeletePending()
    }

    // C++: C4GameObj::Defuse — removes C4 without damage or explosion.
    fun defuse() {
        serverRef?.gameObjManager?.remove(this)
        setDeletePending()
    }

    // C++: C4GameObj::Export_Rare — calls super then appends C4 fields.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addInt(ammoDef)
        packet.addInt(ownerId)
        packet.addFloat(velX, BITPACK_VEHICLE_VELOCITY)
        packet.addFloat(velY, BITPACK_VEHICLE_VELOCITY)
        packet.addFloat(velZ, BITPACK_VEHICLE_VELOCITY)
        packet.addBool(stuck)
        if (stuck) {
            packet.addFloat(stuckPosX, BITPACK_WORLD_POSITION_X)
            packet.addFloat(stuckPosY, BITPACK_WORLD_POSITION_Y)
            packet.addFloat(stuckPosZ, BITPACK_WORLD_POSITION_Z)
            packet.addBool(stuckMct)
            packet.addBool(stuckToObject)
            packet.addInt(stuckObjectId)
            if (stuckToObject) {
                packet.addFloat(stuckOffsetX, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(stuckOffsetY, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(stuckOffsetZ, BITPACK_VEHICLE_VELOCITY)
                packet.addInt(stuckBone)
            }
            packet.addBool(stuckStaticAnim)
            if (stuckStaticAnim) {
                packet.addInt(stuckStaticAnimObjId)
            }
        }
    }

    companion object {
        const val C4_LIMIT = 30
    }
}
