package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChangeTeamEventTest {
    @Test fun `creation round-trip`() {
        val event = ChangeTeamEvent(senderId = 42)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = ChangeTeamEvent()
        parsed.importCreation(bs)
        assertEquals(42, parsed.senderId)
    }

    @Test fun `networkClassId is 1020`() {
        assertEquals(1020, ChangeTeamEvent().networkClassId)
    }
}
