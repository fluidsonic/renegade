package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.GameObjManager
import ccr.server.defs.BeaconGameObjDef

// C++: BeaconGameObj : public SimpleGameObj (beacongameobj.h)
// Hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//          → PhysicalGameObj → SimpleGameObj → BeaconGameObj
class BeaconGameObj() : SimpleGameObj() {

    // C++: GameObjReference Owner (initialized to NULL via default ctor)
    val owner: GameObjReference = GameObjReference()

    // C++: int State (initialized to 0)
    // @JvmName avoids clash with private fun setState() below
    @get:JvmName("stateField") @set:JvmName("setStateField")
    var state: Int = 0

    // C++: float StateTimer (initialized to 0)
    var stateTimer: Float = 0f

    // C++: float PreDetonateTimer (initialized to 0)
    var preDetonateTimer: Float = 0f

    // C++: float DetonateTimer (initialized to 0)
    var detonateTimer: Float = 0f

    // C++: float WarningTimer (initialized to 0.0f)
    var warningTimer: Float = 0f

    // C++: AudibleSoundClass* ArmedSound (initialized to NULL)
    var armedSound: AudibleSoundClass? = null

    // C++: AudibleSoundClass* MessageSound (initialized to NULL)
    var messageSound: AudibleSoundClass? = null

    // C++: bool IsArmed (initialized to false)
    var isArmed: Boolean = false

    // C++: const WeaponDefinitionClass* WeaponDefinition (initialized to NULL)
    var weaponDefinition: ccr.server.defs.WeaponDefinitionClass? = null

    // C++: GameObjReference CinematicObject
    val cinematicObject: GameObjReference = GameObjReference()

    // C++: void* OwnerBackup (initialized to NULL)
    var ownerBackup: Any? = null

    // Test/secondary-constructor fields — allow direct access without going through getDefinition()
    // (which crashes when definition is a synthetic BaseGameObjDef rather than a real BeaconGameObjDef)
    var beaconDef: BeaconGameObjDef? = null

    // C++: float StateTimer — exposed as armTimer for test compatibility (both ARMING and ARMED share stateTimer)
    var armTimer: Float
        get() = stateTimer
        set(value) { stateTimer = value }

    // Owner rhost ID — used by exportRare when owner ref is null (secondary-constructor path)
    var ownerRhostId: Int = 0

    init {
        // C++: BeaconGameObj() { Set_App_Packet_Type(APPPACKETTYPE_BEACON); }
        setAppPacketType(APPPACKETTYPE_BEACON)
    }

    // Secondary constructor for tests — bypasses Init() / getDefinition() pipeline.
    constructor(
        definitionId: Int,
        position: Vector3 = Vector3(),
        modelName: String = "",
        animName: String = "",
        initialState: Int = STATE_NULL,
        initialOwnerId: Int = 0,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "beacon_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.modelName = modelName
        this.animName = animName
        this.state = initialState
        this.ownerRhostId = initialOwnerId
    }

    // C++: simplified timer logic for unit tests — runs the ARMING→ARMED timer without game
    // engine dependencies (TimeManager, BaseControllerClass, weather, HUD, etc.).
    fun thinkInternal(deltaSeconds: Float) {
        val def = beaconDef ?: return
        when (state) {
            STATE_ARMING -> {
                stateTimer -= deltaSeconds
                if (stateTimer <= 0f) {
                    // Transition to ARMED; carry overshoot into detonateTimer
                    detonateTimer = def.detonateTime + stateTimer   // stateTimer is negative (overshoot)
                    isArmed = true
                    state = STATE_ARMED
                }
            }
            STATE_ARMED -> {
                detonateTimer -= deltaSeconds
            }
        }
    }

    // C++: virtual ~BeaconGameObj()
    override fun destruct() {
        stopCurrentMessageSound()
        stopArmedSound()
        stopOwnerAnimation()
        cinematicObject.set(null)
        super.destruct()
    }

    // C++: const BeaconGameObjDef& Get_Definition() const
    fun getDefinition(): BeaconGameObjDef = definition as BeaconGameObjDef

