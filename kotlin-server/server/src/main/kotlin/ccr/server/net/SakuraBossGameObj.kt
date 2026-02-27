package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.math.degToRadF
import ccr.server.defs.DefenseObjectDefClass
import ccr.server.defs.SakuraBossGameObjDef
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

// C++: SakuraBossGameObj : public VehicleGameObj (sakurabossgameobj.h / sakurabossgameobj.cpp)
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//              → PhysicalGameObj → ArmedGameObj → SmartGameObj → VehicleGameObj → SakuraBossGameObj
// Singleplayer-only helicopter boss with waypath-following flight, gatling gun, and rocket launchers.
open class SakuraBossGameObj : VehicleGameObj() {

    // -------------------------------------------------------------------------
    // Bone / animation name constants (from C++ static consts)
    // -------------------------------------------------------------------------
    companion object {
        // C++: CHUNKID_GAME_OBJECT_SAKURA_BOSS from combatchunkid.h
        const val CHUNK_ID: UInt = 0x00040133u

        // -----------------------------------------------------------------------
        // Bone / animation names (C++ static const char*)
        // -----------------------------------------------------------------------
        private const val LEFT_ROCKET_BONE       = "BN_MISSILE_WNGL"
        private const val RIGHT_ROCKET_BONE      = "BN_MISSILE_WNGR"
        private const val ROCKET_DOOR_ANIMATION  = "V_COMMANCHE.A_COMM_MISL"
        private const val GATLING_MUZZLE         = "MUZZLEA0"
        private const val LEFT_ROCKET_MUZZLE     = "MUZZLEB01"
        private const val RIGHT_ROCKET_MUZZLE    = "MUZZLEB0"
        private const val LEFT_ROCKET_MESH       = "V_COM_MISSILEL"
        private const val RIGHT_ROCKET_MESH      = "V_COM_MISSILER"
        private const val ROCKETS_IN_FRAME_NUM   = 0.0f
        private const val ROCKETS_OUT_FRAME_NUM  = 10.0f

        // -----------------------------------------------------------------------
        // Waypath IDs (C++ const int)
        // -----------------------------------------------------------------------
        private const val WID_LONG_STRAFING_RUN      = 1700009
        private const val WID_LONG_STRAFING_RUN2     = 1700016
        private const val WID_TO_LIFT_AREA_SOUTH     = 1700018
        private const val WID_TO_LIFT_AREA_NORTH     = 1700019
        private const val WID_TIBFIELD_TO_VSTRAFE    = 1700004
        private const val WID_LSTRAF2_TO_VSTRAFE     = 1700017
        private const val WID_VSTRAFE_TO_TIBFIELD    = 1700006
        private const val WID_LSTRAFE_TO_TIBFIELD    = 1700002
        private const val WID_REFINERY_TO_TIBFIELD   = 1700012
        private const val WID_LSTRAFE_TO_REFINERY    = 1700010
        private const val WID_VSTRAFE_TO_REFINERY    = 1700014
        private const val WID_PPLANT_TO_REFINERY     = 1700007
        private const val WID_VSTRAFE_TO_PPLANT      = 1700015
        private const val WID_REFINERY_TO_PPLANT     = 1700008
        private const val WID_REFINERY_TO_LSTRAFE2   = 1700003
        private const val WID_VSTRAFE_TO_LSTRAFE2    = 1700005
        private const val WID_REFINERY_TO_LSTRAFE    = 1700011
        private const val WID_VSTRAFE_TO_LSTRAFE     = 1700013

        // -----------------------------------------------------------------------
        // Weapon flags (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val WEAPON_GATLING_GUN   = 0x01
        private const val WEAPON_ROCKETS_LEFT  = 0x02
        private const val WEAPON_ROCKETS_RIGHT = 0x04
        private const val WEAPON_ALL           = WEAPON_GATLING_GUN or WEAPON_ROCKETS_LEFT or WEAPON_ROCKETS_RIGHT

        // -----------------------------------------------------------------------
        // Rocket launcher state (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val ROCKET_STATE_OPENING  = 1
        private const val ROCKET_STATE_OPEN     = 2
        private const val ROCKET_STATE_CLOSING  = 3
        private const val ROCKET_STATE_CLOSED   = 4
        private const val ROCKET_STATE_FIRING   = 5

        // -----------------------------------------------------------------------
        // Gatling gun state (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val GATLING_STATE_NORMAL    = 1
        private const val GATLING_STATE_REVVING_UP = 2
        private const val GATLING_STATE_FIRING    = 3

        // -----------------------------------------------------------------------
        // Sakura state (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val SAKURA_STATE_NORMAL   = 1
        private const val SAKURA_STATE_TAUNTING = 2

        // -----------------------------------------------------------------------
        // Vehicle state (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val VEHICLE_STATE_HOVERING = 1
        private const val VEHICLE_STATE_MOVING   = 2

        // -----------------------------------------------------------------------
        // Overall AI state (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val STATE_CIRCLE_POWER_PLANT    = 1
        private const val STATE_CIRCLE_REFINERY       = 2
        private const val STATE_ATTACK_LIFT_AREA      = 3
        private const val STATE_LONG_STRAFING_RUN     = 4
        private const val STATE_LAND_IN_TIBERIUM_FIELD = 5
        private const val STATE_VALLEY_STRAFE         = 6
        private const val STATE_LONG_STRAFING_RUN2    = 7
        private const val STATE_IN_TRANSITION         = 8

        // -----------------------------------------------------------------------
        // Rocket side index (C++ anonymous enum)
        // -----------------------------------------------------------------------
        private const val ROCKET_RIGHT = 0
        private const val ROCKET_LEFT  = 1

        // -----------------------------------------------------------------------
        // Taunt constants
        // -----------------------------------------------------------------------
        private const val MAX_TAUNTS = 6
        private val TAUNT_IDS = arrayOf(
            "IDS_SAKURA_BOSS_TAUNT1",
            "IDS_SAKURA_BOSS_TAUNT2",
            "IDS_SAKURA_BOSS_TAUNT3",
            "IDS_SAKURA_BOSS_TAUNT4",
            "IDS_SAKURA_BOSS_TAUNT5",
            "IDS_SAKURA_BOSS_TAUNT6",
        )

        // -----------------------------------------------------------------------
        // Save/load chunk IDs (C++ local enum, starting at 0x09070459)
        // -----------------------------------------------------------------------
        private const val CHUNKID_PARENT             = 0x09070459
        private const val CHUNKID_VARIABLES          = 0x0907045A
        private const val CHUNKID_ROCKETL_DEFENSE_OBJ = 0x0907045B
        private const val CHUNKID_ROCKETR_DEFENSE_OBJ = 0x0907045C
        private const val CHUNKID_LAST_DAMAGER        = 0x0907045D
        private const val CHUNKID_CURR_TARGET         = 0x0907045E
        private const val CHUNKID_PILOT               = 0x0907045F
        private const val CHUNKID_PATH                = 0x09070460

        // micro-chunk variable IDs
        private const val VARID_AVAILABLE_WEAPONS                   = 1
        private const val VARID_OVERALL_STATE                       = 2
        private const val VARID_NEXT_OVERALL_STATE                  = 3
        private const val VARID_FACE_TARGET_IN_TRANSITION           = 4
        private const val VARID_ROCKET_LAUNCHER_STATE               = 5
        private const val VARID_GATTLING_GUN_STATE                  = 6
        private const val VARID_SAKURA_STATE                        = 7
        private const val VARID_VEHICLE_STATE                       = 8
        private const val VARID_MOVE_TO_LOCATION                    = 9
        private const val VARID_GATTLING_GUN_STATE_TIME_LEFT        = 10
        private const val VARID_SAKURA_TAUNT_TIME_LEFT              = 11
        private const val VARID_ROCKET_LAUNCHER_STATE_TIME_LEFT     = 12
        private const val VARID_OVERALL_STATE_TIME_LEFT             = 13
        private const val VARID_TARGET_TIME_LEFT                    = 14
        private const val VARID_CURRENT_HEALTH                      = 15
        private const val VARID_TARGET_ANGLE                        = 16
        private const val VARID_TARGET_POS                          = 17
        private const val VARID_IS_ATTACKING                        = 18
        private const val VARID_CURRENT_MUZZLE_TM                   = 19
        private const val VARID_CURRENT_MUZZLE_INDEX                = 20
        private const val VARID_TILT_ANGLE                          = 21
        private const val VARID_CHOPPER_TILT_BONE_INDEX             = 22
    }

