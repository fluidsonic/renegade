package ccr.server

import ccr.net.bitstream.*
import ccr.net.protocol.*
import ccr.server.level.FullDefinitionLoader
import ccr.server.mix.MixReader
import ccr.server.net.PacketDecoder
import java.io.File
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Live UDP proxy with real-time packet decoding between a Renegade client and C++ server.
 *
 * Sits between the game client and server, forwarding all datagrams transparently while
 * decoding and logging packet contents using [PacketDecoder].
 *
 * Usage: gradlew :server:liveProxy -PlocalPort=4848 -PremoteHost=127.0.0.1 -PremotePort=4849
 *        Optional: -PlogFile=.tmp/proxy_log.txt  (writes raw proxy log compatible with ProxyDecoderTest)
 */
fun main() {
    val localPort = System.getProperty("localPort")?.toIntOrNull() ?: 4848
    val remoteHost = System.getProperty("remoteHost") ?: "127.0.0.1"
    val remotePort = System.getProperty("remotePort")?.toIntOrNull() ?: 4849
    val logFilePath = System.getProperty("logFile")

    setupEncoders()
    loadDefinitionRegistry()

    val remoteAddr = InetSocketAddress(remoteHost, remotePort)
    val proxySocket = DatagramSocket(localPort)
    proxySocket.soTimeout = 100 // 100ms non-blocking poll

    val running = AtomicBoolean(true)
    val startTime = System.currentTimeMillis()
    val clientDatagrams = AtomicLong(0)
    val serverDatagrams = AtomicLong(0)
    val totalPacketsDecoded = AtomicLong(0)

    // Optional raw log file (compatible with ProxyDecoderTest format)
    val logWriter: PrintWriter? = logFilePath?.let {
        val file = File(it)
        file.parentFile?.mkdirs()
        PrintWriter(file.bufferedWriter())
    }

    // Shutdown hook for Ctrl+C / SIGINT
    Runtime.getRuntime().addShutdownHook(Thread {
        running.set(false)
        println()
        println("=== LiveProxy Shutdown ===")
        println("Client→Server datagrams: ${clientDatagrams.get()}")
        println("Server→Client datagrams: ${serverDatagrams.get()}")
        println("Total packets decoded:   ${totalPacketsDecoded.get()}")
        println("Uptime: ${System.currentTimeMillis() - startTime} ms")
        logWriter?.flush()
        logWriter?.close()
    })

    println("=== LiveProxy ===")
    println("Listening on :$localPort → forwarding to $remoteHost:$remotePort")
    if (logFilePath != null) println("Raw log: $logFilePath")
    println("Press Ctrl+C to stop.")
    println()

    // Track the client address (learned from the first packet we receive)
    var clientAddr: InetSocketAddress? = null

    val buf = ByteArray(4096)

    while (running.get()) {
        val dp = DatagramPacket(buf, buf.size)
        try {
            proxySocket.receive(dp)
        } catch (_: java.net.SocketTimeoutException) {
            continue
        }

        val rawData = buf.copyOf(dp.length)
        val source = dp.socketAddress as InetSocketAddress
        val elapsedMs = System.currentTimeMillis() - startTime

        val isFromClient: Boolean
        if (source == remoteAddr || (source.address == remoteAddr.address && source.port == remoteAddr.port)) {
            // Packet from the server → forward to client
            isFromClient = false
            serverDatagrams.incrementAndGet()
            if (clientAddr != null) {
                proxySocket.send(DatagramPacket(rawData, rawData.size, clientAddr))
            }
        } else {
            // Packet from a client → remember client address, forward to server
            isFromClient = true
            clientAddr = source
            clientDatagrams.incrementAndGet()
            proxySocket.send(DatagramPacket(rawData, rawData.size, remoteAddr))
        }

        val direction = if (isFromClient) "C\u2192S" else "S\u2192C"
        val directionLog = if (isFromClient) "CLIENT->SERVER" else "SERVER->CLIENT"

        // Write raw log entry (ProxyDecoderTest-compatible format)
        if (logWriter != null) {
            val srcStr = "${source.address.hostAddress}:${source.port}"
            val dstStr = if (isFromClient) {
                "${remoteAddr.address.hostAddress}:${remoteAddr.port}"
            } else {
                clientAddr?.let { "${it.address.hostAddress}:${it.port}" } ?: "unknown"
            }
            val firstByte = if (rawData.isNotEmpty()) rawData[0].toInt() and 0xFF else 0
            logWriter.println("[${"%8d".format(elapsedMs)} ms]  $directionLog  $srcStr -> $dstStr  ${rawData.size} bytes  type=0x${"%02x".format(firstByte)}")
            for (i in rawData.indices step 16) {
                val end = minOf(i + 16, rawData.size)
                val hex = (i until end).joinToString(" ") { "%02x".format(rawData[it]) }
                val ascii = (i until end).map { if (rawData[it] in 32..126) rawData[it].toInt().toChar() else '.' }.joinToString("")
                logWriter.println("  %04x  %-48s  %s".format(i, hex, ascii))
            }
            logWriter.flush()
        }

        // Decode and print
        if (!WrapperCrc.verify(rawData, rawData.size)) {
            println("[${"%7d".format(elapsedMs)} ms] $direction  ${rawData.size}B  CRC FAILED")
            continue
        }

        val packets = PacketCombiner.split(rawData, rawData.size, offset = 4, deltaFormat = true)
        if (packets.isEmpty()) {
            println("[${"%7d".format(elapsedMs)} ms] $direction  ${rawData.size}B  0 pkt(s)")
            continue
        }

        totalPacketsDecoded.addAndGet(packets.size.toLong())

        println("[${"%7d".format(elapsedMs)} ms] $direction  ${rawData.size}B  ${packets.size} pkt(s)")

        for (incoming in packets) {
            try {
                val packet = Packet.parseWirePacket(incoming.data, incoming.length)
                val decoded = PacketDecoder.decodePacket(packet, directionLog)
                println("    $decoded")
            } catch (e: Exception) {
                println("    PARSE ERROR: ${e.message}")
            }
        }
    }

    proxySocket.close()
}

/**
 * Loads the definition registry from always.dbs (or always2.dat / always.dat) and
 * assigns it to [PacketDecoder.definitionRegistry] so classId=1000 creation packets
 * are dispatched to the correct decoder (vehicle vs soldier vs building).
 *
 * Data path: resolved from system property "dataPath" (default: "data").
 */
private fun loadDefinitionRegistry() {
    val dataPath = System.getProperty("dataPath") ?: "data"
    val dataDir = File(dataPath)

    val alwaysMix = listOf("always.dbs", "always2.dat", "always.dat").firstNotNullOfOrNull { fileName ->
        val file = File(dataDir, fileName)
        if (!file.exists()) return@firstNotNullOfOrNull null
        try {
            MixReader(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    if (alwaysMix == null) {
        println("[LiveProxy] WARNING: no always MIX found under ${dataDir.absolutePath} — classId=1000 packets will fall back to soldier decoder")
        return
    }

    val ddbData = alwaysMix.readFile("objects.ddb")
    if (ddbData == null) {
        println("[LiveProxy] WARNING: objects.ddb not found in always MIX — classId=1000 packets will fall back to soldier decoder")
        return
    }

    val registry = FullDefinitionLoader.load(ddbData)
    PacketDecoder.definitionRegistry = registry
    println("[LiveProxy] loaded ${registry.size} definitions — vehicle/soldier/building dispatch enabled")
}

/**
 * Configures BitStream encoder precision for C&C_Under map extents.
 * Must be called before any packet decoding.
 */
private fun setupEncoders() {
    // Packet header encoders
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
