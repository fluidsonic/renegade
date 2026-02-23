package ccr.server.net

// C++: StealthEffectClass (wwphys/stealtheffect.h / stealtheffect.cpp)
// Manages stealth/cloaking state for an object.
class StealthEffectClass {

    // C++: bool IsStealthEnabled
    var isStealthEnabled: Boolean = false
        private set

    // C++: bool IsFriendly
    var isFriendly: Boolean = false
        private set

    // C++: bool IsBroken
    var isBroken: Boolean = false
        private set

    // C++: float FadeDistance
    var fadeDistance: Float = 25.0f

    // C++: float CurrentFraction
    private var currentFraction: Float = 0f

    // C++: float TargetFraction
    private var targetFraction: Float = 0f

    // C++: void Enable_Stealth(bool onoff)
    fun enableStealth(onoff: Boolean) { isStealthEnabled = onoff }

    // C++: void Set_Friendly(bool onoff)
    fun setFriendly(onoff: Boolean) { isFriendly = onoff }

    // C++: void Set_Broken(bool onoff)
    fun setBroken(onoff: Boolean) { isBroken = onoff }

    // C++: bool Is_Stealthed() const — true if CurrentFraction > 0.5
    fun isStealthed(): Boolean = currentFraction > 0.5f

    // C++: void Set_Current_State(float percent)
    fun setCurrentState(percent: Float) { currentFraction = percent }

    // C++: void Set_Target_State(float percent)
    fun setTargetState(percent: Float) { targetFraction = percent }

    // C++: void Timestep(float dt) — updates currentFraction toward targetFraction
    fun timestep(dt: Float) {
        updateTargetFraction()
        val delta = targetFraction - currentFraction
        currentFraction += delta.coerceIn(-dt, dt)
    }

    // C++: void Damage_Occured() — briefly reveals the stealthed object
    fun damageOccurred() {
        setCurrentState(0f)
    }

    // C++: void Update_Target_Fraction() (protected)
    private fun updateTargetFraction() {
        targetFraction = if (isStealthEnabled && !isBroken) 1f else 0f
    }
}
