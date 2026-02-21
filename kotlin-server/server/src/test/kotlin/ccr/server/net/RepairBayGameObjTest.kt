package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RepairBayGameObjTest {

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
    }

    @Test fun `networkClassId is 1000`() {
        val obj = RepairBayGameObj(
            definitionId = 900,
            position     = ZERO,
            sphereCenter = ZERO,
            sphereRadius = 15f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `exportCreation round-trip repair zone fields`() {
        val center = Vector3(5f, 10f, 2f)
        val extent = Vector3(8f, 8f, 3f)
        val facing = 1.5708f  // ~90 degrees in radians

        val obj = RepairBayGameObj(
            definitionId      = 901,
            position          = ZERO,
            sphereCenter      = ZERO,
            sphereRadius      = 10f,
            repairZoneCenter  = center,
            repairZoneExtent  = extent,
            repairZoneFacing  = facing,
        )
        val bs = BitStream()
        obj.exportCreation(bs)

        // Skip BuildingGameObj creation fields:
        // definitionId(32) + pos(13+13+13) + sphereCenter(13+13+13) + radius(9) = 119 bits
        bs.getInt()                            // definitionId
        bs.getFloat(BITPACK_WORLD_POSITION_X)  // position.x
        bs.getFloat(BITPACK_WORLD_POSITION_Y)  // position.y
        bs.getFloat(BITPACK_WORLD_POSITION_Z)  // position.z
        bs.getFloat(BITPACK_WORLD_POSITION_X)  // sphereCenter.x
        bs.getFloat(BITPACK_WORLD_POSITION_Y)  // sphereCenter.y
        bs.getFloat(BITPACK_WORLD_POSITION_Z)  // sphereCenter.z
        bs.getFloat(BITPACK_BUILDING_RADIUS)   // sphereRadius

        // RepairBay zone fields
        val cx = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val cy = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val cz = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        val ex = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val ey = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val ez = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        val f  = bs.getFloat()  // raw float, no encoder

        assertEquals(5f, cx, absoluteTolerance = 0.3f)
        assertEquals(10f, cy, absoluteTolerance = 0.3f)
        assertEquals(2f, cz, absoluteTolerance = 0.3f)
        assertEquals(8f, ex, absoluteTolerance = 0.3f)
        assertEquals(8f, ey, absoluteTolerance = 0.3f)
        assertEquals(3f, ez, absoluteTolerance = 0.3f)
        assertEquals(facing, f, absoluteTolerance = 0.001f)
    }
}

private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
    assert(kotlin.math.abs(expected - actual) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but got $actual"
    }
}
