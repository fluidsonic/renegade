package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cChangeTeamEvent — networkClassId = NETCLASSID_CHANGETEAMEVENT = 1020
// Client→Server event sent when a player requests a team change.
// Wire format (BIT_CREATION):
//   SenderId (int)
class ChangeTeamEvent(
    var senderId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1020

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
    }
}
