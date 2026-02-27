package ccr.server.net

import ccr.math.Vector3
import ccr.server.defs.MendozaBossGameObjDefClass

// C++: MendozaBossGameObjClass : public SoldierGameObj
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//              → PhysicalGameObj → ArmedGameObj → SmartGameObj → SoldierGameObj → MendozaBossGameObj
// Singleplayer-only boss NPC. Controls the Mendoza boss fight in mission 12.
// Full port of mendozabossgameobj.h / mendozabossgameobj.cpp
open class MendozaBossGameObj : SoldierGameObj() {

    // -------------------------------------------------------------------------
    // State enums (C++ inner enums from header)
    // -------------------------------------------------------------------------

    // C++: overall state enum (overall combat phase)
    companion object {
        const val OVERALL_STATE_FLYING_SIDEKICK  = 0
        const val OVERALL_STATE_MELEE_ATTACK      = 1
        const val OVERALL_STATE_RANGED_ATTACK     = 2
        const val OVERALL_STATE_FIREBALL_ATTACK   = 3
        const val OVERALL_STATE_FIND_HEALTH       = 4
        const val OVERALL_STATE_SYDNEY_BOLTS      = 5
        const val OVERALL_STATE_RUN_AFTER_SYDNEY  = 6
        const val OVERALL_STATE_TOY_WITH_SYDNEY   = 7
        const val OVERALL_STATE_DEATH_SEQUENCE    = 8

        // C++: move state enum
        const val MOVE_STATE_STOP                = 0
        const val MOVE_STATE_GET_CLOSE_TO_PLAYER = 1
        const val MOVE_STATE_ATTACK_PATTERN1     = 2
        const val MOVE_STATE_ATTACK_PATTERN2     = 3
        const val MOVE_STATE_GOTO_HELIPAD        = 4
        const val MOVE_STATE_RUN_TO_HEALTH       = 5
        const val MOVE_STATE_CHASE_SYDNEY        = 6

        // C++: Mendoza body state enum
        const val MENDOZA_STATE_STANDING        = 0
        const val MENDOZA_STATE_FALLING         = 1
        const val MENDOZA_STATE_KIPPING         = 2
        const val MENDOZA_STATE_PACK_EXPLODING  = 3
        const val MENDOZA_STATE_DYING           = 4
        const val MENDOZA_STATE_DEAD            = 5

        // C++: Sydney companion state enum
        const val SYDNEY_STATE_WAITING      = 0
        const val SYDNEY_STATE_BOLTING      = 1
        const val SYDNEY_STATE_TRIPPING     = 2
        const val SYDNEY_STATE_GETTING_UP   = 3
        const val SYDNEY_STATE_COWERING     = 4
        const val SYDNEY_STATE_STRIKE_A_POSE = 5

        // C++: head look state enum
        const val HEAD_STATE_NONE              = 0
        const val HEAD_STATE_LOOKING_AT_STAR   = 1
        const val HEAD_STATE_LOOKING_AT_SYDNEY = 2

        // C++: attack state enum
        const val ATTACK_STATE_NONE                       = 0
        const val ATTACK_STATE_MELEE                      = 1
        const val ATTACK_STATE_MELEE_FLYING_SIDEKICK_RUN  = 2
        const val ATTACK_STATE_MELEE_FLYING_SIDEKICK      = 3
        const val ATTACK_STATE_MELEE_CRESENT_KICK         = 4
        const val ATTACK_STATE_MELEE_SIDE_KICK            = 5
        const val ATTACK_STATE_MELEE_SIDE_KICK_RETRACT    = 6
        const val ATTACK_STATE_MELEE_PUNCH                = 7
        const val ATTACK_STATE_FLAMETHROWER               = 8
        const val ATTACK_STATE_FIREBALL                   = 9
        const val ATTACK_STATE_HANDGUN                    = 10
        const val ATTACK_STATE_SYDNEY                     = 11

        // C++: camera state enum
        const val CAMERA_STATE_NORMAL            = 0
        const val CAMERA_STATE_FACE_ZOOM         = 1
        const val CAMERA_STATE_WAYPATH_FOLLOW    = 2
        const val CAMERA_STATE_LOOK_AT_DEAD_BOSS = 3

        // C++: private constants
        const val MAX_TAUNTS = 6

        // C++: save/load chunk IDs — CHUNKID_PARENT=0x09230243
        const val CHUNKID_PARENT                  = 0x09230243.toInt()
        const val CHUNKID_VARIABLES               = 0x09230244.toInt()
        const val CHUNKID_STATE_MACHINES          = 0x09230245.toInt()
        const val CHUNKID_CAMERA_SPLINE           = 0x09230246.toInt()
        const val CHUNKID_OVERALL_STATE_MACHINE   = 0x09230247.toInt()
        const val CHUNKID_MENDOZA_STATE_MACHINE   = 0x09230248.toInt()
        const val CHUNKID_SYDNEY_STATE_MACHINE    = 0x09230249.toInt()
        const val CHUNKID_MOVE_STATE_MACHINE      = 0x0923024A.toInt()
        const val CHUNKID_HEAD_STATE_MACHINE      = 0x0923024B.toInt()
        const val CHUNKID_CAMERA_STATE_MACHINE    = 0x0923024C.toInt()
        const val CHUNKID_ATTACK_STATE_MACHINE    = 0x0923024D.toInt()

        // C++: save/load variable IDs
        const val VARID_OVERALLSTATE_TIMER                = 0
        const val VARID_MENDOZASTATE_TIMER                = 1
        const val VARID_CAMERASTATE_TIMER                 = 2
        const val VARID_CAMERASHAKE_TIMER                 = 3
        const val VARID_ATTACKSTATE_TIMER                 = 4
        const val VARID_RANDOMTARGET_TIMER                = 5
        const val VARID_SYDNEYSTATE_TIMER                 = 6
        const val VARID_MOVESTATE_TIMER                   = 7
        const val VARID_NEXT_MELEE_ATTACK_TIME            = 8
        const val VARID_FIREBALL_START_TIMER              = 9
        const val VARID_NEXT_KNOCKDOWN_AVAILABLE_TIME     = 10
        const val VARID_HAS_MELEE_ATTACK_HIT              = 11
        const val VARID_LAST_MELEE_ANIM_FRAME             = 12
        const val VARID_START_TIMER                       = 13
        const val VARID_SIDE_KICK_POS                     = 14
        const val VARID_SHOOT_GROUND_POS                  = 15
        const val VARID_ATTACKING_BONE_NAME               = 16
        const val VARID_CAMERA_BONE_PTR                   = 17
        const val VARID_CAMERA_BONE_TM                    = 18

        // C++: static level constants
        const val UNINITIALIZED_TIMER = -5000.0f
        const val FALLDOWN_TIMER = 6.0f

        // C++: static positions used in the boss fight
        val MENDOZA_END_POS = Vector3(-58.088f, 15.389f, 0.334f)
        val SYDNEY_END_POS  = Vector3(-57.931f, 12.133f, 0.412f)
        val HELIPAD_CENTER_POS = Vector3(-43.329f, 30.796f, 1.605f)

        // C++: CHUNKID_GAME_OBJECT_MENDOZA_BOSS from combatchunkid.h
        const val CHUNK_ID: UInt = 0x00040149u
    }

    // -------------------------------------------------------------------------
    // Private fields — C++ member data (mendozabossgameobj.h private section)
    // -------------------------------------------------------------------------

    // C++: float MendozaTauntTimeLeft
    private var mendozaTauntTimeLeft: Float = 0f

    // C++: int TauntList[MAX_TAUNTS]
    private val tauntList: IntArray = IntArray(MAX_TAUNTS)

    // C++: int AvailableTaunts
    private var availableTaunts: Int = 0xFFFFFF

    // C++: float OverallStateTimer
    private var overallStateTimer: Float = 0f

    // C++: float MendozaStateTimer
    private var mendozaStateTimer: Float = 0f

    // C++: float CameraStateTimer
    private var cameraStateTimer: Float = 0f

    // C++: float CameraShakeTimer
    private var cameraShakeTimer: Float = 0f

    // C++: float AttackStateTimer
    private var attackStateTimer: Float = 0f

    // C++: float RandomTargetTimer
    private var randomTargetTimer: Float = 0f

    // C++: float SydneyStateTimer
    private var sydneyStateTimer: Float = 0f

    // C++: float MoveStateTimer
    private var moveStateTimer: Float = 0f

    // C++: float NextMeleeAttackTime
    private var nextMeleeAttackTime: Float = 0f

    // C++: float FireballStartTimer
    private var fireballStartTimer: Float = 0f

    // C++: float NextKnockdownAvailableTime
    private var nextKnockdownAvailableTime: Float = 0f

    // C++: bool HasMeleeAttackHit
    private var hasMeleeAttackHit: Boolean = false

    // C++: float LastMeleeAnimFrame
    private var lastMeleeAnimFrame: Float = 0f

    // C++: float StartTimer
    private var startTimer: Float = 0f

    // C++: RenderObjClass* CameraBoneModel — client-side camera rig model
    // C++: WW3DAssetManager::Get_Instance()->Create_Render_Obj("CAMBONE")
    // C++: client-side only — skipped on server
    private var cameraBoneModel: RenderObjClass? = null

    // C++: GameObjReference Sydney — reference to Sydney NPC (network ID 101010)
    private val sydney: GameObjReference = GameObjReference()

    // C++: StringClass AttackingBoneName
    private var attackingBoneName: String = ""

    // C++: Vector3 SideKickPos
    private var sideKickPos: Vector3 = Vector3(0f, 0f, 0f)

    // C++: Vector3 ShootGroundPos
    private var shootGroundPos: Vector3 = Vector3(0f, 0f, 0f)

    // C++: CardinalSpline3DClass CameraSpline — client-side camera path
    // Client-side only (spline evaluation for camera follow)
    // FIXME: stub — no CardinalSpline3DClass in server

