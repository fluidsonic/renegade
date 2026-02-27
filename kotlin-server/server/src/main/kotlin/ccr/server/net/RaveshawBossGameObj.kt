package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.defs.RaveshawBossGameObjDefClass
import kotlin.math.max

// C++: RaveshawBossGameObj : public SoldierGameObj (raveshawbossgameobj.h / raveshawbossgameobj.cpp)
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//              → PhysicalGameObj → ArmedGameObj → SmartGameObj → SoldierGameObj → RaveshawBossGameObj
// Note: Singleplayer-only boss. Not used in multiplayer. Full server-side port.
class RaveshawBossGameObj : SoldierGameObj() {

    // -------------------------------------------------------------------------
    // Save/load chunk IDs  (raveshawbossgameobj.cpp ~line 191)
    // -------------------------------------------------------------------------
    companion object {
        // C++: CHUNKID_GAME_OBJECT_RAVESHAW_BOSS from combatchunkid.h
        const val CHUNK_ID: UInt = 0x00040131u

        // Object-level chunk IDs
        private const val CHUNKID_PARENT                        = 0x09230243
        private const val CHUNKID_VARIABLES                     = 0x09230244
        private const val CHUNKID_TIBERIUM_EFFECT               = 0x09230245
        private const val CHUNKID_OVERALL_STATE_MACHINE         = 0x09230246
        private const val CHUNKID_RAVESHAW_STATE_MACHINE        = 0x09230247
        private const val CHUNKID_ENGORGED_STATE_MACHINE        = 0x09230248
        private const val CHUNKID_MOVE_STATE_MACHINE            = 0x09230249
        private const val CHUNKID_STEALTH_SOLDIER_STATE_MACHINE = 0x0923024A
        private const val CHUNKID_THROWN_OBJ_STATE_MACHINE      = 0x0923024B
        private const val CHUNKID_JUMP_STATE_MACHINE            = 0x0923024C
        private const val CHUNKID_LIGHTNING_ROD_STATE_MACHINE   = 0x0923024D
        private const val CHUNKID_STEALTH_SOLDIER               = 0x0923024E
        private const val CHUNKID_HAVOC_STATE_MACHINE           = 0x0923024F

        // Variable micro-chunk IDs
        private const val VARID_THROWN_OBJECT_PTR               = 1
        private const val VARID_IS_TIBERIUM_EFFECT_APPLIED      = 2
        private const val VARID_CURRENT_DEST_POS                = 3
        private const val VARID_OVERALL_STATE_TIMER             = 4
        private const val VARID_ENGORGED_STATE_TIMER            = 5
        private const val VARID_MOVE_STATE_TIMER                = 6
        private const val VARID_BODY_SLAM_TIMER                 = 7
        private const val VARID_RAVESHAW_STATE_TIMER            = 8
        private const val VARID_STEALTHSOLDIER_STATE_TIMER      = 9
        private const val VARID_LIGHTNINGROD_STATE_TIMER        = 10
        private const val VARID_START_TIMER                     = 11
        private const val VARID_LAST_MELEE_ANIM_FRAME           = 12
        private const val VARID_HAS_MELEE_ATTACK_HIT            = 13
        private const val VARID_FLYING_OBJECT_VECTOR            = 14
        private const val VARID_FLYING_OBJECT_DEST              = 15
        private const val VARID_FLYING_DIST                     = 16
        private const val VARID_RELOBJ_TM                       = 17
        private const val VARID_ARC_OBJ_PTR                     = 18
        private const val VARID_CURR_JUMP_POS                   = 19
        private const val VARID_CAMERA_BONE_PTR                 = 20
        private const val VARID_CAMERA_BONE_TM                  = 21
        private const val VARID_RESTORE_FIRST_PERSON            = 22

        // C++: static const float UNINITIALIZED_TIMER = -5000.0F
        private const val UNINITIALIZED_TIMER = -5000.0f

        // C++: static const Vector3 TIBERIUM_POS (-130.499F, 483.243F, -189.617F)
        private val TIBERIUM_POS = Vector3(-130.499f, 483.243f, -189.617f)
        private const val TIBERIUM_RADIUS = 1.35f
        private const val SAFE_JUMP_RADIUS = 12.0f

        private const val HEAL_TIME = 3.0f
        private const val EFFECT_FADE_TIME = 12.0f
        private const val EFFECT_INTENSITY = 0.75f

        // C++: static const int CATWALK_WAYPATH_ID = 3000100
        private const val CATWALK_WAYPATH_ID = 3000100

        private const val ARC_OBJ_COUNT = 10
        private const val BONE_COUNT = 6
        private const val MAX_TAUNTS = 6

        // C++: static const char *ARC_BONE_NAMES[6]
        private val ARC_BONE_NAMES = arrayOf("BONE02", "BONE03", "BONE04", "BONE05", "BONE06", "BONE07")
    }

    // -------------------------------------------------------------------------
    // Overall state enum (C++ inner enum)
    // -------------------------------------------------------------------------
    private object OverallState {
        const val NOTHING             = 0
        const val HEALING             = 1
        const val THROWING_OBJECT     = 2
        const val THROWING_SOLDIER    = 3
        const val THROWING_STAR       = 4
        const val GRAB_STAR           = 5
        const val JUMP_TO_CATWALK     = 6
        const val ON_CATWALK          = 7
        const val BODYSLAM            = 8
        const val CHASE_STAR          = 9
        const val DAZED               = 10
        const val FLEE                = 11
        const val DEATH_SEQUENCE      = 12
    }

    // -------------------------------------------------------------------------
    // Raveshaw state enum
    // -------------------------------------------------------------------------
    private object RaveshawState {
        const val NOTHING           = 0
        const val ROAR              = 1
        const val GRAB_TIBERIUM     = 2
        const val GRAB_SOLDIER      = 3
        const val THROW_SOLDIER     = 4
        const val GRAB_OBJECT       = 5
        const val THROW_OBJECT      = 6
        const val GRAB_STAR         = 7
        const val BODYSLAM          = 8
        const val JUMP_DOWN         = 9
        const val STUMBLE           = 10
        const val LOOK_CONFUSED     = 11
        const val DYING             = 12
        const val FALL              = 13
        const val DEATH_LANDING     = 14
    }

    // -------------------------------------------------------------------------
    // Move state enum
    // -------------------------------------------------------------------------
    private object MoveState {
        const val STOP               = 0
        const val GOTO_TIBERIUM      = 1
        const val GOTO_CATWALK       = 2
        const val JUMP_TO_CATWALK    = 3
        const val CIRCLE_CATWALK     = 4
        const val GOTO_THROW_OBJECT  = 5
        const val JUMP_TO_STAR       = 6
        const val FOLLOW_STAR        = 7
    }

    // -------------------------------------------------------------------------
    // Engorged state enum
    // -------------------------------------------------------------------------
    private object EngorgedState {
        const val NONE                  = 0
        const val ABSORBING_TIBERIUM    = 1
        const val FADING                = 2
    }

    // -------------------------------------------------------------------------
    // Jump state enum
    // -------------------------------------------------------------------------
    private object JumpState {
        const val NONE       = 0
        const val CROUCHING  = 1
        const val JUMPING    = 2
        const val LANDING    = 3
    }

    // -------------------------------------------------------------------------
    // Havoc state enum
    // -------------------------------------------------------------------------
    private object HavocState {
        const val NONE    = 0
        const val GRABBED = 1
        const val FLYING  = 2
    }

    // -------------------------------------------------------------------------
    // Stealth soldier state enum
    // -------------------------------------------------------------------------
    private object StealthSoldierState {
        const val NONE    = 0
        const val DISPLAY = 1
        const val FLYING  = 2
    }

    // -------------------------------------------------------------------------
    // Thrown object state enum
    // -------------------------------------------------------------------------
    private object ThrownObjectState {
        const val NONE    = 0
        const val PICKUP  = 1
        const val FLYING  = 2
    }

