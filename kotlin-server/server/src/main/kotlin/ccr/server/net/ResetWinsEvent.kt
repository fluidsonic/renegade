package ccr.server.net

import ccr.net.replication.NetworkObject

// C++: ResetWinsEvent (bioevent.cpp) — signal-only event, classId=1006
// Sent to all in-game clients when a core restart happens to reset win counters.
class ResetWinsEvent : NetworkObject() {
    override val networkClassId: Int = 1006
    override fun delete() {}  // transient event, no cleanup
}
