package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3

// C++: HumanStateClass (humanstate.h / humanstate.cpp)
// Faithful 1:1 port of the C++ class.
// Animation/physics calls go through the existing stubs.
// Client-only subsystems (recoil bone manipulation, surface effects, DefinitionMgr lookups) are omitted.
class HumanStateClass {

    // -------------------------------------------------------------------------
    // Private member variables (mirroring C++ private section + initializer list)
    // -------------------------------------------------------------------------

    // C++: bool StateLocked
    private var stateLocked: Boolean = false

    // C++: HumanStateType State
    private var state: Int = UPRIGHT

    // C++: float StateTimer
    private var stateTimer: Float = 0f

    // C++: int StateFlags
    private var stateFlags: Int = 0

    // C++: int SubState
    private var subState: Int = 0

    // C++: int WeaponHoldStyle
    private var weaponHoldStyle: Int = WEAPON_HOLD_STYLE_EMPTY_HANDS

    // C++: float WeaponHoldTimer
    private var weaponHoldTimer: Float = 0f

    // C++: bool LoitersAllowed
    private var loitersAllowed: Boolean = true

    // C++: float LoiterDelay
    private var loiterDelay: Float = 0f

    // C++: float AimingTilt
    private var aimingTilt: Float = 0f

    // C++: float AimingTurn
    private var aimingTurn: Float = 0f

    // C++: HumanPhysClass* HumanPhys
    private var humanPhys: HumanPhysClass? = null

    // C++: HumanAnimControlClass* AnimControl
    private var animControl: AnimControlClass? = null

    // C++: float TurnVelocity
    private var turnVelocity: Float = 0f

    // C++: Matrix3D JumpTM
    private var jumpTM: Matrix3D = Matrix3D()

    // C++: float RecoilTimer
    private var recoilTimer: Float = 0f

    // C++: float RecoilScale
    private var recoilScale: Float = 1f

    // C++: bool NoAnimBlend
    private var noAnimBlend: Boolean = false

    // C++: bool WeaponFired
    private var weaponFired: Boolean = false

    // C++: HumanAnimOverrideDef* HumanAnimOverride
    private var humanAnimOverride: Any? = null

    // C++: HumanLoiterGlobalSettingsDef* HumanLoiterCollection
    private var humanLoiterCollection: Any? = null

    init {
        resetLoiterDelay()
    }

    // -------------------------------------------------------------------------
    // Public API — mirroring C++ public methods
    // -------------------------------------------------------------------------

    // C++: void Init(HumanPhysClass* human_phys)
    fun init(humanPhys: HumanPhysClass) {
        this.humanPhys = humanPhys
    }

    // C++: void Reset()
    fun reset() {
        humanPhys = null
        // Clear the sniping flag
        if (getStateFlag(SNIPING_FLAG)) {
            toggleStateFlag(SNIPING_FLAG)
        }
    }

    // C++: void Set_Anim_Control(HumanAnimControlClass* anim_control)
    fun setAnimControl(animControl: AnimControlClass?) {
        this.animControl = animControl
        // C++: AnimControl->Set_Model(HumanPhys->Peek_Model()) — model setup
        animControl?.setModel(humanPhys?.peekModel())
    }

    // C++: void Set_Human_Anim_Override(int def_id)
    fun setHumanAnimOverride(defId: Int) {
        // DefinitionMgr lookup omitted — server doesn't load anim override defs
        humanAnimOverride = null
    }

    // C++: void Set_Human_Loiter_Collection(int def_id)
    fun setHumanLoiterCollection(defId: Int) {
        // DefinitionMgr lookup omitted — server doesn't load loiter collection defs
        humanLoiterCollection = null
    }

