package ccr.net.reliability

import ccr.net.protocol.Packet
import ccr.net.protocol.PacketType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReliableChannelTest {

    private fun makePacket(id: Int): Packet {
        val p = Packet()
        p.type = PacketType.RELIABLE
        p.id = id
        p.senderId = 0
        return p
    }

    @Test
    fun `enqueue assigns sequential IDs starting at 0`() {
        val ch = ReliableChannel()
        val p1 = makePacket(0)
        val p2 = makePacket(0)
        val id1 = ch.enqueue(p1, ByteArray(7))
        val id2 = ch.enqueue(p2, ByteArray(7))
        assertEquals(0, id1)
        assertEquals(1, id2)
    }

    @Test
    fun `ack removes packet from send queue`() {
        val ch = ReliableChannel()
        val p = makePacket(0)
        ch.enqueue(p, ByteArray(7))
        assertEquals(1, ch.sendQueueSize)
        assertNotNull(ch.ack(0))
        assertEquals(0, ch.sendQueueSize)
    }

    @Test
    fun `getPacketsToResend returns packets older than timeout`() {
        val ch = ReliableChannel()
        val p = makePacket(0)
        val nowMs = System.currentTimeMillis()
        ch.enqueue(p, ByteArray(7), nowMs - 1000) // sent 1 second ago

        val toResend = ch.getPacketsToResend(nowMs, 500)
        assertEquals(1, toResend.size)
    }

    @Test
    fun `getPacketsToResend excludes recent packets`() {
        val ch = ReliableChannel()
        val p = makePacket(0)
        ch.enqueue(p, ByteArray(7)) // sent now

        val toResend = ch.getPacketsToResend(System.currentTimeMillis(), 1000)
        assertEquals(0, toResend.size)
    }

    @Test
    fun `in-order receive delivers immediately`() {
        val ch = ReliableChannel()
        val p = makePacket(0)
        val delivered = ch.receive(p)
        assertNotNull(delivered)
        assertEquals(0, delivered.id)
        assertEquals(1, ch.nextReceiveId)
    }

    @Test
    fun `out-of-order receive buffers then delivers in order`() {
        val ch = ReliableChannel()
        // Receive packet 1 before 0
        val p1 = makePacket(1)
        assertNull(ch.receive(p1))  // buffered, can't deliver yet (waiting for 0)
        assertEquals(1, ch.receiveBufferSize)

        val p0 = makePacket(0)
        val delivered = ch.receive(p0)  // 0 arrives, can deliver 0 and then drain 1
        assertNotNull(delivered)
        assertEquals(0, delivered.id)

        // Should be able to drain packet 1 now
        val drained = ch.drainDeliverable().toList()
        assertEquals(1, drained.size)
        assertEquals(1, drained[0].id)
    }

    @Test
    fun `duplicate packet is discarded`() {
        val ch = ReliableChannel()
        val p0a = makePacket(0)
        val p0b = makePacket(0)
        assertNotNull(ch.receive(p0a))
        assertNull(ch.receive(p0b))  // duplicate, already delivered
    }

    @Test
    fun `computeAndResetLoss returns 0 when no packets sent`() {
        val ch = ReliableChannel()
        assertEquals(0f, ch.computeAndResetLoss())
    }

    @Test
    fun `computeAndResetLoss returns 0 when all packets sent without resend`() {
        val ch = ReliableChannel()
        val p = Packet()
        ch.enqueue(p, ByteArray(4))
        assertEquals(0f, ch.computeAndResetLoss())
    }

    @Test
    fun `computeAndResetLoss returns resend fraction`() {
        val ch = ReliableChannel()
        val p1 = Packet(); ch.enqueue(p1, ByteArray(4))
        val p2 = Packet(); ch.enqueue(p2, ByteArray(4))
        val p3 = Packet(); ch.enqueue(p3, ByteArray(4))
        val p4 = Packet(); ch.enqueue(p4, ByteArray(4))
        // Resend p1 once
        ch.markResent(p1.id, System.currentTimeMillis())
        // Loss = 1 resend / 4 sent = 0.25
        assertEquals(0.25f, ch.computeAndResetLoss())
    }

    @Test
    fun `computeAndResetLoss resets counters after call`() {
        val ch = ReliableChannel()
        val p = Packet(); ch.enqueue(p, ByteArray(4))
        ch.markResent(p.id, System.currentTimeMillis())
        ch.computeAndResetLoss()  // first call resets
        // Second call should return 0 (counters reset)
        assertEquals(0f, ch.computeAndResetLoss())
    }
}
