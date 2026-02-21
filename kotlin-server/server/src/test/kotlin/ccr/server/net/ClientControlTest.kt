package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ClientControlTest {
    @Test fun `creation export writes clientId`() {
        val bs = BitStream()
        ClientControl(clientId = 5).exportCreation(bs)
        assertEquals(5, bs.getInt())
    }

    @Test fun `creation import reads clientId`() {
        val bs = BitStream()
        ClientControl(clientId = 42).exportCreation(bs)
        val parsed = ClientControl()
        parsed.importCreation(bs)
        assertEquals(42, parsed.clientId)
    }

    @Test fun `creation round-trip - boundary values`() {
        for (id in listOf(0, 1, 127)) {
            val bs = BitStream()
            ClientControl(clientId = id).exportCreation(bs)
            val parsed = ClientControl()
            parsed.importCreation(bs)
            assertEquals(id, parsed.clientId)
        }
    }

    @Test fun `frequent - smartObjId = -1`() {
        val bs = BitStream()
        ClientControl(clientId = 1, smartObjId = -1).exportFrequent(bs)
        assertEquals(-1, bs.getInt())
    }

    @Test fun `frequent - import with smartObjId = -1`() {
        val bs = BitStream()
        ClientControl(clientId = 1, smartObjId = -1).exportFrequent(bs)
        val parsed = ClientControl()
        parsed.importFrequent(bs)
        assertEquals(-1, parsed.smartObjId)
    }

    @Test fun `networkClassId is 1017`() {
        assertEquals(1017, ClientControl().networkClassId)
    }
}
