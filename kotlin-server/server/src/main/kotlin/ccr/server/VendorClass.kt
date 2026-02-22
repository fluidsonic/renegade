package ccr.server

import ccr.server.defs.PurchaseSettingsDefClass

/**
 * Port of C++ PurchaseSettingsDefClass purchase handling logic.
 *
 * Handles buy requests from the purchase terminal. Looks up the correct
 * PurchaseSettingsDefClass for the player's team and purchase type, validates
 * factory availability and funds, then deducts cost and returns the result.
 */
class VendorClass(private val server: GameServer) {

    companion object {
        // Purchase type constants (matches C++ CYCLED_ITEM_TYPE enum in purchasesettings.h)
        const val PURCHASE_SOLDIER = 0
        const val PURCHASE_VEHICLE = 1
        const val PURCHASE_EQUIPMENT = 2
        const val PURCHASE_SECRET_SOLDIER = 3
        const val PURCHASE_SECRET_VEHICLE = 4

        // Response IDs sent back to client
        const val RESPONSE_SUCCESS = 0
        const val RESPONSE_DENIED = 2
    }

    data class PurchaseResult(
        val responseId: Int,
        val purchasedDefId: Int = 0,
        val isVehiclePurchase: Boolean = false,
        val isEquipmentPurchase: Boolean = false,
    )

    /**
     * Handles a purchase request from a client.
     *
     * @param rhostId        the requesting client's host ID
     * @param purchaseType   one of PURCHASE_SOLDIER/VEHICLE/EQUIPMENT/SECRET_SOLDIER/SECRET_VEHICLE
     * @param itemIndex      index into the PurchaseSettingsDefClass entries array (0..9)
     * @param altSkinIndex   alternate skin index (0..2), 0 = default
     * @return [PurchaseResult] with responseId and purchased definition ID
     */
    fun handlePurchase(rhostId: Int, purchaseType: Int, itemIndex: Int, altSkinIndex: Int): PurchaseResult {
        // Resolve player team
        val playerType = server.god.playerTeams[rhostId]
        if (playerType == null) {
            println("[VENDOR] purchase denied: no team for rhostId=$rhostId")
            return PurchaseResult(RESPONSE_DENIED)
        }

        // Check factory availability
        val baseController = if (playerType == 0) server.baseControllerNod else server.baseControllerGdi
        if (baseController == null) {
            println("[VENDOR] purchase denied: no base controller for team=$playerType")
            return PurchaseResult(RESPONSE_DENIED)
        }

        val isSoldierPurchase = purchaseType == PURCHASE_SOLDIER || purchaseType == PURCHASE_SECRET_SOLDIER
        val isVehiclePurchase = purchaseType == PURCHASE_VEHICLE || purchaseType == PURCHASE_SECRET_VEHICLE

        if (isSoldierPurchase && !baseController.canGenerateSoldiers) {
            println("[VENDOR] soldier purchase denied: factory destroyed for team=$playerType")
            return PurchaseResult(RESPONSE_DENIED)
        }
        if (isVehiclePurchase && !baseController.canGenerateVehicles) {
            println("[VENDOR] vehicle purchase denied: factory destroyed for team=$playerType")
            return PurchaseResult(RESPONSE_DENIED)
        }

        // Map player team to PurchaseSettingsDefClass team constant
        // playerType 0 = NOD → PurchaseSettingsDefClass.TEAM_NOD (1)
        // playerType 1 = GDI → PurchaseSettingsDefClass.TEAM_GDI (0)
        val purchaseTeam = if (playerType == 0) PurchaseSettingsDefClass.TEAM_NOD else PurchaseSettingsDefClass.TEAM_GDI

        // Find the matching PurchaseSettingsDefClass
        val purchaseDef = server.loadedLevel?.definitions?.all()
            ?.filterIsInstance<PurchaseSettingsDefClass>()
            ?.find { it.team == purchaseTeam && it.type == purchaseType }

        if (purchaseDef == null) {
            println("[VENDOR] purchase denied: no PurchaseSettingsDefClass for team=$purchaseTeam type=$purchaseType")
            return PurchaseResult(RESPONSE_DENIED)
        }

        // Validate item index
        if (itemIndex !in purchaseDef.entries.indices) {
            println("[VENDOR] purchase denied: itemIndex=$itemIndex out of range")
            return PurchaseResult(RESPONSE_DENIED)
        }

        val entry = purchaseDef.entries[itemIndex]

        // Resolve the actual definition ID (considering alternate skins)
        val defId = if (altSkinIndex > 0 && altSkinIndex <= entry.alternateDefinitions.size &&
            entry.alternateDefinitions[altSkinIndex - 1] != 0) {
            entry.alternateDefinitions[altSkinIndex - 1]
        } else {
            entry.definitionId
        }

        if (defId == 0) {
            println("[VENDOR] purchase denied: defId=0 at itemIndex=$itemIndex altSkin=$altSkinIndex")
            return PurchaseResult(RESPONSE_DENIED)
        }

        // Check funds
        val player = server.god.playersByHost[rhostId]
        if (player == null) {
            println("[VENDOR] purchase denied: no player for rhostId=$rhostId")
            return PurchaseResult(RESPONSE_DENIED)
        }

        val cost = entry.cost.toFloat()
        if (player.money < cost) {
            println("[VENDOR] purchase denied: insufficient funds (has=${player.money}, needs=$cost) for rhostId=$rhostId")
            return PurchaseResult(RESPONSE_DENIED)
        }

        // Deduct cost
        player.addMoney(-cost)

        println("[VENDOR] purchase success: rhostId=$rhostId type=$purchaseType index=$itemIndex " +
            "defId=$defId cost=$cost remaining=${player.money}")

        val isEquipmentPurchase = purchaseType == PURCHASE_EQUIPMENT
        return PurchaseResult(
            responseId          = RESPONSE_SUCCESS,
            purchasedDefId      = defId,
            isVehiclePurchase   = isVehiclePurchase,
            isEquipmentPurchase = isEquipmentPurchase,
        )
    }
}
