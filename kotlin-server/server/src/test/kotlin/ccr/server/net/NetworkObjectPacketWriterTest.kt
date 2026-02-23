package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkObjectPacketWriterTest {

    // ---- writeOccasionalUpdate ----

    @Test fun `writeOccasionalUpdate - header layout - dirtyBits=0x03, no networkClassId`() {
        val team = Team(teamNumber = 0, score = 42f)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeOccasionalUpdate(bs, team, networkId = 999)

        assertEquals(999, bs.getInt())                                // networkId
        assertEquals(0x03, bs.getByte().toInt() and 0xFF)             // dirtyBits = BIT_OCCASIONAL
        assertFalse(bs.getBool())                                      // isDeletePending = false
        // No networkClassId — BIT_CREATION (0x08) is not set
        assertEquals(42f, bs.getFloat())                               // score from exportOccasional
    }

    @Test fun `writeOccasionalUpdate - total bits matches expected header + occasional + frequent`() {
        // Team: occasional = 1 float (32b), frequent = 0 bits
        // Header: networkId(32) + dirtyBits(8) + isDeletePending(1) = 41 bits
        // Total: 41 + 32 = 73 bits
        val team = Team(teamNumber = 0, score = 0f)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeOccasionalUpdate(bs, team, networkId = 1)
        assertEquals(73, bs.bitWritePosition)
    }

    // ---- writeFrequentUpdate ----

    @Test fun `writeFrequentUpdate - header layout - dirtyBits=0x01, no networkClassId`() {
        val team = Team(teamNumber = 0)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeFrequentUpdate(bs, team, networkId = 888)

        assertEquals(888, bs.getInt())                                 // networkId
        assertEquals(0x01, bs.getByte().toInt() and 0xFF)              // dirtyBits = BIT_FREQUENT
        assertFalse(bs.getBool())                                       // isDeletePending = false
        // No networkClassId — BIT_CREATION not set. Team has no frequent data — nothing more to read.
    }

    @Test fun `writeFrequentUpdate - total bits for Team (no frequent data) = 41`() {
        // Header: 32 + 8 + 1 = 41. Team::exportFrequent writes nothing.
        val team = Team(teamNumber = 0)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeFrequentUpdate(bs, team, networkId = 1)
        assertEquals(41, bs.bitWritePosition)
    }

    // ---- isDeletePending combined with dirty bits ----

    @Test fun `writeOccasionalUpdate - isDeletePending=true sets flag in header`() {
        val team = Team(teamNumber = 0, score = 7f)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeOccasionalUpdate(bs, team, networkId = 555, isDeletePending = true)

        assertEquals(555, bs.getInt())                                 // networkId
        assertEquals(0x03, bs.getByte().toInt() and 0xFF)              // dirtyBits = BIT_OCCASIONAL
        assertTrue(bs.getBool())                                        // isDeletePending = true
        assertEquals(7f, bs.getFloat())                                 // score — payload still present
    }

    @Test fun `writeFrequentUpdate - isDeletePending=true sets flag in header`() {
        val team = Team(teamNumber = 0)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeFrequentUpdate(bs, team, networkId = 444, isDeletePending = true)

        assertEquals(444, bs.getInt())                                 // networkId
        assertEquals(0x01, bs.getByte().toInt() and 0xFF)              // dirtyBits = BIT_FREQUENT
        assertTrue(bs.getBool())                                        // isDeletePending = true
    }

    // ---- bit constant verification ----

    @Test fun `BIT constants match C++ DIRTY_BIT enum`() {
        assertEquals(0x01, NetworkObject.BIT_FREQUENT)
        assertEquals(0x03, NetworkObject.BIT_OCCASIONAL)
        assertEquals(0x07, NetworkObject.BIT_RARE)
        assertEquals(0x0F, NetworkObject.BIT_CREATION)
    }
}
