package ccr.server.net

// C++: OffenseObjectClass (damage.h)
class OffenseObjectClass(
    var damage: Float = 1.0f,
    var warhead: Int = 0,          // C++: WarheadType
    // @JvmName avoids clash with fun getOwner() below
    @get:JvmName("ownerField") @set:JvmName("setOwnerField")
    var owner: ScriptableGameObj? = null,
) {
    // C++: bool ForceServerDamage
    var forceServerDamage: Boolean = false

    // C++: bool EnableClientDamage
    var enableClientDamage: Boolean = false

    // C++: ScriptableGameObj* Get_Owner() const { return Owner; }
    fun getOwner(): ScriptableGameObj? = owner
}
