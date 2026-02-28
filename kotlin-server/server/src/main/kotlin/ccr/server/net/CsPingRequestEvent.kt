package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

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
        actIfWiredUp()
    }

    override fun act() {
        println("[GAME] CSPINGREQUESTEVENT from senderId=$senderId pingNumber=$pingNumber → ScPingResponseEvent")
        val host = network.connectionManager.getHost(senderId) ?: run { setDeletePending(); return }
        val response = ScPingResponseEvent(pingNumber)
        network.serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, response.networkId) }
        setDeletePending()
    }
}
