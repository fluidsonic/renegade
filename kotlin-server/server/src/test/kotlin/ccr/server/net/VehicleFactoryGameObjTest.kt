package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VehicleFactoryGameObjTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_BUILDING_RADIUS, 0.0, 50.0, 0.1)
            EncoderRegistry.setPrecision(BITPACK_BUILDING_STATE, -1.0, 10.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
        }

        private val ZERO = Vector3(0f, 0f, 0f)

        private fun defaultFactory(isBusy: Boolean = false) = VehicleFactoryGameObj(
            definitionId = 500,
            position     = ZERO,
            sphereCenter = ZERO,
            sphereRadius = 10f,
            isBusy       = isBusy,
        )
    }

    @Test fun `networkClassId is 1000`() {
        assertEquals(1000, defaultFactory().networkClassId)
    }

    @Test fun `exportRare isBusy=false round-trip`() {
        val bs = BitStream()
        defaultFactory(isBusy = false).exportRare(bs)

        bs.getBool()  // isDestroyed
        bs.getBool()  // isPowerOn
        bs.getInt(BITPACK_BUILDING_STATE)  // currentState
        assertFalse(bs.getBool())  // isBusy = false
    }

    @Test fun `exportRare isBusy=true round-trip`() {
        val bs = BitStream()
        defaultFactory(isBusy = true).exportRare(bs)

        bs.getBool()  // isDestroyed
        bs.getBool()  // isPowerOn
        bs.getInt(BITPACK_BUILDING_STATE)  // currentState
        assertTrue(bs.getBool())  // isBusy = true
    }
}