    // C++: virtual void Init()
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const BeaconGameObjDef& definition)
    fun init(definition: BeaconGameObjDef) {
        super.init(definition)
        // C++: Peek_Physical_Object()->Set_Collision_Group(TERRAIN_AND_BULLET_COLLISION_GROUP)
        physObj?.setCollisionGroup(TERRAIN_AND_BULLET_COLLISION_GROUP)
    }

    // C++: BeaconGameObj* As_BeaconGameObj() { return this; }
    fun asBeaconGameObj(): BeaconGameObj = this

    // C++: SoldierGameObj* Get_Owner()
    fun getOwner(): SoldierGameObj? {
        if (owner.get() == null) return null
        val physical = owner.get()?.asPhysicalGameObj()
        return physical?.asSoldierGameObj()
    }

    // C++: void Init_Beacon(const WeaponDefinitionClass*, SoldierGameObj*, const Vector3&)
    fun initBeacon(definition: ccr.server.defs.WeaponDefinitionClass?, ownerSoldier: SoldierGameObj?, position: Vector3) {
        weaponDefinition = definition
        owner.set(ownerSoldier)
        setPosition(position)

        if (ownerSoldier != null) {
            ownerBackup = ownerSoldier.getPlayerData()
        }

        // C++: Become part of same team as player dropping the beacon
        if (owner.get() != null) {
            val smart = owner.get()?.asSmartGameObj()
            if (smart != null) {
                setPlayerType(smart.getPlayerType())
            }
        }
    }

    // C++: bool Can_Place_Here(const Vector3& position) — always returns true
    fun canPlaceHere(position: Vector3): Boolean = true

    // C++: void On_Arming_Interrupted()
    fun onArmingInterrupted() {
        setState(STATE_NULL)
    }

    // C++: cancel / disarm — sets the beacon to DISARMED state and marks for deletion.
    // Used in tests and scripting when a beacon is cancelled externally.
    fun cancel() {
        if (!isDeletePending) {
            setObjectDirtyBit(BIT_RARE, true)
            state = STATE_DISARMED
            setDeletePending()
        }
    }

    // C++: void Begin_Arming()
    fun beginArming() {
        setState(STATE_ARMING)
    }

    // C++: void On_Poked(ScriptableGameObj* poker)
    fun onPoked(poker: ScriptableGameObj?) {
        // C++: No implementation in beacongameobj.cpp
    }

    // C++: virtual void Completely_Damaged(const OffenseObjectClass& damager)
    override fun completelyDamaged(damager: OffenseObjectClass) {
        if (!isDeletePending) {
            setState(STATE_DISARMED)
            setDeletePending()
        }
    }

    // C++: virtual void Think()
    override fun think() {
        restoreOwner()

        // C++: Update_State()
        updateState()

        // C++: Server-only: check if owner was interrupted during arming
        // CombatManager::I_Am_Server() → always true on server
        if (state == STATE_ARMING) {
            if (wasOwnerInterrupted()) {
                stopOwnerAnimation()
                displayMessage(getDefinition().armingInterruptedTextId)

                // C++: Return ammo that created the beacon to owner
                val soldier = getOwner()
                if (soldier != null) {
                    val weaponBag = soldier.getWeaponBag()
                    val weaponDef = weaponDefinition
                    if (weaponDef != null && weaponBag != null) {
                        weaponBag.addWeapon(weaponDef.id.toInt(), 1, false)
                        val currWeapon = weaponBag.getWeapon()
                        if (currWeapon != null && currWeapon.getId() == weaponDef.id.toInt()) {
                            currWeapon.stopFiringSound()
                        }
                    }
                }

                // C++: Now destroy ourselves
                setDeletePending()
            }
        }
    }

    // C++: void Get_Information(StringClass& string)
    override fun getInformation(): String {
        return super.getInformation()
    }

    // C++: virtual void Export_Rare(BitStreamClass& packet)
    override fun exportRare(packet: BitStream) {
        restoreOwner()
        super.exportRare(packet)
        packet.addInt(state)
        val ownerId = if (getOwner() != null) getOwner()!!.getId() else ownerRhostId
        packet.addInt(ownerId)
    }

    // C++: virtual void Import_Rare(BitStreamClass& packet)
    override fun importRare(packet: BitStream) {
        super.importRare(packet)
        val importedState = packet.getInt()
        val ownerId = packet.getInt()
        if (ownerId != 0) {
            owner.set(GameObjManager.findSmartGameObj(ownerId))
        }
        setState(importedState)
    }

    // C++: virtual bool Save(ChunkSaveClass& csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_STATE,              state)
        csave.writeMicroChunk(MICROCHUNKID_STATE_TIMER,        stateTimer)
        csave.writeMicroChunk(MICROCHUNKID_DETONATE_TIMER,     detonateTimer)
        csave.writeMicroChunk(MICROCHUNKID_PRE_DETONATE_TIMER, preDetonateTimer)
        csave.writeMicroChunk(MICROCHUNKID_IS_ARMED,           isArmed)
        csave.endChunk()

        // C++: Save the owner (if necessary)
        if (owner.get() != null) {
            csave.beginChunk(CHUNKID_OWNER)
            owner.save(csave)
            csave.endChunk()
        }

        if (cinematicObject.get() != null) {
            csave.beginChunk(CHUNKID_CINEMATIC)
            cinematicObject.save(csave)
            csave.endChunk()
        }

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass& cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_VARIABLES -> loadVariables(cload)
                CHUNKID_OWNER     -> owner.load(cload)
                CHUNKID_CINEMATIC -> cinematicObject.load(cload)
                else -> error("Unrecognized BeaconGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this) — pointer remap not ported
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass& cload) — private
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                MICROCHUNKID_STATE              -> state              = cload.readInt()
                MICROCHUNKID_STATE_TIMER        -> stateTimer         = cload.readFloat()
                MICROCHUNKID_DETONATE_TIMER     -> detonateTimer      = cload.readFloat()
                MICROCHUNKID_PRE_DETONATE_TIMER -> preDetonateTimer   = cload.readFloat()
                MICROCHUNKID_IS_ARMED           -> isArmed            = cload.readBool()
                else -> error("Unrecognized BeaconGameObj variable chunk ID: ${cload.curMicroChunkId}")
            }
            cload.closeMicroChunk()
        }
    }

    // C++: void Set_State(int state) — private
    private fun setState(newState: Int) {
        if (state == newState) return

        restoreOwner()

        // C++: CombatManager::I_Am_Server() → always true on server
        // Set dirty bit for networking
        setObjectDirtyBit(BIT_RARE, true)

        val isNuke = getDefinition().isNuke()

        when (newState) {
            STATE_NULL -> {
                stopArmedSound()
                displayMessage(getDefinition().armingInterruptedTextId)
            }

            STATE_ARMING -> {
                stateTimer = getDefinition().armTime
                displayMessage(getDefinition().armingTextId)
                startOwnerAnimation()
            }

            STATE_ARMED -> {
                if (!isArmed) {
                    stopOwnerAnimation()

                    // C++: Set the detonation timer
                    detonateTimer = getDefinition().detonateTime
                    warningTimer = getDefinition().broadcastToAllTime

                    // C++: Create the "armed" sound
                    armedSound = WWAudioClass.getInstance()?.createContinuousSound(getDefinition().armedSoundDefId)
                    armedSound?.let { sound ->
                        sound.setTransform(getTransform())
                        sound.addToScene()
                    }

                    // C++: CombatManager::I_Am_Server() → always true on server
                    // Switch to pre-detonate weather
                    if (isNuke) {
                        BackgroundMgrClass.overrideSkyTint(0.8f, detonateTimer / 2f)
                        WeatherMgrClass.overrideWind(0f, 3f, 1f, detonateTimer / 2f)
                    } else {
                        BackgroundMgrClass.overrideClouds(1.0f, 1.0f, detonateTimer / 2f)
                        BackgroundMgrClass.overrideLightning(0.8f, 0.2f, 0.8f, 0f, 1.0f, detonateTimer / 2f)
                        WeatherMgrClass.overridePrecipitation(WeatherMgrClass.PRECIPITATION_RAIN, 2.0f, detonateTimer / 2f)
                    }

                    // C++: Notify base controllers the beacon is armed
                    val base = BaseControllerClass.findBase(getPlayerType())
                    base?.onBeaconArmed(this)

                    preDetonateTimer = maxOf(getDefinition().preDetonateCinematicDelay, 0.001f)
                    isArmed = true
                }
            }

            STATE_DISARMED -> {
                displayMessage(getDefinition().disarmedTextId)
                stopArmedSound()
                stopOwnerAnimation()

                // C++: CombatManager::I_Am_Server() → always true on server
                // Cancel weather override
                if (isNuke) {
                    BackgroundMgrClass.restoreSkyTint(5f)
                    WeatherMgrClass.restoreWind(5f)
                } else {
                    BackgroundMgrClass.restoreClouds(5f)
                    BackgroundMgrClass.restoreLightning(5f)
                    WeatherMgrClass.restorePrecipitation(5f)
                }

                // C++: Notify base that the beacon is disarmed
                val base = BaseControllerClass.findBase(getPlayerType())
                base?.onBeaconDisarmed(this)

                // C++: Stop cinematic
                cinematicObject.get()?.setDeletePending()
                cinematicObject.set(null)

                setDeletePending()
            }

            STATE_DETONATING -> {
                stopArmedSound()

                isArmed = false
                stateTimer = getDefinition().postDetonateTime

                // C++: CombatManager::I_Am_Server() → always true on server
                // Create the cinematic controller
                startCinematic(getDefinition().postDetonateCinematicDefId)

                // C++: Switch to post-detonate weather
                if (isNuke) {
                    WeatherMgrClass.overridePrecipitation(WeatherMgrClass.PRECIPITATION_ASH, 0.3f)
                }

                // C++: Create the explosion (if ExplosionDefID != 0)
                if (getDefinition().explosionDefId != 0) {
                    createExplosion()
                }

                // C++: Hide our model
                peekModel()?.setHidden(true)
            }
        }

        state = newState
    }

    // C++: void Update_State() — private
    private fun updateState() {
        stateTimer -= TimeManager.getFrameSeconds()

        if (isArmed) {
            // C++: Tweak the pitch of the armed sound
            armedSound?.let { sound ->
                val percent = 1.0f - detonateTimer / getDefinition().detonateTime
                sound.setPitchFactor(1.0f + percent * 5.0f)
            }

            // C++: Check to see if we've exploded
            detonateTimer -= TimeManager.getFrameSeconds()
            if (detonateTimer <= 0f) {
                setState(STATE_DETONATING)
            }

            if (preDetonateTimer != 0f) {
                preDetonateTimer -= TimeManager.getFrameSeconds()
                if (preDetonateTimer <= 0f) {
                    preDetonateTimer = 0f

                    // C++: CombatManager::I_Am_Server() → always true on server
                    startCinematic(getDefinition().preDetonateCinematicDefId)
                }
            }
        }

        // C++: Handle each state independently
        when (state) {
            STATE_NULL -> { /* nothing */ }

            STATE_ARMING -> {
                // C++: Update the action bar in the HUD
                val percent = 1.0f - stateTimer / getDefinition().armTime
                HUDInfo.setActionStatusValue(percent)

                // C++: Did the player successfully arm the beacon?
                if (stateTimer <= 0f) {
                    setState(STATE_ARMED)
                }
            }

            STATE_ARMED -> {
                if (warningTimer != 0f) {
                    warningTimer -= TimeManager.getFrameSeconds()
                    if (warningTimer <= 0f) {
                        warningTimer = 0f

                        // C++: Notify base controller beacon warning
                        val base = BaseControllerClass.findBase(getPlayerType())
                        base?.onBeaconWarning(this)
                    }
                }
            }

            STATE_DISARMED -> { /* nothing */ }

            STATE_DETONATING -> {
                // C++: Wait for post-detonate timer
                if (stateTimer <= 0f) {
                    // C++: CombatManager::I_Am_Server() → always true on server
                    if (CombatManager.doesBeaconPlacementEndsGame() && isInEnemyBase()) {
                        val base = getEnemyBase()
                        if (base != null) {
                            base.destroyBase()
                            base.setBeaconDestroyedBase(true)
                        }
                    }

                    // C++: Cancel weather override
                    val isNuke = getDefinition().isNuke()
                    if (isNuke) {
                        BackgroundMgrClass.restoreSkyTint(5f)
                        WeatherMgrClass.restoreWind(5f)
                        WeatherMgrClass.restorePrecipitation(5f)
                    } else {
                        BackgroundMgrClass.restoreClouds(5f)
                        BackgroundMgrClass.restoreLightning(5f)
                        WeatherMgrClass.restorePrecipitation(5f)
                    }

                    setDeletePending()
                }
            }
        }
    }

    // C++: BaseControllerClass* Get_Enemy_Base() — private
    private fun getEnemyBase(): BaseControllerClass? {
        val enemyType = if (getPlayerType() == PLAYERTYPE_GDI) PLAYERTYPE_NOD else PLAYERTYPE_GDI
        return BaseControllerClass.findBase(enemyType)
    }

    // C++: bool Is_In_Enemy_Base() — private
    private fun isInEnemyBase(): Boolean {
        val base = getEnemyBase() ?: return false
        val position = getPosition()
        val zone = base.getBeaconZone()
        return CollisionMath.overlapTest(zone, position) != CollisionMath.OUTSIDE
    }

    // C++: void Stop_Armed_Sound() — private
    private fun stopArmedSound() {
        armedSound?.let { sound ->
            sound.removeFromScene()
            // C++: REF_PTR_RELEASE(ArmedSound) — GC handles in Kotlin
            armedSound = null
        }
    }

    // C++: void Stop_Current_Message_Sound() — private
    private fun stopCurrentMessageSound() {
        messageSound?.let { sound ->
            sound.removeFromScene()
            // C++: REF_PTR_RELEASE(MessageSound) — GC handles in Kotlin
            messageSound = null
        }
    }

    // C++: void Display_Message(int text_id) — private
    private fun displayMessage(textId: Int) {
        stopCurrentMessageSound()

        val translateObj = TranslateDBClass.findObject(textId) ?: return
        val string = translateObj.getString()
        val soundDefId = translateObj.getSoundId()
        var duration = 2.0f

        var displayText = true
        if (soundDefId > 0) {
            messageSound = WWAudioClass.getInstance()?.createSound(soundDefId)
            messageSound?.let { sound ->
                duration = sound.getDuration() / 1000.0f
                sound.setTransform(getTransform())
                sound.addToScene()
                displayText = !sound.isSoundCulled()
            }
        }

        if (displayText && string != null) {
            val messageDuration = maxOf(duration, 5.0f)
            CombatManager.getMessageWindow()?.addMessage(string, Vector3(1f, 1f, 1f), null, messageDuration)
        }
    }

    // C++: void Start_Owner_Animation() — private
    private fun startOwnerAnimation() {
        restoreOwner()

        val soldier = getOwner()
        soldier?.setAnimation(getDefinition().armingAnimationName, true, 0f)

        // C++: Only show the HUD if the owner is the star
        // if (soldier == COMBAT_STAR) HUDInfo::Display_Action_Status_Bar(true)
        // FIXME: CombatManager::Get_The_Star() — star check not ported
        HUDInfo.displayActionStatusBar(true)
    }

    // C++: void Stop_Owner_Animation() — private
    private fun stopOwnerAnimation() {
        restoreOwner()

        val soldier = getOwner()
        soldier?.setAnimation(null)

        // C++: Only hide the HUD if the owner is the star
        // FIXME: CombatManager::Get_The_Star() — star check not ported
        HUDInfo.displayActionStatusBar(false)
    }

    // C++: bool Was_Owner_Interrupted() — private
    private fun wasOwnerInterrupted(): Boolean {
        restoreOwner()

        val soldier = getOwner()
        // C++: owner is interrupted if dead
        if (soldier == null || soldier.isDead) return true

        val control = soldier.getControl()
        // C++: Check each movement control (up to ANALOG_MOVE_LEFT inclusive)
        for (index in 0..ControlClass.AnalogControl.MOVE_LEFT.ordinal) {
            if (control.getAnalog(ControlClass.AnalogControl.entries[index]) != 0f) {
                return true
            }
        }

        // C++: Check each "boolean" control (jumping, etc)
        for (index in ControlClass.BooleanControl.JUMP.ordinal..ControlClass.BooleanControl.DROP_FLAG.ordinal) {
            if (control.getBoolean(ControlClass.BooleanControl.entries[index])) {
                return true
            }
        }

        return false
    }

    // C++: void Start_Cinematic(int id) — private
    private fun startCinematic(id: Int) {
        if (id == 0) return
        val gameObj = ObjectLibraryManager.createObject(id) as? PhysicalGameObj
        if (gameObj != null) {
            gameObj.startObservers()
            val position = getPosition()
            gameObj.setPosition(position)
            cinematicObject.set(gameObj)
        }
    }

    // C++: void Create_Explosion() — private
    private fun createExplosion() {
        if (CombatManager.isServer()) {
            restoreOwner()
        }

        val position = getPosition()

        // C++: don't explode if the owner is gone
        val ownerSoldier = getOwner() ?: return

        ExplosionManager.createExplosionAt(getDefinition().explosionDefId, getTransform(), ownerSoldier)

        // C++: Server-side: apply building damage within blast radius
        // CombatManager::I_Am_Server() → always true on server
        val explosionDef = DefinitionMgrClass.findDefinition(getDefinition().explosionDefId) as? ExplosionDefinitionClass
        if (explosionDef != null) {
            val outerRadius = explosionDef.damageRadius
            val outerRadius2 = outerRadius * outerRadius

            for (building in GameObjManager.getBuildingGameObjList()) {
                val dist2 = floatArrayOf(0f)
                building.findClosestPoly(position, dist2)
                if (dist2[0] <= outerRadius2) {
                    val percent = 1.0f - WWMath.clamp(WWMath.sqrt(dist2[0]) / outerRadius, 0.0f, 1.0f)
                    val strength = percent * explosionDef.damageStrength
                    val offense = OffenseObjectClass(strength, explosionDef.damageWarhead, ownerSoldier)
                    building.applyDamageBuilding(offense, true)
                }
            }
        }
    }

    // C++: void Restore_Owner()
    fun restoreOwner() {
        if (getOwner() == null && ownerBackup != null) {
            // C++: Try to find a SmartGameObj with the same playerData
            for (obj in GameObjManager.getSmartGameObjList()) {
                if (obj.getPlayerData() === ownerBackup) {
                    owner.set(obj)
                    break
                }
            }
        }
    }

    companion object {
        // C++: BeaconGameObj state enum
        const val STATE_NULL       = 0  // C++: STATE_NULL
        const val STATE_ARMING     = 1  // C++: STATE_ARMING
        const val STATE_ARMED      = 2  // C++: STATE_ARMED
        const val STATE_DISARMED   = 3  // C++: STATE_DISARMED
        const val STATE_DETONATING = 4  // C++: STATE_DETONATING

        // C++: CHUNKID_GAME_OBJECT_BEACON from combatchunkid.h = 0x00040135
        const val CHUNK_ID: UInt = 0x00040135u  // CHUNKID_GAME_OBJECT_BEACON

        // BeaconGameObj save/load chunk IDs (beacongameobj.cpp local enum)
        // C++: CHUNKID_PARENT = 0x0219043 (7 hex digits; compiler reads as 0x00219043 = 2199619)
        private const val CHUNKID_PARENT    = 0x00219043
        private const val CHUNKID_VARIABLES = 0x00219044  // CHUNKID_PARENT + 1
        private const val CHUNKID_OWNER     = 0x00219045  // CHUNKID_PARENT + 2
        private const val CHUNKID_CINEMATIC = 0x00219046  // CHUNKID_PARENT + 3

        // BeaconGameObj micro-chunk IDs (beacongameobj.cpp local enum)
        private const val MICROCHUNKID_STATE              = 1
        private const val MICROCHUNKID_STATE_TIMER        = 2
        private const val MICROCHUNKID_DETONATE_TIMER     = 3
        private const val MICROCHUNKID_PRE_DETONATE_TIMER = 4
        private const val MICROCHUNKID_IS_ARMED           = 5

        // C++: collision group constant
        private const val TERRAIN_AND_BULLET_COLLISION_GROUP = 3  // C++: TERRAIN_AND_BULLET_COLLISION_GROUP

        // C++: APPPACKETTYPE_BEACON
        private const val APPPACKETTYPE_BEACON = 5  // C++: AppPacketTypeEnum::APPPACKETTYPE_BEACON
    }
}
