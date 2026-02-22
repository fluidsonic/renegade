package ccr.net.protocol

import java.net.InetSocketAddress

// C++: PacketManagerClass in wwnet/packetmgr.h/.cpp
// Combines multiple small packets destined for the same address into a single UDP datagram.
//
// Wire format for a combined datagram (matches C++ PacketManagerClass::Take_Packet):
//   [GroupHeader1: 2 bytes][Packet1][0x00][Packet2][0x00][Packet3]...[GroupHeader2: 2 bytes][Packet4]...
//
// GroupHeader (16-bit little-endian C bitfield, #pragma pack(1)):
//   [NumPackets : 5 bits]   — number of packets in this group (1..31)
//   [PacketSize : 10 bits]  — size of each packet in bytes (all same size in a group)
//   [MorePackets : 1 bit]   — 1 if another group follows in this datagram
//
// The first packet in each group is written raw (no prefix). Every subsequent packet is
// preceded by a 1-byte PacketDeltaHeaderStruct (value 0x00: ChunkPack=0, BytePack=0),
// which the C++ receiver (Break_Packet) always reads for packets 2..N.

// MTU for the packet manager (with WRAPPER_CRC defined): 540 bytes
const val PACKET_MANAGER_MTU = 540
const val PACKET_MANAGER_MAX_PACKETS = 31

// Data ready to send: a datagram (one or more combined packets) to a destination
data class OutgoingDatagram(
    val destination: InetSocketAddress,
    val data: ByteArray,
)

// A single parsed packet extracted from a received datagram
data class IncomingPacket(
    val data: ByteArray,
    val length: Int,
)

object PacketCombiner {

    // C++: PacketManagerClass::Take_Packet
    // Takes a list of packets (all same size, same destination) and groups them
    // into combined datagrams not exceeding MTU.
    //
    // Packets of different sizes are put in separate groups, chained via MorePackets=1.
    // Returns list of datagrams to send.
    fun combine(
        packets: List<Pair<InetSocketAddress, ByteArray>>,
    ): List<OutgoingDatagram> {
        if (packets.isEmpty()) return emptyList()

        // Group by destination address, then by packet size
        val byDest = packets.groupBy { it.first }
        val result = mutableListOf<OutgoingDatagram>()

        for ((dest, destPackets) in byDest) {
            // Group consecutive same-size packets (mirrors C++ Take_Packet: each call accumulates
            // into the current group if sizes match, otherwise finalizes it and starts a new group).
            // Using groupBy would merge ALL same-size packets regardless of position, breaking order.
            val sizeList = mutableListOf<Pair<Int, List<Pair<InetSocketAddress, ByteArray>>>>()
            var gi = 0
            while (gi < destPackets.size) {
                val size = destPackets[gi].second.size
                val start = gi
                while (gi < destPackets.size && destPackets[gi].second.size == size) gi++
                sizeList.add(Pair(size, destPackets.subList(start, gi)))
            }

            val buf = ByteArray(PACKET_MANAGER_MTU)
            var pos = 0
            var lastHeaderPos = -1  // byte offset of the most recently written group header

            for ((groupIndex, entry) in sizeList.withIndex()) {
                val (packetSize, sizePackets) = entry
                val moreGroupsAfter = groupIndex < sizeList.size - 1

                // Split into chunks that fit in the MTU
                var i = 0
                while (i < sizePackets.size) {
                    // How many packets can fit in the remaining space?
                    // First packet costs packetSize; each subsequent costs packetSize+1 (data + 1-byte delta header).
                    val headerSize = 2
                    val remaining = PACKET_MANAGER_MTU - pos - headerSize
                    val maxFit = if (remaining >= packetSize) {
                        1 + (remaining - packetSize) / (packetSize + 1)
                    } else {
                        0
                    }

                    if (maxFit <= 0) {
                        // The datagram ends here. Clear MorePackets on the last group header written,
                        // since no more groups follow in this datagram.
                        if (lastHeaderPos >= 0) {
                            buf[lastHeaderPos + 1] = (buf[lastHeaderPos + 1].toInt() and 0x7F).toByte()
                        }
                        result.add(OutgoingDatagram(dest, buf.copyOf(pos)))
                        pos = 0
                        lastHeaderPos = -1
                        continue
                    }

                    val count = minOf(maxFit, PACKET_MANAGER_MAX_PACKETS, sizePackets.size - i)
                    val isLast = (i + count >= sizePackets.size) && !moreGroupsAfter
                    val morePackets = !isLast

                    // Write group header (16-bit, little-endian C bitfield)
                    val header = encodeGroupHeader(count, packetSize, morePackets)
                    lastHeaderPos = pos
                    buf[pos++] = header.toByte()
                    buf[pos++] = (header shr 8).toByte()

                    // Write packets: first is raw; each subsequent is prefixed with 0x00 delta header.
                    for (j in 0 until count) {
                        if (j > 0) {
                            buf[pos++] = 0  // PacketDeltaHeaderStruct: ChunkPack=0, BytePack=0
                        }
                        val pData = sizePackets[i + j].second
                        System.arraycopy(pData, 0, buf, pos, pData.size)
                        pos += pData.size
                    }

                    i += count
                }
            }

            if (pos > 0) {
                result.add(OutgoingDatagram(dest, buf.copyOf(pos)))
            }
        }

        return result
    }

