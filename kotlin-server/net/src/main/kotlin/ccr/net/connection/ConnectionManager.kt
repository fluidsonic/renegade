package ccr.net.connection

import ccr.net.protocol.Packet
import ccr.net.protocol.PacketType
import ccr.net.protocol.RefusalCode
import java.net.InetSocketAddress

// C++: cConnection (server-side) in wwnet/connect.h/.cpp
// Orchestrates all per-client RemoteHosts: slot allocation, dispatch, handshake, keepalive, timeout.

// Callback invoked when a CONNECT_CS packet passes preliminary checks.
// Returns RefusalCode.CLIENT_ACCEPTED to allow, or a refusal code to deny.
// The packet's read position is at the application data (nickname, password, etc.)
typealias ApplicationAcceptanceHandler = (packet: Packet, address: InetSocketAddress) -> RefusalCode

// Callback invoked after a client is successfully connected.
typealias ConnHandler = (rhostId: Int, host: RemoteHost) -> Unit

// Callback invoked when a connection is broken (timeout or eviction).
typealias DisconnectHandler = (rhostId: Int) -> Unit

// Callback for application-level reliable/unreliable packets (not system packets).
typealias PacketHandler = (packet: Packet, rhostId: Int) -> Unit

class ConnectionManager(
    val maxPlayers: Int,
    private val localId: Int = 0,  // server is always ID 0
) {
    // C++: PRHost array — slot 0 is reserved; clients are 1..maxPlayers
    // C++: MinRHost=1, MaxRHost=maxPlayers
    private val clients = arrayOfNulls<RemoteHost>(maxPlayers + 1)  // index 0 unused

    // Callbacks
    var applicationAcceptanceHandler: ApplicationAcceptanceHandler = { _, _ -> RefusalCode.CLIENT_ACCEPTED }
    var connHandler: ConnHandler = { _, _ -> }
    var disconnectHandler: DisconnectHandler = { _ -> }
    var serverPacketHandler: PacketHandler = { _, _ -> }

    // C++: RefusalPacketSendId — per-server counter for refusal packets (not per-client)
    private var refusalPacketSendId: Int = 0

    var serviceCount: Int = 0
        private set

    // Build a Packet with the server's sender ID
    private fun serverPacket(type: PacketType, id: Int): Packet {
        val p = Packet()
        p.type = type
        p.id = id
        p.senderId = localId
        return p
    }

    /**
     * Result of [processConnectionRequest]:
     * - [packet]: ACCEPT_SC or REFUSAL_SC to send back
     * - [rhostId] and [host]: non-null when accepted, so the caller can send ACCEPT first,
     *   then invoke [connHandler] to send game objects in the correct order.
     */
    data class ConnectionResult(val packet: Packet, val rhostId: Int = -1, val host: RemoteHost? = null)

    // C++: Process_Connection_Request
    // Handles an incoming CONNECT_CS packet.
    // Returns a ConnectionResult with ACCEPT_SC or REFUSAL_SC, or null if already known.
    // IMPORTANT: the caller must send the ACCEPT_SC *before* calling connHandler, so that
    // the client learns its rhostId before receiving game objects.
    fun processConnectionRequest(
        packet: Packet,
        sourceAddress: InetSocketAddress,
        nowMs: Long = System.currentTimeMillis(),
    ): ConnectionResult? {
        // Check if we already know this address (duplicate/resent connect)
        for (id in 1..maxPlayers) {
            if (clients[id]?.address == sourceAddress) return null  // already connected, ignore
        }

        // Find a free slot
        val slotId = (1..maxPlayers).firstOrNull { clients[it] == null }

        if (slotId == null) {
            return ConnectionResult(buildRefusal(RefusalCode.GAME_FULL, sourceAddress))
        }

        // Application-level acceptance check
        val refusal = applicationAcceptanceHandler(packet, sourceAddress)
        if (refusal != RefusalCode.CLIENT_ACCEPTED) {
            return ConnectionResult(buildRefusal(refusal, sourceAddress))
        }

        // Read bandwidth budget override (BBO) from packet — C++: packet.Get(bbo) after app handler
        val bbo = packet.payload.getInt()

        // Allocate the slot
        val host = RemoteHost(slotId, sourceAddress, bbo.coerceAtLeast(1))
        clients[slotId] = host
        host.touch(nowMs)

        // Advance reliable receive ID for the connect packet (it's treated as reliable ID 0)
        host.reliable.receive(packet)

        // Build ACCEPT_SC (id=0). Do NOT call connHandler here — the caller must send
        // ACCEPT_SC over the wire first, then invoke connHandler to send game objects.
        val accept = buildAccept(slotId, host)
        return ConnectionResult(accept, slotId, host)
    }

    // C++: Send_Accept_Sc
    private fun buildAccept(newRhostId: Int, host: RemoteHost): Packet {
        val packetId = host.reliable.nextSendId  // 0 (called before connHandler)
        val p = serverPacket(PacketType.ACCEPT_SC, packetId)
        p.payload.addInt(newRhostId)
        host.reliable.enqueue(p, Packet.buildWirePacket(p))
        return p
    }

    // C++: Send_Refusal_Sc
    private fun buildRefusal(code: RefusalCode, address: InetSocketAddress): Packet {
        val p = Packet()
        p.type = PacketType.REFUSAL_SC
        p.id = refusalPacketSendId++
        p.senderId = localId
        p.payload.addInt(code.id)
        return p
    }

    // C++: Send_Ack — ACK packet for a reliable packet
    fun buildAck(packetId: Int): Packet {
        val p = Packet()
        p.type = PacketType.ACK
        p.id = packetId
        p.senderId = localId
        return p
    }

    // C++: Send_Keepalives
    // Returns keepalive packets to send for each host where keepalive is due.
    fun getKeepalives(
        nowMs: Long = System.currentTimeMillis(),
    ): List<Pair<RemoteHost, Packet>> {
        val result = mutableListOf<Pair<RemoteHost, Packet>>()
        for (id in 1..maxPlayers) {
            val host = clients[id] ?: continue
            if (!host.isKeepaliveDue(nowMs)) continue

            val serviceRate = 1000 * (serviceCount - host.lastServiceCount)
                .toLong() / (nowMs - host.lastKeepaliveTimeMs).coerceAtLeast(1)

            host.lastServiceCount = serviceCount
            host.markKeepaliveSent(nowMs)

            val packetId = host.reliable.nextSendId
            val p = serverPacket(PacketType.KEEPALIVE, packetId)
            p.payload.addFloat(host.packetLossPc)
            p.payload.addInt(serviceRate.toInt())

            host.reliable.enqueue(p, Packet.buildWirePacket(p))
            result.add(Pair(host, p))
        }
        return result
    }

    // Check for timed-out connections. Returns list of disconnected host IDs.
    fun checkTimeouts(nowMs: Long = System.currentTimeMillis()): List<Int> {
        val disconnected = mutableListOf<Int>()
        for (id in 1..maxPlayers) {
            val host = clients[id] ?: continue
            if (host.mustEvict || host.isTimedOut(nowMs)) {
                clients[id] = null
                disconnectHandler(id)
                disconnected.add(id)
            }
        }
        return disconnected
    }

    // Get packets to resend for all hosts and mark them as resent (updating sentTimeMs + resendCount).
    fun getResendPackets(nowMs: Long = System.currentTimeMillis()): List<Pair<RemoteHost, ccr.net.reliability.PendingPacket>> {
        val result = mutableListOf<Pair<RemoteHost, ccr.net.reliability.PendingPacket>>()
        for (id in 1..maxPlayers) {
            val host = clients[id] ?: continue
            for (pending in host.reliable.getPacketsToResend(nowMs, host.resendPolicy.timeoutMs)) {
                result.add(Pair(host, pending))
                host.reliable.markResent(pending.packet.id, nowMs)
            }
        }
        return result
    }

    // Process an ACK packet. Returns the RemoteHost or null if unknown sender.
    fun processAck(packet: Packet): RemoteHost? {
        val host = clients[packet.senderId] ?: return null
        val pending = host.reliable.ack(packet.id) ?: return host

        // C++: only non-resent packets contribute to ping measurement
        if (!pending.isResend) {
            val pingMs = (System.currentTimeMillis() - pending.firstSendTimeMs).toInt()
            host.resendPolicy.recordPing(pingMs)
        }
        return host
    }

    // Process an incoming packet. Returns the host or null if sender ID is invalid.
    fun processIncoming(packet: Packet, nowMs: Long = System.currentTimeMillis()): RemoteHost? {
        val senderId = packet.senderId
        if (senderId < 1 || senderId > maxPlayers) return null
        val host = clients[senderId] ?: return null
        host.touch(nowMs)
        serviceCount++
        return host
    }

    fun getHost(id: Int): RemoteHost? = clients.getOrNull(id)
    fun getConnectedCount(): Int = (1..maxPlayers).count { clients[it] != null }
    fun evict(rhostId: Int) { clients[rhostId]?.mustEvict = true }
}
