package ccr.server.net

import ccr.math.Vector3
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.WeaponDefinitionClass
import ccr.server.level.DefinitionRegistry

// Simple value type for weapon slot data — used by SoldierGameObj.weapons computed property
// and secondary constructors in tests.
data class WeaponEntry(val definitionId: Int, val totalRounds: Int)

// C++: enum WeaponStateType (weapons.h)
enum class WeaponStateType {
    STATE_IDLE, STATE_READY, STATE_CHARGE, STATE_FIRE_PRIMARY, STATE_FIRE_SECONDARY,
    STATE_RELOAD, STATE_START_SWITCH, STATE_END_SWITCH
}

// C++: WeaponClass (weapons.h / weapons.cpp)
class WeaponClass(val definition: WeaponDefinitionClass) {

    // C++: AmmoDefinitionClass* PrimaryAmmoDefinition
    var primaryAmmoDefinition: AmmoDefinitionClass? = null
        private set

    // C++: AmmoDefinitionClass* SecondaryAmmoDefinition
    var secondaryAmmoDefinition: AmmoDefinitionClass? = null
        private set

    // C++: int ClipRounds
    var clipRounds: Int = 0

    // C++: int InventoryRounds  (-1 = unlimited)
    var inventoryRounds: Int = 0

    // C++: bool WeaponExists
    var weaponExists: Boolean = true

    // C++: int C4DetonationMode
    var c4DetonationMode: Int = 1

    // C++: WeaponStateType State
    var state: WeaponStateType = WeaponStateType.STATE_IDLE

    // C++: float StateTimer
    var stateTimer: Float = 0f

    // C++: bool DidFire
    var didFire: Boolean = false

    // C++: bool IsPrimaryTriggered
    var isPrimaryTriggered: Boolean = false
        private set

    // C++: bool IsSecondaryTriggered
    var isSecondaryTriggered: Boolean = false
        private set

    // C++: bool LastFrameIsPrimaryTriggered
    var lastFrameIsPrimaryTriggered: Boolean = false

    // C++: bool LastFrameIsSecondaryTriggered
    var lastFrameIsSecondaryTriggered: Boolean = false

    // C++: int TotalRoundsFired
    var totalRoundsFired: Int = 0

    // C++: bool SafetySet
    var safetySet: Boolean = false

    // C++: bool LockTriggers
    var lockTriggers: Boolean = false

    // C++: float EmptySoundTimer
    var emptySoundTimer: Float = 0f

    // C++: float BurstDelayTimer
    var burstDelayTimer: Float = 0f

    // C++: int BurstCount
    var burstCount: Int = 0

    // C++: float BulletBumpTime
    var bulletBumpTime: Float = 0f

    // C++: ArmedGameObj* Owner
    var owner: ArmedGameObj? = null

    // C++: Vector3 Target
    var target: Vector3 = Vector3()

    // C++: WeaponClass::Init(DefinitionMgrClass* def_mgr)
    fun init(registry: DefinitionRegistry?) {
        primaryAmmoDefinition = registry?.findById(definition.primaryAmmoDefID.toUInt()) as? AmmoDefinitionClass
        secondaryAmmoDefinition = registry?.findById(definition.secondaryAmmoDefID.toUInt()) as? AmmoDefinitionClass
    }

    // C++: int Get_ID() const
    fun getId(): Int = definition.id.toInt()

    // C++: const char* Get_Name()
    fun getName(): String = definition.name

    // C++: int Get_Style()
    fun getStyle(): Int = definition.style

    // C++: float Get_Key_Number()
    fun getKeyNumber(): Float = definition.keyNumber

    // C++: bool Can_Snipe()
    fun getCanSnipe(): Boolean = definition.canSnipe

    // C++: float Get_Rating()
    fun getRating(): Float = definition.rating

    // C++: float Get_Recoil_Time()
    fun getRecoilTime(): Float = definition.recoilTime

    // C++: float Get_Recoil_Scale()
    fun getRecoilScale(): Float = definition.recoilScale

    // C++: int Get_Clip_Size()
    fun getClipSize(): Int = definition.clipSize

    // C++: bool Can_Receive_Generic_CnC_Ammo()
    val canReceiveGenericCncAmmo: Boolean get() = definition.canReceiveGenericCnCAmmo

    // C++: float Get_Effective_Range()
    fun getEffectiveRange(): Float = primaryAmmoDefinition?.effectiveRange ?: 0f

    // C++: float Get_Primary_Fire_Rate()
    fun getPrimaryFireRate(): Float = primaryAmmoDefinition?.rateOfFire ?: 1f

    // C++: float Get_Range()
    fun getRange(): Float = primaryAmmoDefinition?.range ?: 0f

    // C++: void Set_Total_Rounds(int num)
    fun setTotalRounds(num: Int) {
        if (num == -1) {
            inventoryRounds = -1
            clipRounds = 0
        } else {
            val clipSize = definition.clipSize.coerceAtLeast(1)
            val maxInv = definition.maxInventoryRounds
            val total = num.coerceAtMost(maxInv + clipSize)
            clipRounds = total.coerceAtMost(clipSize)
            inventoryRounds = (total - clipRounds).coerceAtLeast(0)
        }
    }

    // C++: void Add_Rounds(int num)
    fun addRounds(num: Int) {
        if (num < 0) {
            inventoryRounds = -1
        } else if (inventoryRounds >= 0) {
            val clipSize = definition.clipSize.coerceAtLeast(1)
            val maxInv = definition.maxInventoryRounds
            // Fill clip first, then inventory
            val clipSpace = (clipSize - clipRounds).coerceAtLeast(0)
            val toClip = num.coerceAtMost(clipSpace)
            clipRounds += toClip
            val remaining = num - toClip
            inventoryRounds = (inventoryRounds + remaining).coerceAtMost(maxInv)
        }
    }

