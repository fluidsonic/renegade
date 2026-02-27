package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cSvrGoodbyeEvent — networkClassId = NETCLASSID_SVRGOODBYEEVENT = 1007
// BIT_CREATION: [IsQuickFullExitRequested: bool]
class SvrGoodbyeEvent(
    val isQuickFullExitRequested: Boolean,
) : NetEvent() {
    override val networkClassId: Int = 1007

    override fun exportCreation(packet: BitStream) {
        setDeletePending()  // C++: one-shot event — Export_Creation calls Set_Delete_Pending()
        packet.addBool(isQuickFullExitRequested)
    }
}
