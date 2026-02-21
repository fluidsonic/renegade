package ccr.net.reliability

import ccr.net.protocol.Packet

// C++: unreliable send/rcv lists in cRemoteHost (wwnet/rhost.h/.cpp)
// Unreliable: no retransmission. Stale packets (ID < highest received) are discarded.

class UnreliableChannel {
    // C++: UnreliablePacketSendId / UnreliablePacketRcvId
    var nextSendId: Int = 0
        private set

    // Highest received packet ID (stale packets below this are discarded)
    private var highestReceivedId: Int = -1

    fun nextOutgoingId(): Int = nextSendId++

    // Returns true if this packet should be delivered (not stale).
    // C++: unreliable receive stale-discard logic
    fun receive(packet: Packet): Boolean {
        val id = packet.id
        return if (id > highestReceivedId) {
            highestReceivedId = id
            true
        } else {
            // Stale packet: ID ≤ highestReceivedId → discard
            false
        }
    }
}
