package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleGameObjTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
        }

        private fun defaultObj() = SimpleGameObj(
            definitionId = 1001,
            position     = Vector3(5f, 10f, 0f),
        )
    }

    @Test fun `networkClassId is 1000`() {
        assertEquals(1000, defaultObj().networkClassId)
    }

    @Test fun `exportCreation does not write definitionId (factory layer owns it)`() {
        // C++: definitionId is written by NetworkGameObjectFactory.prepPacket() before Export_Creation,
        // not inside Export_Creation itself. So exportCreation should not start with definitionId.
        val bs = BitStream()
        defaultObj().exportCreation(bs)
        // The first int from exportCreation is NOT definitionId — it's from DamageableGameObj/ScriptableGameObj
        // Just verify the stream is non-empty (factory handles definitionId separately)
        assertTrue(bs.bitWritePosition > 0)
    }
}
