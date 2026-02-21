package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: CinematicGameObj (cinematicgameobj.cpp) — extends ArmedGameObj.
// Export_Rare: calls ArmedGameObj::Export_Rare (→ PhysicalGameObj chain), then appends:
//   animationName (terminated string, permitEmpty=true)
//   animMode (int) — AnimCollisionManagerClass::AnimModeType (0 = ANIMATE_TARGET)
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj
//                         → ArmedGameObj → CinematicGameObj
class CinematicGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    targeting: Vector3 = Vector3(position.x + 1f, position.y + 1f, position.z + 1f),
    val cinematicAnimName: String = "",
    val animMode: Int = ANIM_MODE_TARGET,
) : ArmedGameObj(
    definitionId   = definitionId,
    position       = position,
    facing         = facing,
    modelName      = modelName,
    animName       = animName,
    health         = health,
    shieldStrength = shieldStrength,
    shieldType     = shieldType,
    targeting      = targeting,
) {
    companion object {
        // AnimCollisionManagerClass::AnimModeType
        const val ANIM_MODE_TARGET = 0
        const val ANIM_MODE_LOOP   = 1
    }

    // C++: CinematicGameObj::Export_Rare — calls super (ArmedGameObj → PhysicalGameObj chain),
    // then appends cinematicAnimName and animMode.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addTerminatedString(cinematicAnimName, permitEmpty = true)
        packet.addInt(animMode)
    }
}
