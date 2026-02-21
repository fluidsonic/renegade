package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScTextObjTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        ScTextObj(type = 3, senderId = 42, recipientId = 99, isHostAdminMessage = true, text = "Hello World")
            .exportCreation(bs)
        assertEquals(3, bs.getByte().toInt() and 0xFF)
        assertEquals(42, bs.getInt())
        assertEquals(99, bs.getInt())
        assertEquals(true, bs.getBool())
        assertEquals("Hello World", bs.getWideString())
    }

    @Test fun `networkClassId is 1001`() {
        assertEquals(1001, ScTextObj(0, 0, 0, false, "").networkClassId)
    }
}
