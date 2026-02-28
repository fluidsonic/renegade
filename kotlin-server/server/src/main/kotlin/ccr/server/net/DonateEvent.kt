package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cDonateEvent — networkClassId = NETCLASSID_DONATEEVENT = 1038
// Client→Server event for donating credits to another player.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   Amount (int)
//   RecipientId (int)
class DonateEvent(
    var senderId: Int = 0,
    var amount: Int = 0,
    var recipientId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1038

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(amount)
        packet.addInt(recipientId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        amount = packet.getInt()
        recipientId = packet.getInt()
        actIfWiredUp()
    }

    override fun act() {
        if (!network.gameState.isGameplayPermitted) { setDeletePending(); return }
        val sender = network.god.playersByHost.values.find { it.id == senderId }
        val recipient = network.god.playersByHost.values.find { it.id == recipientId }
        if (sender != null && recipient != null) {
            val donateAmount = amount.toFloat().coerceAtMost(sender.money)
            if (donateAmount > 0) {
                sender.addMoney(-donateAmount)
                recipient.addMoney(donateAmount)
                println("[GAME] DONATEEVENT from senderId=$senderId: ${sender.name} donated $donateAmount credits to ${recipient.name}")
            } else {
                println("[GAME] DONATEEVENT from senderId=$senderId: insufficient funds (has ${sender.money}, tried $amount)")
            }
        } else {
            println("[GAME] DONATEEVENT from senderId=$senderId: sender=$senderId or recipient=$recipientId not found")
        }
        setDeletePending()
    }
}
