package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.protocol.*
import ccr.server.NetClassIds
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Decodes captured C++ Renegade server UDP packets from a proxy log using our Kotlin
 * BitStream/encoder code. This serves as an integration test: if our decoding logic
 * matches the real C++ server's wire format, we can parse all packets without errors.
 *
 * Proxy log format (one entry):
 * ```
 * [    8357 ms]  CLIENT->SERVER  127.0.0.1:57003 -> 127.0.0.1:4848  39 bytes  type=0x86
 *   0000  86 21 F1 34 21 04 40 00 00 00 FF 00 D0 00 07 00   .!.4!.@.........
 *   0010  55 00 6E 00 6E 00 61 00 6D 00 65 00 64 00 00 4F   U.n.n.a.m.e.d..O
 *   0020  45 3B A3 00 01 F4 00                              E;.....
 * ```
 *
 * The proxy log is loaded from the classpath resource `/proxy_log.txt`
 * (placed in `server/src/test/resources/proxy_log.txt`). If the resource is absent,
 * all tests in this class skip gracefully.
 */
class ProxyDecoderTest {

    companion object {

        // Loaded once from classpath; null means the resource is absent and tests should skip.
        private val PROXY_LOG: List<String>? by lazy {
            val stream = ProxyDecoderTest::class.java.getResourceAsStream("/proxy_log.txt")
            stream?.bufferedReader()?.readLines()
        }

        // Map-specific encoder precision for C&C_Under
        // These must match the C++ server's encoder setup for correct decoding.
        @BeforeAll
        @JvmStatic
        fun setupEncoders() {
            // Packet header encoders (initialized by Packet companion init, but ensure they're set)
            EncoderRegistry.setPrecision(BITPACK_PACKET_TYPE, 4)
            EncoderRegistry.setPrecision(BITPACK_PACKET_ID, 28)

            // World position encoders for C&C_Under map (from LSD extents + 1.0 margin)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -578.52, 517.15, 0.2)  // 13 bits
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -255.34, 471.86, 0.2)  // 12 bits
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -67.00, 71.46, 0.2)    // 10 bits

