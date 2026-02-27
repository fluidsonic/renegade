package ccr.server.level.ldd

import ccr.math.Vector3
import ccr.server.level.Matrix3D
import ccr.server.level.OBBox
import ccr.server.level.Sphere

// ─── Shared instance-state data classes ────────────────────────────────────────

/** DefenseObjectClass instance state (damage.cpp, CHUNKID_VARIABLES=914991020) */
data class LoadedDefenseObject(
    val health: Float = 0f,
    val healthMax: Float = 0f,
    val skinSaveId: Int = 0,
    val shieldStrength: Float = 0f,
    val shieldStrengthMax: Float = 0f,
    val shieldTypeSaveId: Int = 0,
    val damagePoints: Float = 0f,
    val deathPoints: Float = 0f,
)

/** One weapon entry in an ArmedGameObj weapon bag (armedgameobj.cpp, ARMED_CHUNKID_WEAPONBAG=418001843) */
data class LoadedWeaponBagEntry(
    val definitionId: Int = 0,
    val rounds: Int = 0,
    val inventoryRounds: Int = 0,
)

// ─── Sealed base ────────────────────────────────────────────────────────────────

sealed class LoadedGameObject {
    abstract val definitionId: Int
    abstract val transform: Matrix3D
    abstract val networkId: Int
}

// ─── Per-type data classes ──────────────────────────────────────────────────────

/**
 * BuildingGameObj — extends DamageableGameObj directly (no PhysicalGameObj parent).
 * Position comes from BuildingGameObj CHUNKID_VARIABLES (207011121), micro 1.
 */
data class LoadedBuildingGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,   // IDENTITY with position in column 3
    override val networkId: Int,
    val playerType: Int = -2,           // DamageableGameObj CHUNKID_VARIABLES (207011214), micro 1
    val isHealthBarDisplayed: Boolean = true,  // micro 2
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val isPowerOn: Boolean = true,      // BuildingGameObj CHUNKID_VARIABLES (207011121), micro 2
    val collectionSphere: Sphere = Sphere(Vector3(0f, 0f, 0f), 50f),  // micro 3, 16 bytes
    val factoryChunkId: UInt = 0u,
) : LoadedGameObject()

/**
 * SoldierGameObj — full instance state from soldier.cpp and parent chain.
 */
data class LoadedSoldierGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val detonateC4: Boolean = false,        // SoldierGameObj CHUNKID_VARIABLES (909991657), micro 1
    val legFacing: Float = 0f,              // micro 2
    val syncLegs: Boolean = true,           // micro 3
    val animName: String = "",              // micro 4
    val vehicleId: Int = 0,                 // micro 5
    val innateEnableBits: Int = 0xFF,       // micro 8
    val lastLegMode: Int = 0,               // micro 10
    val headLookDuration: Float = 0f,       // micro 11
    val headRotation: Float = 0f,           // micro 12
    val lookTarget: Vector3 = Vector3(0f, 0f, 0f),  // micro 13
    val keyRing: Int = 0,                   // micro 15
    val aiState: Int = 0,                   // micro 16
    val lookAngle: Float = 0f,              // micro 18
    val lookAngleTimer: Float = 0f,         // micro 19
    val specialDamageMode: Int = 0,         // micro 22
    val specialDamageTimer: Float = 0f,     // micro 23
    val isUsingGhostCollision: Boolean = false,  // micro 24
    val humanState: Int = 0,                // HumanStateClass (909991659), micro 1
    val weaponBag: List<LoadedWeaponBagEntry> = emptyList(),
    val targetingPosition: Vector3 = Vector3(0f, 0f, 0f),
    val controlEnabled: Boolean = true,     // SmartGameObj CHUNKID_CONTROL (910991115)
    val playerName: String = "",
) : LoadedGameObject()

/**
 * VehicleGameObj — full instance state from vehicle.cpp and parent chain.
 */
data class LoadedVehicleGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val turretTurn: Float = 0f,         // VehicleGameObj CHUNKID_VARIABLES (923991633), micro 5
    val barrelTilt: Float = 0f,         // micro 6
    val transitionsEnabled: Boolean = true,  // micro 7
    val occupiedSeats: Int = 0,         // micro 8
    val numSeats: Int = 0,              // micro 9
    val weaponBag: List<LoadedWeaponBagEntry> = emptyList(),
    val targetingPosition: Vector3 = Vector3(0f, 0f, 0f),
) : LoadedGameObject()

/**
 * SimpleGameObj — no own instance variables (simplegameobj.cpp).
 */
data class LoadedSimpleGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val modelName: String = "",
    val playerType: Int = -2,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
) : LoadedGameObject()

/**
 * ScriptZoneGameObj — bounding box and player type filter.
 */
data class LoadedScriptZoneGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val boundingBox: OBBox = OBBox.EMPTY,  // CHUNKID_VARIABLES (922991807), micro 1, 60 bytes
    val playerType: Int = -2,              // micro 2 (with legacy remapping)
    val scripts: List<ScriptAttachment> = emptyList(),
) : LoadedGameObject()

