package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for SoldierGameObj encoding.
 *
 * Wire format reference: docs/soldier-packet-format.md
 *
 * Encoder precision used by these tests (default ±500 fallback, all axes 13 bits):
 * - WORLD_POSITION_X/Y/Z: (-500, 500, 0.2) → 13 bits each
 * - HEALTH/SHIELD_STRENGTH: (0, 2000) → 11 bits each
 * - SHIELD_TYPE: (0, 8) → 4 bits
 * - HUMAN_STATE: (0, 12) → 4 bits
 * - HUMAN_SUB_STATE: (0, 511) → 9 bits
 * - CONTINUOUS_BOOLEAN_BITS: 4 bits (raw)
 * - ANALOG_VALUES: (-1, 1, 0.01) → 8 bits
 *
 * Map-specific precision differs (e.g. C&C_Under: X=13, Y=12, Z=10),
 * which changes total bit counts. These tests use the default precision.
 */
class SoldierGameObjTest {

    companion object {
        // Bit widths for the default ±500 encoders
        private const val POS_X_BITS = 13
        private const val POS_Y_BITS = 13
        private const val POS_Z_BITS = 13
        private const val HEALTH_BITS = 11
        private const val SHIELD_BITS = 11
        private const val SHIELD_TYPE_BITS = 4
        private const val HUMAN_STATE_BITS = 4
        private const val HUMAN_SUB_STATE_BITS = 9
        private const val CONT_BOOL_BITS = 4
        private const val ANALOG_BITS = 8

        // Expected section sizes with default ±500 encoders
        private const val HEADER_BITS = 73 // networkId(32) + dirtyBits(8) + isDeletePending(1) + networkClassId(32)
        private const val CREATION_BITS = POS_X_BITS + POS_Y_BITS + POS_Z_BITS + 32 + 32 // 103 (exportCreation only; factory prepPacket adds +32)
        private const val FREQUENT_BITS = 1 + 1 + POS_X_BITS + POS_Y_BITS + POS_Z_BITS +
                HUMAN_STATE_BITS + HUMAN_SUB_STATE_BITS + 1 + // SoldierGameObj part
                1 + // PhysicalGameObj on_host_bone
                POS_X_BITS + POS_Y_BITS + POS_Z_BITS + // ArmedGameObj targeting
                CONT_BOOL_BITS + 4 * ANALOG_BITS // SmartGameObj control
        // = 131

        // Production soldier constants matching GameServer.spawnSoldier exactly.
        // NOD: model "c_ag_nod_mg" (11 chars), GDI: model "c_ag_gdi_mg" (11 chars)
        // Both use animName "S_A_HUMAN.H_A_AINM" (18 chars).
        const val PROD_ANIM_NAME = "S_A_HUMAN.H_A_AINM"  // 18 chars
        const val NOD_MODEL_NAME = "c_ag_nod_mg"           // 11 chars
        const val GDI_MODEL_NAME = "c_ag_gdi_mg"           // 11 chars
        // Rare bits for any production soldier (both models are the same length):
        //   model (16 + 11*8 = 104) + anim (16 + 18*8 = 160) + 6×int(192) + hud_pokable(1) + defId(32) = 489
        const val PROD_RARE_BITS = 489
        // Full creation packet: header(73) + creation(135) + rare(489) + occasional(59) + frequent(131) = 887
        const val PROD_FULL_CREATION_BITS = 887

        @BeforeAll @JvmStatic fun setupEncoders() {
            // Must match GameServer.initEncoders() defaults (fallback when no map LSD extents)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE, 0.0, 12.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_SUB_STATE, 0.0, 511.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_CONTINUOUS_BOOLEAN_BITS, 4)
            EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
        }
    }

    private fun defaultSoldier() = SoldierGameObj(
        definitionId  = 0x04e22811,
        controlOwner  = 1,
        team          = 0,
        modelName     = "s_a_human",
        position      = Vector3(0f, 0f, 5f),
        facing        = 0f,
        health        = 100f,
    )

    // Calculates expected exportRare bit count for a given model name and anim name.
    // Fixed fields: 6×int(192) + hud_pokable(1) + soldier.defId(32) = 225
    // Plus terminated string overhead for model and anim: 16 bits each for the length prefix.
    private fun expectedRareBits(modelName: String, animName: String = ""): Int {
        val modelBits = 16 + modelName.length * 8 // terminated string: len(16) + chars
        val animBits  = 16 + animName.length  * 8 // terminated string: len(16) + chars
        return modelBits + animBits + 6 * 32 + 1 + 32
    }