            // Fixed-range encoders (same on all maps)
            EncoderRegistry.setPrecision(BITPACK_ONE_TIME_BOOLEAN_BITS, 23)
            EncoderRegistry.setPrecision(BITPACK_CONTINUOUS_BOOLEAN_BITS, 4)
            EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 15.0, 1.0)  // armorTypeCount=15
            EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE, 0.0, 12.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_SUB_STATE, 0.0, 511.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_CS, 8)
            EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_SC, 6)
        }
    }

    // ---- Data classes ----

    data class ProxyEntry(
        val timestampMs: Int,
        val direction: String,      // "CLIENT->SERVER" or "SERVER->CLIENT"
        val sourceAddr: String,
        val destAddr: String,
        val sizeBytes: Int,
        val rawBytes: ByteArray,
    )

    // ---- Log parsing ----

    private fun parseProxyLog(lines: List<String>): List<ProxyEntry> {
        val entries = mutableListOf<ProxyEntry>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            // Match header line: [    8357 ms]  CLIENT->SERVER  127.0.0.1:57003 -> 127.0.0.1:4848  39 bytes  type=0x86
            val headerRegex = Regex(
                """\[\s*(\d+)\s*ms]\s+(CLIENT->SERVER|SERVER->CLIENT)\s+(\S+)\s+->\s+(\S+)\s+(\d+)\s+bytes\s+type=0x[0-9A-Fa-f]+"""
            )
            val match = headerRegex.find(line)
            if (match != null) {
                val timestampMs = match.groupValues[1].toInt()
                val direction = match.groupValues[2]
                val sourceAddr = match.groupValues[3]
                val destAddr = match.groupValues[4]
                val sizeBytes = match.groupValues[5].toInt()

                // Read hex dump lines that follow
                val hexBytes = mutableListOf<Byte>()
                i++
                while (i < lines.size && lines[i].startsWith("  ")) {
                    val hexLine = lines[i]
                    // Format: "  0000  86 21 F1 34 21 04 40 ...   .!.4!.@..."
                    val hexPart = hexLine.substring(8, minOf(55, hexLine.length)).trim()
                    for (hexByte in hexPart.split(" ")) {
                        if (hexByte.length == 2) {
                            hexBytes.add(hexByte.toInt(16).toByte())
                        }
                    }
                    i++
                }

                entries.add(
                    ProxyEntry(
                        timestampMs = timestampMs,
                        direction = direction,
                        sourceAddr = sourceAddr,
                        destAddr = destAddr,
                        sizeBytes = sizeBytes,
                        rawBytes = hexBytes.toByteArray(),
                    )
                )
            } else {
                i++
            }
        }
        return entries
    }

    // ---- Main test ----

    @Test
    fun `decode proxy log session`() {
        val lines = PROXY_LOG
        if (lines == null) {
            println("SKIP: Proxy log not found in classpath (/proxy_log.txt).")
            println("Place a proxy_log.txt in server/src/test/resources/ to run this test.")
            return
        }

        val entries = parseProxyLog(lines)
        println("=== Proxy Log Decoder ===")
        println("Loaded ${entries.size} datagrams from proxy_log.txt (classpath)")
        println()

        var totalDatagrams = 0
        var totalPackets = 0
        var crcFailures = 0
        var parseErrors = 0
        var decodedGameObjects = 0

        for (entry in entries) {
            totalDatagrams++
            val dir = if (entry.direction == "CLIENT->SERVER") "C->S" else "S->C"
            val data = entry.rawBytes
            val length = data.size

            // Verify CRC
            if (!WrapperCrc.verify(data, length)) {
                crcFailures++
                println("[${"%8d".format(entry.timestampMs)} ms] $dir  ${entry.sizeBytes}B  CRC FAILED")
                continue
            }

            // The real C++ PacketManager uses delta encoding for BOTH directions.
            // Our Kotlin combine() doesn't use delta, but the C++ server's Take_Packet does.
            val packets = PacketCombiner.split(data, length, offset = 4, deltaFormat = true)
            if (packets.isEmpty()) {
                println("[${"%8d".format(entry.timestampMs)} ms] $dir  ${entry.sizeBytes}B  CRC OK, 0 packets (combiner failed)")
                continue
            }

            val packetSummaries = mutableListOf<String>()

            for (incoming in packets) {
                totalPackets++
                try {
                    val packet = Packet.parseWirePacket(incoming.data, incoming.length)
                    val decoded = PacketDecoder.decodePacket(packet, entry.direction)
                    packetSummaries.add("    $decoded")

                    if ((packet.type == PacketType.RELIABLE || packet.type == PacketType.UNRELIABLE) && packet.bitLength >= 41) {
                        decodedGameObjects++
                    }
                } catch (e: Exception) {
                    parseErrors++
                    packetSummaries.add("    PARSE ERROR: ${e.message}")
                }
            }

            println("[${"%8d".format(entry.timestampMs)} ms] $dir  ${entry.sizeBytes}B  CRC OK  ${packets.size} packet(s)")
            for (s in packetSummaries) {
                println(s)
            }
        }

        println()
        println("=== Summary ===")
        println("Datagrams:     $totalDatagrams")
        println("CRC failures:  $crcFailures")
        println("Total packets: $totalPackets")
        println("Game objects:  $decodedGameObjects decoded")
        println("Parse errors:  $parseErrors")

        // Assertion: all CRCs should verify (they were captured on the wire)
        assert(crcFailures == 0) { "$crcFailures datagram(s) had CRC failures — the proxy log may be corrupt" }

        // Soft check: at least some packets were decoded
        assert(totalPackets > 0) { "No packets were parsed from the proxy log" }

        println()
        println("All CRCs verified. $totalPackets packets decoded ($parseErrors errors).")
    }

    @Test
    fun `verify CRC on all proxy datagrams`() {
        val lines = PROXY_LOG
        if (lines == null) {
            println("SKIP: Proxy log not found")
            return
        }

        val entries = parseProxyLog(lines)
        var verified = 0
        for (entry in entries) {
            val ok = WrapperCrc.verify(entry.rawBytes, entry.rawBytes.size)
            assert(ok) {
                "CRC mismatch at ${entry.timestampMs}ms ${entry.direction}: " +
                    entry.rawBytes.take(8).joinToString(" ") { "%02x".format(it) }
            }
            verified++
        }
        println("Verified CRC on $verified datagrams.")
    }

    @Test
    fun `split all datagrams into packets`() {
        val lines = PROXY_LOG
        if (lines == null) {
            println("SKIP: Proxy log not found")
            return
        }

        val entries = parseProxyLog(lines)
        var totalPackets = 0
        var packetTypeCount = mutableMapOf<String, Int>()

        for (entry in entries) {
            if (!WrapperCrc.verify(entry.rawBytes, entry.rawBytes.size)) continue
            val packets = PacketCombiner.split(entry.rawBytes, entry.rawBytes.size, offset = 4, deltaFormat = true)
            for (incoming in packets) {
                totalPackets++
                try {
                    val packet = Packet.parseWirePacket(incoming.data, incoming.length)
                    packetTypeCount.merge(packet.type.name, 1) { a, b -> a + b }
                } catch (e: Exception) {
                    packetTypeCount.merge("ERROR", 1) { a, b -> a + b }
                }
            }
        }

        println("Total packets: $totalPackets")
        println("By type:")
        for ((type, count) in packetTypeCount.entries.sortedByDescending { it.value }) {
            println("  $type: $count")
        }
        assert(totalPackets > 0) { "No packets were split from the proxy log" }
    }

    @Test
    fun `decode all server RELIABLE game objects`() {
        val lines = PROXY_LOG
        if (lines == null) {
            println("SKIP: Proxy log not found")
            return
        }

        val entries = parseProxyLog(lines)
        var decoded = 0
        var errors = 0
        val classIdCounts = mutableMapOf<String, Int>()

        for (entry in entries) {
            if (entry.direction != "SERVER->CLIENT") continue
            if (!WrapperCrc.verify(entry.rawBytes, entry.rawBytes.size)) continue

            val packets = PacketCombiner.split(entry.rawBytes, entry.rawBytes.size, offset = 4, deltaFormat = true)
            for (incoming in packets) {
                val packet = try {
                    Packet.parseWirePacket(incoming.data, incoming.length)
                } catch (_: Exception) { continue }

                if (packet.type != PacketType.RELIABLE && packet.type != PacketType.UNRELIABLE) continue
                if (packet.bitLength < 41) continue

                try {
                    val bs = PacketDecoder.clonePayload(packet)
                    val networkId = bs.getInt()
                    val dirtyBits = bs.getByte().toInt() and 0xFF
                    val isDeletePending = bs.getBool()

                    if ((dirtyBits and 0x08) != 0) {
                        val classId = bs.getInt()
                        val key = "${NetClassIds.name(classId)}($classId)"
                        classIdCounts.merge(key, 1) { a, b -> a + b }
                    } else {
                        classIdCounts.merge("UPDATE(dirty=0x${dirtyBits.toString(16)})", 1) { a, b -> a + b }
                    }
                    decoded++
                } catch (e: Exception) {
                    errors++
                }
            }
        }

        println("Server game objects decoded: $decoded (errors: $errors)")
        println("Class ID distribution:")
        for ((key, count) in classIdCounts.entries.sortedByDescending { it.value }) {
            println("  $key: $count")
        }
    }

    @Test
    fun `decode connection handshake`() {
        val lines = PROXY_LOG
        if (lines == null) {
            println("SKIP: Proxy log not found")
            return
        }

        val entries = parseProxyLog(lines)
        println("=== Connection Handshake ===")

        for (entry in entries) {
            if (!WrapperCrc.verify(entry.rawBytes, entry.rawBytes.size)) continue
            val packets = PacketCombiner.split(entry.rawBytes, entry.rawBytes.size, offset = 4, deltaFormat = true)

            for (incoming in packets) {
                val packet = try {
                    Packet.parseWirePacket(incoming.data, incoming.length)
                } catch (_: Exception) { continue }

                when (packet.type) {
                    PacketType.CONNECT_CS -> {
                        println("[${entry.timestampMs}ms] CONNECT_CS from client")
                        try {
                            val bs = PacketDecoder.clonePayload(packet)
                            val nickname = bs.getWideString()
                            val password = bs.getWideString(permitEmpty = true)
                            val exeKey = bs.getInt()
                            val bbo = bs.getInt()
                            println("  nickname='$nickname' password='$password' exeKey=0x${Integer.toUnsignedString(exeKey, 16)} bbo=$bbo")
                        } catch (e: Exception) {
                            println("  decode error: ${e.message}")
                        }
                    }

                    PacketType.ACCEPT_SC -> {
                        println("[${entry.timestampMs}ms] ACCEPT_SC from server")
                        try {
                            val bs = PacketDecoder.clonePayload(packet)
                            val assignedId = bs.getInt()
                            println("  assignedId=$assignedId")
                        } catch (e: Exception) {
                            println("  decode error: ${e.message}")
                        }
                    }

                    PacketType.REFUSAL_SC -> {
                        println("[${entry.timestampMs}ms] REFUSAL_SC from server")
                        try {
                            val bs = PacketDecoder.clonePayload(packet)
                            val code = bs.getInt()
                            println("  refusalCode=$code (${RefusalCode.fromId(code)})")
                        } catch (e: Exception) {
                            println("  decode error: ${e.message}")
                        }
                    }

                    else -> { /* skip non-handshake packets */ }
                }
            }
        }
    }
}
