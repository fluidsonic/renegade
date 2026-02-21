package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MoneyEventTest {
    @Test fun `creation round-trip`() {
        val event = MoneyEvent(senderId = 3, amount = 500)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = MoneyEvent()
        parsed.importCreation(bs)
        assertEquals(3, parsed.senderId)
        assertEquals(500, parsed.amount)
    }

    @Test fun `networkClassId is 1021`() {
        assertEquals(1021, MoneyEvent().networkClassId)
    }
}
