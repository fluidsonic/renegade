package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

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
class BaseControllerClass(
    val playerType: Int = 0,
    val operationTimeFactor: Float = 1f,
    val isBasePowered: Boolean = true,
    val canGenerateSoldiers: Boolean = true,
    val canGenerateVehicles: Boolean = true,
    val isBaseDestroyed: Boolean = false,
    val didBeaconDestroyBase: Boolean = false,
    val isRadarEnabled: Boolean = true,
) : NetworkObject() {

    // C++: Get_Network_Class_ID() not overridden → returns 0 (base class default)
    override val networkClassId: Int = 0

    override fun delete() {}

    // C++: Set_Delete_Pending is overridden to no-op — persists between levels
    override fun setDeletePending() {}

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
}