/**
 * C4GameObj — placed C4 explosive (c4.cpp).
 */
data class LoadedC4GameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val ammoDefId: Int = 0,              // C4 CHUNKID_VARIABLES (922991751), micro 3
    val detonationMode: Int = 0,         // micro 4
    val timer: Float = 0f,               // micro 5
    val stuck: Boolean = false,          // micro 6
    val stuckOffset: Vector3 = Vector3(0f, 0f, 0f),  // micro 7, 12 bytes
    val stuckMct: Boolean = false,       // micro 8
    val stuckBone: String = "",          // micro 9
    val stuckStaticAnimObjId: Int = 0,   // micro 10
    val stuckToObject: Int = 0,          // micro 11
    val age: Float = 0f,                 // micro 12
) : LoadedGameObject()

/**
 * BeaconGameObj — proximity beacon (beacongameobj.cpp).
 */
data class LoadedBeaconGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val state: Int = 0,                  // BeaconGameObj CHUNKID_VARIABLES (0x00219044), micro 1
    val stateTimer: Float = 0f,          // micro 2
    val detonateTimer: Float = 0f,       // micro 3
    val preDetonateTimer: Float = 0f,    // micro 4
    val isArmed: Boolean = false,        // micro 5
) : LoadedGameObject()

/**
 * PowerUpGameObj — power-up object (powerup.cpp).
 */
data class LoadedPowerUpGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val state: Int = 0,                  // PowerUpGameObj CHUNKID_VARIABLES (927991636), micro 1
    val stateEndTimer: Float = 0f,       // micro 2
) : LoadedGameObject()

/**
 * TransitionGameObj — ladder/transition object (transitiongameobj.cpp).
 * No DamageableGameObj parent — extends ScriptableGameObj directly.
 */
data class LoadedTransitionGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,   // from micro 1 (Matrix3D, 48 bytes)
    override val networkId: Int,
    val ladderIndex: Int = 0,           // CHUNKID_VARIABLES (1111991207), micro 2
) : LoadedGameObject()

/**
 * CinematicGameObj — cinematic object (cinematicgameobj.cpp).
 * No own instance variables beyond the base class chain.
 */
data class LoadedCinematicGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
) : LoadedGameObject()

/**
 * DamageZoneGameObj — damage zone (damagezone.cpp).
 * No DamageableGameObj parent — extends ScriptableGameObj directly.
 */
data class LoadedDamageZoneGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val boundingBox: OBBox = OBBox.EMPTY,  // CHUNKID_VARIABLES (626000948), micro 1, 60 bytes
    val damageTimer: Float = 0f,           // micro 2
) : LoadedGameObject()

/**
 * SpecialEffectsGameObj — special effects object (specialeffectsgameobj.cpp).
 */
data class LoadedSpecialEffectsGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val lifeRemaining: Float = 0f,      // CHUNKID_VARIABLES (0x09010237), micro 1
    val isInitialized: Boolean = false, // micro 2
) : LoadedGameObject()

/**
 * SAMSiteGameObj — SAM site (uses same chunk IDs as C4).
 */
data class LoadedSAMSiteGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val state: Int = 0,                 // CHUNKID_VARIABLES (922991751), micro 2
    val timer: Float = 0f,              // micro 3
) : LoadedGameObject()

/**
 * SakuraBossGameObj — Sakura helicopter boss (sakurabossgameobj.cpp).
 */
data class LoadedSakuraBossGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val weaponBag: List<LoadedWeaponBagEntry> = emptyList(),
    val targetingPosition: Vector3 = Vector3(0f, 0f, 0f),
    // Boss-specific state — parsed from CHUNKID_VARIABLES (0x0907045A)
    val availableWeapons: Int = 0,
    val overallState: Int = 0,
    val nextOverallState: Int = 0,
    val faceTargetInTransition: Boolean = false,
    val rocketLauncherState: Int = 0,
    val gattlingGunState: Int = 0,
) : LoadedGameObject()

/**
 * MendozaBossGameObj — Mendoza boss (mendozabossgameobj.cpp).
 */
data class LoadedMendozaBossGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val weaponBag: List<LoadedWeaponBagEntry> = emptyList(),
    val targetingPosition: Vector3 = Vector3(0f, 0f, 0f),
) : LoadedGameObject()

/**
 * RaveshawBossGameObj — Raveshaw boss (raveshawbossgameobj.cpp).
 */
data class LoadedRaveshawBossGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerType: Int = -2,
    val isHealthBarDisplayed: Boolean = true,
    val defense: LoadedDefenseObject = LoadedDefenseObject(),
    val weaponBag: List<LoadedWeaponBagEntry> = emptyList(),
    val targetingPosition: Vector3 = Vector3(0f, 0f, 0f),
) : LoadedGameObject()

/** Fallback for unrecognized factory chunk IDs. */
data class UnknownGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val factoryChunkId: UInt,
) : LoadedGameObject()
