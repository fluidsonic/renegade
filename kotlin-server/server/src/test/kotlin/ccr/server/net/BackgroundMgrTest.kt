package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.math.abs

class BackgroundMgrTest {
    private val zeroParam = BackgroundParameter()
    private val testParam = BackgroundParameter(normalTarget = 0.5f, normalDuration = 3.0f, overrideTarget = 0.8f, overrideDuration = 1.0f)

    @Test fun `rare - all zero parameters`() {
        val bs = BitStream()
        BackgroundMgr().exportRare(bs)
        repeat(8 * 4) { assertEquals(0f, bs.getFloat()) }
        assertEquals(0, bs.getInt())  // cloudOverrideCount
        assertEquals(0, bs.getInt())  // lightningOverrideCount
        assertEquals(0, bs.getInt())  // skyTintOverrideCount
    }

    @Test fun `rare - skyTintFactor is first parameter`() {
        val bs = BitStream()
        BackgroundMgr(skyTintFactor = testParam).exportRare(bs)
        assert(abs(bs.getFloat() - 0.5f) < 0.001f) { "normalTarget" }
        assert(abs(bs.getFloat() - 3.0f) < 0.001f) { "normalDuration" }
        assert(abs(bs.getFloat() - 0.8f) < 0.001f) { "overrideTarget" }
        assert(abs(bs.getFloat() - 1.0f) < 0.001f) { "overrideDuration" }
        repeat(7 * 4) { assertEquals(0f, bs.getFloat()) }
    }

    @Test fun `rare - override counts at end`() {
        val bs = BitStream()
        BackgroundMgr(cloudOverrideCount = 3, lightningOverrideCount = 1, skyTintOverrideCount = 2).exportRare(bs)
        repeat(8 * 4) { bs.getFloat() }
        assertEquals(3, bs.getInt())
        assertEquals(1, bs.getInt())
        assertEquals(2, bs.getInt())
    }

    @Test fun `rare - total bit count`() {
        val bs = BitStream()
        BackgroundMgr().exportRare(bs)
        // 8 params * 4 floats * 32 bits + 3 ints * 32 bits = 8*4*32 + 3*32 = 1024 + 96 = 1120
        assertEquals(8 * 4 * 32 + 3 * 32, bs.bitWritePosition)
    }

    @Test fun `setDeletePending is no-op`() {
        val mgr = BackgroundMgr()
        mgr.setDeletePending()
        assertFalse(mgr.isDeletePending)
    }

    @Test fun `networkClassId is 0`() {
        assertEquals(0, BackgroundMgr().networkClassId)
    }
}
