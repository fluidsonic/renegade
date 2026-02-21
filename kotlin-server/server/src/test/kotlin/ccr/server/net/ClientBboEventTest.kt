package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClientBboEventTest {
    @Test fun `creation round-trip`() {
        val event = ClientBboEvent(senderId = 9, bbo = 256)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = ClientBboEvent()
        parsed.importCreation(bs)
        assertEquals(9, parsed.senderId)
        assertEquals(256, parsed.bbo)
    }

    @Test fun `networkClassId is 1030`() {
        assertEquals(1030, ClientBboEvent().networkClassId)
    }
}
