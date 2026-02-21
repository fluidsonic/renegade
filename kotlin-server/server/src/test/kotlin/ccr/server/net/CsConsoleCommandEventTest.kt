package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsConsoleCommandEventTest {
    @Test fun `creation round-trip`() {
        val event = CsConsoleCommandEvent(command = "kick 5")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsConsoleCommandEvent()
        parsed.importCreation(bs)
        assertEquals("kick 5", parsed.command)
    }

    @Test fun `networkClassId is 1035`() {
        assertEquals(1035, CsConsoleCommandEvent().networkClassId)
    }
}
