package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.Network

// C++: cRequestKillEvent — networkClassId = NETCLASSID_REQUESTKILLEVENT = 1034
// Client→Server event requesting the server kill a specific game object.
// Wire format (BIT_CREATION):
//   ObjectId (int)
class RequestKillEvent(
    var objectId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1034

    override fun exportCreation(packet: BitStream) {
        packet.addInt(objectId)
    }

    override fun importCreation(packet: BitStream) {
        objectId = packet.getInt()
    }

    override fun act(server: Network, rhostId: Int) {
        println("[GAME] REQUESTKILLEVENT from rhostId=$rhostId objectId=$objectId")
        // Only allow self-kill: check if the requested object is this player's soldier
        val soldier = server.god.soldiersByHost[rhostId]
        if (soldier != null && soldier.networkId == objectId) {
            server.broadcastPlayerKill(-1, rhostId)
            server.god.deleteSoldier(rhostId)
        }
        setDeletePending()
    }
}