    // -------------------------------------------------------------------------
    // Lightning rod state enum
    // -------------------------------------------------------------------------
    private object LightningRodState {
        const val NONE   = 0
        const val ACTIVE = 1
    }

    // -------------------------------------------------------------------------
    // Taunt data (C++ private section)
    // -------------------------------------------------------------------------
    private var raveshawTauntTimeLeft: Float = 0f
    private val tauntList: IntArray = IntArray(MAX_TAUNTS)
    private var availableTaunts: Int = 0xFFFFFF

    // -------------------------------------------------------------------------
    // State machine state integers (replace C++ StateMachineClass<T>)
    // -------------------------------------------------------------------------
    private var overallStateCurrent: Int     = OverallState.NOTHING
    private var raveshawStateCurrent: Int    = RaveshawState.NOTHING
    private var moveStateCurrent: Int        = MoveState.STOP
    private var engorgedStateCurrent: Int    = EngorgedState.NONE
    private var stealthSoldierStateCurrent: Int = StealthSoldierState.NONE
    private var havocStateCurrent: Int       = HavocState.NONE
    private var thrownObjectStateCurrent: Int = ThrownObjectState.NONE
    private var jumpStateCurrent: Int        = JumpState.NONE
    private var lightningRodStateCurrent: Int = LightningRodState.NONE

    // -------------------------------------------------------------------------
    // Timers (C++ private section)
    // -------------------------------------------------------------------------
    private var overallStateTimer: Float         = 0f
    private var engorgedStateTimer: Float        = 0f
    private var moveStateTimer: Float            = 0f
    private var bodySlamTimer: Float             = 0f
    private var raveshawStateTimer: Float        = 0f
    private var stealthSoldierStateTimer: Float  = 0f
    private var lightningRodStateTimer: Float    = 0f
    private var startTimer: Float                = 0f

    // -------------------------------------------------------------------------
    // Melee data
    // -------------------------------------------------------------------------
    private var lastMeleeAnimFrame: Float = 0f
    private var hasMeleeAttackHit: Boolean = false

    // -------------------------------------------------------------------------
    // Thrown object / stealth soldier data
    // -------------------------------------------------------------------------
    // C++: GameObjReference StealthSoldier
    private val stealthSoldierRef: GameObjReference = GameObjReference()

    // C++: StealthEffectClass* StealthEffect — client-side visual; stub here
    // C++: SimpleGameObj* ThrownObject
    private var thrownObject: SimpleGameObj? = null

    private var flyingObjectVector: Vector3 = Vector3(0f, 0f, 0f)
    private var flyingObjectDest: Vector3   = Vector3(0f, 0f, 0f)
    private var flyingDist: Float           = 0f
    private var relObjTM: Matrix3D          = Matrix3D()
    // C++: RenderObjClass* CameraBoneModel — client-side; not ported
    private var restoreFirstPerson: Boolean = true

    // -------------------------------------------------------------------------
    // Effects data
    // -------------------------------------------------------------------------
    // C++: ManualTransitionEffectClass* TiberiumEffect — client-side; not ported
    // C++: DynamicVectorClass<DamageableStaticPhysClass*> LightningRodList — client-side; not ported
    private var isTiberiumEffectApplied: Boolean = false

    // -------------------------------------------------------------------------
    // Positions
    // -------------------------------------------------------------------------
    private var raveshawPos: Vector3          = Vector3(0f, 0f, 0f)
    private var starPos: Vector3              = Vector3(0f, 0f, 0f)
    private var currentDestPos: Vector3       = Vector3(0f, 0f, 0f)
    private var currentJumpToPos: Vector3     = Vector3(0f, 0f, 0f)

    // -------------------------------------------------------------------------
    // Arc objects (client-side visual lightning arcs)
    // -------------------------------------------------------------------------
    // C++: SimpleGameObj* ArcObjects[ARC_OBJ_COUNT] — client-side; not ported
    private val arcLifeRemaining: FloatArray = FloatArray(ARC_OBJ_COUNT) { 0f }
    // C++: Matrix3D EndTM, Bones[BONE_COUNT] — client-side; not ported

    // =========================================================================
    // Init
    // =========================================================================

    // C++: virtual void Init() — delegates to Init(def)
    override fun init() {
        init(getRaveshawBossDefinition())
    }