    // Calculates expected exportOccasional bit count for a given weapon count.
    // Fixed: isDead(1) + health(11) + shieldStr(11) + shieldType(4) + weaponCount(32) = 59
    // Plus: 64 bits per weapon (id + rounds)
    private fun expectedOccasionalBits(realWeaponCount: Int): Int {
        return 1 + HEALTH_BITS + SHIELD_BITS + SHIELD_TYPE_BITS + 32 + realWeaponCount * 64
    }

    @Test
    fun `networkClassId is 1000`() {
        assertEquals(1000, defaultSoldier().networkClassId)
    }

    @Test
    fun `header fields round-trip`() {
        val soldier = defaultSoldier()
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, networkId = 200001)

        assertEquals(200001, bs.getInt())                        // networkId
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)        // dirtyBits = BIT_CREATION
        assertFalse(bs.getBool())                                 // isDeletePending
        assertEquals(1000, bs.getInt())                          // networkClassId
    }

    @Test
    fun `exportCreation fields round-trip`() {
        val defId = 0x04e22811
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = 3,
            team = 1,
            modelName = "s_a_human",
            position = Vector3(10f, 20f, 5f),
        )
        val bs = BitStream()
        soldier.exportCreation(bs)

        // Positions are quantized — read back and accept rounding
        val x = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val y = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val z = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        assertEquals(10f, x, absoluteTolerance = 0.3f)
        assertEquals(20f, y, absoluteTolerance = 0.3f)
        assertEquals( 5f, z, absoluteTolerance = 0.3f)
        bs.getFloat()                               // facing (raw float)
        assertEquals(3, bs.getInt())               // controlOwner
    }

    @Test
    fun `exportCreation is 135 bits with default encoders`() {
        val bs = BitStream()
        defaultSoldier().exportCreation(bs)
        assertEquals(CREATION_BITS, bs.bitWritePosition)
    }

    @Test
    fun `exportRare writes model, team, and definitionId`() {
        val defId = 0x04e22803
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = 2,
            team = 1,   // Nod
            modelName = "s_a_human",
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        soldier.exportRare(bs)

        // PhysicalGameObj fields
        assertEquals("s_a_human", bs.getTerminatedString(permitEmpty = true))  // modelName
        assertEquals("", bs.getTerminatedString(permitEmpty = true))            // anim_name (empty)
        assertEquals(0, bs.getInt())                                             // curr_frame
        assertEquals(0, bs.getInt())                                             // target_frame
        assertEquals(0, bs.getInt())                                             // anim_mode
        assertEquals(0, bs.getInt())                                             // host_model_id
        assertEquals(0, bs.getInt())                                             // host_bone
        // playerType = team (SmartGameObj overrides Get_Player_Type)
        assertEquals(1, bs.getInt())                                             // team (Nod)
        assertFalse(bs.getBool())                                                // hud_pokable
        // SoldierGameObj appends definitionId
        assertEquals(defId, bs.getInt())                                         // soldier definitionId
    }

    @Test
    fun `exportRare is 329 bits for model s_a_human with empty anim`() {
        // modelName "s_a_human" = 16+72=88 bits, animName "" = 16 bits,
        // 6 ints = 192 bits, hud_pokable = 1 bit, soldier.defId = 32 bits
        // Total: 88 + 16 + 192 + 1 + 32 = 329
        val bs = BitStream()
        defaultSoldier().exportRare(bs)
        assertEquals(expectedRareBits("s_a_human", ""), bs.bitWritePosition)
        assertEquals(329, bs.bitWritePosition)
    }

    @Test
    fun `exportRare sends non-empty animName`() {
        // animName "S_A_HUMAN.H_A_AINM" = 18 chars → 16 + 18*8 = 160 bits
        // vs empty animName = 16 bits → +144 bits compared to empty anim
        val anim = "S_A_HUMAN.H_A_AINM"
        val soldier = SoldierGameObj(
            definitionId = 0x04e22811,
            controlOwner = 1,
            team = 0,
            modelName = "c_ag_nod_mg",
            animName = anim,
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        soldier.exportRare(bs)

        assertEquals("c_ag_nod_mg", bs.getTerminatedString(permitEmpty = true))  // modelName
        assertEquals(anim, bs.getTerminatedString(permitEmpty = true))             // animName
        // Verify bit count: model "c_ag_nod_mg"=11 chars → 16+88=104, anim=18 chars → 16+144=160
        assertEquals(expectedRareBits("c_ag_nod_mg", anim), bs.bitWritePosition)
        assertEquals(104 + 160 + 192 + 1 + 32, bs.bitWritePosition)  // = 489
    }

    @Test
    fun `exportOccasional writes health and zero weapons`() {
        val bs = BitStream()
        defaultSoldier().exportOccasional(bs)

        // DamageableGameObj (DefenseObject.Export)
        assertFalse(bs.getBool())                                    // isDead (health != 0)
        val health = bs.getFloat(BITPACK_HEALTH)
        assertEquals(100f, health, absoluteTolerance = 1.0f)        // health ≈ 100
        val shield = bs.getFloat(BITPACK_SHIELD_STRENGTH)
        assertEquals(0f, shield, absoluteTolerance = 1.0f)          // no shield
        bs.getInt(BITPACK_SHIELD_TYPE)                               // shieldType

        // SoldierGameObj: WeaponBag.Export_Weapon_List
        // C++ writes (WeaponList.Count()-1) = number of REAL weapons to follow.
        // Value 0 means "no real weapons follow" (only slot 0 fists exist).
        // Client reads this directly as the loop count (no +1 adjustment).
        assertEquals(0, bs.getInt())                                 // weaponCount = 0 real weapons
    }

    @Test
    fun `exportOccasional is 59 bits with zero weapons`() {
        // isDead(1) + health(11) + shieldStr(11) + shieldType(4) + weaponCount(32) = 59
        val bs = BitStream()
        defaultSoldier().exportOccasional(bs)
        assertEquals(expectedOccasionalBits(0), bs.bitWritePosition)
        assertEquals(59, bs.bitWritePosition)
    }

    @Test
    fun `exportFrequent writes position and zero control`() {
        val bs = BitStream()
        defaultSoldier().exportFrequent(bs)

        // SoldierGameObj fields
        assertFalse(bs.getBool())                                    // in_vehicle
        assertFalse(bs.getBool())                                    // has_weapon (no weapon → no weapon data)
        val x = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val y = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val z = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        assertEquals(0f, x, absoluteTolerance = 0.3f)
        assertEquals(0f, y, absoluteTolerance = 0.3f)
        assertEquals(5f, z, absoluteTolerance = 0.3f)
        assertEquals(0, bs.getInt(BITPACK_HUMAN_STATE))              // UPRIGHT
        assertEquals(0, bs.getInt(BITPACK_HUMAN_SUB_STATE))
        // state != AIRBORNE → no velocity; state != TRANSITION/ANIMATION → no anim name
        assertFalse(bs.getBool())                                    // is_special_damage

        // PhysicalGameObj
        assertFalse(bs.getBool())                                    // on_host_bone

        // ArmedGameObj — targeting position
        bs.getFloat(BITPACK_WORLD_POSITION_X)                       // targeting.x
        bs.getFloat(BITPACK_WORLD_POSITION_Y)                       // targeting.y
        bs.getFloat(BITPACK_WORLD_POSITION_Z)                       // targeting.z

        // SmartGameObj — Export_Control_Sc (#if 01 branch)
        assertEquals(0, bs.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toInt()) // ContinuousBooleanBits
        // Analog values: 0.0 encodes to center of range (-1,1) = raw 128 → decodes back to ≈ 0.0
        val fwd = bs.getFloat(BITPACK_ANALOG_VALUES)
        val left = bs.getFloat(BITPACK_ANALOG_VALUES)
        val up = bs.getFloat(BITPACK_ANALOG_VALUES)
        val turn = bs.getFloat(BITPACK_ANALOG_VALUES)
        assertEquals(0f, fwd, absoluteTolerance = 0.02f)
        assertEquals(0f, left, absoluteTolerance = 0.02f)
        assertEquals(0f, up, absoluteTolerance = 0.02f)
        assertEquals(0f, turn, absoluteTolerance = 0.02f)
    }

    @Test
    fun `exportFrequent is 131 bits with default encoders`() {
        // SoldierGameObj: in_vehicle(1) + has_weapon(1) + pos(13+13+13) + humanState(4) +
        //   humanSubState(9) + is_special_damage(1) = 55
        // PhysicalGameObj: on_host_bone(1)
        // ArmedGameObj: targeting(13+13+13) = 39
        // SmartGameObj: continuousBool(4) + 4×analog(8) = 36
        // Total: 55 + 1 + 39 + 36 = 131
        val bs = BitStream()
        defaultSoldier().exportFrequent(bs)
        assertEquals(FREQUENT_BITS, bs.bitWritePosition)
        assertEquals(131, bs.bitWritePosition)
    }

    @Test
    fun `exportOccasional writes one weapon`() {
        val soldier = SoldierGameObj(
            definitionId = 0x04e22811,
            controlOwner = 1,
            team = 0,
            modelName = "s_a_human",
            position = Vector3(0f, 0f, 5f),
            weapons = mutableListOf(WeaponEntry(definitionId = 12345, totalRounds = 100)),
        )
        val bs = BitStream()
        soldier.exportOccasional(bs)

        // DamageableGameObj
        bs.getBool()                                                     // isDead
        bs.getFloat(BITPACK_HEALTH)                                      // health
        bs.getFloat(BITPACK_SHIELD_STRENGTH)                             // shield
        bs.getInt(BITPACK_SHIELD_TYPE)                                   // shieldType

        // WeaponBag: 1 real weapon
        assertEquals(1, bs.getInt())                                     // weaponCount
        assertEquals(12345, bs.getInt())                                 // weapon defId
        assertEquals(100, bs.getInt())                                   // totalRounds
        assertEquals(expectedOccasionalBits(1), bs.bitWritePosition)     // 59 + 64 = 123
    }

    @Test
    fun `exportFrequent writes weapon data when has weapon`() {
        val soldier = SoldierGameObj(
            definitionId = 0x04e22811,
            controlOwner = 1,
            team = 0,
            modelName = "s_a_human",
            position = Vector3(0f, 0f, 5f),
            weapons = mutableListOf(WeaponEntry(definitionId = 12345, totalRounds = 100)),
        )
        val bs = BitStream()
        soldier.exportFrequent(bs)

        assertFalse(bs.getBool())                                        // in_vehicle
        assertEquals(true, bs.getBool())                                 // has_weapon
        assertEquals(12345, bs.getInt())                                 // weapon defId
        assertEquals(100, bs.getInt())                                   // totalRounds
        // Rest of frequent fields follow...
        bs.getFloat(BITPACK_WORLD_POSITION_X)
        bs.getFloat(BITPACK_WORLD_POSITION_Y)
        bs.getFloat(BITPACK_WORLD_POSITION_Z)

        // has_weapon adds 64 bits to FREQUENT_BITS
        assertEquals(FREQUENT_BITS + 64, bs.bitWritePosition)
    }

    @Test
    fun `full creation packet bit count with default encoders`() {
        // Header(73) + Creation(135) + Rare(329) + Occasional(59) + Frequent(131) = 727
        val expected = HEADER_BITS + 32 + CREATION_BITS + expectedRareBits("s_a_human") +
                expectedOccasionalBits(0) + FREQUENT_BITS
        assertEquals(727, expected)

        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, defaultSoldier(), networkId = 200001)
        assertEquals(expected, bs.bitWritePosition)
    }

    @Test
    fun `full creation packet round-trip preserves all fields`() {
        val defId = 0x04e22811
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = 42,
            team = 1,
            modelName = "s_a_human",
            position = Vector3(10f, -20f, 5f),
            facing = 1.5f,
            health = 200f,
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, networkId = 0x00030D41)

        // Header
        assertEquals(0x00030D41, bs.getInt())                       // networkId
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)           // dirtyBits
        assertFalse(bs.getBool())                                    // isDeletePending
        assertEquals(1000, bs.getInt())                             // networkClassId

        // exportCreation
        assertEquals(defId, bs.getInt())                            // definitionId
        assertEquals(10f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)
        assertEquals(-20f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals(5f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        assertEquals(1.5f, bs.getFloat(), absoluteTolerance = 0.001f) // facing (raw float)
        assertEquals(42, bs.getInt())                               // controlOwner

        // exportRare
        assertEquals("s_a_human", bs.getTerminatedString(permitEmpty = true))
        assertEquals("", bs.getTerminatedString(permitEmpty = true))
        repeat(5) { bs.getInt() }                                    // frame/mode/host fields
        assertEquals(1, bs.getInt())                                // playerType = team
        assertFalse(bs.getBool())                                    // hud_pokable
        assertEquals(defId, bs.getInt())                            // soldier.definitionId

        // exportOccasional
        assertFalse(bs.getBool())                                    // isDead
        assertEquals(200f, bs.getFloat(BITPACK_HEALTH), absoluteTolerance = 1.0f)
        bs.getFloat(BITPACK_SHIELD_STRENGTH)
        bs.getInt(BITPACK_SHIELD_TYPE)
        assertEquals(0, bs.getInt())                                // 0 real weapons

        // exportFrequent
        assertFalse(bs.getBool())                                    // in_vehicle
        assertFalse(bs.getBool())                                    // has_weapon
        assertEquals(10f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)
        assertEquals(-20f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals(5f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        assertEquals(0, bs.getInt(BITPACK_HUMAN_STATE))
        assertEquals(0, bs.getInt(BITPACK_HUMAN_SUB_STATE))
        assertFalse(bs.getBool())                                    // is_special_damage
        assertFalse(bs.getBool())                                    // on_host_bone
        bs.getFloat(BITPACK_WORLD_POSITION_X)                       // targeting
        bs.getFloat(BITPACK_WORLD_POSITION_Y)
        bs.getFloat(BITPACK_WORLD_POSITION_Z)
        bs.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS)                  // control booleans
        bs.getFloat(BITPACK_ANALOG_VALUES)                           // 4 analog values
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)

        // All bits consumed
        assertEquals(bs.bitWritePosition, bs.bitReadPosition)
    }

    // ---- Production soldier tests (matching spawnSoldier in GameServer) ----

    @Test
    fun `animName defaults to empty string`() {
        // SoldierGameObj constructor has animName = "" default.
        // An empty animName is NOT used in production but must compile and write correctly.
        val soldier = SoldierGameObj(
            definitionId = 0x04e22811,
            controlOwner = 1,
            team = 0,
            modelName = "s_a_human",
            // animName intentionally omitted → defaults to ""
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        soldier.exportRare(bs)
        bs.getTerminatedString(permitEmpty = true)                      // modelName
        assertEquals("", bs.getTerminatedString(permitEmpty = true))   // animName must be ""
    }

    @Test
    fun `production NOD soldier exportRare has correct model, animName, and bit count`() {
        val defId = 0x04e22811
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = 1,
            team = 0,
            modelName = NOD_MODEL_NAME,
            animName = PROD_ANIM_NAME,
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        soldier.exportRare(bs)

        assertEquals(NOD_MODEL_NAME, bs.getTerminatedString(permitEmpty = true))  // modelName
        assertEquals(PROD_ANIM_NAME, bs.getTerminatedString(permitEmpty = true))  // animName
        repeat(5) { bs.getInt() }                                                   // frame/mode/host fields
        assertEquals(0, bs.getInt())                                               // playerType = NOD (0)
        assertFalse(bs.getBool())                                                   // hud_pokable
        assertEquals(defId, bs.getInt())                                           // soldier definitionId

        assertEquals(PROD_RARE_BITS, bs.bitWritePosition)
        // All bits consumed
        assertEquals(bs.bitWritePosition, bs.bitReadPosition)
    }

    @Test
    fun `production GDI soldier exportRare has correct model, animName, and bit count`() {
        val defId = 0x04e22803
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = 2,
            team = 1,
            modelName = GDI_MODEL_NAME,
            animName = PROD_ANIM_NAME,
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        soldier.exportRare(bs)

        assertEquals(GDI_MODEL_NAME, bs.getTerminatedString(permitEmpty = true))  // modelName
        assertEquals(PROD_ANIM_NAME, bs.getTerminatedString(permitEmpty = true))  // animName
        repeat(5) { bs.getInt() }                                                   // frame/mode/host fields
        assertEquals(1, bs.getInt())                                               // playerType = GDI (1)
        assertFalse(bs.getBool())                                                   // hud_pokable
        assertEquals(defId, bs.getInt())                                           // soldier definitionId

        assertEquals(PROD_RARE_BITS, bs.bitWritePosition)
        assertEquals(bs.bitWritePosition, bs.bitReadPosition)
    }

    @Test
    fun `full creation packet bit count for production NOD soldier`() {
        // NOD production soldier: model "c_ag_nod_mg" (11 chars), anim "S_A_HUMAN.H_A_AINM" (18 chars)
        // Header(73) + Creation(135) + Rare(489) + Occasional(59) + Frequent(131) = 887
        assertEquals(887, PROD_FULL_CREATION_BITS)

        val soldier = SoldierGameObj(
            definitionId = 0x04e22811,
            controlOwner = 1,
            team = 0,
            modelName = NOD_MODEL_NAME,
            animName = PROD_ANIM_NAME,
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, networkId = 200001)
        assertEquals(PROD_FULL_CREATION_BITS, bs.bitWritePosition)
    }

    @Test
    fun `full creation packet bit count for production GDI soldier`() {
        // GDI production soldier: model "c_ag_gdi_mg" (11 chars, same length as NOD model),
        // anim "S_A_HUMAN.H_A_AINM" (18 chars) → same total bit count as NOD soldier.
        val soldier = SoldierGameObj(
            definitionId = 0x04e22803,
            controlOwner = 2,
            team = 1,
            modelName = GDI_MODEL_NAME,
            animName = PROD_ANIM_NAME,
            position = Vector3(0f, 0f, 5f),
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, networkId = 200002)
        assertEquals(PROD_FULL_CREATION_BITS, bs.bitWritePosition)
    }

    @Test
    fun `production soldier round-trip preserves animName and model`() {
        // Full round-trip for the exact soldier that spawnSoldier creates for a NOD player.
        val defId = 0x04e22811
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner = 7,
            team = 0,
            modelName = NOD_MODEL_NAME,
            animName = PROD_ANIM_NAME,
            position = Vector3(5f, 10f, 3f),
            health = 100f,                                           // must be > 0 so isDead=false
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, networkId = 1_500_000_001)

        // Header
        assertEquals(1_500_000_001, bs.getInt())
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)
        assertFalse(bs.getBool())
        assertEquals(1000, bs.getInt())                              // SoldierGameObj networkClassId

        // exportCreation
        assertEquals(defId, bs.getInt())
        assertEquals(5f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)
        assertEquals(10f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals(3f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        bs.getFloat()                                                // facing (raw)
        assertEquals(7, bs.getInt())                                // controlOwner

        // exportRare — key assertions: model and animName must survive round-trip
        assertEquals(NOD_MODEL_NAME, bs.getTerminatedString(permitEmpty = true))
        assertEquals(PROD_ANIM_NAME, bs.getTerminatedString(permitEmpty = true))
        repeat(5) { bs.getInt() }                                    // frame/mode/host fields
        assertEquals(0, bs.getInt())                                // playerType = NOD (0)
        assertFalse(bs.getBool())                                    // hud_pokable
        assertEquals(defId, bs.getInt())                            // soldier.definitionId

        // exportOccasional
        assertFalse(bs.getBool())                                    // isDead (health > 0 → false)
        bs.getFloat(BITPACK_HEALTH)
        bs.getFloat(BITPACK_SHIELD_STRENGTH)
        bs.getInt(BITPACK_SHIELD_TYPE)
        assertEquals(0, bs.getInt())                                // 0 real weapons

        // exportFrequent
        assertFalse(bs.getBool())                                    // in_vehicle
        assertFalse(bs.getBool())                                    // has_weapon
        bs.getFloat(BITPACK_WORLD_POSITION_X)
        bs.getFloat(BITPACK_WORLD_POSITION_Y)
        bs.getFloat(BITPACK_WORLD_POSITION_Z)
        bs.getInt(BITPACK_HUMAN_STATE)
        bs.getInt(BITPACK_HUMAN_SUB_STATE)
        assertFalse(bs.getBool())                                    // is_special_damage
        assertFalse(bs.getBool())                                    // on_host_bone
        bs.getFloat(BITPACK_WORLD_POSITION_X)                       // targeting x/y/z
        bs.getFloat(BITPACK_WORLD_POSITION_Y)
        bs.getFloat(BITPACK_WORLD_POSITION_Z)
        bs.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)

        // All bits consumed — production packet is exactly PROD_FULL_CREATION_BITS wide
        assertEquals(PROD_FULL_CREATION_BITS, bs.bitReadPosition)
        assertEquals(bs.bitWritePosition, bs.bitReadPosition)
    }
}

private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
    assert(kotlin.math.abs(expected - actual) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but got $actual"
    }
}
