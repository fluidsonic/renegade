package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

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

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] CSANNOUNCEMENT from rhostId=$rhostId fromId=$fromId " +
            "toId=$toId announcementId=$announcementId " +
            "radioCmdId=$radioCmdId type=$type")
        // Relay to all in-game clients as ScAnnouncement
        val relay = ScAnnouncement(
            toId = toId,
            fromId = fromId,
            announcementId = announcementId,
            radioCmdId = radioCmdId,
            type = type,
        )
        for (clientId in server.god.playerInGame) {
            val clientHost = server.connectionManager.getHost(clientId) ?: continue
            server.serverSendPacket(clientHost) { bs ->
                NetworkObjectPacketWriter.writeCreation(bs, relay, relay.networkId)
            }
        }
        setDeletePending()
    }
}
