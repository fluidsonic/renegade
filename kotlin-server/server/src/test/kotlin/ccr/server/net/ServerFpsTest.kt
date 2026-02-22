package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ServerFpsTest {
    @Test fun `frequent tier - fps value written as int`() {
        val bs = BitStream()
        ServerFps().also { it.setFps(60) }.exportFrequent(bs)
        assertEquals(60, bs.getInt())
    }

    @Test fun `frequent tier - zero fps`() {
        val bs = BitStream()
        ServerFps().also { it.setFps(0) }.exportFrequent(bs)
        assertEquals(0, bs.getInt())
    }

    @Test fun `frequent tier - max fps`() {
        val bs = BitStream()
        ServerFps().also { it.setFps(100) }.exportFrequent(bs)
        assertEquals(100, bs.getInt())
    }

    @Test fun `setDeletePending is no-op`() {
        val obj = ServerFps().also { it.setFps(30) }
        obj.setDeletePending()
        assertFalse(obj.isDeletePending)
    }

    @Test fun `full creation envelope`() {
        val obj = ServerFps().also { it.setFps(45) }
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, obj, networkId = 300001)
        bs.getInt()   // networkId
        bs.getByte()  // dirtyBits
        bs.getBool()  // isDeletePending
        val networkClassId = bs.getInt()
        assertEquals(obj.networkClassId, networkClassId)
    }
}
