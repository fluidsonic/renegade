package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cMoneyEvent — networkClassId = NETCLASSID_MONEYEVENT = 1021
// Client→Server event for money transactions.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   Amount (int)
class MoneyEvent(
    var senderId: Int = 0,
    var amount: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1021

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(amount)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        amount = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] MONEYEVENT from rhostId=$rhostId senderId=$senderId amount=$amount (ignored — no god mode)")
        setDeletePending()
    }
}
