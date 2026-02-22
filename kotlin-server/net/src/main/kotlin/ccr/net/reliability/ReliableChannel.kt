package ccr.net.reliability

import ccr.net.protocol.Packet
import ccr.net.protocol.PacketType

// C++: reliable send/rcv lists in cRemoteHost (wwnet/rhost.h/.cpp)
// Implements reliable delivery: send with retry until ACK received, receive in-order.

// An entry in the reliable send queue
data class PendingPacket(
    val packet: Packet,
    val rawData: ByteArray,        // pre-serialized wire bytes
    val sentTimeMs: Long,
    val firstSendTimeMs: Long,
    var resendCount: Int = 0,
) {
    val isResend: Boolean get() = resendCount > 0
}

// C++: reliable send list + receive list in cRemoteHost
class ReliableChannel {
    // Send queue: packets waiting for ACK, keyed by packet id
    private val sendQueue = mutableMapOf<Int, PendingPacket>()

    // Receive queue: out-of-order packets waiting to be delivered in sequence
    private val receiveBuffer = mutableMapOf<Int, Packet>()

    // C++: ReliablePacketSendId / ReliablePacketRcvId
    var nextSendId: Int = 0
        private set
    var nextReceiveId: Int = 0
        private set

    // C++: per-keepalive-interval packet loss stats
    private var sentSinceReset: Int = 0
    private var resentSinceReset: Int = 0

    // Enqueue a packet for reliable sending. Assigns the next send ID.
    fun enqueue(packet: Packet, rawData: ByteArray, nowMs: Long = System.currentTimeMillis()): Int {
        val id = nextSendId++
        packet.id = id
        sendQueue[id] = PendingPacket(packet, rawData, nowMs, nowMs)
        sentSinceReset++
        return id
    }

    // Mark a packet as acknowledged. Returns the PendingPacket for ping measurement.
    // C++: Remove_Packet from reliable send list
    fun ack(packetId: Int): PendingPacket? {
        return sendQueue.remove(packetId)
    }

    // Get packets that need to be (re)sent based on the resend timeout.
    // Returns packets that haven't been ACKed and whose send time is older than timeoutMs.
    fun getPacketsToResend(nowMs: Long, timeoutMs: Int): List<PendingPacket> {
        return sendQueue.values.filter { pending ->
            nowMs - pending.sentTimeMs >= timeoutMs
        }
    }

    // Update send time after a resend attempt
    fun markResent(packetId: Int, nowMs: Long) {
        sendQueue[packetId]?.let { pending ->
            sendQueue[packetId] = pending.copy(
                sentTimeMs = nowMs,
                resendCount = pending.resendCount + 1,
            )
            resentSinceReset++
        }
    }

    // Receive a reliable packet. Returns the next in-order packet to deliver (if any),
    // or null if we need to wait for a gap to be filled.
    // C++: unreliable receive handling in Service_Read
    fun receive(packet: Packet): Packet? {
        val id = packet.id

        if (id < nextReceiveId) {
            // Duplicate: already delivered this ID
            return null
        }

        receiveBuffer[id] = packet

        if (!receiveBuffer.containsKey(nextReceiveId)) return null
        val delivered = receiveBuffer.remove(nextReceiveId)!!
        nextReceiveId++
        return delivered
    }

    // C++: packet loss percentage for keepalive reporting.
    // Returns fraction of sends that required a resend in the current interval.
    // Resets counters so each keepalive interval reflects only that interval's traffic.
    fun computeAndResetLoss(): Float {
        val loss = if (sentSinceReset > 0) resentSinceReset.toFloat() / sentSinceReset else 0f
        sentSinceReset = 0
        resentSinceReset = 0
        return loss.coerceIn(0f, 1f)
    }

    // Deliver all buffered in-order packets (call repeatedly until null)
    fun drainDeliverable(): Sequence<Packet> = sequence {
        while (receiveBuffer.containsKey(nextReceiveId)) {
            yield(receiveBuffer.remove(nextReceiveId)!!)
            nextReceiveId++
        }
    }

    val sendQueueSize: Int get() = sendQueue.size
    val receiveBufferSize: Int get() = receiveBuffer.size
}
