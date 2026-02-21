package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GodModeEventTest {
    @Test fun `creation round-trip`() {
        val event = GodModeEvent(senderId = 1, password = "secret")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = GodModeEvent()
        parsed.importCreation(bs)
        assertEquals(1, parsed.senderId)
        assertEquals("secret", parsed.password)
    }

    @Test fun `creation round-trip - empty password`() {
        val event = GodModeEvent(senderId = 2, password = "")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = GodModeEvent()
        parsed.importCreation(bs)
        assertEquals("", parsed.password)
    }

    @Test fun `networkClassId is 1027`() {
        assertEquals(1027, GodModeEvent().networkClassId)
    }
}
