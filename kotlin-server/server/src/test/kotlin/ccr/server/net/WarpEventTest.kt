package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WarpEventTest {
    @Test fun `creation round-trip`() {
        val event = WarpEvent(senderId = 8, playerName = "Tester")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = WarpEvent()
        parsed.importCreation(bs)
        assertEquals(8, parsed.senderId)
        assertEquals("Tester", parsed.playerName)
    }

    @Test fun `creation round-trip - empty player name`() {
        val event = WarpEvent(senderId = 1, playerName = "")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = WarpEvent()
        parsed.importCreation(bs)
        assertEquals("", parsed.playerName)
    }

    @Test fun `networkClassId is 1022`() {
        assertEquals(1022, WarpEvent().networkClassId)
    }
}
