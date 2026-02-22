package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.bitstream.BITPACK_WORLD_POSITION_X
import ccr.net.bitstream.BITPACK_WORLD_POSITION_Y
import ccr.net.bitstream.BITPACK_WORLD_POSITION_Z
import ccr.net.bitstream.EncoderRegistry
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals

class ScObeliskEventTest {
    companion object {
        @JvmStatic @BeforeAll fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -578.52, 517.15, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -255.34, 471.86, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -67.0, 71.46, 0.2)
        }
    }

    @Test fun `creation - field order`() {
        val bs = BitStream()
        ScObeliskEvent(defId = 5678, posX = 10f, posY = -5f, posZ = 3f, ownerId = 100042)
            .exportCreation(bs)
        assertEquals(5678, bs.getInt())
        assert(abs(bs.getFloat(BITPACK_WORLD_POSITION_X) - 10f) < 0.3f) { "posX" }
        assert(abs(bs.getFloat(BITPACK_WORLD_POSITION_Y) - (-5f)) < 0.3f) { "posY" }
        assert(abs(bs.getFloat(BITPACK_WORLD_POSITION_Z) - 3f) < 0.3f) { "posZ" }
        assertEquals(100042, bs.getInt())
    }

    @Test fun `full creation envelope - networkClassId 1015`() {
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, ScObeliskEvent(1, 0f, 0f, 0f, 3), networkId = 900001)
        // Skip: networkId(32) + dirtyBits(8) + isDeletePending(1) = 41 bits → read in chunks
        bs.getBits(32)  // networkId
        bs.getBits(8)   // dirtyBits
        bs.getBits(1)   // isDeletePending
        assertEquals(1015, bs.getInt())  // networkClassId
    }

    @Test fun `networkClassId is 1015`() {
        assertEquals(1015, ScObeliskEvent(0, 0f, 0f, 0f, 0).networkClassId)
    }
}
