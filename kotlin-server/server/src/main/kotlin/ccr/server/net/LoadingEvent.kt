package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.GameServer

// C++: cLoadingEvent — networkClassId = NETCLASSID_LOADINGEVENT = 1026
// Client→Server event reporting whether the client is currently loading a map.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   IsLoading (bool)
class LoadingEvent(
    var senderId: Int = 0,
    var isLoading: Boolean = false,
) : NetEvent() {
    override val networkClassId: Int = 1026

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addBool(isLoading)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        isLoading = packet.getBool()
    }

    override fun act(server: GameServer, rhostId: Int) {
        if (isLoading) server.loadingHosts.add(rhostId) else server.loadingHosts.remove(rhostId)
        println("[GAME] LOADINGEVENT from rhostId=$rhostId senderId=$senderId isLoading=$isLoading")
        setDeletePending()
    }
}
