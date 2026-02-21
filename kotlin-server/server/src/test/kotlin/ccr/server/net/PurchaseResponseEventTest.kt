package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PurchaseResponseEventTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        PurchaseResponseEvent(purchaserId = 55, responseId = 2).exportCreation(bs)
        assertEquals(55, bs.getInt())
        assertEquals(2, bs.getInt())
    }

    @Test fun `networkClassId is 1004`() {
        assertEquals(1004, PurchaseResponseEvent(0, 0).networkClassId)
    }
}
