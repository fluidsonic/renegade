package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cGodModeEvent — classId = NETCLASSID_GODMODEEVENT = 1027
// Client→Server event for toggling god mode via password.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   Password (terminatedString, ASCII, permitEmpty=true)
class GodModeEvent(
    var senderId: Int = 0,
    var password: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1027

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addTerminatedString(password, permitEmpty = true)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        password = packet.getTerminatedString(permitEmpty = true)
    }
}
