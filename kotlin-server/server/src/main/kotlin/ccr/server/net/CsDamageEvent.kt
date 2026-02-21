package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cCsDamageEvent — classId = NETCLASSID_CSDAMAGEEVENT = 1033
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
}
