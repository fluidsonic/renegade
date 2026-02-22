package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: cServerFps (Commando/serverfps.h / serverfps.cpp)
// S->C singleton object that informs clients of server framerate.
// Does NOT override Get_Network_Class_ID() — returns 0 (base class default).
// Uses a static network ID (NETID_SERVER_FPS) and AppPacketType instead of factory lookup.
// Set_Delete_Pending is a no-op (persists for entire game session).
//
// Wire format:
//   Export_Frequent: fps (int)
class ServerFps(val fps: Int = 0) : NetworkObject() {

    // C++: Get_Network_Class_ID() not overridden → returns 0 (base class default)
    override val networkClassId: Int = 0
    override val creationDirtyBit = BIT_FREQUENT

    override fun delete() {}

    // C++: Set_Delete_Pending is overridden to be a no-op — singleton persists
    override fun setDeletePending() {}

    // C++: cServerFps::Export_Frequent — packet.Add(Fps) where Fps is int
    override fun exportFrequent(packet: BitStream) {
        packet.addInt(fps)
    }
}
