package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cGameDataUpdateEvent (gamedataupdateevent.cpp) — classId = NETCLASSID_GAMEDATAUPDATEEVENT = 1012
// Sent after client finishes loading; triggers gameplay activation via Act().
// gamedataupdateevent.h:27 declares TimeRemainingSeconds as INT (not float).
class GameDataUpdateEvent(
    val timeRemainingSeconds: Int,  // INT — gamedataupdateevent.h:27
    val hostedGameNumber: Int,
) : NetEvent() {

    override val networkClassId: Int = 1012

    // C++: cGameDataUpdateEvent::Export_Creation (gamedataupdateevent.cpp:62-70)
    override fun exportCreation(packet: BitStream) {
        packet.addInt(timeRemainingSeconds)  // INT (not float!) — bug fix vs original inline code
        packet.addInt(hostedGameNumber)
    }
}
