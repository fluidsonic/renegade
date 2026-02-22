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

        // Datagram: [header:2][p1:7][delta-hdr:1][p2:7] = 17 bytes
        assertEquals(2 + 7 + 1 + 7, datagrams[0].data.size)

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

    @Test
    fun `combine writes 0x00 delta header before secondary packets`() {
        // The C++ Break_Packet reads a 1-byte PacketDeltaHeaderStruct before packets 2..N.
        // combine() must write 0x00 (ChunkPack=0, BytePack=0) before each secondary packet.
        val p1 = ByteArray(7) { 0xAA.toByte() }
        val p2 = ByteArray(7) { 0xBB.toByte() }
        val p3 = ByteArray(7) { 0xCC.toByte() }

        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, p1), Pair(addr1, p2), Pair(addr1, p3)))
        assertEquals(1, datagrams.size)
        // Wire: [header:2][p1:7][0x00:1][p2:7][0x00:1][p3:7] = 25 bytes
        assertEquals(2 + 7 + 1 + 7 + 1 + 7, datagrams[0].data.size)

        val d = datagrams[0].data
        // Delta header before p2 is at offset 2+7=9
        assertEquals(0x00.toByte(), d[9], "Delta header before p2 must be 0x00")
        // Delta header before p3 is at offset 2+7+1+7=17
        assertEquals(0x00.toByte(), d[17], "Delta header before p3 must be 0x00")

        // Round-trip must recover all three packets intact
        val packets = PacketCombiner.split(d, d.size)
        assertEquals(3, packets.size)
        assertTrue(p1.contentEquals(packets[0].data))
        assertTrue(p2.contentEquals(packets[1].data))
        assertTrue(p3.contentEquals(packets[2].data))
    }

    @Test
    fun `non-consecutive same-size packets preserve delivery order`() {
        // C++ Take_Packet groups only consecutive same-size packets.
        // Packets A(25B), B(21B), C(25B) must be delivered as A, B, C — not A, C, B.
        val pA = ByteArray(25) { it.toByte() }
        val pB = ByteArray(21) { it.toByte() }
        val pC = ByteArray(25) { (it + 100).toByte() }

        val datagrams = PacketCombiner.combine(listOf(Pair(addr1, pA), Pair(addr1, pB), Pair(addr1, pC)))
        assertEquals(1, datagrams.size)

        val packets = PacketCombiner.split(datagrams[0].data, datagrams[0].data.size)
        assertEquals(3, packets.size)
        assertTrue(pA.contentEquals(packets[0].data), "first packet must be A (25B)")
        assertTrue(pB.contentEquals(packets[1].data), "second packet must be B (21B), not C")
        assertTrue(pC.contentEquals(packets[2].data), "third packet must be C (25B)")
    }

    @Test
    fun `MTU overflow splits into multiple datagrams with correct MorePackets flag`() {
        // Use 35-byte packets (matches real server scenario for buildings+events).
        // Capacity per datagram: header(2) + 35 + 13×36 = 505 bytes → 14 packets fit.
        // 20 packets → datagram 1 has 14, datagram 2 has 6.
        // The MorePackets bit on the last group header of datagram 1 must be 0 (not 1).
        val packetSize = 35
        val packets = (0 until 20).map { i ->
            Pair(addr1, ByteArray(packetSize) { b -> (i * 7 + b).toByte() })
        }

        val datagrams = PacketCombiner.combine(packets)
        assertEquals(2, datagrams.size, "20 packets of 35B should produce exactly 2 datagrams")

        // Verify last group header in datagram 1 has MorePackets=0
        val d1 = datagrams[0].data
        val header1Low = d1[0].toInt() and 0xFF
        val header1High = d1[1].toInt() and 0xFF
        val morePackets1 = (header1High shr 7) and 1
        assertEquals(0, morePackets1, "Last group header in datagram 1 must have MorePackets=0")

        // All 20 packets must round-trip correctly
        val recovered = datagrams.flatMap { dg -> PacketCombiner.split(dg.data, dg.data.size) }
        assertEquals(20, recovered.size, "All 20 packets must be recovered")
        for (i in 0 until 20) {
            val expected = ByteArray(packetSize) { b -> (i * 7 + b).toByte() }
            assertTrue(expected.contentEquals(recovered[i].data), "Packet $i must round-trip intact")
        }
    }

    @Test
    fun `MTU overflow with mixed sizes clears MorePackets on datagram boundary`() {
        // Mix of small (17B) and large (77B) packets that together overflow one datagram.
        // The plan's real scenario: 11 size-groups across 3 datagrams.
        // Here we use 2 sizes: many 77B packets to force a split.
        // 77B packets: header(2) + 77 + 6×78 = 2+77+468 = 547 > MTU → only 6 fit (6×78+77+2=547>540, so 5 fit)
        // Actually: 2 + 77 + N×78 ≤ 540 → N×78 ≤ 461 → N=5, total=6 packets per datagram.
        // 12 packets → 2 datagrams.
        val packets = (0 until 12).map { i ->
            Pair(addr1, ByteArray(77) { b -> (i + b).toByte() })
        }

        val datagrams = PacketCombiner.combine(packets)
        assertEquals(2, datagrams.size, "12 packets of 77B should produce 2 datagrams")

        // Last group header of datagram 1 must have MorePackets=0
        val d1 = datagrams[0].data
        val morePackets1 = (d1[1].toInt() and 0x80) ushr 7
        assertEquals(0, morePackets1, "Datagram 1 last group header MorePackets must be 0")

        // All 12 packets must round-trip
        val recovered = datagrams.flatMap { dg -> PacketCombiner.split(dg.data, dg.data.size) }
        assertEquals(12, recovered.size)
        for (i in 0 until 12) {
            val expected = ByteArray(77) { b -> (i + b).toByte() }
            assertTrue(expected.contentEquals(recovered[i].data), "Packet $i must round-trip intact")
        }
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
