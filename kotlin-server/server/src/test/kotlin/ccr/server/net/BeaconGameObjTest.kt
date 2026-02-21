package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BeaconGameObjTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
        }
    }

    @Test fun `networkClassId is 1000`() {
        val beacon = BeaconGameObj(definitionId = 55, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, beacon.networkClassId)
    }

    @Test fun `exportRare round-trip state and ownerId`() {
        val beacon = BeaconGameObj(
            definitionId = 55,
            position     = Vector3(0f, 0f, 0f),
            modelName    = "",
            animName     = "",
            state        = 3,
            ownerId      = 777,
        )
        val bs = BitStream()
        beacon.exportRare(bs)

        // Skip PhysicalGameObj rare fields
        bs.getTerminatedString(permitEmpty = true)   // modelName
        bs.getTerminatedString(permitEmpty = true)   // animName
        bs.getInt()  // currFrame
        bs.getInt()  // targetFrame
        bs.getInt()  // animMode
        bs.getInt()  // hostModelId
        bs.getInt()  // hostBone
        bs.getInt()  // playerType
        bs.getBool() // hudPokable

        // Beacon fields
        assertEquals(3, bs.getInt())    // state
        assertEquals(777, bs.getInt())  // ownerId
    }
}
