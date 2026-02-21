package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: SAMSiteGameObj (samsite.cpp) — extends SmartGameObj.
// Export_Frequent just calls super.exportFrequent(packet) — no additional fields.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj
//                         → ArmedGameObj → SmartGameObj → SAMSiteGameObj
class SAMSiteGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    targeting: Vector3 = Vector3(position.x + 1f, position.y + 1f, position.z + 1f),
    controlOwner: Int = 0,
    team: Int = 0,
) : SmartGameObj(
    definitionId   = definitionId,
    position       = position,
    facing         = facing,
    modelName      = modelName,
    animName       = animName,
    health         = health,
    shieldStrength = shieldStrength,
    shieldType     = shieldType,
    targeting      = targeting,
    controlOwner   = controlOwner,
    team           = team,
) {
    // C++: SAMSiteGameObj::Export_Frequent — calls super only; no additional fields.
    override fun exportFrequent(packet: BitStream) {
        super.exportFrequent(packet)
    }
}