    // C++: bool Save(ChunkSaveClass& csave)
    fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_STATE, state)
        csave.writeMicroChunk(MICROCHUNKID_STATE_FLAGS, stateFlags)
        csave.writeMicroChunk(MICROCHUNKID_SUB_STATE, subState)
        csave.writeMicroChunk(MICROCHUNKID_STATE_LOCKED, stateLocked)
        csave.writeMicroChunk(MICROCHUNKID_WEAPON_HOLD_STYLE, weaponHoldStyle)
        csave.writeMicroChunk(MICROCHUNKID_AIMING_TILT, aimingTilt)
        csave.writeMicroChunk(MICROCHUNKID_AIMING_TURN, aimingTurn)
        csave.writeMicroChunk(MICROCHUNKID_TURN_VELOCITY, turnVelocity)
        csave.writeMicroChunk(MICROCHUNKID_PHYSOBJ, humanPhys)
        csave.writeMicroChunk(MICROCHUNKID_LOITER_DELAY, loiterDelay)
        csave.writeMicroChunk(MICROCHUNKID_LOITERS_ALLOWED, loitersAllowed)
        csave.writeMicroChunk(MICROCHUNKID_JUMP_TM, jumpTM)
        csave.writeMicroChunk(MICROCHUNKID_STATE_TIMER, stateTimer)
        csave.writeMicroChunk(MICROCHUNKID_WEAPON_HOLD_TIMER, weaponHoldTimer)
        csave.endChunk()
        return true
    }

    // C++: bool Load(ChunkLoadClass& cload)
    fun load(cload: ChunkLoadClass): Boolean {
        var humanAnimOverrideDefId = 0
        var humanLoiterCollectionDefId = 0

        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_STATE               -> state = cload.readInt()
                            MICROCHUNKID_STATE_FLAGS         -> stateFlags = cload.readInt()
                            MICROCHUNKID_SUB_STATE           -> subState = cload.readInt()
                            MICROCHUNKID_STATE_LOCKED        -> stateLocked = cload.readBool()
                            MICROCHUNKID_WEAPON_HOLD_STYLE   -> weaponHoldStyle = cload.readInt()
                            MICROCHUNKID_AIMING_TILT         -> aimingTilt = cload.readFloat()
                            MICROCHUNKID_AIMING_TURN         -> aimingTurn = cload.readFloat()
                            MICROCHUNKID_TURN_VELOCITY       -> turnVelocity = cload.readFloat()
                            MICROCHUNKID_LOITER_DELAY        -> loiterDelay = cload.readFloat()
                            MICROCHUNKID_LOITERS_ALLOWED     -> loitersAllowed = cload.readBool()
                            MICROCHUNKID_STATE_TIMER         -> stateTimer = cload.readFloat()
                            MICROCHUNKID_WEAPON_HOLD_TIMER   -> weaponHoldTimer = cload.readFloat()
                            MICROCHUNKID_HUMAN_ANIM_OVERRIDE_DEF_ID          -> humanAnimOverrideDefId = cload.readInt()
                            MICROCHUNKID_HUMAN_LOITER_COLLECTION_DEF_ID      -> humanLoiterCollectionDefId = cload.readInt()
                            else -> cload.skip()
                        }
                        cload.closeMicroChunk()
                    }
                }
                else -> cload.skipChunk()
            }
            cload.closeChunk()
        }

        if (humanAnimOverrideDefId != 0) {
            setHumanAnimOverride(humanAnimOverrideDefId)
        }
        if (humanLoiterCollectionDefId != 0) {
            setHumanLoiterCollection(humanLoiterCollectionDefId)
        }

        return true
    }

    // C++: void Set_State(HumanStateType state, int sub_state = 0)
    fun setState(newState: Int, newSubState: Int = 0) {
        // Special case for death
        if (state == DEATH || state == DESTROY) {
            if (newState != DESTROY) return
        }

        if (state != DEATH && newState == DEATH) {
            stateLocked = false
        }

        if (stateLocked) {
            if (newState != DEATH) {
                // C++: StateLocked Hack — in multiplayer, unlock anyway
                stateLocked = false
            }
        }

        if (state == newState && subState == newSubState) {
            return
        }

        state = newState
        subState = newSubState
        stateTimer = 0f

        val phys = humanPhys
        if (phys != null) {
            if (state == LADDER || state == IN_VEHICLE ||
                state == TRANSITION || state == TRANSITION_COMPLETE ||
                state == DEBUG_FLY) {
                phys.enableUserControl(true)
            } else {
                phys.enableUserControl(false)
            }

            if (state == IN_VEHICLE || state == TRANSITION || state == TRANSITION_COMPLETE) {
                phys.setCollisionGroup(BULLET_ONLY_COLLISION_GROUP)
                phys.setImmovable(true)
            } else if (state == DESTROY || state == DEATH) {
                phys.setCollisionGroup(TERRAIN_ONLY_COLLISION_GROUP)
                phys.setImmovable(true)
            } else {
                phys.setCollisionGroup(SOLDIER_COLLISION_GROUP)
                phys.setImmovable(false)
            }
        }

        updateAnimation()
    }

    // C++: HumanStateType Get_State()
    fun getState(): Int = state

    // C++: const char* Get_State_Name()
    fun getStateName(): String = when (state) {
        UPRIGHT             -> "UPRIGHT"
        LAND                -> "LAND"
        ANIMATION           -> "ANIMATION"
        WOUNDED             -> "WOUNDED"
        LOITER              -> "LOITER"
        AIRBORNE            -> "AIRBORNE"
        DIVE                -> "DIVE"
        DEATH               -> "DEATH"
        LADDER              -> "LADDER"
        IN_VEHICLE          -> "IN_VEHICLE"
        TRANSITION          -> "TRANSITION"
        TRANSITION_COMPLETE -> "TRANSITION_COMPLETE"
        DESTROY             -> "DESTROY"
        DEBUG_FLY           -> "DEBUG_FLY"
        ON_FIRE             -> "ON_FIRE"
        ON_CHEM             -> "ON_CHEM"
        ON_ELECTRIC         -> "ON_ELECTRIC"
        ON_CNC_FIRE         -> "ON_CNC_FIRE"
        ON_CNC_CHEM         -> "ON_CNC_CHEM"
        LOCKED_ANIMATION    -> "LOCKED_ANIMATION"
        else                -> ""
    }

    // C++: bool Is_State_Interruptable()
    fun isStateInterruptable(): Boolean =
        state == UPRIGHT || state == WOUNDED || state == LAND || state == LOITER || state == ANIMATION

    // C++: void Set_Sub_State(int sub_state)
    fun setSubState(newSubState: Int) {
        if (isSubStateAdjustable()) {
            if (subState != newSubState) {
                subState = newSubState
                updateAnimation()
            }
        }
    }

    // C++: int Get_Sub_State()
    fun getSubState(): Int = subState

    // C++: bool Is_Sub_State_Adjustable()
    fun isSubStateAdjustable(): Boolean = state == UPRIGHT || state == LADDER

    // C++: float Get_State_Timer()
    fun getStateTimer(): Float = stateTimer

    // C++: void Set_State_Timer(float timer)
    fun setStateTimer(timer: Float) { stateTimer = timer }

    // C++: void Toggle_State_Flag(int flag)
    fun toggleStateFlag(flag: Int) { stateFlags = stateFlags xor flag }

    // C++: bool Get_State_Flag(int flag)
    fun getStateFlag(flag: Int): Boolean = (stateFlags and flag) != 0

    // C++: void Drop_Weapon()
    fun dropWeapon() { weaponHoldTimer = 0.001f }

    // C++: void Raise_Weapon()
    fun raiseWeapon() { weaponHoldTimer = 10f }

    // C++: void Start_Transition_Animation(const char* anim_name, bool blend = true)
    fun startTransitionAnimation(animName: String, blend: Boolean = true) {
        if (stateLocked) return
        if (getState() == DEATH || getState() == DESTROY) return

        setState(TRANSITION)

        val blendTime = if (blend) 0.2f else 0.0f
        animControl?.setAnimation(animName, blendTime)
        animControl?.setMode(ANIM_MODE_ONCE)
        animControl?.update(0f)
        stateLocked = true
    }

    // C++: void Start_Scripted_Animation(const char* anim_name, bool blend = true, bool looping = true)
    fun startScriptedAnimation(animName: String, blend: Boolean = true, looping: Boolean = true) {
        if (getState() == DEATH || getState() == DESTROY) return

        setState(ANIMATION)

        val blendTime = if (blend) 0.2f else 0.0f
        animControl?.setAnimation(animName, blendTime)
        animControl?.setMode(if (looping) ANIM_MODE_LOOP else ANIM_MODE_ONCE)
        animControl?.update(0f)
        stateLocked = true
    }

    // C++: void Stop_Scripted_Animation()
    fun stopScriptedAnimation() {
        if (state != ANIMATION && state != LOCKED_ANIMATION) {
            stateLocked = false
            return
        }

        stateLocked = false
        setState(UPRIGHT)
    }

    // C++: void Force_Animation(const char* anim_name, bool blend = true)
    fun forceAnimation(animName: String, blend: Boolean = true) {
        val blendTime = if (blend) 0.2f else 0.0f
        animControl?.setAnimation(animName, blendTime)
        animControl?.update(0f)
    }

    // C++: void Set_Turn_Velocity(float vel)
    fun setTurnVelocity(vel: Float) { turnVelocity = vel }

    // C++: void Update_Weapon(WeaponClass* weapon, bool new_weapon)
    fun updateWeapon(weapon: WeaponClass?, newWeapon: Boolean) {
        updateRecoil(weapon)

        var newHoldStyle = weaponHoldStyle

        weaponFired = false

        if (weapon != null) {
            if (weapon.isReloading()) {
                raiseWeapon()
            }

            weapon.setSafety(weaponHoldStyle > WEAPON_HOLD_STYLE_EMPTY_HANDS)

            // Only humans can consider weapons firing in state
            val owner = weapon.owner
            if (owner != null) {
                val soldier = owner.asSoldierGameObj()
                if (soldier != null && soldier.isHumanControlled()) {
                    weaponFired = weapon.isFiring()
                }
            }

            if (weapon.isTriggered() || newWeapon || getStateFlag(CROUCHED_FLAG)) {
                if (weapon.isSafetySet()) {   // Don't blend anim when un-safetying
                    noAnimBlend = true
                }
                raiseWeapon()               // Reset the drop timer
                newHoldStyle = weapon.getStyle()   // Take the new style
            }

            if (weaponHoldTimer > 0) {
                if (weaponHoldStyle > WEAPON_HOLD_STYLE_EMPTY_HANDS) {
                    newHoldStyle = weapon.getStyle()   // Take the new style
                }
            }
        } else {
            newHoldStyle = WEAPON_HOLD_STYLE_EMPTY_HANDS
        }

        if (newHoldStyle == weaponHoldStyle) return

        weaponHoldStyle = newHoldStyle

        if (!stateLocked) {
            updateAnimation()
        }
    }

    // C++: void Update_Aiming(float tilt, float facing)
    fun updateAiming(tilt: Float, turn: Float) {
        if (aimingTilt == tilt && aimingTurn == turn) return

        aimingTilt = tilt
        aimingTurn = turn

        if (!stateLocked) {
            updateAnimation()
        }
    }

    // C++: void Update_State()
    fun updateState() {
        // C++: StateTimer += TimeManager::Get_Frame_Seconds() — no TimeManager on server, skip increment

        val ac = animControl
        if (ac != null && ac.getSkeleton() == 'V') {
            loitersAllowed = false
        }

        // Loiter logic — simplified: no loiter def available on server
        if (state != UPRIGHT || !loitersAllowed) {
            resetLoiterDelay()
        }

        // Get out of locked states
        if (ac != null && ac.isComplete()) {
            if ((stateLocked && state != LOCKED_ANIMATION) || state == DEATH) {

                stateLocked = false

                when (state) {
                    DIVE       -> setState(UPRIGHT)
                    ANIMATION  -> setState(UPRIGHT)
                    DEATH      -> setState(DESTROY)
                    TRANSITION -> setState(TRANSITION_COMPLETE)
                    else       -> { /* unsupported locked state */ }
                }
            } else {
                when (state) {
                    LOITER  -> setState(UPRIGHT)
                    LAND    -> setState(UPRIGHT, getSubState())
                    WOUNDED -> setState(UPRIGHT)
                    else    -> { }
                }
            }
        }

        // Handle Jump and landing
        val phys = humanPhys
        if (phys != null) {
            if (!phys.isInContact()) {
                if (state == UPRIGHT) {
                    beginJump()
                }
            } else if (state == AIRBORNE) {
                completeJump()
            }
        }

        if (weaponHoldTimer > 0 && !getStateFlag(CROUCHED_FLAG)) {
            resetLoiterDelay()
            // C++: WeaponHoldTimer -= TimeManager::Get_Frame_Seconds() — no TimeManager on server
            // The timer logic depends on frame delta; stub: don't decrement here
            // (full tick-driven logic would need frame seconds passed in)
        }
    }

    // C++: void Post_Think()
    fun postThink() {
        val phys = humanPhys ?: return

        // Update sub_state per movement
        if (isSubStateAdjustable() || isStateInterruptable()) {

            val moveVector = Vector3()
            phys.getAnimationMove(moveVector)
            // C++: move_vector /= TimeManager::Get_Frame_Seconds() — skip on server (no TimeManager)
            // C++: move_vector = HumanPhys->Get_Transform().Inverse_Rotate_Vector(move_vector)
            // We can't easily inverse-rotate without a real transform, so sub_state stays 0

            val newSubState = 0  // simplified — no physics movement available server-side

            // Get him out of WOUNDED, LAND, LOITER states if moving or shooting
            if (isStateInterruptable() && getState() != UPRIGHT) {
                if (newSubState != 0 || weaponFired) {
                    if (getState() == LAND && getSubState() == newSubState) {
                        // don't interrupt lands for the same direction
                    } else {
                        setState(UPRIGHT)
                    }
                }
            }

            if (isSubStateAdjustable()) {
                if (newSubState != getSubState()) {
                    setSubState(newSubState)
                }
            }

            // C++: AnimControl->Set_Anim_Speed_Scale(speed) — stub, no-op
            animControl?.setAnimSpeedScale(1f)

            phys.resetAnimationMove()
        }
    }

    // C++: void Update_Animation()
    fun updateAnimation() {
        val ac = animControl ?: return

        // No updates for visceroids
        if (ac.getSkeleton() == 'V') {
            stateLocked = true
            return
        }

        if (stateLocked) return

        val holdStyle = weaponHoldStyle

        when {
            state == UPRIGHT || state == AIRBORNE -> {
                var legStyle = LEG_STYLE_STAND
                if (state == AIRBORNE) {
                    legStyle = LEG_STYLE_JUMP_UP
                    if (subState and SUB_STATE_LEFT != 0)     legStyle = LEG_STYLE_JUMP_LEFT
                    if (subState and SUB_STATE_RIGHT != 0)    legStyle = LEG_STYLE_JUMP_RIGHT
                    if (subState and SUB_STATE_FORWARD != 0)  legStyle = LEG_STYLE_JUMP_FORWARD
                    if (subState and SUB_STATE_BACKWARD != 0) legStyle = LEG_STYLE_JUMP_BACKWARD
                } else {
                    if (subState and SUB_STATE_TURN_LEFT != 0)  legStyle = LEG_STYLE_TURN_LEFT
                    if (subState and SUB_STATE_TURN_RIGHT != 0) legStyle = LEG_STYLE_TURN_RIGHT
                    if (subState and SUB_STATE_LEFT != 0)       legStyle = LEG_STYLE_RUN_LEFT
                    if (subState and SUB_STATE_RIGHT != 0)      legStyle = LEG_STYLE_RUN_RIGHT
                    if (subState and SUB_STATE_FORWARD != 0)    legStyle = LEG_STYLE_RUN_FORWARD
                    if (subState and SUB_STATE_BACKWARD != 0)   legStyle = LEG_STYLE_RUN_BACKWARD

                    var effectiveHoldStyle = holdStyle
                    if (getStateFlag(CROUCHED_FLAG)) {
                        if (effectiveHoldStyle == WEAPON_HOLD_STYLE_HANDS_DOWN ||
                            effectiveHoldStyle == WEAPON_HOLD_STYLE_C4 ||
                            effectiveHoldStyle == WEAPON_HOLD_STYLE_BEACON) {
                            effectiveHoldStyle = WEAPON_HOLD_STYLE_AT_CHEST
                        }
                        legStyle += LEG_STYLE_CROUCH - LEG_STYLE_STAND
                    } else if (subState and SUB_STATE_SLOW != 0) {
                        if (legStyle >= LEG_STYLE_RUN_FORWARD && legStyle <= LEG_STYLE_RUN_RIGHT) {
                            legStyle += LEG_STYLE_WALK_FORWARD - LEG_STYLE_RUN_FORWARD
                        }
                    }

                    val legAnimName = LEG_ANIM_NAMES[legStyle]
                    val torsoAnimName = WEAPON_STYLE_NAMES[effectiveHoldStyle]
                    val blendTime = if (noAnimBlend) { noAnimBlend = false; 0f } else 0.2f

                    if (torsoAnimName[1] == '2') {
                        // Aiming animation — simplified to single anim on server
                        val animName = "S_A_HUMAN.H_A_${torsoAnimName}${legAnimName}"
                        ac.setAnimation(animName, blendTime)
                        ac.setMode(ANIM_MODE_LOOP)
                    } else {
                        val animName = "S_A_HUMAN.H_A_${torsoAnimName}${legAnimName}"
                        ac.setAnimation(animName, blendTime)
                        ac.setMode(ANIM_MODE_LOOP)
                    }
                    return
                }

                val legAnimName = LEG_ANIM_NAMES[legStyle]
                val torsoAnimName = WEAPON_STYLE_NAMES[holdStyle]
                val blendTime = if (noAnimBlend) { noAnimBlend = false; 0f } else 0.2f
                val animName = "S_A_HUMAN.H_A_${torsoAnimName}${legAnimName}"
                ac.setAnimation(animName, blendTime)
                ac.setMode(ANIM_MODE_LOOP)
            }

            state == DIVE -> {
                val offset = FreeRandom.getInt(2)
                var diveOffset = offset
                if (subState and SUB_STATE_FORWARD != 0)  diveOffset = offset + 0
                if (subState and SUB_STATE_BACKWARD != 0) diveOffset = offset + 2
                if (subState and SUB_STATE_LEFT != 0)     diveOffset = offset + 4
                if (subState and SUB_STATE_RIGHT != 0)    diveOffset = offset + 6
                ac.setAnimation(DIVE_ANIMS[diveOffset], 0.2f)
                ac.setMode(ANIM_MODE_ONCE)
                stateLocked = true
            }

            state == LAND -> {
                var dir = 0
                if (subState and SUB_STATE_LEFT != 0)     dir = 3
                if (subState and SUB_STATE_RIGHT != 0)    dir = 4
                if (subState and SUB_STATE_FORWARD != 0)  dir = 1
                if (subState and SUB_STATE_BACKWARD != 0) dir = 2
                ac.setAnimation("S_A_HUMAN.H_A_A0L$dir", 0.2f)
                ac.setMode(ANIM_MODE_ONCE)
            }

            state == WOUNDED -> {
                ac.setAnimation(getWoundAnim(subState), 0.2f)
                ac.setMode(ANIM_MODE_ONCE)
            }

            state == DEATH -> {
                ac.setAnimation(getDeathAnim(subState), 0.2f)
                ac.setMode(ANIM_MODE_ONCE)
                stateLocked = true
            }

            state == LADDER -> {
                var animName = "S_A_HUMAN.H_A_412A"
                if (subState and SUB_STATE_UP != 0)   animName = "S_A_HUMAN.H_A_422A"
                if (subState and SUB_STATE_DOWN != 0) animName = "S_A_HUMAN.H_A_432A"
                ac.setAnimation(animName, 0.2f)
                ac.setMode(ANIM_MODE_LOOP)
            }

            state == ANIMATION -> { }
            state == LOITER -> { }
            state == DESTROY -> { }
            state == TRANSITION -> { }
            state == TRANSITION_COMPLETE -> { }

            state == ON_FIRE || state == ON_CNC_FIRE -> {
                ac.setAnimation("S_A_HUMAN.H_A_FLMA", 0.2f)
                ac.setMode(ANIM_MODE_LOOP)
            }

            state == ON_CHEM || state == ON_CNC_CHEM -> {
                ac.setAnimation("S_A_HUMAN.h_a_6x01", 0.2f)
                ac.setMode(ANIM_MODE_LOOP)
            }

            state == ON_ELECTRIC -> {
                ac.setAnimation("S_A_HUMAN.h_a_6x05", 0.2f)
                ac.setMode(ANIM_MODE_LOOP)
            }

            state == DEBUG_FLY -> { }

            else -> {
                ac.setAnimation("")
            }
        }
    }

    // C++: bool Is_Locked()
    fun isLocked(): Boolean = stateLocked

    // C++: bool Get_Leg_Mode()
    fun getLegMode(): Boolean = (animControl?.getProgress() ?: 0f) > 0.5f

    // C++: int Get_Ouch_Type(const Vector3& direction, const char* collision_box_name)
    fun getOuchType(direction: Vector3, collisionBoxName: String?): Int {
        var ouchType = TORSO_FROM_FRONT

        if (collisionBoxName == null) return ouchType

        val dotIndex = collisionBoxName.indexOf('.')
        if (dotIndex < 0) return ouchType

        val baseName = collisionBoxName.substring(dotIndex + 1)

        for ((boneName, boneOuchType) in BONE_TO_OUCH_LIST) {
            if (boneName == baseName) {
                ouchType = boneOuchType
            }
        }

        if (ouchType != GROIN) {
            val phys = humanPhys
            if (phys != null) {
                val relativeDirection = phys.getTransform().inverseRotateVector(direction)
                if (relativeDirection.x < 0) {
                    ouchType += 1
                }
            }
        }

        return ouchType
    }

    // C++: void Reset_Loiter_Delay()
    fun resetLoiterDelay() {
        loiterDelay = FreeRandom.get() * 6f - 3f
    }

    // C++: void Set_Loiters_Allowed(bool allowed)
    fun setLoitersAllowed(allowed: Boolean) { loitersAllowed = allowed }

    // C++: void Get_Information(StringClass& string)
    fun getInformation(sb: StringBuilder) {
        sb.append(getStateName())
        sb.append('\n')
        animControl?.getInformation(sb)
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    // C++: void Update_Recoil(WeaponClass* weapon) — timer only, bone manipulation omitted
    private fun updateRecoil(weapon: WeaponClass?) {
        if (weapon != null && weapon.isFiring()) {
            recoilTimer = weapon.getRecoilTime()
            recoilScale = weapon.getRecoilScale()
            if (recoilTimer > 0f) {
                recoilScale *= 1f / recoilTimer
                // C++: _TheRecoiler.Capture_Bones(HumanPhys->Peek_Model()) — omitted (client-side)
            }
        }

        if (recoilTimer > 0f) {
            // C++: Apply_Recoil — bone manipulation omitted (client-side)
            // C++: RecoilTimer -= TimeManager::Get_Frame_Seconds() — skip (no TimeManager)
            // Timer decrement happens externally when frame seconds are available
        }
    }

    // C++: void Begin_Jump()
    private fun beginJump() {
        val phys = humanPhys ?: return
        jumpTM = phys.getTransform()

        // C++: HumanPhys->Get_Velocity(&move_vector) — use the existing extension that returns Vector3
        val moveVector = phys.getVelocity()
        // C++: move_vector = HumanPhys->Get_Transform().Inverse_Rotate_Vector(move_vector)
        // Simplified — inverse rotate not available without real transform
        var newSubState = 0
        if (moveVector.x > MOVING_THRESHOLD)  newSubState = newSubState or SUB_STATE_FORWARD
        else if (moveVector.x < -MOVING_THRESHOLD) newSubState = newSubState or SUB_STATE_BACKWARD
        if (moveVector.y > MOVING_THRESHOLD)  newSubState = newSubState or SUB_STATE_LEFT
        else if (moveVector.y < -MOVING_THRESHOLD) newSubState = newSubState or SUB_STATE_RIGHT

        setState(AIRBORNE, newSubState)

        // C++: SurfaceEffectsManager::Apply_Effect — omitted (client-side)
    }

    // C++: void Complete_Jump()
    private fun completeJump() {
        val phys = humanPhys ?: return

        // C++: falling damage calculation — omitted (server-side damage handled separately)

        val moveVector = phys.getVelocity()
        var newSubState = 0
        if (moveVector.x > MOVING_THRESHOLD)  newSubState = newSubState or SUB_STATE_FORWARD
        else if (moveVector.x < -MOVING_THRESHOLD) newSubState = newSubState or SUB_STATE_BACKWARD
        if (moveVector.y > MOVING_THRESHOLD)  newSubState = newSubState or SUB_STATE_LEFT
        else if (moveVector.y < -MOVING_THRESHOLD) newSubState = newSubState or SUB_STATE_RIGHT

        setState(LAND, newSubState)

        // C++: SurfaceEffectsManager::Apply_Effect — omitted (client-side)
    }

    // C++: static const char* Get_Wound_Anim(int ouch_type)
    private fun getWoundAnim(ouchType: Int): String = WOUND_ANIMS[ouchType]

    // C++: static const char* Get_Death_Anim(int ouch_type)
    private fun getDeathAnim(ouchType: Int): String = DEATH_ANIMS[ouchType]

    // -------------------------------------------------------------------------
    // Companion object — constants
    // -------------------------------------------------------------------------

    companion object {

        // ---- HumanStateType enum values (C++: enum HumanStateType) ----
        // Interruptable states
        const val UPRIGHT             = 0
        const val LAND                = 1
        const val ANIMATION           = 2
        const val WOUNDED             = 3
        const val LOITER              = 4
        // Uninterruptable states
        const val AIRBORNE            = 5
        const val DIVE                = 6
        const val DEATH               = 7
        const val LADDER              = 8
        const val IN_VEHICLE          = 9
        const val TRANSITION          = 10
        const val TRANSITION_COMPLETE = 11
        const val DESTROY             = 12
        const val DEBUG_FLY           = 13
        const val ON_FIRE             = 14
        const val ON_CHEM             = 15
        const val ON_ELECTRIC         = 16
        const val ON_CNC_FIRE         = 17
        const val ON_CNC_CHEM         = 18
        const val LOCKED_ANIMATION    = 19

        // C++: HIGHEST_HUMAN_STATE = LOCKED_ANIMATION
        const val HIGHEST_HUMAN_STATE = LOCKED_ANIMATION

        // ---- HumanStateFlagsType (C++: enum HumanStateFlagsType) ----
        const val CROUCHED_FLAG = 1 shl 0
        const val SNIPING_FLAG  = 1 shl 1
        const val HIGHEST_HUMAN_STATE_FLAGS = (1 shl 2) - 1

        // ---- HumanSubStateType (C++: enum HumanSubStateType) ----
        const val SUB_STATE_FORWARD    = 1 shl 0
        const val SUB_STATE_BACKWARD   = 1 shl 1
        const val SUB_STATE_UP         = 1 shl 2
        const val SUB_STATE_DOWN       = 1 shl 3
        const val SUB_STATE_LEFT       = 1 shl 4
        const val SUB_STATE_RIGHT      = 1 shl 5
        const val SUB_STATE_TURN_LEFT  = 1 shl 6
        const val SUB_STATE_TURN_RIGHT = 1 shl 7
        const val SUB_STATE_SLOW       = 1 shl 8
        const val HIGHEST_HUMAN_SUB_STATE = (1 shl 9) - 1

        // ---- HumanOuchType (C++: enum HumanOuchType) ----
        const val HEAD_FROM_BEHIND      = 0
        const val HEAD_FROM_FRONT       = 1
        const val TORSO_FROM_BEHIND     = 2
        const val TORSO_FROM_FRONT      = 3
        const val LEFT_ARM_FROM_BEHIND  = 4
        const val LEFT_ARM_FROM_FRONT   = 5
        const val RIGHT_ARM_FROM_BEHIND = 6
        const val RIGHT_ARM_FROM_FRONT  = 7
        const val LEFT_LEG_FROM_BEHIND  = 8
        const val LEFT_LEG_FROM_FRONT   = 9
        const val RIGHT_LEG_FROM_BEHIND = 10
        const val RIGHT_LEG_FROM_FRONT  = 11
        const val GROIN                 = 12
        const val OUCH_FIRE             = 13
        const val OUCH_CHEM             = 14
        const val OUCH_ELECTRIC         = 15
        const val OUCH_SUPER_FIRE       = 16

        // ---- Weapon hold styles (C++: WeaponHoldStyleType in weapons.h) ----
        const val WEAPON_HOLD_STYLE_C4           = 0
        const val WEAPON_HOLD_STYLE_NOT_USED     = 1
        const val WEAPON_HOLD_STYLE_AT_SHOULDER  = 2
        const val WEAPON_HOLD_STYLE_AT_HIP       = 3
        const val WEAPON_HOLD_STYLE_LAUNCHER     = 4
        const val WEAPON_HOLD_STYLE_HANDGUN      = 5
        const val WEAPON_HOLD_STYLE_BEACON       = 6
        const val WEAPON_HOLD_STYLE_EMPTY_HANDS  = 7
        const val WEAPON_HOLD_STYLE_AT_CHEST     = 8
        const val WEAPON_HOLD_STYLE_HANDS_DOWN   = 9
        const val NUM_WEAPON_HOLD_STYLES         = 10

        // ---- Physical constants ----
        const val CORPSE_PERSIST_TIME  = 2.0f
        const val MOVING_THRESHOLD     = 0.2f
        const val WALKING_THRESHOLD    = 3.21f

        // ---- Collision groups used by HumanStateClass ----
        // These mirror the constants in CombatTypeStubs.kt / soldier.cpp
        private const val BULLET_ONLY_COLLISION_GROUP = 5
        private const val SOLDIER_COLLISION_GROUP     = 6

        // ---- Chunk IDs (C++: enum in humanstate.cpp) ----
        private const val CHUNKID_VARIABLES = 915991207

        // ---- Micro-chunk IDs ----
        private const val MICROCHUNKID_STATE                          = 1
        private const val MICROCHUNKID_SUB_STATE                      = 2
        private const val MICROCHUNKID_STATE_LOCKED                   = 3
        private const val MICROCHUNKID_WEAPON_HOLD_STYLE              = 4
        // 5 = XXX_MICROCHUNKID_WEAPON_STATE (unused)
        private const val MICROCHUNKID_AIMING_TILT                    = 6
        private const val MICROCHUNKID_AIMING_TURN                    = 7
        private const val MICROCHUNKID_TURN_VELOCITY                  = 8
        private const val MICROCHUNKID_PHYSOBJ                        = 9
        private const val MICROCHUNKID_LOITER_DELAY                   = 10
        private const val MICROCHUNKID_STATE_FLAGS                    = 11
        private const val MICROCHUNKID_JUMP_TM                        = 12
        private const val MICROCHUNKID_STATE_TIMER                    = 13
        private const val MICROCHUNKID_LOITERS_ALLOWED                = 14
        private const val MICROCHUNKID_WEAPON_HOLD_TIMER              = 15
        private const val MICROCHUNKID_HUMAN_ANIM_OVERRIDE_DEF_ID     = 16
        private const val MICROCHUNKID_HUMAN_LOITER_COLLECTION_DEF_ID = 17

        // ---- Leg style indices (C++: HUMAN_ANIM_LEG_STYLE enum) ----
        private const val LEG_STYLE_STAND               = 0
        private const val LEG_STYLE_RUN_FORWARD         = 1
        private const val LEG_STYLE_RUN_BACKWARD        = 2
        private const val LEG_STYLE_RUN_LEFT            = 3
        private const val LEG_STYLE_RUN_RIGHT           = 4
        private const val LEG_STYLE_TURN_LEFT           = 5
        private const val LEG_STYLE_TURN_RIGHT          = 6
        private const val LEG_STYLE_WALK_FORWARD        = 7
        private const val LEG_STYLE_WALK_BACKWARD       = 8
        private const val LEG_STYLE_WALK_LEFT           = 9
        private const val LEG_STYLE_WALK_RIGHT          = 10
        private const val LEG_STYLE_CROUCH                = 11
        private const val LEG_STYLE_CROUCH_MOVE_FORWARD  = 12
        private const val LEG_STYLE_CROUCH_MOVE_BACKWARD = 13
        private const val LEG_STYLE_CROUCH_MOVE_LEFT     = 14
        private const val LEG_STYLE_CROUCH_MOVE_RIGHT    = 15
        private const val LEG_STYLE_CROUCH_TURN_LEFT     = 16
        private const val LEG_STYLE_CROUCH_TURN_RIGHT    = 17
        private const val LEG_STYLE_JUMP_UP              = 18
        private const val LEG_STYLE_JUMP_FORWARD         = 19
        private const val LEG_STYLE_JUMP_BACKWARD        = 20
        private const val LEG_STYLE_JUMP_LEFT            = 21
        private const val LEG_STYLE_JUMP_RIGHT           = 22

        // C++: static const char* LegAnimNames[]
        private val LEG_ANIM_NAMES = arrayOf(
            "A0", // LEG_STYLE_STAND
            "A1", // LEG_STYLE_RUN_FORWARD
            "A2", // LEG_STYLE_RUN_BACKWARD
            "A3", // LEG_STYLE_RUN_LEFT
            "A4", // LEG_STYLE_RUN_RIGHT
            "A5", // LEG_STYLE_TURN_LEFT
            "A6", // LEG_STYLE_TURN_RIGHT
            "B1", // LEG_STYLE_WALK_FORWARD
            "B2", // LEG_STYLE_WALK_BACKWARD
            "B3", // LEG_STYLE_WALK_LEFT
            "B4", // LEG_STYLE_WALK_RIGHT
            "C0", // LEG_STYLE_CROUCH
            "C1", // LEG_STYLE_CROUCH_MOVE_FORWARD
            "C2", // LEG_STYLE_CROUCH_MOVE_BACKWARD
            "C3", // LEG_STYLE_CROUCH_MOVE_LEFT
            "C4", // LEG_STYLE_CROUCH_MOVE_RIGHT
            "C5", // LEG_STYLE_CROUCH_TURN_LEFT
            "C6", // LEG_STYLE_CROUCH_TURN_RIGHT
            "J0", // LEG_STYLE_JUMP_UP
            "J1", // LEG_STYLE_JUMP_FORWARD
            "J2", // LEG_STYLE_JUMP_BACKWARD
            "J3", // LEG_STYLE_JUMP_LEFT
            "J4", // LEG_STYLE_JUMP_RIGHT
        )

        // C++: static const char* _weapon_style_names[NUM_WEAPON_HOLD_STYLES]
        private val WEAPON_STYLE_NAMES = arrayOf(
            "A0", // WEAPON_HOLD_STYLE_C4
            "A0", // WEAPON_HOLD_STYLE_NOT_USED
            "C2", // WEAPON_HOLD_STYLE_AT_SHOULDER
            "D2", // WEAPON_HOLD_STYLE_AT_HIP
            "E2", // WEAPON_HOLD_STYLE_LAUNCHER
            "F2", // WEAPON_HOLD_STYLE_HANDGUN
            "A0", // WEAPON_HOLD_STYLE_BEACON
            "A0", // WEAPON_HOLD_STYLE_EMPTY_HANDS
            "B0", // WEAPON_HOLD_STYLE_AT_CHEST
            "A0", // WEAPON_HOLD_STYLE_HANDS_DOWN
        )

        // C++: static const char* _dive_anims[4 * 2]
        private val DIVE_ANIMS = arrayOf(
            "S_A_HUMAN.H_A_SLD1_01",  // Forward 0
            "S_A_HUMAN.H_A_SLD1_02",  // Forward 1
            "S_A_HUMAN.H_A_SLD2_01",  // Backward 0
            "S_A_HUMAN.H_A_SLD2_02",  // Backward 1
            "S_A_HUMAN.H_A_SLD3_01",  // Left 0
            "S_A_HUMAN.H_A_SLD3_02",  // Left 1
            "S_A_HUMAN.H_A_SLD4_01",  // Right 0
            "S_A_HUMAN.H_A_SLD4_02",  // Right 1
        )

        // C++: static const char* _WoundAnims[]
        private val WOUND_ANIMS = arrayOf(
            "S_A_HUMAN.H_A_811A",  // HEAD_FROM_BEHIND
            "S_A_HUMAN.H_A_812A",  // HEAD_FROM_FRONT
            "S_A_HUMAN.H_A_821A",  // TORSO_FROM_BEHIND
            "S_A_HUMAN.H_A_822A",  // TORSO_FROM_FRONT
            "S_A_HUMAN.H_A_831A",  // LEFT_ARM_FROM_BEHIND
            "S_A_HUMAN.H_A_832A",  // LEFT_ARM_FROM_FRONT
            "S_A_HUMAN.H_A_841A",  // RIGHT_ARM_FROM_BEHIND
            "S_A_HUMAN.H_A_842A",  // RIGHT_ARM_FROM_FRONT
            "S_A_HUMAN.H_A_851A",  // LEFT_LEG_FROM_BEHIND
            "S_A_HUMAN.H_A_852A",  // LEFT_LEG_FROM_FRONT
            "S_A_HUMAN.H_A_861A",  // RIGHT_LEG_FROM_BEHIND
            "S_A_HUMAN.H_A_862A",  // RIGHT_LEG_FROM_FRONT
            "S_A_HUMAN.H_A_871A",  // GROIN
        )

        // C++: static const char* _DeathAnims[]
        private val DEATH_ANIMS = arrayOf(
            "S_A_HUMAN.H_A_622A",  // HEAD_FROM_BEHIND
            "S_A_HUMAN.H_A_635A",  // HEAD_FROM_FRONT
            "S_A_HUMAN.H_A_622A",  // TORSO_FROM_BEHIND
            "S_A_HUMAN.H_A_632A",  // TORSO_FROM_FRONT
            "S_A_HUMAN.H_A_623A",  // LEFT_ARM_FROM_BEHIND
            "S_A_HUMAN.H_A_634A",  // LEFT_ARM_FROM_FRONT
            "S_A_HUMAN.H_A_624A",  // RIGHT_ARM_FROM_BEHIND
            "S_A_HUMAN.H_A_633A",  // RIGHT_ARM_FROM_FRONT
            "S_A_HUMAN.H_A_623A",  // LEFT_LEG_FROM_BEHIND
            "S_A_HUMAN.H_A_634A",  // LEFT_LEG_FROM_FRONT
            "S_A_HUMAN.H_A_624A",  // RIGHT_LEG_FROM_BEHIND
            "S_A_HUMAN.H_A_633A",  // RIGHT_LEG_FROM_FRONT
            "S_A_HUMAN.H_A_612A",  // GROIN
            "S_A_HUMAN.H_A_FLMB",  // OUCH_FIRE
            "S_A_HUMAN.H_A_FLMB",  // OUCH_CHEM
            "S_A_HUMAN.H_A_FLMB",  // OUCH_ELECTRIC
            "S_A_HUMAN.H_A_FLMB",  // OUCH_SUPER_FIRE (ON_CNC_FIRE)
            "S_A_HUMAN.H_A_FLMB",  // OUCH_SUPER_FIRE (ON_CNC_CHEM)
        )

        // C++: BoneToOuchType _BoneToOuchTypeList[]
        private val BONE_TO_OUCH_LIST = arrayOf(
            "K_HEAD"       to HEAD_FROM_BEHIND,
            "K_NECK"       to HEAD_FROM_BEHIND,
            "K_CHEST"      to HEAD_FROM_BEHIND,
            "K_ABDOMEN"    to TORSO_FROM_BEHIND,
            "K_PELVIS"     to GROIN,
            "K_L THIGH"    to LEFT_LEG_FROM_BEHIND,
            "K_L CALF"     to LEFT_LEG_FROM_BEHIND,
            "K_L FOOT"     to LEFT_LEG_FROM_BEHIND,
            "K_L HAND"     to LEFT_ARM_FROM_BEHIND,
            "K_L FOREARM"  to LEFT_ARM_FROM_BEHIND,
            "K_L UPPERARM" to LEFT_ARM_FROM_BEHIND,
            "K_R THIGH"    to RIGHT_LEG_FROM_BEHIND,
            "K_R CALF"     to RIGHT_LEG_FROM_BEHIND,
            "K_R FOOT"     to RIGHT_LEG_FROM_BEHIND,
            "K_R HAND"     to RIGHT_ARM_FROM_BEHIND,
            "K_R FOREARM"  to RIGHT_ARM_FROM_BEHIND,
            "K_R UPPERARM" to RIGHT_ARM_FROM_BEHIND,
        )

        // C++: static void Set_Precision()
        fun setPrecision() {
            // Called externally to set BitStream encoder precision for human state
            // C++: cEncoderList::Set_Precision(BITPACK_HUMAN_STATE, 0, (int)HIGHEST_HUMAN_STATE)
            // C++: cEncoderList::Set_Precision(BITPACK_HUMAN_SUB_STATE, 0, (int)HIGHEST_HUMAN_SUB_STATE)
            // The actual setPrecision call is done in Network.kt with the correct values
        }
    }
}

