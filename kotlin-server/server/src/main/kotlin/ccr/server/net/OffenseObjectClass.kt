package ccr.server.net

// C++: OffenseObjectClass (damage.h)
class OffenseObjectClass(
    var damage: Float = 1.0f,
    var warhead: Int = 0,          // C++: WarheadType
    var owner: ScriptableGameObj? = null,
) {
    // C++: bool ForceServerDamage
    var forceServerDamage: Boolean = false

    // C++: bool EnableClientDamage
    var enableClientDamage: Boolean = false
}
