package ccr.server.net

import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.net.bitstream.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for VehicleGameObj encoding.
 *
 * Precision registered in @BeforeAll (vehicle defaults from C++ VehicleGameObj::Set_Precision):
 *   VEHICLE_VELOCITY:         (-90, 90, 0.01)  → 15 bits each
 *   VEHICLE_ANGULAR_VELOCITY: (-20, 20, 0.01)  → 12 bits each
 *   VEHICLE_QUATERNION:       (-1,   1, 0.0005)→ 12 bits each
 *   VEHICLE_LOCK_TIMER:       (0,   16, 0.25)  →  7 bits
 *   WORLD_POSITION_X/Y/Z:    (-500,500, 0.2)  → 13 bits each
 *
 * Frequent bit layout — physics vehicle (CAR/TANK/BIKE/FLYING):
 *   totalRounds(32) + isEngineOn(1) + pos.xyz(13×3) + q.xyzw(12×4) +
 *   vel.xyz(15×3) + angVel.xyz(12×3) + driverIsGunner(1) + SmartGameObj control(4+4×8)
 *   = 32+1+39+48+45+36+1+36 = 238
 *
 * Frequent bit layout — turret:
 *   totalRounds(32) + driverIsGunner(1) + SmartGameObj control(36) = 69
 *
 * Rare bit layout — vehicle with modelName and optional seats:
 *   model + anim + 6×int(192) + hudPokable(1) + hidden(1) + seatCount×32 + vehicleDelivered(1)
 */
class VehicleGameObjTest {

