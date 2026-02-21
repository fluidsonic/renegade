package ccr.net.protocol

// C++: packettype.h enum (anonymous)
// These are the 8 low-level packet types used by the wwnet layer.
// Not exposed to application code directly.
enum class PacketType(val id: Int) {
    UNRELIABLE(0),      // best-effort delivery, no ordering
    RELIABLE(1),        // guaranteed delivery in order
    ACK(2),             // acknowledgement for a reliable packet
    KEEPALIVE(3),       // heartbeat with packetloss% and service count
    CONNECT_CS(4),      // client → server: connection request
    ACCEPT_SC(5),       // server → client: connection accepted (includes assigned client ID)
    REFUSAL_SC(6),      // server → client: connection refused
    FIREWALL_PROBE(7),  // firewall traversal probe (not used in LAN mode)
    ;

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: Int): PacketType = checkNotNull(byId[id]) { "Unknown packet type id: $id" }
    }
}
