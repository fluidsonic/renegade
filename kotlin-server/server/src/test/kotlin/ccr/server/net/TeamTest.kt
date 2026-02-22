package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TeamTest {
    // Skip the 41-bit envelope prefix: networkId(32) + dirtyBits(8) + isDeletePending(1)
    private fun skipEnvelopePrefix(bs: BitStream) {
        bs.getInt()   // networkId (32 bits)
        bs.getByte()  // dirtyBits (8 bits)
        bs.getBool()  // isDeletePending (1 bit)
    }

    @Test fun `full creation envelope - NOD team`() {
        val team = Team(teamNumber = 0, kills = 0, deaths = 0, score = 0f)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, team, networkId = 100001)
        skipEnvelopePrefix(bs)
        assertEquals(1010, bs.getInt())  // networkClassId
        assertEquals(0, bs.getInt())     // teamNumber = NOD
    }

    @Test fun `full creation envelope - GDI team`() {
        val team = Team(teamNumber = 1)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, team, networkId = 100002)
        skipEnvelopePrefix(bs)
        assertEquals(1010, bs.getInt())  // networkClassId
        assertEquals(1, bs.getInt())     // teamNumber = GDI
    }

    @Test fun `rare tier - zero stats`() {
        val bs = BitStream()
        Team(teamNumber = 0, kills = 0, deaths = 0).exportRare(bs)
        assertEquals(0, bs.getInt())  // kills
        assertEquals(0, bs.getInt())  // deaths
    }

    @Test fun `rare tier - non-zero stats`() {
        val bs = BitStream()
        Team(teamNumber = 1, kills = 42, deaths = 17).exportRare(bs)
        assertEquals(42, bs.getInt())  // kills
        assertEquals(17, bs.getInt())  // deaths
    }

    @Test fun `occasional tier - zero score`() {
        val bs = BitStream()
        Team(teamNumber = 0, score = 0f).exportOccasional(bs)
        assertEquals(0f, bs.getFloat())
    }

    @Test fun `occasional tier - non-zero score`() {
        val bs = BitStream()
        Team(teamNumber = 0, score = 1500f).exportOccasional(bs)
        assertEquals(1500f, bs.getFloat())
    }

    @Test fun `networkClassId is 1010`() {
        assertEquals(1010, Team(0).networkClassId)
    }
}
