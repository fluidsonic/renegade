package ccr.server

import ccr.net.bitstream.BitStream
import ccr.net.protocol.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Minimal Renegade network client for packet-capture diagnostics.
 *
 * Connects to a C++ or Kotlin Renegade server, logs every datagram in hex,
 * and sends proper ACKs so the server progresses through the full handshake.
 *
 * Usage: gradlew :server:miniClient -PserverHost=<ip> -PserverPort=<port>
 *        Defaults: localhost:4848
 */
fun main(args: Array<String>) {
    val host = System.getProperty("serverHost") ?: args.getOrNull(0) ?: "127.0.0.1"
    val port = (System.getProperty("serverPort") ?: args.getOrNull(1) ?: "4848").toInt()
    val exeKey = (System.getProperty("exeKey") ?: "0").let {
        if (it.startsWith("0x", ignoreCase = true)) java.lang.Long.decode(it).toInt() else it.toInt()
    }
    val serverAddr = InetSocketAddress(host, port)
    println("exeKey = 0x${Integer.toUnsignedString(exeKey, 16)} ($exeKey)")

    println("=== MiniClient connecting to $serverAddr ===")
    println()

    val socket = DatagramSocket()
    socket.soTimeout = 5000  // 5s receive timeout

    var localId = -1          // assigned by ACCEPT_SC
    var reliableRcvId = 0     // expected next reliable receive ID
    var reliableSendId = 0    // our next reliable send ID
    var packetCount = 0

    // ── Send CONNECT_CS ──────────────────────────────────────────────────
    val connectPacket = Packet().apply {
        type = PacketType.CONNECT_CS
        id = 0
        senderId = 0
        // Payload: nickname + password + exeKey + bbo
        // C++: cnetwork.cpp:213-216
        payload.addWideString("MiniClient")
        payload.addWideString("", permitEmpty = true)  // password (empty)
        payload.addInt(exeKey)                             // exeKey (configurable via -PexeKey)
        // bbo is read by wwnet AFTER the app handler returns
        payload.addInt(10000)                           // bbo (bandwidth budget override)
    }
    sendPacket(socket, serverAddr, connectPacket, "CONNECT_CS")

    // ── Receive loop ─────────────────────────────────────────────────────
    val buf = ByteArray(2048)
    var running = true
    var bioEventSent = false
    var gameObjectCount = 0

    while (running) {
        val dp = DatagramPacket(buf, buf.size)
        try {
            socket.receive(dp)
        } catch (_: java.net.SocketTimeoutException) {
            println("[TIMEOUT] no data for 5s")
            if (bioEventSent) {
                println("[DONE] received all post-BIOEVENT packets, exiting")
                running = false
            }
            continue
        }

        val rawData = buf.copyOf(dp.length)
        val source = dp.socketAddress as InetSocketAddress

        println("╔══ DATAGRAM #${++packetCount} from $source (${dp.length} bytes) ══")
        printHex(rawData, prefix = "║ RAW ")

        // Verify CRC
        if (!WrapperCrc.verify(rawData, rawData.size)) {
            println("║ !! CRC MISMATCH — discarding")
            println("╚══════════════════════════════════════════")
            println()
            continue
        }
        println("║ CRC OK")

        // Split combined datagram (skip 4-byte CRC prefix).
        // Try delta format first — C++ server uses delta encoding for combined datagrams.
        var packets = PacketCombiner.split(rawData, rawData.size, offset = 4, deltaFormat = true)
        if (packets.isEmpty()) {
            packets = PacketCombiner.split(rawData, rawData.size, offset = 4, deltaFormat = false)
        }
        if (packets.isEmpty()) {
            println("║ !! split → 0 packets")
            println("╚══════════════════════════════════════════")
            println()
            continue
        }
        processPackets(packets, socket, serverAddr, { localId }, { localId = it }, { reliableRcvId }, { reliableRcvId++ })

        // Count game objects for BIOEVENT trigger
        for (incoming in packets) {
            try {
                val pkt = Packet.parseWirePacket(incoming.data, incoming.length)
                if (pkt.type == PacketType.RELIABLE || pkt.type == PacketType.ACCEPT_SC) {
                    gameObjectCount++
                }
            } catch (_: Exception) {}
        }

        println("╚══════════════════════════════════════════")
        println()

        // After receiving enough connection objects (ACCEPT + 4 game objects), send client events then BIOEVENT
        if (!bioEventSent && localId >= 0 && gameObjectCount >= 4) {
            println()
            // Match real client: CLIENTCONTROL → CLIENTFPS → BIOEVENT
            println("════ Sending CLIENTCONTROL (reliable id=$reliableSendId) ════")
            sendClientControl(socket, serverAddr, localId, reliableSendId++)
            println("════ Sending CLIENTFPS (reliable id=$reliableSendId) ════")
            sendClientFps(socket, serverAddr, localId, reliableSendId++)
            println("════ Sending BIOEVENT (reliable id=$reliableSendId) ════")
            sendBioEvent(socket, serverAddr, localId, reliableSendId++)
            bioEventSent = true
            // Extend timeout to capture response packets
            socket.soTimeout = 8000
        }
    }

    socket.close()
    println()
    println("=== MiniClient done ===")
}

