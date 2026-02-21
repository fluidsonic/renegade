package ccr.net.protocol

import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PacketCombinerTest {

    private val addr1 = InetSocketAddress("127.0.0.1", 4848)
    private val addr2 = InetSocketAddress("127.0.0.1", 9999)

    @Test
    fun `single packet round trip`() {
        val packetData = ByteArray(7) { it.toByte() }
        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, packetData)))
        assertEquals(1, datagrams.size)

        val packets = PacketCombiner.split(datagrams[0].data, datagrams[0].data.size)
        assertEquals(1, packets.size)
        assertTrue(packetData.contentEquals(packets[0].data))
    }

    @Test
    fun `two same-size packets combine into one datagram`() {
        val p1 = ByteArray(7) { 0xAA.toByte() }
        val p2 = ByteArray(7) { 0xBB.toByte() }

        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, p1), Pair(addr1, p2)))
        assertEquals(1, datagrams.size)

        // Datagram: [header:2][p1:7][p2:7] = 16 bytes
        assertEquals(2 + 7 + 7, datagrams[0].data.size)

        val packets = PacketCombiner.split(datagrams[0].data, datagrams[0].data.size)
        assertEquals(2, packets.size)
        assertTrue(p1.contentEquals(packets[0].data))
        assertTrue(p2.contentEquals(packets[1].data))
    }

    @Test
    fun `different-size packets produce separate groups`() {
        val small = ByteArray(7) { 0x11.toByte() }
        val large = ByteArray(14) { 0x22.toByte() }

        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, small), Pair(addr1, large)))
        assertEquals(1, datagrams.size)

        // Two groups: [header1:2][small:7][header2:2][large:14] = 25 bytes
        assertEquals(2 + 7 + 2 + 14, datagrams[0].data.size)

        val packets = PacketCombiner.split(datagrams[0].data, datagrams[0].data.size)
        assertEquals(2, packets.size)
        assertEquals(7, packets[0].length)
        assertEquals(14, packets[1].length)
    }

    @Test
    fun `different destinations produce separate datagrams`() {
        val p1 = ByteArray(7) { 0xAA.toByte() }
        val p2 = ByteArray(7) { 0xBB.toByte() }

        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, p1), Pair(addr2, p2)))
        assertEquals(2, datagrams.size)
    }

    @Test
    fun `group header encodes and decodes correctly`() {
        // Test the group header encoding directly through a round-trip
        val packets = (1..5).map { i -> Pair(addr1, ByteArray(7) { (i * 16 + it).toByte() }) }
        val datagrams = PacketCombiner.combine(packets)
        assertEquals(1, datagrams.size)

        val recovered = PacketCombiner.split(datagrams[0].data, datagrams[0].data.size)
        assertEquals(5, recovered.size)
        for (i in 0 until 5) {
            assertEquals(7, recovered[i].length)
        }
    }

    @Test
    fun `more-packets bit set when multiple groups in one datagram`() {
        val small = ByteArray(7) { 0x11.toByte() }
        val large = ByteArray(14) { 0x22.toByte() }

        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, small), Pair(addr1, large)))
        val header1 = (datagrams[0].data[0].toInt() and 0xFF) or ((datagrams[0].data[1].toInt() and 0xFF) shl 8)
        val morePackets = (header1 shr 15) and 1
        assertEquals(1, morePackets, "MorePackets bit should be set when multiple groups follow")
    }

    // --- Delta format tests (C++ client wire format) ---

    @Test
    fun `delta split - 4 ACK packets matching real network capture`() {
        // Real capture: client sends 4 ACK packets (id=0..3) in one datagram.
        // Base ACK(id=0): type=2(4b) id=0(28b) senderId=1(8b) bitLength=0(16b) → MSB-first = 20 00 00 00 01 00 00
        // Each subsequent ACK differs only in byte 3 (id low byte): Δ02 77 01 / Δ02 77 02 / Δ02 77 03
        //   DeltaHeader=0x02 (BytePack=1,ChunkPack=0), BitField=0x77 (all 7 bytes same except bit3), Patch=id

        // Group header: numPackets=4, packetSize=7, morePackets=false
        //   (4 & 0x1F) | ((7 & 0x3FF) << 5) | (0 << 15) = 4 | 224 = 228 = 0xE4
        val datagram = byteArrayOf(
            0xE4.toByte(), 0x00,                          // group header
            0x20, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,    // base: ACK id=0
            0x02, 0x77.toByte(), 0x01,                    // delta: ACK id=1
            0x02, 0x77.toByte(), 0x02,                    // delta: ACK id=2
            0x02, 0x77.toByte(), 0x03,                    // delta: ACK id=3
        )

        val packets = PacketCombiner.split(datagram, datagram.size, deltaFormat = true)
        assertEquals(4, packets.size)

        // All packets are 7 bytes
        packets.forEach { assertEquals(7, it.length) }

        // Verify each reconstructed packet: byte 3 = id, byte 4 = senderId=1
        assertEquals(0x00.toByte(), packets[0].data[3])  // id=0
        assertEquals(0x01.toByte(), packets[1].data[3])  // id=1
        assertEquals(0x02.toByte(), packets[2].data[3])  // id=2
        assertEquals(0x03.toByte(), packets[3].data[3])  // id=3

        // All share same type/senderId/bitLength from base
        for (p in packets) {
            assertEquals(0x20.toByte(), p.data[0])       // type bits
            assertEquals(0x01.toByte(), p.data[4])       // senderId=1
            assertEquals(0x00.toByte(), p.data[5])       // bitLength hi
            assertEquals(0x00.toByte(), p.data[6])       // bitLength lo
        }
    }

    @Test
    fun `delta split - single packet group needs no delta header`() {
        // numPackets=1, packetSize=7, morePackets=0: (1 & 0x1F) | ((7 & 0x3FF) << 5) | (0 << 15) = 0xE1
        val datagram = byteArrayOf(
            0xE1.toByte(), 0x00,                          // group header: numPackets=1, packetSize=7
            0x20, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,    // only packet (base, no delta)
        )
        val packets = PacketCombiner.split(datagram, datagram.size, deltaFormat = true)
        assertEquals(1, packets.size)
        assertEquals(7, packets[0].length)
        assertTrue(byteArrayOf(0x20, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00).contentEquals(packets[0].data))
    }

    @Test
    fun `max 31 packets per group`() {
        // 32 packets should result in at least 2 groups or 2 datagrams
        val packets = (1..32).map { Pair(addr1, ByteArray(7) { it.toByte() }) }
        val datagrams = PacketCombiner.combine(packets)
        val totalRecovered = datagrams.sumOf { dg ->
            PacketCombiner.split(dg.data, dg.data.size).size
        }
        assertEquals(32, totalRecovered)
    }
}
