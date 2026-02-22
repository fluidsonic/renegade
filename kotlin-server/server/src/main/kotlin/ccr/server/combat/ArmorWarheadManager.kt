package ccr.server.combat

// C++: ArmorWarheadManager (armor.cpp / damage.cpp)
// Manages the warhead-vs-armor damage multiplier matrix.
object ArmorWarheadManager {

    // Stub — returns 1.0f for all combinations.
    // Full matrix can be loaded from defs later.
    fun getDamageMultiplier(warhead: Int, armorType: Int): Float = 1.0f

    fun scaleDamage(rawDamage: Float, warhead: Int, armorType: Int): Float =
        rawDamage * getDamageMultiplier(warhead, armorType)
}
