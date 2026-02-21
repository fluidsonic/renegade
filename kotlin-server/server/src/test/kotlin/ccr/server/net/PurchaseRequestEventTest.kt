package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PurchaseRequestEventTest {
    @Test fun `creation round-trip`() {
        val event = PurchaseRequestEvent(
            senderId = 5,
            purchaseType = 1,
            itemIndex = 3,
            altSkinIndex = 2,
        )
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = PurchaseRequestEvent()
        parsed.importCreation(bs)
        assertEquals(5, parsed.senderId)
        assertEquals(1, parsed.purchaseType)
        assertEquals(3, parsed.itemIndex)
        assertEquals(2, parsed.altSkinIndex)
    }

    @Test fun `networkClassId is 1023`() {
        assertEquals(1023, PurchaseRequestEvent().networkClassId)
    }
}
