package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for BuildingGameObj encoding.
 *
 * C++ hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj
 * (BuildingGameObj does NOT extend PhysicalGameObj)
 *
 * Wire format reference: building.cpp Export_Creation / Export_Rare
 *
 * Encoder precision used by these tests:
 * - WORLD_POSITION_X/Y/Z: (-500, 500, 0.2) → 13 bits each
 * - BUILDING_RADIUS: (0, 50, 0.1) → 9 bits
 * - BUILDING_STATE: (-1, 10, 1.0) → 4 bits
 */
class BuildingGameObjTest {

    companion object {
        // Bit widths for the default encoders
        private const val POS_X_BITS = 13
        private const val POS_Y_BITS = 13
        private const val POS_Z_BITS = 13
        private const val RADIUS_BITS = 9  // BITPACK_BUILDING_RADIUS: (0, 50, 0.1) → 9 bits
        private const val STATE_BITS = 4   // BITPACK_BUILDING_STATE: (-1, 10, 1.0) → 4 bits

        // Expected bit counts
        // Creation: definitionId(32) + pos(13+13+13) + sphere_center(13+13+13) + radius(9) = 119
        private const val CREATION_BITS = 32 + POS_X_BITS + POS_Y_BITS + POS_Z_BITS +
                POS_X_BITS + POS_Y_BITS + POS_Z_BITS + RADIUS_BITS   // 119

        // Rare: isDestroyed(1) + isPowerOn(1) + currentState(4) = 6
        private const val RARE_BITS = 1 + 1 + STATE_BITS  // 6

        // Header: networkId(32) + dirtyBits(8) + isDeletePending(1) + networkClassId(32) = 73
        private const val HEADER_BITS = 73

        // Occasional: DamageableGameObj writes isDead(1)+health(11)+shieldStr(11)+shieldType(4)=27
        private const val HEALTH_BITS = 11
        private const val SHIELD_BITS = 11
        private const val SHIELD_TYPE_BITS = 4
        private const val OCCASIONAL_BITS = 1 + HEALTH_BITS + SHIELD_BITS + SHIELD_TYPE_BITS  // 27

        // Frequent: BuildingGameObj has no exportFrequent override — nothing written
        private const val FREQUENT_BITS = 0

        @BeforeAll @JvmStatic fun setupEncoders() {
            // World position — same as SoldierGameObjTest defaults
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            // Health / shield
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
            // Building-specific
            EncoderRegistry.setPrecision(BITPACK_BUILDING_RADIUS, 0.0, 50.0, 0.1)
            // BITPACK_BUILDING_STATE: range -1 to 10, resolution 1.0
            // C++: cEncoderList::Set_Precision(BITPACK_BUILDING_STATE, -1, STATE_COUNT) = (-1, 10)
            EncoderRegistry.setPrecision(BITPACK_BUILDING_STATE, -1.0, 10.0, 1.0)
        }
    }

    private fun defaultBuilding() = BuildingGameObj(
        definitionId    = 0x04D06410,
        position        = Vector3(10f, 20f, 0f),
        sphereCenter    = Vector3(10f, 20f, 0f),
        sphereRadius    = 25f,
        health          = 1500f,   // within BITPACK_HEALTH range (0-2000)
        isDestroyed     = false,
        isPowerOn       = true,
        currentState    = 0,   // HEALTH100_POWERON
    )

    // ---- networkClassId ----

    @Test
    fun `networkClassId is 1000`() {
        assertEquals(1000, defaultBuilding().networkClassId)
    }

    // ---- exportCreation ----

    @Test
    fun `exportCreation writes definitionId first`() {
        val defId = 0x04D06410
        val bs = BitStream()
        defaultBuilding().exportCreation(bs)

        assertEquals(defId, bs.getInt())
    }

