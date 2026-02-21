package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cPurchaseResponseEvent — classId = NETCLASSID_PURCHASERESPONSEEVENT = 1004
// BIT_CREATION: [PurchaserId: int] [ResponseId: int]
class PurchaseResponseEvent(
    val purchaserId: Int,
    val responseId: Int,
) : NetEvent() {
    override val networkClassId: Int = 1004

    override fun exportCreation(packet: BitStream) {
        packet.addInt(purchaserId)
        packet.addInt(responseId)
    }
}
