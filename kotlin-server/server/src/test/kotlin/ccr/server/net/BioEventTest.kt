package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BioEventTest {
    @Test fun `creation round-trip - typical join`() {
        val event = BioEvent(
            senderId = 7,
            nickname = "TestPlayer",
            teamChoice = 1,
            clanId = 0,
            mapName = "C&C_Under",
        )
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = BioEvent()
        parsed.importCreation(bs)

        assertEquals(7, parsed.senderId)
        assertEquals("TestPlayer", parsed.nickname)
        assertEquals(1, parsed.teamChoice)
        assertEquals(0, parsed.clanId)
        assertEquals("C&C_Under", parsed.mapName)
    }

    @Test fun `creation round-trip - NOD team`() {
        val event = BioEvent(senderId = 2, nickname = "X", teamChoice = 0, mapName = "C&C_Islands")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = BioEvent()
        parsed.importCreation(bs)
        assertEquals(0, parsed.teamChoice)
    }

    @Test fun `creation round-trip - with clan id`() {
        val event = BioEvent(senderId = 3, nickname = "Clan", teamChoice = 1,
                             clanId = 999999, mapName = "C&C_Under")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = BioEvent()
        parsed.importCreation(bs)
        assertEquals(999999, parsed.clanId)
    }

    @Test fun `full creation envelope - classId is 1025`() {
        val event = BioEvent(senderId = 1, nickname = "P", teamChoice = 0, mapName = "C&C_Under")
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 500001)
        // Skip 41-bit header: networkId(32) + dirtyBits(8) + isDeletePending(1, compressed bool)
        bs.getInt()   // networkId
        bs.getByte()  // dirtyBits
        bs.getBool()  // isDeletePending (1 bit when compression enabled)
        assertEquals(1025, bs.getInt())
    }

    @Test fun `networkClassId is 1025`() {
        assertEquals(1025, BioEvent().networkClassId)
    }
}