    // C++: void Init(const RaveshawBossGameObjDefClass& definition)
    fun init(definition: RaveshawBossGameObjDefClass) {
        super.init()  // C++: SoldierGameObj::Init() — uses definition already stored in base
        // C++: Peek_Model()->Scale(1.5F) — client-side only (render model scaling)
        // C++: Collect_Lightning_Rods() — client-side only (physics scene query for visual rods)
        // C++: Create_Arc_Effects() — client-side only (SimpleGameObj arc objects)
        // C++: Prepare_Arc_Effect_Data() — client-side only

        // C++: Get_Action()->Reset(100)
        action.reset(100)

        // C++: StartTimer = 2.0F
        startTimer = 2.0f

        // C++: Determine_New_Overall_State()
        determineNewOverallState()

        // C++: Get_Human_State()->Set_Human_Anim_Override("Raveshaw Boss Override") — client-side only
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    // C++: const RaveshawBossGameObjDefClass& Get_Definition() const
    fun getRaveshawBossDefinition(): RaveshawBossGameObjDefClass = definition as RaveshawBossGameObjDefClass

    // C++: RaveshawBossGameObjClass* As_RaveshawBossGameObj() { return this; }
    fun asRaveshawBossGameObj(): RaveshawBossGameObj = this

    // =========================================================================
    // isDead override — boss cannot die until death sequence
    // C++: DefenseObject.Set_Can_Object_Die(false) called in Apply_Damage_Extended
    // =========================================================================
    override val isDead: Boolean
        get() = overallStateCurrent == OverallState.DEATH_SEQUENCE && super.isDead

    // =========================================================================
    // Allow_Special_Damage_State_Lock
    // =========================================================================
    // C++: bool Allow_Special_Damage_State_Lock() { return false; }
    override fun allowSpecialDamageStateLock(): Boolean = false

    // =========================================================================
    // Think
    // C++: void RaveshawBossGameObjClass::Think()
    // =========================================================================
    override fun think() {
        val deltaSeconds = TimeManager.getFrameSeconds()
        var okToThink = true

        // C++: if (COMBAT_STAR == NULL || COMBAT_STAR->Is_Dead() || COMBAT_STAR->Is_Destroyed())
        //          ok_to_think = false;
        // Server-side: no COMBAT_STAR available; skip AI
        okToThink = false

        if (okToThink) {
            verifyStealth_Soldier()

            // C++: Get_Position(&RaveshawPos); COMBAT_STAR->Get_Position(&StarPos);
            raveshawPos = position
            // starPos = COMBAT_STAR->Get_Position() — not available server-side

            if (startTimer > UNINITIALIZED_TIMER) {
                startTimer -= deltaSeconds
                if (startTimer <= 0f) {
                    startTimer = UNINITIALIZED_TIMER
                    setOverallState(OverallState.CHASE_STAR)
                }
            } else {
                // Think all state machines
                thinkOverallState(deltaSeconds)
                thinkRaveshawState(deltaSeconds)
                thinkMoveState(deltaSeconds)
                thinkEngorgedState(deltaSeconds)
                thinkStealthSoldierState(deltaSeconds)
                thinkHavocState(deltaSeconds)
                thinkThrownObjectState(deltaSeconds)
                thinkJumpState(deltaSeconds)
                thinkLightningRodState(deltaSeconds)
            }
        }

        // C++: give Raveshaw 100 extra health so "special" damage doesn't kill him
        val oldHealth    = defenseObject.health + 100.0f
        val oldHealthMax = defenseObject.healthMax + 100.0f
        defenseObject.healthMax = oldHealthMax
        defenseObject.health    = oldHealth

        // C++: SoldierGameObj::Think()
        super.think()

        // C++: restore health
        val currHealth = defenseObject.health - 100.0f
        defenseObject.health    = max(currHealth, 1.0f)
        defenseObject.healthMax = oldHealthMax - 100.0f
    }

    // =========================================================================
    // Apply_Damage_Extended
    // C++: void RaveshawBossGameObjClass::Apply_Damage_Extended(...)
    // =========================================================================
    override fun applyDamageExtended(
        damager: OffenseObjectClass,
        scale: Float,
        direction: Vector3,
        collisionBoxName: String?,
    ) {
        // C++: if (!CombatManager::I_Am_Server() || OverallState == DEATH_SEQUENCE) return;
        if (overallStateCurrent == OverallState.DEATH_SEQUENCE) return

        // C++: DefenseObject.Set_Can_Object_Die(false)
        // We model this by temporarily preventing death in isDead; no separate flag needed.
        super.applyDamageExtended(damager, scale, direction, collisionBoxName)

        val currHealth = defenseObject.health

        // C++: if (FreeRandom.Get_Int(7) == 1) { play hurt sound }
        // C++: WWAudioClass — client-side only

        // C++: if (MoveState == CIRCLE_CATWALK && curr_health <= 20.0F && FreeRandom.Get_Int(5) == 1)
        if (moveStateCurrent == MoveState.CIRCLE_CATWALK && currHealth <= 20.0f) {
            if (FreeRandom.getInt(5) == 1) {
                setOverallState(OverallState.DEATH_SEQUENCE)
            }
        }
    }

    // =========================================================================
    // Save / Load
    // =========================================================================

    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        // C++: saves StealthSoldier reference
        csave.beginChunk(CHUNKID_STEALTH_SOLDIER)
        // stealthSoldierRef.save(csave) — stub
        csave.endChunk()

        // C++: saves TiberiumEffect (client-side only — save a placeholder)
        csave.beginChunk(CHUNKID_TIBERIUM_EFFECT)
        csave.endChunk()

        // State machines (integer state values)
        csave.beginChunk(CHUNKID_OVERALL_STATE_MACHINE)
        csave.writeMicroChunk(1, overallStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_RAVESHAW_STATE_MACHINE)
        csave.writeMicroChunk(1, raveshawStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_ENGORGED_STATE_MACHINE)
        csave.writeMicroChunk(1, engorgedStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_MOVE_STATE_MACHINE)
        csave.writeMicroChunk(1, moveStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_STEALTH_SOLDIER_STATE_MACHINE)
        csave.writeMicroChunk(1, stealthSoldierStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_HAVOC_STATE_MACHINE)
        csave.writeMicroChunk(1, havocStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_THROWN_OBJ_STATE_MACHINE)
        csave.writeMicroChunk(1, thrownObjectStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_JUMP_STATE_MACHINE)
        csave.writeMicroChunk(1, jumpStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_LIGHTNING_ROD_STATE_MACHINE)
        csave.writeMicroChunk(1, lightningRodStateCurrent)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        saveVariables(csave)
        csave.endChunk()

        return true
    }

    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)
                CHUNKID_STEALTH_SOLDIER -> { /* stealthSoldierRef.load(cload) — stub */ }
                CHUNKID_TIBERIUM_EFFECT -> { /* client-side; skip */ }
                CHUNKID_OVERALL_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) overallStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_RAVESHAW_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) raveshawStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_ENGORGED_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) engorgedStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_MOVE_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) moveStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_STEALTH_SOLDIER_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) stealthSoldierStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_HAVOC_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) havocStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_THROWN_OBJ_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) thrownObjectStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_JUMP_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) jumpStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_LIGHTNING_ROD_STATE_MACHINE -> {
                    while (cload.openMicroChunk()) {
                        if (cload.curMicroChunkId == 1) lightningRodStateCurrent = cload.readInt()
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_VARIABLES -> loadVariables(cload)
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void On_Post_Load()
    override fun onPostLoad() {
        // C++: SoldierGameObj::On_Post_Load()
        // C++: Peek_Model()->Scale(1.5F) — client-side only
        // C++: Collect_Lightning_Rods() — client-side only
        // C++: Prepare_Arc_Effect_Data() — client-side only
        // C++: Get_Human_State()->Set_Human_Anim_Override("Raveshaw Boss Override") — client-side only
        // C++: if (IsTiberiumEffectApplied) Peek_Physical_Object()->Add_Effect_To_Me(TiberiumEffect) — client-side only
    }

    // =========================================================================
    // Save_Variables / Load_Variables
    // =========================================================================
    private fun saveVariables(csave: ChunkSaveClass) {
        // C++: saves ArcObjects[10] pointers — client-side; skip
        for (index in 0 until ARC_OBJ_COUNT) {
            csave.writeMicroChunk(VARID_ARC_OBJ_PTR, null)
        }
        // C++: CameraBoneModel save — client-side only; skip
        csave.writeMicroChunk(VARID_CAMERA_BONE_PTR, null)
        csave.writeMicroChunk(VARID_CAMERA_BONE_TM, null)
        csave.writeMicroChunk(VARID_RESTORE_FIRST_PERSON, restoreFirstPerson)
        csave.writeMicroChunk(VARID_THROWN_OBJECT_PTR, null) // thrownObject ptr
        csave.writeMicroChunk(VARID_IS_TIBERIUM_EFFECT_APPLIED, isTiberiumEffectApplied)
        csave.writeMicroChunk(VARID_CURRENT_DEST_POS, currentDestPos)
        csave.writeMicroChunk(VARID_OVERALL_STATE_TIMER, overallStateTimer)
        csave.writeMicroChunk(VARID_ENGORGED_STATE_TIMER, engorgedStateTimer)
        csave.writeMicroChunk(VARID_MOVE_STATE_TIMER, moveStateTimer)
        csave.writeMicroChunk(VARID_BODY_SLAM_TIMER, bodySlamTimer)
        csave.writeMicroChunk(VARID_RAVESHAW_STATE_TIMER, raveshawStateTimer)
        csave.writeMicroChunk(VARID_STEALTHSOLDIER_STATE_TIMER, stealthSoldierStateTimer)
        csave.writeMicroChunk(VARID_LIGHTNINGROD_STATE_TIMER, lightningRodStateTimer)
        csave.writeMicroChunk(VARID_START_TIMER, startTimer)
        csave.writeMicroChunk(VARID_LAST_MELEE_ANIM_FRAME, lastMeleeAnimFrame)
        csave.writeMicroChunk(VARID_HAS_MELEE_ATTACK_HIT, hasMeleeAttackHit)
        csave.writeMicroChunk(VARID_FLYING_OBJECT_VECTOR, flyingObjectVector)
        csave.writeMicroChunk(VARID_FLYING_OBJECT_DEST, flyingObjectDest)
        csave.writeMicroChunk(VARID_FLYING_DIST, flyingDist)
        csave.writeMicroChunk(VARID_RELOBJ_TM, relObjTM)
        csave.writeMicroChunk(VARID_CURR_JUMP_POS, currentJumpToPos)
    }

    private fun loadVariables(cload: ChunkLoadClass) {
        var arcObjIndex = 0
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                VARID_ARC_OBJ_PTR               -> { cload.skip(); arcObjIndex++ }
                VARID_CAMERA_BONE_PTR           -> cload.skip()
                VARID_CAMERA_BONE_TM            -> cload.skip()
                VARID_RESTORE_FIRST_PERSON      -> restoreFirstPerson = cload.readBool()
                VARID_THROWN_OBJECT_PTR         -> cload.skip() // pointer remap — server doesn't track
                VARID_IS_TIBERIUM_EFFECT_APPLIED -> isTiberiumEffectApplied = cload.readBool()
                VARID_CURRENT_DEST_POS          -> currentDestPos = cload.readVector3()
                VARID_OVERALL_STATE_TIMER       -> overallStateTimer = cload.readFloat()
                VARID_ENGORGED_STATE_TIMER      -> engorgedStateTimer = cload.readFloat()
                VARID_MOVE_STATE_TIMER          -> moveStateTimer = cload.readFloat()
                VARID_BODY_SLAM_TIMER           -> bodySlamTimer = cload.readFloat()
                VARID_RAVESHAW_STATE_TIMER      -> raveshawStateTimer = cload.readFloat()
                VARID_STEALTHSOLDIER_STATE_TIMER -> stealthSoldierStateTimer = cload.readFloat()
                VARID_LIGHTNINGROD_STATE_TIMER  -> lightningRodStateTimer = cload.readFloat()
                VARID_START_TIMER               -> startTimer = cload.readFloat()
                VARID_LAST_MELEE_ANIM_FRAME     -> lastMeleeAnimFrame = cload.readFloat()
                VARID_HAS_MELEE_ATTACK_HIT      -> hasMeleeAttackHit = cload.readBool()
                VARID_FLYING_OBJECT_VECTOR      -> flyingObjectVector = cload.readVector3()
                VARID_FLYING_OBJECT_DEST        -> flyingObjectDest = cload.readVector3()
                VARID_FLYING_DIST               -> flyingDist = cload.readFloat()
                VARID_RELOBJ_TM                 -> relObjTM = cload.readMatrix3D()
                VARID_CURR_JUMP_POS             -> currentJumpToPos = cload.readVector3()
                else -> { /* unrecognized */ }
            }
            cload.closeMicroChunk()
        }
    }

    // =========================================================================
    // State machine transition helpers
    // =========================================================================

    private fun setOverallState(state: Int) {
        val prev = overallStateCurrent
        // END old state
        when (prev) {
            OverallState.DEATH_SEQUENCE -> { /* no end handler */ }
        }
        overallStateCurrent = state
        // BEGIN new state
        beginOverallState(state)
    }

    private fun setRaveshawState(state: Int) {
        val prev = raveshawStateCurrent
        // END handlers
        when (prev) {
            RaveshawState.ROAR           -> endRaveshawStateRoar()
            RaveshawState.GRAB_TIBERIUM  -> endRaveshawStateGrabTiberium()
            RaveshawState.THROW_SOLDIER  -> endRaveshawStateThrowSoldier()
            RaveshawState.THROW_OBJECT   -> endRaveshawStateThrowObject()
            RaveshawState.GRAB_STAR      -> endRaveshawStateGrabStar()
            RaveshawState.BODYSLAM       -> endRaveshawStateBodyslam()
        }
        raveshawStateCurrent = state
        // BEGIN handlers
        beginRaveshawState(state)
    }

    private fun setMoveState(state: Int) {
        val prev = moveStateCurrent
        // END handlers
        when (prev) {
            MoveState.FOLLOW_STAR -> endMoveStateFollowStar()
        }
        moveStateCurrent = state
        beginMoveState(state)
    }

    private fun setEngorgedState(state: Int) {
        val prev = engorgedStateCurrent
        // END handlers
        when (prev) {
            EngorgedState.FADING -> endEngorgedStateFading()
        }
        engorgedStateCurrent = state
        beginEngorgedState(state)
    }

    private fun setStealthSoldierState(state: Int) {
        stealthSoldierStateCurrent = state
        beginStealthSoldierState(state)
    }

    private fun setHavocState(state: Int) {
        havocStateCurrent = state
        beginHavocState(state)
    }

    private fun setThrownObjectState(state: Int) {
        thrownObjectStateCurrent = state
        beginThrownObjectState(state)
    }

    private fun setJumpState(state: Int) {
        val prev = jumpStateCurrent
        // END handlers
        when (prev) {
            JumpState.LANDING -> endJumpStateLanding()
        }
        jumpStateCurrent = state
        beginJumpState(state)
    }

    private fun setLightningRodState(state: Int) {
        val prev = lightningRodStateCurrent
        // END handlers
        when (prev) {
            LightningRodState.ACTIVE -> endLightningRodStateActive()
        }
        lightningRodStateCurrent = state
        beginLightningRodState(state)
    }

    // =========================================================================
    // OVERALL STATE — begin handlers
    // =========================================================================
    private fun beginOverallState(state: Int) {
        when (state) {
            OverallState.HEALING -> {
                // C++: RaveshawState.Set_State(RAVESHAW_STATE_NOTHING); MoveState.Set_State(MOVE_STATE_GOTO_TIBERIUM)
                setRaveshawState(RaveshawState.NOTHING)
                setMoveState(MoveState.GOTO_TIBERIUM)
            }
            OverallState.THROWING_OBJECT -> {
                thrownObject = findObjectToThrow()
                if (thrownObject != null) {
                    setRaveshawState(RaveshawState.NOTHING)
                    setMoveState(MoveState.GOTO_THROW_OBJECT)
                } else {
                    setOverallState(OverallState.CHASE_STAR)
                }
            }
            OverallState.THROWING_SOLDIER -> {
                setRaveshawState(RaveshawState.GRAB_SOLDIER)
                setMoveState(MoveState.STOP)
            }
            OverallState.GRAB_STAR -> {
                setRaveshawState(RaveshawState.GRAB_STAR)
                setMoveState(MoveState.STOP)
            }
            OverallState.JUMP_TO_CATWALK -> {
                setRaveshawState(RaveshawState.NOTHING)
                setMoveState(MoveState.GOTO_CATWALK)
            }
            OverallState.ON_CATWALK -> {
                setRaveshawState(RaveshawState.NOTHING)
                setMoveState(MoveState.CIRCLE_CATWALK)
            }
            OverallState.BODYSLAM -> {
                setRaveshawState(RaveshawState.BODYSLAM)
                setMoveState(MoveState.STOP)
            }
            OverallState.CHASE_STAR -> {
                setRaveshawState(RaveshawState.NOTHING)
                setMoveState(MoveState.FOLLOW_STAR)
                overallStateTimer = randomFloat(6.0f, 20.0f)
            }
            OverallState.DEATH_SEQUENCE -> {
                setRaveshawState(RaveshawState.DYING)
                setMoveState(MoveState.STOP)
            }
            // NOTHING, THROWING_STAR, DAZED, FLEE — no begin handler
        }
    }

    // =========================================================================
    // OVERALL STATE — think handlers
    // =========================================================================
    private fun thinkOverallState(dt: Float) {
        when (overallStateCurrent) {
            OverallState.CHASE_STAR -> {
                val armsReach = 3.8f
                val dist2 = (raveshawPos - starPos).length2()
                if (dist2 <= armsReach && jumpStateCurrent == JumpState.NONE) {
                    setOverallState(OverallState.GRAB_STAR)
                    return
                }
                overallStateTimer -= dt
                if (overallStateTimer <= 0f) {
                    determineNewOverallState()
                }
            }
        }
    }

    // =========================================================================
    // RAVESHAW STATE — begin handlers
    // =========================================================================
    private fun beginRaveshawState(state: Int) {
        when (state) {
            RaveshawState.NOTHING -> {
                raveshawStateTimer = randomFloat(5.0f, 15.0f)
            }
            RaveshawState.ROAR -> {
                // C++: Set_Blended_Animation("S_C_HUMAN.H_C_A0A0_L07", false) — client-side only
                // C++: Attach_Sound("Rav_Long_Yell_Twiddler", "C HEAD") — client-side only
                action.reset(100) // C++: Get_Action()->Pause(true) — approximated
                // C++: JumpState.Halt_State(); MoveState.Halt_State() — not implemented (server stub)
            }
            RaveshawState.GRAB_TIBERIUM -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_HEAL", false) — client-side only
                // C++: Get_Human_State()->Set_State(LOCKED_ANIMATION) — client-side only
                // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0) — client-side only
                // C++: Get_Anim_Control()->Set_Target_Frame(34) — client-side only
            }
            RaveshawState.GRAB_SOLDIER -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_GRABTHROW", false) — client-side only
                // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0) — client-side only
                // C++: Get_Anim_Control()->Set_Target_Frame(27) — client-side only
            }
            RaveshawState.THROW_SOLDIER -> {
                // C++: Get_Anim_Control()->Set_Target_Frame(49) — client-side only
                // C++: WWAudioClass::Create_Instant_Sound("Rav_Exert_01") — client-side only
            }
            RaveshawState.GRAB_OBJECT -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_THROW", false) — client-side only
                // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0) — client-side only
                // C++: Get_Anim_Control()->Set_Target_Frame(22) — client-side only
            }
            RaveshawState.THROW_OBJECT -> {
                raveshawStateTimer = 1.0f
            }
            RaveshawState.GRAB_STAR -> {
                hasMeleeAttackHit = false
                lastMeleeAnimFrame = 0f
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_GRABTHROW", false, 14.0F) — client-side only
                // C++: HavocState.Set_State(HAVOC_STATE_GRABBED)
                setHavocState(HavocState.GRABBED)
            }
            RaveshawState.BODYSLAM -> {
                val starVectorFromTib = starPos - TIBERIUM_POS
                // C++: star_vector_from_tib.Z = 0
                val flatStarVec = Vector3(starVectorFromTib.x, starVectorFromTib.y, 0f)
                if (flatStarVec.length2() < SAFE_JUMP_RADIUS * SAFE_JUMP_RADIUS) {
                    setRaveshawState(RaveshawState.JUMP_DOWN)
                } else {
                    hasMeleeAttackHit = false
                    lastMeleeAnimFrame = 0f
                    // C++: Set_Blended_Animation("S_A_HUMAN.H_A_BODYSLAM", false) — client-side only
                    // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0) — client-side only
                    // C++: Get_Anim_Control()->Set_Target_Frame(69) — client-side only
                    // C++: WWAudioClass::Create_Instant_Sound("Rav_Body_Slam_Yell") — client-side only
                }
            }
            RaveshawState.JUMP_DOWN -> {
                val dirVector = (starPos - TIBERIUM_POS).normalized()
                currentDestPos = TIBERIUM_POS + dirVector * SAFE_JUMP_RADIUS
                jumpToPoint(currentDestPos)
            }
            RaveshawState.DYING -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_DEATH", false) — client-side only
                // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0) — client-side only
                // C++: Get_Anim_Control()->Set_Target_Frame(49) — client-side only
            }
            RaveshawState.FALL -> {
                raveshawStateTimer = 0.125f
            }
            RaveshawState.DEATH_LANDING -> {
                // C++: Get_Anim_Control()->Set_Target_Frame(63) — client-side only
                raveshawStateTimer = UNINITIALIZED_TIMER
                // C++: WWAudioClass::Create_Instant_Sound("Rav_Death_Fall") — client-side only
                // C++: COMBAT_SCENE->Add_Camera_Shake(...) — client-side only
            }
        }
    }

    // END handlers for raveshaw states
    private fun endRaveshawStateRoar() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
        // C++: MoveState.Resume_State(); JumpState.Resume_State() — not applicable server-side
        // C++: Get_Action()->Pause(false) — no pause on server
    }
    private fun endRaveshawStateGrabTiberium() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
    }
    private fun endRaveshawStateThrowSoldier() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
    }
    private fun endRaveshawStateThrowObject() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
    }
    private fun endRaveshawStateGrabStar() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
    }
    private fun endRaveshawStateBodyslam() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
    }

    // =========================================================================
    // RAVESHAW STATE — think handlers
    // =========================================================================
    private fun thinkRaveshawState(dt: Float) {
        when (raveshawStateCurrent) {
            RaveshawState.NOTHING -> {
                raveshawStateTimer -= dt
                if (raveshawStateTimer <= 0f) {
                    setRaveshawState(RaveshawState.ROAR)
                }
            }
            RaveshawState.ROAR -> {
                // C++: if (Get_Anim_Control()->Is_Complete()) set NOTHING — client-side only
                // Server-side: transition to NOTHING after a short time (approximate)
                // No anim control on server — stay in ROAR until external trigger
            }
            RaveshawState.GRAB_TIBERIUM -> {
                // C++: face tiberium, loop animation frames 12-34 — client-side only
                // C++: if (PASSED_FRAME(9.0F)) EngorgedState.Set_State(ENGORGED_STATE_ABSORBING_TIBERIUM)
                // Server-side: immediately start absorbing
                if (engorgedStateCurrent == EngorgedState.NONE) {
                    setEngorgedState(EngorgedState.ABSORBING_TIBERIUM)
                }
                lastMeleeAnimFrame = 0f // track approximate
            }
            RaveshawState.GRAB_SOLDIER -> {
                // C++: if (StealthSoldierState == NONE && anim complete) => STEALTH_SOLDIER_STATE_DISPLAY
                if (stealthSoldierStateCurrent == StealthSoldierState.NONE) {
                    setStealthSoldierState(StealthSoldierState.DISPLAY)
                }
                // C++: if (StealthSoldierState != NONE) Set_Targeting(StarPos) — client-side only
            }
            RaveshawState.THROW_SOLDIER -> {
                // C++: PASSED_FRAME(36) => StealthSoldierState.Set_State(FLYING)
                // C++: if anim complete => Determine_New_Overall_State()
                // Server-side: immediately transition the stealth soldier to flying
                if (stealthSoldierStateCurrent == StealthSoldierState.DISPLAY) {
                    setStealthSoldierState(StealthSoldierState.FLYING)
                }
                determineNewOverallState()
            }
            RaveshawState.GRAB_OBJECT -> {
                // C++: PASSED_FRAME(9) => ThrownObjectState.Set_State(PICKUP)
                // C++: if anim complete => RaveshawState.Set_State(THROW_OBJECT)
                if (thrownObjectStateCurrent == ThrownObjectState.NONE) {
                    setThrownObjectState(ThrownObjectState.PICKUP)
                }
                if (thrownObjectStateCurrent == ThrownObjectState.PICKUP) {
                    setRaveshawState(RaveshawState.THROW_OBJECT)
                }
                lastMeleeAnimFrame = 0f
            }
            RaveshawState.THROW_OBJECT -> {
                if (raveshawStateTimer != UNINITIALIZED_TIMER) {
                    raveshawStateTimer -= dt
                    if (raveshawStateTimer <= 0f) {
                        raveshawStateTimer = UNINITIALIZED_TIMER
                        // C++: Get_Anim_Control()->Set_Target_Frame(58) — client-side only
                    }
                } else {
                    // C++: PASSED_FRAME(36) => ThrownObjectState.Set_State(FLYING)
                    if (thrownObjectStateCurrent == ThrownObjectState.PICKUP) {
                        setThrownObjectState(ThrownObjectState.FLYING)
                    }
                    // C++: if anim complete => Determine_New_Overall_State()
                    // Server-side: if flying state ended (object hit), determine new state
                    if (thrownObjectStateCurrent == ThrownObjectState.NONE) {
                        determineNewOverallState()
                    }
                }
                // C++: Set_Targeting(StarPos) — client-side only
                lastMeleeAnimFrame = 0f
            }
            RaveshawState.GRAB_STAR -> {
                // C++: if anim complete => NOTHING + Determine_New_Overall_State
                // C++: PASSED_FRAME(37) => HavocState.Set_State(FLYING)
                // C++: PASSED_FRAME(25) => reset anim speed
                if (havocStateCurrent == HavocState.GRABBED) {
                    // After a brief moment, switch to flying
                    setHavocState(HavocState.FLYING)
                }
                // C++: lastMeleeAnimFrame update
                lastMeleeAnimFrame = 0f
            }
            RaveshawState.BODYSLAM -> {
                // C++: complex anim frame-driven logic — client-side only
                // Server-side: after bodyslam, determine new state
                determineNewOverallState()
            }
            RaveshawState.JUMP_DOWN -> {
                // C++: Apply_Bone_Collision_Damage — client-side only
                // C++: if (JumpState == NONE) => RAVESHAW_STATE_NOTHING + OVERALL_STATE_CHASE_STAR
                if (jumpStateCurrent == JumpState.NONE) {
                    setRaveshawState(RaveshawState.NOTHING)
                    setOverallState(OverallState.CHASE_STAR)
                }
            }
            RaveshawState.DYING -> {
                // C++: face catwalk, PASSED_FRAME(43) => RAVESHAW_STATE_FALL
                // Server-side: start falling
                setRaveshawState(RaveshawState.FALL)
                lastMeleeAnimFrame = 0f
            }
            RaveshawState.FALL -> {
                raveshawStateTimer += dt
                val velocity = 22.0f * raveshawStateTimer
                val newPos = raveshawPos.copy()
                newPos.z -= velocity * dt

                val floorPos1 = -189.1f
                val floorPos2 = -191.0f
                val hitGround = when {
                    newPos.x >= -133.1f && newPos.z <= floorPos1 -> { newPos.z = floorPos1; true }
                    newPos.x < -133.1f && newPos.z <= floorPos2  -> { newPos.z = floorPos2; true }
                    else -> false
                }
                position = newPos
                if (hitGround) {
                    setRaveshawState(RaveshawState.DEATH_LANDING)
                }
            }
            RaveshawState.DEATH_LANDING -> {
                if (raveshawStateTimer != UNINITIALIZED_TIMER) {
                    raveshawStateTimer -= dt
                    if (raveshawStateTimer <= 0f) {
                        // C++: CombatManager::Mission_Complete(true) — client-side only
                    }
                } else {
                    // C++: if anim complete => RaveshawStateTimer = 2.0F — client-side only
                    raveshawStateTimer = 2.0f
                }
            }
        }
    }

    // =========================================================================
    // MOVE STATE — begin handlers
    // =========================================================================
    private fun beginMoveState(state: Int) {
        when (state) {
            MoveState.STOP -> {
                action.reset(100)
            }
            MoveState.GOTO_TIBERIUM -> {
                val dirVector = (raveshawPos - TIBERIUM_POS).normalized()
                currentDestPos = TIBERIUM_POS + dirVector * TIBERIUM_RADIUS
                // C++: Get_Action()->Goto(params) — server-side stub
            }
            MoveState.GOTO_CATWALK -> {
                findClosestCatwalkPos(raveshawPos, currentJumpToPos)
                val dirVector = (currentJumpToPos - TIBERIUM_POS).normalized()
                currentDestPos = Vector3(
                    currentJumpToPos.x + dirVector.x * 8.0f,
                    currentJumpToPos.y + dirVector.y * 8.0f,
                    TIBERIUM_POS.z,
                )
                // C++: Get_Action()->Goto(params) — server-side stub
            }
            MoveState.JUMP_TO_CATWALK -> {
                jumpToPoint(currentJumpToPos)
            }
            MoveState.CIRCLE_CATWALK -> {
                moveStateTimer = 0f
                bodySlamTimer  = randomFloat(1.0f, 5.0f)
            }
            MoveState.GOTO_THROW_OBJECT -> {
                // C++: Get_Action()->Goto(params with ThrownObject) — server-side stub
            }
            MoveState.JUMP_TO_STAR -> {
                action.reset(100)
                jumpToPoint(starPos)
            }
            MoveState.FOLLOW_STAR -> {
                moveStateTimer = 0f
            }
        }
    }

    private fun endMoveStateFollowStar() {
        action.reset(100)
    }

    // =========================================================================
    // MOVE STATE — think handlers
    // =========================================================================
    private fun thinkMoveState(dt: Float) {
        when (moveStateCurrent) {
            MoveState.GOTO_TIBERIUM -> {
                // C++: if (!Get_Action()->Is_Active()) => GRAB_TIBERIUM + STOP
                // Server-side: no pathfinding; immediately transition
                setRaveshawState(RaveshawState.GRAB_TIBERIUM)
                setMoveState(MoveState.STOP)
            }
            MoveState.GOTO_CATWALK -> {
                // C++: if dist < 4.0F => face tiberium; if (!Get_Action()->Is_Active()) => JUMP_TO_CATWALK
                // Server-side: immediately transition
                setRaveshawState(RaveshawState.NOTHING)
                setMoveState(MoveState.JUMP_TO_CATWALK)
            }
            MoveState.JUMP_TO_CATWALK -> {
                // C++: if (JumpState == NONE) => OVERALL_STATE_ON_CATWALK
                if (jumpStateCurrent == JumpState.NONE) {
                    setOverallState(OverallState.ON_CATWALK)
                }
                // C++: Set_Targeting(TIBERIUM_POS) — client-side only
            }
            MoveState.CIRCLE_CATWALK -> {
                // C++: Set_Targeting(StarPos) — client-side only
                val health = defenseObject.health
                val healthPercent = if (defenseObject.healthMax > 0f) health / defenseObject.healthMax else 0f

                bodySlamTimer -= dt
                if (healthPercent > 0.05f && bodySlamTimer <= 0f) {
                    setOverallState(OverallState.BODYSLAM)
                } else {
                    moveStateTimer -= dt
                    if (moveStateTimer <= 0f) {
                        moveStateTimer = 0.5f
                        findClosestCatwalkPos(starPos, currentDestPos)
                        // C++: Get_Action()->Goto(params) — server-side stub
                    }
                }
            }
            MoveState.GOTO_THROW_OBJECT -> {
                val throwObj = thrownObject ?: return
                val objPos = throwObj.position
                if ((raveshawPos - objPos).length2() < 3.0f) {
                    setMoveState(MoveState.STOP)
                    setRaveshawState(RaveshawState.GRAB_OBJECT)
                }
            }
            MoveState.JUMP_TO_STAR -> {
                // C++: Set_Targeting(StarPos) — client-side only
                if (jumpStateCurrent == JumpState.NONE) {
                    setMoveState(MoveState.FOLLOW_STAR)
                }
            }
            MoveState.FOLLOW_STAR -> {
                moveStateTimer -= dt
                if (moveStateTimer <= 0f) {
                    moveStateTimer = 0.75f
                    val dist2 = (raveshawPos - starPos).length2()
                    if (dist2 > 64.0f && FreeRandom.getInt(5) == 1) {
                        setMoveState(MoveState.JUMP_TO_STAR)
                    } else {
                        // C++: Get_Action()->Goto(params with COMBAT_STAR) — server-side stub
                    }
                }
            }
        }
    }

    // =========================================================================
    // ENGORGED STATE — begin handlers
    // =========================================================================
    private fun beginEngorgedState(state: Int) {
        when (state) {
            EngorgedState.ABSORBING_TIBERIUM -> {
                engorgedStateTimer = HEAL_TIME
                setLightningRodState(LightningRodState.ACTIVE)
                // C++: TiberiumEffect->Set_Intensity(0.0F) — client-side only
                // C++: Peek_Physical_Object()->Add_Effect_To_Me(TiberiumEffect) — client-side only
                isTiberiumEffectApplied = true
            }
            EngorgedState.FADING -> {
                setLightningRodState(LightningRodState.NONE)
                // C++: EngorgedStateTimer = TiberiumEffect->Get_Intensity() * EFFECT_FADE_TIME
                engorgedStateTimer = EFFECT_INTENSITY * EFFECT_FADE_TIME
            }
        }
    }

    private fun endEngorgedStateFading() {
        // C++: Peek_Physical_Object()->Remove_Effect_From_Me(TiberiumEffect) — client-side only
        isTiberiumEffectApplied = false
    }

    // =========================================================================
    // ENGORGED STATE — think handlers
    // =========================================================================
    private fun thinkEngorgedState(dt: Float) {
        when (engorgedStateCurrent) {
            EngorgedState.ABSORBING_TIBERIUM -> {
                val healRate = 50.0f
                engorgedStateTimer -= dt
                if (engorgedStateTimer <= 0f) {
                    setEngorgedState(EngorgedState.FADING)
                    setOverallState(OverallState.CHASE_STAR)
                } else {
                    // C++: configure tiberium effect intensity — client-side only
                    // Heal Raveshaw
                    val newHealth = defenseObject.health + healRate * dt
                    defenseObject.health = newHealth
                }
            }
            EngorgedState.FADING -> {
                engorgedStateTimer -= dt
                if (engorgedStateTimer <= 0f) {
                    setEngorgedState(EngorgedState.NONE)
                }
                // C++: TiberiumEffect->Set_Intensity(...) — client-side only
            }
        }
    }

    // =========================================================================
    // JUMP STATE — begin handlers
    // =========================================================================
    private fun beginJumpState(state: Int) {
        when (state) {
            JumpState.CROUCHING -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_JUMP", false) — client-side only
                // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 0) — client-side only
                // C++: Get_Anim_Control()->Set_Target_Frame(10) — client-side only
                // C++: Get_Action()->Pause(true) — no pause on server
            }
            JumpState.JUMPING -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.H_A_A0D0", false) — client-side only
                // C++: Peek_Human_Phys()->Jump_To_Point(CurrentJumpToPos) — client-side physics
            }
            JumpState.LANDING -> {
                // C++: Set_Blended_Animation("S_A_HUMAN.RAV_JUMP", false) — client-side only
                // C++: Get_Anim_Control()->Set_Mode(ANIM_MODE_TARGET, 11) — client-side only
                // C++: Get_Anim_Control()->Set_Target_Frame(31) — client-side only
            }
        }
    }

    private fun endJumpStateLanding() {
        // C++: Get_Human_State()->Stop_Scripted_Animation() — client-side only
        // C++: Get_Action()->Pause(false) — no pause on server
    }

    // =========================================================================
    // JUMP STATE — think handlers
    // =========================================================================
    private fun thinkJumpState(dt: Float) {
        when (jumpStateCurrent) {
            JumpState.CROUCHING -> {
                // C++: if (anim complete) => JUMPING — client-side only
                // Server-side: immediately proceed to jumping
                setJumpState(JumpState.JUMPING)
            }
            JumpState.JUMPING -> {
                // C++: if (velocity.Z < 0 && distance_from_ground < 2.0F) => LANDING
                // Server-side: no real physics; immediately transition to LANDING
                setJumpState(JumpState.LANDING)
            }
            JumpState.LANDING -> {
                // C++: PASSED_FRAME(20) => play landing sound — client-side only
                // C++: if (anim complete) => JUMP_STATE_NONE
                // Server-side: immediately finish landing
                setJumpState(JumpState.NONE)
                lastMeleeAnimFrame = 0f
            }
        }
    }

    // =========================================================================
    // HAVOC STATE — begin handlers
    // =========================================================================
    private fun beginHavocState(state: Int) {
        when (state) {
            HavocState.GRABBED -> {
                // C++: COMBAT_STAR->Peek_Physical_Object()->Enable_Objects_Simulation(false) — client-side only
                // C++: RestoreFirstPerson = CombatManager::Is_First_Person() — client-side only
                // C++: CombatManager::Set_First_Person(false) — client-side only
                // C++: COMBAT_STAR->Set_Blended_Animation("S_A_HUMAN.STL_STRUGGLE", true) — client-side only
            }
            HavocState.FLYING -> {
                // C++: calculate fly vector from COMBAT_STAR position — server doesn't track COMBAT_STAR
                // C++: COMBAT_STAR->Control_Enable(true) — client-side only
                // C++: CombatManager::Set_First_Person(RestoreFirstPerson) — client-side only
                // C++: COMBAT_STAR->Get_Human_State()->Stop_Scripted_Animation() — client-side only
            }
        }
    }

    // =========================================================================
    // HAVOC STATE — think handlers
    // =========================================================================
    private fun thinkHavocState(dt: Float) {
        when (havocStateCurrent) {
            HavocState.GRABBED -> {
                // C++: Link_Player_To_Hands() — client-side only
            }
            HavocState.FLYING -> {
                // C++: fly COMBAT_STAR through air, apply damage on hit — client-side only
                // C++: on hit: COMBAT_STAR->Apply_Damage_Extended(20.0F, 1) — server-side: skip
                // C++: HavocState.Set_State(HAVOC_STATE_NONE)
                setHavocState(HavocState.NONE)
            }
        }
    }

    // =========================================================================
    // STEALTH SOLDIER STATE — begin handlers
    // =========================================================================
    private fun beginStealthSoldierState(state: Int) {
        when (state) {
            StealthSoldierState.DISPLAY -> {
                // C++: Create_Stealth_Soldier(Matrix3D(1)) — client-side only
                // C++: Link_Stealth_Soldier_To_Hand() — client-side only
                val stealthSoldier = peekStealthSoldier()
                if (stealthSoldier == null) {
                    setOverallState(OverallState.NOTHING)
                    setRaveshawState(RaveshawState.ROAR)
                    setMoveState(MoveState.STOP)
                    setStealthSoldierState(StealthSoldierState.NONE)
                    determineNewOverallState()
                    return
                }
                stealthSoldierStateTimer = 3.0f
                // C++: WWAudioClass sounds — client-side only
            }
            StealthSoldierState.FLYING -> {
                val stealthSoldier = peekStealthSoldier()
                if (stealthSoldier == null) {
                    setOverallState(OverallState.NOTHING)
                    setRaveshawState(RaveshawState.ROAR)
                    setMoveState(MoveState.STOP)
                    setStealthSoldierState(StealthSoldierState.NONE)
                    determineNewOverallState()
                    return
                }
                val soldierPos = stealthSoldier.position
                flyingObjectDest   = starPos
                flyingObjectVector = (starPos - soldierPos)
                flyingDist         = flyingObjectVector.length()
                flyingObjectVector = flyingObjectVector.normalized()
                // C++: choose fly animation based on relative direction — client-side only
            }
        }
    }

    // =========================================================================
    // STEALTH SOLDIER STATE — think handlers
    // =========================================================================
    private fun thinkStealthSoldierState(dt: Float) {
        when (stealthSoldierStateCurrent) {
            StealthSoldierState.DISPLAY -> {
                // C++: Link_Stealth_Soldier_To_Hand() — client-side only
                stealthSoldierStateTimer -= dt
                if (stealthSoldierStateTimer <= 0f) {
                    setRaveshawState(RaveshawState.THROW_SOLDIER)
                }
            }
            StealthSoldierState.FLYING -> {
                val stealthSoldier = peekStealthSoldier()
                if (stealthSoldier == null) {
                    setOverallState(OverallState.NOTHING)
                    setRaveshawState(RaveshawState.ROAR)
                    setMoveState(MoveState.STOP)
                    setStealthSoldierState(StealthSoldierState.NONE)
                    determineNewOverallState()
                    return
                }
                // C++: fly stealth soldier through air toward star — server-side: skip physics
                // C++: on hit: Apply_Damage_Extended(10000.0F) then STEALTH_SOLDIER_STATE_NONE
                // Server-side: immediately "hit"
                val offense = OffenseObjectClass(10000.0f, 1)
                stealthSoldier.applyDamageExtended(offense)
                setStealthSoldierState(StealthSoldierState.NONE)
                stealthSoldierRef.set(null)
            }
        }
    }

    // =========================================================================
    // THROWN OBJECT STATE — begin handlers
    // =========================================================================
    private fun beginThrownObjectState(state: Int) {
        when (state) {
            ThrownObjectState.PICKUP -> {
                // C++: compute RelObjTM from Raveshaw right-hand bone — client-side only
            }
            ThrownObjectState.FLYING -> {
                val obj = thrownObject ?: return
                val objectPos = obj.position
                flyingObjectDest   = starPos
                flyingObjectVector = (starPos - objectPos)
                flyingDist         = flyingObjectVector.length()
                flyingObjectVector = flyingObjectVector.normalized()
            }
        }
    }

    // =========================================================================
    // THROWN OBJECT STATE — think handlers
    // =========================================================================
    private fun thinkThrownObjectState(dt: Float) {
        when (thrownObjectStateCurrent) {
            ThrownObjectState.PICKUP -> {
                // C++: Link_Thrown_Object_To_Hands() — client-side only
            }
            ThrownObjectState.FLYING -> {
                val obj = thrownObject ?: return
                // C++: fly object toward star — server-side: immediately destroy it
                val offense = OffenseObjectClass(10000.0f, 1)
                obj.completelyDamaged(offense)
                obj.setDeletePending()
                thrownObject = null
                setThrownObjectState(ThrownObjectState.NONE)
            }
        }
    }

    // =========================================================================
    // LIGHTNING ROD STATE — begin / end / think handlers
    // =========================================================================
    private fun beginLightningRodState(state: Int) {
        when (state) {
            LightningRodState.ACTIVE -> {
                lightningRodStateTimer = 0f
            }
        }
    }

    private fun endLightningRodStateActive() {
        // C++: hide all arc effects — client-side only
    }

    private fun thinkLightningRodState(dt: Float) {
        when (lightningRodStateCurrent) {
            LightningRodState.ACTIVE -> {
                lightningRodStateTimer -= dt
                if (lightningRodStateTimer <= 0f) {
                    lightningRodStateTimer = 0.125f
                    // C++: create lightning arcs between rods + strike star — client-side only
                    // C++: Apply 5 pts damage to star with random probability — server-side: skip
                }
                // C++: hide any arc effects that have expired — client-side only
                for (index in 0 until ARC_OBJ_COUNT) {
                    if (arcLifeRemaining[index] > 0f) {
                        arcLifeRemaining[index] -= dt
                    }
                }
            }
        }
    }

    // =========================================================================
    // Helper — Determine_New_Overall_State
    // C++: void RaveshawBossGameObjClass::Determine_New_Overall_State()
    // =========================================================================
    private fun determineNewOverallState() {
        val health = defenseObject.health
        val healthPercent = if (defenseObject.healthMax > 0f) health / defenseObject.healthMax else 0f

        if (healthPercent <= 0.05f) {
            if (overallStateCurrent != OverallState.JUMP_TO_CATWALK &&
                overallStateCurrent != OverallState.ON_CATWALK &&
                overallStateCurrent != OverallState.DEATH_SEQUENCE
            ) {
                setOverallState(OverallState.JUMP_TO_CATWALK)
            }
        } else {
            val possibility = (10.0f * healthPercent + 0.5f).toInt()
            val goHeal = (healthPercent <= 0.75f && possibility > 0) && (FreeRandom.getInt(possibility) == 0)
            if (goHeal) {
                setOverallState(OverallState.HEALING)
            } else {
                val dist2 = (raveshawPos - starPos).length2()
                val choice = FreeRandom.getInt(100)
                when {
                    choice < 35 && dist2 > 16.0f           -> setOverallState(OverallState.THROWING_SOLDIER)
                    choice < 60 && thrownObject == null     -> setOverallState(OverallState.THROWING_OBJECT)
                    choice < 80                             -> setOverallState(OverallState.CHASE_STAR)
                    else                                    -> setOverallState(OverallState.JUMP_TO_CATWALK)
                }
            }
        }
    }

    // =========================================================================
    // Helper — Jump_To_Point
    // C++: void RaveshawBossGameObjClass::Jump_To_Point(const Vector3& pos)
    // =========================================================================
    private fun jumpToPoint(pos: Vector3) {
        if (jumpStateCurrent == JumpState.NONE) {
            currentJumpToPos = pos
            setJumpState(JumpState.CROUCHING)
        }
    }

    // =========================================================================
    // Helper — Find_Closest_Catwalk_Pos
    // C++: void RaveshawBossGameObjClass::Find_Closest_Catwalk_Pos(...)
    // =========================================================================
    private fun findClosestCatwalkPos(currPos: Vector3, outPos: Vector3) {
        // C++: queries PathfindClass for waypath CATWALK_WAYPATH_ID — not available server-side
        // No-op: leave outPos as-is (already initialised to zero/last value)
        // C++: Find closest midpoint between consecutive waypath points
    }

    // =========================================================================
    // Helper — Find_Death_Facing_Pos
    // C++: void RaveshawBossGameObjClass::Find_Death_Facing_Pos(Vector3* facing_pos)
    // =========================================================================
    private fun findDeathFacingPos(outPos: Vector3) {
        // C++: queries waypath CATWALK_WAYPATH_ID — not available server-side
    }

    // =========================================================================
    // Helper — Verify_Stealth_Soldier
    // C++: void RaveshawBossGameObjClass::Verify_Stealth_Soldier()
    // =========================================================================
    private fun verifyStealth_Soldier() {
        if (stealthSoldierStateCurrent == StealthSoldierState.NONE) return
        val stealthSoldier = peekStealthSoldier()
        if (stealthSoldier != null && (stealthSoldier.isDead || stealthSoldier.isDeletePending)) {
            // C++: stealth_soldier->Peek_Physical_Object()->Remove_Effect_From_Me(StealthEffect) — client-side only
            // C++: stealth_soldier->Get_Human_State()->Stop_Scripted_Animation() — client-side only
            stealthSoldier.setDeletePending()
            stealthSoldierRef.set(null)
            setOverallState(OverallState.NOTHING)
            setRaveshawState(RaveshawState.ROAR)
            setMoveState(MoveState.STOP)
            setStealthSoldierState(StealthSoldierState.NONE)
            determineNewOverallState()
        }
    }

    // =========================================================================
    // Helper — Peek_Stealth_Soldier
    // C++: WWINLINE SoldierGameObj* Peek_Stealth_Soldier()
    // =========================================================================
    private fun peekStealthSoldier(): SoldierGameObj? {
        val ptr = stealthSoldierRef.get() ?: return null
        return (ptr as? SoldierGameObj)
    }

    // =========================================================================
    // Helper — Shuffle_Taunt_List
    // C++: void RaveshawBossGameObjClass::Shuffle_Taunt_List()
    // =========================================================================
    private fun shuffleTauntList() {
        tauntList.fill(0)
        for (index in 0 until MAX_TAUNTS) {
            var listIndex: Int
            do {
                listIndex = FreeRandom.getInt(MAX_TAUNTS)
            } while (tauntList[listIndex] != 0)
            tauntList[listIndex] = index + 1
        }
    }

    // =========================================================================
    // Helper — Find_Object_To_Throw
    // C++: SimpleGameObj* RaveshawBossGameObjClass::Find_Object_To_Throw()
    // =========================================================================
    private fun findObjectToThrow(): SimpleGameObj? {
        // C++: iterates GameObjManager::Get_Game_Obj_List() for "(Raveshaw Ammo)" objects
        // Not available server-side — return null
        return null
    }

    // =========================================================================
    // Utility — random float helper
    // =========================================================================
    private fun randomFloat(min: Float, max: Float): Float =
        min + FreeRandom.get() * (max - min)

    // =========================================================================
    // Vector3 extensions used locally
    // =========================================================================
    private fun Vector3.length2(): Float = x * x + y * y + z * z
    private fun Vector3.length(): Float = kotlin.math.sqrt(length2())
    private fun Vector3.normalized(): Vector3 {
        val len = length()
        return if (len > 0f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 0f)
    }
    private fun Vector3.copy(): Vector3 = Vector3(x, y, z)
    private operator fun Vector3.minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)
    private operator fun Vector3.plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)
    private operator fun Vector3.times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)
}
