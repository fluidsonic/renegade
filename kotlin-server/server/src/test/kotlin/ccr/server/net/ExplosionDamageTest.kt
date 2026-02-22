package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExplosionDamageTest {

    companion object {
        @BeforeAll @JvmStatic fun setupEncoders() {
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_VELOCITY, -90.0, 90.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE, 0.0, 16.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
        }
    }

    @Test fun `SoldierGameObj applyDamage reduces health and sets BIT_OCCASIONAL dirty`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        soldier.clearObjectDirtyBits()
        soldier.applyDamage(30f)
        assertEquals(70f, soldier.health)
        assertTrue(soldier.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
    }

    @Test fun `VehicleGameObj applyDamage reduces health and sets BIT_OCCASIONAL dirty`() {
        val vehicle = VehicleGameObj(definitionId = 2, position = Vector3(0f, 0f, 0f), health = 200f, shieldStrength = 0f)
        vehicle.clearObjectDirtyBits()
        vehicle.applyDamage(50f)
        assertEquals(150f, vehicle.health)
        assertTrue(vehicle.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
    }

    @Test fun `SoldierGameObj applyDamage zero damage does not dirty`() {
        val soldier = SoldierGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f), health = 100f, shieldStrength = 0f)
        soldier.clearObjectDirtyBits()
        soldier.applyDamage(0f)
        assertEquals(100f, soldier.health)
        assertFalse(soldier.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
    }
}
