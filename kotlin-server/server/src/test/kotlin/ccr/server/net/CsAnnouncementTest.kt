package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsAnnouncementTest {
    @Test fun `creation round-trip`() {
        val event = CsAnnouncement(
            toId = 10,
            fromId = 20,
            announcementId = 5,
            radioCmdId = 3,
            type = 2,
        )
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsAnnouncement()
        parsed.importCreation(bs)
        assertEquals(10, parsed.toId)
        assertEquals(20, parsed.fromId)
        assertEquals(5, parsed.announcementId)
        assertEquals(3, parsed.radioCmdId)
        assertEquals(2, parsed.type)
    }

    @Test fun `networkClassId is 1037`() {
        assertEquals(1037, CsAnnouncement().networkClassId)
    }
}
