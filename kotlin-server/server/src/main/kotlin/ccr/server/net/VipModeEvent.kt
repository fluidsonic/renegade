package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cVipModeEvent — networkClassId = NETCLASSID_VIPMODEEVENT = 1028
// Client→Server event for toggling VIP mode via password.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   Password (terminatedString, ASCII, permitEmpty=true)
class VipModeEvent(
    var senderId: Int = 0,
    var password: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1028

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addTerminatedString(password, permitEmpty = true)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        password = packet.getTerminatedString(permitEmpty = true)
    }

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] VIPMODEEVENT from rhostId=$rhostId senderId=$senderId (ignored — debug-only)")
        setDeletePending()
    }
}
