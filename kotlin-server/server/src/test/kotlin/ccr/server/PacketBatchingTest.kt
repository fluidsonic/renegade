package ccr.server

import ccr.net.protocol.PacketCombiner
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PacketBatchingTest {

    private val addr = InetSocketAddress("127.0.0.1", 4848)

    @Test
    fun `three same-size packets to same host combine into one datagram`() {
        val pkt1 = ByteArray(20) { it.toByte() }
        val pkt2 = ByteArray(20) { (it + 1).toByte() }
        val pkt3 = ByteArray(20) { (it + 2).toByte() }

        val datagrams = PacketCombiner.combine(listOf(addr to pkt1, addr to pkt2, addr to pkt3))

        assertEquals(1, datagrams.size, "three small same-size packets should fit in one datagram")
        // 2-byte group header + 20 (first) + (1+20) × 2 (delta-header + data for packets 2,3) = 64 bytes
        assertEquals(64, datagrams[0].data.size)
    }

    @Test
    fun `single packet still produces one datagram`() {
        val pkt = ByteArray(30) { it.toByte() }
        val datagrams = PacketCombiner.combine(listOf(addr to pkt))
        assertEquals(1, datagrams.size)
    }

    @Test
    fun `packets exceeding MTU split across multiple datagrams`() {
        val large = (1..50).map { addr to ByteArray(20) { i -> i.toByte() } }
        val datagrams = PacketCombiner.combine(large)
        assertTrue(datagrams.size > 1, "50 × 20-byte packets should not all fit in one datagram")
        for (dg in datagrams) {
            assertTrue(dg.data.size <= 540, "each datagram must be ≤ MTU")
        }
    }
}