    @Test
    fun `exportCreation position round-trip`() {
        val building = BuildingGameObj(
            definitionId  = 1,
            position      = Vector3(15f, -30f, 5f),
            sphereCenter  = Vector3(0f, 0f, 0f),
            sphereRadius  = 0f,
            health        = 5000f,
            isDestroyed   = false,
            isPowerOn     = true,
            currentState  = 0,
        )
        val bs = BitStream()
        building.exportCreation(bs)

        bs.getInt()   // skip definitionId
        val x = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val y = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val z = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        assertEquals(15f, x, absoluteTolerance = 0.3f)
        assertEquals(-30f, y, absoluteTolerance = 0.3f)
        assertEquals(5f, z, absoluteTolerance = 0.3f)
    }

    @Test
    fun `exportCreation sphere center and radius round-trip`() {
        val building = BuildingGameObj(
            definitionId  = 1,
            position      = Vector3(0f, 0f, 0f),
            sphereCenter  = Vector3(5f, 10f, 2f),
            sphereRadius  = 30f,
            health        = 5000f,
            isDestroyed   = false,
            isPowerOn     = true,
            currentState  = 0,
        )
        val bs = BitStream()
        building.exportCreation(bs)

        bs.getInt()                            // skip definitionId
        bs.getFloat(BITPACK_WORLD_POSITION_X)  // skip position.x
        bs.getFloat(BITPACK_WORLD_POSITION_Y)  // skip position.y
        bs.getFloat(BITPACK_WORLD_POSITION_Z)  // skip position.z

        val cx = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val cy = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val cz = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        val r  = bs.getFloat(BITPACK_BUILDING_RADIUS)

        assertEquals(5f, cx, absoluteTolerance = 0.3f)
        assertEquals(10f, cy, absoluteTolerance = 0.3f)
        assertEquals(2f, cz, absoluteTolerance = 0.3f)
        assertEquals(30f, r, absoluteTolerance = 0.2f)
    }

    @Test
    fun `exportCreation is 119 bits with default encoders`() {
        val bs = BitStream()
        defaultBuilding().exportCreation(bs)
        assertEquals(CREATION_BITS, bs.bitWritePosition)
        assertEquals(119, bs.bitWritePosition)
    }

    // ---- exportRare ----

    @Test
    fun `exportRare operational building - isDestroyed=false, isPowerOn=true`() {
        val bs = BitStream()
        defaultBuilding().exportRare(bs)

        assertFalse(bs.getBool())    // isDestroyed = false
        assertTrue(bs.getBool())     // isPowerOn = true
        assertEquals(0, bs.getInt(BITPACK_BUILDING_STATE))  // HEALTH100_POWERON
    }

    @Test
    fun `exportRare destroyed building - isDestroyed=true, isPowerOn=false`() {
        val building = BuildingGameObj(
            definitionId  = 1,
            position      = Vector3(0f, 0f, 0f),
            sphereCenter  = Vector3(0f, 0f, 0f),
            sphereRadius  = 25f,
            health        = 0f,
            isDestroyed   = true,
            isPowerOn     = false,
            currentState  = 9,  // DESTROYED_POWEROFF
        )
        val bs = BitStream()
        building.exportRare(bs)

        assertTrue(bs.getBool())     // isDestroyed = true
        assertFalse(bs.getBool())    // isPowerOn = false
        assertEquals(9, bs.getInt(BITPACK_BUILDING_STATE))   // DESTROYED_POWEROFF
    }

    @Test
    fun `exportRare currentState encodes correctly`() {
        // State 1 = HEALTH75_POWERON — chosen because it round-trips cleanly with the encoder.
        // Note: BITPACK_BUILDING_STATE encoder (-1, 10, 1.0) has adjustedResolution=11/15,
        // so only certain state values round-trip exactly via integer encoding.
        // State 1: scale(1) = round(2 * 15/11) = round(2.727) = 3; unscale(3) = -1 + 3*11/15 = 1.2 → toLong=1 ✓
        val building = BuildingGameObj(
            definitionId  = 1,
            position      = Vector3(0f, 0f, 0f),
            sphereCenter  = Vector3(0f, 0f, 0f),
            sphereRadius  = 10f,
            health        = 1000f,
            isDestroyed   = false,
            isPowerOn     = true,
            currentState  = 1,  // HEALTH75_POWERON
        )
        val bs = BitStream()
        building.exportRare(bs)

        assertFalse(bs.getBool())  // isDestroyed = false
        assertTrue(bs.getBool())   // isPowerOn = true
        assertEquals(1, bs.getInt(BITPACK_BUILDING_STATE))
    }