    // C++: PacketManagerClass::Get_Packet / Break_Packet (receive side)
    // Splits a received combined datagram into individual packets.
    // offset: number of bytes to skip at the start (e.g. 4 for WRAPPER_CRC header).
    //
    // deltaFormat=true (default): wire format produced by combine() and used by the C++ client —
    //   the first packet in each group is the full base packet; each subsequent packet is preceded
    //   by a 1-byte PacketDeltaHeaderStruct (ChunkPack:1, BytePack:1) followed by delta or full data.
    // deltaFormat=false: simple non-delta format — every packet is a full packetSize-byte block with
    //   no delta headers (used only for single-packet groups or unit tests of isolated group parsing).
    fun split(data: ByteArray, length: Int, offset: Int = 0, deltaFormat: Boolean = true): List<IncomingPacket> {
        val result = mutableListOf<IncomingPacket>()
        var pos = offset

        while (pos < length) {
            if (pos + 2 > length) break  // can't read group header

            val headerLow = data[pos].toInt() and 0xFF
            val headerHigh = data[pos + 1].toInt() and 0xFF
            val header = headerLow or (headerHigh shl 8)
            pos += 2

            val numPackets = decodeNumPackets(header)
            val packetSize = decodePacketSize(header)
            val morePackets = decodeMorePackets(header)

            if (numPackets <= 0 || packetSize <= 0) break

            if (!deltaFormat) {
                // Non-delta: every packet is a full packetSize-byte block.
                repeat(numPackets) {
                    if (pos + packetSize <= length) {
                        val packetData = ByteArray(packetSize)
                        System.arraycopy(data, pos, packetData, 0, packetSize)
                        result.add(IncomingPacket(packetData, packetSize))
                        pos += packetSize
                    }
                }
            } else {
                // Delta format (C++ client): base packet + DeltaHeader-prefixed subsequent packets.
                if (pos + packetSize > length) break
                val base = ByteArray(packetSize)
                System.arraycopy(data, pos, base, 0, packetSize)
                result.add(IncomingPacket(base, packetSize))
                pos += packetSize

                for (i in 1 until numPackets) {
                    if (pos >= length) break
                    val deltaHeader = data[pos].toInt() and 0xFF
                    val chunkPack = deltaHeader and 1
                    val bytePack = (deltaHeader shr 1) and 1
                    if (chunkPack == 0 && bytePack == 0) {
                        // Non-delta secondary packet: skip 1-byte header, copy full packet.
                        pos += 1
                        if (pos + packetSize > length) break
                        val pkt = ByteArray(packetSize)
                        System.arraycopy(data, pos, pkt, 0, packetSize)
                        result.add(IncomingPacket(pkt, packetSize))
                        pos += packetSize
                    } else {
                        val (reconstructed, deltaSize) = reconstructFromDelta(base, packetSize, data, pos)
                        result.add(IncomingPacket(reconstructed, packetSize))
                        pos += deltaSize
                    }
                }
            }

            if (!morePackets) break
        }

        return result
    }

