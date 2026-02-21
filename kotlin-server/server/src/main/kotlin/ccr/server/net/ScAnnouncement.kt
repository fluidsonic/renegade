package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: SCAnnouncement — classId = NETCLASSID_SCANNOUNCEMENT = 1016
// BIT_CREATION:
//   ToId (int)
//   FromId (int)
//   AnnouncementId (int)
//   RadioCmdId (int)
//   Type (BYTE) — AnnouncementEnum value
class ScAnnouncement(
    val toId: Int,
    val fromId: Int,
    val announcementId: Int,
    val radioCmdId: Int,
    val type: Int,
) : NetEvent() {
    override val networkClassId: Int = 1016

    override fun exportCreation(packet: BitStream) {
        packet.addInt(toId)
        packet.addInt(fromId)
        packet.addInt(announcementId)
        packet.addInt(radioCmdId)
        packet.addByte(type.toByte())
    }
}