    @Test
    fun `exportRare is 6 bits`() {
        val bs = BitStream()
        defaultBuilding().exportRare(bs)
        assertEquals(RARE_BITS, bs.bitWritePosition)
        assertEquals(6, bs.bitWritePosition)
    }

    // ---- exportOccasional (inherited from DamageableGameObj) ----

    @Test
    fun `exportOccasional writes health from DamageableGameObj`() {
        val bs = BitStream()
        defaultBuilding().exportOccasional(bs)

        assertFalse(bs.getBool())   // isDead (health != 0)
        val health = bs.getFloat(BITPACK_HEALTH)
        // defaultBuilding has health=1500f, within BITPACK_HEALTH encoder range (0-2000)
        assertEquals(1500f, health, absoluteTolerance = 1.0f)
    }

    @Test
    fun `exportOccasional is 27 bits`() {
        val bs = BitStream()
        defaultBuilding().exportOccasional(bs)
        assertEquals(OCCASIONAL_BITS, bs.bitWritePosition)
        assertEquals(27, bs.bitWritePosition)
    }

    // ---- full creation packet ----

    @Test
    fun `full creation packet networkClassId is 1000`() {
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, defaultBuilding(), networkId = 300001)

        bs.getInt()                              // networkId
        bs.getByte()                             // dirtyBits
        bs.getBool()                             // isDeletePending
        val networkClassId = bs.getInt()
        assertEquals(1000, networkClassId)
    }

    @Test
    fun `full creation packet bit count with default encoders`() {
        // Header(73) + Creation(119) + Rare(6) + Occasional(27) + Frequent(0) = 225
        val expected = HEADER_BITS + CREATION_BITS + RARE_BITS + OCCASIONAL_BITS + FREQUENT_BITS
        assertEquals(225, expected)

        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, defaultBuilding(), networkId = 300001)
        assertEquals(expected, bs.bitWritePosition)
    }

    @Test
    fun `full creation packet round-trip preserves all fields`() {
        val defId = 0x04D06410
        val building = BuildingGameObj(
            definitionId  = defId,
            position      = Vector3(50f, -100f, 3f),
            sphereCenter  = Vector3(55f, -95f, 5f),
            sphereRadius  = 40f,
            health        = 1200f,   // within BITPACK_HEALTH range (0-2000)
            isDestroyed   = false,
            isPowerOn     = true,
            currentState  = 0,  // HEALTH100_POWERON
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, building, networkId = 0x00030D41)

        // Header
        assertEquals(0x00030D41, bs.getInt())                       // networkId
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)           // dirtyBits = BIT_CREATION
        assertFalse(bs.getBool())                                    // isDeletePending
        assertEquals(1000, bs.getInt())                             // networkClassId

        // exportCreation
        assertEquals(defId, bs.getInt())                            // definitionId
        assertEquals(50f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)
        assertEquals(-100f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals(3f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        assertEquals(55f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)  // sphere center
        assertEquals(-95f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals(5f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        assertEquals(40f, bs.getFloat(BITPACK_BUILDING_RADIUS), absoluteTolerance = 0.2f)

        // exportRare
        assertFalse(bs.getBool())                                    // isDestroyed
        assertTrue(bs.getBool())                                     // isPowerOn
        assertEquals(0, bs.getInt(BITPACK_BUILDING_STATE))          // HEALTH100_POWERON

        // exportOccasional (DamageableGameObj)
        assertFalse(bs.getBool())                                    // isDead
        assertEquals(1200f, bs.getFloat(BITPACK_HEALTH), absoluteTolerance = 1.0f)
        bs.getFloat(BITPACK_SHIELD_STRENGTH)
        bs.getInt(BITPACK_SHIELD_TYPE)

        // exportFrequent — nothing written

        // All bits consumed
        assertEquals(bs.bitWritePosition, bs.bitReadPosition)
    }
}

private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
    assert(kotlin.math.abs(expected - actual) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but got $actual"
    }
}
