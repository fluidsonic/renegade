package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network
import ccr.server.combat.ArmorWarheadManager

// C++: cCsDamageEvent — networkClassId = NETCLASSID_CSDAMAGEEVENT = 1033
// Client→Server event reporting damage dealt by this client.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   DamagerGOID (int)
//   DamageeGOID (int)
//   Damage (float)
//   Warhead (int)
class CsDamageEvent(
    var senderId: Int = 0,
    var damagerGoid: Int = 0,
    var damageeGoid: Int = 0,
    var damage: Float = 0f,
    var warhead: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1033

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(damagerGoid)
        packet.addInt(damageeGoid)
        packet.addFloat(damage)
        packet.addInt(warhead)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        damagerGoid = packet.getInt()
        damageeGoid = packet.getInt()
        damage = packet.getFloat()
        warhead = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        if (!server.gameState.isGameplayPermitted) { setDeletePending(); return }
        println("[GAME] CSDAMAGEEVENT from rhostId=$rhostId damagee=$damageeGoid damage=$damage warhead=$warhead")
        val target = server.gameObjManager.findObject(damageeGoid)
        if (target != null) {
            val scaledDamage = ArmorWarheadManager.scaleDamage(damage, warhead, target.shieldType)
            target.applyDamage(scaledDamage)
            println("[GAME] applied damage=$scaledDamage to netId=$damageeGoid health=${target.health}")
            if (target.isDead) {
                val victimRhostId = server.god.soldiersByHost.entries.find { it.value.networkId == damageeGoid }?.key
                if (victimRhostId != null) {
                    server.broadcastPlayerKill(rhostId, victimRhostId)
                    server.god.deleteSoldier(victimRhostId)
                }
            }
        } else {
            println("[GAME] CSDAMAGEEVENT: target netId=$damageeGoid not found in gameObjManager")
        }
        setDeletePending()
    }
}
