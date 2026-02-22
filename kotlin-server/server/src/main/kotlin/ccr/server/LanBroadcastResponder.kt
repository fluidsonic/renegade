package ccr.server

import ccr.net.bitstream.BitStream
import ccr.net.bitstream.EncoderRegistry
import ccr.net.connection.ConnectionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

// C++: LAN broadcast matching the Renegade client's LAN discovery protocol.

private const val LAN_BROADCAST_PORT = 3373
private const val LAN_MESSAGE_POSITION: Byte = 0
private const val GAME_TYPE_CNC = 2
private const val BROADCAST_INTERVAL_MS = 1_000L

/**
 * Returns the outbound LAN IPv4 address as a 32-bit integer (network byte order).
 * Uses the routing trick: connecting a UDP socket (no packets sent) causes the OS
 * to select the correct outbound interface via the routing table.
 * Falls back to iterating interfaces if the routing trick fails.
 * Returns 0 if no suitable address is found.
 */
fun detectLocalIp(): Int {
    // Routing trick: connect to a dummy external address; OS picks the right interface.
    try {
        java.net.DatagramSocket().use { socket ->
            socket.connect(java.net.InetAddress.getByName("1.1.1.1"), 53)
            val addr = socket.localAddress
            if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                return ByteBuffer.wrap(addr.address).int
            }
        }
    } catch (_: Exception) { }

    // Fallback: first non-loopback IPv4
    for (iface in NetworkInterface.getNetworkInterfaces() ?: return 0) {
        if (iface.isLoopback || !iface.isUp) continue
        for (addr in iface.inetAddresses) {
            if (addr is java.net.Inet4Address) {
                return ByteBuffer.wrap(addr.address).int
            }
        }
    }
    return 0
}

class LanBroadcastResponder(
    private val config: ServerConfig,
    private val connectionManager: ConnectionManager,
    private val mapNameCrc: () -> Int,
) {
    private var broadcastNumber = 0

    /** Broadcasts server presence every 1 second until cancelled. */
    suspend fun broadcastLoop() {
        val channel = DatagramChannel.open().apply {
            setOption(java.net.StandardSocketOptions.SO_BROADCAST, true)
            bind(null) // ephemeral port
        }
        val localIp = detectLocalIp()
        val localIpStr = "%d.%d.%d.%d".format(
            (localIp ushr 24) and 0xFF, (localIp ushr 16) and 0xFF,
            (localIp ushr 8) and 0xFF, localIp and 0xFF,
        )
        println("[LAN] broadcast loop started — telling clients to connect to $localIpStr:${config.gamePort}")
        try {
            while (currentCoroutineContext().isActive) {
                try {
                    sendBroadcast(channel, localIp)
                    if (broadcastNumber == 0) println("[LAN] first broadcast sent ok")
                } catch (e: java.io.IOException) {
                    if (broadcastNumber == 0) println("[LAN] broadcast failed: $e")
                }
                broadcastNumber++
                delay(BROADCAST_INTERVAL_MS)
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            channel.close()
        }
    }

    private fun sendBroadcast(channel: DatagramChannel, localIp: Int = detectLocalIp()) {
        EncoderRegistry.setCompressionEnabled(true)
        val bs = BitStream()

        bs.addByte(LAN_MESSAGE_POSITION)
        bs.addWideString(config.serverName)
        bs.addInt(broadcastNumber)
        bs.addBool(true) // is_hosting
        bs.addInt(GAME_TYPE_CNC)

        // Tier 1 data
        bs.addInt(localIp)
        bs.addWideString(config.ownerName)
        bs.addWideString(config.gameTitle, permitEmpty = true)
        bs.addInt(config.gamePort)
        bs.addInt(connectionManager.getConnectedCount())
        bs.addInt(config.maxPlayers)
        bs.addInt(config.versionNumber)
        bs.addInt(config.exeCrc)
        bs.addInt(config.stringsCrc)
        bs.addBool(config.isDedicated)
        bs.addBool(config.isTeamChangingAllowed)
        bs.addBool(config.isPassworded)
        bs.addBool(config.isLaddered)
        bs.addBool(config.isClanGame)
        bs.addInt(mapNameCrc()) // C++: CRC_Stringi(MapName)
        bs.addInt(config.modNameCrc) // C++: CRC_Stringi(ModName)

        val data = bs.buffer.copyOf(bs.compressedSizeBytes)
        val dest = InetSocketAddress("255.255.255.255", LAN_BROADCAST_PORT)
        channel.send(ByteBuffer.wrap(data), dest)
    }

}