private fun processPackets(
    packets: List<IncomingPacket>,
    socket: DatagramSocket,
    serverAddr: InetSocketAddress,
    getLocalId: () -> Int,
    setLocalId: (Int) -> Unit,
    getExpectedRcvId: () -> Int,
    advanceRcvId: () -> Unit,
) {
    for ((idx, incoming) in packets.withIndex()) {
        println("║ ── packet[$idx] ${incoming.length} bytes ──")
        printHex(incoming.data.copyOf(incoming.length), prefix = "║   ")

        val pkt = try {
            Packet.parseWirePacket(incoming.data, incoming.length)
        } catch (e: Exception) {
            println("║   !! parse failed: $e")
            continue
        }

        println("║   type=${pkt.type} id=${pkt.id} senderId=${pkt.senderId} bitLength=${pkt.bitLength}")

        when (pkt.type) {
            PacketType.ACCEPT_SC -> {
                val rhostId = pkt.payload.getInt()
                setLocalId(rhostId)
                advanceRcvId()  // ACCEPT_SC occupies reliable slot 0
                println("║   → ACCEPT_SC: assigned localId=$rhostId")
                println("║   → sending ACK id=${pkt.id}")
                sendAck(socket, serverAddr, pkt.id, rhostId)
            }

            PacketType.RELIABLE -> {
                println("║   → RELIABLE id=${pkt.id} (expected=${getExpectedRcvId()})")
                advanceRcvId()
                describePayload(pkt)
                val myId = getLocalId()
                if (myId >= 0) {
                    println("║   → sending ACK id=${pkt.id} senderId=$myId")
                    sendAck(socket, serverAddr, pkt.id, myId)
                } else {
                    println("║   → SKIPPING ACK (localId not yet assigned)")
                }
            }

            PacketType.KEEPALIVE -> {
                val packetLoss = pkt.payload.getFloat()
                val serviceRate = pkt.payload.getInt()
                println("║   → KEEPALIVE: packetLoss=$packetLoss serviceRate=$serviceRate")
                advanceRcvId()
                val myId = getLocalId()
                if (myId >= 0) {
                    println("║   → sending ACK id=${pkt.id} senderId=$myId")
                    sendAck(socket, serverAddr, pkt.id, myId)
                }
            }

            PacketType.ACK -> {
                println("║   → ACK for our packet id=${pkt.id}")
            }

            PacketType.REFUSAL_SC -> {
                val code = pkt.payload.getInt()
                println("║   → REFUSAL_SC: code=$code")
                println("║   !! Connection refused")
            }

            else -> {
                println("║   → (unhandled type)")
            }
        }
    }
}

private fun describePayload(pkt: Packet) {
    if (pkt.bitLength < 41) return
    try {
        val bs = BitStream()
        val payloadBytes = (pkt.bitLength + 7) / 8
        System.arraycopy(pkt.payload.buffer, 0, bs.buffer, 0, payloadBytes)
        bs.setBitWritePosition(pkt.bitLength)

        val networkId = bs.getInt()
        val dirtyBits = bs.getByte().toInt() and 0xFF
        val isDeletePending = bs.getBool()

        val sb = StringBuilder("║   payload: netId=$networkId dirty=0x${dirtyBits.toString(16).uppercase()}")
        if (isDeletePending) sb.append(" DELETE")

        if ((dirtyBits and 0x08) != 0) {
            val classId = bs.getInt()
            sb.append(" classId=$classId(${NetClassIds.name(classId)})")
        }
        println(sb.toString())
    } catch (e: Exception) {
        println("║   payload: (describe failed: $e)")
    }
}

private fun sendPacket(socket: DatagramSocket, dest: InetSocketAddress, packet: Packet, label: String) {
    val wireData = Packet.buildWirePacket(packet)
    val combined = PacketCombiner.combine(listOf(dest to wireData))
    for (dg in combined) {
        val withCrc = WrapperCrc.prepend(dg.data)
        println(">>> SEND $label (${withCrc.size} bytes)")
        printHex(withCrc, prefix = "    ")
        socket.send(DatagramPacket(withCrc, withCrc.size, dest))
    }
}

