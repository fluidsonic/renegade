package ccr.server.net

import ccr.net.bitstream.BitStream

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
        actIfWiredUp()
    }

    override fun act() {
        println("[GAME] REQUESTKILLEVENT objectId=$objectId")
        // Reverse-lookup: find which host owns the soldier with this networkId
        val entry = network.god.soldiersByHost.entries.find { it.value.networkId == objectId }
        if (entry != null) {
            network.broadcastPlayerKill(-1, entry.key)
            network.god.deleteSoldier(entry.key)
        }
        setDeletePending()
    }
}
