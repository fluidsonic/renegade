package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VipModeEventTest {
    @Test fun `creation round-trip`() {
        val event = VipModeEvent(senderId = 6, password = "vip123")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = VipModeEvent()
        parsed.importCreation(bs)
        assertEquals(6, parsed.senderId)
        assertEquals("vip123", parsed.password)
    }

    @Test fun `creation round-trip - empty password`() {
        val event = VipModeEvent(senderId = 3, password = "")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = VipModeEvent()
        parsed.importCreation(bs)
        assertEquals("", parsed.password)
    }

    @Test fun `networkClassId is 1028`() {
        assertEquals(1028, VipModeEvent().networkClassId)
    }
}