    // -------------------------------------------------------------------------
    // Fields matching C++ SakuraBossGameObj members
    // -------------------------------------------------------------------------

    // C++: int AvailableWeapons
    private var availableWeapons: Int = WEAPON_ALL

    // C++: int OverallState
    private var overallState: Int = STATE_ATTACK_LIFT_AREA

    // C++: int NextOverallState
    private var nextOverallState: Int = STATE_ATTACK_LIFT_AREA

    // C++: bool FaceTargetInTransition
    private var faceTargetInTransition: Boolean = false

    // C++: int RocketLauncherState
    private var rocketLauncherState: Int = ROCKET_STATE_CLOSED

    // C++: int GattlingGunState
    private var gattlingGunState: Int = GATLING_STATE_NORMAL

    // C++: int SakuraState
    private var sakuraState: Int = SAKURA_STATE_NORMAL

    // C++: int VehicleState
    private var vehicleState: Int = VEHICLE_STATE_HOVERING

    // C++: DefenseObjectClass LeftRocketDefenseObject
    private val leftRocketDefenseObject: DefenseObjectClass = DefenseObjectClass()

    // C++: DefenseObjectClass RightRocketDefenseObject
    private val rightRocketDefenseObject: DefenseObjectClass = DefenseObjectClass()

    // C++: GameObjReference LastDamager
    private val lastDamager: GameObjReference = GameObjReference()

    // C++: GameObjReference CurrentTarget
    private val currentTarget: GameObjReference = GameObjReference()

    // C++: Vector3 MoveToLocation
    private var moveToLocation: Vector3 = Vector3()

    // C++: float GattlingGunStateTimeLeft
    private var gattlingGunStateTimeLeft: Float = 0f

    // C++: float SakuraTauntTimeLeft
    private var sakuraTauntTimeLeft: Float = 0f

    // C++: float RocketLauncherStateTimeLeft
    private var rocketLauncherStateTimeLeft: Float = 0f

    // C++: float OverallStateTimeLeft
    private var overallStateTimeLeft: Float = 10.0f

    // C++: float TargetTimeLeft
    private var targetTimeLeft: Float = 0f

    // C++: float CurrentHealth
    private var currentHealth: Float = 1.0f

    // C++: float TargetAngle
    private var targetAngle: Float = 0f

    // C++: Vector3 TargetPos
    private var targetPos: Vector3 = Vector3()

    // C++: bool IsAttacking
    private var isAttacking: Boolean = false

    // C++: WeaponClass* GattlingGun (server-side stub — tracks trigger state)
    // FIXME: WeaponClass not fully ported for AI use; set isPrimaryTriggered only
    private var gattlingGun: WeaponClass? = null

    // C++: WeaponClass* RockerLauncherLeft
    private var rockerLauncherLeft: WeaponClass? = null

    // C++: WeaponClass* RockerLauncherRight
    private var rockerLauncherRight: WeaponClass? = null

    // C++: Matrix3D CurrentMuzzleTM
    private var currentMuzzleTM: Matrix3D = Matrix3D.IDENTITY

    // C++: int CurrentMuzzleIndex
    private var currentMuzzleIndex: Int = 0

    // C++: PilotClass Pilot — AI flight control
    // FIXME: PilotClass not yet ported; replaced with stub tracking mode/destination/target
    private val pilot: PilotClassStub = PilotClassStub()

    // C++: PathClass* Path — waypath traversal state
    // FIXME: PathClass not yet ported; replaced with stub
    private var path: PathClassStub? = PathClassStub()

    // C++: int TauntList[MAX_TAUNTS]
    private val tauntList: IntArray = IntArray(MAX_TAUNTS)

    // C++: int AvailableTaunts
    private var availableTaunts: Int = 0xFFFFFF

    // C++: float TiltAngle
    private var tiltAngle: Float = 0f

    // C++: int ChopperTiltBoneIndex
    private var chopperTiltBoneIndex: Int = 0

    // -------------------------------------------------------------------------
    // Constructor initialisation (mirrors C++ constructor body)
    // -------------------------------------------------------------------------
    init {
        shuffleTauntList()
        // C++: explicit override back to 1..6 after shuffle
        tauntList[0] = 1
        tauntList[1] = 2
        tauntList[2] = 3
        tauntList[3] = 4
        tauntList[4] = 5
        tauntList[5] = 6
    }

    // -------------------------------------------------------------------------
    // Definition access
    // -------------------------------------------------------------------------

    // C++: const SakuraBossGameObjDef& Get_Definition() const
    fun getSakuraBossDefinition(): SakuraBossGameObjDef = definition as SakuraBossGameObjDef

    // C++: SakuraBossGameObj* As_SakuraBossGameObj() { return this; }
    fun asSakuraBossGameObj(): SakuraBossGameObj = this

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    // C++: virtual void Init()
    override fun init() {
        init(getSakuraBossDefinition())
    }

    // C++: void Init(const SakuraBossGameObjDef& definition)
    fun init(def: SakuraBossGameObjDef) {
        super.init()

        // C++: Initialize the defense for the left and right rocket launchers
        // Pass NULL for owner so we don't think we have killed Sakura too early
        leftRocketDefenseObject.init(def.rocketsDefense, this)
        leftRocketDefenseObject.owner = null
        rightRocketDefenseObject.init(def.rocketsDefense, this)
        rightRocketDefenseObject.owner = null

        // C++: Create the gatling gun
        // C++: WeaponManager::Find_Weapon_Definition / new WeaponClass — client-side weapon creation skipped server-side
        if (def.gattlingGunDefId != 0) {
            gattlingGun = WeaponClass(definitionId = def.gattlingGunDefId)
            gattlingGun!!.owner = this
        }

        // C++: Create the rocket launchers
        if (def.rocketLauncherDefId != 0) {
            rockerLauncherLeft  = WeaponClass(definitionId = def.rocketLauncherDefId)
            rockerLauncherRight = WeaponClass(definitionId = def.rocketLauncherDefId)
            rockerLauncherLeft!!.owner  = this
            rockerLauncherRight!!.owner = this
        }

        // C++: Enable_Engine(true) — client-side only
        // C++: Pilot.Initialize(this) / Set_Arrived_Dist / Set_Aggressivness / Set_Is_Exact_Z_Important
        pilot.arrivedDist  = 1.0f
        pilot.aggressivness = 1.0f
        pilot.isExactZImportant = true

        // C++: ChopperTiltBoneIndex = Peek_Model()->Get_Bone_Index("V_CHOPPER")
        // Model not available server-side; stays 0 (disabled)
        chopperTiltBoneIndex = 0
    }

    // -------------------------------------------------------------------------
    // Save / Load
    // -------------------------------------------------------------------------

