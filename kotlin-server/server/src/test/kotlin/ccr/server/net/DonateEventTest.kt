package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DonateEventTest {
    @Test fun `creation round-trip`() {
        val event = DonateEvent(senderId = 7, amount = 200, recipientId = 13)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = DonateEvent()
        parsed.importCreation(bs)
        assertEquals(7, parsed.senderId)
        assertEquals(200, parsed.amount)
        assertEquals(13, parsed.recipientId)
    }

    @Test fun `networkClassId is 1038`() {
        assertEquals(1038, DonateEvent().networkClassId)
    }
}
