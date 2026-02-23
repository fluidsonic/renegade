package ccr.server.net

import ccr.math.Vector3
import ccr.math.degToRadF
import ccr.net.bitstream.*
import ccr.physics.PhysController
import ccr.physics.moveable.MoveablePhysClass
import ccr.server.defs.SmartGameObjDef

// C++: SmartGameObj (smartgameobj.cpp)
// C++ hierarchy: ArmedGameObj → SmartGameObj
abstract class SmartGameObj : ArmedGameObj() {

    // C++: ControlClass Control
    val control: ControlClass = ControlClass()

    // C++: PhysControllerClass Controller
    val controller: PhysController = PhysController()

    // C++: bool ControlEnabled
    var controlEnabled: Boolean = true

    // C++: bool StealthEnabled
    var stealthEnabled: Boolean = false

    // C++: float StealthPowerupTimer
    var stealthPowerupTimer: Float = 0f

    // C++: float StealthFiringTimer
    var stealthFiringTimer: Float = 0f

    // C++: StealthEffectClass* StealthEffect
    var stealthEffect: StealthEffectClass? = null

    // C++: ActionClass Action
    val action: ActionClass = ActionClass()

    // C++: int ControlOwner
    var controlOwner: Int = 0

    // C++: PlayerDataClass* PlayerData
    var playerData: PlayerDataClass? = null
        private set

    // C++: bool IsEnemySeenEnabled
    var isEnemySeenEnabled: Boolean = false

    // C++: float MovingSoundTimer
    var movingSoundTimer: Float = 0f

    // C++: LogicalListenerClass* Listener
    private var listener: LogicalListenerClass? = null

    // C++: static float GlobalSightRangeScale
    companion object {
        var globalSightRangeScale: Float = 1.0f
    }

    // C++: void Import_Control_Cs(BitStreamClass& packet)
    fun importControlCs(packet: BitStream) = control.importCs(packet)

    // C++: void Export_Control_Cs(BitStreamClass& packet)
    fun exportControlCs(packet: BitStream) = control.exportCs(packet)

    // C++: void Import_Control_Sc(BitStreamClass& packet)
    fun importControlSc(packet: BitStream) = control.importSc(packet)

    // C++: void Export_Control_Sc(BitStreamClass& packet)
    fun exportControlSc(packet: BitStream) = control.exportSc(packet)

    // C++: PlayerDataClass* Get_Player_Data() — covered by var playerData

    // C++: void Set_Player_Data(PlayerDataClass*)
    fun setPlayerData(data: PlayerDataClass?) {
        playerData?.setGameObj(null)
        playerData = data
        playerData?.setGameObj(this)
    }

    // C++: bool Has_Player() { return ControlOwner != SERVER_CONTROL_OWNER; }
    fun hasPlayer(): Boolean = controlOwner != SERVER_CONTROL_OWNER

    // C++: bool Is_Human_Controlled() { return ControlOwner >= 0; }
    fun isHumanControlled(): Boolean = controlOwner >= 0

    // C++: bool Is_Controlled_By_Me() — always false on server
    fun isControlledByMe(): Boolean = false

    // C++: virtual void Generate_Control()
    open fun generateControl() {}

    // C++: int Get_Control_Owner() — covered by var controlOwner
    // C++: virtual int Get_Weapon_Control_Owner()
    open fun getWeaponControlOwner(): Int = controlOwner

    // C++: virtual void Set_Control_Owner(int) — covered by var controlOwner

    // C++: void Clear_Control()
    fun clearControl() = control.clearControl()

    // C++: void Set_Boolean_Control(BooleanControl, bool)
    fun setBooleanControl(c: ControlClass.BooleanControl, state: Boolean = true) = control.setBoolean(c, state)

    // C++: void Set_Analog_Control(AnalogControl, float)
    fun setAnalogControl(c: ControlClass.AnalogControl, value: Float) = control.setAnalog(c, value)

    // C++: ControlClass& Get_Control() — covered by val control

    // C++: void Control_Enable(bool enable) — covered by var controlEnabled
    // C++: bool Is_Control_Enabled() — covered by var controlEnabled

