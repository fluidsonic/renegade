package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScoreEventTest {
    @Test fun `creation round-trip`() {
        val event = ScoreEvent(senderId = 11, amount = 150)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = ScoreEvent()
        parsed.importCreation(bs)
        assertEquals(11, parsed.senderId)
        assertEquals(150, parsed.amount)
    }

    @Test fun `networkClassId is 1029`() {
        assertEquals(1029, ScoreEvent().networkClassId)
    }
}
