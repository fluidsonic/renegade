package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cScPingResponseEvent (scpingresponseevent.cpp) — classId = NETCLASSID_SCPINGRESPONSEEVENT = 1013
// Sent by server in response to cCsPingRequestEvent; client uses PingNumber to measure RTT.
// Act() on the client calls cClientPingManager::Response_Received(PingNumber).
class ScPingResponseEvent(val pingNumber: Int) : NetEvent() {

    override val networkClassId: Int = 1013

    // C++: cScPingResponseEvent::Export_Creation — writes only PingNumber.
    override fun exportCreation(packet: BitStream) {
        packet.addInt(pingNumber)
    }
}
