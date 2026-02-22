package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: CSAnnouncement — networkClassId = NETCLASSID_CSANNOUNCEMENT = 1037
// Client→Server radio/announcement event.
// Wire format (BIT_CREATION):
//   ToId (int)
//   FromId (int)
//   AnnouncementId (int)
//   RadioCmdId (int)
//   Type (BYTE) — AnnouncementEnum value
class CsAnnouncement(
    var toId: Int = 0,
    var fromId: Int = 0,
    var announcementId: Int = 0,
    var radioCmdId: Int = 0,
    var type: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1037

    override fun exportCreation(packet: BitStream) {
        packet.addInt(toId)
        packet.addInt(fromId)
        packet.addInt(announcementId)
        packet.addInt(radioCmdId)
        packet.addByte(type.toByte())
    }

    override fun importCreation(packet: BitStream) {
        toId = packet.getInt()
        fromId = packet.getInt()
        announcementId = packet.getInt()
        radioCmdId = packet.getInt()
        type = packet.getByte().toInt() and 0xFF
    }
}
