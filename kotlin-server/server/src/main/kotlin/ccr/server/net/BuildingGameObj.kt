package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject

// C++: BuildingGameObj (building.cpp)
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj
// NOTE: BuildingGameObj does NOT extend PhysicalGameObj — it has its own position/sphere.
//
// definitionId is normally written by NetworkGameObjectFactoryClass::Prep_Packet before
// Export_Creation is called in C++. In our Kotlin approach (no factory layer), we write it
// explicitly at the start of exportCreation, matching PhysicalGameObj's convention.
open class BuildingGameObj(
    definitionId: Int,
    val position: Vector3,
    val sphereCenter: Vector3,
    val sphereRadius: Float,
    health: Float = 5000f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    var isDestroyed: Boolean = false,
    var isPowerOn: Boolean = true,
    var currentState: Int = 0,   // BuildingStateClass::HEALTH100_POWERON
    val playerType: Int = 0,
) : DamageableGameObj(definitionId, health, shieldStrength, shieldType) {

    // Set healthMax to match initial health
    init { healthMax = health; shieldStrengthMax = shieldStrength }

    // MCT alternate armor type save ID (from BuildingGameObjDef.mctSkin).
    // Used by C4GameObj.detonate() when stuckMct = true.
    var mctSkinSaveId: Int = 0

    // C++: BuildingGameObj::BaseController — set by cncInitialize
    var baseController: BaseControllerClass? = null

    // Context reference for think() loops that need frameDeltaSeconds or starList
    var gameContext: ccr.server.GameContext? = null

    // C++: BuildingGameObj::Apply_Damage (override DamageableGameObj)
    override fun applyDamage(damage: Float) {
        if (isDestroyed) return
        val oldHealth = health
        super.applyDamage(damage)
        if (oldHealth != health) {
            setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
        }
        updateState()
    }

    // C++: BuildingGameObj::Update_State
    fun updateState(forceUpdate: Boolean = false) {
        val healthPct = if (healthMax > 0f) 100f * health / healthMax else 0f
        val healthState = percentageToHealthState(healthPct)
        val newState = composeState(healthState, isPowerOn)
        if (newState != currentState || forceUpdate) {
            currentState = newState
            setObjectDirtyBit(NetworkObject.BIT_RARE, true)
        }
    }

    // C++: BuildingGameObj::On_Destroyed — called when health reaches 0
    open fun onDestroyed() {
        isDestroyed = true
        baseController?.onBuildingDestroyed(this)
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    override fun completelyDamaged() {
        onDestroyed()
    }

    // C++: BuildingGameObj::Enable_Power
    fun enablePower(on: Boolean) {
        if (isPowerOn != on) {
            isPowerOn = on
            updateState()
        }
    }

    // C++: BuildingGameObj::CnC_Initialize (base)
    open fun cncInitialize(base: BaseControllerClass) {
        baseController = base
    }

    // Reset for new round — restore full health and power
    fun resetToFull() {
        health = healthMax
        shieldStrength = shieldStrengthMax
        isDestroyed = false
        isPowerOn = true
        updateState(forceUpdate = true)
        setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    // C++: BuildingGameObj::Export_Creation — calls DamageableGameObj::Export_Creation (empty chain),
    // then writes position and CollectionSphere (center + radius).
    // definitionId is written here because our Kotlin design has no factory Prep_Packet layer.
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)  // DamageableGameObj → BaseGameObj → NetworkObject — all empty
        packet.addInt(definitionId)                               // Written by factory Prep_Packet in C++
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)    // Position.X
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)    // Position.Y
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)    // Position.Z
        packet.addFloat(sphereCenter.x, BITPACK_WORLD_POSITION_X)  // CollectionSphere.Center.X
        packet.addFloat(sphereCenter.y, BITPACK_WORLD_POSITION_Y)  // CollectionSphere.Center.Y
        packet.addFloat(sphereCenter.z, BITPACK_WORLD_POSITION_Z)  // CollectionSphere.Center.Z
        packet.addFloat(sphereRadius, BITPACK_BUILDING_RADIUS)      // CollectionSphere.Radius
    }

    // C++: BuildingGameObj::Export_Rare — calls DamageableGameObj::Export_Rare (empty chain),
    // then writes IsDestroyed, IsPowerOn, CurrentState.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)  // DamageableGameObj → BaseGameObj → NetworkObject — all empty
        packet.addBool(isDestroyed)                               // IsDestroyed
        packet.addBool(isPowerOn)                                 // IsPowerOn
        packet.addInt(currentState, BITPACK_BUILDING_STATE)       // CurrentState
    }

    companion object {
        // C++: BuildingStateClass health thresholds
        fun percentageToHealthState(pct: Float): Int = when {
            pct <= 0f  -> 4   // HEALTH_0
            pct <= 25f -> 3   // HEALTH_25
            pct <= 50f -> 2   // HEALTH_50
            pct <= 75f -> 1   // HEALTH_75
            else       -> 0   // HEALTH_100
        }
        // Power state offset: 0..4 = power on, 5..9 = power off
        fun composeState(healthState: Int, powerOn: Boolean): Int =
            healthState + if (!powerOn) 5 else 0
    }
}
