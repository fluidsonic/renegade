package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClientFpsTest {
    @Test fun `creation export writes clientId`() {
        val bs = BitStream()
        ClientFps(clientId = 3).exportCreation(bs)
        assertEquals(3, bs.getInt())
    }

    @Test fun `creation import reads clientId`() {
        val bs = BitStream()
        ClientFps(clientId = 7).exportCreation(bs)
        val parsed = ClientFps()
        parsed.importCreation(bs)
        assertEquals(7, parsed.clientId)
    }

    @Test fun `frequent export writes fps as byte`() {
        val bs = BitStream()
        ClientFps(fps = 45).exportFrequent(bs)
        assertEquals(45, bs.getByte().toInt() and 0xFF)
    }

    @Test fun `frequent import reads fps`() {
        val bs = BitStream()
        ClientFps(fps = 30).exportFrequent(bs)
        val parsed = ClientFps()
        parsed.importFrequent(bs)
        assertEquals(30, parsed.fps)
    }

    @Test fun `frequent - fps boundary values`() {
        for (fps in listOf(0, 1, 60, 127, 255)) {
            val bs = BitStream()
            ClientFps(fps = fps).exportFrequent(bs)
            val parsed = ClientFps()
            parsed.importFrequent(bs)
            assertEquals(fps, parsed.fps)
        }
    }

    @Test fun `networkClassId is 1031`() {
        assertEquals(1031, ClientFps().networkClassId)
    }
}
