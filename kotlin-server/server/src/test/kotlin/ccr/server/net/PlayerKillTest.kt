package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PlayerKillTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        PlayerKill(killerId = 101, victimId = 202).exportCreation(bs)
        assertEquals(101, bs.getInt())
        assertEquals(202, bs.getInt())
    }

    @Test fun `networkClassId is 1002`() {
        assertEquals(1002, PlayerKill(0, 0).networkClassId)
    }
}
