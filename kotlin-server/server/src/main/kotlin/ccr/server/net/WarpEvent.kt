package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cWarpEvent — classId = NETCLASSID_WARPEVENT = 1022
// Client→Server event for warping a player by name.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   PlayerName (wideString, permitEmpty=true)
class WarpEvent(
    var senderId: Int = 0,
    var playerName: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1022

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addWideString(playerName, permitEmpty = true)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        playerName = packet.getWideString(permitEmpty = true)
    }
}
