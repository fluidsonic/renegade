package ccr.server.defs

// C++: DefenseObjectDefClass (damage.h)
class DefenseObjectDefClass(
    var health: Float = 100f,
    var healthMax: Float = 100f,
    var skin: Int = 0,                 // C++: SafeArmorType Skin
    var shieldStrength: Float = 0f,
    var shieldStrengthMax: Float = 0f,
    var shieldType: Int = 0,           // C++: SafeArmorType ShieldType
    var damagePoints: Float = 0f,
    var deathPoints: Float = 0f,
)
