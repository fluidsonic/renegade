package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.defs.combat.BeaconGameObjDef
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    private fun makeBeacon(armTime: Float = 10f, detonateTime: Float = 30f): BeaconGameObj {
        val def = BeaconGameObjDef(
            name = "Test Beacon", id = 1u, chunkId = BeaconGameObjDef.CHUNK_ID,
            armTime = armTime, detonateTime = detonateTime,
            explosionDefId = 0,
        )
        val beacon = BeaconGameObj(
            definitionId = 1,
            position = Vector3(0f, 0f, 0f),
            initialState = BeaconGameObj.STATE_ARMING,
        )
        beacon.beaconDef = def
        beacon.armTimer = armTime
        return beacon
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
            initialState = 3,
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

    @Test
    fun `beacon transitions from ARMING to ARMED after armTime`() {
        val beacon = makeBeacon(armTime = 5f, detonateTime = 30f)
        assertEquals(BeaconGameObj.STATE_ARMING, beacon.state)

        beacon.thinkInternal(3f)
        assertEquals(BeaconGameObj.STATE_ARMING, beacon.state)  // not yet

        beacon.thinkInternal(3f)  // total 6f > 5f arm time
        assertEquals(BeaconGameObj.STATE_ARMED, beacon.state)
    }

    @Test
    fun `detonateTimer carries overshoot from arming`() {
        val beacon = makeBeacon(armTime = 5f, detonateTime = 30f)
        beacon.thinkInternal(6f)  // 1s overshoot
        assertEquals(BeaconGameObj.STATE_ARMED, beacon.state)
        // detonateTimer = 30 + (armTimer after subtraction) = 30 + (-1) = 29
        assertEquals(29f, beacon.detonateTimer)
    }

    @Test
    fun `exportRare serialises state and ownerId`() {
        val beacon = BeaconGameObj(
            definitionId = 1,
            position = Vector3(0f, 0f, 0f),
            initialState = BeaconGameObj.STATE_ARMED,
        )
        beacon.ownerId = 42
        val bs = BitStream()
        beacon.exportRare(bs)
        assertTrue(bs.bitWritePosition > 0)
    }
}
