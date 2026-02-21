package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WinEventTest {
    @Test fun `creation round-trip`() {
        val bs = BitStream()
        WinEvent(
            winner = 1,
            loser = 2,
            hostedGameNumber = 5,
            isMapCycleOver = false,
            winType = 3,
            gameDuration = 1200,
            mvpName = "TopGun",
            mvpCount = 7,
            modNameCrc = -123456789,
            mapNameCrc = 987654321,
        ).exportCreation(bs)
        assertEquals(1, bs.getInt())
        assertEquals(2, bs.getInt())
        assertEquals(5, bs.getInt())
        assertEquals(false, bs.getBool())
        assertEquals(3, bs.getInt())
        assertEquals(1200, bs.getInt())
        assertEquals("TopGun", bs.getWideString(permitEmpty = true))
        assertEquals(7, bs.getInt())
        assertEquals(-123456789, bs.getInt())
        assertEquals(987654321, bs.getInt())
    }

    @Test fun `networkClassId is 1003`() {
        assertEquals(1003, WinEvent(0, 0, 0, false, 0, 0, "", 0, 0, 0).networkClassId)
    }
}
