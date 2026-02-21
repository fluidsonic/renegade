package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WeatherMgrTest {
    @Test fun `rare - all zero parameters`() {
        val bs = BitStream()
        WeatherMgr().exportRare(bs)
        repeat(6 * 4) { assertEquals(0f, bs.getFloat()) }
        assertEquals(0, bs.getInt())  // windOverrideCount
        assertEquals(0, bs.getInt())  // precipitationOverrideCount
    }

    @Test fun `rare - windHeading is first parameter`() {
        val bs = BitStream()
        WeatherMgr(windHeading = BackgroundParameter(normalTarget = 90f)).exportRare(bs)
        assert(abs(bs.getFloat() - 90f) < 0.001f) { "windHeading.normalTarget" }
    }

    @Test fun `rare - total bit count`() {
        val bs = BitStream()
        WeatherMgr().exportRare(bs)
        // 6 params * 4 floats * 32 bits + 2 ints * 32 bits = 6*4*32 + 2*32 = 768 + 64 = 832
        assertEquals(6 * 4 * 32 + 2 * 32, bs.bitWritePosition)
    }

    @Test fun `rare - override counts at end`() {
        val bs = BitStream()
        WeatherMgr(windOverrideCount = 2, precipitationOverrideCount = 5).exportRare(bs)
        repeat(6 * 4) { bs.getFloat() }
        assertEquals(2, bs.getInt())
        assertEquals(5, bs.getInt())
    }

    @Test fun `setDeletePending is no-op`() {
        val mgr = WeatherMgr()
        mgr.setDeletePending()
        assertFalse(mgr.isDeletePending)
    }

    @Test fun `networkClassId is 0`() {
        assertEquals(0, WeatherMgr().networkClassId)
    }
}
