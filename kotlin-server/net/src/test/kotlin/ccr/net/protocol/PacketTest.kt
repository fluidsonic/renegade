package ccr.net.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class PacketTest {

    @Test
    fun `packet header encodes to exactly 7 bytes`() {
        val packet = Packet()
        packet.type = PacketType.RELIABLE
        packet.id = 0
        packet.senderId = 0

        val wire = Packet.buildWirePacket(packet)
        assertEquals(PACKET_HEADER_SIZE, wire.size)
    }

    @Test
    fun `packet header round trip - CONNECT_CS type=4 id=0 sender=255`() {
        val packet = Packet()
        packet.type = PacketType.CONNECT_CS
        packet.id = 0
        packet.senderId = 255

        val wire = Packet.buildWirePacket(packet)
        val parsed = Packet.parseWirePacket(wire, wire.size)

        assertEquals(PacketType.CONNECT_CS, parsed.type)
        assertEquals(0, parsed.id)
        assertEquals(255, parsed.senderId)
    }

    @Test
    fun `packet round trip with payload`() {
        val packet = Packet()
        packet.type = PacketType.RELIABLE
        packet.id = 42
        packet.senderId = 1
        packet.payload.addInt(0xDEADBEEF.toInt())
        packet.payload.addWideString("Player")

        val wire = Packet.buildWirePacket(packet)
        val parsed = Packet.parseWirePacket(wire, wire.size)

        assertEquals(PacketType.RELIABLE, parsed.type)
        assertEquals(42, parsed.id)
        assertEquals(1, parsed.senderId)
        assertEquals(0xDEADBEEF.toInt(), parsed.payload.getInt())
        assertEquals("Player", parsed.payload.getWideString())
    }

    @Test
    fun `type bits in header - verify wire byte for KEEPALIVE type=3`() {
        val packet = Packet()
        packet.type = PacketType.KEEPALIVE
        packet.id = 0
        packet.senderId = 0

        val wire = Packet.buildWirePacket(packet)

        // Type=3 (0011) in 4 bits + id=0 in 28 bits = 32 bits = 4 bytes
        // First nibble: 0011 → high bits of byte[0] = 0011_xxxx
        // id=0 → remaining 28 bits = 0
        // byte[0]: 0011_0000 = 0x30
        assertEquals(0x30.toByte(), wire[0], "First byte should be 0x30 for type=3,id=0")
    }

    @Test
    fun `all packet types encode and decode correctly`() {
        for (type in PacketType.entries) {
            val packet = Packet()
            packet.type = type
            packet.id = 1000
            packet.senderId = 5

            val wire = Packet.buildWirePacket(packet)
            val parsed = Packet.parseWirePacket(wire, wire.size)

            assertEquals(type, parsed.type, "Round trip failed for $type")
            assertEquals(1000, parsed.id, "Id round trip failed for $type")
            assertEquals(5, parsed.senderId, "SenderId round trip failed for $type")
        }
    }

    @Test
    fun `large packet id round trip`() {
        val packet = Packet()
        packet.type = PacketType.UNRELIABLE
        packet.id = 0x0FFFFFFF  // max 28-bit value
        packet.senderId = 0

        val wire = Packet.buildWirePacket(packet)
        val parsed = Packet.parseWirePacket(wire, wire.size)

        assertEquals(0x0FFFFFFF, parsed.id)
    }
}
