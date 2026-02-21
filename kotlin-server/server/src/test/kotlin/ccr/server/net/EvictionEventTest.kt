package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EvictionEventTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        EvictionEvent(evictionCode = 7).exportCreation(bs)
        assertEquals(7, bs.getInt())
    }

    @Test fun `networkClassId is 1009`() {
        assertEquals(1009, EvictionEvent(0).networkClassId)
    }
}
