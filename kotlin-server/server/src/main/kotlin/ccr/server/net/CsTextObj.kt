package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cCsTextObj — classId = NETCLASSID_CSTEXTOBJ = 1018
// Client→Server chat message. Mirror of cScTextObj (1001) in the opposite direction.
// Wire format (BIT_CREATION):
//   Type (BYTE) — TextMessageEnum
//   SenderId (int)
//   RecipientId (int)
//   IsHostAdminMessage (bool)
//   Text (wideString)
class CsTextObj(
    var type: Int = 0,
    var senderId: Int = 0,
    var recipientId: Int = 0,
    var isHostAdminMessage: Boolean = false,
    var text: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1018

    override fun exportCreation(packet: BitStream) {
        packet.addByte(type.toByte())
        packet.addInt(senderId)
        packet.addInt(recipientId)
        packet.addBool(isHostAdminMessage)
        packet.addWideString(text, permitEmpty = true)
    }

    override fun importCreation(packet: BitStream) {
        type = packet.getByte().toInt() and 0xFF
        senderId = packet.getInt()
        recipientId = packet.getInt()
        isHostAdminMessage = packet.getBool()
        text = packet.getWideString(permitEmpty = true)
    }
}
