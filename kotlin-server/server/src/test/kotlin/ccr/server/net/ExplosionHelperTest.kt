package ccr.server.net

import ccr.math.Vector3
import ccr.server.defs.ExplosionDefinitionClass
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExplosionHelperTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            // Register all encoders needed for SoldierGameObj/VehicleGameObj creation
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_VEHICLE_VELOCITY, -90.0, 90.0, 0.01)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_HUMAN_STATE, 0.0, 16.0, 1.0)
            ccr.net.bitstream.EncoderRegistry.setPrecision(ccr.net.bitstream.BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
        }
    }

    private fun makeExplosionDef(radius: Float, strength: Float, warhead: Int = 0, scaled: Boolean = true) =
        ExplosionDefinitionClass(name = "TestExplosion", id = 999u, chunkId = 0u,
            damageRadius = radius, damageStrength = strength, damageWarhead = warhead, damageIsScaled = scaled)

    @Test fun `object at explosion center takes full damage when scaled`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val objects = listOf<Any>(soldier)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 50f), 0f, 0f, 0f, objects)
        // scale = 1 - 0/10 = 1.0 → 50 damage → health = 50
        assertApprox(50f, soldier.health)
    }

    @Test fun `object at radius edge takes zero damage when scaled`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(10f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val objects = listOf<Any>(soldier)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 50f), 0f, 0f, 0f, objects)
        // scale = 1 - 10/10 = 0.0 → 0 damage
        assertApprox(100f, soldier.health)
    }

    @Test fun `object beyond radius takes no damage`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(20f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val objects = listOf<Any>(soldier)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 50f), 0f, 0f, 0f, objects)
        assertApprox(100f, soldier.health)
    }

    @Test fun `damageIsScaled false applies flat damage`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(5f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val objects = listOf<Any>(soldier)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 40f, scaled = false), 0f, 0f, 0f, objects)
        // No falloff → full 40 damage
        assertApprox(60f, soldier.health)
    }

    @Test fun `dead object is skipped`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 0f, shieldStrength = 0f)
        val objects = listOf<Any>(soldier)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 50f), 0f, 0f, 0f, objects)
        assertApprox(0f, soldier.health)
    }

    @Test fun `delete-pending object is skipped`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        soldier.setDeletePending()
        val objects = listOf<Any>(soldier)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 50f), 0f, 0f, 0f, objects)
        assertApprox(100f, soldier.health)
    }

    @Test fun `multiple objects damaged proportionally by distance`() {
        val near   = SoldierGameObj(definitionId = 1, position = Vector3(2f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val middle = SoldierGameObj(definitionId = 1, position = Vector3(5f, 0f, 0f), health = 100f, shieldStrength = 0f)
        val objects = listOf<Any>(near, middle)
        ExplosionHelper.applyDamageToObjects(makeExplosionDef(radius = 10f, strength = 100f), 0f, 0f, 0f, objects)
        // near: scale=0.8 → 80 damage → health=20
        // middle: scale=0.5 → 50 damage → health=50
        assertApprox(20f, near.health,   tolerance = 0.1f)
        assertApprox(50f, middle.health, tolerance = 0.1f)
    }
}

private fun assertApprox(expected: Float, actual: Float, tolerance: Float = 0.01f) {
    assert(kotlin.math.abs(expected - actual) <= tolerance) {
        "Expected $expected ± $tolerance but got $actual"
    }
}
