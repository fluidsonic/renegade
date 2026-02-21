package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsHintTest {
    @Test fun `creation round-trip`() {
        val event = CsHint(senderId = 15, subjectId = 300)
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsHint()
        parsed.importCreation(bs)
        assertEquals(15, parsed.senderId)
        assertEquals(300, parsed.subjectId)
    }

    @Test fun `networkClassId is 1036`() {
        assertEquals(1036, CsHint().networkClassId)
    }
}
