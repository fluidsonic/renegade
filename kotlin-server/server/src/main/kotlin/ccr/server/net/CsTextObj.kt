package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObjectManager
import ccr.server.GameServer

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

    override fun act(server: GameServer, rhostId: Int) {
        println("[GAME] CHAT from rhostId=$rhostId type=$type text='$text'")
        // TEXT_MESSAGE_PUBLIC=0, TEXT_MESSAGE_TEAM=1, TEXT_MESSAGE_PRIVATE=2
        val relay = ScTextObj(
            type = type,
            senderId = senderId,
            recipientId = recipientId,
            isHostAdminMessage = false,
            text = text,
        )
        when (type) {
            0 -> {  // PUBLIC — broadcast to all in-game
                for (clientId in server.god.playerInGame) {
                    val clientHost = server.connectionManager.getHost(clientId) ?: continue
                    server.sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                }
            }
            1 -> {  // TEAM — send to same team only
                val senderTeam = server.god.playerTeams[rhostId] ?: -1
                for (clientId in server.god.playerInGame) {
                    if ((server.god.playerTeams[clientId] ?: -1) != senderTeam) continue
                    val clientHost = server.connectionManager.getHost(clientId) ?: continue
                    server.sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                }
            }
            2 -> {  // PRIVATE — send to sender and recipient only
                val recipientRhostId = server.god.playersByHost.entries.find { it.value.id == recipientId }?.key
                for (clientId in listOfNotNull(rhostId, recipientRhostId)) {
                    val clientHost = server.connectionManager.getHost(clientId) ?: continue
                    server.sendGameNetObj(clientHost) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
                }
            }
            else -> {
                val host = server.connectionManager.getHost(rhostId) ?: run { setDeletePending(); return }
                server.sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, relay, NetworkObjectManager.getNewDynamicId()) }
            }
        }
        setDeletePending()
    }
}
