package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.VendorClass

// C++: cPurchaseRequestEvent — networkClassId = NETCLASSID_PURCHASEREQUESTEVENT = 1023
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
        actIfWiredUp()
    }

    override fun act() {
        println("[GAME] PURCHASEREQUESTEVENT from senderId=$senderId " +
            "type=$purchaseType item=$itemIndex altSkin=$altSkinIndex")
        val host = network.connectionManager.getHost(senderId) ?: run { setDeletePending(); return }

        if (!network.gameState.isGameplayPermitted) {
            val response = PurchaseResponseEvent(purchaserId = senderId, responseId = 2)
            network.serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, response.networkId) }
            setDeletePending(); return
        }

        val result = network.vendor.handlePurchase(senderId, purchaseType, itemIndex, altSkinIndex)
        val response = PurchaseResponseEvent(purchaserId = senderId, responseId = result.responseId)
        network.serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, response.networkId) }

        if (result.responseId == VendorClass.RESPONSE_SUCCESS) {
            if (result.isVehiclePurchase) {
                // Route to vehicle factory — find the team's available factory and start the timer
                val playerTeam = network.god.playerTeams[senderId] ?: 0
                val baseController = if (playerTeam == 0) network.baseControllerNod else network.baseControllerGdi
                val factory = network.gameObjManager.getAllObjects()
                    .filterIsInstance<VehicleFactoryGameObj>()
                    .find { it.baseController === baseController && !it.isBusy && !it.isDestroyed }
                if (factory != null) {
                    factory.requestVehicle(result.purchasedDefId, 12.0f, network.god.soldiersByHost[senderId])
                    println("[GAME] vehicle order queued: defId=${result.purchasedDefId} factory=${factory.networkId} buyer=$senderId")
                } else {
                    // Factory became unavailable between vendor check and now — log only
                    // (VendorClass already checked canGenerateVehicles so this is very rare)
                    println("[GAME] no available vehicle factory for team=$playerTeam (race condition)")
                }
            } else if (result.isEquipmentPurchase) {
                // C++: PowerUpGameObjDef::Grant() — update existing soldier, no respawn
                network.god.grantPowerUp(senderId, result.purchasedDefId)
            } else {
                // Kill current soldier and respawn as purchased character
                network.god.deleteSoldier(senderId)
                val playerTeam = network.god.playerTeams[senderId] ?: 0
                network.god.createCommandoWithDef(senderId, playerTeam, result.purchasedDefId)
            }
        }
        setDeletePending()
    }
}
