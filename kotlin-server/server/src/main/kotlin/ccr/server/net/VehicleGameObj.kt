package ccr.server.net

import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: VehicleGameObj (vehicle.cpp)
// Full hierarchy: NetworkObject → BaseGameObj → PhysicalGameObj → DamageableGameObj
//                             → ArmedGameObj → SmartGameObj → VehicleGameObj
//
// Vehicle types (C++ VehicleType enum in vehicle.h):
//   CAR=0, TANK=1, BIKE=2, FLYING=3, TURRET=4
//
// Precision (from C++ VehicleGameObj::Set_Precision):
//   BITPACK_VEHICLE_VELOCITY:         (-90, 90, 0.01)   → 15 bits/axis
//   BITPACK_VEHICLE_ANGULAR_VELOCITY: (-20, 20, 0.01)   → 12 bits/axis
//   BITPACK_VEHICLE_QUATERNION:       (-1,   1, 0.0005) → 12 bits/component
//   BITPACK_VEHICLE_LOCK_TIMER:       (0,   16, 0.25)   →  7 bits
class VehicleGameObj(
    definitionId: Int,
    position: Vector3 = Vector3(0f, 0f, 0f),
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    targeting: Vector3 = Vector3(position.x + 1f, position.y + 1f, position.z + 1f),
    controlOwner: Int = 0,
    team: Int = 0,
    // Vehicle-specific constructor params (no val/var — properties declared in class body)
    vehicleType: Int = VEHICLE_TYPE_CAR,
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
    isHidden: Boolean = false,
) : SmartGameObj(
    definitionId    = definitionId,
    position        = position,
    facing          = facing,
    modelName       = modelName,
    animName        = animName,
    health          = health,
    shieldStrength  = shieldStrength,
    shieldType      = shieldType,
    targeting       = targeting,
    controlOwner    = controlOwner,
    team            = team,
) {
    companion object {
        const val VEHICLE_TYPE_CAR    = 0
        const val VEHICLE_TYPE_TANK   = 1
        const val VEHICLE_TYPE_BIKE   = 2
        const val VEHICLE_TYPE_FLYING = 3
        const val VEHICLE_TYPE_TURRET = 4
    }

    // Definitional (immutable — set at construction from definition data)
    val vehicleType: Int = vehicleType
    val seatCount: Int = seatCount

    // Mutable runtime state
    var lockOwnerId: Int = lockOwnerId
    var lockTimer: Float = lockTimer
    var vehicleDelivered: Boolean = vehicleDelivered
    var isEngineOn: Boolean = isEngineOn
    var quaternion: Quaternion = quaternion
    var velocity: Vector3 = velocity
    var angularVelocity: Vector3 = angularVelocity
    var totalRounds: Int = totalRounds
    var driverIsGunner: Boolean = driverIsGunner
    var isHidden: Boolean = isHidden
    // One slot per seat; -1 means empty. Initialised from constructor param or filled with -1.
    val seatOccupantIds: MutableList<Int> = MutableList(seatCount) { seatOccupantIds.getOrElse(it) { -1 } }

    // C++: VehicleGameObj::Export_Creation — calls SmartGameObj chain, then writes lock state.
    // SmartGameObj::Export_Creation → PhysicalGameObj (definitionId + position + facing) → controlOwner
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)   // SmartGameObj → ... → definitionId + pos + facing + controlOwner
        packet.addInt(lockOwnerId)
        if (lockOwnerId != 0) {
            packet.addFloat(lockTimer, BITPACK_VEHICLE_LOCK_TIMER)
        }
    }

    // C++: VehicleGameObj::Export_Rare — calls SmartGameObj chain, then seat occupants + vehicleDelivered.
    // PhysicalGameObj::Export_Rare writes the "hidden" bool for VehicleGameObj instances (C++ check:
    // if (As_VehicleGameObj() != NULL) packet.Add(hidden)).  We include it here after calling super.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)       // PhysicalGameObj: model + anim + 6×int + playerType + hudPokable
        // C++: PhysicalGameObj::Export_Rare writes "hidden" only when As_VehicleGameObj() != NULL.
        packet.addBool(isHidden)       // hidden (vehicle-specific, written by PhysicalGameObj in C++)
        // VehicleGameObj adds seat occupants (one int per seat, -1 if empty)
        for (i in 0 until seatCount) {
            packet.addInt(seatOccupantIds.getOrElse(i) { -1 })
        }
        packet.addBool(vehicleDelivered)
    }

    // C++: VehicleGameObj::Export_Frequent
    // Order: totalRounds → (if physics type: engineOn + pos + q + vel + angVel) → driverIsGunner → super (control)
    override fun exportFrequent(packet: BitStream) {
        packet.addInt(totalRounds)

        when (vehicleType) {
            VEHICLE_TYPE_CAR, VEHICLE_TYPE_TANK, VEHICLE_TYPE_BIKE, VEHICLE_TYPE_FLYING -> {
                packet.addBool(isEngineOn)
                packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
                packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
                packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)
                packet.addFloat(quaternion.x, BITPACK_VEHICLE_QUATERNION)
                packet.addFloat(quaternion.y, BITPACK_VEHICLE_QUATERNION)
                packet.addFloat(quaternion.z, BITPACK_VEHICLE_QUATERNION)
                packet.addFloat(quaternion.w, BITPACK_VEHICLE_QUATERNION)
                packet.addFloat(velocity.x, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(velocity.y, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(velocity.z, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(angularVelocity.x, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                packet.addFloat(angularVelocity.y, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                packet.addFloat(angularVelocity.z, BITPACK_VEHICLE_ANGULAR_VELOCITY)
            }
            // VEHICLE_TYPE_TURRET: no physics data written
        }

        packet.addBool(driverIsGunner)
        super.exportFrequent(packet)   // SmartGameObj → ArmedGameObj → PhysicalGameObj (control + targeting + on_host_bone)
    }
}
