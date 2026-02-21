package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cPurchaseRequestEvent — classId = NETCLASSID_PURCHASEREQUESTEVENT = 1023
// Client→Server event for purchasing a character/vehicle/item from the PT.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   PurchaseType (int)
//   ItemIndex (int)
//   AltSkinIndex (int)
class PurchaseRequestEvent(
    var senderId: Int = 0,
    var purchaseType: Int = 0,
    var itemIndex: Int = 0,
    var altSkinIndex: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1023

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(purchaseType)
        packet.addInt(itemIndex)
        packet.addInt(altSkinIndex)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        purchaseType = packet.getInt()
        itemIndex = packet.getInt()
        altSkinIndex = packet.getInt()
    }
}
