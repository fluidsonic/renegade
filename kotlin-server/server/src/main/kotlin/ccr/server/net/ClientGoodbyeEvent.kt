package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cClientGoodbyeEvent — networkClassId = NETCLASSID_CLIENTGOODBYEEVENT = 1024
// Client→Server event sent when a client disconnects gracefully.
// Wire format (BIT_CREATION):
//   SenderId (int)
class ClientGoodbyeEvent(
    var senderId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1024

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] CLIENTGOODBYE from rhostId=$rhostId senderId=$senderId — removing player")
        server.flowControllers.remove(rhostId)
        server.god.removePlayer(rhostId)
        setDeletePending()
    }
}
