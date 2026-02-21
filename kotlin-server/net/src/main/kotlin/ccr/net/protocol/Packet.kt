package ccr.net.protocol

import ccr.net.bitstream.BITPACK_PACKET_ID
import ccr.net.bitstream.BITPACK_PACKET_TYPE
import ccr.net.bitstream.BitStream
import ccr.net.bitstream.EncoderRegistry
import ccr.net.bitstream.MAX_BUFFER_SIZE

// Bit widths for packet header fields — C++: PACKET_ID_BITS / PACKET_TYPE_BITS in wwpacket.cpp
private const val PACKET_TYPE_BITS = 4   // supports up to 16 packet types (currently 8)
private const val PACKET_ID_BITS = 28    // ~268 million IDs ≈ 100 pkts/sec for 31 days

// C++: cPacket in wwnet/wwpacket.h
// Extends BitStream with header fields: type, id, senderId.
// Construct_Full_Packet serializes these into the wire format; Construct_App_Packet reverses it.
//
// Wire header (with WRAPPER_CRC defined = 7 bytes):
//   [Type: 4 bits][Id: 28 bits][SenderId: 1 byte][BitLength: 2 bytes]  = 56 bits = 7 bytes
//
// Encoder IDs for header fields: imported from BitPackIds.kt (bitpackids.h enum, 0-indexed):
//   BITPACK_PACKET_TYPE = 24
//   BITPACK_PACKET_ID   = 25

const val PACKET_HEADER_SIZE = 7  // bytes

const val UNDEFINED_ID = -1

class Packet {
    val payload = BitStream()  // application-level data

    var type: PacketType = PacketType.UNRELIABLE
    var id: Int = UNDEFINED_ID
    var senderId: Int = UNDEFINED_ID

    // Source address is filled in when a packet is received over the network
    var sourceAddress: java.net.InetSocketAddress? = null

    val bitLength: Int get() = payload.bitWritePosition

    companion object {
        init {
            // Register packet header encoders. Called once at startup.
            // C++: cPacket::Init_Encoder() sets these same precisions.
            EncoderRegistry.setPrecision(BITPACK_PACKET_TYPE, PACKET_TYPE_BITS)
            EncoderRegistry.setPrecision(BITPACK_PACKET_ID, PACKET_ID_BITS)
        }

        // C++: cPacket::Construct_Full_Packet
        // Serializes packet header + payload into a single flat byte buffer for sending.
        // Returns byte count of the full packet.
        fun buildWirePacket(packet: Packet): ByteArray {
            val full = BitStream()
            // Header
            full.addByte(packet.type.id.toByte(), encoderType = BITPACK_PACKET_TYPE)
            full.addInt(packet.id, encoderType = BITPACK_PACKET_ID)
            full.addByte(packet.senderId.toByte())
            full.addShort(packet.bitLength.toShort())

            check(full.bitWritePosition == PACKET_HEADER_SIZE * 8) {
                "Header size mismatch: ${full.bitWritePosition} != ${PACKET_HEADER_SIZE * 8}"
            }

            // Copy payload bytes directly (memcpy equivalent)
            val headerBytes = PACKET_HEADER_SIZE
            val payloadBytes = packet.payload.compressedSizeBytes
            val total = headerBytes + payloadBytes
            val result = ByteArray(total)
            System.arraycopy(full.buffer, 0, result, 0, headerBytes)
            System.arraycopy(packet.payload.buffer, 0, result, headerBytes, payloadBytes)
            return result
        }

        // C++: cPacket::Construct_App_Packet
        // Parses wire bytes (header + payload) into a Packet.
        fun parseWirePacket(data: ByteArray, length: Int): Packet {
            val full = BitStream()
            System.arraycopy(data, 0, full.buffer, 0, length)
            full.setBitWritePosition(length * 8)

            val typeId = full.getByte(encoderType = BITPACK_PACKET_TYPE).toInt() and 0xFF
            val id = full.getInt(encoderType = BITPACK_PACKET_ID)
            val senderId = full.getByte().toInt() and 0xFF
            val bitLength = full.getShort().toInt() and 0xFFFF

            val packet = Packet()
            packet.type = PacketType.fromId(typeId)
            packet.id = id
            packet.senderId = senderId

            // Copy payload from the wire data
            val payloadBytes = (bitLength + 7) / 8
            System.arraycopy(data, PACKET_HEADER_SIZE, packet.payload.buffer, 0, payloadBytes)
            packet.payload.setBitWritePosition(bitLength)

            return packet
        }
    }
}
