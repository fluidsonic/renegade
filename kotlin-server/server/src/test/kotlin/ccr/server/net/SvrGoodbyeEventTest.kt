package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SvrGoodbyeEventTest {
    @Test fun `creation round-trip - true`() {
        val bs = BitStream()
        SvrGoodbyeEvent(isQuickFullExitRequested = true).exportCreation(bs)
        assertEquals(true, bs.getBool())
    }

    @Test fun `creation round-trip - false`() {
        val bs = BitStream()
        SvrGoodbyeEvent(isQuickFullExitRequested = false).exportCreation(bs)
        assertEquals(false, bs.getBool())
    }

    @Test fun `networkClassId is 1007`() {
        assertEquals(1007, SvrGoodbyeEvent(false).networkClassId)
    }
}
