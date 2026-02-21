package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class C4GameObjTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_VELOCITY, -90.0, 90.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
        }
    }

    @Test fun `networkClassId is 1000`() {
        val c4 = C4GameObj(definitionId = 42, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, c4.networkClassId)
    }

    @Test fun `exportRare unstuck - round-trip basic fields`() {
        val c4 = C4GameObj(
            definitionId = 42,
            position     = Vector3(0f, 0f, 0f),
            modelName    = "",
            animName     = "",
            ammoDef      = 123,
            ownerId      = 456,
            velX         = 1.5f,
            velY         = -2.0f,
            velZ         = 0.5f,
            stuck        = false,
        )
        val bs = BitStream()
        c4.exportRare(bs)

        // Skip PhysicalGameObj rare fields: modelName(short+0) + animName(short+0)
        // + currFrame(32) + targetFrame(32) + animMode(32) + hostModelId(32) + hostBone(32)
        // + playerType(32) + hudPokable(1) = 16+16+5*32+32+1 = 225 bits
        // modelName="" → short(0) = 16 bits; animName="" → short(0) = 16 bits
        bs.getTerminatedString(permitEmpty = true)   // modelName
        bs.getTerminatedString(permitEmpty = true)   // animName
        bs.getInt()  // currFrame
        bs.getInt()  // targetFrame
        bs.getInt()  // animMode
        bs.getInt()  // hostModelId
        bs.getInt()  // hostBone
        bs.getInt()  // playerType
        bs.getBool() // hudPokable

        // Now C4 fields
        assertEquals(123, bs.getInt())   // ammoDef
        assertEquals(456, bs.getInt())   // ownerId
        val vx = bs.getFloat(BITPACK_VEHICLE_VELOCITY)
        val vy = bs.getFloat(BITPACK_VEHICLE_VELOCITY)
        val vz = bs.getFloat(BITPACK_VEHICLE_VELOCITY)
        assertEquals(1.5f, vx, absoluteTolerance = 0.02f)
        assertEquals(-2.0f, vy, absoluteTolerance = 0.02f)
        assertEquals(0.5f, vz, absoluteTolerance = 0.02f)
        assertFalse(bs.getBool())  // stuck = false
    }

    @Test fun `exportRare stuck with stuckToObject - round-trip`() {
        val c4 = C4GameObj(
            definitionId   = 42,
            position       = Vector3(0f, 0f, 0f),
            modelName      = "",
            animName       = "",
            ammoDef        = 10,
            ownerId        = 20,
            velX           = 0f,
            velY           = 0f,
            velZ           = 0f,
            stuck          = true,
            stuckPosX      = 10f,
            stuckPosY      = 20f,
            stuckPosZ      = 5f,
            stuckMct       = true,
            stuckToObject  = true,
            stuckObjectId  = 999,
            stuckOffsetX   = 0.1f,
            stuckOffsetY   = 0.2f,
            stuckOffsetZ   = 0.3f,
            stuckBone      = 7,
            stuckStaticAnim = false,
        )
        val bs = BitStream()
        c4.exportRare(bs)

        // Skip PhysicalGameObj fields
        bs.getTerminatedString(permitEmpty = true)
        bs.getTerminatedString(permitEmpty = true)
        bs.getInt(); bs.getInt(); bs.getInt(); bs.getInt(); bs.getInt()
        bs.getInt(); bs.getBool()

        // C4 base fields
        assertEquals(10, bs.getInt())   // ammoDef
        assertEquals(20, bs.getInt())   // ownerId
        bs.getFloat(BITPACK_VEHICLE_VELOCITY)
        bs.getFloat(BITPACK_VEHICLE_VELOCITY)
        bs.getFloat(BITPACK_VEHICLE_VELOCITY)
        assertTrue(bs.getBool())   // stuck = true

        // stuck-specific fields
        assertEquals(10f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)
        assertEquals(20f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals(5f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        assertTrue(bs.getBool())   // stuckMct
        assertTrue(bs.getBool())   // stuckToObject
        assertEquals(999, bs.getInt())   // stuckObjectId

        // stuckToObject=true fields
        assertEquals(0.1f, bs.getFloat(BITPACK_VEHICLE_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(0.2f, bs.getFloat(BITPACK_VEHICLE_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(0.3f, bs.getFloat(BITPACK_VEHICLE_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(7, bs.getInt())   // stuckBone

        assertFalse(bs.getBool())  // stuckStaticAnim = false
    }
}

private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
    assert(kotlin.math.abs(expected - actual) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but got $actual"
    }
}
