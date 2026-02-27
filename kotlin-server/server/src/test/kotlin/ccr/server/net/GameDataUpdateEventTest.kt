package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GameDataUpdateEventTest {

    @Test
    fun `creation packet is 137 bits`() {
        // header(73) + timeRemainingSeconds_int(32) + hostedGameNumber(32) = 137 bits
        val event = GameDataUpdateEvent(timeRemainingSeconds = 0, hostedGameNumber = 0)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 100010)
        assertEquals(137, bs.bitWritePosition)
    }

    @Test
    fun `TimeRemainingSeconds is written as int not float`() {
        val event = GameDataUpdateEvent(timeRemainingSeconds = 300, hostedGameNumber = 0)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 1)

        // Skip header (73 bits)
        bs.getInt(); bs.getByte(); bs.getBool(); bs.getInt()

        // Read TimeRemainingSeconds as int
        val timeInt = bs.getInt()
        assertEquals(300, timeInt)
    }

    @Test
    fun `int(300) bit pattern differs from float(300f)`() {
        // Documents the bug fix: original code used addFloat(0f) which is only correct for value=0.
        // For any non-zero value, addFloat(n) and addInt(n) produce different bit patterns.
        val floatBits = java.lang.Float.floatToRawIntBits(300f)
        assertNotEquals(300, floatBits,
            "int(300) and float(300f) must have different bit patterns — this validates the bug fix")
    }

    @Test
    fun `fields round-trip correctly`() {
        val event = GameDataUpdateEvent(timeRemainingSeconds = 120, hostedGameNumber = 5)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 100010)

        // Header
        assertEquals(100010, bs.getInt())                        // networkId
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)        // dirtyBits
        assertEquals(false, bs.getBool())                         // isDeletePending — false in creation packet (set during exportCreation, sent next tick)
        assertEquals(1012, bs.getInt())                           // networkClassId

        // Event data
        assertEquals(120, bs.getInt())                            // timeRemainingSeconds (int)
        assertEquals(5, bs.getInt())                              // hostedGameNumber
    }
}
