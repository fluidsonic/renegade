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

class ScExplosionEventTest {
    companion object {
        @JvmStatic @BeforeAll fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -578.52, 517.15, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -255.34, 471.86, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -67.0, 71.46, 0.2)
        }
    }

    @Test fun `creation - field order`() {
        val bs = BitStream()
        ScExplosionEvent(defId = 1234, posX = 5f, posY = -10f, posZ = 2f, ownerId = 200001)
            .exportCreation(bs)
        assertEquals(1234, bs.getInt())
        assert(abs(bs.getFloat(BITPACK_WORLD_POSITION_X) - 5f) < 0.3f) { "posX" }
        assert(abs(bs.getFloat(BITPACK_WORLD_POSITION_Y) - (-10f)) < 0.3f) { "posY" }
        assert(abs(bs.getFloat(BITPACK_WORLD_POSITION_Z) - 2f) < 0.3f) { "posZ" }
        assertEquals(200001, bs.getInt())
    }

    @Test fun `full creation envelope - networkClassId 1014`() {
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, ScExplosionEvent(1, 0f, 0f, 0f, 2), networkId = 800001)
        // Skip: networkId(32) + dirtyBits(8) + isDeletePending(1) = 41 bits → read in chunks
        bs.getBits(32)  // networkId
        bs.getBits(8)   // dirtyBits
        bs.getBits(1)   // isDeletePending
        assertEquals(1014, bs.getInt())  // networkClassId
    }

    @Test fun `networkClassId is 1014`() {
        assertEquals(1014, ScExplosionEvent(0, 0f, 0f, 0f, 0).networkClassId)
    }
}
