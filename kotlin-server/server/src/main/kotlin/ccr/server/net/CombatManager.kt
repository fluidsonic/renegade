package ccr.server.net

import ccr.physics.scene.PhysicsScene

// C++: CombatManager (combat.h / combat.cpp)
object CombatManager {

    // --- Server-relevant state (C++ static fields) ---
    var friendlyFirePermitted: Boolean = false
    var beaconPlacementEndsGame: Boolean = false
    var areObserversActive: Boolean = true
    var networkHandler: CombatNetworkHandlerClass? = null
    @get:JvmName("difficultyLevelField")
    var difficultyLevel: Int = 1
    var isGameplayPermittedFlag: Boolean = false

    // C++ statics that are always true on a dedicated server
    private var iAmServer: Boolean = true

    // --- Init / Shutdown (C++: CombatManager::Init / Shutdown) ---

    fun init() {
        friendlyFirePermitted = false
        beaconPlacementEndsGame = false
        areObserversActive = true
        networkHandler = null
        difficultyLevel = 1
        isGameplayPermittedFlag = false
    }

    fun shutdown() {
        networkHandler = null
    }

    // --- Network handler delegates (C++: CombatManager::Can_Damage etc.) ---

    fun canDamage(damager: ArmedGameObj?, victim: PhysicalGameObj?): Boolean =
        networkHandler?.canDamage(damager, victim) ?: true

    fun getDamageFactor(damager: ArmedGameObj?, victim: PhysicalGameObj?): Float =
        networkHandler?.getDamageFactor(damager, victim) ?: 1.0f

    fun onSoldierKill(killer: SoldierGameObj, victim: SoldierGameObj) {
        networkHandler?.onSoldierKill(killer, victim)
    }

    fun onSoldierDeath(soldier: SoldierGameObj) {
        networkHandler?.onSoldierDeath(soldier)
    }

    // C++: Is_Gameplay_Permitted — uses cached IsGameplayPermitted field when handler is set
    fun isGameplayPermitted(): Boolean =
        if (networkHandler == null) true else isGameplayPermittedFlag

    // --- Simple accessors matching C++ getters/setters ---

    fun isFriendlyFirePermitted(): Boolean = friendlyFirePermitted

    fun doesBeaconPlacementEndsGame(): Boolean = beaconPlacementEndsGame

    fun areObserversActive(): Boolean = areObserversActive

    // getDifficultyLevel() — use .difficultyLevel property directly in Kotlin

    fun isServer(): Boolean = iAmServer

    // --- Stub methods for client-side functionality (server returns defaults) ---

    fun getScene(): PhysicsScene? = null

    fun getTheStar(): DamageableGameObj? = null

    fun getCombatStar(): SoldierGameObj? = null

    fun getGlobalSettings(): GlobalSettingsDefClass = GlobalSettingsDefClass

    fun isSkeletonSliderDemoEnabled(): Boolean = false

    fun getMessageWindow(): MessageWindowClass = MessageWindowClass()
}