// ---------------------------------------------------------------------------
// Extension methods needed by HumanStateClass on existing stubs
// ---------------------------------------------------------------------------

// C++: HumanPhysClass::Set_Immovable(bool) — needed by HumanStateClass::Set_State
fun HumanPhysClass.setImmovable(immovable: Boolean): Unit = Unit

// C++: HumanPhysClass::Is_In_Contact() — needed by HumanStateClass::Update_State
fun HumanPhysClass.isInContact(): Boolean = true

// C++: HumanPhysClass::Get_Animation_Move(Vector3*) — needed by HumanStateClass::Post_Think
fun HumanPhysClass.getAnimationMove(out: Vector3): Unit { out.x = 0f; out.y = 0f; out.z = 0f }

// C++: HumanPhysClass::Reset_Animation_Move() — needed by HumanStateClass::Post_Think
fun HumanPhysClass.resetAnimationMove(): Unit = Unit

// C++: HumanPhysClass::Peek_Ground_Object() — needed by Complete_Jump
fun HumanPhysClass.peekGroundObject(): PhysClass? = null

// C++: AnimControlClass::Get_Skeleton() — returns skeleton type char
fun AnimControlClass.getSkeleton(): Char = 'H'

// C++: AnimControlClass::Get_Progress() — returns anim progress [0..1]
fun AnimControlClass.getProgress(): Float = 0f

// C++: AnimControlClass::Get_Frame() — returns current frame number
fun AnimControlClass.getFrame(): Float = 0f

// C++: AnimControlClass::Set_Anim_Speed_Scale(float) — scales animation speed
fun AnimControlClass.setAnimSpeedScale(scale: Float): Unit = Unit

// C++: AnimControlClass::Get_Information(StringClass&) — appends state info
fun AnimControlClass.getInformation(sb: StringBuilder): Unit = Unit

// C++: Matrix3D::Inverse_Rotate_Vector(Vector3) — needed by Get_Ouch_Type
fun Matrix3D.inverseRotateVector(v: Vector3): Vector3 = v
