package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.protocol.*
import ccr.server.NetClassIds
import ccr.server.level.FullDefinitionLoader
import ccr.server.mix.MixReader
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Side-by-side decoder for C++ vs Kotlin server proxy logs.
 *
 * Reads .tmp/proxy_log_cpp.txt and .tmp/proxy_log_kotlin.txt, decodes all
 * SERVER→CLIENT packets from each, and prints them in a pair-wise comparison.
 * For post-BIOEVENT packets, also prints a byte-level hex diff.
 *
 * Capture instructions:
 *
 *   C++ server (port 4848) + proxy on 4849:
 *     gradlew :server:liveProxy -PlocalPort=4849 -PremoteHost=127.0.0.1 -PremotePort=4848 -PlogFile=.tmp/proxy_log_cpp.txt
 *
 *   Kotlin server (port 4849) + proxy on 4850:
 *     gradlew :server:liveProxy -PlocalPort=4850 -PremoteHost=127.0.0.1 -PremotePort=4849 -PlogFile=.tmp/proxy_log_kotlin.txt
 *
 * Skips gracefully when either log is absent.
 */
class ProxyComparisonTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_PACKET_TYPE, 4)
            EncoderRegistry.setPrecision(BITPACK_PACKET_ID, 28)

            // C&C_Under map extents
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -578.52, 517.15, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -255.34, 471.86, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -67.00, 71.46, 0.2)

            EncoderRegistry.setPrecision(BITPACK_ONE_TIME_BOOLEAN_BITS, 23)
            EncoderRegistry.setPrecision(BITPACK_CONTINUOUS_BOOLEAN_BITS, 4)
            EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 15.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE, 0.0, 12.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_SUB_STATE, 0.0, 511.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_CS, 8)
            EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_SC, 6)

            // Load definition registry so classId=1000 packets dispatch to the correct
            // decoder (vehicle vs soldier vs building) instead of always falling back to soldier.
            // Tests run with cwd = kotlin-server/server/; project root is two levels up.
            val dataCandidates = listOf("../../data", "../data", "data")
            val alwaysMix = dataCandidates.firstNotNullOfOrNull { dataPath ->
                listOf("always.dbs", "always2.dat", "always.dat").firstNotNullOfOrNull { fileName ->
                    val file = File(dataPath, fileName)
                    if (!file.exists()) return@firstNotNullOfOrNull null
                    try { MixReader(file.readBytes()) } catch (_: Exception) { null }
                }
            }
            val ddbData = alwaysMix?.readFile("objects.ddb")
            if (ddbData != null) {
                PacketDecoder.definitionRegistry = FullDefinitionLoader.load(ddbData)
            }
        }
    }

    // ---- Data classes ----

    data class ProxyEntry(
        val timestampMs: Int,
        val direction: String,
        val sizeBytes: Int,
        val rawBytes: ByteArray,
    )

    data class DecodedPacket(
        val reliableId: Int,      // -1 if unreliable or non-game packet
        val packetType: String,
        val payloadBits: Int,
        val networkClassId: Int,  // -1 if no creation
        val networkId: Int,       // -1 if unavailable
        val decoded: String,
        val rawBytes: ByteArray,  // the full datagram bytes this packet came from
        val phase: String,        // "handshake", "conn-objects", "post-bioevent"
    )

    // ---- Log parsing ----

    private fun loadLog(filename: String): List<String>? {
        // Tests run with cwd = kotlin-server/server/; project root is two levels up
        val candidates = listOf("../../.tmp/$filename", "../.tmp/$filename", ".tmp/$filename")
        val f = candidates.map { File(it) }.firstOrNull { it.exists() } ?: return null
        return f.readLines()
    }

    private fun parseProxyLog(lines: List<String>): List<ProxyEntry> {
        val entries = mutableListOf<ProxyEntry>()
        val headerRegex = Regex(
            """\[\s*(\d+)\s*ms]\s+(CLIENT->SERVER|SERVER->CLIENT)\s+\S+\s+->\s+\S+\s+(\d+)\s+bytes"""
        )
        var i = 0
        while (i < lines.size) {
            val match = headerRegex.find(lines[i])
            if (match != null) {
                val ts = match.groupValues[1].toInt()
                val dir = match.groupValues[2]
                val size = match.groupValues[3].toInt()
                val hexBytes = mutableListOf<Byte>()
                i++
                while (i < lines.size && lines[i].startsWith("  ")) {
                    val hexPart = lines[i].substring(8, minOf(55, lines[i].length)).trim()
                    for (b in hexPart.split(" ")) {
                        if (b.length == 2) hexBytes.add(b.toInt(16).toByte())
                    }
                    i++
                }
                entries.add(ProxyEntry(ts, dir, size, hexBytes.toByteArray()))
            } else {
                i++
            }
        }
        return entries
    }

    // ---- Phase detection + packet extraction ----

    /**
     * Extracts all SERVER→CLIENT decoded packets from the proxy entries, with phase tagging.
     *
     * Phase transitions:
     *   "handshake"    — before the first ACCEPT_SC
     *   "conn-objects" — after ACCEPT_SC, before the client sends BIOEVENT
     *   "post-bioevent"— after the client's BIOEVENT packet
     *
     * BIOEVENT detection: first C→S reliable with networkClassId=1026 (wire +1 offset from header 1025).
     */
    private fun extractDecodedPackets(entries: List<ProxyEntry>): List<DecodedPacket> {
        val result = mutableListOf<DecodedPacket>()
        var phase = "handshake"
        var acceptSeen = false

        for (entry in entries) {
            if (!WrapperCrc.verify(entry.rawBytes, entry.rawBytes.size)) continue

            val packets = PacketCombiner.split(entry.rawBytes, entry.rawBytes.size, offset = 4, deltaFormat = true)

            for (incoming in packets) {
                val packet = try {
                    Packet.parseWirePacket(incoming.data, incoming.length)
                } catch (_: Exception) { continue }

                // Phase transitions on S→C
                if (entry.direction == "SERVER->CLIENT" && packet.type == PacketType.ACCEPT_SC) {
                    acceptSeen = true
                    if (phase == "handshake") phase = "conn-objects"
                }

                // Detect BIOEVENT from C→S reliable packets (wire classId=1026 = header 1025+1)
                if (entry.direction == "CLIENT->SERVER" && acceptSeen && phase == "conn-objects" &&
                    (packet.type == PacketType.RELIABLE || packet.type == PacketType.UNRELIABLE) &&
                    packet.bitLength >= 41
                ) {
                    try {
                        val bs = PacketDecoder.clonePayload(packet)
                        bs.getInt()  // networkId
                        val dirty = bs.getByte().toInt() and 0xFF
                        bs.getBool() // isDeletePending
                        if ((dirty and 0x08) != 0) {
                            val classId = bs.getInt()
                            if (classId == 1026) { // BIOEVENT wire id
                                phase = "post-bioevent"
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Only decode S→C packets
                if (entry.direction != "SERVER->CLIENT") continue

                val currentPhase = phase

                val reliableId = if (packet.type == PacketType.RELIABLE) packet.id else -1
                var networkClassId = -1
                var networkId = -1

                if ((packet.type == PacketType.RELIABLE || packet.type == PacketType.UNRELIABLE) && packet.bitLength >= 41) {
                    try {
                        val bs = PacketDecoder.clonePayload(packet)
                        networkId = bs.getInt()
                        val dirty = bs.getByte().toInt() and 0xFF
                        bs.getBool()
                        if ((dirty and 0x08) != 0) {
                            networkClassId = bs.getInt()
                        }
                    } catch (_: Exception) {}
                }

                val decoded = try {
                    PacketDecoder.decodePacket(packet, "SERVER->CLIENT")
                } catch (e: Exception) {
                    "DECODE ERROR: ${e.message}"
                }

                result.add(
                    DecodedPacket(
                        reliableId = reliableId,
                        packetType = packet.type.name,
                        payloadBits = packet.bitLength,
                        networkClassId = networkClassId,
                        networkId = networkId,
                        decoded = decoded,
                        rawBytes = incoming.data.copyOf(incoming.length),
                        phase = currentPhase,
                    )
                )
            }
        }

        return result
    }

    // ---- Hex diff ----

    /** Prints a hex dump of both byte arrays, highlighting bytes that differ. */
    private fun printHexDiff(cppBytes: ByteArray, ktBytes: ByteArray) {
        val maxLen = maxOf(cppBytes.size, ktBytes.size)
        var anyDiff = false
        for (i in 0 until maxLen) {
            val c = if (i < cppBytes.size) cppBytes[i].toInt() and 0xFF else -1
            val k = if (i < ktBytes.size) ktBytes[i].toInt() and 0xFF else -1
            if (c != k) { anyDiff = true; break }
        }
        if (!anyDiff) {
            println("        [bytes identical]")
            return
        }

        // Print up to 128 bytes per row × 16
        val limit = minOf(maxLen, 256)
        var i = 0
        while (i < limit) {
            val end = minOf(i + 16, limit)
            val cppRow = (i until end).map { idx -> if (idx < cppBytes.size) cppBytes[idx].toInt() and 0xFF else -1 }
            val ktRow  = (i until end).map { idx -> if (idx < ktBytes.size)  ktBytes[idx].toInt()  and 0xFF else -1 }
            val rowDiff = cppRow.zip(ktRow).any { (a, b) -> a != b }

            if (rowDiff) {
                val cppHex = cppRow.joinToString(" ") { if (it >= 0) "%02x".format(it) else "--" }
                val ktHex  = ktRow.joinToString(" ")  { if (it >= 0) "%02x".format(it) else "--" }
                println("        %04x  C++: %-48s".format(i, cppHex))
                println("              KT:  %-48s".format(ktHex))
                // Show diff markers
                val markers = cppRow.zip(ktRow).joinToString(" ") { (a, b) -> if (a != b) "^^" else "  " }
                println("              dif: $markers")
            }
            i += 16
        }
        if (maxLen > 256) println("        ... (${maxLen - 256} more bytes not shown)")
    }

    // ---- Main comparison test ----

    @Test
    fun `compare cpp vs kotlin server traffic`() {
        val cppLines = loadLog("proxy_log_cpp.txt")
        val ktLines  = loadLog("proxy_log_kotlin.txt")

        if (cppLines == null || ktLines == null) {
            println("SKIP: Both .tmp/proxy_log_cpp.txt and .tmp/proxy_log_kotlin.txt must exist.")
            println()
            println("Capture C++ log:    Run C++ server on :4848, then:")
            println("  gradlew :server:liveProxy -PlocalPort=4849 -PremoteHost=127.0.0.1 -PremotePort=4848 -PlogFile=.tmp/proxy_log_cpp.txt")
            println()
            println("Capture Kotlin log: Run Kotlin server on :4849, then:")
            println("  gradlew :server:liveProxy -PlocalPort=4850 -PremoteHost=127.0.0.1 -PremotePort=4849 -PlogFile=.tmp/proxy_log_kotlin.txt")
            return
        }

        val cppEntries = parseProxyLog(cppLines)
        val ktEntries  = parseProxyLog(ktLines)
        println("Parsed ${cppEntries.size} datagrams from proxy_log_cpp.txt")
        println("Parsed ${ktEntries.size} datagrams from proxy_log_kotlin.txt")

        val cppPackets = extractDecodedPackets(cppEntries)
        val ktPackets  = extractDecodedPackets(ktEntries)

        val cppByPhase = cppPackets.groupBy { it.phase }
        val ktByPhase  = ktPackets.groupBy { it.phase }

        for (phase in listOf("handshake", "conn-objects", "post-bioevent")) {
            val cppPkts = cppByPhase[phase] ?: emptyList()
            val ktPkts  = ktByPhase[phase]  ?: emptyList()

            println()
            println("╔══════════════════════════════════════════════════════╗")
            println("║  Phase: ${phase.padEnd(44)}║")
            println("║  C++: ${cppPkts.size} packets    Kotlin: ${ktPkts.size} packets${" ".repeat((44 - "C++: ${cppPkts.size} packets    Kotlin: ${ktPkts.size} packets".length).coerceAtLeast(0))}║")
            println("╚══════════════════════════════════════════════════════╝")

            val maxPkts = maxOf(cppPkts.size, ktPkts.size)
            for (i in 0 until maxPkts) {
                val cpp = cppPkts.getOrNull(i)
                val kt  = ktPkts.getOrNull(i)

                println()
                println("  ── Packet #$i ──")

                if (cpp != null) {
                    println("  C++  [${cpp.packetType} id=${if (cpp.reliableId >= 0) cpp.reliableId else "unrel"} ${cpp.payloadBits}b]")
                    for (line in cpp.decoded.lines()) println("    $line")
                } else {
                    println("  C++  (no packet at this position)")
                }

                if (kt != null) {
                    println("  KT   [${kt.packetType} id=${if (kt.reliableId >= 0) kt.reliableId else "unrel"} ${kt.payloadBits}b]")
                    for (line in kt.decoded.lines()) println("    $line")
                } else {
                    println("  KT   (no packet at this position)")
                }

                if (cpp != null && kt != null && phase == "post-bioevent") {
                    println("  Hex diff:")
                    printHexDiff(cpp.rawBytes, kt.rawBytes)
                }
            }
        }

        println()
        println("=== Summary ===")
        println("C++    S→C packets: ${cppPackets.size}  (handshake=${cppByPhase["handshake"]?.size ?: 0}, conn=${cppByPhase["conn-objects"]?.size ?: 0}, post-bio=${cppByPhase["post-bioevent"]?.size ?: 0})")
        println("Kotlin S→C packets: ${ktPackets.size}  (handshake=${ktByPhase["handshake"]?.size ?: 0}, conn=${ktByPhase["conn-objects"]?.size ?: 0}, post-bio=${ktByPhase["post-bioevent"]?.size ?: 0})")
    }

    /** Secondary test: dump all S→C packets from the C++ log for reference. */
    @Test
    fun `dump cpp server traffic`() {
        val lines = loadLog("proxy_log_cpp.txt") ?: run {
            println("SKIP: .tmp/proxy_log_cpp.txt not found")
            return
        }
        val entries = parseProxyLog(lines)
        val packets = extractDecodedPackets(entries)
        println("=== C++ Server S→C Packets (${packets.size} total) ===")
        for ((i, pkt) in packets.withIndex()) {
            println("\n[#$i  ${pkt.phase}  ${pkt.packetType} id=${pkt.reliableId}  ${pkt.payloadBits}b]")
            for (line in pkt.decoded.lines()) println("  $line")
            // Raw hex
            val hex = pkt.rawBytes.take(64).joinToString(" ") { "%02x".format(it) }
            println("  hex: $hex${if (pkt.rawBytes.size > 64) "..." else ""}")
        }
    }

    /** Secondary test: dump all S→C packets from the Kotlin log for reference. */
    @Test
    fun `dump kotlin server traffic`() {
        val lines = loadLog("proxy_log_kotlin.txt") ?: run {
            println("SKIP: .tmp/proxy_log_kotlin.txt not found")
            return
        }
        val entries = parseProxyLog(lines)
        val packets = extractDecodedPackets(entries)
        println("=== Kotlin Server S→C Packets (${packets.size} total) ===")
        for ((i, pkt) in packets.withIndex()) {
            println("\n[#$i  ${pkt.phase}  ${pkt.packetType} id=${pkt.reliableId}  ${pkt.payloadBits}b]")
            for (line in pkt.decoded.lines()) println("  $line")
            val hex = pkt.rawBytes.take(64).joinToString(" ") { "%02x".format(it) }
            println("  hex: $hex${if (pkt.rawBytes.size > 64) "..." else ""}")
        }
    }

    /** Analyzes the event lifecycle in the Kotlin server log — detects S→C events missing DELETE_PENDING. */
    @Test
    fun `analyze kotlin event lifecycle`() {
        val lines = loadLog("proxy_log_kotlin.txt") ?: run {
            println("SKIP: .tmp/proxy_log_kotlin.txt not found")
            return
        }

        val entries = parseProxyLog(lines)
        val scPackets = extractDecodedPackets(entries)

        // One-shot S→C event classIds that must have DELETE_PENDING (exclude 1000=GAMEOBJ, 1010=TEAM, 1011=PLAYER)
        val oneShortEventClassIds = (1001..1016).toSet() - setOf(1010, 1011)

        // --- Track lifecycle from S→C packets ---
        // networkId -> (classId, creationSeen, deletePendingSeen)
        data class LifecycleEntry(
            val networkId: Int,
            val classId: Int,
            val direction: String,
            var creationSeen: Boolean,
            var deletePendingSeen: Boolean,
        )

        val scLifecycle = mutableMapOf<Int, LifecycleEntry>()

        for (pkt in scPackets) {
            if (pkt.networkId == -1) continue
            if (pkt.packetType != PacketType.RELIABLE.name && pkt.packetType != PacketType.UNRELIABLE.name) continue

            val existing = scLifecycle[pkt.networkId]
            val classId = if (pkt.networkClassId != -1) pkt.networkClassId else existing?.classId ?: -1
            val creationSeen = pkt.networkClassId != -1 || (existing?.creationSeen ?: false)

            // Parse isDeletePending directly from the raw payload
            val isDeletePending = try {
                val bs = PacketDecoder.clonePayload(Packet.parseWirePacket(pkt.rawBytes, pkt.rawBytes.size))
                bs.getInt()  // networkId
                bs.getByte() // dirty
                bs.getBool() // isDeletePending
            } catch (_: Exception) { false }

            val deletePendingSeen = isDeletePending || (existing?.deletePendingSeen ?: false)

            scLifecycle[pkt.networkId] = LifecycleEntry(
                networkId = pkt.networkId,
                classId = classId,
                direction = "S->C",
                creationSeen = creationSeen,
                deletePendingSeen = deletePendingSeen,
            )
        }

        // --- Track C→S networkIds from raw entries ---
        val csNetworkIds = mutableMapOf<Int, LifecycleEntry>()

        for (entry in entries) {
            if (entry.direction != "CLIENT->SERVER") continue
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
                    val dirty = bs.getByte().toInt() and 0xFF
                    bs.getBool() // isDeletePending (C->S doesn't use it the same way, but consume the bit)
                    val classId = if ((dirty and 0x08) != 0) bs.getInt() else -1

                    // Check if the server sent DELETE_PENDING for this networkId (from S->C lifecycle)
                    val serverDeletePending = scLifecycle[networkId]?.deletePendingSeen ?: false

                    val existing = csNetworkIds[networkId]
                    csNetworkIds[networkId] = LifecycleEntry(
                        networkId = networkId,
                        classId = if (classId != -1) classId else existing?.classId ?: -1,
                        direction = "C->S",
                        creationSeen = (classId != -1) || (existing?.creationSeen ?: false),
                        deletePendingSeen = serverDeletePending || (existing?.deletePendingSeen ?: false),
                    )
                } catch (_: Exception) {}
            }
        }

        // Merge C->S entries (only those not already in S->C lifecycle)
        val allLifecycle = mutableMapOf<Int, LifecycleEntry>()
        allLifecycle.putAll(scLifecycle)
        for ((netId, csEntry) in csNetworkIds) {
            if (netId !in allLifecycle) {
                allLifecycle[netId] = csEntry
            }
        }

        // --- Print lifecycle summary table ---
        println()
        println("=== Event Lifecycle Analysis ===")
        for (entry in allLifecycle.values.sortedBy { it.networkId }) {
            val classLabel = if (entry.classId != -1) "${entry.classId}(${NetClassIds.name(entry.classId)})" else "UNKNOWN"
            val status = when {
                entry.direction == "S->C" && entry.classId in oneShortEventClassIds && entry.creationSeen && !entry.deletePendingSeen -> "MISSING_DELETE"
                entry.classId == 1000 || entry.classId == 1010 || entry.classId == 1011 -> "PERSISTENT"
                entry.classId !in oneShortEventClassIds && entry.direction == "S->C" -> "PERSISTENT"
                else -> "OK"
            }
            println(
                "netId=%-12d  class=%-35s  dir=%-5s  created=%-5s  deletePending=%-5s  %s".format(
                    entry.networkId,
                    classLabel,
                    entry.direction,
                    entry.creationSeen,
                    entry.deletePendingSeen,
                    status,
                )
            )
        }

        // --- Print first 50 S→C reliable post-bioevent packets ---
        val postBioReliable = scPackets.filter { it.phase == "post-bioevent" && it.packetType == PacketType.RELIABLE.name }
        println()
        println("=== First 50 S→C packets post-BIOEVENT ===")
        for ((i, pkt) in postBioReliable.take(50).withIndex()) {
            val classLabel = if (pkt.networkClassId != -1) "${pkt.networkClassId}(${NetClassIds.name(pkt.networkClassId)})" else ""
            val deleteFlag = try {
                val bs = PacketDecoder.clonePayload(Packet.parseWirePacket(pkt.rawBytes, pkt.rawBytes.size))
                bs.getInt()  // networkId
                bs.getByte() // dirty
                if (bs.getBool()) " DELETE_PENDING" else ""
            } catch (_: Exception) { "" }
            val classIdStr = if (classLabel.isNotEmpty()) " classId=$classLabel" else ""
            val dirtyHex = try {
                val bs = PacketDecoder.clonePayload(Packet.parseWirePacket(pkt.rawBytes, pkt.rawBytes.size))
                bs.getInt()  // networkId
                "0x%02X".format(bs.getByte().toInt() and 0xFF)
            } catch (_: Exception) { "?" }
            println("[#$i] RELIABLE id=${pkt.reliableId}  netId=${pkt.networkId} dirty=$dirtyHex$classIdStr$deleteFlag")
        }

        // --- Print freeze suspects ---
        val freezeSuspects = allLifecycle.values.filter { entry ->
            entry.direction == "S->C" &&
                entry.classId in oneShortEventClassIds &&
                entry.creationSeen &&
                !entry.deletePendingSeen
        }.sortedBy { it.networkId }

        println()
        println("=== FREEZE SUSPECTS (S->C events missing DELETE_PENDING) ===")
        if (freezeSuspects.isEmpty()) {
            println("None found — all S->C events have DELETE_PENDING")
        } else {
            for (entry in freezeSuspects) {
                println("netId=${entry.networkId} classId=${entry.classId}(${NetClassIds.name(entry.classId)}) — likely freeze culprit")
            }
        }
    }
}
