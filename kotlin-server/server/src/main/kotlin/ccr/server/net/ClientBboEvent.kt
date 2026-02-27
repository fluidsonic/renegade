package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cClientBboEvent — networkClassId = NETCLASSID_CLIENTBBOEVENT = 1030
// Client→Server event reporting the client's bandwidth/backlog (BBO) value.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   Bbo (int)
class ClientBboEvent(
    var senderId: Int = 0,
    var bbo: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1030

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(bbo)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        bbo = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] CLIENTBBOEVENT from rhostId=$rhostId senderId=$senderId bbo=$bbo")
        // C++: rhost->Set_Maximum_Bps(Bbo) — no BPS cap in Kotlin server yet
        setDeletePending()
    }
}
