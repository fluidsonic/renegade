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

    // C++: cNetEvent::Act() — called server-side after Import_Creation for C→S events.
    // Subclasses override to implement their server-side behavior.
    open fun act(server: Network, rhostId: Int) {}
}
