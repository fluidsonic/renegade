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

    // Maximum values — set at construction time
    var healthMax: Float = health
    var shieldStrengthMax: Float = shieldStrength

    // C++: DefenseObjectClass::IsDead
    val isDead: Boolean get() = health <= 0f

    // Convenience alias
    val isDestroyedDmg: Boolean get() = isDead

    // C++: DamageableGameObj::Apply_Damage (simplified — no armor table yet)
    // Damage path: shield absorbs first, remainder goes to health.
    // Repair (negative damage): heals health first, then shield.
    open fun applyDamage(damage: Float) {
        if (isDead && damage >= 0f) return
        if (damage >= 0f) {
            val shieldDamage = damage.coerceAtMost(shieldStrength)
            shieldStrength -= shieldDamage
            val healthDamage = (damage - shieldDamage).coerceAtMost(health)
            health -= healthDamage
        } else {
            val repair = -damage
            val healthRepair = repair.coerceAtMost(healthMax - health)
            health += healthRepair
            val shieldRepair = (repair - healthRepair).coerceAtMost(shieldStrengthMax - shieldStrength)
            shieldStrength += shieldRepair
        }
        if (isDead) {
            completelyDamaged()
        }
    }

    // C++: DamageableGameObj::Completely_Damaged — virtual, override in subtypes
    protected open fun completelyDamaged() {}

    fun getNormalizedHealth(): Float = if (healthMax > 0f) health / healthMax else 0f

    fun setNormalizedHealth(pct: Float) {
        health = (pct * healthMax).coerceIn(0f, healthMax)
    }

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
