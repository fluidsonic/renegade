package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.bitstream.BITPACK_DOOR_STATE
import ccr.net.bitstream.BITPACK_HEALTH
import ccr.net.bitstream.BITPACK_SHIELD_STRENGTH
import ccr.net.bitstream.BITPACK_SHIELD_TYPE
import ccr.net.bitstream.EncoderRegistry
import ccr.physics.static.DoorPhysClass
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticNetworkObjectTest {
    companion object {
        @JvmStatic @BeforeAll fun setupEncoders() {
            // C++: DoorPhysClass::Set_Precision: BITPACK_DOOR_STATE = range (0, STATE_MAX=5), 3 bits.
            // Using raw 3-bit encoding for Kotlin round-trip correctness (0..7 fits door states 0..4).
            EncoderRegistry.setPrecision(BITPACK_DOOR_STATE, 3)
            // Defense object encoders (match SoldierGameObjTest defaults)
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
        }

        private fun makeDoor(): DoorPhysClass = DoorPhysClass(
            definitionId = 1,
            closeDelay = 2f,
            triggerZone1 = null,
            triggerZone2 = null,
            lockCode = 0,
            doorOpensForVehicles = false,
        )
    }

    @Test fun `door rare - closed state`() {
        val bs = BitStream()
        DoorNetworkObject(makeDoor()).exportRare(bs)
        assertEquals(DoorPhysClass.STATE_CLOSED_DOOR, bs.getInt(BITPACK_DOOR_STATE))
    }

    @Test fun `door rare - opening state`() {
        val door = makeDoor()
        door.open() // transitions CLOSED -> OPENING
        val bs = BitStream()
        DoorNetworkObject(door).exportRare(bs)
        assertEquals(DoorPhysClass.STATE_OPENING_DOOR, bs.getInt(BITPACK_DOOR_STATE))
    }

    @Test fun `elevator rare - three state fields in order`() {
        val bs = BitStream()
        ElevatorNetworkObject(state = 1, doorStateTop = 2, doorStateBottom = 0).exportRare(bs)
        assertEquals(1, bs.getInt())
        assertEquals(2, bs.getInt())
        assertEquals(0, bs.getInt())
    }

    @Test fun `elevator rare - all zeros`() {
        val bs = BitStream()
        ElevatorNetworkObject(0, 0, 0).exportRare(bs)
        assertEquals(0, bs.getInt())
        assertEquals(0, bs.getInt())
        assertEquals(0, bs.getInt())
    }

    @Test fun `dsapo rare - animation fields first then defense`() {
        val bs = BitStream()
        DsapoNetworkObject(
            animationMode = 2, loopStart = 0f, loopEnd = 100f,
            currFrame = 25f, targetFrame = 75f,
            health = 75f, shieldStrength = 0f, shieldType = 0,
        ).exportRare(bs)
        assertEquals(2, bs.getInt())           // animationMode
        assertEquals(0f, bs.getFloat())         // loopStart
        assertEquals(100f, bs.getFloat())       // loopEnd
        assert(abs(bs.getFloat() - 25f) < 0.01f) { "currFrame expected ~25f" }
        assert(abs(bs.getFloat() - 75f) < 0.01f) { "targetFrame expected ~75f" }
        assertFalse(bs.getBool())               // isDead (health=75 > 0)
    }

    @Test fun `dsapo rare - dead object`() {
        val bs = BitStream()
        DsapoNetworkObject(health = 0f).exportRare(bs)
        bs.getInt()   // animationMode
        repeat(4) { bs.getFloat() }
        assertTrue(bs.getBool())  // isDead
    }

    @Test fun `setDeletePending is no-op for static types`() {
        val door = DoorNetworkObject(makeDoor())
        val elev = ElevatorNetworkObject(0, 0, 0)
        door.setDeletePending(); elev.setDeletePending()
        assertFalse(door.isDeletePending)
        assertFalse(elev.isDeletePending)
    }

    @Test fun `networkClassId is 0 for all static types`() {
        assertEquals(0, DoorNetworkObject(makeDoor()).networkClassId)
        assertEquals(0, ElevatorNetworkObject(0, 0, 0).networkClassId)
        assertEquals(0, DsapoNetworkObject().networkClassId)
    }
}
