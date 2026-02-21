package ccr.net.rcon

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

// C++: servercontrolsocket.cpp — RCON UDP server.

private const val LOOPBACK = "127.0.0.1"
private const val MAX_PAYLOAD_SIZE = 512
private const val MAX_MESSAGE_SIZE = 499
private const val RCON_REQUEST = 0
private const val RCON_RESPONSE = 1

/**
 * UDP-based Remote Console (RCON) server.
 *
 * Wire format: `[CRC: 4 bytes LE][encrypted payload]`
 * Decrypted payload: `[Type: 4 bytes LE int][Message: null-terminated, max 500 bytes]`
 *
 * @param port             UDP port to listen on (default 63999)
 * @param password         Required password; empty string means no password required
 * @param remoteAdminAllowed  If false, only 127.0.0.1 connections are allowed
 * @param welcomeMessage   Sent after successful authentication
 * @param commandHandler   Returns response text for a given command string
 */
class RconServer(
    val port: Int = 63999,
    val password: String = "",
    val remoteAdminAllowed: Boolean = false,
    val welcomeMessage: String = "",
    val commandHandler: (String) -> String = { "" },
) {
    private val sessions = mutableMapOf<InetSocketAddress, RconSession>()

    /** Runs the RCON loop until the coroutine is cancelled. */
    suspend fun run() {
        val channel = DatagramChannel.open().apply {
            configureBlocking(false)
            bind(InetSocketAddress(port))
        }
        val selector = Selector.open()
        channel.register(selector, SelectionKey.OP_READ)
        val buf = ByteBuffer.allocate(MAX_PAYLOAD_SIZE + 4)

        try {
            while (currentCoroutineContext().isActive) {
                selector.select(100L)

                val nowMs = System.currentTimeMillis()
                expireSessions(channel, nowMs)

                val keys = selector.selectedKeys()
                val iter = keys.iterator()
                while (iter.hasNext()) {
                    val key = iter.next()
                    iter.remove()
                    if (!key.isReadable) continue

                    buf.clear()
                    val source = channel.receive(buf) as? InetSocketAddress ?: continue
                    buf.flip()
                    if (buf.remaining() < 5) continue // too short: 4 CRC + at least 1 payload byte

                    handleDatagram(channel, source, buf, nowMs)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            selector.close()
            channel.close()
        }
    }

    // ---- Internal ----

    private fun handleDatagram(
        channel: DatagramChannel,
        source: InetSocketAddress,
        buf: ByteBuffer,
        nowMs: Long,
    ) {
        if (!remoteAdminAllowed && source.address.hostAddress != LOOPBACK) return

        val raw = ByteArray(buf.remaining())
        buf.get(raw)

        // Verify CRC: first 4 bytes are CRC of the remaining bytes
        if (raw.size < 5) return
        val expectedCrc = ByteBuffer.wrap(raw, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val payload = raw.copyOfRange(4, raw.size)
        val actualCrc = computeCrc(payload)
        if (expectedCrc != actualCrc) return

        // Decrypt
        val session = sessions.getOrPut(source) { RconSession(source) }
        val key = deriveKey(password)
        val decrypted = decrypt(payload, key)

        // Parse type (4 bytes LE) + null-terminated message
        if (decrypted.size < 4) return
        val msgType = ByteBuffer.wrap(decrypted, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        if (msgType != RCON_REQUEST) return

        val messageBytes = decrypted.copyOfRange(4, decrypted.size)
        val nullIdx = messageBytes.indexOfFirst { it == 0.toByte() }
        val message = if (nullIdx >= 0) {
            String(messageBytes, 0, nullIdx, Charsets.US_ASCII)
        } else {
            String(messageBytes, Charsets.US_ASCII)
        }

        session.touch(nowMs)
        dispatch(channel, source, session, message, nowMs)
    }

    private fun dispatch(
        channel: DatagramChannel,
        source: InetSocketAddress,
        session: RconSession,
        message: String,
        nowMs: Long,
    ) {
        when {
            message.equals("CONNECT", ignoreCase = true) -> {
                if (password.isEmpty()) {
                    session.isAuthenticated = true
                    sendMessage(channel, source, "Password accepted.\n")
                    if (welcomeMessage.isNotEmpty()) sendMessage(channel, source, welcomeMessage)
                } else {
                    sendMessage(channel, source, "Password required:")
                }
            }
            !session.isAuthenticated -> {
                // Treat as password attempt
                if (message == password) {
                    session.isAuthenticated = true
                    sendMessage(channel, source, "Password accepted.\n")
                    if (welcomeMessage.isNotEmpty()) sendMessage(channel, source, welcomeMessage)
                } else {
                    sendMessage(channel, source, "Invalid password.")
                    sessions.remove(source)
                }
            }
            message.equals("BYE", ignoreCase = true) -> {
                sendMessage(channel, source, "Goodbye!\n")
                sessions.remove(source)
            }
            else -> {
                val response = commandHandler(message)
                sendChunked(channel, source, response)
            }
        }
    }

    private fun sendChunked(channel: DatagramChannel, dest: InetSocketAddress, text: String) {
        if (text.isEmpty()) {
            sendMessage(channel, dest, "")
            return
        }
        var offset = 0
        while (offset < text.length) {
            val chunk = text.substring(offset, minOf(offset + MAX_MESSAGE_SIZE, text.length))
            sendMessage(channel, dest, chunk)
            offset += MAX_MESSAGE_SIZE
        }
    }

    private fun sendMessage(channel: DatagramChannel, dest: InetSocketAddress, text: String) {
        val msgBytes = text.toByteArray(Charsets.US_ASCII)
        // Payload: [type: 4 bytes LE][message][null terminator]
        val payload = ByteArray(4 + msgBytes.size + 1)
        ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).putInt(RCON_RESPONSE)
        System.arraycopy(msgBytes, 0, payload, 4, msgBytes.size)
        payload[4 + msgBytes.size] = 0

        val key = deriveKey(password)
        val encrypted = encrypt(payload, key)
        val crc = computeCrc(encrypted)

        val packet = ByteArray(4 + encrypted.size)
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).putInt(crc)
        System.arraycopy(encrypted, 0, packet, 4, encrypted.size)

        channel.send(ByteBuffer.wrap(packet), dest)
    }

    private fun expireSessions(channel: DatagramChannel, nowMs: Long) {
        val expired = sessions.values.filter { it.isTimedOut(nowMs) }
        for (session in expired) {
            sendMessage(channel, session.address, "** Connection timed out - Bye! **\n")
            sessions.remove(session.address)
        }
    }
}
