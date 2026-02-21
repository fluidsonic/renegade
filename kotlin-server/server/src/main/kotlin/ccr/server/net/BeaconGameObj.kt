package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: BeaconGameObj (beacongameobj.cpp) — extends SimpleGameObj.
// Export_Rare: calls super first, then appends state and ownerId.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → BeaconGameObj
class BeaconGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    val state: Int = 0,
    val ownerId: Int = 0,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    // C++: BeaconGameObj::Export_Rare — calls super then appends state + ownerId.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addInt(state)
        packet.addInt(ownerId)
    }
}
