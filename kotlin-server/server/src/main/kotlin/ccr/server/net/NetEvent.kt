package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.server.Network

// C++: cNetEvent (netevent.h:11) — base class for one-shot network events.
// cNetEvent extends NetworkObjectClass but has no persistent state (no RARE/OCCASIONAL/FREQUENT data).
// Export_Creation is the only meaningful override in subclasses.
abstract class NetEvent : NetworkObject() {
    // Transient objects: no cleanup needed on delete
    override fun delete() {}

    // Server reference and sender ID — set by Network packet handler after factory creation.
    lateinit var network: Network
    var rhostId: Int = -1

    // C++: cNetEvent::Act() — called server-side after Import_Creation for C→S events.
    // Subclasses override to implement their server-side behavior.
    open fun act() {}

    // Guard for round-trip tests: only call act() if network has been wired up.
    protected fun actIfWiredUp() {
        if (::network.isInitialized) act()
    }
}
