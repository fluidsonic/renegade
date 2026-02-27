package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cCsTextObj (cstextobj.h/.cpp) — networkClassId = NETCLASSID_CSTEXTOBJ = 1018
// Client→Server chat message.
// Wire format (Export_Creation from cstextobj.cpp:124-137):
//   SenderId (int)
//   Type (BYTE) — TextMessageEnum value
//   Text (wideString)
//   RecipientId (int)
//   IsHostAdminMessage (bool)
class CsTextObj(
    var senderId: Int = 0,
    var type: Int = 0,
    var text: String = "",
    var recipientId: Int = -1,
    var isHostAdminMessage: Boolean = false,
) : NetEvent() {
    override val networkClassId: Int = 1018

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addByte(type.toByte())
        packet.addWideString(text, permitEmpty = true)
        packet.addInt(recipientId)
        packet.addBool(isHostAdminMessage)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        type = packet.getByte().toInt() and 0xFF
        text = packet.getWideString(permitEmpty = true)
        recipientId = packet.getInt()
        isHostAdminMessage = packet.getBool()
    }
}
