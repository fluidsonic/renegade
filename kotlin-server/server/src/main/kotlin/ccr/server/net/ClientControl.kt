package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: CClientControl (Combat/clientcontrol.h) — classId = NETCLASSID_CLIENTCONTROL = 1017
// C→S mirrored object to represent client control and targeting data.
// The server only imports (reads) this object; the client exports (writes) it.
//
// Wire format:
//   Export_Creation: Add(ClientId: int)
//   Import_Creation: Get(ClientId: int)
//   Export_Frequent: Add(SmartObjId: int); if SmartObjId != -1: control+state data follows
//   Import_Frequent: Get(SmartObjId: int); if SmartObjId != -1: flush remaining bits
class ClientControl(
    var clientId: Int = 0,
    var smartObjId: Int = -1,
) : NetworkObject() {
    override val networkClassId: Int = 1017
    override fun delete() {}

    override fun exportCreation(packet: BitStream) {
        packet.addInt(clientId)
    }

    override fun importCreation(packet: BitStream) {
        clientId = packet.getInt()
    }

    override fun exportFrequent(packet: BitStream) {
        packet.addInt(smartObjId)
        // When smartObjId != -1, the client would normally write Export_Control_Cs + Export_State_Cs here.
        // The server-side implementation does not export frequent data with non-null objects.
    }

    override fun importFrequent(packet: BitStream) {
        smartObjId = packet.getInt()
        if (smartObjId != -1) {
            // C++: Import_Frequent calls SmartGameObj::Import_Control_Cs + Import_State_Cs,
            // or flushes if the SmartGameObj is not found.
            // Server-side: flush remaining bits (no game object manager available here).
            packet.flush()
        }
    }
}