    // C++: void Reset_Controller()
    fun resetController() { controller.reset() }

    // C++: void Init(const SmartGameObjDef&)
    fun init(definition: SmartGameObjDef) {
        super.init(definition)
        copySettings(definition)
    }

    // C++: void Copy_Settings(const SmartGameObjDef&)
    fun copySettings(definition: SmartGameObjDef) {
        check(physObj != null)
        (physObj as? MoveablePhysClass)?.controller = controller
        registerListener()
        if (definition.isStealthUnit) enableStealth(true)
    }

    // C++: void Register_Listener()
    private fun registerListener() {
        listener?.let {
            // FIXME: Listener->Set_Scale(definition.ListenerScale) — LogicalListenerClass.setScale() not yet ported
            // FIXME: Listener->Attach_To_Object(Peek_Model()) — LogicalListenerClass.attachToObject() not yet ported
            it.addToScene()
        }
    }

    // C++: void Re_Init(const SmartGameObjDef&)
    fun reInit(definition: SmartGameObjDef) {
        super.reInit(definition)
        listener?.removeFromScene()
        stealthEffect = null  // C++: REF_PTR_RELEASE(StealthEffect) — GC handles in Kotlin
        copySettings(definition)
    }

    // C++: const SmartGameObjDef& Get_Definition() const
    fun getSmartDefinition(): SmartGameObjDef = definition as SmartGameObjDef

    // C++: void Enable_Stealth(bool onoff)
    open fun enableStealth(onoff: Boolean) {
        stealthEnabled = onoff
        allocStealthEffect()
        stealthEffect?.enableStealth(onoff)
    }

    // C++: void Alloc_Stealth_Effect()
    protected fun allocStealthEffect() {
        if (stealthEffect == null) {
            stealthEffect = StealthEffectClass()
            stealthEffect!!.fadeDistance = getStealthFadeDistance()
        }
    }

    // C++: virtual bool Save(ChunkSaveClass&)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_ARMEDGAMEOBJ_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_CONTROL_ENABLED,       controlEnabled)
        csave.writeMicroChunk(MICROCHUNKID_CONTROL_OWNER,         controlOwner)
        csave.writeMicroChunk(MICROCHUNKID_IS_ENEMY_SEEN_ENABLED, isEnemySeenEnabled)
        // MICROCHUNKID_CONTROLLER_PTR — pointer remap; saved for legacy load support
        csave.writeMicroChunk(MICROCHUNKID_MOVING_SOUND_TIMER,    movingSoundTimer)
        // MICROCHUNKID_PLAYER_DATA — pointer remap; not yet ported
        csave.writeMicroChunk(MICROCHUNKID_STEALTH_ENABLED,       stealthEnabled)
        csave.writeMicroChunk(MICROCHUNKID_STEALTH_POWERUP_TIMER, stealthPowerupTimer)
        csave.writeMicroChunk(MICROCHUNKID_STEALTH_FIRING_TIMER,  stealthFiringTimer)
        csave.endChunk()

        // CHUNKID_CONTROL — ControlClass not yet ported
        csave.beginChunk(CHUNKID_CONTROLLER)
        csave.beginChunk(PHYSCONTROLLER_CHUNK_VARIABLES)
        csave.writeMicroChunk(PHYSCONTROLLER_VARIABLE_MOVEVECTOR, controller.moveVector)
        csave.writeMicroChunk(PHYSCONTROLLER_VARIABLE_TURNLEFT,   controller.turnLeft)
        csave.endChunk()
        csave.endChunk()