    companion object {
        // Bit widths
        private const val POS_BITS      = 13    // world position each axis
        private const val VEL_BITS      = 15    // vehicle velocity each axis
        private const val ANG_VEL_BITS  = 12    // vehicle angular velocity each axis
        private const val QUAT_BITS     = 12    // vehicle quaternion each component
        private const val LOCK_BITS     = 7     // lock timer
        private const val CONT_BOOL_BITS = 4
        private const val ANALOG_BITS   = 8

        // SmartGameObj::exportFrequent calls super (ArmedGameObj → PhysicalGameObj) then adds control.
        // PhysicalGameObj: on_host_bone(1)
        // ArmedGameObj: targeting.xyz (13×3 = 39)
        // SmartGameObj: continuousBoolBits(4) + 4 × analog(8) = 36
        // Total from super.exportFrequent in VehicleGameObj: 1 + 39 + 36 = 76 bits
        private const val SMART_SUPER_BITS = 1 + 3 * POS_BITS + CONT_BOOL_BITS + 4 * ANALOG_BITS
        // = 1 + 39 + 4 + 32 = 76

        // VehicleGameObj frequent: physics section (engineOn + pos + q + vel + angVel)
        private const val VEHICLE_PHYS_BITS =
            1 + 3 * POS_BITS + 4 * QUAT_BITS + 3 * VEL_BITS + 3 * ANG_VEL_BITS
        // = 1 + 39 + 48 + 45 + 36 = 169

        // Full frequent for a physics vehicle (CAR/TANK/BIKE/FLYING):
        //   totalRounds(32) + VEHICLE_PHYS_BITS(169) + driverIsGunner(1) + SMART_SUPER_BITS(76)
        private const val FREQUENT_PHYS_BITS =
            32 + VEHICLE_PHYS_BITS + 1 + SMART_SUPER_BITS
        // = 32 + 169 + 1 + 76 = 278

        // Full frequent for a turret (no physics):
        //   totalRounds(32) + driverIsGunner(1) + SMART_SUPER_BITS(76)
        private const val FREQUENT_TURRET_BITS = 32 + 1 + SMART_SUPER_BITS
        // = 32 + 1 + 76 = 109

        @BeforeAll @JvmStatic fun setupEncoders() {
            // World position (same as SoldierGameObjTest)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -500.0, 500.0, 0.2)
            EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -500.0, 500.0, 0.2)
            // Health/shield for the rare chain
            EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
            EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)
            // SmartGameObj control
            EncoderRegistry.setPrecision(BITPACK_CONTINUOUS_BOOLEAN_BITS, 4)
            EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
            // Vehicle-specific (from C++ VehicleGameObj::Set_Precision)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_VELOCITY,         -90.0,  90.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_ANGULAR_VELOCITY, -20.0,  20.0, 0.01)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_QUATERNION,        -1.0,   1.0, 0.0005)
            EncoderRegistry.setPrecision(BITPACK_VEHICLE_LOCK_TIMER,         0.0,  16.0, 0.25)
        }

        private fun defaultVehicle(
            vehicleType: Int = VehicleGameObj.VEHICLE_TYPE_CAR,
            seatCount: Int = 1,
            lockOwnerId: Int = 0,
            lockTimer: Float = 0f,
            vehicleDelivered: Boolean = false,
            isEngineOn: Boolean = true,
            quaternion: Quaternion = Quaternion.IDENTITY,
            velocity: Vector3 = Vector3(0f, 0f, 0f),
            angularVelocity: Vector3 = Vector3(0f, 0f, 0f),
            totalRounds: Int = 0,
            driverIsGunner: Boolean = false,
            seatOccupantIds: List<Int> = emptyList(),
        ) = VehicleGameObj(
            definitionId      = 0x04e20000,
            position          = Vector3(0f, 0f, 0f),
            facing            = 0f,
            modelName         = "rhino",
            animName          = "",
            health            = 100f,
            controlOwner      = 0,
            team              = 0,
            vehicleType       = vehicleType,
            seatCount         = seatCount,
            lockOwnerId       = lockOwnerId,
            lockTimer         = lockTimer,
            vehicleDelivered  = vehicleDelivered,
            isEngineOn        = isEngineOn,
            quaternion        = quaternion,
            velocity          = velocity,
            angularVelocity   = angularVelocity,
            totalRounds       = totalRounds,
            driverIsGunner    = driverIsGunner,
            seatOccupantIds   = seatOccupantIds,
        )
    }

    // ---- 1: networkClassId ----

    @Test
    fun `networkClassId is 1000`() {
        assertEquals(1000, defaultVehicle().networkClassId)
    }

    // ---- 2: full creation envelope ----

    @Test
    fun `full creation envelope has networkClassId 1000`() {
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, defaultVehicle(), networkId = 300001)

        assertEquals(300001, bs.getInt())                       // networkId
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)       // dirtyBits = BIT_CREATION
        assertFalse(bs.getBool())                               // isDeletePending
        assertEquals(1000, bs.getInt())                         // networkClassId
    }

    // ---- 3: creation - locked vs unlocked ----

    @Test
    fun `creation - locked vehicle is larger than unlocked`() {
        val unlocked = BitStream().also { defaultVehicle(lockOwnerId = 0).exportCreation(it) }
        val locked   = BitStream().also { defaultVehicle(lockOwnerId = 5, lockTimer = 3.0f).exportCreation(it) }

        // locked adds an extra LOCK_BITS=7 for the lockTimer float
        assertEquals(unlocked.bitWritePosition + LOCK_BITS, locked.bitWritePosition)
    }

    @Test
    fun `creation - lockOwnerId 0 means no lockTimer written`() {
        // Base: PhysicalGameObj(definitionId+pos.xyz+facing) + SmartGameObj(controlOwner) + VehicleGameObj(lockOwnerId)
        // = 32 + 13+13+13 + 32 + 32 + 32 = 167 bits (when unlocked)
        val bs = BitStream()
        defaultVehicle(lockOwnerId = 0).exportCreation(bs)

        val expectedBits = 32 + 3 * POS_BITS + 32 + 32 + 32  // defId + pos + facing + controlOwner + lockOwnerId
        assertEquals(expectedBits, bs.bitWritePosition)
        assertEquals(167, bs.bitWritePosition)
    }

    @Test
    fun `creation - locked vehicle has lockTimer field`() {
        val bs = BitStream()
        defaultVehicle(lockOwnerId = 7, lockTimer = 4.0f).exportCreation(bs)

        // same as unlocked + LOCK_BITS
        assertEquals(167 + LOCK_BITS, bs.bitWritePosition)
        assertEquals(174, bs.bitWritePosition)
    }

    // ---- 4: rare - empty seats write -1 ----

    @Test
    fun `rare - empty seats write -1 per seat`() {
        val vehicle = defaultVehicle(seatCount = 2)
        val bs = BitStream()
        vehicle.exportRare(bs)

        // Skip PhysicalGameObj rare fields
        bs.getTerminatedString(permitEmpty = true)   // modelName
        bs.getTerminatedString(permitEmpty = true)   // animName
        repeat(5) { bs.getInt() }                    // currFrame, targetFrame, animMode, hostModelId, hostBone
        bs.getInt()                                  // playerType
        bs.getBool()                                 // hudPokable
        bs.getBool()                                 // hidden (vehicle-only)

        // VehicleGameObj rare: seats
        assertEquals(-1, bs.getInt())               // seat[0] = -1 (empty)
        assertEquals(-1, bs.getInt())               // seat[1] = -1 (empty)
    }

    @Test
    fun `rare - vehicleDelivered bool is written`() {
        val vehicle = defaultVehicle(seatCount = 1, vehicleDelivered = true)
        val bs = BitStream()
        vehicle.exportRare(bs)

        // Skip all before vehicleDelivered
        bs.getTerminatedString(permitEmpty = true)   // modelName
        bs.getTerminatedString(permitEmpty = true)   // animName
        repeat(5) { bs.getInt() }
        bs.getInt()                                  // playerType
        bs.getBool()                                 // hudPokable
        bs.getBool()                                 // hidden
        bs.getInt()                                  // seat[0] = -1

        assertTrue(bs.getBool())                     // vehicleDelivered = true
    }

    @Test
    fun `rare - bit count for vehicle with model rhino and 1 seat`() {
        // model "rhino" = 5 chars → 16 + 40 = 56 bits
        // anim "" = 16 bits
        // 6 ints = 192 bits
        // hudPokable(1) + hidden(1) + seat[0](32) + vehicleDelivered(1) = 35 bits
        // Total = 56 + 16 + 192 + 35 = 299 bits
        val bs = BitStream()
        defaultVehicle(seatCount = 1).exportRare(bs)

        val modelBits = 16 + "rhino".length * 8    // 56
        val animBits  = 16                          // 16
        val expected  = modelBits + animBits + 6 * 32 + 1 + 1 + 1 * 32 + 1
        assertEquals(expected, bs.bitWritePosition)
        assertEquals(299, bs.bitWritePosition)
    }

    @Test
    fun `rare - seatCount affects bit count`() {
        val oneS = BitStream().also { defaultVehicle(seatCount = 1).exportRare(it) }
        val twoS = BitStream().also { defaultVehicle(seatCount = 2).exportRare(it) }
        val fourS = BitStream().also { defaultVehicle(seatCount = 4).exportRare(it) }

        assertEquals(oneS.bitWritePosition + 32, twoS.bitWritePosition)
        assertEquals(oneS.bitWritePosition + 3 * 32, fourS.bitWritePosition)
    }

    // ---- 6: frequent - totalRounds is first field ----

    @Test
    fun `frequent - totalRounds is first field`() {
        val vehicle = defaultVehicle(totalRounds = 42)
        val bs = BitStream()
        vehicle.exportFrequent(bs)

        assertEquals(42, bs.getInt())   // totalRounds read first
    }

    // ---- 7: frequent - physics vehicle vs turret ----

    @Test
    fun `frequent - physics vehicle is larger than turret`() {
        val car    = BitStream().also { defaultVehicle(vehicleType = VehicleGameObj.VEHICLE_TYPE_CAR).exportFrequent(it) }
        val turret = BitStream().also { defaultVehicle(vehicleType = VehicleGameObj.VEHICLE_TYPE_TURRET).exportFrequent(it) }

        assertEquals(FREQUENT_PHYS_BITS, car.bitWritePosition)
        assertEquals(FREQUENT_TURRET_BITS, turret.bitWritePosition)
        assertTrue(car.bitWritePosition > turret.bitWritePosition)
    }

    @Test
    fun `frequent - tank and bike and flying match car bit count`() {
        val car    = BitStream().also { defaultVehicle(vehicleType = VehicleGameObj.VEHICLE_TYPE_CAR).exportFrequent(it) }
        val tank   = BitStream().also { defaultVehicle(vehicleType = VehicleGameObj.VEHICLE_TYPE_TANK).exportFrequent(it) }
        val bike   = BitStream().also { defaultVehicle(vehicleType = VehicleGameObj.VEHICLE_TYPE_BIKE).exportFrequent(it) }
        val flying = BitStream().also { defaultVehicle(vehicleType = VehicleGameObj.VEHICLE_TYPE_FLYING).exportFrequent(it) }

        assertEquals(car.bitWritePosition, tank.bitWritePosition)
        assertEquals(car.bitWritePosition, bike.bitWritePosition)
        assertEquals(car.bitWritePosition, flying.bitWritePosition)
    }

    // ---- 8: frequent - turret has no physics state ----

    @Test
    fun `frequent - turret has no physics state, reads totalRounds then driverIsGunner`() {
        val vehicle = defaultVehicle(
            vehicleType    = VehicleGameObj.VEHICLE_TYPE_TURRET,
            totalRounds    = 99,
            driverIsGunner = true,
        )
        val bs = BitStream()
        vehicle.exportFrequent(bs)

        assertEquals(99, bs.getInt())       // totalRounds (32 bits read)
        // No physics block for turret — driverIsGunner comes immediately after totalRounds
        assertTrue(bs.getBool())             // driverIsGunner (1 bit read)
        // Read position should be 33 (32 + 1)
        assertEquals(33, bs.bitReadPosition)
    }

    // ---- Additional: round-trip fields ----

    @Test
    fun `frequent - driverIsGunner is written before SmartGameObj control`() {
        val vehicle = defaultVehicle(
            vehicleType    = VehicleGameObj.VEHICLE_TYPE_CAR,
            driverIsGunner = true,
        )
        val bs = BitStream()
        vehicle.exportFrequent(bs)

        bs.getInt()                                      // totalRounds
        bs.getBool()                                     // isEngineOn
        bs.getFloat(BITPACK_WORLD_POSITION_X)            // pos.x
        bs.getFloat(BITPACK_WORLD_POSITION_Y)            // pos.y
        bs.getFloat(BITPACK_WORLD_POSITION_Z)            // pos.z
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)          // q.x
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)          // q.y
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)          // q.z
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)          // q.w
        bs.getFloat(BITPACK_VEHICLE_VELOCITY)            // vel.x
        bs.getFloat(BITPACK_VEHICLE_VELOCITY)            // vel.y
        bs.getFloat(BITPACK_VEHICLE_VELOCITY)            // vel.z
        bs.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY)    // angVel.x
        bs.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY)    // angVel.y
        bs.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY)    // angVel.z

        assertTrue(bs.getBool())                         // driverIsGunner = true

        // SmartGameObj::exportFrequent chain follows (super.exportFrequent from VehicleGameObj):
        // PhysicalGameObj: on_host_bone
        bs.getBool()                                     // on_host_bone
        // ArmedGameObj: targeting.xyz
        bs.getFloat(BITPACK_WORLD_POSITION_X)            // targeting.x
        bs.getFloat(BITPACK_WORLD_POSITION_Y)            // targeting.y
        bs.getFloat(BITPACK_WORLD_POSITION_Z)            // targeting.z
        // SmartGameObj: control
        bs.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)
        bs.getFloat(BITPACK_ANALOG_VALUES)

        assertEquals(FREQUENT_PHYS_BITS, bs.bitReadPosition)
        assertEquals(bs.bitWritePosition, bs.bitReadPosition)
    }

    @Test
    fun `frequent - physics fields round-trip`() {
        val pos    = Vector3(10f, 20f, 5f)
        val q      = Quaternion(0.1f, 0.2f, 0.3f, 0.9274f).normalized()
        val vel    = Vector3(5.5f, -3.2f, 0f)
        val angVel = Vector3(0.5f, 0f, -0.5f)

        val vehicle = defaultVehicle(
            vehicleType     = VehicleGameObj.VEHICLE_TYPE_CAR,
            isEngineOn      = true,
            quaternion      = q,
            velocity        = vel,
            angularVelocity = angVel,
        ).apply {
            // Override position via a new vehicle with the right position
        }

        // Use a vehicle that has the specified position
        val v2 = VehicleGameObj(
            definitionId      = 0x04e20000,
            position          = pos,
            facing            = 0f,
            modelName         = "rhino",
            animName          = "",
            health            = 100f,
            controlOwner      = 0,
            team              = 0,
            vehicleType       = VehicleGameObj.VEHICLE_TYPE_CAR,
            seatCount         = 1,
            lockOwnerId       = 0,
            isEngineOn        = true,
            quaternion        = q,
            velocity          = vel,
            angularVelocity   = angVel,
        )
        val bs = BitStream()
        v2.exportFrequent(bs)

        bs.getInt()                                                          // totalRounds
        assertTrue(bs.getBool())                                             // isEngineOn
        assertEquals(10f, bs.getFloat(BITPACK_WORLD_POSITION_X), absoluteTolerance = 0.3f)
        assertEquals(20f, bs.getFloat(BITPACK_WORLD_POSITION_Y), absoluteTolerance = 0.3f)
        assertEquals( 5f, bs.getFloat(BITPACK_WORLD_POSITION_Z), absoluteTolerance = 0.3f)
        // quaternion components
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)
        bs.getFloat(BITPACK_VEHICLE_QUATERNION)
        // velocity
        assertEquals(5.5f,  bs.getFloat(BITPACK_VEHICLE_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(-3.2f, bs.getFloat(BITPACK_VEHICLE_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(0f,    bs.getFloat(BITPACK_VEHICLE_VELOCITY), absoluteTolerance = 0.02f)
        // angular velocity
        assertEquals(0.5f,  bs.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(0f,    bs.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY), absoluteTolerance = 0.02f)
        assertEquals(-0.5f, bs.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY), absoluteTolerance = 0.02f)
    }

    @Test
    fun `creation - lockOwnerId round-trip`() {
        val vehicle = defaultVehicle(lockOwnerId = 7, lockTimer = 4.25f)
        val bs = BitStream()
        vehicle.exportCreation(bs)

        bs.getInt()                                  // definitionId
        bs.getFloat(BITPACK_WORLD_POSITION_X)
        bs.getFloat(BITPACK_WORLD_POSITION_Y)
        bs.getFloat(BITPACK_WORLD_POSITION_Z)
        bs.getFloat()                                // facing (raw)
        bs.getInt()                                  // controlOwner
        assertEquals(7, bs.getInt())                // lockOwnerId
        val timer = bs.getFloat(BITPACK_VEHICLE_LOCK_TIMER)
        assertEquals(4.25f, timer, absoluteTolerance = 0.3f)
    }
}

private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
    assert(kotlin.math.abs(expected - actual) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but got $actual"
    }
}
