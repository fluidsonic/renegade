package ccr.server.net

import ccr.net.bitstream.*
import kotlin.math.roundToInt

// C++: DamageableGameObj (damageablegameobj.cpp) → DefenseObjectClass (damage.cpp)
// Adds health and shield state.
// C++ hierarchy: ScriptableGameObj → DamageableGameObj → PhysicalGameObj
// (DamageableGameObj is ABOVE PhysicalGameObj in the inheritance chain)
abstract class DamageableGameObj(
    definitionId: Int,
    var health: Float = 100f,
    var shieldStrength: Float = 0f,
    var shieldType: Int = 0,
) : BaseGameObj(definitionId) {

    // C++: DamageableGameObj::Export_Occasional → DefenseObjectClass::Export (damage.cpp)
    // Chain: calls ScriptableGameObj::Export_Occasional (empty) then writes defense state.
    override fun exportOccasional(packet: BitStream) {
        super.exportOccasional(packet)  // ScriptableGameObj::Export_Occasional → empty chain
        packet.addBool(health == 0f)                               // (bool)(Health == 0)
        packet.addInt(health.roundToInt(), BITPACK_HEALTH)         // Round((double)(float)Health)
        packet.addInt(shieldStrength.roundToInt(), BITPACK_SHIELD_STRENGTH)
        packet.addInt(shieldType, BITPACK_SHIELD_TYPE)             // (UINT)ShieldType
    }
}
