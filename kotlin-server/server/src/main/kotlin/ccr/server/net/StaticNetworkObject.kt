package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import kotlin.math.roundToInt

// C++: StaticNetworkObjectClass (staticnetworkobject.h/.cpp)
// Static objects are identified by well-known networkId values (not networkClassId).
// Get_Network_Class_ID() is not overridden — returns 0 (base class default).
// SetDeletePending is overridden to no-op (static objects are never deleted).
// C++: constructor sets BIT_RARE; animationMode/currFrame/targetFrame/loopStart/loopEnd are mutable
//      and tracked for change detection in Network_Think.
abstract class StaticNetworkObject : NetworkObject() {
    override val networkClassId: Int = 0
    val creationDirtyBit: Int = NetworkObject.BIT_RARE
    override fun delete() {}
    override fun setDeletePending() {}  // static objects cannot be deleted

    // C++: StaticNetworkObjectClass private fields — mutable, updated by Network_Think
    var animationMode: Int = 0
    var loopStart: Float = 0f
    var loopEnd: Float = 0f
    var currFrame: Float = 0f
    var targetFrame: Float = 0f

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
// C++: Network_Think only marks dirty when DoorState is OPENING, CLOSING, or ACCESS_DENIED.
// The door's live state is read from an associated DoorPhysClass on each network tick.
class DoorNetworkObject(
    val door: ccr.physics.static.DoorPhysClass,
) : StaticNetworkObject() {
    // C++: DoorNetworkObjectClass::DoorState — cached state for change detection
    private var lastSentState: Int = door.state

    // C++: DoorNetworkObjectClass::Export_Rare — skips animation fields (state-based optimization)
    override fun exportRare(packet: BitStream) {
        packet.addInt(door.state, BITPACK_DOOR_STATE)
    }

    // C++: DoorNetworkObjectClass::Network_Think — marks BIT_RARE dirty on state change
    override fun networkThink() {
        val current = door.state
        if (current != lastSentState) {
            lastSentState = current
            setObjectDirtyBit(NetworkObject.BIT_RARE, true)
        }
    }
}

// C++: ElevatorNetworkObjectClass — does NOT call super.Export_Rare.
// C++: private fields State, DoorStateTop, DoorStateBottom — all mutable, tracked in Network_Think.
class ElevatorNetworkObject(
    // Constructor params share names with the mutable properties below.
    // Kotlin resolves this correctly: `var state: Int = state` initialises the property from the param.
    state: Int = 0,
    doorStateTop: Int = 0,
    doorStateBottom: Int = 0,
) : StaticNetworkObject() {

    // C++: ElevatorNetworkObjectClass private fields — mutable to support Network_Think updates
    var state: Int = state
    var doorStateTop: Int = doorStateTop
    var doorStateBottom: Int = doorStateBottom

    // C++: ElevatorNetworkObjectClass::Export_Rare — raw ints, no encoder
    override fun exportRare(packet: BitStream) {
        packet.addInt(state)
        packet.addInt(doorStateTop)
        packet.addInt(doorStateBottom)
    }
}

// C++: DSAPONetworkObjectClass (Destructible Static Anim Physics Object)
// DOES call super.Export_Rare first (animation fields), then DefenseObject.Export (health/shield).
// C++: Health field tracks current health for change detection in Network_Think.
class DsapoNetworkObject(
    animationMode: Int = 0,
    loopStart: Float = 0f,
    loopEnd: Float = 0f,
    currFrame: Float = 0f,
    targetFrame: Float = 0f,
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
) : StaticNetworkObject() {

    init {
        this.animationMode = animationMode
        this.loopStart = loopStart
        this.loopEnd = loopEnd
        this.currFrame = currFrame
        this.targetFrame = targetFrame
    }

    // C++: DSAPONetworkObjectClass::Health — tracks current health for dirty detection
    var health: Float = health
    var shieldStrength: Float = shieldStrength
    var shieldType: Int = shieldType

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
