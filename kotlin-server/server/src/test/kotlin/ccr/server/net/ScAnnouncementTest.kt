package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScAnnouncementTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        ScAnnouncement(toId = 10, fromId = 20, announcementId = 5, radioCmdId = 3, type = 1)
            .exportCreation(bs)
        assertEquals(10, bs.getInt())
        assertEquals(20, bs.getInt())
        assertEquals(5, bs.getInt())
        assertEquals(3, bs.getInt())
        assertEquals(1, bs.getByte().toInt() and 0xFF)
    }

    @Test fun `networkClassId is 1016`() {
        assertEquals(1016, ScAnnouncement(0, 0, 0, 0, 0).networkClassId)
    }
}
