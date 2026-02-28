package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

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
        actIfWiredUp()
    }

    override fun act() {
        if (!network.gameState.isGameplayPermitted) { setDeletePending(); return }
        println("[GAME] CSDAMAGEEVENT from senderId=$senderId damager=$damagerGoid damagee=$damageeGoid damage=$damage warhead=$warhead")
        // C++: cCsDamageEvent::Act — both target and damager must be PhysicalGameObj;
        // damager must also be an ArmedGameObj, otherwise damage is silently rejected.
        val target = network.gameObjManager.findPhysicalGameObj(damageeGoid)
        if (target != null) {
            val damager = network.gameObjManager.findPhysicalGameObj(damagerGoid)
            if (damager !is ArmedGameObj) { setDeletePending(); return }
            // C++: OffenseObjectClass offense(Damage, Warhead, damager->As_ArmedGameObj())
            // Raw damage — scaling happens inside Apply_Damage_Extended via DefenseObjectClass.
            val offense = OffenseObjectClass(damage, warhead, damager)
            offense.forceServerDamage = true
            target.applyDamageExtended(offense)
        }
        setDeletePending()
    }
}
