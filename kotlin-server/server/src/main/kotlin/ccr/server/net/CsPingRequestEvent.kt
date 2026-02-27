package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObjectManager
import ccr.server.GameServer

// C++: cCsPingRequestEvent — networkClassId = NETCLASSID_CSPINGREQUESTEVENT = 1032
// Client→Server ping request event used to measure round-trip latency.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   PingNumber (int)
class CsPingRequestEvent(
    var senderId: Int = 0,
    var pingNumber: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1032

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(pingNumber)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        pingNumber = packet.getInt()
    }

    override fun act(server: GameServer, rhostId: Int) {
        println("[GAME] CSPINGREQUESTEVENT from rhostId=$rhostId senderId=$senderId pingNumber=$pingNumber → ScPingResponseEvent")
        val host = server.connectionManager.getHost(rhostId) ?: run { setDeletePending(); return }
        val response = ScPingResponseEvent(pingNumber)
        server.sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, NetworkObjectManager.getNewDynamicId()) }
        setDeletePending()
    }
}
