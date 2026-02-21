package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LoadingEventTest {
    @Test fun `creation round-trip - is loading`() {
        val event = LoadingEvent(senderId = 4, isLoading = true)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = LoadingEvent()
        parsed.importCreation(bs)
        assertEquals(4, parsed.senderId)
        assertEquals(true, parsed.isLoading)
    }

    @Test fun `creation round-trip - done loading`() {
        val event = LoadingEvent(senderId = 4, isLoading = false)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = LoadingEvent()
        parsed.importCreation(bs)
        assertEquals(false, parsed.isLoading)
    }

    @Test fun `networkClassId is 1026`() {
        assertEquals(1026, LoadingEvent().networkClassId)
    }
}
