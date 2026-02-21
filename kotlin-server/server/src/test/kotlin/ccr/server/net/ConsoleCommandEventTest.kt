package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConsoleCommandEventTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        ConsoleCommandEvent(command = "kick 42").exportCreation(bs)
        assertEquals("kick 42", bs.getTerminatedString())
    }

    @Test fun `networkClassId is 1005`() {
        assertEquals(1005, ConsoleCommandEvent("").networkClassId)
    }
}