    // C++: bool Save(ChunkSaveClass& csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_ROCKETL_DEFENSE_OBJ)
        leftRocketDefenseObject.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_ROCKETR_DEFENSE_OBJ)
        rightRocketDefenseObject.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_LAST_DAMAGER)
        lastDamager.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_CURR_TARGET)
        currentTarget.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_PILOT)
        pilot.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        saveVariables(csave)
        csave.endChunk()

        if (path != null) {
            csave.beginChunk(CHUNKID_PATH)
            path!!.save(csave)
            csave.endChunk()
        }

        return true
    }

    // C++: bool Load(ChunkLoadClass& cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT              -> super.load(cload)
                CHUNKID_ROCKETL_DEFENSE_OBJ -> leftRocketDefenseObject.load(cload)
                CHUNKID_ROCKETR_DEFENSE_OBJ -> rightRocketDefenseObject.load(cload)
                CHUNKID_LAST_DAMAGER        -> lastDamager.load(cload)
                CHUNKID_CURR_TARGET         -> currentTarget.load(cload)
                CHUNKID_PILOT               -> pilot.load(cload)
                CHUNKID_PATH                -> {
                    path = PathClassStub()
                    path!!.load(cload)
                }
                CHUNKID_VARIABLES           -> loadVariables(cload)
                else -> { /* Unrecognized SakuraBossGameObj chunk ID — skip */ }
            }
            cload.closeChunk()
        }

        // C++: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        onPostLoad()
        return true
    }

    // C++: void On_Post_Load()
    override fun onPostLoad() {
        // C++: Re-create weapons from def (after load, pointers are gone)
        val def = getSakuraBossDefinition()

        if (def.gattlingGunDefId != 0) {
            gattlingGun = WeaponClass(definitionId = def.gattlingGunDefId)
            gattlingGun!!.owner = this
        }

        if (def.rocketLauncherDefId != 0) {
            rockerLauncherLeft  = WeaponClass(definitionId = def.rocketLauncherDefId)
            rockerLauncherRight = WeaponClass(definitionId = def.rocketLauncherDefId)
            rockerLauncherLeft!!.owner  = this
            rockerLauncherRight!!.owner = this
        }

        // C++: ChopperTiltBoneIndex = Peek_Model()->Get_Bone_Index("V_CHOPPER")
        // Model not available server-side; stays 0 (disabled)
        chopperTiltBoneIndex = 0
    }

    // C++: void Save_Variables(ChunkSaveClass& csave)
    private fun saveVariables(csave: ChunkSaveClass) {
        csave.writeMicroChunk(VARID_AVAILABLE_WEAPONS,                availableWeapons)
        csave.writeMicroChunk(VARID_OVERALL_STATE,                    overallState)
        csave.writeMicroChunk(VARID_NEXT_OVERALL_STATE,               nextOverallState)
        csave.writeMicroChunk(VARID_FACE_TARGET_IN_TRANSITION,        faceTargetInTransition)
        csave.writeMicroChunk(VARID_ROCKET_LAUNCHER_STATE,            rocketLauncherState)
        csave.writeMicroChunk(VARID_GATTLING_GUN_STATE,               gattlingGunState)
        csave.writeMicroChunk(VARID_SAKURA_STATE,                     sakuraState)
        csave.writeMicroChunk(VARID_VEHICLE_STATE,                    vehicleState)
        csave.writeMicroChunk(VARID_MOVE_TO_LOCATION,                 moveToLocation)
        csave.writeMicroChunk(VARID_GATTLING_GUN_STATE_TIME_LEFT,     gattlingGunStateTimeLeft)
        csave.writeMicroChunk(VARID_SAKURA_TAUNT_TIME_LEFT,           sakuraTauntTimeLeft)
        csave.writeMicroChunk(VARID_ROCKET_LAUNCHER_STATE_TIME_LEFT,  rocketLauncherStateTimeLeft)
        csave.writeMicroChunk(VARID_OVERALL_STATE_TIME_LEFT,          overallStateTimeLeft)
        csave.writeMicroChunk(VARID_TARGET_TIME_LEFT,                 targetTimeLeft)
        csave.writeMicroChunk(VARID_CURRENT_HEALTH,                   currentHealth)
        csave.writeMicroChunk(VARID_TARGET_ANGLE,                     targetAngle)
        csave.writeMicroChunk(VARID_TARGET_POS,                       targetPos)
        csave.writeMicroChunk(VARID_IS_ATTACKING,                     isAttacking)
        csave.writeMicroChunk(VARID_CURRENT_MUZZLE_TM,                currentMuzzleTM)
        csave.writeMicroChunk(VARID_CURRENT_MUZZLE_INDEX,             currentMuzzleIndex)
        csave.writeMicroChunk(VARID_TILT_ANGLE,                       tiltAngle)
        csave.writeMicroChunk(VARID_CHOPPER_TILT_BONE_INDEX,          chopperTiltBoneIndex)
    }

    // C++: void Load_Variables(ChunkLoadClass& cload)
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                VARID_AVAILABLE_WEAPONS                -> availableWeapons               = cload.readInt()
                VARID_OVERALL_STATE                    -> overallState                    = cload.readInt()
                VARID_NEXT_OVERALL_STATE               -> nextOverallState                = cload.readInt()
                VARID_FACE_TARGET_IN_TRANSITION        -> faceTargetInTransition          = cload.readBool()
                VARID_ROCKET_LAUNCHER_STATE            -> rocketLauncherState             = cload.readInt()
                VARID_GATTLING_GUN_STATE               -> gattlingGunState                = cload.readInt()
                VARID_SAKURA_STATE                     -> sakuraState                     = cload.readInt()
                VARID_VEHICLE_STATE                    -> vehicleState                    = cload.readInt()
                VARID_MOVE_TO_LOCATION                 -> moveToLocation                  = cload.readVector3()
                VARID_GATTLING_GUN_STATE_TIME_LEFT     -> gattlingGunStateTimeLeft        = cload.readFloat()
                VARID_SAKURA_TAUNT_TIME_LEFT           -> sakuraTauntTimeLeft             = cload.readFloat()
                VARID_ROCKET_LAUNCHER_STATE_TIME_LEFT  -> rocketLauncherStateTimeLeft     = cload.readFloat()
                VARID_OVERALL_STATE_TIME_LEFT          -> overallStateTimeLeft            = cload.readFloat()
                VARID_TARGET_TIME_LEFT                 -> targetTimeLeft                  = cload.readFloat()
                VARID_CURRENT_HEALTH                   -> currentHealth                   = cload.readFloat()
                VARID_TARGET_ANGLE                     -> targetAngle                     = cload.readFloat()
                VARID_TARGET_POS                       -> targetPos                       = cload.readVector3()
                VARID_IS_ATTACKING                     -> isAttacking                     = cload.readBool()
                VARID_CURRENT_MUZZLE_TM                -> currentMuzzleTM                 = cload.readMatrix3D()
                VARID_CURRENT_MUZZLE_INDEX             -> currentMuzzleIndex              = cload.readInt()
                VARID_TILT_ANGLE                       -> tiltAngle                       = cload.readFloat()
                VARID_CHOPPER_TILT_BONE_INDEX          -> chopperTiltBoneIndex            = cload.readInt()
                else -> { /* Unrecognized variable chunk — skip */ }
            }
            cload.closeMicroChunk()
        }
    }

    // -------------------------------------------------------------------------
    // Think / ApplyControl
    // -------------------------------------------------------------------------

    // C++: void Think()
    override fun think() {
        val deltaSeconds = TimeManager.getFrameSeconds()
        // C++: Pilot.Think()
        pilot.think(deltaSeconds)
        // C++: VehicleGameObj::Think()
        super.think()

        // C++: Update_Target()
        updateTarget()

        // C++: Update_Decision_Data()
        updateDecisionData()

        // C++: Update individual part states
        updateRocketState()
        updateGattlingGunState()
        updateSakuraState()
        updateVehicleState()
        updateTilt()

        // C++: Decide / Update overall AI state
        decideNewOverallState()
        updateOverallState()

        // C++: Update weapons — keep total rounds at 1000 so they never run dry
        rockerLauncherLeft?.let {
            currentMuzzleIndex = ROCKET_LEFT
            it.setTotalRounds(1000)
            // C++: RockerLauncherLeft->Update() — client-side firing update; not ported
        }
        rockerLauncherRight?.let {
            currentMuzzleIndex = ROCKET_RIGHT
            it.setTotalRounds(1000)
            // C++: RockerLauncherRight->Update() — client-side firing update; not ported
        }
        gattlingGun?.let {
            it.setTotalRounds(1000)
            // C++: GattlingGun->Update() — client-side firing update; not ported
        }
    }

    // C++: void Apply_Control()
    override fun applyControl() {
        // C++: VehicleGameObj::Apply_Control()
        super.applyControl()
    }

    // -------------------------------------------------------------------------
    // Damage
    // -------------------------------------------------------------------------

    // C++: virtual void Apply_Damage_Extended(const OffenseObjectClass&, float, const Vector3&, const char*)
    override fun applyDamageExtended(damager: OffenseObjectClass, scale: Float, direction: Vector3, collisionBoxName: String?) {
        // C++: if (!CombatManager::I_Am_Server()) return;
        // (always server in our Kotlin port)

        // C++: If rockets are out and player shot the rocket, damage only the rocket defense object
        var damagedRockets = false
        if (rocketLauncherState != ROCKET_STATE_CLOSED && collisionBoxName != null) {
            if (collisionBoxName.contains(LEFT_ROCKET_MESH)) {
                damagedRockets = true
                val healthLeft = leftRocketDefenseObject.applyDamage(damager, scale)
                if (healthLeft <= 0) {
                    blowOffWeapon(WEAPON_ROCKETS_LEFT)
                }
            } else if (collisionBoxName.contains(RIGHT_ROCKET_MESH)) {
                damagedRockets = true
                val healthLeft = rightRocketDefenseObject.applyDamage(damager, scale)
                if (healthLeft <= 0) {
                    blowOffWeapon(WEAPON_ROCKETS_RIGHT)
                }
            }
        }

        // C++: If rockets weren't specifically targeted, let damage occur normally
        if (!damagedRockets) {
            super.applyDamageExtended(damager, scale, direction, collisionBoxName)
        }

        // C++: Remember who was last shooting at us
        lastDamager.set(damager.getOwner())
        currentTarget.set(lastDamager.get())
    }

    // -------------------------------------------------------------------------
    // Entry guard
    // -------------------------------------------------------------------------

    // C++: bool Is_Entry_Permitted(SoldierGameObj*) { return false; }
    override fun isEntryPermitted(soldier: SoldierGameObj): Boolean = false

    // -------------------------------------------------------------------------
    // Protected helpers — Weapon management
    // -------------------------------------------------------------------------

    // C++: void Blow_Off_Weapon(int weapon_id)
    private fun blowOffWeapon(weaponId: Int) {
        // C++: Determine where to create the explosion at (bone transform)
        // C++: ExplosionManager::Create_Explosion_At — client-side only (no model server-side)
        // C++: Remove the rocket launcher sub-object from model — client-side only

        // C++: 'Shake' the vehicle (apply impulses to rigid body) — physics not available server-side

        // C++: Make sure the weapon stops firing
        if (weaponId == WEAPON_ROCKETS_LEFT) {
            rockerLauncherLeft?.setPrimaryTriggered(false)
        } else {
            rockerLauncherRight?.setPrimaryTriggered(false)
        }

        // C++: Remove the weapon from the list of available weapons
        availableWeapons = availableWeapons and weaponId.inv()
    }

    // C++: void Open_Rocket_Launchers(bool onoff)
    private fun openRocketLaunchers(onoff: Boolean) {
        // C++: anim_control->Set_Animation(ROCKET_DOOR_ANIMATION)
        // C++: anim_control->Set_Mode(ANIM_MODE_TARGET)
        // C++: anim_control->Set_Target_Frame(onoff ? ROCKETS_OUT_FRAME_NUM : ROCKETS_IN_FRAME_NUM)
        val targetFrame = if (onoff) ROCKETS_OUT_FRAME_NUM else ROCKETS_IN_FRAME_NUM
        val animControl = getAnimControl()
        if (animControl != null) {
            animControl.setAnimation(ROCKET_DOOR_ANIMATION)
            animControl.setMode(ANIM_MODE_TARGET)
            animControl.setTargetFrame(targetFrame.toInt())
        }
    }

    // C++: bool Are_Rocker_Launchers_Ready()
    private fun areRockerLaunchersReady(): Boolean {
        val animControl = getAnimControl() ?: return false
        return animControl.getCurrentFrame() == ROCKETS_OUT_FRAME_NUM.toInt()
    }

    // -------------------------------------------------------------------------
    // Rocket launcher state management
    // -------------------------------------------------------------------------

    // C++: void Set_Rocket_State(int new_state)
    private fun setRocketState(newState: Int) {
        if (rocketLauncherState == newState) return

        when (newState) {
            ROCKET_STATE_OPENING -> {
                // C++: Play door-open sound on each available launcher — client-side only
                // C++: Open_Rocket_Launchers(true)
                openRocketLaunchers(true)
            }
            ROCKET_STATE_CLOSING -> {
                // C++: Close the rocket doors
                openRocketLaunchers(false)
                // C++: Stop firing
                rockerLauncherLeft?.setPrimaryTriggered(false)
                rockerLauncherRight?.setPrimaryTriggered(false)
            }
            ROCKET_STATE_FIRING -> {
                // C++: Pick a random amount of time to fire at the player
                rocketLauncherStateTimeLeft = randomFloat(2.0f, 6.0f)
                rockerLauncherLeft?.setTarget(targetPos)
                rockerLauncherRight?.setTarget(targetPos)
                // C++: Start launching rockets at the player
                if ((availableWeapons and WEAPON_ROCKETS_LEFT) != 0) {
                    rockerLauncherLeft?.setPrimaryTriggered(true)
                }
                if ((availableWeapons and WEAPON_ROCKETS_RIGHT) != 0) {
                    rockerLauncherRight?.setPrimaryTriggered(true)
                }
            }
            ROCKET_STATE_OPEN -> {
                rocketLauncherStateTimeLeft = 2.0f
            }
            ROCKET_STATE_CLOSED -> { /* nothing */ }
        }

        rocketLauncherState = newState
    }

    // C++: void Update_Rocket_State()
    private fun updateRocketState() {
        when (rocketLauncherState) {
            ROCKET_STATE_OPENING -> {
                // C++: Are the rocket launcher doors completely open?
                val animControl = getAnimControl()
                if (animControl != null && animControl.getCurrentFrame() == ROCKETS_OUT_FRAME_NUM.toInt()) {
                    setRocketState(ROCKET_STATE_OPEN)
                }
            }
            ROCKET_STATE_CLOSING -> {
                // C++: Are the rocket launcher doors completely closed?
                val animControl = getAnimControl()
                if (animControl != null && animControl.getCurrentFrame() == ROCKETS_IN_FRAME_NUM.toInt()) {
                    setRocketState(ROCKET_STATE_CLOSED)
                }
            }
            ROCKET_STATE_FIRING -> {
                rockerLauncherLeft?.setTarget(targetPos)
                rockerLauncherRight?.setTarget(targetPos)

                rocketLauncherStateTimeLeft -= TimeManager.getFrameSeconds()
                if (rocketLauncherStateTimeLeft <= 0) {
                    // C++: Stop firing
                    rockerLauncherLeft?.setPrimaryTriggered(false)
                    rockerLauncherRight?.setPrimaryTriggered(false)
                    // C++: Close the rocket doors
                    setRocketState(ROCKET_STATE_CLOSING)
                }
            }
            ROCKET_STATE_OPEN -> {
                rocketLauncherStateTimeLeft -= TimeManager.getFrameSeconds()
                if (rocketLauncherStateTimeLeft <= 0) {
                    // C++: Commence firing
                    setRocketState(ROCKET_STATE_FIRING)
                }
            }
            ROCKET_STATE_CLOSED -> { /* nothing */ }
        }
    }

    // -------------------------------------------------------------------------
    // Gatling gun state management
    // -------------------------------------------------------------------------

    // C++: void Set_Gattling_Gun_State(int new_state)
    private fun setGattlingGunState(newState: Int) {
        if (gattlingGunState == newState) return

        when (newState) {
            GATLING_STATE_NORMAL -> {
                gattlingGun?.setPrimaryTriggered(false)
            }
            GATLING_STATE_REVVING_UP -> {
                // C++: Create sound and use its duration as the rev-up timer — client-side only
                // C++: WWAudioClass::Get_Instance()->Create_Sound(GattlingGunRevSoundDefID) — client only
                // Default to 1 second rev-up if no sound available
                gattlingGunStateTimeLeft = 1.0f
            }
            GATLING_STATE_FIRING -> {
                // C++: Pick a random amount of time to fire at the player
                gattlingGunStateTimeLeft = randomFloat(5.0f, 10.0f)
                // C++: Start firing the gatling gun at the player
                gattlingGun?.setPrimaryTriggered(true)
            }
        }

        gattlingGunState = newState
    }

    // C++: void Update_Gattling_Gun_State()
    private fun updateGattlingGunState() {
        when (gattlingGunState) {
            GATLING_STATE_REVVING_UP -> {
                gattlingGunStateTimeLeft -= TimeManager.getFrameSeconds()
                if (gattlingGunStateTimeLeft <= 0) {
                    // C++: Finished revving up — now start firing
                    setGattlingGunState(GATLING_STATE_FIRING)
                }
            }
            GATLING_STATE_NORMAL -> { /* nothing */ }
            GATLING_STATE_FIRING -> {
                // C++: Target the player
                val target = currentTarget.get()
                if (target != null) {
                    setTargeting(targetPos, true)
                }
                // C++: Stop firing when we've shot at the player for a few seconds
                gattlingGunStateTimeLeft -= TimeManager.getFrameSeconds()
                if (gattlingGunStateTimeLeft <= 0) {
                    setGattlingGunState(GATLING_STATE_NORMAL)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sakura state management
    // -------------------------------------------------------------------------

    // C++: void Set_Sakura_State(int new_state)
    private fun setSakuraState(newState: Int) {
        if (sakuraState == newState) return

        when (newState) {
            SAKURA_STATE_NORMAL -> { /* nothing */ }
            SAKURA_STATE_TAUNTING -> {
                // C++: Find the first available taunt
                var tauntId = 0
                var index = 0
                while (index < MAX_TAUNTS) {
                    if (tauntList[index] != 0) {
                        tauntId = tauntList[index] - 1
                        tauntList[index] = 0
                        break
                    }
                    index++
                }

                // C++: Reshuffle the taunts if necessary
                if (index >= MAX_TAUNTS - 1) {
                    shuffleTauntList()
                }

                // C++: Default to a 2 second taunt delay
                sakuraTauntTimeLeft = 2.0f

                // C++: Lookup the conversation for this taunt and start it — client-side only
                // C++: ConversationMgrClass::Find_Conversation(TAUNT_IDS[tauntId]) — client-side only
                // C++: taunt_conversation->Get_Conversation_Time() would update sakuraTauntTimeLeft
            }
        }

        sakuraState = newState
    }

    // C++: void Update_Sakura_State()
    private fun updateSakuraState() {
        when (sakuraState) {
            SAKURA_STATE_NORMAL -> { /* nothing */ }
            SAKURA_STATE_TAUNTING -> {
                sakuraTauntTimeLeft -= TimeManager.getFrameSeconds()
                if (sakuraTauntTimeLeft <= 0) {
                    setSakuraState(SAKURA_STATE_NORMAL)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Vehicle state management
    // -------------------------------------------------------------------------

    // C++: void Set_Vehicle_State(int new_state)
    private fun setVehicleState(newState: Int) {
        if (vehicleState == newState) return
        // C++: switch(newState) { case HOVERING: case MOVING: break; } — no entry actions
        vehicleState = newState
    }

    // C++: void Update_Vehicle_State()
    private fun updateVehicleState() {
        // C++: Point all weapons at target (or forward if out of arc)
        if (targetAngle > degToRadF(-90f) && targetAngle < degToRadF(90f)) {
            setTargeting(targetPos, true)
        } else {
            // C++: Vector3 target_pos = Get_Transform() * Vector3(100,0,0)
            // Simulate: just target 100 units forward in local space
            val forwardTarget = Vector3(position.x + 100f, position.y, position.z)
            setTargeting(forwardTarget, true)
        }

        when (vehicleState) {
            VEHICLE_STATE_HOVERING -> { /* nothing */ }
            VEHICLE_STATE_MOVING -> {
                // C++: Evaluate next waypath point via PathClass
                // C++: If pilot mode is HOVER → switch to hovering state
                // C++: Otherwise give pilot the next path point
                // FIXME: PathClass not ported — just check if pilot mode changed to HOVER
                if (pilot.mode == PilotClassStub.MODE_HOVER) {
                    setVehicleState(VEHICLE_STATE_HOVERING)
                }
                // FIXME: path.Evaluate_Next_Point / pilot.Set_Next_Point — PathClass not ported
            }
        }
    }

    // -------------------------------------------------------------------------
    // Overall state management
    // -------------------------------------------------------------------------

    // C++: void Set_Overall_State(int new_state)
    private fun setOverallState(newState: Int) {
        if (overallState == newState) return

        when (newState) {
            STATE_CIRCLE_POWER_PLANT -> {
                pilot.setDestination(Vector3(-63.34f, 21.26f, 26.0f))
                pilot.mode = PilotClassStub.MODE_CIRCLE_POINT
                // C++: Pilot.Set_Circle_Bounds(DEG_TO_RADF(100), DEG_TO_RADF(225))
                pilot.circleMin = degToRadF(100f)
                pilot.circleMax = degToRadF(225f)
                pilot.circleDist = 30f
            }
            STATE_CIRCLE_REFINERY -> {
                val dest = Vector3(-166.0f, 40.13f, 42.23f)
                pilot.setDestination(dest)
                pilot.mode = PilotClassStub.MODE_HOVER
                val target = Vector3(dest.x, dest.y - 10f, dest.z)
                pilot.setTarget(target)
            }
            STATE_ATTACK_LIFT_AREA -> {
                val dest = Vector3(-161.78f, 79.03f, 18.88f)
                pilot.setDestination(dest)
                pilot.mode = PilotClassStub.MODE_HOVER
                pilot.setTarget(Vector3(dest.x + 10f, dest.y, dest.z))
            }
            STATE_LONG_STRAFING_RUN2 -> {
                pilot.setMaxSpeed(100.0f)
                doWaypath(WID_LONG_STRAFING_RUN2)
            }
            STATE_LONG_STRAFING_RUN -> {
                pilot.setMaxSpeed(100.0f)
                doWaypath(WID_LONG_STRAFING_RUN)
            }
            STATE_VALLEY_STRAFE -> {
                val dest = Vector3(-84.33f, 77.99f, 26.77f)
                pilot.setDestination(dest)
                pilot.mode = PilotClassStub.MODE_HOVER
            }
            STATE_LAND_IN_TIBERIUM_FIELD -> {
                val dest = Vector3(-107.0f, -10.0f, 15.0f)
                pilot.setDestination(dest)
                pilot.mode = PilotClassStub.MODE_HOVER
            }
            STATE_IN_TRANSITION -> {
                faceTargetInTransition = false
            }
        }

        overallState = newState

        // C++: Force a new state change in a little while
        overallStateTimeLeft = randomFloat(5.0f, 10.0f)
    }

    // C++: bool Should_Change_Overall_State()
    private fun shouldChangeOverallState(): Boolean {
        if (overallState == STATE_IN_TRANSITION || vehicleState == VEHICLE_STATE_MOVING) {
            return false
        }

        // C++: Change states if timer expired
        overallStateTimeLeft -= TimeManager.getFrameSeconds()
        if (overallStateTimeLeft <= 0) {
            return true
        }

        if (currentTarget.get() != null) {
            return when (overallState) {
                STATE_CIRCLE_POWER_PLANT      -> targetPos.z < 12.0f
                STATE_CIRCLE_REFINERY         -> targetPos.x > -91.0f || targetPos.y > 69.0f
                STATE_ATTACK_LIFT_AREA        -> targetPos.x > -100.0f || targetPos.y < 50.0f
                STATE_LAND_IN_TIBERIUM_FIELD  -> targetPos.x > -119.0f && targetPos.x < -95.0f && targetPos.y < 0.0f
                STATE_VALLEY_STRAFE           -> targetPos.y < 20.0f && (targetPos.x < -138.0f || targetPos.x > -78.0f)
                STATE_LONG_STRAFING_RUN       -> targetPos.x > -100.0f && targetPos.y > 35.0f
                STATE_LONG_STRAFING_RUN2      -> targetPos.x > -80.0f || targetPos.y < 10.0f
                else -> false
            }
        }

        return false
    }

    // C++: void Decide_New_Overall_State()
    private fun decideNewOverallState() {
        if (!shouldChangeOverallState()) return

        // C++: If we are going to change states, then stop attacking the target
        stopAttacking()

        // C++: If we've been injured enough, bump up the long strafing run possibility
        if (currentHealth < 0.45f && FreeRandom.getInt(4) == 1) {
            if (FreeRandom.getInt(2) == 0) {
                requestOverallState(STATE_LONG_STRAFING_RUN)
            } else {
                requestOverallState(STATE_LONG_STRAFING_RUN2)
            }
        } else {
            // C++: Based on target position, determine which state to go to
            when {
                targetPos.x < -104.0f && targetPos.y > 69.0f && targetPos.y < 85.8f ->
                    requestOverallState(STATE_ATTACK_LIFT_AREA)

                overallState != STATE_CIRCLE_REFINERY && targetPos.x < -125.0f && targetPos.y > -19.0f ->
                    requestOverallState(STATE_CIRCLE_REFINERY)

                overallState != STATE_CIRCLE_POWER_PLANT && targetPos.x > -84.0f && targetPos.z > 11.0f ->
                    requestOverallState(STATE_CIRCLE_POWER_PLANT)

                overallState != STATE_LAND_IN_TIBERIUM_FIELD && targetPos.y < 39.0f ->
                    requestOverallState(STATE_LAND_IN_TIBERIUM_FIELD)

                overallState != STATE_VALLEY_STRAFE ->
                    requestOverallState(STATE_VALLEY_STRAFE)

                FreeRandom.getInt(5) == 1 -> {
                    if (FreeRandom.getInt(2) == 0) {
                        requestOverallState(STATE_LONG_STRAFING_RUN)
                    } else {
                        requestOverallState(STATE_LONG_STRAFING_RUN2)
                    }
                }

                else -> { /* stay in current state */ }
            }
        }

        overallStateTimeLeft = randomFloat(5.0f, 10.0f)
    }

    // C++: void Request_Overall_State(int new_state)
    private fun requestOverallState(newState: Int) {
        if (overallState == STATE_IN_TRANSITION || overallState == newState) return

        when (newState) {
            STATE_CIRCLE_POWER_PLANT -> {
                when (overallState) {
                    STATE_CIRCLE_REFINERY, STATE_ATTACK_LIFT_AREA -> {
                        doWaypath(WID_REFINERY_TO_PPLANT)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_LONG_STRAFING_RUN, STATE_LAND_IN_TIBERIUM_FIELD, STATE_LONG_STRAFING_RUN2 -> {
                        setOverallState(STATE_CIRCLE_POWER_PLANT)
                    }
                    STATE_VALLEY_STRAFE -> {
                        doWaypath(WID_VSTRAFE_TO_PPLANT)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                }
            }
            STATE_CIRCLE_REFINERY -> {
                when (overallState) {
                    STATE_CIRCLE_POWER_PLANT, STATE_LAND_IN_TIBERIUM_FIELD, STATE_LONG_STRAFING_RUN2 -> {
                        doWaypath(WID_PPLANT_TO_REFINERY)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_ATTACK_LIFT_AREA -> {
                        setOverallState(STATE_CIRCLE_REFINERY)
                    }
                    STATE_VALLEY_STRAFE -> {
                        doWaypath(WID_VSTRAFE_TO_REFINERY)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_LONG_STRAFING_RUN -> {
                        doWaypath(WID_LSTRAFE_TO_REFINERY)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                }
            }
            STATE_LAND_IN_TIBERIUM_FIELD -> {
                when (overallState) {
                    STATE_CIRCLE_REFINERY, STATE_ATTACK_LIFT_AREA -> {
                        doWaypath(WID_REFINERY_TO_TIBFIELD)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_CIRCLE_POWER_PLANT, STATE_LONG_STRAFING_RUN2 -> {
                        setOverallState(STATE_LAND_IN_TIBERIUM_FIELD)
                    }
                    STATE_LONG_STRAFING_RUN -> {
                        doWaypath(WID_LSTRAFE_TO_TIBFIELD)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_VALLEY_STRAFE -> {
                        doWaypath(WID_VSTRAFE_TO_TIBFIELD)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                }
            }
            STATE_VALLEY_STRAFE -> {
                when (overallState) {
                    STATE_CIRCLE_REFINERY, STATE_CIRCLE_POWER_PLANT, STATE_LONG_STRAFING_RUN -> {
                        setOverallState(STATE_VALLEY_STRAFE)
                    }
                    STATE_ATTACK_LIFT_AREA -> {
                        setOverallState(STATE_VALLEY_STRAFE)
                    }
                    STATE_LONG_STRAFING_RUN2 -> {
                        doWaypath(WID_LSTRAF2_TO_VSTRAFE)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_LAND_IN_TIBERIUM_FIELD -> {
                        doWaypath(WID_TIBFIELD_TO_VSTRAFE)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                }
            }
            STATE_ATTACK_LIFT_AREA -> {
                when (overallState) {
                    STATE_CIRCLE_REFINERY, STATE_LONG_STRAFING_RUN, STATE_VALLEY_STRAFE -> {
                        doWaypath(WID_TO_LIFT_AREA_SOUTH)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_CIRCLE_POWER_PLANT, STATE_LONG_STRAFING_RUN2, STATE_LAND_IN_TIBERIUM_FIELD -> {
                        doWaypath(WID_TO_LIFT_AREA_NORTH)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                }
            }
            STATE_LONG_STRAFING_RUN2 -> {
                when (overallState) {
                    STATE_CIRCLE_REFINERY, STATE_ATTACK_LIFT_AREA -> {
                        doWaypath(WID_REFINERY_TO_LSTRAFE2)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_VALLEY_STRAFE -> {
                        doWaypath(WID_VSTRAFE_TO_LSTRAFE2)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_CIRCLE_POWER_PLANT, STATE_LAND_IN_TIBERIUM_FIELD, STATE_LONG_STRAFING_RUN -> {
                        setOverallState(STATE_LONG_STRAFING_RUN2)
                    }
                }
            }
            STATE_LONG_STRAFING_RUN -> {
                when (overallState) {
                    STATE_CIRCLE_REFINERY, STATE_ATTACK_LIFT_AREA -> {
                        doWaypath(WID_REFINERY_TO_LSTRAFE)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_VALLEY_STRAFE -> {
                        doWaypath(WID_VSTRAFE_TO_LSTRAFE)
                        setOverallState(STATE_IN_TRANSITION)
                    }
                    STATE_CIRCLE_POWER_PLANT, STATE_LAND_IN_TIBERIUM_FIELD, STATE_LONG_STRAFING_RUN2 -> {
                        setOverallState(STATE_LONG_STRAFING_RUN)
                    }
                }
            }
        }

        nextOverallState = newState
    }

    // C++: void Update_Overall_State()
    private fun updateOverallState() {
        when (overallState) {
            STATE_CIRCLE_POWER_PLANT -> {
                if (currentTarget.get() != null) {
                    pilot.setCirclePos(targetPos)
                } else {
                    pilot.clearTarget()
                }
                // C++: Use the gattling guns on the player
                attackTarget(rocketPriority = 0f, gattlingPriority = 1f)
            }

            STATE_CIRCLE_REFINERY -> {
                if (currentTarget.get() != null) {
                    var destination = Vector3(-166.0f, 40.13f, 42.23f)
                    var targetPlayer = true

                    if (targetPos.y < 21.0f && targetPos.x < -151.0f) {
                        targetPlayer = false
                    }

                    if (targetPos.x > -140.0f && targetPos.y > 34.0f) {
                        destination = Vector3(
                            destination.x + 10.0f,
                            min(targetPos.y + 1.0f, 78.0f),
                            destination.z - 20.0f
                        )
                    } else {
                        if (targetPlayer || targetPos.x > -151.0f) {
                            destination = Vector3(destination.x + 20.0f, destination.y, destination.z - 20.0f)
                        }
                        if (targetPos.y > -6.0f) {
                            destination = Vector3(destination.x, min(targetPos.y + 46.0f, 78.0f), destination.z)
                        }
                    }

                    // C++: Don't let the commanche dip below the player's level
                    destination = Vector3(destination.x, destination.y, max(targetPos.z + 10.0f, destination.z))

                    pilot.setDestination(destination)
                    pilot.mode = PilotClassStub.MODE_HOVER

                    if (targetPlayer) {
                        pilot.setTarget(targetPos)
                    } else {
                        val selfPos = position
                        pilot.setTarget(Vector3(selfPos.x, selfPos.y - 10f, selfPos.z))
                    }

                    // C++: Start attacking the player if we are within 4 meters of expected destination
                    val currPos = position
                    val diff = Vector3(destination.x - currPos.x, destination.y - currPos.y, destination.z - currPos.z)
                    if (diff.x * diff.x + diff.y * diff.y + diff.z * diff.z < 16f) {
                        // C++: Use the rockets to attack the player
                        attackTarget(rocketPriority = 1f, gattlingPriority = 0f)
                    }
                } else {
                    pilot.clearTarget()
                }
            }

            STATE_ATTACK_LIFT_AREA -> {
                val currZ = targetPos.z
                val destZ = max(currZ + 7.0f, 18.88f)
                val destination = Vector3(-161.78f, 79.03f, destZ)
                pilot.setDestination(destination)
                pilot.setTarget(Vector3(destination.x + 10f, destination.y, destination.z))

                // C++: Make sure we are attacking the player
                if (!isAttacking) {
                    if (targetPos.x > -130.0f) {
                        attackTarget(rocketPriority = 1f, gattlingPriority = 0f)
                    } else {
                        attackTarget(rocketPriority = 0f, gattlingPriority = 1f)
                    }
                }
            }

            STATE_LONG_STRAFING_RUN2,
            STATE_LONG_STRAFING_RUN -> {
                var shouldFire = false

                // C++: Determine if we should start firing or not
                if (currentTarget.get() != null) {
                    // C++: Is the target in front of us?
                    if (targetAngle > degToRadF(-75f) && targetAngle < degToRadF(75f)) {
                        val currPos = position
                        // C++: Are we inside the valley?
                        if (currPos.y >= -40.0f && currPos.y <= 140.0f && currPos.x >= -178.0f) {
                            shouldFire = true
                        }
                    }
                } else {
                    pilot.clearTarget()
                }

                // C++: Turn the gattling guns on/off
                if (shouldFire) {
                    setGattlingGunState(GATLING_STATE_FIRING)
                } else {
                    setGattlingGunState(GATLING_STATE_NORMAL)
                }

                // C++: Restore the vehicle's speed when we've finished moving
                if (vehicleState != VEHICLE_STATE_MOVING) {
                    // C++: Target the player
                    pilot.setTarget(targetPos)
                    // C++: Restore the speed
                    pilot.setMaxSpeed(20.0f)
                }
            }

            STATE_VALLEY_STRAFE -> {
                if (currentTarget.get() != null) {
                    var destX = -84.33f
                    if (targetPos.y < 42.0f) {
                        destX = WWMath.clamp(targetPos.x, -143.0f, -84.33f)
                    } else {
                        destX = if (targetPos.x > -113.0f) {
                            WWMath.clamp(targetPos.x - 35.0f, -143.0f, -84.33f)
                        } else {
                            WWMath.clamp(targetPos.x + 35.0f, -143.0f, -84.33f)
                        }
                    }

                    val destination = Vector3(destX, 77.99f, 26.77f)
                    pilot.setDestination(destination)
                    pilot.mode = PilotClassStub.MODE_HOVER
                    pilot.setTarget(targetPos)

                    // C++: Start attacking if within 4 meters of destination
                    val currPos = position
                    val diff = Vector3(destination.x - currPos.x, destination.y - currPos.y, destination.z - currPos.z)
                    if (diff.x * diff.x + diff.y * diff.y + diff.z * diff.z < 16f) {
                        // C++: Attack with either weapon
                        attackTarget(rocketPriority = 1f, gattlingPriority = 1f)
                    }
                } else {
                    pilot.clearTarget()
                }
            }

            STATE_LAND_IN_TIBERIUM_FIELD -> {
                if (currentTarget.get() != null) {
                    pilot.setTarget(targetPos)
                } else {
                    pilot.clearTarget()
                }

                // C++: Start attacking if within 4 meters of destination
                val currPos = position
                val destination = pilot.getDestination()
                val diff = Vector3(destination.x - currPos.x, destination.y - currPos.y, destination.z - currPos.z)
                if (diff.x * diff.x + diff.y * diff.y + diff.z * diff.z < 16f) {
                    // C++: Attack the player with either weapon
                    attackTarget(rocketPriority = 1f, gattlingPriority = 1f)
                }
            }

            STATE_IN_TRANSITION -> {
                // C++: If we've finished moving, we are done with the transition
                if (vehicleState != VEHICLE_STATE_MOVING) {
                    setOverallState(nextOverallState)
                } else if (faceTargetInTransition) {
                    if (currentTarget.get() != null) {
                        pilot.setTarget(targetPos)
                    } else {
                        pilot.clearTarget()
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Attack control
    // -------------------------------------------------------------------------

    // C++: void Attack_Target(float rocket_priority, float gattling_priority)
    private fun attackTarget(rocketPriority: Float, gattlingPriority: Float) {
        // C++: Don't attack if we don't have a target, or we are already attacking
        if (currentTarget.get() == null || isAttacking || sakuraState != SAKURA_STATE_NORMAL) {
            return
        }

        // C++: There's a 1 in 4 chance that Sakura will taunt the player instead of attacking
        if (FreeRandom.getInt(4) == 2) {
            setSakuraState(SAKURA_STATE_TAUNTING)
        } else {
            var rkPriority = rocketPriority
            // C++: If both rockets are gone, decrease priority so we won't choose them
            if ((availableWeapons and WEAPON_ROCKETS_LEFT) == 0 &&
                (availableWeapons and WEAPON_ROCKETS_RIGHT) == 0)
            {
                rkPriority = gattlingPriority - 1.0f
            }

            // C++: Decide which weapon to use
            when {
                rkPriority > gattlingPriority  -> setRocketState(ROCKET_STATE_OPENING)
                gattlingPriority > rkPriority  -> setGattlingGunState(GATLING_STATE_REVVING_UP)
                else -> {
                    // C++: Priorities are equal — toss a coin
                    if (FreeRandom.getInt(2) == 1) {
                        setRocketState(ROCKET_STATE_OPENING)
                    } else {
                        setGattlingGunState(GATLING_STATE_REVVING_UP)
                    }
                }
            }
        }
    }

    // C++: void Stop_Attacking()
    private fun stopAttacking() {
        // C++: Force the rockets closed
        if (rocketLauncherState != ROCKET_STATE_CLOSED) {
            setRocketState(ROCKET_STATE_CLOSING)
        }
        // C++: Force the gattling gun to stop
        if (gattlingGunState != GATLING_STATE_NORMAL) {
            setGattlingGunState(GATLING_STATE_NORMAL)
        }
    }

    // -------------------------------------------------------------------------
    // Target management
    // -------------------------------------------------------------------------

    // C++: void Update_Target()
    private fun updateTarget() {
        targetTimeLeft -= TimeManager.getFrameSeconds()
        if (targetTimeLeft <= 0) {
            // C++: Choose either the closest commando or the last person to damage us
            if (FreeRandom.getInt(2) == 0 && lastDamager.get() != null) {
                currentTarget.set(lastDamager.get())
            } else {
                currentTarget.set(findClosestHumanPlayer())
            }
            // C++: Wait a random amount of time before we choose a new target
            targetTimeLeft = randomFloat(5.0f, 20.0f)
        } else if (currentTarget.get() == null) {
            targetTimeLeft *= 0.25f
        }
    }

    // C++: SoldierGameObj* Find_Closest_Human_Player()
    private fun findClosestHumanPlayer(): SoldierGameObj? {
        var closestHumanPlayer: SoldierGameObj? = null
        var closestDistance = 9999.0f

        val sakuraPosition = position

        // C++: valley_box centered at (-105, 35, 20) with extent (75, 70.5, 14)
        // C++: PhysicsSceneClass::Get_Instance()->Collect_Objects(valley_box, ...) — physics query
        // FIXME: Physics scene not available server-side for spatial queries; fall back to iterating registered objects
        // C++: filter: Is_Human_Controlled() soldiers
        // Server-side: iterate GameObjManager for soldiers (not yet implemented — return null)
        return closestHumanPlayer
    }

    // -------------------------------------------------------------------------
    // Decision data snapshot
    // -------------------------------------------------------------------------

    // C++: void Update_Decision_Data()
    private fun updateDecisionData() {
        // C++: Update our current health
        // C++: DefenseObjectClass* Get_Defense_Object() — access via DamageableGameObj delegating props
        currentHealth = health / healthMax.coerceAtLeast(1f)

        // C++: Determine if any weapons are currently attacking the player
        isAttacking = rocketLauncherState != ROCKET_STATE_CLOSED || gattlingGunState != GATLING_STATE_NORMAL

        // C++: Update information about our current target
        targetAngle = 0f
        targetPos = Vector3()
        val target = currentTarget.get()
        if (target != null) {
            // C++: Get the target's position
            targetPos = target.getPosition()

            // C++: Get the target's position relative to ours (object space)
            // C++: TargetAngle = WWMath::Atan2(obj_space_target.Y, obj_space_target.X)
            // C++: then Wrap to [-180deg, 180deg]
            // Simplified: compute world-space angle difference
            val dx = targetPos.x - position.x
            val dy = targetPos.y - position.y
            val worldAngle = atan2(dy, dx)
            // C++: Inverse_Transform_Vector then Atan2 on local X/Y
            // For now compute angle relative to facing
            targetAngle = (worldAngle - facing).let { a ->
                // wrap to [-PI, PI]
                var wrapped = a % (2 * Math.PI.toFloat())
                if (wrapped > Math.PI.toFloat()) wrapped -= 2 * Math.PI.toFloat()
                if (wrapped < -Math.PI.toFloat()) wrapped += 2 * Math.PI.toFloat()
                wrapped
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tilt control
    // -------------------------------------------------------------------------

    // C++: void Update_Tilt()
    private fun updateTilt() {
        // C++: Bone manipulation (model-based) — client-side rendering only
        // Server-side: just track tiltAngle value so it saves/loads correctly
        val TILT_RATE = degToRadF(25.0f)
        val dt = TimeManager.getFrameSeconds()

        if (chopperTiltBoneIndex > 0) {
            val maxTilt = if (overallState == STATE_LAND_IN_TIBERIUM_FIELD) degToRadF(10.0f) else degToRadF(35.0f)

            if (overallState != STATE_IN_TRANSITION) {
                var targetAngleLocal = degToRadF(25.0f)
                if (currentTarget.get() != null) {
                    // C++: Compute tilt from target elevation angle (Y-axis rotation)
                    // Simplified: use the elevation difference
                    val dx = targetPos.x - position.x
                    val dz = targetPos.z - position.z
                    val dist = kotlin.math.sqrt((dx * dx + dz * dz).toDouble()).toFloat().coerceAtLeast(0.001f)
                    targetAngleLocal = (-atan2(dz, dist)).coerceIn(-maxTilt, maxTilt)
                }

                if (tiltAngle < targetAngleLocal) {
                    tiltAngle = min(tiltAngle + dt * TILT_RATE, targetAngleLocal)
                } else {
                    tiltAngle = max(tiltAngle - dt * TILT_RATE, targetAngleLocal)
                }
                // C++: model->Capture_Bone / Control_Bone — client-side only
            } else {
                // C++: Decrease tilt in transition
                tiltAngle = max(tiltAngle - dt * TILT_RATE, 0.0f)
                // C++: model->Release_Bone — client-side only
            }
        }
    }

    // -------------------------------------------------------------------------
    // Movement helpers
    // -------------------------------------------------------------------------

    // C++: void Move_To_Location(const Vector3& pos, float speed)
    private fun moveToLocation(pos: Vector3, speed: Float = 0.75f) {
        moveToLocation = pos
        setVehicleState(VEHICLE_STATE_MOVING)
        // C++: Pilot.Set_Mode(MODE_FLY_TO_POINT)
        pilot.mode = PilotClassStub.MODE_FLY_TO_POINT
        pilot.setDestination(pos)
    }

    // C++: void Do_Waypath(int waypath_id, int start_id, int end_id)
    private fun doWaypath(waypathId: Int, startId: Int = -1, endId: Int = -1) {
        // C++: PathfindClass::Get_Instance()->Find_Waypath(waypath_id)
        // C++: Path->Initialize(waypath, start_id, end_id)
        // C++: Pilot.Set_Mode(MODE_FLY_TO_POINT)
        // C++: Set_Vehicle_State(VEHICLE_STATE_MOVING)
        // FIXME: PathfindClass and PathClass not yet ported; just set vehicle state as moving
        pilot.mode = PilotClassStub.MODE_FLY_TO_POINT
        setVehicleState(VEHICLE_STATE_MOVING)
    }

    // -------------------------------------------------------------------------
    // Taunt list shuffling
    // -------------------------------------------------------------------------

    // C++: void Shuffle_Taunt_List()
    private fun shuffleTauntList() {
        tauntList.fill(0)
        // C++: Choose a random list entry for each taunt ID, avoiding duplicates
        for (index in 0 until MAX_TAUNTS) {
            var listIndex: Int
            do {
                listIndex = FreeRandom.getInt(MAX_TAUNTS)
            } while (tauntList[listIndex] != 0)
            tauntList[listIndex] = index + 1
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    // C++: WWMath::Random_Float(min, max)
    private fun randomFloat(min: Float, max: Float): Float =
        min + FreeRandom.get() * (max - min)
}

// ---------------------------------------------------------------------------
// PilotClassStub — minimal stub for PilotClass (pilot.h / pilot.cpp)
// C++: PilotClass controls VTOL vehicle flight (mode, destination, speed, etc.)
// FIXME: Full PilotClass not yet ported.
// ---------------------------------------------------------------------------
class PilotClassStub {
    companion object {
        const val MODE_HOVER         = 0
        const val MODE_CIRCLE_POINT  = 1
        const val MODE_FLY_TO_POINT  = 2
    }

    var mode: Int = MODE_HOVER
    var arrivedDist: Float = 1.0f
    var aggressivness: Float = 1.0f
    var isExactZImportant: Boolean = false
    var circleMin: Float = 0f
    var circleMax: Float = 0f
    var circleDist: Float = 0f

    private var destination: Vector3 = Vector3()
    private var targetPos: Vector3? = null

    fun setDestination(pos: Vector3) { destination = pos }
    fun getDestination(): Vector3 = destination
    fun setTarget(pos: Vector3) { targetPos = pos }
    fun clearTarget() { targetPos = null }
    fun setCirclePos(pos: Vector3) { destination = pos }
    fun setMaxSpeed(speed: Float) { /* FIXME: not yet ported */ }
    fun setNextPoint(pos: Vector3) { /* FIXME: not yet ported */ }
    fun setHoverDist(dist: Float) { /* FIXME: not yet ported */ }

    // C++: void Think()
    fun think(deltaSeconds: Float) {
        // FIXME: actual flight simulation not yet ported
    }

    // C++: bool Save(ChunkSaveClass&)
    fun save(csave: ChunkSaveClass) { /* FIXME: pilot save not yet ported */ }

    // C++: bool Load(ChunkLoadClass&)
    fun load(cload: ChunkLoadClass) { /* FIXME: pilot load not yet ported */ }
}

// ---------------------------------------------------------------------------
// PathClassStub — minimal stub for PathClass (path.h)
// C++: PathClass tracks current position along a waypath with spline interpolation.
// FIXME: Full PathClass not yet ported.
// ---------------------------------------------------------------------------
class PathClassStub {

    // C++: bool Save(ChunkSaveClass&)
    fun save(csave: ChunkSaveClass) { /* FIXME: path save not yet ported */ }

    // C++: bool Load(ChunkLoadClass&)
    fun load(cload: ChunkLoadClass) { /* FIXME: path load not yet ported */ }
}
