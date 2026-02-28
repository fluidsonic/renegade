package ccr.server.net

import ccr.net.bitstream.BitStream

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
        actIfWiredUp()
    }

    override fun act() {
        if (isLoading) network.loadingHosts.add(senderId) else network.loadingHosts.remove(senderId)
        println("[GAME] LOADINGEVENT from senderId=$senderId isLoading=$isLoading")
        setDeletePending()
    }
}
