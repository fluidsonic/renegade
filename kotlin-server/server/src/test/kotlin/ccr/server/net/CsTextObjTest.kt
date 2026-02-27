package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsTextObjTest {
    @Test fun `creation round-trip`() {
        val event = CsTextObj(
            type = 2,
            senderId = 10,
            recipientId = 20,
            text = "Hello World",
        )
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsTextObj()
        parsed.importCreation(bs)
        assertEquals(2, parsed.type)
        assertEquals(10, parsed.senderId)
        assertEquals(20, parsed.recipientId)
        assertEquals("Hello World", parsed.text)
    }

    @Test fun `creation round-trip - empty text`() {
        val event = CsTextObj(type = 0, senderId = 5, recipientId = 0, text = "")
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsTextObj()
        parsed.importCreation(bs)
        assertEquals("", parsed.text)
    }

    @Test fun `networkClassId is 1018`() {
        assertEquals(1018, CsTextObj().networkClassId)
    }
}
