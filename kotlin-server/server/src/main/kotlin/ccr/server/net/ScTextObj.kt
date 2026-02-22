package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cScTextObj — networkClassId = NETCLASSID_SCTEXTOBJ = 1001
// Server-to-client text message event.
// BIT_CREATION:
//   Type (BYTE) — TextMessageEnum value
//   SenderId (int)
//   RecipientId (int)
//   IsHostAdminMessage (bool)
//   Text (wideString)
class ScTextObj(
    val type: Int,
    val senderId: Int,
    val recipientId: Int,
    val isHostAdminMessage: Boolean,
    val text: String,
) : NetEvent() {
    override val networkClassId: Int = 1001

    override fun exportCreation(packet: BitStream) {
        packet.addByte(type.toByte())
        packet.addInt(senderId)
        packet.addInt(recipientId)
        packet.addBool(isHostAdminMessage)
        packet.addWideString(text)
    }
}
