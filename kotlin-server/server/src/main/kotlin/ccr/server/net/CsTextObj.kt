package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cCsTextObj (cstextobj.h/.cpp) — networkClassId = NETCLASSID_CSTEXTOBJ = 1018
// Client→Server chat message.
// Wire format (Export_Creation / Import_Creation from cstextobj.cpp):
//   SenderId (int)
//   Type (BYTE) — TextMessageEnum value
//   Text (wideString)
//   Recipient (int)
// Note: IsHostAdminMessage does NOT appear in cCsTextObj (1018); it only exists
// in the S→C cScTextObj (1001). Reading it here would corrupt the packet stream.
class CsTextObj(
    var senderId: Int = 0,
    var type: Int = 0,
    var text: String = "",
    var recipientId: Int = -1,
) : NetEvent() {
    override val networkClassId: Int = 1018

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addByte(type.toByte())
        packet.addWideString(text, permitEmpty = true)
        packet.addInt(recipientId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        type = packet.getByte().toInt() and 0xFF
        text = packet.getWideString(permitEmpty = true)
        recipientId = packet.getInt()
    }
}
