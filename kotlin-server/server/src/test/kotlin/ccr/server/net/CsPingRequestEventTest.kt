package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsPingRequestEventTest {
    @Test fun `creation round-trip`() {
        val event = CsPingRequestEvent(senderId = 12, pingNumber = 5)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsPingRequestEvent()
        parsed.importCreation(bs)
        assertEquals(12, parsed.senderId)
        assertEquals(5, parsed.pingNumber)
    }

    @Test fun `networkClassId is 1032`() {
        assertEquals(1032, CsPingRequestEvent().networkClassId)
    }
}
