package ccr.server.net

import ccr.math.Vector3

// Simple value type for weapon slot data — used by SoldierGameObj.weapons computed property
// and secondary constructors in tests.
data class WeaponEntry(val definitionId: Int, val totalRounds: Int)

// C++: WeaponClass (weapons.h / weapons.cpp)
// Minimal server-side port — client-side rendering/firing logic not ported.
class WeaponClass(
    val definitionId: Int,                 // C++: Definition->Get_ID()
    // @JvmName avoids clash with fun setTotalRounds() below
    @get:JvmName("totalRoundsField") @set:JvmName("setTotalRoundsField")
    var totalRounds: Int = 0,              // C++: Get_Total_Rounds() / Set_Total_Rounds()
) {
    // C++: int ClipRounds
    var clipRounds: Int = 0

    // C++: int InventoryRounds
    var inventoryRounds: Int = 0

    // C++: ArmedGameObj* Owner (widened to PhysicalGameObj to allow PowerUpGameObj backpacks)
    var owner: PhysicalGameObj? = null

    // C++: void Set_Total_Rounds(int num)
    fun setTotalRounds(num: Int) { totalRounds = num }

    // C++: void Add_Rounds(int num)
    fun addRounds(num: Int) { totalRounds += num }

    // C++: bool Is_Ammo_Maxed() — FIXME: needs WeaponDefinitionClass for max rounds
    fun isAmmoMaxed(): Boolean = false

    // C++: bool Can_Receive_Generic_CnC_Ammo() — true if weapon accepts generic clip powerups
    // FIXME: real check reads WeaponDefinitionClass::CanReceiveGenericCNCAmmo; stub returns false
    val canReceiveGenericCncAmmo: Boolean get() = false

    // C++: int Get_Clip_Size() — rounds granted per clip powerup pickup
    // FIXME: real value comes from WeaponDefinitionClass::ClipSize; stub returns 0
    fun getClipSize(): Int = 0

    // C++: bool IsPrimaryTriggered, IsSecondaryTriggered
    // @JvmName avoids clash with fun setPrimaryTriggered() / fun setSecondaryTriggered() below
    @get:JvmName("isPrimaryTriggeredField") @set:JvmName("setIsPrimaryTriggeredField")
    var isPrimaryTriggered: Boolean = false
    @get:JvmName("isSecondaryTriggeredField") @set:JvmName("setIsSecondaryTriggeredField")
    var isSecondaryTriggered: Boolean = false

    // C++: void Set_Primary_Triggered(bool)
    fun setPrimaryTriggered(value: Boolean) { isPrimaryTriggered = value }

    // C++: void Set_Secondary_Triggered(bool)
    fun setSecondaryTriggered(value: Boolean) { isSecondaryTriggered = value }

    // C++: bool Is_Triggered() — true if either primary or secondary trigger is active
    fun isTriggered(): Boolean = isPrimaryTriggered || isSecondaryTriggered

    // C++: int Get_ID() const — delegates to definition->Get_ID()
    fun getId(): Int = definitionId

    // C++: int Get_Total_Rounds() const
    fun getTotalRounds(): Int = totalRounds

    // C++: const char* Get_Name() — delegates to definition->Get_Name(); stub returns defId as string
    fun getName(): String = "weapon_$definitionId"

    // C++: bool Is_Reloading() — true when weapon is in reload animation cycle; stub returns false
    fun isReloading(): Boolean = false

    // C++: void Update() — firing state machine, burst logic, muzzle flashes
    // FIXME: full implementation requires WeaponDefinitionClass, AmmoDefinitionClass, TimeManager — not yet ported
    fun update() {}

    // C++: void Set_Target(const Vector3& pos) — sets world-space target position for projectile aim
    // FIXME: actual targeting logic not ported; server-side stub stores target for AI state tracking
    var targetPos: Vector3 = Vector3()
    fun setTarget(pos: Vector3) { targetPos = pos }
}
