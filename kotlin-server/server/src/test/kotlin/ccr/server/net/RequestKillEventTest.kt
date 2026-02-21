package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RequestKillEventTest {
    @Test fun `creation round-trip`() {
        val event = RequestKillEvent(objectId = 777)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = RequestKillEvent()
        parsed.importCreation(bs)
        assertEquals(777, parsed.objectId)
    }

    @Test fun `networkClassId is 1034`() {
        assertEquals(1034, RequestKillEvent().networkClassId)
    }
}
