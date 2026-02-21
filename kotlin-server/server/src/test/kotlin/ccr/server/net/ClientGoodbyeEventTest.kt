package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClientGoodbyeEventTest {
    @Test fun `creation round-trip`() {
        val event = ClientGoodbyeEvent(senderId = 99)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = ClientGoodbyeEvent()
        parsed.importCreation(bs)
        assertEquals(99, parsed.senderId)
    }

    @Test fun `networkClassId is 1024`() {
        assertEquals(1024, ClientGoodbyeEvent().networkClassId)
    }
}
