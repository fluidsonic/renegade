package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject

// C++: CClientFps (Commando/clientfps.h) — networkClassId = NETCLASSID_CLIENTFPS = 1031
// C→S mirrored object to inform server of client framerate.
// The server only imports (reads) this object; the client exports (writes) it.
//
// Wire format:
//   Export_Creation: Add(ClientId: int)
//   Import_Creation: Get(ClientId: int)
//   Export_Frequent: Add(Fps: BYTE)  — 8-bit unsigned
//   Import_Frequent: Get(Fps: BYTE)  — 8-bit unsigned
class ClientFps(
    var clientId: Int = 0,
    var fps: Int = 0,
) : NetworkObject() {
    override val networkClassId: Int = 1031
    override fun delete() {}

    override fun exportCreation(packet: BitStream) {
        packet.addInt(clientId)
    }

    override fun importCreation(packet: BitStream) {
        clientId = packet.getInt()
    }

    // C++: Export_Frequent writes Fps as BYTE (8-bit unsigned)
    override fun exportFrequent(packet: BitStream) {
        packet.addByte(fps.toByte())
    }

    // C++: Import_Frequent reads Fps as BYTE (8-bit unsigned)
    override fun importFrequent(packet: BitStream) {
        fps = packet.getByte().toInt() and 0xFF
    }
}
