package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObjectManager
import ccr.server.GameServer
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
    }

    override fun act(server: GameServer, rhostId: Int) {
        println("[GAME] PURCHASEREQUESTEVENT from rhostId=$rhostId senderId=$senderId " +
            "type=$purchaseType item=$itemIndex altSkin=$altSkinIndex")
        val host = server.connectionManager.getHost(rhostId) ?: run { setDeletePending(); return }

        if (!server.gameState.isGameplayPermitted) {
            val response = PurchaseResponseEvent(purchaserId = senderId, responseId = 2)
            server.sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, NetworkObjectManager.getNewDynamicId()) }
            setDeletePending(); return
        }

        val result = server.vendor.handlePurchase(rhostId, purchaseType, itemIndex, altSkinIndex)
        val response = PurchaseResponseEvent(purchaserId = senderId, responseId = result.responseId)
        server.sendGameNetObj(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, response, NetworkObjectManager.getNewDynamicId()) }

        if (result.responseId == VendorClass.RESPONSE_SUCCESS) {
            if (result.isVehiclePurchase) {
                // Route to vehicle factory — find the team's available factory and start the timer
                val playerTeam = server.god.playerTeams[rhostId] ?: 0
                val baseController = if (playerTeam == 0) server.baseControllerNod else server.baseControllerGdi
                val factory = server.gameObjManager.getAllObjects()
                    .filterIsInstance<VehicleFactoryGameObj>()
                    .find { it.baseController === baseController && !it.isBusy && !it.isDestroyed }
                if (factory != null) {
                    factory.requestVehicle(result.purchasedDefId, 12.0f, server.god.soldiersByHost[rhostId])
                    println("[GAME] vehicle order queued: defId=${result.purchasedDefId} factory=${factory.networkId} buyer=$rhostId")
                } else {
                    // Factory became unavailable between vendor check and now — log only
                    // (VendorClass already checked canGenerateVehicles so this is very rare)
                    println("[GAME] no available vehicle factory for team=$playerTeam (race condition)")
                }
            } else if (result.isEquipmentPurchase) {
                // C++: PowerUpGameObjDef::Grant() — update existing soldier, no respawn
                server.god.grantPowerUp(rhostId, result.purchasedDefId)
            } else {
                // Kill current soldier and respawn as purchased character
                server.god.deleteSoldier(rhostId)
                val playerTeam = server.god.playerTeams[rhostId] ?: 0
                server.god.createCommandoWithDef(rhostId, playerTeam, result.purchasedDefId)
            }
        }
        setDeletePending()
    }
}