private fun sendAck(socket: DatagramSocket, dest: InetSocketAddress, packetId: Int, myLocalId: Int) {
    val ack = Packet().apply {
        type = PacketType.ACK
        id = packetId
        senderId = myLocalId
    }
    val wireData = Packet.buildWirePacket(ack)
    val combined = PacketCombiner.combine(listOf(dest to wireData))
    for (dg in combined) {
        val withCrc = WrapperCrc.prepend(dg.data)
        socket.send(DatagramPacket(withCrc, withCrc.size, dest))
    }
}

private fun sendClientControl(socket: DatagramSocket, dest: InetSocketAddress, localId: Int, reliableSendId: Int) {
    // C++: cClientControl — classId=1018
    // Export_Creation: ClientId (int)
    // Export_Frequent: SmartObjId (int) — -1 means no soldier yet
    // Total: 73 (header) + 32 + 32 = 137 bits (matches real client)
    val netIdBase = 2110000000
    val pkt = Packet().apply {
        type = PacketType.RELIABLE
        id = reliableSendId
        senderId = localId
        payload.addInt(netIdBase + 1)          // networkId
        payload.addByte(0x0F.toByte())         // dirtyBits = BIT_CREATION
        payload.addBool(false)                 // isDeletePending
        payload.addInt(1018)                   // classId = CLIENTCONTROL
        payload.addInt(localId)                // ClientId (Export_Creation)
        payload.addInt(-1)                     // SmartObjId = -1 (Export_Frequent, no soldier)
    }
    sendPacket(socket, dest, pkt, "CLIENTCONTROL (reliable id=$reliableSendId)")
}

private fun sendClientFps(socket: DatagramSocket, dest: InetSocketAddress, localId: Int, reliableSendId: Int) {
    // C++: cClientFps — classId=1032
    // Export_Creation: ClientId (int)
    // Export_Frequent: Fps (BYTE)
    // Total: 73 (header) + 32 + 8 = 113 bits (matches real client)
    val netIdBase = 2110000000
    val pkt = Packet().apply {
        type = PacketType.RELIABLE
        id = reliableSendId
        senderId = localId
        payload.addInt(netIdBase + 2)          // networkId
        payload.addByte(0x0F.toByte())         // dirtyBits = BIT_CREATION
        payload.addBool(false)                 // isDeletePending
        payload.addInt(1032)                   // classId = CLIENTFPS
        payload.addInt(localId)                // ClientId (Export_Creation)
        payload.addByte(60.toByte())           // Fps = 60 (Export_Frequent)
    }
    sendPacket(socket, dest, pkt, "CLIENTFPS (reliable id=$reliableSendId)")
}

private fun sendBioEvent(socket: DatagramSocket, dest: InetSocketAddress, localId: Int, reliableSendId: Int) {
    // C++: cBioEvent is a cNetEvent with classId=1026
    // Export_Creation writes: SenderId, Nickname, TeamChoice, ClanID, MapName
    val pkt = Packet().apply {
        type = PacketType.RELIABLE
        id = reliableSendId
        senderId = localId
        // Network object envelope
        payload.addInt(2110000003)           // networkId (matching real client scheme)
        payload.addByte(0x0F.toByte())     // dirtyBits = BIT_CREATION
        payload.addBool(false)             // isDeletePending
        payload.addInt(1026)               // classId = NETCLASSID_BIOEVENT
        // cBioEvent::Export_Creation fields:
        payload.addInt(localId)            // SenderId
        payload.addWideString("MiniClient") // Nickname
        payload.addInt(0)                  // TeamChoice (0=NOD)
        payload.addInt(0)                  // ClanID
        payload.addTerminatedString("", permitEmpty = true)  // MapName (terminated string, not wide)
        // cNetEvent subclasses have no RARE/OCCASIONAL/FREQUENT
    }
    sendPacket(socket, dest, pkt, "BIOEVENT (reliable id=$reliableSendId)")
}

private fun printHex(data: ByteArray, prefix: String = "") {
    for (i in data.indices step 16) {
        val end = minOf(i + 16, data.size)
        val hex = (i until end).joinToString(" ") { "%02x".format(data[it]) }
        val ascii = (i until end).map { if (data[it] in 32..126) data[it].toInt().toChar() else '.' }.joinToString("")
        println("$prefix%04x: %-48s  %s".format(i, hex, ascii))
    }
}