        // CHUNKID_ACTION — ActionClass not yet ported

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass&)
    override fun load(cload: ChunkLoadClass): Boolean {
        check(controlOwner == 0) // WWASSERT(PlayerData == NULL)
        var newControlOwner = 0

        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_ARMEDGAMEOBJ_PARENT -> super.load(cload)
                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_CONTROL_ENABLED       -> controlEnabled       = cload.readBool()
                            MICROCHUNKID_CONTROL_OWNER         -> newControlOwner      = cload.readInt()
                            MICROCHUNKID_IS_ENEMY_SEEN_ENABLED -> isEnemySeenEnabled   = cload.readBool()
                            MICROCHUNKID_CONTROLLER_PTR        -> cload.readInt() // legacy pointer — ignored
                            MICROCHUNKID_MOVING_SOUND_TIMER    -> movingSoundTimer     = cload.readFloat()
                            MICROCHUNKID_PLAYER_DATA           -> cload.readInt() // pointer remap — not yet ported
                            MICROCHUNKID_STEALTH_ENABLED       -> stealthEnabled       = cload.readBool()
                            MICROCHUNKID_STEALTH_POWERUP_TIMER -> stealthPowerupTimer  = cload.readFloat()
                            MICROCHUNKID_STEALTH_FIRING_TIMER  -> stealthFiringTimer   = cload.readFloat()
                            else -> error("Unrecognized SmartGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_CONTROLLER -> {
                    while (cload.openChunk()) {
                        when (cload.curChunkId) {
                            PHYSCONTROLLER_CHUNK_VARIABLES -> {
                                while (cload.openMicroChunk()) {
                                    when (cload.curMicroChunkId) {
                                        PHYSCONTROLLER_VARIABLE_MOVEVECTOR -> {
                                            val v = cload.readVector3()
                                            controller.moveLeft    = v.x
                                            controller.moveForward = v.y
                                            controller.moveUp      = v.z
                                        }
                                        PHYSCONTROLLER_VARIABLE_TURNLEFT -> controller.turnLeft = cload.readFloat()
                                        else -> error("Unrecognized PhysController micro chunk ID: ${cload.curMicroChunkId}")
                                    }
                                    cload.closeMicroChunk()
                                }
                            }
                            else -> error("Unrecognized PhysController chunk ID: ${cload.curChunkId}")
                        }
                        cload.closeChunk()
                    }
                }
                CHUNKID_CONTROL       -> cload.skipChunk() // ControlClass not yet ported
                CHUNKID_ACTION        -> cload.skipChunk() // ActionClass not yet ported
                CHUNKID_STEALTH_EFFECT -> cload.skipChunk() // StealthEffectClass not yet ported
                else -> error("Unrecognized SmartGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        controlOwner = newControlOwner
        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        return true
    }

    companion object {
        private const val CHUNKID_VARIABLES            = 910991114
        private const val CHUNKID_CONTROL              = 910991115
        private const val CHUNKID_CONTROLLER           = 910991116
        private const val CHUNKID_ACTION               = 910991117
        private const val CHUNKID_ARMEDGAMEOBJ_PARENT  = 910991119
        private const val CHUNKID_STEALTH_EFFECT       = 910991121

        private const val MICROCHUNKID_CONTROL_ENABLED       = 1
        private const val MICROCHUNKID_CONTROL_OWNER         = 4
        private const val MICROCHUNKID_CONTROLLER_PTR        = 8
        private const val MICROCHUNKID_IS_ENEMY_SEEN_ENABLED = 9
        private const val MICROCHUNKID_MOVING_SOUND_TIMER    = 11
        private const val MICROCHUNKID_PLAYER_DATA           = 12
        private const val MICROCHUNKID_STEALTH_ENABLED       = 13
        private const val MICROCHUNKID_STEALTH_POWERUP_TIMER = 14
        private const val MICROCHUNKID_STEALTH_FIRING_TIMER  = 15

        // C++: PhysControllerClass chunk/micro IDs (wwphys/physcontrol.cpp)
        const val SERVER_CONTROL_OWNER = -99999    // C++: enum { SERVER_CONTROL_OWNER = -99999 }
        private const val STEALTH_BROKEN_FRACTION = 0.25f

        private const val PHYSCONTROLLER_CHUNK_VARIABLES       = 0x00000100
        private const val PHYSCONTROLLER_VARIABLE_MOVEVECTOR   = 0x00
        private const val PHYSCONTROLLER_VARIABLE_TURNLEFT     = 0x01
    }

    // C++: virtual void Think()
    override fun think() {
        if (controlEnabled) {
            applyControl()
        } else {
            controller.reset()
            getWeapon()?.setPrimaryTriggered(false)
            getWeapon()?.setSecondaryTriggered(false)
        }

        movingSoundTimer -= TimeManager.getFrameSeconds()
        if (movingSoundTimer < 0f) {
            movingSoundTimer += (0.5f + Math.random().toFloat() * 0.5f)  // C++: FreeRandom.Get_Float(0.5, 1)
            if (isEnemySeenEnabled) {
                for (obj in GameObjManager.getSmartGameObjs()) {
                    if (obj === this) continue
                    if (!isEnemy(obj)) continue
                    if (!obj.isVisible()) continue
                    if (isObjVisible(obj)) {
                        for (observer in observers) observer.enemySeen(this, obj)
                    }
                }
            }
        }

        // C++: stealth logic
        if (stealthPowerupTimer > 0f) stealthPowerupTimer -= TimeManager.getFrameSeconds()
        if (stealthFiringTimer > 0f)  stealthFiringTimer  -= TimeManager.getFrameSeconds()

        if ((stealthPowerupTimer > 0f || stealthEnabled) && stealthFiringTimer <= 0f) {
            allocStealthEffect()
            check(stealthEffect != null)
            stealthEffect!!.enableStealth(true)
            val healthFrac = if (defenseObject.healthMax > 0f) defenseObject.health / defenseObject.healthMax else 0f
            stealthEffect!!.setBroken(healthFrac < STEALTH_BROKEN_FRACTION)
        } else {
            stealthEffect?.enableStealth(false)
        }

        super.think()
    }

    // C++: virtual void Post_Think()
    override fun postThink() {
        super.postThink()
        if (isDeletePending) return
        control.clearOneTimeBoolean()
    }

    // C++: bool Is_Control_Data_Dirty(cPacket&) { return true; }
    fun isControlDataDirty(): Boolean = true

    // C++: ActionClass* Get_Action() — covered by val action

    // C++: void Set_Enemy_Seen_Enabled(bool) — covered by var isEnemySeenEnabled
    // C++: bool Is_Enemy_Seen_Enabled() — covered by var isEnemySeenEnabled

    // C++: static float Get_Global_Sight_Range_Scale()
    // C++: static void Set_Global_Sight_Range_Scale(float) — covered by companion object var

    // C++: virtual float Get_Max_Speed() { return 10; }
    open fun getMaxSpeed(): Float = 10f

    // C++: virtual float Get_Turn_Rate() { return DEG_TO_RADF(360); }
    open fun getTurnRate(): Float = degToRadF(360f)

    // C++: virtual void Begin_Hibernation()
    override fun beginHibernation() {
        super.beginHibernation()
        controller.reset()
        listener?.removeFromScene()
        action.beginHibernation()
    }

    // C++: virtual void End_Hibernation()
    override fun endHibernation() {
        super.endHibernation()
        listener?.addToScene()
        action.endHibernation()
    }

    // C++: void Toggle_Stealth()
    fun toggleStealth() { stealthEnabled = !stealthEnabled }

    // C++: bool Is_Stealth_Enabled()
    fun isStealthEnabled(): Boolean = stealthEnabled

    // C++: bool Is_Stealthed() const
    fun isStealthed(): Boolean = stealthEffect?.isStealthed() ?: false

    // C++: void Grant_Stealth_Powerup(float seconds)
    fun grantStealthPowerup(seconds: Float) { stealthPowerupTimer = seconds }

    // C++: float Remaining_Stealth_Powerup_Time()
    fun remainingStealthPowerupTime(): Float = stealthPowerupTimer

    // C++: StealthEffectClass* Peek_Stealth_Effect()
    fun peekStealthEffect(): StealthEffectClass? = stealthEffect

    // C++: virtual float Get_Stealth_Fade_Distance() { return 25.0f; }
    open fun getStealthFadeDistance(): Float = 25.0f

    // C++: virtual void Get_Information(StringClass& string)
    override fun getInformation(): String {
        val sb = StringBuilder(super.getInformation())
        val params = action.getParameters()
        sb.append("Observer:${params.observerId}\n")
        sb.append("Priority:${params.priority}\n")
        if (params.waypathId != 0) sb.append("Waypath:${params.waypathId}\n")
        return sb.toString()
    }

    // C++: virtual void On_Logical_Heard(LogicalListenerClass*, LogicalSoundClass*)
    open fun onLogicalHeard(listener: LogicalListenerClass, soundObj: LogicalSoundClass) {
        val sound = CombatSound()
        sound.type = CombatSoundType.entries.find { it.value == soundObj.typeMask } ?: CombatSoundType.NONE
        sound.position = soundObj.getPosition()
        sound.creator = soundObj.peekUserObj()
        for (observer in observers) observer.soundHeard(this, sound)
    }

    // C++: virtual Matrix3D Get_Look_Transform() { return Get_Transform(); }
    open fun getLookTransform(): ccr.math.Matrix3D = getTransform()

    // C++: virtual void Get_Velocity(Vector3& vel) { vel.Set(0,0,0); }
    open fun getVelocity(): Vector3 = Vector3(0f, 0f, 0f)

    // C++: virtual void Apply_Control() — subclasses override to move object
    open fun applyControl() {}

    // C++: virtual bool Is_Visible() { return true; }
    open fun isVisible(): Boolean = true

    // C++: bool Is_Obj_Visible(PhysicalGameObj* obj)
    fun isObjVisible(obj: PhysicalGameObj): Boolean {
        val def = getSmartDefinition()
        // C++: diff = obj->Get_Bullseye_Position() — use position as approximation
        // FIXME: use obj.getBullseyePosition() when ported to PhysicalGameObj
        var diff = obj.getPosition()

        // C++: Matrix3D::Inverse_Transform_Vector(look_tm, diff, &diff)
        // FIXME: use getLookTransform().inverseTransformVector(diff) when Matrix3D method is ported
        val lookTm = getLookTransform()
        val me = lookTm.translation
        diff = diff - me

        val dist = diff.length()
        if (dist >= def.sightRange * globalSightRangeScale) return false

        // C++: diff.Z = 0; diff.Normalize(); angle = Fast_Acos(diff.X)
        diff = Vector3(diff.x, diff.y, 0f)
        val len2d = kotlin.math.sqrt(diff.x * diff.x + diff.y * diff.y)
        if (len2d == 0f) return false
        val cosAngle = diff.x / len2d
        val angle = kotlin.math.acos(cosAngle.coerceIn(-1f, 1f))
        if (kotlin.math.abs(angle) >= def.sightArc / 2f) return false

        // C++: raycast for line-of-sight
        // FIXME: PhysicsScene raycast — requires physObj ignore counter + scene raycast
        return true
    }

    // C++: virtual void Apply_Damage(const OffenseObjectClass&, float, int)
    override fun applyDamage(damager: OffenseObjectClass, scale: Float, alternateSkin: Int) {
        val damage = damager.damage * scale
        if (damage > 0f) {
            stealthEffect?.damageOccurred()
        }
        super.applyDamage(damager, scale, alternateSkin)
    }

    // C++: void On_Post_Load()
    override fun onPostLoad() {
        super.onPostLoad()
        // StealthEffect != NULL: physObj!!.addEffectToMe(stealthEffect) — StealthEffectClass not yet ported
        registerListener()
        // PlayerData != NULL: setPlayerData(playerData) — PlayerDataClass not yet ported
    }

    // C++: SmartGameObj::Export_State_Cs
    override fun exportStateCs(packet: BitStream) {
        super.exportStateCs(packet)
        exportControlCs(packet)
    }

    // C++: SmartGameObj::Import_State_Cs
    override fun importStateCs(packet: BitStream) {
        super.importStateCs(packet)
        importControlCs(packet)
    }

    // C++: SmartGameObj::Export_Creation
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)
        packet.addInt(controlOwner)
    }

    // C++: SmartGameObj::Import_Creation
    override fun importCreation(packet: BitStream) {
        super.importCreation(packet)
        controlOwner = packet.getInt()
    }

    // C++: SmartGameObj::Export_Frequent
    override fun exportFrequent(packet: BitStream) {
        super.exportFrequent(packet)
        exportControlSc(packet)
    }

    // C++: SmartGameObj::Import_Frequent
    override fun importFrequent(packet: BitStream) {
        super.importFrequent(packet)
        importControlSc(packet)
    }
}
