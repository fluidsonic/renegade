package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cScoreEvent — networkClassId = NETCLASSID_SCOREEVENT = 1029
// Client→Server event for score adjustments.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   Amount (int)
class ScoreEvent(
    var senderId: Int = 0,
    var amount: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1029

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(amount)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        amount = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] SCOREEVENT from rhostId=$rhostId senderId=$senderId amount=$amount (ignored — no god mode)")
        setDeletePending()
    }
}
