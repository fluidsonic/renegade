package ccr.server.net

// C++: WeaponClass (weapons.h / weapons.cpp)
// Minimal server-side port — client-side rendering/firing logic not ported.
class WeaponClass(
    val definitionId: Int,                 // C++: Definition->Get_ID()
    var totalRounds: Int = 0,              // C++: Get_Total_Rounds() / Set_Total_Rounds()
) {
    // C++: int ClipRounds
    var clipRounds: Int = 0

    // C++: int InventoryRounds
    var inventoryRounds: Int = 0

    // C++: ArmedGameObj* Owner
    var owner: ArmedGameObj? = null

    // C++: void Set_Total_Rounds(int num)
    fun setTotalRounds(num: Int) { totalRounds = num }

    // C++: void Add_Rounds(int num)
    fun addRounds(num: Int) { totalRounds += num }

    // C++: bool Is_Ammo_Maxed() — FIXME: needs WeaponDefinitionClass for max rounds
    fun isAmmoMaxed(): Boolean = false

    // C++: bool IsPrimaryTriggered, IsSecondaryTriggered
    var isPrimaryTriggered: Boolean = false
    var isSecondaryTriggered: Boolean = false

    // C++: void Set_Primary_Triggered(bool)
    fun setPrimaryTriggered(value: Boolean) { isPrimaryTriggered = value }

    // C++: void Set_Secondary_Triggered(bool)
    fun setSecondaryTriggered(value: Boolean) { isSecondaryTriggered = value }

    // C++: void Update() — firing state machine, burst logic, muzzle flashes
    // FIXME: full implementation requires WeaponDefinitionClass, AmmoDefinitionClass, TimeManager — not yet ported
    fun update() {}
}