    // C++: Vector3 MendozaPos — cached position updated each Think()
    private var mendozaPos: Vector3 = Vector3(0f, 0f, 0f)

    // C++: Vector3 StarPos — cached player (star) position updated each Think()
    private var starPos: Vector3 = Vector3(0f, 0f, 0f)

    // -------------------------------------------------------------------------
    // State machine fields — C++ StateMachineClass<MendozaBossGameObjClass>
    // Represented here as plain Int state fields (no C++ DECLARE_STATE macro)
    // -------------------------------------------------------------------------

    // C++: StateMachineClass<> OverallState
    private var overallState: Int = -1

    // C++: StateMachineClass<> MendozaState
    private var mendozaState: Int = MENDOZA_STATE_STANDING

    // C++: StateMachineClass<> SydneyState
    private var sydneyState: Int = -1

    // C++: StateMachineClass<> MoveState
    private var moveState: Int = -1

    // C++: StateMachineClass<> HeadState
    private var headState: Int = -1

    // C++: StateMachineClass<> CameraState
    private var cameraState: Int = CAMERA_STATE_NORMAL

    // C++: StateMachineClass<> AttackState
    private var attackState: Int = ATTACK_STATE_NONE

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    // C++: virtual void Init() — delegates to Init(definition)
    override fun init() {
        super.init()
        shuffleTauntList()
        tauntList[0] = 1
        tauntList[1] = 2
        tauntList[2] = 3
        tauntList[3] = 4
        tauntList[4] = 5
        tauntList[5] = 6
        initStateMachines()
        initializeBoss()
    }

    // C++: void Init(const MendozaBossGameObjDefClass &definition)
    // Called from Create() on the def class; calls SoldierGameObj::Init + Initialize_Boss
    fun init(definition: MendozaBossGameObjDefClass) {
        // C++: SoldierGameObj::Init(definition) then Initialize_Boss()
        shuffleTauntList()
        tauntList[0] = 1
        tauntList[1] = 2
        tauntList[2] = 3
        tauntList[3] = 4
        tauntList[4] = 5
        tauntList[5] = 6
        initStateMachines()
        initializeBoss()
    }

    // C++: const MendozaBossGameObjDefClass& Get_Definition() const
    fun getMendozaBossDefinition(): MendozaBossGameObjDefClass = definition as MendozaBossGameObjDefClass

    // C++: MendozaBossGameObj* As_MendozaBossGameObj() { return this; }
    fun asMendozaBossGameObj(): MendozaBossGameObj = this

    // C++: bool Wants_Powerups() { return true; }
    // Overrides default to allow Mendoza to pick up powerups
    override fun wantsPowerups(): Boolean = true

    // C++: bool Allow_Special_Damage_State_Lock() { return false; }
    // Mendoza does not lock into special damage states (fire/electric)
    override fun allowSpecialDamageStateLock(): Boolean = false

    // -------------------------------------------------------------------------
    // Private init helpers
    // -------------------------------------------------------------------------

    // C++: void Initialize_Boss()
    // Finds the Sydney NPC (network ID 101010) and disables her innate AI
    private fun initializeBoss() {
        // C++: GameObjManager::Find_PhysicalGameObj(101010)
        // FIXME: GameObjManager.findPhysicalGameObj not yet implemented in server
        // When available: sydney.setPtr(GameObjManager.findPhysicalGameObj(101010)?.asSoldierGameObj())
        // C++: sydney->Innate_Disable() — disables innate AI so boss controls her
    }

    // C++: void Shuffle_Taunt_List()
    // Shuffles the taunt ID list randomly (Fisher-Yates style via Get_Int)
    private fun shuffleTauntList() {
        tauntList.fill(0)
        for (index in 0 until MAX_TAUNTS) {
            var listIndex: Int
            do {
                listIndex = (Math.random() * MAX_TAUNTS).toInt()
            } while (tauntList[listIndex] != 0)
            tauntList[listIndex] = index + 1
        }
    }

    // Initializes all state machine starting states (mirrors C++ ctor)
    private fun initStateMachines() {
        mendozaState = MENDOZA_STATE_STANDING
        attackState  = ATTACK_STATE_NONE
        cameraState  = CAMERA_STATE_NORMAL
    }

    // -------------------------------------------------------------------------
    // Think / Apply_Control
    // -------------------------------------------------------------------------

    // C++: void Think()
    // Main boss AI tick. Guards on COMBAT_STAR and Sydney being non-null.
    // Temporarily boosts health by 100 to prevent accidental death via special damage.
    override fun think() {
        val dt = TimeManager.getFrameSeconds()
        val combatStar = CombatManager.getCombatStar()

        // C++: if (COMBAT_STAR == NULL || Sydney == NULL) ok_to_think = false
        val okToThink = (combatStar != null && sydney.get() != null)

        if (okToThink) {
            // C++: Get_Position(&MendozaPos); COMBAT_STAR->Get_Position(&StarPos)
            // FIXME: position queries not wired in server
            // mendozaPos = getPosition()
            // starPos = combatStar.getPosition()

            if (startTimer > UNINITIALIZED_TIMER) {
                // C++: StartTimer -= TimeManager::Get_Frame_Seconds()
                startTimer -= dt
                if (startTimer <= 0f) {
                    spawnHealthPowerups()
                    startTimer = UNINITIALIZED_TIMER
                    setOverallState(OVERALL_STATE_FLYING_SIDEKICK)
                }
            } else {
                // C++: NextKnockdownAvailableTime -= TimeManager::Get_Frame_Seconds()
                nextKnockdownAvailableTime -= dt

                // Tick all state machines
                thinkOverallState(dt)
                thinkMendozaState(dt)
                thinkMoveState(dt)
                thinkHeadState(dt)
                thinkCameraState(dt)
                thinkAttackState(dt)
                thinkSydneyState(dt)
            }
        }

        // C++: Give Mendoza 100 extra health so special damage (fire/electricity)
        //      cannot kill him during the scripted sequence.
        // FIXME: DefenseObject not wired in server; logic preserved as comment.
        // val oldHealth = defenseObject.health + 100f
        // val oldHealthMax = defenseObject.healthMax + 100f
        // defenseObject.healthMax = oldHealthMax
        // defenseObject.health = oldHealth

        // C++: SoldierGameObj::Think()
        super.think()

        // C++: Restore his health (subtract the 100 back, clamp to 1)
        // FIXME: DefenseObject not wired in server
        // defenseObject.health = maxOf(defenseObject.health - 100f, 1f)
        // defenseObject.healthMax -= 100f
    }

    // C++: void Apply_Control() — delegates to SoldierGameObj::Apply_Control()
    override fun applyControl() {
        // C++: SoldierGameObj::Apply_Control()
        // (no override logic in MendozaBossGameObjClass)
        super.applyControl()
    }

    // -------------------------------------------------------------------------
    // Apply_Damage_Extended
    // -------------------------------------------------------------------------

