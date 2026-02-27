package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.GameServer

// C++: cSuicideEvent — networkClassId = NETCLASSID_SUICIDEEVENT = 1019
// Client→Server event sent when a player kills themselves.
// Wire format (BIT_CREATION):
//   SenderId (int)
class SuicideEvent(
    var senderId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1019

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
    }

    override fun act(server: GameServer, rhostId: Int) {
        println("[GAME] SUICIDEEVENT from rhostId=$rhostId senderId=$senderId")
        if (server.gameState.isGameplayPermitted) {
            if (rhostId in server.god.soldiersByHost) {
                server.broadcastPlayerKill(-1, rhostId)
                server.god.deleteSoldier(rhostId)
            }
        }
        setDeletePending()
    }
}
