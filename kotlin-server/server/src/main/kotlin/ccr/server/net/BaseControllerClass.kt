package ccr.server.net

import ccr.math.OBBox
import ccr.math.Vector3
import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.server.defs.CNCModeSettingsDef

// C++: BaseControllerClass (Combat/basecontroller.h / basecontroller.cpp)
// S->C object tracking base state for one team (NOD or GDI).
// Does NOT override Get_Network_Class_ID() → returns 0 (base class default).
// Uses a static network ID (NETID_GDI_BASE_CONTROLLER or NETID_NOD_BASE_CONTROLLER)
// and AppPacketType=APPPACKETTYPE_BASECONTROLLER for identification.
// Set_Delete_Pending is a no-op (persists between levels).
//
// Wire format:
//   Export_Occasional:
//     operationTimeFactor (float)
//     isBasePowered (bool)
//     canGenerateSoldiers (bool)
//     canGenerateVehicles (bool)
//     isBaseDestroyed (bool)
//     didBeaconDestroyBase (bool)
//     isRadarEnabled (bool)
class BaseControllerClass(val playerType: Int = 0) : NetworkObject() {

    // C++: Get_Network_Class_ID() not overridden → returns 0 (base class default)
    override val networkClassId: Int = 0

    override fun delete() {}

    // C++: Set_Delete_Pending is overridden to no-op — persists between levels
    override fun setDeletePending() {}

    // Back-reference to the Network instance — set by Network.kt after cncInitialize.
    // Used by VehicleFactoryGameObj.createVehicle() to reach God.createHarvester/createVehicle.
    var network: ccr.server.Network? = null

    // C++: OBBoxClass BeaconZone — bounding box of the closest beacon zone to this base's buildings.
    // Set externally during level init (C++: ScriptZoneGameObj::Find_Closest_Zone → Get_Bounding_Box).
    var beaconZone: OBBox = OBBox(Vector3(), Vector3())

    // All fields start at C++ Initialize() defaults
    var operationTimeFactor: Float = 1f; private set
    var isBasePowered: Boolean = true; private set
    var canGenerateSoldiers: Boolean = false; private set   // C++: Initialize sets false
    var canGenerateVehicles: Boolean = false; private set   // C++: Initialize sets false
    var isBaseDestroyed: Boolean = false; private set
    var didBeaconDestroyBase: Boolean = false; private set
    var isRadarEnabled: Boolean = true; private set

    // C++ setters — each sets BIT_OCCASIONAL
    fun setOperationTimeFactor(v: Float) { operationTimeFactor = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }
    fun setBasePowered(v: Boolean) { isBasePowered = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }
    fun setCanGenerateSoldiers(v: Boolean) { canGenerateSoldiers = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }
    fun setCanGenerateVehicles(v: Boolean) { canGenerateVehicles = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }
    fun setBaseDestroyed(v: Boolean) { isBaseDestroyed = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }
    fun setBeaconDestroyedBase(v: Boolean) { didBeaconDestroyBase = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }
    fun enableRadar(v: Boolean) { isRadarEnabled = v; setObjectDirtyBit(BIT_OCCASIONAL, true) }

    private val buildings = mutableListOf<BuildingGameObj>()

    fun addBuilding(building: BuildingGameObj) { buildings.add(building) }
    fun getBuildings(): List<BuildingGameObj> = buildings

    // C++: BaseControllerClass::Are_All_Buildings_Destroyed()
    fun areAllBuildingsDestroyed(): Boolean {
        if (buildings.isEmpty()) return false
        return buildings.all { it.isDestroyed }
    }

    // C++: BaseControllerClass::Check_Base_Power
    // Power is ON if any PowerPlant is alive. Power is OFF only if ALL PowerPlants are destroyed.
    fun checkBasePower(powerPlants: List<PowerPlantGameObj>) {
        val anyAlive = powerPlants.any { !it.isDestroyed }
        powerBase(anyAlive)
    }

    // C++: BaseControllerClass::Power_Base
    fun powerBase(onoff: Boolean) {
        if (isBasePowered != onoff) {
            setBasePowered(onoff)
            setOperationTimeFactor(if (onoff) 1.0f else 2.0f)
            for (building in buildings) {
                if (!onoff || !building.isDestroyed) {
                    building.enablePower(onoff)
                }
            }
        }
    }

    // C++: BaseControllerClass::Check_Radar
    fun checkRadar() {
        val enable = buildings.any { it is ComCenterGameObj && !it.isDestroyed }
        enableRadar(enable)
    }

    // C++: BaseControllerClass::On_Building_Damaged
    fun onBuildingDamaged(building: BuildingGameObj) {
        // C++: notify observers, update stats — stub; no logic needed server-side
    }

    // C++: BaseControllerClass::On_Building_Destroyed
    fun onBuildingDestroyed(building: BuildingGameObj) {
        if (areAllBuildingsDestroyed()) {
            setBaseDestroyed(true)
        }
    }

    // C++: BaseControllerClass::Distribute_Funds_To_Each_Teammate
    fun distributeFundsToEachTeammate(funds: Int, starList: List<SoldierGameObj>) {
        if (funds <= 0) return
        for (soldier in starList) {
            if (soldier.playerType == playerType) {
                soldier.playerData?.addMoney(funds.toFloat())
            }
        }
    }

    // C++: BaseControllerClass::On_Vehicle_Generated
    fun onVehicleGenerated(vehicle: VehicleGameObj) {
        // C++: notify vehicle-generated — stub; no logic needed server-side
    }