    // C++: PacketManagerClass::Reconstruct_From_Delta (packetmgr.cpp:428-543)
    // Rebuilds a packet from a delta stream. All packets in a group are encoded relative
    // to the first (base) packet. Returns the reconstructed packet and the total number of
    // bytes consumed from `data` starting at `deltaStart` (including the 1-byte header).
    //
    // Delta wire layout at deltaStart:
    //   [DeltaHeader: 1 byte]   ChunkPack:bit0, BytePack:bit1
    //   [bitfield bytes]        chunk bits (LSB-first), then byte bits
    //   [patch bytes]           new values for bytes that differ from base
    private fun reconstructFromDelta(
        base: ByteArray,
        packetSize: Int,
        data: ByteArray,
        deltaStart: Int,
    ): Pair<ByteArray, Int> {
        val header = data[deltaStart].toInt() and 0xFF
        val chunkPack = header and 1
        val bitfieldStart = deltaStart + 1   // first byte of bitfield data
        var readBitPos = 0                   // bit position for phase-1 chunk bits + phase-2 byte bits
        var readChunkPos = 0                 // bit position for re-reading chunk bits in phase 2

        val out = ByteArray(packetSize)
        val patchList = IntArray(packetSize)
        var numPatches = 0

        // Phase 1: if ChunkPack, copy matching 8-byte chunks from base into output.
        if (chunkPack != 0) {
            var i = 0
            while (i < packetSize - 7) {
                if (getBit(data, bitfieldStart, readBitPos++) != 0) {
                    System.arraycopy(base, i, out, i, 8)
                }
                i += 8
            }
            // For non-8-aligned packet sizes the encoder writes an extra tail 0-bit; consume it.
            if (packetSize and 7 != 0) readBitPos++
        }

        // Phase 2: byte-level pass. Re-reads chunk bits (readChunkPos) to skip matching chunks,
        // then reads byte bits (readBitPos) to decide copy-from-base vs record-patch-position.
        var i = 0
        while (i < packetSize) {
            if (chunkPack != 0) {
                if (getBit(data, bitfieldStart, readChunkPos++) != 0) {
                    i += 8; continue   // chunk matched in phase 1, already copied
                }
            }
            var j = i
            val end = minOf(i + 8, packetSize)
            while (j < end) {
                if (getBit(data, bitfieldStart, readBitPos++) != 0) {
                    out[j] = base[j]
                } else {
                    patchList[numPatches++] = j
                }
                j++
            }
            i += 8
        }

        // Patch bytes follow immediately after the bitfield (rounded up to byte boundary).
        val numBitfieldBytes = (readBitPos + 7) / 8
        var patchSrc = bitfieldStart + numBitfieldBytes
        for (p in 0 until numPatches) out[patchList[p]] = data[patchSrc++]

        val deltaSize = 1 + numBitfieldBytes + numPatches   // DeltaHeader + bitfield + patches
        return Pair(out, deltaSize)
    }

    // C++: static char Get_Bit(unsigned char *data, int &pos) in packetmgr.cpp
    // LSB-first: bit 0 of data[offset+0] is bitPos=0, bit 1 is bitPos=1, ..., bit 0 of data[offset+1] is bitPos=8.
    private fun getBit(data: ByteArray, offset: Int, bitPos: Int): Int =
        (data[offset + (bitPos shr 3)].toInt() ushr (bitPos and 7)) and 1

    // C++ PacketPackHeaderStruct bit layout (little-endian C bitfield, packed):
    //   bits [0..4]  = NumPackets (5 bits)
    //   bits [5..14] = PacketSize (10 bits)
    //   bit  [15]    = MorePackets (1 bit)

    private fun encodeGroupHeader(numPackets: Int, packetSize: Int, morePackets: Boolean): Int {
        require(numPackets in 1..PACKET_MANAGER_MAX_PACKETS)
        require(packetSize in 1..1023)
        val more = if (morePackets) 1 else 0
        return (numPackets and 0x1F) or ((packetSize and 0x3FF) shl 5) or (more shl 15)
    }

    private fun decodeNumPackets(header: Int): Int = header and 0x1F
    private fun decodePacketSize(header: Int): Int = (header shr 5) and 0x3FF
    private fun decodeMorePackets(header: Int): Boolean = (header shr 15) and 1 == 1
}
