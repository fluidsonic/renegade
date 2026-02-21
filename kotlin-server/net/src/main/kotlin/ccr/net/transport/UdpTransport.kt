package ccr.net.transport

import ccr.net.protocol.OutgoingDatagram
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector

// Max UDP payload size: slightly above the PacketManager MTU (540) to accommodate
// combined datagrams and any framing overhead.
private const val MAX_DATAGRAM_SIZE = 600

data class ReceivedDatagram(
    val data: ByteArray,
    val length: Int,
    val source: InetSocketAddress,
) {
    // ByteArray equality/hashCode by content, not identity.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReceivedDatagram) return false
        return length == other.length &&
            source == other.source &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + length
        result = 31 * result + source.hashCode()
        return result
    }
}

/**
 * Non-blocking UDP transport backed by a [DatagramChannel] and a NIO [Selector].
 *
 * Inbound datagrams received from the network are placed on [receiveChannel].
 * Outbound datagrams are submitted via [enqueue] and are drained eagerly inside
 * [ioLoop] after every selector wake-up.
 *
 * Typical usage:
 * ```
 * val transport = UdpTransport(port = 4848)
 * val job = launch(Dispatchers.IO) { transport.ioLoop() }
 * // ...
 * transport.close()
 * job.join()
 * ```
 */
class UdpTransport(val port: Int) {

    val receiveChannel: Channel<ReceivedDatagram> = Channel(Channel.BUFFERED)

    // Internal send queue; callers use enqueue() rather than accessing this directly.
    private val sendChannel: Channel<OutgoingDatagram> = Channel(Channel.BUFFERED)

    /**
     * Enqueues a datagram for sending. Drops silently if the channel is full
     * (non-blocking; the caller must handle back-pressure at a higher layer).
     */
    fun enqueue(datagram: OutgoingDatagram) {
        sendChannel.trySend(datagram)
    }

    /**
     * Runs the selector I/O loop on whichever dispatcher the caller provides
     * (intended to be [kotlinx.coroutines.Dispatchers.IO]).
     *
     * The function returns when the coroutine is cancelled or an unrecoverable
     * exception occurs. Both channels are closed before the function returns.
     */
    suspend fun ioLoop() {
        val channel = DatagramChannel.open(StandardProtocolFamily.INET).apply {
            configureBlocking(false)
            bind(InetSocketAddress(port))
        }
        println("[UDP] socket bound to 0.0.0.0:$port")

        val selector = Selector.open()
        channel.register(selector, SelectionKey.OP_READ)

        val receiveBuffer = ByteBuffer.allocateDirect(MAX_DATAGRAM_SIZE)

        try {
            while (currentCoroutineContext().isActive) {
                // Block at most 10 ms so we can check for cancellation and drain
                // the send queue even when no inbound traffic arrives.
                val ready = selector.select(10L)

                if (ready > 0) {
                    val keys = selector.selectedKeys()
                    val iter = keys.iterator()
                    while (iter.hasNext()) {
                        val key = iter.next()
                        iter.remove()

                        if (key.isReadable) {
                            receiveBuffer.clear()
                            val source = channel.receive(receiveBuffer) as? InetSocketAddress
                            if (source != null) {
                                receiveBuffer.flip()
                                val length = receiveBuffer.remaining()
                                val data = ByteArray(length)
                                receiveBuffer.get(data)
                                println("[UDP] received $length bytes from $source (first bytes: ${data.take(8).joinToString(" ") { "%02x".format(it) }})")
                                val result = receiveChannel.trySend(ReceivedDatagram(data, length, source))
                                if (result.isFailure) println("[UDP] WARNING: receiveChannel full, datagram from $source dropped")
                            }
                        }
                    }
                }

                // Drain the outbound queue after every select, regardless of
                // whether a key was ready. This keeps latency low when the
                // network is quiet on the inbound side.
                drainSendQueue(channel)
            }
        } catch (e: CancellationException) {
            // Normal shutdown — re-throw so the coroutine machinery sees it.
            throw e
        } finally {
            selector.close()
            channel.close()
            receiveChannel.close()
            sendChannel.close()
        }
    }

    /**
     * Signals the transport to stop. Closes [sendChannel] so that [ioLoop]
     * will drain any remaining outbound datagrams and then exit on the next
     * cancellation check. Callers should also cancel the coroutine that runs
     * [ioLoop] and join it to ensure the underlying socket is fully closed.
     */
    fun close() {
        sendChannel.close()
        receiveChannel.close()
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun drainSendQueue(channel: DatagramChannel) {
        while (true) {
            val datagram = sendChannel.tryReceive().getOrNull() ?: break
            val buf = ByteBuffer.wrap(datagram.data)
            try {
                channel.send(buf, datagram.destination)
            } catch (_: Exception) {
                // A single failed send should not tear down the loop; log or
                // count at a higher layer if needed.
            }
        }
    }
}
