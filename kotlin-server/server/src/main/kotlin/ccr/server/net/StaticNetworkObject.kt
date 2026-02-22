package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import kotlin.math.roundToInt

// C++: StaticNetworkObjectClass (staticnetworkobject.h/.cpp)
// Static objects are identified by well-known networkId values (not networkClassId).
// Get_Network_Class_ID() is not overridden — returns 0 (base class default).
// SetDeletePending is overridden to no-op (static objects are never deleted).
abstract class StaticNetworkObject(
    val animationMode: Int = 0,
    val loopStart: Float = 0f,
    val loopEnd: Float = 0f,
    val currFrame: Float = 0f,
    val targetFrame: Float = 0f,
) : NetworkObject() {
    override val networkClassId: Int = 0
    override fun delete() {}
    override fun setDeletePending() {}  // static objects cannot be deleted

    // C++: StaticNetworkObjectClass::Export_Rare — writes animation state
    override fun exportRare(packet: BitStream) {
        packet.addInt(animationMode)
        packet.addFloat(loopStart)
        packet.addFloat(loopEnd)
        packet.addFloat(currFrame)
        packet.addFloat(targetFrame)
    }
}

// C++: DoorNetworkObjectClass — does NOT call super.Export_Rare; state-based only.
// Uses BITPACK_DOOR_STATE for encoding (0=CLOSED, 1=OPENED, 2=OPENING, 3=CLOSING, 4=ACCESS_DENIED).
class DoorNetworkObject(
    val doorState: Int = 0,
) : StaticNetworkObject() {
    // C++: DoorNetworkObjectClass::Export_Rare — skips animation fields (state-based optimization)
    override fun exportRare(packet: BitStream) {
        packet.addInt(doorState, BITPACK_DOOR_STATE)
    }
}

// C++: ElevatorNetworkObjectClass — does NOT call super.Export_Rare.
class ElevatorNetworkObject(
    val state: Int = 0,
    val doorStateTop: Int = 0,
    val doorStateBottom: Int = 0,
) : StaticNetworkObject() {
    // C++: ElevatorNetworkObjectClass::Export_Rare — raw ints, no encoder
    override fun exportRare(packet: BitStream) {
        packet.addInt(state)
        packet.addInt(doorStateTop)
        packet.addInt(doorStateBottom)
    }
}

// C++: DSAPONetworkObjectClass (Destructible Static Anim Physics Object)
// DOES call super.Export_Rare first (animation fields), then DefenseObject.Export (health/shield).
class DsapoNetworkObject(
    animationMode: Int = 0,
    loopStart: Float = 0f,
    loopEnd: Float = 0f,
    currFrame: Float = 0f,
    targetFrame: Float = 0f,
    val health: Float = 100f,
    val shieldStrength: Float = 0f,
    val shieldType: Int = 0,
) : StaticNetworkObject(animationMode, loopStart, loopEnd, currFrame, targetFrame) {
    // C++: DSAPONetworkObjectClass::Export_Rare — calls StaticNetworkObject::Export_Rare then DefenseObject.Export
    // DefenseObject.Export format: isDead(bool) + health(BITPACK_HEALTH) + shieldStrength(BITPACK_SHIELD_STRENGTH) + shieldType(BITPACK_SHIELD_TYPE)
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)
        // C++: DefenseObjectClass::Export — isDead check uses float comparison exactly
        packet.addBool(health == 0f)
        packet.addInt(health.roundToInt(), BITPACK_HEALTH)
        packet.addInt(shieldStrength.roundToInt(), BITPACK_SHIELD_STRENGTH)
        packet.addInt(shieldType, BITPACK_SHIELD_TYPE)
    }
}