    // C++: BaseControllerClass::On_Vehicle_Delivered
    fun onVehicleDelivered(vehicle: VehicleGameObj) {
        // C++: notify vehicle-delivered — stub; no logic needed server-side
    }

    // C++: BaseControllerClass::Request_Harvester — ask the war/vehicle factory to build a harvester
    fun requestHarvester(definitionId: Int): Boolean {
        require(definitionId > 0) { "`definitionId` must be positive." }

        val factory = buildings.filterIsInstance<VehicleFactoryGameObj>()
            .filter { it.isAvailable() }
            .randomOrNull()
        if (factory == null) {
            System.err.println("No available vehicle factories to request harvester for player type $playerType.")
            return false
        }

        factory.requestVehicle(definitionId, 8f * operationTimeFactor, null)
        return true
    }

    // C++: BaseControllerClass::Destroy_Base (forced)
    fun destroyBase() {
        for (b in buildings) b.setNormalizedHealth(0f)
        setBasePowered(false)
        setCanGenerateSoldiers(false)
        setCanGenerateVehicles(false)
        setBaseDestroyed(true)
        setOperationTimeFactor(2.0f)
    }

    // Reset for new round
    fun reset() {
        operationTimeFactor = 1f
        isBasePowered = true
        canGenerateSoldiers = false
        canGenerateVehicles = false
        isBaseDestroyed = false
        didBeaconDestroyBase = false
        isRadarEnabled = true
        setObjectDirtyBit(BIT_OCCASIONAL, true)
    }

    // C++: BaseControllerClass::Export_Occasional — transmits all base state variables
    override fun exportOccasional(packet: BitStream) {
        packet.addFloat(operationTimeFactor)
        packet.addBool(isBasePowered)
        packet.addBool(canGenerateSoldiers)
        packet.addBool(canGenerateVehicles)
        packet.addBool(isBaseDestroyed)
        packet.addBool(didBeaconDestroyBase)
        packet.addBool(isRadarEnabled)
    }

    override fun importOccasional(packet: BitStream) {}

    // C++: const OBBoxClass& Get_Beacon_Zone() const { return BeaconZone; }
    fun getBeaconZone(): OBBox = beaconZone

    // C++: BaseControllerClass::On_Beacon_Armed(BeaconGameObj* beacon)
    fun onBeaconArmed(beacon: BeaconGameObj) {
        val reportId = beaconReportId(beacon,
            nodNuke = { it.nodNukeBeaconDeployedId }, gdiNuke = { it.gdiNukeBeaconDeployedId },
            nodIon  = { it.nodIonBeaconDeployedId  }, gdiIon  = { it.gdiIonBeaconDeployedId  })
        playAnnouncement(reportId)
    }

    // C++: BaseControllerClass::On_Beacon_Disarmed(BeaconGameObj* beacon)
    fun onBeaconDisarmed(beacon: BeaconGameObj) {
        val reportId = beaconReportId(beacon,
            nodNuke = { it.nodNukeBeaconDisarmedId }, gdiNuke = { it.gdiNukeBeaconDisarmedId },
            nodIon  = { it.nodIonBeaconDisarmedId  }, gdiIon  = { it.gdiIonBeaconDisarmedId  })
        playAnnouncement(reportId)
    }

    // C++: BaseControllerClass::On_Beacon_Warning(BeaconGameObj* beacon)
    fun onBeaconWarning(beacon: BeaconGameObj) {
        val reportId = beaconReportId(beacon,
            nodNuke = { it.nodNukeBeaconWarningId }, gdiNuke = { it.gdiNukeBeaconWarningId },
            nodIon  = { it.nodIonBeaconWarningId  }, gdiIon  = { it.gdiIonBeaconWarningId  })
        playAnnouncement(reportId)
    }

    // Selects the correct announcement ID from CNCModeSettingsDef based on beacon type and team.
    private fun beaconReportId(
        beacon: BeaconGameObj,
        nodNuke: (CNCModeSettingsDef) -> Int,
        gdiNuke: (CNCModeSettingsDef) -> Int,
        nodIon:  (CNCModeSettingsDef) -> Int,
        gdiIon:  (CNCModeSettingsDef) -> Int,
    ): Int {
        val cncDef = network?.loadedLevel?.definitions?.all()
            ?.filterIsInstance<CNCModeSettingsDef>()?.firstOrNull() ?: return 0
        val isNuke = beacon.getDefinition().isNuke()
        val team = beacon.getPlayerType()
        return if (isNuke) {
            if (team == PLAYERTYPE_NOD) nodNuke(cncDef) else gdiNuke(cncDef)
        } else {
            if (team == PLAYERTYPE_NOD) nodIon(cncDef) else gdiIon(cncDef)
        }
    }

    // C++: BaseControllerClass::Play_Announcement(int text_id) — broadcasts EVA announcement to all clients.
    private fun playAnnouncement(reportId: Int) {
        if (reportId == 0) return
        val net = network ?: return
        val announcement = ScAnnouncement(toId = 0, fromId = 0, announcementId = reportId, radioCmdId = 0, type = 0)
        for (clientId in net.god.playerInGame) {
            val host = net.connectionManager.getHost(clientId) ?: continue
            net.serverSendPacket(host) { bs -> NetworkObjectPacketWriter.writeCreation(bs, announcement, announcement.networkId) }
        }
    }

    companion object {
        /** Sentinel buyerRhostId used to distinguish harvester requests from player purchases. */
        const val HARVESTER_BUYER_ID: Int = -1

        // C++: BaseControllerClass::Find_Base(int player_type) — finds the base controller for a team
        fun findBase(playerType: Int): BaseControllerClass? = null
    }
}