    // C++: bool Is_Ammo_Maxed()
    fun isAmmoMaxed(): Boolean {
        if (inventoryRounds < 0) return true  // unlimited
        return inventoryRounds >= definition.maxInventoryRounds
    }

    // C++: bool Is_Loaded()
    fun isLoaded(): Boolean = clipRounds != 0

    // C++: bool Is_Reload_OK()
    fun isReloadOk(): Boolean = inventoryRounds != 0

    // C++: bool Is_Reload_Needed()
    fun isReloadNeeded(): Boolean = !isLoaded() && isReloadOk()

    // C++: void Do_Reload()
    fun doReload() {
        if (inventoryRounds < 0) {
            clipRounds = definition.clipSize
            return
        }
        val clipSize = definition.clipSize
        val needed = clipSize - clipRounds
        val moved = needed.coerceAtMost(inventoryRounds)
        clipRounds += moved
        inventoryRounds -= moved
    }

    // C++: void Decrement_Rounds(int rounds = 1)
    fun decrementRounds(rounds: Int = 1) {
        if (inventoryRounds >= 0) {
            clipRounds = (clipRounds - rounds).coerceAtLeast(0)
        }
    }

    // C++: int Get_Total_Rounds() const
    fun getTotalRounds(): Int = if (inventoryRounds < 0) -1 else clipRounds + inventoryRounds

    // C++: void Set_Primary_Triggered(bool)
    fun setPrimaryTriggered(value: Boolean) {
        if (!lockTriggers) isPrimaryTriggered = value
    }

    // C++: void Set_Secondary_Triggered(bool)
    fun setSecondaryTriggered(value: Boolean) {
        if (!lockTriggers) isSecondaryTriggered = value
    }

    // C++: bool Is_Triggered()
    fun isTriggered(): Boolean = isPrimaryTriggered || isSecondaryTriggered

    // C++: bool Does_Weapon_Exist()
    fun doesWeaponExist(): Boolean = weaponExists

    // C++: bool Is_Firing()
    fun isFiring(): Boolean = didFire

    // C++: bool Is_Reloading()
    fun isReloading(): Boolean = state == WeaponStateType.STATE_RELOAD

    // C++: bool Is_Switching()
    fun isSwitching(): Boolean = state == WeaponStateType.STATE_START_SWITCH || state == WeaponStateType.STATE_END_SWITCH

    // C++: void Select()
    fun select() { state = WeaponStateType.STATE_START_SWITCH }

    // C++: void Deselect()
    fun deselect() {
        isPrimaryTriggered = false
        isSecondaryTriggered = false
        state = WeaponStateType.STATE_IDLE
    }

    // C++: void Force_Reload()
    fun forceReload() { state = WeaponStateType.STATE_RELOAD }

    // C++: void Next_C4_Detonation_Mode()
    fun nextC4DetonationMode() {
        c4DetonationMode = if (c4DetonationMode >= 3) 1 else c4DetonationMode + 1
    }

    // C++: void Set_Safety(bool)
    fun setSafety(value: Boolean) { safetySet = value }

    // C++: bool Is_Safety_Set()
    fun isSafetySet(): Boolean = safetySet

    // C++: void Update()
    fun update() {}

    // C++: void Reset_Anim_Update() — clears the "anim updated this frame" flag
    fun resetAnimUpdate() {}

    // C++: int Get_Anim_State() — returns current weapon fire animation state
    fun getAnimState(): Int = WEAPON_ANIM_NOT_FIRING

    // C++: const char* Get_Back_Model_Name() — returns the model name for the back-of-soldier slot
    fun getBackModelName(): String = definition.backModel

    // C++: bool Is_Model_Update_Needed() — true when the weapon model needs to be re-attached
    fun isModelUpdateNeeded(): Boolean = false

    // C++: void Reset_Model_Update() — clears the model-update-needed flag
    fun resetModelUpdate() {}

    // C++: const char* Get_Model_Name() — returns the render model name for this weapon
    fun getModelName(): String = definition.model

    // C++: void Stop_Firing_Sound()
    // Server-side: FiringSound is client-only audio — no-op on the server.
    // The field firingSound is not tracked server-side (audio state is never replicated).
    // TODO: if server-side muzzle/fire effects are ever needed, mirror C++ weapons.cpp:1572–1583
    fun stopFiringSound() {
        // no-op: FiringSound / FiringSoundDefID are client-side audio state
    }

    companion object {
        // C++: WeaponClass::AnimStateType enum (weapons.h)
        const val WEAPON_ANIM_NOT_FIRING = 0
        const val WEAPON_ANIM_FIRING_0   = 1
        const val WEAPON_ANIM_FIRING_1   = 2

        // C++: WeaponHoldStyleType enum (weapons.h) — weapon carry/hold pose
        const val WEAPON_HOLD_STYLE_C4             = 0
        const val WEAPON_HOLD_STYLE_NOT_USED       = 1
        const val WEAPON_HOLD_STYLE_AT_SHOULDER    = 2
        const val WEAPON_HOLD_STYLE_AT_HIP         = 3
        const val WEAPON_HOLD_STYLE_LAUNCHER       = 4
        const val WEAPON_HOLD_STYLE_HANDGUN        = 5
        const val WEAPON_HOLD_STYLE_BEACON         = 6
        const val WEAPON_HOLD_STYLE_EMPTY_HANDS    = 7
        const val WEAPON_HOLD_STYLE_AT_CHEST       = 8
        const val WEAPON_HOLD_STYLE_HANDS_DOWN     = 9
        const val NUM_WEAPON_HOLD_STYLES           = 10
    }
}