    // C++: virtual void Apply_Damage_Extended(const OffenseObjectClass&, float, const Vector3&, const char*)
    // Intercepts all damage, prevents death outside the scripted sequence,
    // and uses damage thresholds to drive state transitions.
    override fun applyDamageExtended(
        damager: OffenseObjectClass,
        scale: Float,
        direction: Vector3,
        collisionBoxName: String?
    ) {
        // C++: if (!CombatManager::I_Am_Server() || OverallState == DEATH_SEQUENCE) return
        if (overallState == OVERALL_STATE_DEATH_SEQUENCE) {
            return
        }

        // C++: float old_health = DefenseObject.Get_Health()
        // FIXME: DefenseObject not wired; using stub 0f
        val oldHealth = 0f

        // C++: DefenseObject.Set_Can_Object_Die(false)
        // C++: SoldierGameObj::Apply_Damage_Extended(...)
        super.applyDamageExtended(damager, scale, direction, collisionBoxName)

        // C++: float curr_health = DefenseObject.Get_Health()
        val currHealth = 0f

        if (sydneyState == SYDNEY_STATE_COWERING) {
            // C++: if (FreeRandom.Get_Int(4) == 1) → trigger death
            if ((Math.random() * 4).toInt() == 1) {
                setOverallState(OVERALL_STATE_DEATH_SEQUENCE)
            }
        } else {
            // C++: calculate health percent thresholds
            // FIXME: DefenseObject.Get_Health_Max() stubbed as 1f to avoid division by zero
            val healthMax = 1f
            val oldHealthPercent = oldHealth / healthMax
            val healthPercent = currHealth / healthMax

            if (healthPercent > 0.25f) {
                // C++: frontal hit knockdown check
                if (healthPercent > 0.3f &&
                    nextKnockdownAvailableTime <= 0f &&
                    overallState >= OVERALL_STATE_MELEE_ATTACK
                ) {
                    // C++: Vector3 relative_direction = Get_Transform().Inverse_Rotate_Vector(direction)
                    // C++: if (relative_direction.X < -0.7F && FreeRandom.Get_Int(5) == 1) → FALLING
                    // FIXME: transform inverse-rotate not wired; direction.x used directly as approximation
                    if (direction.x < -0.7f && (Math.random() * 5).toInt() == 1) {
                        setMendozaState(MENDOZA_STATE_FALLING)
                    }
                    nextKnockdownAvailableTime = FALLDOWN_TIMER
                }
                // C++: crossing 75% health → switch to ranged attack
                if (oldHealthPercent > 0.75f && healthPercent <= 0.75f) {
                    setOverallState(OVERALL_STATE_RANGED_ATTACK)
                }
            } else {
                // C++: crossing 25% health → Sydney bolts
                if (oldHealthPercent > 0.25f) {
                    setOverallState(OVERALL_STATE_SYDNEY_BOLTS)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Save / Load
    // -------------------------------------------------------------------------

    // C++: bool Save(ChunkSaveClass &csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        // C++: SoldierGameObj::Save(csave)
        csave.endChunk()

        // State machine chunks — each machine saves its current state index
        csave.beginChunk(CHUNKID_OVERALL_STATE_MACHINE)
        csave.writeInt(overallState)
        csave.endChunk()

        csave.beginChunk(CHUNKID_MENDOZA_STATE_MACHINE)
        csave.writeInt(mendozaState)
        csave.endChunk()

        csave.beginChunk(CHUNKID_SYDNEY_STATE_MACHINE)
        csave.writeInt(sydneyState)
        csave.endChunk()

        csave.beginChunk(CHUNKID_MOVE_STATE_MACHINE)
        csave.writeInt(moveState)
        csave.endChunk()

        csave.beginChunk(CHUNKID_HEAD_STATE_MACHINE)
        csave.writeInt(headState)
        csave.endChunk()

        csave.beginChunk(CHUNKID_CAMERA_STATE_MACHINE)
        csave.writeInt(cameraState)
        csave.endChunk()

        csave.beginChunk(CHUNKID_ATTACK_STATE_MACHINE)
        csave.writeInt(attackState)
        csave.endChunk()

        // C++: CameraSpline.Save(csave) — client-side only; skipped

        csave.beginChunk(CHUNKID_VARIABLES)
        saveVariables(csave)
        csave.endChunk()

        return true
    }

    // C++: bool Load(ChunkLoadClass &cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> {
                    // C++: SoldierGameObj::Load(cload)
                }
                CHUNKID_OVERALL_STATE_MACHINE -> {
                    overallState = cload.readInt()
                }
                CHUNKID_MENDOZA_STATE_MACHINE -> {
                    mendozaState = cload.readInt()
                }
                CHUNKID_SYDNEY_STATE_MACHINE -> {
                    sydneyState = cload.readInt()
                }
                CHUNKID_MOVE_STATE_MACHINE -> {
                    moveState = cload.readInt()
                }
                CHUNKID_HEAD_STATE_MACHINE -> {
                    headState = cload.readInt()
                }
                CHUNKID_CAMERA_STATE_MACHINE -> {
                    cameraState = cload.readInt()
                }
                CHUNKID_ATTACK_STATE_MACHINE -> {
                    attackState = cload.readInt()
                }
                CHUNKID_STATE_MACHINES -> {
                    // C++: legacy combined chunk — load all seven machines in order
                    overallState  = cload.readInt()
                    mendozaState  = cload.readInt()
                    sydneyState   = cload.readInt()
                    moveState     = cload.readInt()
                    headState     = cload.readInt()
                    cameraState   = cload.readInt()
                    attackState   = cload.readInt()
                }
                CHUNKID_CAMERA_SPLINE -> {
                    // C++: CameraSpline.Load(cload) — client-side only; skip
                    cload.skipChunk()
                }
                CHUNKID_VARIABLES -> {
                    loadVariables(cload)
                }
                else -> {
                    // Unrecognized chunk — skip
                    cload.skipChunk()
                }
            }
            cload.closeChunk()
        }
        // C++: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        // On_Post_Load() will be called by the save/load system
        return true
    }

    // C++: void On_Post_Load()
    override fun onPostLoad() {
        // C++: SoldierGameObj::On_Post_Load()
        initializeBoss()
    }

    // C++: void Save_Variables(ChunkSaveClass &csave)
    private fun saveVariables(csave: ChunkSaveClass) {
        // C++: WRITE_MICRO_CHUNK for camera bone model pointer and TM (client-side only)
        csave.writeMicroChunk(VARID_OVERALLSTATE_TIMER,             overallStateTimer)
        csave.writeMicroChunk(VARID_MENDOZASTATE_TIMER,             mendozaStateTimer)
        csave.writeMicroChunk(VARID_CAMERASTATE_TIMER,              cameraStateTimer)
        csave.writeMicroChunk(VARID_CAMERASHAKE_TIMER,              cameraShakeTimer)
        csave.writeMicroChunk(VARID_ATTACKSTATE_TIMER,              attackStateTimer)
        csave.writeMicroChunk(VARID_RANDOMTARGET_TIMER,             randomTargetTimer)
        csave.writeMicroChunk(VARID_SYDNEYSTATE_TIMER,              sydneyStateTimer)
        csave.writeMicroChunk(VARID_MOVESTATE_TIMER,                moveStateTimer)
        csave.writeMicroChunk(VARID_NEXT_MELEE_ATTACK_TIME,         nextMeleeAttackTime)
        csave.writeMicroChunk(VARID_FIREBALL_START_TIMER,           fireballStartTimer)
        csave.writeMicroChunk(VARID_NEXT_KNOCKDOWN_AVAILABLE_TIME,  nextKnockdownAvailableTime)
        csave.writeMicroChunk(VARID_HAS_MELEE_ATTACK_HIT,           hasMeleeAttackHit)
        csave.writeMicroChunk(VARID_LAST_MELEE_ANIM_FRAME,          lastMeleeAnimFrame)
        csave.writeMicroChunk(VARID_START_TIMER,                    startTimer)
        csave.writeMicroChunk(VARID_SIDE_KICK_POS,                  sideKickPos)
        csave.writeMicroChunk(VARID_SHOOT_GROUND_POS,               shootGroundPos)
        csave.writeMicroChunkWwString(VARID_ATTACKING_BONE_NAME,    attackingBoneName)
    }

    // C++: void Load_Variables(ChunkLoadClass &cload)
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                // C++: camera bone pointer / TM — client-side only; skip
                VARID_CAMERA_BONE_PTR, VARID_CAMERA_BONE_TM -> cload.skip()
                VARID_OVERALLSTATE_TIMER            -> overallStateTimer = cload.readFloat()
                VARID_MENDOZASTATE_TIMER            -> mendozaStateTimer = cload.readFloat()
                VARID_CAMERASTATE_TIMER             -> cameraStateTimer = cload.readFloat()
                VARID_CAMERASHAKE_TIMER             -> cameraShakeTimer = cload.readFloat()
                VARID_ATTACKSTATE_TIMER             -> attackStateTimer = cload.readFloat()
                VARID_RANDOMTARGET_TIMER            -> randomTargetTimer = cload.readFloat()
                VARID_SYDNEYSTATE_TIMER             -> sydneyStateTimer = cload.readFloat()
                VARID_MOVESTATE_TIMER               -> moveStateTimer = cload.readFloat()
                VARID_NEXT_MELEE_ATTACK_TIME        -> nextMeleeAttackTime = cload.readFloat()
                VARID_FIREBALL_START_TIMER          -> fireballStartTimer = cload.readFloat()
                VARID_NEXT_KNOCKDOWN_AVAILABLE_TIME -> nextKnockdownAvailableTime = cload.readFloat()
                VARID_HAS_MELEE_ATTACK_HIT          -> hasMeleeAttackHit = cload.readBool()
                VARID_LAST_MELEE_ANIM_FRAME         -> lastMeleeAnimFrame = cload.readFloat()
                VARID_START_TIMER                   -> startTimer = cload.readFloat()
                VARID_SIDE_KICK_POS                 -> sideKickPos = cload.readVector3()
                VARID_SHOOT_GROUND_POS              -> shootGroundPos = cload.readVector3()
                VARID_ATTACKING_BONE_NAME           -> attackingBoneName = cload.readWwString()
                else -> cload.skip()
            }
            cload.closeMicroChunk()
        }
    }

    // -------------------------------------------------------------------------
    // State machine dispatch helpers
    // -------------------------------------------------------------------------

    // Sets the overall state and calls the Begin handler
    private fun setOverallState(newState: Int, force: Boolean = false) {
        overallState = newState
        onOverallStateBegin(newState)
    }

    // Sets the Mendoza body state and calls the Begin handler
    private fun setMendozaState(newState: Int, force: Boolean = false) {
        mendozaState = newState
        onMendozaStateBegin(newState)
    }

    // Sets the Sydney state and calls the Begin handler
    private fun setSydneyState(newState: Int, force: Boolean = false) {
        sydneyState = newState
        onSydneyStateBegin(newState)
    }

    // Sets the move state and calls the Begin handler
    private fun setMoveState(newState: Int, force: Boolean = false) {
        moveState = newState
        onMoveStateBegin(newState)
    }

    // Sets the head state and calls the Begin handler
    private fun setHeadState(newState: Int, force: Boolean = false) {
        headState = newState
        onHeadStateBegin(newState)
    }

    // Sets the camera state and calls the Begin handler
    private fun setCameraState(newState: Int, force: Boolean = false) {
        cameraState = newState
        onCameraStateBegin(newState)
    }

    // Sets the attack state and calls the Begin handler
    private fun setAttackState(newState: Int, force: Boolean = false) {
        attackState = newState
        onAttackStateBegin(newState)
    }

    // -------------------------------------------------------------------------
    // State machine Think dispatchers
    // -------------------------------------------------------------------------

    private fun thinkOverallState(dt: Float) {
        when (overallState) {
            OVERALL_STATE_RANGED_ATTACK    -> onOverallStateRangedAttackThink(dt)
            OVERALL_STATE_FIREBALL_ATTACK  -> { /* no-op think */ }
            OVERALL_STATE_FIND_HEALTH      -> onOverallStateFindHealthThink(dt)
            OVERALL_STATE_SYDNEY_BOLTS     -> onOverallStateSydneyBoltsThink(dt)
            OVERALL_STATE_RUN_AFTER_SYDNEY -> onOverallStateRunAfterSydneyThink(dt)
            OVERALL_STATE_TOY_WITH_SYDNEY  -> onOverallStateToyWithSydneyThink(dt)
        }
    }

    private fun thinkMendozaState(dt: Float) {
        when (mendozaState) {
            MENDOZA_STATE_STANDING       -> onMendozaStateStandingThink(dt)
            MENDOZA_STATE_FALLING        -> onMendozaStateFallingThink(dt)
            MENDOZA_STATE_KIPPING        -> onMendozaStateKippingThink(dt)
            MENDOZA_STATE_PACK_EXPLODING -> onMendozaStatePackExplodingThink(dt)
            MENDOZA_STATE_DYING          -> onMendozaStateDyingThink(dt)
            MENDOZA_STATE_DEAD           -> onMendozaStateDeadThink(dt)
        }
    }

    private fun thinkMoveState(dt: Float) {
        when (moveState) {
            MOVE_STATE_GET_CLOSE_TO_PLAYER -> onMoveStateGetCloseToPlayerThink(dt)
            MOVE_STATE_ATTACK_PATTERN1     -> onMoveStateAttackPattern1Think(dt)
            MOVE_STATE_ATTACK_PATTERN2     -> onMoveStateAttackPattern2Think(dt)
            MOVE_STATE_GOTO_HELIPAD        -> onMoveStateGotoHelipadThink(dt)
        }
    }

    private fun thinkHeadState(dt: Float) {
        when (headState) {
            HEAD_STATE_LOOKING_AT_STAR   -> onHeadStateLookingAtStarThink(dt)
            HEAD_STATE_LOOKING_AT_SYDNEY -> onHeadStateLookingAtSydneyThink(dt)
        }
    }

    private fun thinkCameraState(dt: Float) {
        when (cameraState) {
            CAMERA_STATE_FACE_ZOOM         -> onCameraStateFaceZoomThink(dt)
            CAMERA_STATE_WAYPATH_FOLLOW    -> onCameraStateWaypathFollowThink(dt)
            CAMERA_STATE_LOOK_AT_DEAD_BOSS -> onCameraStateLookAtDeadBossThink(dt)
        }
    }

    private fun thinkAttackState(dt: Float) {
        when (attackState) {
            ATTACK_STATE_NONE                      -> onAttackStateNoneThink(dt)
            ATTACK_STATE_MELEE                     -> onAttackStateMeleeThink(dt)
            ATTACK_STATE_MELEE_FLYING_SIDEKICK_RUN -> onAttackStateMeleeFlyingSidekickRunThink(dt)
            ATTACK_STATE_MELEE_FLYING_SIDEKICK     -> onAttackStateMeleeFlyingSidekickThink(dt)
            ATTACK_STATE_MELEE_CRESENT_KICK        -> onAttackStateMeleeCresentKickThink(dt)
            ATTACK_STATE_MELEE_SIDE_KICK           -> onAttackStateMeleeSideKickThink(dt)
            ATTACK_STATE_MELEE_SIDE_KICK_RETRACT   -> onAttackStateMeleeSideKickRetractThink(dt)
            ATTACK_STATE_MELEE_PUNCH               -> onAttackStateMeleePunchThink(dt)
            ATTACK_STATE_FLAMETHROWER              -> onAttackStateFlamethrowerThink(dt)
            ATTACK_STATE_FIREBALL                  -> onAttackStateFireballThink(dt)
            ATTACK_STATE_HANDGUN                   -> onAttackStateHandgunThink(dt)
            ATTACK_STATE_SYDNEY                    -> onAttackStateSydneyThink(dt)
        }
    }

    private fun thinkSydneyState(dt: Float) {
        when (sydneyState) {
            SYDNEY_STATE_BOLTING     -> onSydneyStateBoltingThink(dt)
            SYDNEY_STATE_TRIPPING    -> onSydneyStateTrippingThink(dt)
            SYDNEY_STATE_GETTING_UP  -> onSydneyStateGettingUpThink(dt)
            SYDNEY_STATE_COWERING    -> onSydneyStateCoweringThink(dt)
        }
    }

    // -------------------------------------------------------------------------
    // State machine Begin dispatchers
    // -------------------------------------------------------------------------

    private fun onOverallStateBegin(state: Int) {
        when (state) {
            OVERALL_STATE_FLYING_SIDEKICK  -> onOverallStateFlyingSidekickBegin()
            OVERALL_STATE_MELEE_ATTACK     -> onOverallStateMeleeAttackBegin()
            OVERALL_STATE_RANGED_ATTACK    -> onOverallStateRangedAttackBegin()
            OVERALL_STATE_FIREBALL_ATTACK  -> onOverallStateFireballAttackBegin()
            OVERALL_STATE_FIND_HEALTH      -> onOverallStateFindHealthBegin()
            OVERALL_STATE_SYDNEY_BOLTS     -> onOverallStateSydneyBoltsBegin()
            OVERALL_STATE_RUN_AFTER_SYDNEY -> onOverallStateRunAfterSydneyBegin()
            OVERALL_STATE_TOY_WITH_SYDNEY  -> onOverallStateToyWithSydneyBegin()
            OVERALL_STATE_DEATH_SEQUENCE   -> onOverallStateDeathSequenceBegin()
        }
    }

    private fun onMendozaStateBegin(state: Int) {
        when (state) {
            MENDOZA_STATE_STANDING       -> onMendozaStateStandingBegin()
            MENDOZA_STATE_FALLING        -> onMendozaStateFallingBegin()
            MENDOZA_STATE_KIPPING        -> onMendozaStateKippingBegin()
            MENDOZA_STATE_PACK_EXPLODING -> onMendozaStatePackExplodingBegin()
            MENDOZA_STATE_DYING          -> onMendozaStateDyingBegin()
            MENDOZA_STATE_DEAD           -> onMendozaStateDeadBegin()
        }
    }

    private fun onSydneyStateBegin(state: Int) {
        when (state) {
            SYDNEY_STATE_WAITING      -> { /* no-op */ }
            SYDNEY_STATE_BOLTING      -> onSydneyStateBoltingBegin()
            SYDNEY_STATE_TRIPPING     -> onSydneyStateTrippingBegin()
            SYDNEY_STATE_GETTING_UP   -> onSydneyStateGettingUpBegin()
            SYDNEY_STATE_COWERING     -> onSydneyStateCoweringBegin()
            SYDNEY_STATE_STRIKE_A_POSE -> onSydneyStateStrikeAPoseBegin()
        }
    }

    private fun onMoveStateBegin(state: Int) {
        when (state) {
            MOVE_STATE_STOP                -> onMoveStateStopBegin()
            MOVE_STATE_GET_CLOSE_TO_PLAYER -> onMoveStateGetCloseToPlayerBegin()
            MOVE_STATE_ATTACK_PATTERN1     -> onMoveStateAttackPattern1Begin()
            MOVE_STATE_ATTACK_PATTERN2     -> onMoveStateAttackPattern2Begin()
            MOVE_STATE_GOTO_HELIPAD        -> onMoveStateGotoHelipadBegin()
            MOVE_STATE_RUN_TO_HEALTH       -> onMoveStateRunToHealthBegin()
            MOVE_STATE_CHASE_SYDNEY        -> onMoveStateChaseSydneyBegin()
        }
    }

    private fun onHeadStateBegin(state: Int) {
        when (state) {
            HEAD_STATE_NONE -> onHeadStateNoneBegin()
            // HEAD_STATE_LOOKING_AT_STAR and HEAD_STATE_LOOKING_AT_SYDNEY: SM_NO_BEGIN
        }
    }

    private fun onCameraStateBegin(state: Int) {
        when (state) {
            CAMERA_STATE_NORMAL            -> onCameraStateNormalBegin()
            CAMERA_STATE_FACE_ZOOM         -> onCameraStateFaceZoomBegin()
            CAMERA_STATE_WAYPATH_FOLLOW    -> onCameraStateWaypathFollowBegin()
            CAMERA_STATE_LOOK_AT_DEAD_BOSS -> onCameraStateLookAtDeadBossBegin()
        }
    }

    private fun onAttackStateBegin(state: Int) {
        when (state) {
            ATTACK_STATE_NONE                      -> onAttackStateNoneBegin()
            ATTACK_STATE_MELEE                     -> onAttackStateMeleeBegin()
            ATTACK_STATE_MELEE_FLYING_SIDEKICK_RUN -> onAttackStateMeleeFlyingSidekickRunBegin()
            ATTACK_STATE_MELEE_FLYING_SIDEKICK     -> onAttackStateMeleeFlyingSidekickBegin()
            ATTACK_STATE_MELEE_CRESENT_KICK        -> onAttackStateMeleeCresentKickBegin()
            ATTACK_STATE_MELEE_SIDE_KICK           -> onAttackStateMeleeSideKickBegin()
            ATTACK_STATE_MELEE_SIDE_KICK_RETRACT   -> onAttackStateMeleeSideKickRetractBegin()
            ATTACK_STATE_MELEE_PUNCH               -> onAttackStateMeleePunchBegin()
            ATTACK_STATE_FLAMETHROWER              -> onAttackStateFlamethrowerBegin()
            ATTACK_STATE_FIREBALL                  -> onAttackStateFireballBegin()
            ATTACK_STATE_HANDGUN                   -> onAttackStateHandgunBegin()
            ATTACK_STATE_SYDNEY                    -> onAttackStateSydneyBegin()
        }
    }

    // -------------------------------------------------------------------------
    // Mendoza body state handlers
    // -------------------------------------------------------------------------

    // C++: On_MENDOZA_STATE_STANDING_Begin
    private fun onMendozaStateStandingBegin() {
        mendozaStateTimer = 0f
    }

    // C++: On_MENDOZA_STATE_STANDING_Think
    private fun onMendozaStateStandingThink(dt: Float) {
        // C++: if (COMBAT_STAR == NULL) return
        // no additional logic beyond the null guard
    }

    // C++: On_MENDOZA_STATE_FALLING_Begin
    private fun onMendozaStateFallingBegin() {
        // C++: Set_Animation("S_A_HUMAN.H_A_635A", false)
        // C++: AttackState.Halt_State(); MoveState.Halt_State()
        // Animation is client-side only; halting states tracked here via flags if needed
    }

    // C++: On_MENDOZA_STATE_FALLING_Think
    private fun onMendozaStateFallingThink(dt: Float) {
        // C++: if (Get_Anim_Control()->Is_Complete()) MendozaState.Set_State(KIPPING, true)
        // FIXME: anim completion not tracked server-side; transition skipped
    }

    // C++: On_MENDOZA_STATE_KIPPING_Begin
    private fun onMendozaStateKippingBegin() {
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_H12C", false)
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
        // C++: ((HumanAnimControlClass*)Get_Anim_Control())->Set_Anim_Speed_Scale(2.0F)
        // Client-side only — server has no animation system
    }

    // C++: STATE_IMPL_END(MENDOZA_STATE_KIPPING) — called when leaving KIPPING state
    private fun onMendozaStateKippingEnd() {
        // C++: MoveState.Resume_State()
        // C++: AttackState.Set_State(ATTACK_STATE_HANDGUN)
        // C++: AttackState.Resume_State()
        setAttackState(ATTACK_STATE_HANDGUN)
    }

    // C++: On_MENDOZA_STATE_KIPPING_Think
    private fun onMendozaStateKippingThink(dt: Float) {
        // C++: if (Get_Anim_Control()->Is_Complete())
        //          MendozaState.Set_State(STANDING, true)
        //          HumanState.Set_State(ANIMATION)
        //          Set_Anim_Speed_Scale(1.0F)
        // FIXME: anim completion not tracked server-side
    }

    // C++: On_MENDOZA_STATE_PACK_EXPLODING_Begin
    private fun onMendozaStatePackExplodingBegin() {
        // C++: Creates AG_MENDOZA_DIE aggregate and attaches it to BACKGUNBONE (client-side)
        // C++: SydneyState.Set_State(SYDNEY_STATE_STRIKE_A_POSE)
        setSydneyState(SYDNEY_STATE_STRIKE_A_POSE)
        mendozaStateTimer = 8.0f
        cameraShakeTimer  = 8.0f
    }

    // C++: On_MENDOZA_STATE_PACK_EXPLODING_Think
    private fun onMendozaStatePackExplodingThink(dt: Float) {
        val oldTimer = mendozaStateTimer

        // C++: CameraShakeTimer -= TimeManager::Get_Frame_Real_Seconds()
        // C++: COMBAT_SCENE->Add_Camera_Shake(...) — client-side only
        cameraShakeTimer -= dt
        if (cameraShakeTimer <= 0f && cameraShakeTimer > UNINITIALIZED_TIMER) {
            // C++: COMBAT_SCENE->Add_Camera_Shake(MendozaPos, 50.0, 1.0F, 0.25F)
            // client-side only — skipped on server
            cameraShakeTimer = UNINITIALIZED_TIMER
        }

        mendozaStateTimer -= dt
        if (mendozaStateTimer <= 0f) {
            // C++: MendozaState.Set_State(MENDOZA_STATE_DYING, true)
            setMendozaState(MENDOZA_STATE_DYING)
        } else if (oldTimer >= 7.5f && mendozaStateTimer < 7.5f) {
            // C++: Set_Special_Damage_Mode(SUPER_FIRE); SpecialDamageTimer = 100.0F
            // C++: Set_Blended_Animation("S_A_HUMAN.H_A_FLMA", true) — client-side
            // C++: Attach_Fire_Sound() — client-side
            specialDamageMode = ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_SUPER_FIRE
            specialDamageTimer = 100.0f
        }
    }

    // C++: On_MENDOZA_STATE_DYING_Begin
    private fun onMendozaStateDyingBegin() {
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_FLMB", false) — client-side
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
        mendozaStateTimer = 7.0f
    }

    // C++: On_MENDOZA_STATE_DYING_Think
    private fun onMendozaStateDyingThink(dt: Float) {
        mendozaStateTimer -= dt
        if (mendozaStateTimer <= 0f) {
            setMendozaState(MENDOZA_STATE_DEAD)
        }
    }

    // C++: On_MENDOZA_STATE_DEAD_Begin
    private fun onMendozaStateDeadBegin() {
        mendozaStateTimer = 10.0f
    }

    // C++: On_MENDOZA_STATE_DEAD_Think
    private fun onMendozaStateDeadThink(dt: Float) {
        mendozaStateTimer -= dt
        if (mendozaStateTimer <= 0f) {
            // C++: MendozaState.Set_State(STANDING)
            // C++: Set_Special_Damage_Mode(NONE)
            // C++: HumanState.Set_State(ANIMATION)
            setMendozaState(MENDOZA_STATE_STANDING)
            specialDamageMode = ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE
        }
    }

    // -------------------------------------------------------------------------
    // Overall state handlers
    // -------------------------------------------------------------------------

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_FLYING_SIDEKICK)
    private fun onOverallStateFlyingSidekickBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setMoveState(MOVE_STATE_STOP)
        setAttackState(ATTACK_STATE_MELEE_FLYING_SIDEKICK_RUN)
        setCameraState(CAMERA_STATE_NORMAL)
        setSydneyState(SYDNEY_STATE_WAITING)
        setHeadState(HEAD_STATE_LOOKING_AT_STAR)
        // C++: WeaponBag->Deselect()
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_MELEE_ATTACK)
    private fun onOverallStateMeleeAttackBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setMoveState(MOVE_STATE_GET_CLOSE_TO_PLAYER)
        setAttackState(ATTACK_STATE_MELEE)
        setCameraState(CAMERA_STATE_NORMAL)
        setSydneyState(SYDNEY_STATE_WAITING)
        setHeadState(HEAD_STATE_LOOKING_AT_STAR)
        // C++: WeaponBag->Deselect()
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_RANGED_ATTACK)
    private fun onOverallStateRangedAttackBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setMoveState(MOVE_STATE_ATTACK_PATTERN1)
        setAttackState(ATTACK_STATE_FLAMETHROWER)
        setCameraState(CAMERA_STATE_NORMAL)
        setHeadState(HEAD_STATE_LOOKING_AT_STAR)
        overallStateTimer = 0f
        if (fireballStartTimer == 0f) {
            fireballStartTimer = randomFloat(6.0f, 12.0f)
        }
    }

    // C++: STATE_IMPL_THINK(OVERALL_STATE_RANGED_ATTACK)
    private fun onOverallStateRangedAttackThink(dt: Float) {
        overallStateTimer -= dt
        if (overallStateTimer <= 0f) {
            overallStateTimer = 0.5f
            // C++: probability-based health pickup check
            // float curr_health = DefenseObject.Get_Health() / DefenseObject.Get_Health_Max()
            // FIXME: DefenseObject not wired
            val currHealth = 1.0f // stub
            val probability = (currHealth * 20.0f).toInt().coerceAtLeast(1)
            if ((Math.random() * probability).toInt() == 1) {
                setOverallState(OVERALL_STATE_FIND_HEALTH)
            }
        } else {
            fireballStartTimer -= dt
            if (fireballStartTimer <= 0f) {
                fireballStartTimer = randomFloat(6.0f, 12.0f)
                setOverallState(OVERALL_STATE_FIREBALL_ATTACK)
            }
        }
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_FIREBALL_ATTACK)
    private fun onOverallStateFireballAttackBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setMoveState(MOVE_STATE_GOTO_HELIPAD)
        setAttackState(ATTACK_STATE_NONE)
        setCameraState(CAMERA_STATE_NORMAL)
        setHeadState(HEAD_STATE_LOOKING_AT_STAR)
    }

    // C++: STATE_IMPL_THINK(OVERALL_STATE_FIREBALL_ATTACK) — no-op
    // (fireball state transitions are driven from MOVE_STATE_GOTO_HELIPAD_Think)

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_FIND_HEALTH)
    private fun onOverallStateFindHealthBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setAttackState(ATTACK_STATE_NONE)
        setCameraState(CAMERA_STATE_NORMAL)
        setMoveState(MOVE_STATE_RUN_TO_HEALTH)
        setHeadState(HEAD_STATE_LOOKING_AT_STAR)
    }

    // C++: STATE_IMPL_THINK(OVERALL_STATE_FIND_HEALTH)
    private fun onOverallStateFindHealthThink(dt: Float) {
        // C++: if (!Get_Action()->Is_Active()) OverallState.Set_State(RANGED_ATTACK)
        // FIXME: action system not wired in server — always transition back
        setOverallState(OVERALL_STATE_RANGED_ATTACK)
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_SYDNEY_BOLTS)
    private fun onOverallStateSydneyBoltsBegin() {
        overallStateTimer = 3.0f
        setMendozaState(MENDOZA_STATE_STANDING)
        setAttackState(ATTACK_STATE_NONE)
        setCameraState(CAMERA_STATE_NORMAL)
        setMoveState(MOVE_STATE_STOP)
        setSydneyState(SYDNEY_STATE_BOLTING)
        setHeadState(HEAD_STATE_LOOKING_AT_SYDNEY)
    }

    // C++: STATE_IMPL_THINK(OVERALL_STATE_SYDNEY_BOLTS)
    private fun onOverallStateSydneyBoltsThink(dt: Float) {
        overallStateTimer -= dt
        if (overallStateTimer <= 0f) {
            setOverallState(OVERALL_STATE_RUN_AFTER_SYDNEY)
        }
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_RUN_AFTER_SYDNEY)
    private fun onOverallStateRunAfterSydneyBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setAttackState(ATTACK_STATE_NONE)
        setCameraState(CAMERA_STATE_NORMAL)
        setMoveState(MOVE_STATE_CHASE_SYDNEY)
        setHeadState(HEAD_STATE_LOOKING_AT_SYDNEY)
        // C++: HUDInfo::Set_HUD_Help_Text(...) — client-side only
    }

    // C++: STATE_IMPL_THINK(OVERALL_STATE_RUN_AFTER_SYDNEY)
    private fun onOverallStateRunAfterSydneyThink(dt: Float) {
        // C++: if ((MendozaPos - MENDOZA_END_POS).Length() < 2.0F) → TOY_WITH_SYDNEY
        val dist = (mendozaPos - MENDOZA_END_POS).length()
        if (dist < 2.0f) {
            setOverallState(OVERALL_STATE_TOY_WITH_SYDNEY)
        }
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_TOY_WITH_SYDNEY)
    private fun onOverallStateToyWithSydneyBegin() {
        setMendozaState(MENDOZA_STATE_STANDING)
        setAttackState(ATTACK_STATE_SYDNEY)
        setCameraState(CAMERA_STATE_NORMAL)
        setHeadState(HEAD_STATE_LOOKING_AT_SYDNEY)
    }

    // C++: STATE_IMPL_THINK(OVERALL_STATE_TOY_WITH_SYDNEY)
    private fun onOverallStateToyWithSydneyThink(dt: Float) {
        if (moveState != MOVE_STATE_STOP) {
            val dist = (mendozaPos - MENDOZA_END_POS).length()
            if (dist < 1.0f) {
                setMoveState(MOVE_STATE_STOP)
            }
        }
    }

    // C++: STATE_IMPL_BEGIN(OVERALL_STATE_DEATH_SEQUENCE)
    private fun onOverallStateDeathSequenceBegin() {
        // C++: DefenseObject.Set_Skin((ArmorType)1); DefenseObject.Set_Shield_Type((ArmorType)1)
        // Makes Mendoza effectively invulnerable during the cutscene
        setMendozaState(MENDOZA_STATE_STANDING)
        setAttackState(ATTACK_STATE_NONE)
        setCameraState(CAMERA_STATE_FACE_ZOOM)
        setMoveState(MOVE_STATE_STOP)
        setHeadState(HEAD_STATE_NONE)
    }

    // -------------------------------------------------------------------------
    // Sydney state handlers
    // -------------------------------------------------------------------------

    // C++: STATE_IMPL_BEGIN(SYDNEY_STATE_BOLTING)
    private fun onSydneyStateBoltingBegin() {
        // C++: Sydney -> Get_Action()->Goto(params) — pathfind to SYDNEY_END_POS at run speed
        // FIXME: action system not wired in server
    }

    // C++: STATE_IMPL_THINK(SYDNEY_STATE_BOLTING)
    private fun onSydneyStateBoltingThink(dt: Float) {
        // C++: if ((SYDNEY_END_POS - syd_pos).Length() <= 5.0F) SydneyState = TRIPPING
        // FIXME: Sydney position not wired; transition skipped
    }

    // C++: STATE_IMPL_BEGIN(SYDNEY_STATE_TRIPPING)
    private fun onSydneyStateTrippingBegin() {
        // C++: sydney->Set_Blended_Animation("S_A_HUMAN.H_A_690A", false, 7.0F)
        // C++: sydney->Get_Human_State()->Set_State(LOCKED_ANIMATION)
        // C++: Set_Anim_Speed_Scale(2.0F) — client-side only
    }

    // C++: STATE_IMPL_THINK(SYDNEY_STATE_TRIPPING)
    private fun onSydneyStateTrippingThink(dt: Float) {
        // C++: if (anim complete && OverallState == TOY_WITH_SYDNEY) → GETTING_UP
        // FIXME: anim completion not tracked server-side
    }

    // C++: STATE_IMPL_BEGIN(SYDNEY_STATE_GETTING_UP)
    private fun onSydneyStateGettingUpBegin() {
        // C++: sydney->Set_Blended_Animation("S_A_HUMAN.H_A_H11C", false)
        // C++: sydney->Get_Human_State()->Set_State(LOCKED_ANIMATION) — client-side
    }

    // C++: STATE_IMPL_THINK(SYDNEY_STATE_GETTING_UP)
    private fun onSydneyStateGettingUpThink(dt: Float) {
        // C++: if (sydney anim complete) SydneyState = COWERING
        // FIXME: anim completion not tracked server-side
    }

    // C++: STATE_IMPL_BEGIN(SYDNEY_STATE_COWERING)
    private fun onSydneyStateCoweringBegin() {
        // C++: sydney->Get_Action()->Reset(100)
        // C++: sydney->Set_Blended_Animation("S_A_HUMAN.H_A_A0A0_L50", false)
        // C++: Set target frame 7 — client-side animation
        sydneyStateTimer = 1.0f
    }

    // C++: STATE_IMPL_END(SYDNEY_STATE_COWERING)
    private fun onSydneyStateCoweringEnd() {
        // C++: sydney->Get_Human_State()->Set_State(ANIMATION)
    }

    // C++: STATE_IMPL_THINK(SYDNEY_STATE_COWERING)
    private fun onSydneyStateCoweringThink(dt: Float) {
        // C++: sydney->Set_Targeting(MENDOZA_END_POS, false) — look at Mendoza
        sydneyStateTimer -= dt
        if (sydneyStateTimer <= 0f) {
            sydneyStateTimer = randomFloat(1.0f, 2.0f)
            // C++: Toggle sydney anim target frame 7 ↔ 12 (flinching loop)
            // C++: apply 2 pts of damage to Sydney — FIXME: damage system not wired
        }
    }

    // C++: STATE_IMPL_BEGIN(SYDNEY_STATE_STRIKE_A_POSE)
    private fun onSydneyStateStrikeAPoseBegin() {
        // C++: sydney->Set_Animation("S_B_HUMAN.H_B_A0A0_14", true) — looping pose
        // C++: snap Sydney to SYDNEY_END_POS rotated 90 degrees
        // Client-side only
    }

    // -------------------------------------------------------------------------
    // Move state handlers
    // -------------------------------------------------------------------------

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_STOP)
    private fun onMoveStateStopBegin() {
        // C++: Get_Action()->Reset(100) — stop all pathfinding
    }

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_GET_CLOSE_TO_PLAYER)
    private fun onMoveStateGetCloseToPlayerBegin() {
        moveStateTimer = 0.0f
    }

    // C++: STATE_IMPL_END(MOVE_STATE_GET_CLOSE_TO_PLAYER)
    private fun onMoveStateGetCloseToPlayerEnd() {
        // C++: Get_Action()->Reset(100)
    }

    // C++: STATE_IMPL_THINK(MOVE_STATE_GET_CLOSE_TO_PLAYER)
    private fun onMoveStateGetCloseToPlayerThink(dt: Float) {
        moveStateTimer -= dt
        if (moveStateTimer <= 0f) {
            moveStateTimer = 1.0f
            // C++: if (COMBAT_STAR != NULL && StarPos.X > -70.0F)
            //          Get_Action()->Goto(params) — pathfind to player
            //      else → OVERALL_STATE_FIREBALL_ATTACK
            // FIXME: pathfind action not wired in server
        }
    }

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_ATTACK_PATTERN1)
    private fun onMoveStateAttackPattern1Begin() {
        moveStateTimer = 0.0f
    }

    // C++: STATE_IMPL_END(MOVE_STATE_ATTACK_PATTERN1)
    private fun onMoveStateAttackPattern1End() {
        // C++: Get_Action()->Reset(100)
    }

    // C++: STATE_IMPL_THINK(MOVE_STATE_ATTACK_PATTERN1)
    private fun onMoveStateAttackPattern1Think(dt: Float) {
        // C++: Set_Targeting(StarPos+Z1, true) — always face player
        moveStateTimer -= dt
        if (moveStateTimer <= 0f) {
            moveStateTimer = randomFloat(1.0f, 7.0f)
            // C++: Find random spot near player and pathfind there (ignore facing)
            // FIXME: pathfind not wired in server
        }
    }

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_ATTACK_PATTERN2)
    private fun onMoveStateAttackPattern2Begin() {
        moveStateTimer = 0.0f
    }

    // C++: STATE_IMPL_END(MOVE_STATE_ATTACK_PATTERN2)
    private fun onMoveStateAttackPattern2End() {
        // C++: Get_Action()->Reset(100)
    }

    // C++: STATE_IMPL_THINK(MOVE_STATE_ATTACK_PATTERN2)
    private fun onMoveStateAttackPattern2Think(dt: Float) {
        moveStateTimer -= dt
        if (moveStateTimer <= 0f) {
            moveStateTimer = randomFloat(1.0f, 5.0f)
            // C++: Find random spot and pathfind there — FIXME: not wired
        }
    }

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_GOTO_HELIPAD)
    private fun onMoveStateGotoHelipadBegin() {
        // C++: Get_Action()->Goto(HELIPAD_CENTER_POS at full speed)
        // FIXME: pathfind action not wired in server
    }

    // C++: STATE_IMPL_END(MOVE_STATE_GOTO_HELIPAD)
    private fun onMoveStateGotoHelipadEnd() {
        // C++: Get_Action()->Reset(100)
    }

    // C++: STATE_IMPL_THINK(MOVE_STATE_GOTO_HELIPAD)
    private fun onMoveStateGotoHelipadThink(dt: Float) {
        val dist2 = (mendozaPos - HELIPAD_CENTER_POS).length2()
        if (dist2 <= 1.0f) {
            setAttackState(ATTACK_STATE_FIREBALL)
            setMoveState(MOVE_STATE_STOP)
        }
    }

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_RUN_TO_HEALTH)
    private fun onMoveStateRunToHealthBegin() {
        // C++: PowerUpGameObj *powerup = Find_Best_Powerup()
        // C++: Get_Action()->Goto(powerup position, 1.0F, 0.125F)
        // FIXME: powerup search and pathfind not wired in server
    }

    // C++: STATE_IMPL_END(MOVE_STATE_RUN_TO_HEALTH)
    private fun onMoveStateRunToHealthEnd() {
        // C++: Get_Action()->Reset(100)
    }

    // C++: STATE_IMPL_BEGIN(MOVE_STATE_CHASE_SYDNEY)
    private fun onMoveStateChaseSydneyBegin() {
        // C++: Get_Action()->Goto(MENDOZA_END_POS at 0.75 speed)
        // FIXME: pathfind action not wired in server
    }

    // -------------------------------------------------------------------------
    // Head state handlers
    // -------------------------------------------------------------------------

    // C++: STATE_IMPL_BEGIN(HEAD_STATE_NONE)
    private fun onHeadStateNoneBegin() {
        // C++: Cancel_Look_At()
    }

    // C++: STATE_IMPL_THINK(HEAD_STATE_LOOKING_AT_STAR)
    private fun onHeadStateLookingAtStarThink(dt: Float) {
        // C++: Vector3 position(StarPos); position.Z += 1.7F
        // C++: Look_At(position, 100) or Update_Look_At(position)
        // Client-side head tracking animation — skipped on server
    }

    // C++: STATE_IMPL_THINK(HEAD_STATE_LOOKING_AT_SYDNEY)
    private fun onHeadStateLookingAtSydneyThink(dt: Float) {
        // C++: Get Sydney position, add 1.7 to Z, Look_At / Update_Look_At
        // Client-side head tracking animation — skipped on server
    }

    // -------------------------------------------------------------------------
    // Camera state handlers
    // -------------------------------------------------------------------------

    // C++: On_CAMERA_STATE_NORMAL_Begin — no-op
    private fun onCameraStateNormalBegin() {
        // no-op
    }

    // C++: On_CAMERA_STATE_FACE_ZOOM_Begin
    private fun onCameraStateFaceZoomBegin() {
        // C++: CameraBoneModel->Set_Transform(COMBAT_CAMERA->Get_Transform())
        // C++: COMBAT_CAMERA->Set_Host_Model(CameraBoneModel)
        // C++: TimeManager::Set_Time_Scale(0.25F)
        // C++: GameObjManager::Activate_Cinematic_Freeze(false)
        // Client-side only — skipped on server
    }

    // C++: On_CAMERA_STATE_FACE_ZOOM_Think
    private fun onCameraStateFaceZoomThink(dt: Float) {
        // C++: moves camera toward Mendoza's head bone until close enough,
        //      then triggers MENDOZA_STATE_PACK_EXPLODING and CAMERA_STATE_WAYPATH_FOLLOW
        // Client-side camera movement — skipped on server
        // Server drives state directly: pack exploding triggered by overall death sequence
    }

    // C++: On_CAMERA_STATE_WAYPATH_FOLLOW_Begin
    private fun onCameraStateWaypathFollowBegin() {
        // C++: TimeManager::Set_Time_Scale(1.0F)
        // C++: Load waypath 3000100 into CameraSpline
        // C++: CameraStateTimer = 8.0F
        cameraStateTimer = 8.0f
        // Client-side only — skipped on server
    }

    // C++: On_CAMERA_STATE_WAYPATH_FOLLOW_Think
    private fun onCameraStateWaypathFollowThink(dt: Float) {
        cameraStateTimer -= dt
        if (cameraStateTimer <= 0f) {
            setCameraState(CAMERA_STATE_LOOK_AT_DEAD_BOSS)
        }
        // C++: spline evaluation and camera movement — client-side only
    }

    // C++: On_CAMERA_STATE_LOOK_AT_DEAD_BOSS_Begin
    private fun onCameraStateLookAtDeadBossBegin() {
        cameraStateTimer = 7.0f
        // C++: TimeManager::Set_Time_Scale(0.5F) — client-side
    }

    // C++: On_CAMERA_STATE_LOOK_AT_DEAD_BOSS_Think
    private fun onCameraStateLookAtDeadBossThink(dt: Float) {
        cameraStateTimer -= dt
        if (cameraStateTimer <= 0f) {
            setCameraState(CAMERA_STATE_NORMAL)
            // C++: TimeManager::Set_Time_Scale(1.0F)
            // C++: COMBAT_CAMERA->Set_Host_Model(NULL) — client-side
            // C++: CombatManager::Mission_Complete(true)
            // FIXME: mission complete not wired; log as server event
        }
    }

    // -------------------------------------------------------------------------
    // Attack state handlers
    // -------------------------------------------------------------------------

    // C++: On_ATTACK_STATE_NONE_Begin / Think — no-op
    private fun onAttackStateNoneBegin() { /* no-op */ }
    private fun onAttackStateNoneThink(dt: Float) { /* no-op */ }

    // C++: On_ATTACK_STATE_MELEE_Begin
    private fun onAttackStateMeleeBegin() {
        nextMeleeAttackTime = randomFloat(0.125f, 0.7f)
    }

    // C++: On_ATTACK_STATE_MELEE_Think
    private fun onAttackStateMeleeThink(dt: Float) {
        nextMeleeAttackTime -= dt
        if (nextMeleeAttackTime <= 0f) {
            val dist2 = (mendozaPos - starPos).length2()
            if (dist2 < 4.0f) {
                val attackId = (Math.random() * 3).toInt()
                when (attackId) {
                    0 -> setAttackState(ATTACK_STATE_MELEE_CRESENT_KICK)
                    1 -> setAttackState(ATTACK_STATE_MELEE_SIDE_KICK)
                    2 -> setAttackState(ATTACK_STATE_MELEE_PUNCH)
                }
            }
        }
    }

    // C++: STATE_IMPL_BEGIN(ATTACK_STATE_MELEE_FLYING_SIDEKICK_RUN)
    private fun onAttackStateMeleeFlyingSidekickRunBegin() {
        hasMeleeAttackHit = false
        lastMeleeAnimFrame = 0f
        sideKickPos = Vector3(starPos.x, starPos.y, starPos.z)
        // C++: Set_Targeting(StarPos, false)
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_A0A1", false) — client-side
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
        // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0), Set_Target_Frame(4)
        // C++: Enable_Objects_Simulation(false)
    }

    // C++: STATE_IMPL_THINK(ATTACK_STATE_MELEE_FLYING_SIDEKICK_RUN)
    private fun onAttackStateMeleeFlyingSidekickRunThink(dt: Float) {
        // C++: if (anim complete) → ATTACK_STATE_MELEE_FLYING_SIDEKICK
        // C++: else fly toward SideKickPos
        // FIXME: anim completion not tracked; transition skipped on server
    }

    // C++: STATE_IMPL_BEGIN(ATTACK_STATE_MELEE_FLYING_SIDEKICK)
    private fun onAttackStateMeleeFlyingSidekickBegin() {
        hasMeleeAttackHit = false
        lastMeleeAnimFrame = 0f
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_FLYKICK", false) — client-side
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_MELEE_FLYING_SIDEKICK)
    private fun onAttackStateMeleeFlyingSidekickEnd() {
        // C++: play animation backwards for landing effect — client-side
        // C++: HumanState.Set_State(ANIMATION)
    }

    // C++: STATE_IMPL_THINK(ATTACK_STATE_MELEE_FLYING_SIDEKICK)
    private fun onAttackStateMeleeFlyingSidekickThink(dt: Float) {
        // C++: fly toward SideKickPos; Apply_Bone_Collision_Damage("C L FOOT")
        // C++: if close or hit → Enable_Objects_Simulation(true); OverallState = MELEE_ATTACK
        // FIXME: fly movement and collision not wired; transition skipped
    }

    // C++: On_ATTACK_STATE_MELEE_CRESENT_KICK_Begin
    private fun onAttackStateMeleeCresentKickBegin() {
        hasMeleeAttackHit = false
        lastMeleeAnimFrame = 0f
        // C++: MoveState.Halt_State()
        // C++: Set_Targeting(StarPos, false)
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_CRESENTKICK", false, 0, true) — reversed
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
        // C++: Attach_Sound("Fight Whoosh Sound Twiddler", "ROOTTRANSFORM") — client-side
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_MELEE_CRESENT_KICK)
    private fun onAttackStateMeleeCresentKickEnd() {
        // C++: HumanState.Set_State(ANIMATION); MoveState.Resume_State()
    }

    // C++: On_ATTACK_STATE_MELEE_CRESENT_KICK_Think
    private fun onAttackStateMeleeCresentKickThink(dt: Float) {
        // C++: if complete → AttackState = MELEE
        // C++: else if frame < 7 → Apply_Bone_Collision_Damage("C R FOOT")
        // FIXME: anim frame not tracked server-side
    }

    // C++: On_ATTACK_STATE_MELEE_SIDE_KICK_Begin
    private fun onAttackStateMeleeSideKickBegin() {
        hasMeleeAttackHit = false
        // C++: MoveState.Halt_State()
        // C++: Set_Targeting(StarPos, false)
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_SIDEKICK", false) — client-side
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
        // C++: Attach_Sound("Fight Whoosh Sound Twiddler", "ROOTTRANSFORM") — client-side
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_MELEE_SIDE_KICK)
    private fun onAttackStateMeleeSideKickEnd() {
        // C++: HumanState.Set_State(ANIMATION); MoveState.Resume_State()
    }

    // C++: On_ATTACK_STATE_MELEE_SIDE_KICK_Think
    private fun onAttackStateMeleeSideKickThink(dt: Float) {
        // C++: if complete → AttackState = SIDE_KICK_RETRACT
        // C++: else if frame > 6 → Apply_Bone_Collision_Damage("C L FOOT")
        // FIXME: anim frame not tracked server-side
    }

    // C++: On_ATTACK_STATE_MELEE_SIDE_KICK_RETRACT_Begin
    private fun onAttackStateMeleeSideKickRetractBegin() {
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_SIDEKICK", false, 0, true) — reversed
        // C++: HumanState.Set_State(LOCKED_ANIMATION) — client-side
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_MELEE_SIDE_KICK_RETRACT)
    private fun onAttackStateMeleeSideKickRetractEnd() {
        // C++: HumanState.Set_State(ANIMATION); MoveState.Resume_State()
    }

    // C++: On_ATTACK_STATE_MELEE_SIDE_KICK_RETRACT_Think
    private fun onAttackStateMeleeSideKickRetractThink(dt: Float) {
        // C++: if complete → AttackState = MELEE — FIXME: anim not tracked
    }

    // C++: On_ATTACK_STATE_MELEE_PUNCH_Begin
    private fun onAttackStateMeleePunchBegin() {
        hasMeleeAttackHit = false
        lastMeleeAnimFrame = 0f
        attackingBoneName = "C R HAND"
        // C++: MoveState.Halt_State()
        // C++: Set_Targeting(StarPos, false)
        // C++: Set_Blended_Animation("S_A_HUMAN.H_A_PunchCombo", false) — client-side
        // C++: HumanState.Set_State(LOCKED_ANIMATION)
        // C++: Attach_Sound("Fight Whoosh Sound Twiddler", "ROOTTRANSFORM") — client-side
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_MELEE_PUNCH)
    private fun onAttackStateMeleePunchEnd() {
        // C++: HumanState.Set_State(ANIMATION); MoveState.Resume_State()
    }

    // C++: On_ATTACK_STATE_MELEE_PUNCH_Think
    private fun onAttackStateMeleePunchThink(dt: Float) {
        // C++: tracks frame-based "PASSED_FRAME" boundaries to switch AttackingBoneName
        //      and Apply_Bone_Collision_Damage at the right frames
        // FIXME: anim frame tracking not wired server-side
    }

    // C++: STATE_IMPL_BEGIN(ATTACK_STATE_FLAMETHROWER)
    private fun onAttackStateFlamethrowerBegin() {
        // C++: WeaponBag->Select_Weapon_ID(def.WeaponDefID)
        attackStateTimer = 0.0f
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_FLAMETHROWER)
    private fun onAttackStateFlamethrowerEnd() {
        // C++: Set_Boolean_Control(WEAPON_FIRE_SECONDARY, false)
    }

    // C++: STATE_IMPL_THINK(ATTACK_STATE_FLAMETHROWER)
    private fun onAttackStateFlamethrowerThink(dt: Float) {
        // C++: if player is within 10m and in front (relative X > 0 and abs(Y) < 5) → fire secondary
        // C++: Set_Boolean_Control(WEAPON_FIRE_SECONDARY, turn_on_weapon)
        // FIXME: weapon controls not wired in server
    }

    // C++: STATE_IMPL_BEGIN(ATTACK_STATE_FIREBALL)
    private fun onAttackStateFireballBegin() {
        // C++: WeaponBag->Select_Weapon_ID(def.WeaponDefID)
        attackStateTimer = randomFloat(5.0f, 9.0f)
        randomTargetTimer = 0f
        shootGroundPos = Vector3(starPos.x, starPos.y, starPos.z)
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_FIREBALL)
    private fun onAttackStateFireballEnd() {
        // C++: Set_Boolean_Control(WEAPON_FIRE_PRIMARY, false)
        // C++: Set_Boolean_Control(WEAPON_FIRE_SECONDARY, false)
    }

    // C++: STATE_IMPL_THINK(ATTACK_STATE_FIREBALL)
    private fun onAttackStateFireballThink(dt: Float) {
        randomTargetTimer -= dt
        if (randomTargetTimer <= 0f) {
            randomTargetTimer = randomFloat(0.25f, 0.6f)
            shootGroundPos = Vector3(
                starPos.x + randomFloat(-1.0f, 1.0f),
                starPos.y + randomFloat(-1.0f, 1.0f),
                starPos.z + randomFloat(0.0f, 2.0f),
            )
        }
        // C++: Set_Targeting(ShootGroundPos, true)
        // C++: cast weapon down muzzle to check safe-to-fire distance
        // C++: set weapon fire primary/secondary based on angle + safety — FIXME: not wired

        attackStateTimer -= dt
        if (attackStateTimer <= 0f) {
            setOverallState(OVERALL_STATE_RANGED_ATTACK)
        }
    }

    // C++: STATE_IMPL_BEGIN(ATTACK_STATE_HANDGUN)
    private fun onAttackStateHandgunBegin() {
        // C++: WeaponBag->Select_Weapon_ID(def.SecondaryWeaponDefID)
        attackStateTimer = 2.0f
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_HANDGUN)
    private fun onAttackStateHandgunEnd() {
        // C++: Set_Boolean_Control(WEAPON_FIRE_PRIMARY, false)
        // C++: WeaponBag->Deselect()
    }

    // C++: STATE_IMPL_THINK(ATTACK_STATE_HANDGUN)
    private fun onAttackStateHandgunThink(dt: Float) {
        // C++: if relative X > 0 → fire primary
        // FIXME: weapon controls not wired in server
        attackStateTimer -= dt
        if (attackStateTimer <= 0f) {
            // C++: determine which attack state to revert to based on OverallState
            when (overallState) {
                OVERALL_STATE_MELEE_ATTACK    -> setAttackState(ATTACK_STATE_MELEE)
                OVERALL_STATE_RANGED_ATTACK   -> setAttackState(ATTACK_STATE_FLAMETHROWER)
                OVERALL_STATE_FIREBALL_ATTACK -> setAttackState(ATTACK_STATE_FIREBALL)
            }
        }
    }

    // C++: STATE_IMPL_BEGIN(ATTACK_STATE_SYDNEY)
    private fun onAttackStateSydneyBegin() {
        // C++: WeaponBag->Select_Weapon_ID(def.WeaponDefID)
        attackStateTimer = 0f
    }

    // C++: STATE_IMPL_END(ATTACK_STATE_SYDNEY)
    private fun onAttackStateSydneyEnd() {
        // C++: Set_Boolean_Control(WEAPON_FIRE_SECONDARY, false)
    }

    // C++: STATE_IMPL_THINK(ATTACK_STATE_SYDNEY)
    private fun onAttackStateSydneyThink(dt: Float) {
        attackStateTimer -= dt
        if (attackStateTimer <= 0f) {
            attackStateTimer = randomFloat(0.5f, 1.5f)
            // C++: get Sydney pos, pick random circle pos around her feet
            shootGroundPos = Vector3(0f, 0f, 0f) // FIXME: use sydney position
        }
        // C++: Set_Targeting(ShootGroundPos, true)
        // C++: Set_Boolean_Control(WEAPON_FIRE_SECONDARY, true)
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    // C++: bool Fly_Move(const Vector3 &vector)
    // Moves Mendoza through the air with AABox collision check against the star's model.
    // Returns true if contact was made.
    private fun flyMove(vector: Vector3): Boolean {
        // C++: AABoxClass collision_box + move vector → Cast_AABox against COMBAT_STAR model
        // FIXME: physics collision query not wired in server
        // val newPos = mendozaPos + vector
        // setPosition(newPos)
        return false
    }

    // C++: bool Apply_Bone_Collision_Damage(const char *bone_name)
    // Checks if the named bone swept through the player's model; if so, applies 5pt "steel" damage.
    private fun applyBoneCollisionDamage(boneName: String): Boolean {
        // C++: Simple_Evaluate_Bone(boneIndex, currFrame, &curr_bone_tm)
        // C++: Simple_Evaluate_Bone(boneIndex, lastFrame, &last_bone_tm)
        // C++: AABoxCollisionTestClass → Cast_AABox against COMBAT_STAR model
        // C++: if hit → OffenseObjectClass(5.0F, 1) → COMBAT_STAR->Apply_Damage_Extended(...)
        // C++: Attach_Sound("Fight Impact Sound Twiddler", "ROOTTRANSFORM") — client-side
        // FIXME: bone evaluation and collision queries not wired in server
        return false
    }

    // C++: void Attach_Sound(const char *sound_name, const char *bone_name) — client-side only
    private fun attachSound(soundName: String, boneName: String) {
        // C++: WWAudioClass::Get_Instance()->Create_Sound(sound_name)
        // C++: sound->Attach_To_Object(Peek_Model(), bone_name); sound->Add_To_Scene()
        // Client-side audio — skipped on server
    }

    // C++: void Attach_Fire_Sound() — client-side only
    private fun attachFireSound() {
        // C++: WWAudioClass::Get_Instance()->Create_Sound("SFX.Fire_Small_01")
        // C++: sound->Attach_To_Object(Peek_Model()); sound->Add_To_Scene()
        // Client-side audio — skipped on server
    }

    // C++: void Spawn_Health_Powerups()
    // Spawns 15 "POW_Health_025" powerups scattered across the 3 boss-area boxes.
    private fun spawnHealthPowerups() {
        // C++: for each of 15 powerups: pick random area box, random XY inside box,
        //      raycast down to find terrain Z, Create_Object("POW_Health_025")
        // FIXME: ObjectLibraryManager.createObject and pathfind ray-cast not wired in server
    }

    // C++: PowerUpGameObj* Find_Best_Powerup()
    // Iterates all game objects looking for powerups inside the boss area boxes,
    // rates them by distance to Mendoza vs distance to star.
    private fun findBestPowerup(): Any? {
        // C++: loops GameObjManager::Get_Game_Obj_List() checking As_PowerUpGameObj()
        //      rates by (distToMe * 0.5 + distToStar * 0.5)
        // FIXME: GameObjManager not wired in server
        return null
    }

    // Simple random float helper mirroring WWMath::Random_Float
    private fun randomFloat(min: Float, max: Float): Float {
        return min + (Math.random() * (max - min)).toFloat()
    }

    // Simple Vector3 length² helper
    private fun Vector3.length2(): Float = x * x + y * y + z * z

    // Simple Vector3 length helper
    private fun Vector3.length(): Float = kotlin.math.sqrt(length2())
}
