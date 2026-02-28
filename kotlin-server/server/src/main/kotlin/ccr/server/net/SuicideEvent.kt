package ccr.server.net

import ccr.net.bitstream.BitStream

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
        actIfWiredUp()
    }

    override fun act() {
        println("[GAME] SUICIDEEVENT from senderId=$senderId")
        if (network.gameState.isGameplayPermitted) {
            if (senderId in network.god.soldiersByHost) {
                network.broadcastPlayerKill(-1, senderId)
                network.god.deleteSoldier(senderId)
            }
        }
        setDeletePending()
    }
}
