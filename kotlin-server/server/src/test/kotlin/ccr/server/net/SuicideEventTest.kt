package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SuicideEventTest {
    @Test fun `creation round-trip`() {
        val event = SuicideEvent(senderId = 7)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = SuicideEvent()
        parsed.importCreation(bs)
        assertEquals(7, parsed.senderId)
    }

    @Test fun `networkClassId is 1019`() {
        assertEquals(1019, SuicideEvent().networkClassId)
    }
}
