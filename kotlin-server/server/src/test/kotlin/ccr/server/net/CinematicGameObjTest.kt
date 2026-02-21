package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CinematicGameObjTest {

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
        val obj = CinematicGameObj(definitionId = 1300, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `exportRare round-trip cinematicAnimName and animMode`() {
        val obj = CinematicGameObj(
            definitionId       = 1301,
            position           = Vector3(0f, 0f, 0f),
            modelName          = "",
            animName           = "",
            cinematicAnimName  = "C_ANIM.IDLE",
            animMode           = CinematicGameObj.ANIM_MODE_LOOP,
        )
        val bs = BitStream()
        obj.exportRare(bs)

        // Skip PhysicalGameObj rare fields
        bs.getTerminatedString(permitEmpty = true)   // modelName
        bs.getTerminatedString(permitEmpty = true)   // animName
        bs.getInt()  // currFrame
        bs.getInt()  // targetFrame
        bs.getInt()  // animMode (PhysicalGameObj's anim_mode field)
        bs.getInt()  // hostModelId
        bs.getInt()  // hostBone
        bs.getInt()  // playerType
        bs.getBool() // hudPokable

        // CinematicGameObj fields
        val name = bs.getTerminatedString(permitEmpty = true)
        val mode = bs.getInt()

        assertEquals("C_ANIM.IDLE", name)
        assertEquals(CinematicGameObj.ANIM_MODE_LOOP, mode)
    }

    @Test fun `exportRare empty cinematicAnimName is permitted`() {
        val obj = CinematicGameObj(
            definitionId      = 1302,
            position          = Vector3(0f, 0f, 0f),
            cinematicAnimName = "",
            animMode          = CinematicGameObj.ANIM_MODE_TARGET,
        )
        val bs = BitStream()
        obj.exportRare(bs)  // should not throw

        // Navigate to cinematic fields
        bs.getTerminatedString(permitEmpty = true)
        bs.getTerminatedString(permitEmpty = true)
        bs.getInt(); bs.getInt(); bs.getInt(); bs.getInt(); bs.getInt()
        bs.getInt(); bs.getBool()

        val name = bs.getTerminatedString(permitEmpty = true)
        val mode = bs.getInt()

        assertEquals("", name)
        assertEquals(CinematicGameObj.ANIM_MODE_TARGET, mode)
    }
}
