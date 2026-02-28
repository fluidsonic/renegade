package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.GameObjManager
import ccr.server.defs.SoldierGameObjDef
import ccr.server.defs.WeaponDefinitionClass

// C++: SoldierGameObj : public SmartGameObj (soldier.cpp / soldier.h)
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//                → PhysicalGameObj → ArmedGameObj → SmartGameObj → SoldierGameObj
open class SoldierGameObj() : SmartGameObj() {

    // -------------------------------------------------------------------------
    // Protected render / animation fields (C++ protected section)
    // -------------------------------------------------------------------------

    // C++: RenderObjClass* WeaponRenderModel (initialized to NULL)
    protected var weaponRenderModel: RenderObjClass? = null

    // C++: RenderObjClass* BackWeaponRenderModel (initialized to NULL)
    protected var backWeaponRenderModel: RenderObjClass? = null

    // C++: RenderObjClass* BackFlagRenderModel (initialized to NULL)
    protected var backFlagRenderModel: RenderObjClass? = null

    // C++: AnimControlClass* WeaponAnimControl (initialized to NULL)
    protected var weaponAnimControl: AnimControlClass? = null

    // C++: bool DetonateC4 (initialized implicitly false in constructor list — not listed, defaults to false)
    var detonateC4: Boolean = false

    // C++: TransitionCompletionDataStruct* TransitionCompletionData (initialized to NULL)
    protected var transitionCompletionData: TransitionCompletionDataStruct? = null

    // C++: StringClass AnimationName (initialized to empty in constructor implicitly)
    var animationName: String = ""

    // C++: VehicleGameObj* Vehicle (initialized to NULL)
    var vehicle: VehicleGameObj? = null

    // C++: HumanStateClass HumanState (initialized via Init() in Copy_Settings)
    val humanState: HumanStateClass = HumanStateClass()

    // C++: float LegFacing (initialized to 0)
    protected var legFacing: Float = 0f

    // C++: bool SyncLegs (initialized to false)
    protected var syncLegs: Boolean = false

    // C++: bool LastLegMode (initialized to 0 / false)
    protected var lastLegMode: Boolean = false

    // C++: int KeyRing (initialized to 0)
    var keyRing: Int = 0

    // C++: bool IsUsingGhostCollision (not in ctor list — defaults false)
    protected var isUsingGhostCollision: Boolean = false

    // C++: DialogueClass DialogList[DIALOG_MAX] — list of dialogue entries
    protected val dialogList: MutableList<DialogueClass> = mutableListOf()

    // C++: AudibleSoundClass* CurrentSpeech (initialized to NULL)
    protected var currentSpeech: AudibleSoundClass? = null

    // C++: float HeadLookDuration (initialized to 0)
    protected var headLookDuration: Float = 0f

    // C++: Vector3 HeadRotation (initialized to 0,0,0)
    protected var headRotation: Vector3 = Vector3(0f, 0f, 0f)

    // C++: Vector3 HeadLookTarget (initialized to 0,0,0)
    protected var headLookTarget: Vector3 = Vector3(0f, 0f, 0f)

    // C++: Vector3 HeadLookAngle (initialized to 0,0,0)
    protected var headLookAngle: Vector3 = Vector3(0f, 0f, 0f)

    // C++: float HeadLookAngleTimer (initialized to 0)
    protected var headLookAngleTimer: Float = 0f

    // C++: ArmorWarheadManager::SpecialDamageType SpecialDamageMode (initialized to NONE)
    var specialDamageMode: Int = ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE

    // C++: float SpecialDamageTimer (initialized to 0)
    var specialDamageTimer: Float = 0f

    // C++: GameObjReference SpecialDamageDamager
    val specialDamageDamager: GameObjReference = GameObjReference()

    // C++: TransitionEffectClass* SpecialDamageEffect (initialized to NULL)
    protected var specialDamageEffect: TransitionEffectClass? = null

    // C++: TransitionEffectClass* HealingEffect (initialized to NULL)
    protected var healingEffect: TransitionEffectClass? = null

    // C++: GameObjReference FacingObject
    val facingObject: GameObjReference = GameObjReference()

    // C++: bool FacingAllowBodyTurn (not in ctor list — defaults false)
    protected var facingAllowBodyTurn: Boolean = false

    // C++: int InnateEnableBits (initialized to 0xFFFFFFFF)
    var innateEnableBits: Int = 0xFFFFFFFF.toInt()

    // C++: SoldierObserverClass* InnateObserver (initialized to NULL)
    var innateObserver: SoldierObserverClass? = null

    // C++: SoldierAIState AIState (initialized to AI_STATE_IDLE)
    // @JvmName avoids clash with fun setAiState() below
    @get:JvmName("aiStateField") @set:JvmName("setAiStateField")
    var aiState: Int = AI_STATE_IDLE

    // C++: DynamicSpeechAnimClass* SpeechAnim (initialized to NULL)
    protected var speechAnim: DynamicSpeechAnimClass? = null

    // C++: float GenerateIdleFacialAnimTimer (initialized to 0)
    protected var generateIdleFacialAnimTimer: Float = 0f

    // C++: RenderObjClass* HeadModel (initialized to NULL)
    protected var headModel: RenderObjClass? = null

    // C++: RenderObjClass* EmotIconModel (initialized to NULL)
    protected var emotIconModel: RenderObjClass? = null

    // C++: float EmotIconTimer (initialized to 0)
    protected var emotIconTimer: Float = 0f

    // C++: bool InFlyMode (initialized to false)
    protected var inFlyMode: Boolean = false

    // C++: bool IsVisible (initialized to true)
    // @JvmName avoids clash with override fun isVisible() below
    @get:JvmName("isVisibleField") @set:JvmName("setIsVisibleField")
    protected var isVisible: Boolean = true

    // C++: bool LadderUpMask (initialized to false)
    protected var ladderUpMask: Boolean = false

    // C++: bool LadderDownMask (initialized to false)
    protected var ladderDownMask: Boolean = false

    // C++: float ReloadingTilt (initialized to 0)
    protected var reloadingTilt: Float = 0f

    // C++: bool WeaponChanged (initialized to false)
    protected var weaponChanged: Boolean = false

    // C++: PersistantSurfaceEmitterClass* WaterWake (initialized via Create_Persistant_Emitter())
    protected var waterWake: PersistantSurfaceEmitterClass? = null

    // C++: DynamicVectorClass<RenderObjClass*> RenderObjList
    protected val renderObjList: MutableList<RenderObjClass> = mutableListOf()

    // -------------------------------------------------------------------------
    // Constructor / Destructor
    // -------------------------------------------------------------------------

    init {
        // C++: SoldierGameObj() constructor body:
        //   Set_Anim_Control(new HumanAnimControlClass);
        setAnimControl(HumanAnimControlClass())
        //   Set_App_Packet_Type(APPPACKETTYPE_SOLDIER);
        setAppPacketType(APPPACKETTYPE_SOLDIER)
        //   WaterWake = SurfaceEffectsManager::Create_Persistant_Emitter();
        waterWake = SurfaceEffectsManager.createPersistantEmitter()
    }

    // Secondary constructor for tests and server-side spawning — bypasses the full Init() pipeline.
    // Creates a soldier with the given parameters, setting up direct fields (position, modelName, etc.)
    // and populating the weapon bag (null at slot 0 = no-weapon sentinel, real weapons at slots 1+).
    constructor(
        definitionId: Int,
        controlOwner: Int = 0,
        team: Int = PLAYERTYPE_NEUTRAL,
        modelName: String = "",
        animName: String = "",
        position: ccr.math.Vector3 = ccr.math.Vector3(),
        facing: Float = 0f,
        health: Float = 0f,
        shieldStrength: Float = 0f,
        weapons: MutableList<WeaponEntry> = mutableListOf(),
    ) : this() {
        // Assign a synthetic definition so definitionId returns the correct value
        definition = ccr.server.defs.BaseGameObjDef(
            name = "soldier_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.controlOwner = controlOwner
        playerType = team
        this.position = position
        this.facing = facing
        this.modelName = modelName
        this.animName = animName
        defenseObject.health = health
        defenseObject.healthMax = health
        defenseObject.shieldStrength = shieldStrength
        defenseObject.shieldStrengthMax = shieldStrength
        // Set up weapon bag: null sentinel at slot 0, real weapons at slots 1+
        weaponBag.clearWeapons()  // clears and re-adds null sentinel at index 0
        for (entry in weapons) {
            weaponBag.addWeapon(entry.definitionId, entry.totalRounds)
        }
        // Select the first real weapon if any, otherwise keep fists selected (index 0)
        if (weapons.isNotEmpty()) weaponBag.selectIndex(1)
    }

    // Computed property: real weapons (slots 1+) in the weapon bag as WeaponEntry list
    val weapons: List<WeaponEntry>
        get() = (1 until weaponBag.getCount()).map { i ->
            val w = weaponBag.peekWeapon(i)!!  // index 1+ is always a real WeaponClass
            WeaponEntry(definitionId = w.definitionId, totalRounds = w.getTotalRounds())
        }

    // C++: ~SoldierGameObj()
    override fun destruct() {
        // C++: if (HealingEffect != NULL) { ... REF_PTR_RELEASE }
        healingEffect?.let {
            peekHumanPhys()?.removeEffectFromMe(it)
            healingEffect = null
        }

        // C++: Set_Special_Damage_Mode(NONE)
        setSpecialDamageMode(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE)

        // C++: Set_Emot_Icon(NULL, 0)
        setEmotIcon(null, 0f)

        // C++: REF_PTR_RELEASE(HeadModel); REF_PTR_RELEASE(SpeechAnim); REF_PTR_RELEASE(CurrentSpeech)
        headModel = null
        speechAnim = null
        currentSpeech = null  // In C++ also calls Stop()/Remove_From_Scene() — GC handles in Kotlin

        // C++: if (CombatManager::I_Am_Server()) CombatManager::On_Soldier_Death(this)
        CombatManager.onSoldierDeath(this)

        // C++: Set_Weapon_Model(NULL)
        setWeaponModel(null)

        // C++: BackWeaponRenderModel removal
        backWeaponRenderModel?.let {
            peekModel()?.removeSubObject(it)
            backWeaponRenderModel = null
        }

        // C++: BackFlagRenderModel removal
        backFlagRenderModel?.let {
            peekModel()?.removeSubObject(it)
            backFlagRenderModel = null
        }

        // C++: delete WeaponAnimControl; WeaponAnimControl = NULL
        weaponAnimControl = null

        // C++: if (Vehicle != NULL) Vehicle->Remove_Occupant(this)
        vehicle?.removeOccupant(this)

        // C++: COMBAT_SCENE->Remove_Object(Peek_Physical_Object())
        CombatManager.getScene()?.removeObject(peekPhysicalObject())

        // C++: if (Is_Human_Controlled()) GameObjManager::Remove_Star(this)
        if (isHumanControlled()) {
            GameObjManager.removeStar(this)
        }

        // C++: Reset_RenderObjs()
        resetRenderObjs()

        // C++: WaterWake cleanup
        waterWake?.let {
            SurfaceEffectsManager.destroyPersistantEmitter(it)
            waterWake = null
        }

        // Remove real physics object from scene (mirrors C++ COMBAT_SCENE->Remove_Object for the real phys obj)
        realPhysObj?.let { rp ->
            rp.scene?.removeObject(rp)
            realPhysObj = null
        }

        super.destruct()
    }

    // -------------------------------------------------------------------------
    // Init / Copy_Settings / Re_Init / Get_Definition
    // -------------------------------------------------------------------------

    // C++: virtual void Init(void) { Re_Init(Get_Definition()); }
    // NOTE: In C++, SoldierGameObjDef extends SmartGameObjDef; the Kotlin defs use composition.
    // We initialize by casting the stored definition at runtime.
    override fun init() {
        val smartDef = definition as? ccr.server.defs.SmartGameObjDef ?: return
        reInit(smartDef)
    }

    // C++: void Init(const SoldierGameObjDef & definition)
    override fun init(definition: ccr.server.defs.SmartGameObjDef) {
        super.init(definition)
        copySettings(definition)
    }

    // C++: void Copy_Settings(const SoldierGameObjDef & definition)
    override fun copySettings(definition: ccr.server.defs.SmartGameObjDef) {
        val soldierDef = definition as? SoldierGameObjDef ?: return

        // C++: HumanState.Init(Peek_Human_Phys())
        humanState.init(peekHumanPhys())

        // C++: HumanState.Set_Anim_Control((HumanAnimControlClass*)Get_Anim_Control())
        humanState.setAnimControl(animControl as? HumanAnimControlClass)

        // C++: if (Get_Definition().HumanAnimOverrideDefID != 0) HumanState.Set_Human_Anim_Override(...)
        if (soldierDef.humanAnimOverrideDefId != 0) {
            humanState.setHumanAnimOverride(soldierDef.humanAnimOverrideDefId)
        }

        // C++: if (Get_Definition().HumanLoiterCollectionDefID != 0) HumanState.Set_Human_Loiter_Collection(...)
        if (soldierDef.humanLoiterCollectionDefId != 0) {
            humanState.setHumanLoiterCollection(soldierDef.humanLoiterCollectionDefId)
        }

        // C++: Adjust_Skeleton(definition.SkeletonHeight, definition.SkeletonWidth)
        adjustSkeleton(soldierDef.skeletonHeight, soldierDef.skeletonWidth)

        // C++: model->Set_Sub_Objects_Match_LOD(true)
        peekHumanPhys()?.peekModel()?.setSubObjectsMatchLod(true)

        // C++: if (InnateObserver == NULL && Get_Definition().UseInnateBehavior && !Is_Controlled_By_Me())
        if (innateObserver == null && soldierDef.useInnateBehavior && !isControlledByMe()) {
            innateObserver = SoldierObserverClass()
            insertObserver(innateObserver!!)
        }

        // C++: for dialog copy
        dialogList.clear()
        for (i in 0 until DIALOG_MAX) {
            dialogList.add(
                if (i < soldierDef.dialogList.size) DialogueClass.fromData(soldierDef.dialogList[i])
                else DialogueClass()
            )
        }

        // C++: Peek_Physical_Object()->Set_Collision_Group(SOLDIER_COLLISION_GROUP)
        peekPhysicalObject()?.setCollisionGroup(SOLDIER_COLLISION_GROUP)

        // C++: Prepare_Speech_Framework()
        prepareSpeechFramework()

        // C++: SoldierGameObj::Init calls Setup_Innate_Weapons via ArmedGameObj::Copy_Settings
        // SoldierGameObjDef doesn't extend ArmedGameObjDef, so weapon setup must be done here.
        setupInnateWeapons(soldierDef)
    }

    // C++: ArmedGameObj::Copy_Settings — adds weapons from def's WeaponDefID / SecondaryWeaponDefID
    // SoldierGameObjDef uses composition: weapon fields are in def.armed.weaponDefId etc.
    private fun setupInnateWeapons(def: SoldierGameObjDef) {
        // Only run if real weapons haven't been added yet (count > 1 since index 0 is always null sentinel)
        if (weaponBag.getCount() > 1) return
        weaponBag.clearWeapons()
        val armed = def.armed
        if (armed.weaponDefId != 0) {
            weaponBag.addWeapon(armed.weaponDefId, armed.weaponRounds)
        }
        if (armed.secondaryWeaponDefId != 0) {
            weaponBag.addWeapon(armed.secondaryWeaponDefId, armed.weaponRounds)
        }
    }

    // C++: void Re_Init(const SoldierGameObjDef & definition)
    override fun reInit(definition: ccr.server.defs.SmartGameObjDef) {
        // C++: if (this == COMBAT_STAR) HUDClass::Force_Weapon_Chart_Update(); WeaponViewClass::Reset()
        // FIXME: wire HUDClass::Force_Weapon_Chart_Update() and WeaponViewClass::Reset() when ported

        // C++: COMBAT_SCENE->Remove_Object(Peek_Physical_Object())
        CombatManager.getScene()?.removeObject(peekPhysicalObject())

        // C++: Set_Weapon_Model(NULL)
        setWeaponModel(null)

        // C++: BackWeaponRenderModel cleanup
        backWeaponRenderModel?.let {
            peekModel()?.removeSubObject(it)
            backWeaponRenderModel = null
        }

        // C++: BackFlagRenderModel cleanup
        backFlagRenderModel?.let {
            peekModel()?.removeSubObject(it)
            backFlagRenderModel = null
        }

        // C++: delete WeaponAnimControl; WeaponAnimControl = NULL
        weaponAnimControl = null

        // C++: SmartGameObj::Re_Init(definition)
        super.reInit(definition)

        // C++: REF_PTR_RELEASE(HeadModel); REF_PTR_RELEASE(SpeechAnim); REF_PTR_RELEASE(CurrentSpeech)
        headModel = null
        speechAnim = null
        currentSpeech = null

        // C++: HumanState.Reset()
        humanState.reset()

        // C++: Copy_Settings(definition)
        copySettings(definition)

        // C++: Set_Object_Dirty_Bit(BIT_RARE, true)
        setObjectDirtyBit(BIT_RARE, true)

        // C++: if (this == COMBAT_STAR) HUDClass::Reset()
        // FIXME: wire HUDClass::Reset() when ported
    }

    // C++: const SoldierGameObjDef & Get_Definition() const
    fun getSoldierDefinition(): SoldierGameObjDef {
        return definition as SoldierGameObjDef
    }

    // -------------------------------------------------------------------------
    // Save / Load
    // -------------------------------------------------------------------------

    // C++: virtual bool Save(ChunkSaveClass&)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        // C++: for (int index = 0; index < DIALOG_MAX; index++) save dialog entries
        for (i in 0 until DIALOG_MAX) {
            csave.beginChunk(CHUNKID_DIALOG_ENTRY)
            if (i < dialogList.size) dialogList[i].save(csave) else DialogueClass().save(csave)
            csave.endChunk()
        }

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_DETONATE_C4,              detonateC4)
        csave.writeMicroChunk(MICROCHUNKID_LEG_FACING,               legFacing)
        csave.writeMicroChunk(MICROCHUNKID_SYNC_LEGS,                syncLegs)
        csave.writeMicroChunk(MICROCHUNKID_KEY_RING,                 keyRing)
        vehicle?.let { csave.writeMicroChunk(MICROCHUNKID_VEHICLE, it) }
        csave.beginMicroChunk(MICROCHUNKID_ANIMATION_NAME)
        csave.writeString(animationName)
        csave.endMicroChunk()
        csave.writeMicroChunk(MICROCHUNKID_INNATE_ENABLE_BITS,       innateEnableBits)
        // MICROCHUNKID_INNATE_OBSERVER_PTR — pointer remap; saved for legacy load support
        innateObserver?.let { csave.writeMicroChunk(MICROCHUNKID_INNATE_OBSERVER_PTR, it) }
        csave.writeMicroChunk(MICROCHUNKID_LAST_LEG_MODE,            lastLegMode)
        csave.writeMicroChunk(MICROCHUNKID_HEAD_LOOK_DURATION,       headLookDuration)
        csave.writeMicroChunk(MICROCHUNKID_HEAD_ROTATION,            headRotation)
        csave.writeMicroChunk(MICROCHUNKID_LOOK_TARGET,              headLookTarget)
        csave.writeMicroChunk(MICROCHUNKID_LOOK_ANGLE,               headLookAngle)
        csave.writeMicroChunk(MICROCHUNKID_LOOK_ANGLE_TIMER,         headLookAngleTimer)
        csave.writeMicroChunk(MICROCHUNKID_SPECIAL_DAMAGE_MODE,      specialDamageMode)
        csave.writeMicroChunk(MICROCHUNKID_SPECIAL_DAMAGE_TIMER,     specialDamageTimer)
        csave.writeMicroChunk(MICROCHUNKID_AI_STATE,                 aiState)
        // MICROCHUNKID_WEAPON_MODEL — pointer remap; saved for legacy load support
        weaponRenderModel?.let { csave.writeMicroChunk(MICROCHUNKID_WEAPON_MODEL, it) }
        csave.writeMicroChunk(MICROCHUNKID_IS_USING_GHOST_COLLISION,  isUsingGhostCollision)
        csave.endChunk()

        // C++: if (WeaponAnimControl) { CHUNKID_WEAPON_ANIM }
        weaponAnimControl?.let {
            csave.beginChunk(CHUNKID_WEAPON_ANIM)
            it.save(csave)
            csave.endChunk()
        }

        // C++: CHUNKID_HUMAN_STATE
        csave.beginChunk(CHUNKID_HUMAN_STATE)
        humanState.save(csave)
        csave.endChunk()

        // C++: if (TransitionCompletionData) { CHUNKID_TRANSITION_COMPLETION_DATA }
        transitionCompletionData?.let {
            csave.beginChunk(CHUNKID_TRANSITION_COMPLETION_DATA)
            it.save(csave)
            csave.endChunk()
        }

        // C++: for RenderObjList
        for (robj in renderObjList) {
            csave.beginChunk(CHUNKID_RENDER_OBJS)
            csave.beginChunk(robj.getFactory().chunkId)
            robj.getFactory().save(csave, robj)
            csave.endChunk()
            csave.endChunk()
        }

        // C++: if (SpecialDamageDamager.Get_Ptr() != NULL) CHUNKID_SPECIAL_DAMAGE_DAMAGER
        if (specialDamageDamager.get() != null) {
            csave.beginChunk(CHUNKID_SPECIAL_DAMAGE_DAMAGER)
            specialDamageDamager.save(csave)
            csave.endChunk()
        }

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass&)
    override fun load(cload: ChunkLoadClass): Boolean {
        var dialogIndex = 0

        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)

                CHUNKID_DIALOG_ENTRY -> {
                    if (dialogIndex < DIALOG_MAX) {
                        val dlg = DialogueClass()
                        dlg.load(cload)
                        dialogList.add(dlg)
                        dialogIndex++
                    }
                }

                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_DETONATE_C4              -> detonateC4              = cload.readBool()
                            MICROCHUNKID_LEG_FACING               -> legFacing               = cload.readFloat()
                            MICROCHUNKID_SYNC_LEGS                -> syncLegs                = cload.readBool()
                            MICROCHUNKID_VEHICLE                  -> cload.readInt() // pointer remap — FIXME: REQUEST_POINTER_REMAP for Vehicle
                            MICROCHUNKID_KEY_RING                 -> keyRing                 = cload.readInt()
                            MICROCHUNKID_ANIMATION_NAME           -> animationName           = cload.readString()
                            MICROCHUNKID_INNATE_OBSERVER_PTR      -> {
                                cload.readInt() // pointer remap — FIXME: REQUEST_POINTER_REMAP for InnateObserver
                            }
                            MICROCHUNKID_INNATE_ENABLE_BITS       -> innateEnableBits        = cload.readInt()
                            MICROCHUNKID_LAST_LEG_MODE            -> lastLegMode             = cload.readBool()
                            MICROCHUNKID_HEAD_LOOK_DURATION       -> headLookDuration        = cload.readFloat()
                            MICROCHUNKID_HEAD_ROTATION            -> headRotation            = cload.readVector3()
                            MICROCHUNKID_LOOK_TARGET              -> headLookTarget          = cload.readVector3()
                            MICROCHUNKID_LOOK_ANGLE               -> headLookAngle           = cload.readVector3()
                            MICROCHUNKID_LOOK_ANGLE_TIMER         -> headLookAngleTimer      = cload.readFloat()
                            MICROCHUNKID_SPECIAL_DAMAGE_MODE      -> specialDamageMode       = cload.readInt()
                            MICROCHUNKID_SPECIAL_DAMAGE_TIMER     -> specialDamageTimer      = cload.readFloat()
                            MICROCHUNKID_AI_STATE                 -> aiState                 = cload.readInt()
                            MICROCHUNKID_WEAPON_MODEL             -> cload.readInt() // pointer remap — FIXME: REQUEST_REF_COUNTED_POINTER_REMAP for WeaponRenderModel
                            MICROCHUNKID_IS_USING_GHOST_COLLISION -> isUsingGhostCollision   = cload.readBool()
                            else -> error("Unrecognized SoldierGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }

                CHUNKID_WEAPON_ANIM -> {
                    // C++: Set_Weapon_Animation(NULL) to get anim control built, then load
                    setWeaponAnimation(null)
                    weaponAnimControl?.load(cload)
                }

                CHUNKID_HUMAN_STATE -> humanState.load(cload)

                CHUNKID_RENDER_OBJS -> {
                    cload.openChunk()
                    val factory = SaveLoadSystemClass.findPersistFactory(cload.curChunkId)
                    if (factory != null) {
                        val robj = factory.load(cload) as? RenderObjClass
                        if (robj != null) {
                            addRenderObj(robj)
                        }
                    }
                    cload.closeChunk()
                }

                CHUNKID_TRANSITION_COMPLETION_DATA -> {
                    transitionCompletionData = TransitionCompletionDataStruct()
                    transitionCompletionData!!.load(cload)
                }

                CHUNKID_SPECIAL_DAMAGE_DAMAGER -> specialDamageDamager.load(cload)

                // Legacy chunks — skip silently
                XXXCHUNKID_C4_TIMER   -> cload.skipChunk()
                XXXCHUNKID_WEAPON_MODEL -> cload.skipChunk()

                else -> error("Unrecognized SoldierGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // C++: if (WeaponRenderModel != NULL) REQUEST_REF_COUNTED_POINTER_REMAP(...)
        // FIXME: REQUEST_REF_COUNTED_POINTER_REMAP for WeaponRenderModel — pointer remap not ported
        // C++: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback — not yet ported

        return true
    }

    // C++: virtual void On_Post_Load()
    override fun onPostLoad() {
        // C++: HumanState.Set_Anim_Control((HumanAnimControlClass*)Get_Anim_Control())
        humanState.setAnimControl(animControl as? HumanAnimControlClass)

        super.onPostLoad()

        // C++: if (Peek_Model() && WeaponRenderModel != NULL) Add_Sub_Object_To_Bone(...)
        if (peekModel() != null && weaponRenderModel != null) {
            peekModel()!!.addSubObjectToBone(weaponRenderModel!!, GUN_BONE_NAME)
        }

        // C++: Adjust_Skeleton(...)
        adjustSkeleton(getSoldierDefinition().skeletonHeight, getSoldierDefinition().skeletonWidth)

        // C++: Update_Back_Gun()
        updateBackGun()

        // C++: if (Is_In_Vehicle() && Peek_Model() != NULL) Peek_Model()->Set_Hidden(true)
        if (isInVehicle() && peekModel() != null) {
            peekModel()!!.setHidden(true)
        }

        // C++: Prepare_Speech_Framework()
        prepareSpeechFramework()
    }

    // -------------------------------------------------------------------------
    // Think / Post_Think
    // -------------------------------------------------------------------------

    // C++: virtual void Think()
    override fun think() {
        // C++: ghost collision zone check
        run {
            val position = getPosition()
            if (UnitCoordinationZoneMgr.isUnitInZone(position)) {
                enableGhostCollision(true)
            } else if (isSafeToDisableGhostCollision(position)) {
                enableGhostCollision(false)
            }
        }

        // C++: Stats — if (Get_Player_Data() != NULL) Stats_Add_Game_Time / Stats_Set_Final_Health
        playerData?.let { pd ->
            pd.statsAddGameTime(TimeManager.getFrameSeconds())
            pd.statsSetFinalHealth(defenseObject.health)
            if (vehicle != null) {
                pd.statsAddVehicleTime(TimeManager.getFrameSeconds())
            }
        }

        // C++: Update_Locked_Facing()
        updateLockedFacing()

        // C++: Handle_Legs()
        handleLegs()

        // C++: SmartGameObj::Think()
        super.think()

        // C++: Server: handle C4 scripted animation
        run {
            val weapon = getWeapon()
            if (weapon != null &&
                weapon.getStyle() == WEAPON_HOLD_STYLE_C4 &&
                weapon.isFiring() &&
                getState() == HumanStateClass.UPRIGHT
            ) {
                animationName = "s_a_human.h_a_j12c"
                humanState.startScriptedAnimation(animationName, true, false)
            }
        }

        // C++: if (Get_State() != IN_VEHICLE) HumanState.Update_Weapon / Update_State
        if (getState() != HumanStateClass.IN_VEHICLE) {
            humanState.updateWeapon(getWeapon(), weaponBag.isChanged)
            if (weaponBag.isChanged) {
                weaponChanged = true
                weaponBag.resetChanged()
            }
            humanState.updateState()

            // C++: Remove C4 from hand if empty
            val weapon = getWeapon()
            if (weapon != null && weaponRenderModel != null) {
                if (weapon.getStyle() == WEAPON_HOLD_STYLE_C4 || weapon.getStyle() == WEAPON_HOLD_STYLE_BEACON) {
                    weaponRenderModel!!.setHidden(!weapon.isLoaded())
                } else {
                    weaponRenderModel!!.setHidden(false)
                }
            }
        }

        // C++: Special damage tick
        if (specialDamageMode != ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE && specialDamageTimer > 0f) {
            val previousTimer = specialDamageTimer
            specialDamageTimer -= TimeManager.getFrameSeconds()

            // C++: once a second, create an explosion
            if (specialDamageTimer.toInt() != previousTimer.toInt()) {
                val explosionName = ArmorWarheadManager.getSpecialDamageExplosion(specialDamageMode)
                if (explosionName.isNotEmpty()) {
                    val def = DefinitionMgrClass.findTypedDefinition(explosionName, CLASSID_DEF_EXPLOSION)
                    if (def != null) {
                        val pos = getBullseyePosition()
                        val defId = (def as? ccr.server.defs.DefinitionClass)?.id?.toInt() ?: 0
                        ExplosionManager.createExplosionAt(defId, Matrix3D.translation(pos), null)
                    }
                }
            }

            if (defenseObject.health > 0f &&
                (allowSpecialDamageStateLock() || isHumanControlled())
            ) {
                val warhead = ArmorWarheadManager.getSpecialDamageWarhead(specialDamageMode)
                val damage = ArmorWarheadManager.getSpecialDamageScale(specialDamageMode) * TimeManager.getFrameSeconds()
                val off = OffenseObjectClass(damage, warhead, specialDamageDamager.get() as? ArmedGameObj)
                applyDamageExtended(off, 1f)
            }

            if (humanState.getState() != HumanStateClass.DEATH) {
                if (defenseObject.health <= 0f) {
                    humanState.setState(
                        HumanStateClass.DEATH,
                        specialDamageMode - ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE + HumanStateClass.OUCH_FIRE
                    )
                }
                if (specialDamageTimer <= 0f) {
                    setSpecialDamageMode(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE)
                }
            }
        }

        // C++: DESTROY state corpse timer
        if (getState() == HumanStateClass.DESTROY) {
            if (humanState.getStateTimer() > CORPSE_PERSIST_TIME) {
                setDeletePending()
            }
        }

        // C++: TRANSITION_COMPLETE handling (not client-only)
        if (getState() == HumanStateClass.TRANSITION_COMPLETE) {
            if (transitionCompletionData != null) {
                TransitionInstanceClass.end(this, transitionCompletionData!!)
                transitionCompletionData = null
            } else {
                humanState.setState(HumanStateClass.UPRIGHT)
            }
        }

        // C++: Emot icon timer update
        run {
            emotIconTimer -= TimeManager.getFrameSeconds()
            if (emotIconTimer <= 0f) {
                emotIconModel?.let {
                    if (it.isInScene()) {
                        it.remove()
                        emotIconModel = null
                    }
                }
            } else {
                emotIconModel?.let {
                    val tm = getTransform()
                    tm.setTranslation(tm.getTranslation() + Vector3(0f, 0f, EMOT_ICON_HEIGHT))
                    it.setTransform(tm)
                }
            }
        }

        // C++: Idle facial anim timer
        generateIdleFacialAnimTimer -= TimeManager.getFrameSeconds()
        if (generateIdleFacialAnimTimer <= 0f) {
            headModel?.let { hm ->
                if (hm.peekAnimation() == null || hm.peekAnimation() === speechAnim) {
                    speechAnim?.generateIdleAnimation(20, 0.5f)
                    speechAnim?.let { sa ->
                        hm.setAnimation(sa, 0f, RenderObjClass.ANIM_MODE_LOOP)
                    }
                }
            }
            generateIdleFacialAnimTimer = 100.0f
        }

        // C++: Apply Surface damage
        peekHumanPhys()?.let { hp ->
            SurfaceEffectsManager.applyDamage(hp.getContactSurfaceType(), this)
        }

        // C++: Water wake update
        run {
            var inWater = false
            val p0 = getTransform().getTranslation()
            val p1 = getBullseyePosition()
            val vel = peekHumanPhys()?.getVelocity() ?: Vector3()

            if (peekHumanPhys()?.getContactSurfaceType() == SURFACE_TYPE_UNDERWATER_DIRT &&
                vel.length2() > 0.1f
            ) {
                // FIXME: COMBAT_SCENE->Cast_Ray for water surface — raycast not yet fully wired
                // For now skip the raycast portion
            }

            if (!inWater) {
                SurfaceEffectsManager.updatePersistantEmitter(
                    waterWake, SURFACE_TYPE_DEFAULT,
                    SurfaceEffectsManager.HITTER_TYPE_FOOTSTEP_RUN,
                    Matrix3D.translation(p0)
                )
            }
        }

        // C++: Punish update
        playerData?.let { pd ->
            if (pd.getPunishTimer() > 0f) {
                pd.incPunishTimer(TimeManager.getFrameSeconds())
            }
        }
    }

    // C++: virtual void Post_Think()
    override fun postThink() {
        // Sync position/facing from real physics back to game-object fields before any export.
        syncPositionFromPhys()

        // C++: if (Get_State() == IN_VEHICLE) { SmartGameObj::Post_Think(); return; }
        if (getState() == HumanStateClass.IN_VEHICLE) {
            super.postThink()
            return
        }

        // C++: HumanState.Post_Think()
        humanState.postThink()

        var updateWeapon = false

        // C++: if (WeaponChanged) { Update_Back_Gun(); update_weapon = true; }
        if (weaponChanged) {
            weaponChanged = false
            updateBackGun()
            updateWeapon = true
        }

        // C++: if weapon model update needed
        val weapon = getWeapon()
        if (weapon != null && weapon.isModelUpdateNeeded()) {
            updateWeapon = true
            weapon.resetModelUpdate()
        }

        if (updateWeapon) {
            if (weapon != null) {
                setWeaponModel(weapon.getModelName())
            } else {
                setWeaponModel(null)
            }
        }

        // C++: WeaponAnimControl update
        weaponAnimControl?.update(TimeManager.getFrameSeconds())

        // C++: Handle_Head_look()
        handleHeadLook()

        // C++: if (Get_Weapon() != NULL && Get_State() == ON_FIRE) suppress triggers
        if (weapon != null && getState() == HumanStateClass.ON_FIRE) {
            weapon.setPrimaryTriggered(false)
            weapon.setSecondaryTriggered(false)
        }

        // C++: SmartGameObj::Post_Think()
        super.postThink()

        // C++: Update_Healing_Effect()
        updateHealingEffect()
    }

    // -------------------------------------------------------------------------
    // Control
    // -------------------------------------------------------------------------

    // C++: virtual void Set_Control_Owner(int control_owner)
    override var controlOwner: Int
        get() = super.controlOwner
        set(value) {
            if (isHumanControlled()) {
                GameObjManager.removeStar(this)
            }
            super.controlOwner = value
            if (isHumanControlled()) {
                GameObjManager.addStar(this)
            }
        }

    // C++: virtual void Generate_Control()
    override fun generateControl() {
        when (getState()) {
            HumanStateClass.DEATH -> clearControl()
            else                  -> super.generateControl()
        }
    }

    // C++: virtual void Apply_Control()
    override fun applyControl() {
        // C++: if (!CombatManager::Is_Gameplay_Permitted()) { Clear_Control(); Controller.Reset(); return; }
        if (!CombatManager.isGameplayPermitted()) {
            clearControl()
            controller.reset()
            return
        }

        // C++: IN_VEHICLE handling
        if (getState() == HumanStateClass.IN_VEHICLE) {
            // C++: handle vehicle gunner toggle
            if (vehicle != null && vehicle!!.getDriver() === this) {
                if (control.getBoolean(ControlClass.BOOLEAN_VEHICLE_TOGGLE_GUNNER)) {
                    vehicle!!.toggleDriverIsGunner()
                }
            }
            // C++: I_Am_Server() — action → TransitionManager::Check
            if (control.getBoolean(ControlClass.BOOLEAN_ACTION)) {
                TransitionManager.check(this, true)
            }
            return
        }

        // C++: Various states that clear control
        if (getState() == HumanStateClass.TRANSITION ||
            getState() == HumanStateClass.ANIMATION ||
            getState() == HumanStateClass.DESTROY ||
            getState() == HumanStateClass.DEATH ||
            getState() == HumanStateClass.ON_FIRE ||
            getState() == HumanStateClass.ON_CHEM ||
            getState() == HumanStateClass.ON_CNC_FIRE ||
            getState() == HumanStateClass.ON_CNC_CHEM ||
            getState() == HumanStateClass.ON_ELECTRIC
        ) {
            // C++: Force exit of corpse mode for human-controlled
            if (getState() == HumanStateClass.DESTROY && isHumanControlled()) {
                if (control.getBoolean(ControlClass.BOOLEAN_WEAPON_FIRE_PRIMARY)) {
                    humanState.setStateTimer(1000f)
                }
            }
            clearControl()
        }

        // C++: DIVE state
        if (getState() == HumanStateClass.DIVE) {
            clearControl()
            super.applyControl()
            val forcedMove = Vector3(0f, 0f, 0f)
            val subState = humanState.getSubState()
            val fm = when {
                (subState and HumanStateClass.SUB_STATE_FORWARD)  != 0 -> Vector3(0.5f, 0f, 0f)
                (subState and HumanStateClass.SUB_STATE_BACKWARD) != 0 -> Vector3(-0.5f, 0f, 0f)
                (subState and HumanStateClass.SUB_STATE_LEFT)     != 0 -> Vector3(0f, 0.5f, 0f)
                (subState and HumanStateClass.SUB_STATE_RIGHT)    != 0 -> Vector3(0f, -0.5f, 0f)
                else                                                    -> forcedMove
            }
            controller.setMoveForward(fm.x)
            controller.setMoveLeft(fm.y)
            controller.setMoveUp(fm.z)
            return
        }

        // C++: LADDER state — convert forward to up
        if (getState() == HumanStateClass.LADDER) {
            var upDown = CLIMB_SCALE * control.getAnalog(ControlClass.ANALOG_MOVE_FORWARD)
            if (control.getAnalog(ControlClass.ANALOG_MOVE_FORWARD) == 0f) {
                ladderUpMask = false
                ladderDownMask = false
            }
            if (ladderUpMask)   upDown = minOf(upDown, 0f)
            if (ladderDownMask) upDown = maxOf(upDown, 0f)
            control.setAnalog(ControlClass.ANALOG_MOVE_UP, upDown)
            control.setAnalog(ControlClass.ANALOG_MOVE_LEFT, 0f)
            control.setAnalog(ControlClass.ANALOG_TURN_LEFT, 0f)
            control.setBoolean(ControlClass.BOOLEAN_JUMP, false)
        } else {
            ladderUpMask = false
            ladderDownMask = false
            if (!inFlyMode) {
                control.setAnalog(ControlClass.ANALOG_MOVE_UP, 0f)
            }
        }

        // C++: Crouch toggle
        if (humanState.isSubStateAdjustable()) {
            val newCrouched = control.getBoolean(ControlClass.BOOLEAN_CROUCH)
            val oldCrouched = humanState.getStateFlag(HumanStateClass.CROUCHED_FLAG)
            if (newCrouched != oldCrouched) {
                humanState.toggleStateFlag(HumanStateClass.CROUCHED_FLAG)
            }
        }

        // C++: DetonateC4 / snipe toggle
        detonateC4 = false
        val currentWeapon = getWeapon()
        if (currentWeapon != null && control.getBoolean(ControlClass.BOOLEAN_WEAPON_USE)) {
            if (currentWeapon.getStyle() == WEAPON_HOLD_STYLE_C4) {
                detonateC4 = true
            }
            if (currentWeapon.getCanSnipe()) {
                if (humanState.isSubStateAdjustable()) {
                    humanState.toggleStateFlag(HumanStateClass.SNIPING_FLAG)
                }
            }
        }

        // C++: No diving for snipers/ladders
        if (isSniping() || isOnLadder()) {
            control.setBoolean(ControlClass.BOOLEAN_DIVE_FORWARD, false)
            control.setBoolean(ControlClass.BOOLEAN_DIVE_BACKWARD, false)
            control.setBoolean(ControlClass.BOOLEAN_DIVE_LEFT, false)
            control.setBoolean(ControlClass.BOOLEAN_DIVE_RIGHT, false)
        }

        // C++: Dive handling
        if (getState() == HumanStateClass.UPRIGHT) {
            if (control.getBoolean(ControlClass.BOOLEAN_DIVE_FORWARD)) {
                humanState.setState(HumanStateClass.DIVE, HumanStateClass.SUB_STATE_FORWARD)
            } else if (control.getBoolean(ControlClass.BOOLEAN_DIVE_BACKWARD)) {
                humanState.setState(HumanStateClass.DIVE, HumanStateClass.SUB_STATE_BACKWARD)
            } else if (control.getBoolean(ControlClass.BOOLEAN_DIVE_LEFT)) {
                humanState.setState(HumanStateClass.DIVE, HumanStateClass.SUB_STATE_LEFT)
            } else if (control.getBoolean(ControlClass.BOOLEAN_DIVE_RIGHT)) {
                humanState.setState(HumanStateClass.DIVE, HumanStateClass.SUB_STATE_RIGHT)
            }
        }

        // C++: Handle facing (turn rate)
        val turnAmount = control.getAnalog(ControlClass.ANALOG_TURN_LEFT)
        if (turnAmount != 0f) {
            var heading = peekHumanPhys()!!.getHeading()
            heading += turnAmount * getTurnRate() * TimeManager.getFrameSeconds()
            heading = WWMath.wrap(heading, DEG_TO_RADF_NEG_180, DEG_TO_RADF_POS_180)
            peekHumanPhys()!!.setHeading(heading)
            control.setAnalog(ControlClass.ANALOG_TURN_LEFT, 0f)
            if (!isHumanControlled()) {
                humanState.setTurnVelocity(turnAmount)
            }
        }

        // C++: Let parent class handle the rest
        super.applyControl()

        // C++: Jump handling (after Apply_Control, BOOLEAN_JUMP is cleared)
        if (control.getBoolean(ControlClass.BOOLEAN_JUMP)) {
            if (getState() != HumanStateClass.AIRBORNE) {
                controller.setMoveUp(getSoldierDefinition().jumpVelocity)
            }
        }

        // C++: Sniper speed clamp
        if (isSniping()) {
            controller.setMoveForward(controller.getMoveForward().coerceIn(-0.25f, 0.25f))
            controller.setMoveLeft(controller.getMoveLeft().coerceIn(-0.25f, 0.25f))
        }

        // C++: Ladder speed
        if (getState() == HumanStateClass.LADDER) {
            controller.setMoveForward(0f)
        }

        // C++: Crouch speed
        if (getState() == HumanStateClass.UPRIGHT && isCrouched()) {
            val crouchSpeed = GlobalSettingsDef.getGlobalSettings().getSoldierCrouchSpeed()
            controller.setMoveForward(controller.getMoveForward() * crouchSpeed)
            controller.setMoveLeft(controller.getMoveLeft() * crouchSpeed)
        } else if (control.getBoolean(ControlClass.BOOLEAN_WALK)) {
            val walkSpeed = GlobalSettingsDef.getGlobalSettings().getSoldierWalkSpeed()
            controller.setMoveForward(controller.getMoveForward() * walkSpeed)
            controller.setMoveLeft(controller.getMoveLeft() * walkSpeed)
        }

        // C++: Server action / transition check
        val actionTriggered = control.getBoolean(ControlClass.BOOLEAN_ACTION)
        val transitionTriggered = TransitionManager.check(this, actionTriggered)

        if (!transitionTriggered) {
            if (actionTriggered) {
                // C++: poke nearby objects
                val damageableTarget = HUDInfo.getInfoObject()
                if (damageableTarget != null) {
                    val physicalTarget = damageableTarget.asPhysicalGameObj()
                    if (physicalTarget != null) {
                        val targetPos = physicalTarget.getPosition()
                        val myPos = getPosition()
                        if ((targetPos - myPos).length() <= 2f) {
                            // C++: notify observers (server only) — poked
                            for (obs in physicalTarget.getObservers().toList()) {
                                obs.poked(physicalTarget, this)
                            }
                            // FIXME: EncyclopediaMgrClass::Reveal_Object — not yet ported
                            // FIXME: PlayerTerminalClass — not yet ported
                        }
                    }
                }
            }
        } else {
            // C++: transition triggered — reset controller
            controller.reset()
        }
    }

    // -------------------------------------------------------------------------
    // Damage
    // -------------------------------------------------------------------------

    // C++: virtual void Apply_Damage(const OffenseObjectClass&, float, int)
    override fun applyDamage(damager: OffenseObjectClass, scale: Float, alternateSkin: Int) {
        // C++: if (!Is_In_Vehicle()) SmartGameObj::Apply_Damage(...)
        if (!isInVehicle()) {
            super.applyDamage(damager, scale, alternateSkin)
        }
    }

    // C++: virtual void Apply_Damage_Extended(...)
    override fun applyDamageExtended(
        damager: OffenseObjectClass,
        scale: Float,
        direction: Vector3,
        collisionBoxName: String?
    ) {
        // C++: if repair → apply + manage healing effect
        if (defenseObject.isRepair(damager)) {
            super.applyDamageExtended(damager, scale, direction, collisionBoxName)
            if (healingEffect == null) {
                healingEffect = CombatMaterialEffectManager.getHealthEffect()
                healingEffect?.let { peekHumanPhys()?.addEffectToMe(it) }
            }
            healingEffect?.setTargetParameter(0.495f)
            return
        }

        if (isInVehicle()) return

        // C++: ambush damage scale check
        var effectiveScale = scale
        if ((aiState == AI_STATE_IDLE || aiState == AI_STATE_SECONDARY_IDLE) &&
            damager.getOwner() != null && !isHumanControlled()
        ) {
            effectiveScale *= AMBUSH_DAMAGE_SCALE
        }

        resetHibernating()

        val ouchType = getOuchType(direction, collisionBoxName)

        // C++: stats tracking
        run {
            val damagerOwner = damager.getOwner()
            val damagerData: PlayerDataClass? =
                (damagerOwner as? SoldierGameObj)?.playerData as? PlayerDataClass

            if (direction.length2() > WWMATH_EPSILON) {
                when (ouchType) {
                    HumanStateClass.HEAD_FROM_BEHIND, HumanStateClass.HEAD_FROM_FRONT -> {
                        playerData?.statsAddHeadHit()
                        damagerData?.statsAddHeadShot()
                    }
                    HumanStateClass.TORSO_FROM_BEHIND, HumanStateClass.TORSO_FROM_FRONT -> {
                        playerData?.statsAddTorsoHit()
                        damagerData?.statsAddTorsoShot()
                    }
                    HumanStateClass.LEFT_ARM_FROM_BEHIND, HumanStateClass.LEFT_ARM_FROM_FRONT,
                    HumanStateClass.RIGHT_ARM_FROM_BEHIND, HumanStateClass.RIGHT_ARM_FROM_FRONT -> {
                        playerData?.statsAddArmHit()
                        damagerData?.statsAddArmShot()
                    }
                    HumanStateClass.LEFT_LEG_FROM_BEHIND, HumanStateClass.LEFT_LEG_FROM_FRONT,
                    HumanStateClass.RIGHT_LEG_FROM_BEHIND, HumanStateClass.RIGHT_LEG_FROM_FRONT -> {
                        playerData?.statsAddLegHit()
                        damagerData?.statsAddLegShot()
                    }
                    HumanStateClass.GROIN -> {
                        playerData?.statsAddCrotchHit()
                        damagerData?.statsAddCrotchShot()
                    }
                }
            }
        }

        // C++: determine dialogue
        var dialogueId = -1
        val damageOwner = damager.getOwner()
        if (damageOwner != null) {
            dialogueId = if (isEnemy(damageOwner as? DamageableGameObj)) DIALOG_ON_TAKE_DAMAGE_FROM_ENEMY
                         else DIALOG_ON_TAKE_DAMAGE_FROM_FRIEND
        }

        val healthBefore = defenseObject.health
        val armorBefore  = defenseObject.shieldStrength

        // C++: bone damage scale
        var boneScale = effectiveScale
        if (collisionBoxName != null) {
            val start = collisionBoxName.indexOf('.')
            if (start >= 0) {
                val boneName = collisionBoxName.substring(start + 1)
                boneScale *= BonesManager.getBoneDamageScale(boneName)
            }
        }

        super.applyDamageExtended(damager, boneScale, direction, collisionBoxName)
        val health = defenseObject.health

        if (health == healthBefore && defenseObject.shieldStrength == armorBefore) {
            if (health > 0f ||
                humanState.getState() == HumanStateClass.DEATH ||
                humanState.getState() == HumanStateClass.DESTROY
            ) {
                return
            }
        }

        // C++: special damage check
        val warhead = damager.getWarhead()
        val specialDamage = ArmorWarheadManager.getSpecialDamageType(warhead)
        if (specialDamage != 0) {
            var probability = ArmorWarheadManager.getSpecialDamageProbability(warhead)
            if (health <= 0f) probability = 1f
            if (FreeRandom.getFloat() < probability) {
                val skin = defenseObject.getSkin()
                val sdType = ArmorWarheadManager.getSpecialDamageType(warhead)
                if (!ArmorWarheadManager.isSkinImpervious(sdType, skin)) {
                    setSpecialDamageMode(sdType, damager.getOwner() as? ArmedGameObj)
                }
            }
        }

        if (health <= 0f) {
            enableHibernation(false)

            // C++: stats for killer
            val damagerSoldier = damager.getOwner() as? SoldierGameObj
            if (damagerSoldier != null && damagerSoldier.playerData != null) {
                val pd = damagerSoldier.playerData as PlayerDataClass
                when {
                    isTeammate(damagerSoldier) -> pd.statsAddAllyKilled()
                    isEnemy(damagerSoldier)    -> pd.statsAddEnemyKilled()
                }
                if (damagerSoldier.vehicle != null) pd.statsAddKillFromVehicle()
            }

            if (humanState.getState() != HumanStateClass.DEATH) {
                // FIXME: CombatManager::Register_Star_Killer — not yet ported
                humanState.setState(HumanStateClass.DEATH, ouchType)
                stopCurrentSpeech()

                // C++: death sound
                var deathSoundId = getSoldierDefinition().deathSoundPresetId
                if (deathSoundId == 0) {
                    deathSoundId = GlobalSettingsDef.getGlobalSettings().getDeathSoundId()
                }
                // FIXME: EncyclopediaMgrClass::Reveal_Object — not yet ported
                if (deathSoundId != 0) {
                    val tm = getTransform()
                    WWAudioClass.getInstance().createInstantSound(deathSoundId, tm, this)
                }

                // C++: killed explosion
                if (getSoldierDefinition().killedExplosion != 0) {
                    val pos = getPosition()
                    ExplosionManager.createExplosionAt(getSoldierDefinition().killedExplosion, Matrix3D.translation(pos), damager.getOwner())
                }

                // C++: killer says dialogue
                val ourKiller = (damageOwner as? SoldierGameObj)
                if (ourKiller != null && ourKiller !== this) {
                    if (isEnemy(damageOwner)) ourKiller.sayDialogue(DIALOG_ON_KILLED_ENEMY)
                    else                      ourKiller.sayDialogue(DIALOG_ON_KILLED_FRIEND)
                }
            }
        } else {
            // C++: wound animation
            if (getState() == HumanStateClass.UPRIGHT) {
                humanState.setState(HumanStateClass.WOUNDED, ouchType)
            }
        }

        if (currentSpeech == null && dialogueId >= 0) {
            sayDialogue(dialogueId)
        }
    }

    // C++: virtual void Completely_Damaged(const OffenseObjectClass&) {}
    override fun completelyDamaged(damager: OffenseObjectClass) {
        // C++: intentionally empty for SoldierGameObj — death handled in Apply_Damage_Extended
    }

    // C++: virtual CollisionReactionType Collision_Occurred(const CollisionEventClass&)
    override fun collisionOccurred(event: CollisionEventClass): CollisionReactionType {
        // C++: detect squishing by vehicle
        event.otherObj?.let { otherPhys ->
            val observer = otherPhys.getObserver()
            if (observer != null) {
                val obj = (observer as? CombatPhysObserverClass)?.asPhysicalGameObj()
                val vehicle = obj?.asVehicleGameObj()
                if (vehicle != null && isEnemy(obj)) {
                    val moveablePhys = otherPhys.asMoveablePhysClass()
                    if (moveablePhys != null) {
                        val vel = moveablePhys.getVelocity()
                        val myPos = getPosition()
                        val vehiclePos = obj.getPosition()
                        if (vel.length() > vehicle.getSquishVelocity() &&
                            Vector3.dotProduct(vel, myPos - vehiclePos) > 0f
                        ) {
                            if (humanState.getState() != HumanStateClass.DEATH) {
                                val damager = vehicle.getDriver() ?: vehicle
                                val offense = OffenseObjectClass(10000f, 1, damager)
                                applyDamageExtended(offense, 10000f, vel, null)

                                // C++: Stats_Add_Squish
                                val playerData = vehicle.getDriver()?.playerData
                                (playerData as? PlayerDataClass)?.statsAddSquish()
                            }
                        }
                    }
                }
            }
        }
        return CollisionReactionType.DEFAULT
    }

    // -------------------------------------------------------------------------
    // Network Export / Import
    // -------------------------------------------------------------------------

    // C++: virtual void Export_Creation(BitStreamClass&)
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)
    }

    // C++: virtual void Import_Creation(BitStreamClass&)
    override fun importCreation(packet: BitStream) {
        super.importCreation(packet)
    }

    // C++: virtual void Export_Rare(BitStreamClass&)
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)
        // C++: packet.Add(definition_id) — adds definition ID
        val definitionId = definition?.id?.toInt() ?: 0
        packet.addInt(definitionId)
    }

    // C++: virtual void Import_Rare(BitStreamClass&)
    override fun importRare(packet: BitStream) {
        // C++: Set_Weapon_Model(NULL); Get_Weapon_Bag()->Force_Changed()
        setWeaponModel(null)
        weaponBag.forceChanged()

        super.importRare(packet)

        // C++: read definition_id; if changed, Re_Init
        val definitionId = packet.getInt()
        val currentDefId = definition?.id?.toInt() ?: 0
        if (definitionId != currentDefId) {
            val def = DefinitionMgrClass.findDefinition(definitionId)
            if (def != null && def.chunkId.toInt() == CLASSID_GAME_OBJECT_DEF_SOLDIER) {
                val soldierDef = def as? SoldierGameObjDef
                if (soldierDef != null) {
                    reInit(soldierDef)
                }
            }
        }
    }

    // C++: virtual void Export_Occasional(BitStreamClass&)
    override fun exportOccasional(packet: BitStream) {
        super.exportOccasional(packet)
        // C++: WeaponBag->Export_Weapon_List(packet)
        weaponBag.exportWeaponList(packet)
    }

    // C++: virtual void Import_Occasional(BitStreamClass&)
    override fun importOccasional(packet: BitStream) {
        super.importOccasional(packet)
        // C++: WeaponBag->Import_Weapon_List(packet)
        weaponBag.importWeaponList(packet)
    }

    // C++: virtual void Export_Frequent(BitStreamClass&)
    override fun exportFrequent(packet: BitStream) {
        // C++: bool in_vehicle = (Get_State() == IN_VEHICLE); packet.Add(in_vehicle)
        val inVehicle = (getState() == HumanStateClass.IN_VEHICLE)
        packet.addBool(inVehicle)
        if (inVehicle) {
            // C++: just do control info
            super.exportFrequent(packet)
            return
        }

        // C++: has_weapon + weapon id + rounds
        // C++: has_weapon = (WeaponBag && WeaponBag->Get_Index()) — non-null weapon means selected
        // Index 0 is null sentinel (no weapon); getWeapon() returns null when index 0 is selected.
        val p_weapon = getWeapon()
        val hasWeapon = (p_weapon != null)
        packet.addBool(hasWeapon)
        if (hasWeapon) {
            packet.addInt(p_weapon!!.definitionId)
            packet.addInt(p_weapon.getTotalRounds())
        }

        // C++: position — use the 'position' field directly (physObj may be null in tests)
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)

        // C++: human state + sub state
        packet.addInt(humanState.getState(), BITPACK_HUMAN_STATE)
        packet.addInt(humanState.getSubState(), BITPACK_HUMAN_SUB_STATE)

        // C++: velocity if airborne
        if (humanState.getState() == HumanStateClass.AIRBORNE) {
            val velocity = getVelocityVec()
            packet.addFloat(velocity.x)
            packet.addFloat(velocity.y)
            packet.addFloat(velocity.z)
        }

        // C++: animation name if TRANSITION, ANIMATION, or IN_VEHICLE
        if (getState() == HumanStateClass.TRANSITION ||
            getState() == HumanStateClass.ANIMATION ||
            getState() == HumanStateClass.IN_VEHICLE
        ) {
            packet.addTerminatedString(animationName)
        }

        // C++: is_special_damage + mode
        val isSpecialDamage = (specialDamageMode != ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE)
        packet.addBool(isSpecialDamage)
        if (isSpecialDamage) {
            packet.addInt(specialDamageMode)
        }

        super.exportFrequent(packet)
    }

    // C++: virtual void Import_Frequent(BitStreamClass&)
    override fun importFrequent(packet: BitStream) {
        // C++: bool in_vehicle = packet.Get(in_vehicle)
        val inVehicle = packet.getBool()
        if (inVehicle) {
            super.importFrequent(packet)
            return
        }

        // C++: has_weapon
        val hasWeapon = packet.getBool()
        if (hasWeapon) {
            val weaponId = packet.getInt()
            val rounds   = packet.getInt()
            if (getWeapon() == null || weaponId != getWeapon()!!.getId()) {
                weaponBag.selectWeaponId(weaponId)
            }
            val w = getWeapon()
            if (w != null && !w.isTriggered()) {
                w.setTotalRounds(rounds)
            }
        } else {
            if (getWeapon() != null) {
                weaponBag.deselect()
            }
        }

        // C++: position
        val scPosition = Vector3(
            packet.getFloat(BITPACK_WORLD_POSITION_X),
            packet.getFloat(BITPACK_WORLD_POSITION_Y),
            packet.getFloat(BITPACK_WORLD_POSITION_Z)
        )

        // C++: Bump Z up by half the resolution
        scPosition.z += BitPackResolution.getWorldPositionZHalfResolution()

        interpretScPositionData(scPosition)

        // C++: state and substate
        val hState = packet.getInt(BITPACK_HUMAN_STATE)
        val state = hState
        val subState = packet.getInt(BITPACK_HUMAN_SUB_STATE)

        // C++: velocity if airborne
        val velocity = Vector3(0f, 0f, 0f)
        if (state == HumanStateClass.AIRBORNE) {
            velocity.x = packet.getFloat()
            velocity.y = packet.getFloat()
            velocity.z = packet.getFloat()
        }

        if (humanState.isLocked()) {
            packet.flush()
            return
        }

        var transName = ""
        if (state == HumanStateClass.TRANSITION ||
            state == HumanStateClass.ANIMATION  ||
            state == HumanStateClass.IN_VEHICLE
        ) {
            transName = packet.getTerminatedString()
        }

        interpretScStateData(state, subState, transName, velocity, scPosition)

        // C++: special damage
        val isSpecialDamage = packet.getBool()
        var mode = ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE
        if (isSpecialDamage) {
            mode = packet.getInt()
        }
        if (mode != specialDamageMode) {
            setSpecialDamageMode(mode)
        }

        if (getState() == HumanStateClass.DIVE) {
            packet.flush()
        } else {
            super.importFrequent(packet)
        }
    }

    // C++: virtual void Import_State_Cs(BitStreamClass&)
    override fun importStateCs(packet: BitStream) {
        val isSniping = packet.getBool()
        if (isSniping != isSniping()) {
            humanState.toggleStateFlag(HumanStateClass.SNIPING_FLAG)
        }

        val checking = packet.getBool()
        if (checking) {
            val check = packet.getInt()
            if (check != computeCheck()) {
                // C++: Punish timer increase
                playerData?.let { (it as? PlayerDataClass)?.incPunishTimer(TimeManager.getFrameSeconds()) }
            }
        }

        super.importStateCs(packet)
    }

    // C++: virtual void Export_State_Cs(BitStreamClass&)
    override fun exportStateCs(packet: BitStream) {
        val isSniping = isSniping()
        packet.addBool(isSniping)

        val checking = control.getBoolean(ControlClass.BOOLEAN_ACTION)
        packet.addBool(checking)
        if (checking) {
            packet.addInt(computeCheck())
        }

        super.exportStateCs(packet)
    }

    // -------------------------------------------------------------------------
    // State helpers — proxy through HumanState
    // -------------------------------------------------------------------------

    // C++: HumanStateType Get_State() — proxy
    fun getState(): Int = humanState.getState()

    // C++: int Get_Sub_State() — proxy
    fun getSubState(): Int = humanState.getSubState()

    // C++: bool Is_Dead() — overrides DamageableGameObj.isDead (health-based) with state-based check
    override val isDead: Boolean get() = getState() == HumanStateClass.DEATH

    // C++: bool Is_Destroyed()
    fun isDestroyed(): Boolean = getState() == HumanStateClass.DESTROY

    // C++: bool Is_Upright()
    fun isUpright(): Boolean = getState() == HumanStateClass.UPRIGHT

    // C++: bool Is_Wounded()
    fun isWounded(): Boolean = getState() == HumanStateClass.WOUNDED

    // C++: bool In_Transition()
    fun inTransition(): Boolean = getState() == HumanStateClass.TRANSITION

    // C++: bool Is_Airborne()
    fun isAirborne(): Boolean = getState() == HumanStateClass.AIRBORNE

    // C++: bool Is_Crouched()
    fun isCrouched(): Boolean = humanState.getStateFlag(HumanStateClass.CROUCHED_FLAG)

    // C++: bool Is_Sniping()
    fun isSniping(): Boolean = humanState.getStateFlag(HumanStateClass.SNIPING_FLAG)

    // C++: bool Is_Slow()
    fun isSlow(): Boolean = (getSubState() and HumanStateClass.SUB_STATE_SLOW) != 0

    // C++: bool Is_On_Ladder()
    fun isOnLadder(): Boolean = getState() == HumanStateClass.LADDER

    // C++: bool Is_State_Locked()
    fun isStateLocked(): Boolean = humanState.isLocked()

    // C++: bool Is_In_Vehicle()
    fun isInVehicle(): Boolean = getState() == HumanStateClass.IN_VEHICLE

    // C++: const char* Get_State_Name()
    fun getStateName(): String = humanState.getStateName()

    // C++: void Reset_Loiter_Delay()
    fun resetLoiterDelay() = humanState.resetLoiterDelay()

    // C++: void Set_Loiters_Allowed(bool)
    fun setLoitersAllowed(allowed: Boolean) = humanState.setLoitersAllowed(allowed)

    // -------------------------------------------------------------------------
    // Physics
    // -------------------------------------------------------------------------

    // Real physics object from ccr.physics.moveable — null until wired by God at spawn time.
    // Alongside the stub physObj, this carries actual movement simulation.
    var realPhysObj: ccr.physics.moveable.HumanPhysClass? = null

    // Bridge: a stub HumanPhysClass whose member methods delegate to realPhysObj.
    // Extension functions on HumanPhysClass (setVelocity, setPosition, networkStateUpdate,
    // canTeleport(Matrix3D), getCollisionBox, getNormalizedSpeed, setNormalizedSpeed) are
    // already no-ops in the stubs and do not need bridging here.
    private val humanPhysBridge by lazy { object : HumanPhysClass() {
        // The `?: 0f` null branch is unreachable in practice: peekHumanPhys() only returns
        // this bridge when realPhysObj != null, so realPhysObj will always be non-null here.
        override fun getHeading(): Float = realPhysObj?.heading ?: 0f
        override fun setHeading(heading: Float) { realPhysObj?.heading = heading }
        override fun getVelocity(): Vector3 = realPhysObj?.velocity?.copy() ?: Vector3(0f, 0f, 0f)
        // setInContact, setLoitersAllowed, resetLoiterDelay, setLadderIndex, getLegMode,
        // isSubStateAdjustable, setDisabled, isEngineEnabled, enableEngine, canTeleport(Vector3),
        // findTeleportLocation — all remain as no-ops / TODO stubs in the base class for now
    } }

    // Syncs position and facing from the real physics object back to the game-object fields
    // used by exportFrequent. Called at the start of postThink().
    private fun syncPositionFromPhys() {
        realPhysObj?.let { rp ->
            position = rp.position
            facing = rp.heading
        }
    }

    // C++: HumanPhysClass* Peek_Human_Phys() const
    // Returns the bridge when realPhysObj is wired; returns null otherwise (same as before).
    // Not an `override`: CombatTypeStubs.kt defines peekHumanPhys() as an extension function on
    // PhysicalGameObj. Kotlin extension functions cannot be overridden as members — this member
    // declaration on SoldierGameObj simply shadows the extension when called on SoldierGameObj
    // instances, which is the desired behaviour.
    fun peekHumanPhys(): HumanPhysClass? = if (realPhysObj != null) humanPhysBridge else peekPhysicalObject()?.asHumanPhysClass()

    // -------------------------------------------------------------------------
    // Object Motion
    // -------------------------------------------------------------------------

    // C++: virtual float Get_Max_Speed()
    override fun getMaxSpeed(): Float = peekHumanPhys()?.getNormalizedSpeed() ?: super.getMaxSpeed()

    // C++: virtual void Set_Max_Speed(float)
    open fun setMaxSpeed(speed: Float) { peekHumanPhys()?.setNormalizedSpeed(speed) }

    // C++: virtual float Get_Turn_Rate()
    override fun getTurnRate(): Float = getSoldierDefinition().turnRate

    // -------------------------------------------------------------------------
    // Weapons
    // -------------------------------------------------------------------------

    // C++: virtual bool Set_Targeting(const Vector3&, bool)
    override fun setTargeting(pos: Vector3, doTilt: Boolean): Boolean {
        return if (facingObject.get() == null) {
            internalSetTargeting(pos, doTilt)
        } else {
            false
        }
    }

    // C++: bool Internal_Set_Targeting(const Vector3&, bool)
    protected fun internalSetTargeting(targetPos: Vector3, doTilt: Boolean): Boolean {
        if (CombatManager.isSkeletonSliderDemoEnabled()) return false

        super.setTargeting(targetPos, doTilt)

        if (getState() == HumanStateClass.DEATH     ||
            getState() == HumanStateClass.DESTROY   ||
            getState() == HumanStateClass.TRANSITION ||
            getState() == HumanStateClass.LADDER
        ) {
            return false
        }

        if (getState() == HumanStateClass.IN_VEHICLE) {
            vehicle?.let { v ->
                if (v.driverIsGunner) {
                    if (v.getDriver() === this) v.setTargeting(targetPos)
                } else {
                    if (v.getDriver() === this && v.getGunner() == null) v.setTargeting(targetPos)
                    else if (v.getGunner() === this) v.setTargeting(targetPos)
                }
            }
            return false
        }

        val muzzlePos = getPosition().also { it.z += getWeaponHeight() }
        val relTarget = targetPos - muzzlePos

        val dist = relTarget.length()
        var tilt = 0f
        if (dist != 0f && doTilt) {
            tilt = WWMath.fastAsin(relTarget.z / dist)
        }

        var isComplete = true

        val curFacing = peekHumanPhys()!!.getHeading()
        val facing = WWMath.atan2(relTarget.y, relTarget.x)
        var facingDif = facing - curFacing
        if (kotlin.math.abs(facingDif) > 0.001f) {
            facingDif = WWMath.wrap(facingDif, DEG_TO_RADF_NEG_180, DEG_TO_RADF_POS_180)
            var change = facingDif
            if (!isHumanControlled()) {
                var maxChange = getTurnRate() * TimeManager.getFrameSeconds()
                if (kotlin.math.abs(change) < DEG_20_RAD) maxChange *= 0.3f
                change = change.coerceIn(-maxChange, maxChange)
            }
            if (!isHumanControlled()) {
                humanState.setTurnVelocity(change)
            }
            isComplete = (change == facingDif)
            val newFacing = WWMath.wrap(curFacing + change, DEG_TO_RADF_NEG_180, DEG_TO_RADF_POS_180)
            peekHumanPhys()!!.setHeading(newFacing)
        }

        if (isHumanControlled() && getState() != HumanStateClass.IN_VEHICLE) {
            val direction = if (getWeapon()?.isReloading() == true) 1f else -1f
            reloadingTilt += direction * TimeManager.getFrameSeconds() * TILT_DOWN_SPEED
            reloadingTilt = reloadingTilt.coerceIn(0f, 1f)
            if (reloadingTilt > 0f) {
                tilt = WWMath.lerp(tilt, DEG_TO_RADF_NEG_90, reloadingTilt)
            }
        }

        humanState.updateAiming(tilt, 0f)

        return isComplete
    }

    // C++: float Get_Weapon_Height()
    fun getWeaponHeight(): Float = if (isCrouched()) 1.62f - 0.56f else 1.62f

    // C++: float Get_Weapon_Length()
    fun getWeaponLength(): Float = 0.8f

    // C++: virtual const Matrix3D& Get_Muzzle(int)
    override fun getMuzzle(index: Int): Matrix3D {
        // FIXME: WeaponRenderModel bone transform — requires full model/bone system
        return getTransform()
    }

    // C++: bool Detonate_C4() { return DetonateC4; }
    fun detonateC4(): Boolean = detonateC4

    // C++: void Give_All_Weapons()
    fun giveAllWeapons() {
        var weaponDef = DefinitionMgrClass.getFirst(CLASSID_DEF_WEAPON) as? WeaponDefinitionClass
        while (weaponDef != null) {
            if (weaponDef.aGiveWeaponsWeapon) {
                weaponBag.addWeapon(weaponDef.id.toInt(), -1)
            }
            weaponDef = DefinitionMgrClass.getNext(weaponDef, CLASSID_DEF_WEAPON) as? WeaponDefinitionClass
        }
    }

    // -------------------------------------------------------------------------
    // Display / Animation
    // -------------------------------------------------------------------------

    // C++: virtual void Set_Weapon_Model(const char*)
    open fun setWeaponModel(modelName: String?) {
        // C++: remove old gun model
        weaponRenderModel?.let {
            peekModel()?.removeSubObject(it)
            weaponRenderModel = null
        }

        if (!modelName.isNullOrEmpty()) {
            val strippedName = getRenderObjNameFromFilename(modelName)
            var robj = findRenderObj(strippedName)
            if (robj == null) {
                robj = createRenderObjFromFilename(modelName)
                if (robj != null) {
                    addRenderObj(robj)
                }
            }
            if (robj != null) {
                weaponRenderModel = robj
                peekModel()?.let { model ->
                    if (robj.getContainer() !== model) {
                        model.addSubObjectToBone(robj, GUN_BONE_NAME)
                    }
                }
                getWeapon()?.setModel(robj)
            }
        }
    }

    // C++: virtual void Set_Weapon_Animation(const char*)
    open fun setWeaponAnimation(animName: String?) {
        if (weaponAnimControl == null) {
            weaponAnimControl = SimpleAnimControlClass()
            weaponRenderModel?.let { weaponAnimControl!!.setModel(it) }
        }
        if (animName != null && animName.isNotEmpty()) {
            weaponAnimControl!!.setAnimation(animName)
        }
    }

    // C++: virtual void Set_Animation(const char*, bool, float)
    override fun setAnimation(animationName: String?, looping: Boolean, frameOffset: Float) {
        if (animationName == null) {
            humanState.stopScriptedAnimation()
            return
        }
        this.animationName = animationName
        humanState.startScriptedAnimation(animationName, false, looping)
    }

    // C++: virtual void Set_Blended_Animation(const char*, bool, float, bool)
    open fun setBlendedAnimation(
        animationName: String?,
        looping: Boolean = true,
        frameOffset: Float = 0f,
        playBackwards: Boolean = false
    ) {
        if (animationName == null) {
            humanState.stopScriptedAnimation()
            return
        }
        this.animationName = animationName
        humanState.startScriptedAnimation(animationName, true, looping)
        animControl?.setMode(if (looping) ANIM_MODE_LOOP else ANIM_MODE_ONCE, frameOffset)
        if (playBackwards) {
            val anim = animControl?.peekAnimation()
            if (anim != null) {
                val frameCount = anim.getNumFrames()
                animControl?.setMode(ANIM_MODE_TARGET, frameCount - 1f)
                animControl?.setTargetFrame(0)
            }
        }
    }

    // C++: virtual void Start_Transition_Animation(const char*, TransitionCompletionDataStruct*)
    open fun startTransitionAnimation(animName: String, data: TransitionCompletionDataStruct?) {
        transitionCompletionData = data
        animationName = animName
        humanState.startTransitionAnimation(animName, false)
    }

    // C++: void Set_Model(const char*)
    open fun setModel(modelName: String) {
        peekPhysicalObject()?.setModelByName(modelName)
        humanState.setAnimControl(animControl as? HumanAnimControlClass)
    }

    // -------------------------------------------------------------------------
    // Vehicles
    // -------------------------------------------------------------------------

    // C++: void Enter_Vehicle(VehicleGameObj*, const char*)
    fun enterVehicle(vehicle: VehicleGameObj, animName: String) {
        this.vehicle = vehicle
        humanState.setState(HumanStateClass.IN_VEHICLE)
        animationName = animName
        humanState.forceAnimation(animName, false)
    }

    // C++: void Exit_Vehicle()
    fun exitVehicle() {
        vehicle = null
        humanState.setState(HumanStateClass.UPRIGHT)
    }

    // C++: void Exit_Destroyed_Vehicle(int, const Vector3&)
    fun exitDestroyedVehicle(seatNum: Int, vehiclePos: Vector3) {
        vehicle = null
        humanState.setState(HumanStateClass.UPRIGHT)

        val extent = peekHumanPhys()?.getCollisionBox()?.extent ?: Vector3(0.3f, 0.3f, 1f)
        val width = maxOf(extent.x, extent.y) * 1.1f

        val offsets = arrayOf(
            Vector3(-1f, -1f, 0f),
            Vector3(-1f,  1f, 0f),
            Vector3( 1f, -1f, 0f),
            Vector3( 1f,  1f, 0f)
        )

        peekModel()?.setHidden(false)

        val pos = vehiclePos + offsets[seatNum and 3] * width
        val tm = Matrix3D.translation(pos)
        val hp = peekHumanPhys()
        if (hp != null) {
            if (hp.canTeleport(tm)) {
                hp.setPosition(pos)
            } else {
                val newPos = Vector3(pos.x, pos.y, pos.z)
                hp.findTeleportLocation(pos, 4f, newPos)
                hp.setPosition(newPos)
            }
        }
    }

    // C++: bool Is_Permitted_To_Enter_Vehicle()
    fun isPermittedToEnterVehicle(): Boolean = CombatManager.isGameplayPermitted()

    // C++: VehicleGameObj* Get_Profile_Vehicle()
    fun getProfileVehicle(): VehicleGameObj? {
        if (getState() == HumanStateClass.IN_VEHICLE && vehicle != null) return vehicle
        if (getState() == HumanStateClass.TRANSITION && transitionCompletionData != null) {
            if (transitionCompletionData!!.type == TransitionDataClass.VEHICLE_ENTER) {
                return transitionCompletionData!!.vehicle.get() as? VehicleGameObj
            }
        }
        return null
    }

    // C++: bool Use_Ladder_View()
    fun useLadderView(): Boolean {
        if (getState() == HumanStateClass.LADDER) return true
        if (getState() == HumanStateClass.TRANSITION && transitionCompletionData != null) {
            val type = transitionCompletionData!!.type
            if (type == TransitionDataClass.LADDER_ENTER_TOP || type == TransitionDataClass.LADDER_ENTER_BOTTOM) {
                return true
            }
        }
        return false
    }

    // -------------------------------------------------------------------------
    // Ladders
    // -------------------------------------------------------------------------

    // C++: void Exit_Ladder()
    fun exitLadder() { humanState.setState(HumanStateClass.UPRIGHT) }

    // C++: void Enter_Ladder(bool)
    fun enterLadder(top: Boolean) {
        humanState.setState(HumanStateClass.LADDER)
        if (isHumanControlled()) {
            if (top) ladderUpMask = true
            else     ladderDownMask = true
        }
    }

    // -------------------------------------------------------------------------
    // Velocity
    // -------------------------------------------------------------------------

    // C++: virtual void Get_Velocity(Vector3& vel)
    override fun getVelocity(): Vector3 = peekHumanPhys()?.getVelocity() ?: Vector3(0f, 0f, 0f)

    // Non-override alias (returns by value like C++)
    fun getVelocityVec(): Vector3 = getVelocity()

    // C++: void Set_Velocity(Vector3&)
    fun setVelocity(vel: Vector3) { peekHumanPhys()?.setVelocity(vel) }

    // -------------------------------------------------------------------------
    // Bullseye
    // -------------------------------------------------------------------------

    // C++: virtual Vector3 Get_Bullseye_Position()
    override fun getBullseyePosition(): Vector3 {
        vehicle?.let { return it.getBullseyePosition() }
        val pos = getPosition()
        pos.z += if (isCrouched()) 0.5f else getBullseyeOffsetZ()
        return pos
    }

    // -------------------------------------------------------------------------
    // Visibility
    // -------------------------------------------------------------------------

    // C++: virtual bool Is_Visible() { return IsVisible; }
    override fun isVisible(): Boolean = isVisible

    // C++: void Set_Is_Visible(bool)
    fun setIsVisible(visible: Boolean) { isVisible = visible }

    // -------------------------------------------------------------------------
    // Targetable
    // -------------------------------------------------------------------------

    // C++: virtual bool Is_Targetable() const
    override fun isTargetable(): Boolean = (!isInVehicle()) && super.isTargetable()

    // -------------------------------------------------------------------------
    // Turret
    // -------------------------------------------------------------------------

    // C++: virtual bool Is_Turreted() { return false; }
    open fun isTurreted(): Boolean = false

    // -------------------------------------------------------------------------
    // Head Look
    // -------------------------------------------------------------------------

    // C++: void Look_At(const Vector3&, float)
    fun lookAt(pos: Vector3, time: Float) {
        headLookTarget = pos
        headLookDuration = time
        headLookAngle = Vector3(0f, 0f, 0f)
    }

    // C++: void Update_Look_At(const Vector3&)
    fun updateLookAt(pos: Vector3) { headLookTarget = pos }

    // C++: void Cancel_Look_At()
    fun cancelLookAt() { headLookDuration = 0.001f }

    // C++: void Look_Random(float)
    fun lookRandom(time: Float) {
        if (time == 0f && headLookDuration != 0f) {
            headLookDuration = 0.0001f
        } else {
            headLookDuration = time
        }
        headLookAngleTimer = 0f
        headLookAngle = Vector3(1f, 1f, 1f)
    }

    // C++: bool Is_Looking() { return (HeadLookDuration > 0); }
    fun isLooking(): Boolean = headLookDuration > 0f

    // C++: virtual Matrix3D Get_Look_Transform()
    override fun getLookTransform(): Matrix3D {
        // FIXME: head_bone from model — requires model/bone system
        return getTransform()
    }

    // C++: void Lock_Facing(PhysicalGameObj*, bool)
    fun lockFacing(gameObj: PhysicalGameObj?, turnBody: Boolean) {
        facingObject.set(gameObj)
        facingAllowBodyTurn = turnBody
        if (gameObj == null) {
            cancelLookAt()
        }
    }

    // -------------------------------------------------------------------------
    // Innate AI / observer
    // -------------------------------------------------------------------------

    // C++: void Innate_Enable(int)
    fun innateEnable(bits: Int = 0xFFFFFFFF.toInt()) { innateEnableBits = innateEnableBits or bits }

    // C++: void Innate_Disable(int)
    fun innateDisable(bits: Int = 0xFFFFFFFF.toInt()) { innateEnableBits = innateEnableBits and bits.inv() }

    // C++: bool Is_Innate_Enabled(int)
    fun isInnateEnabled(bits: Int = 0xFFFFFFFF.toInt()): Boolean = (innateEnableBits and bits) != 0

    // C++: SoldierObserverClass* Get_Innate_Controller()
    fun getInnateController(): SoldierObserverClass? {
        if (!getSoldierDefinition().useInnateBehavior) return null
        return getObservers().firstOrNull { it.getName() == "Innate Soldier" } as? SoldierObserverClass
    }

    // C++: SoldierAIState Get_AI_State() const { return AIState; }
    // covered by var aiState

    // C++: void Set_AI_State(SoldierAIState)
    fun setAiState(state: Int) {
        if (aiState != state) {
            // C++: stop interruptible conversation if moving to higher alert state
            if (state >= AI_STATE_SEARCH && state > aiState) {
                activeConversation?.let {
                    if (it.isInterruptable()) {
                        it.stopConversation(ACTION_COMPLETE_CONVERSATION_INTERRUPTED)
                    }
                }
            }
            aiState = state
        }
    }

    // C++: void Clear_Innate_Observer()
    fun clearInnateObserver() { innateObserver = null }

    // -------------------------------------------------------------------------
    // Key ring
    // -------------------------------------------------------------------------

    // C++: void Give_Key(int)
    fun giveKey(keyNumber: Int) { keyRing = keyRing or (1 shl keyNumber) }

    // C++: void Remove_Key(int)
    fun removeKey(keyNumber: Int) { keyRing = keyRing and (1 shl keyNumber).inv() }

    // C++: bool Has_Key(int)
    fun hasKey(keyNumber: Int): Boolean = (keyRing and (1 shl keyNumber)) != 0

    // -------------------------------------------------------------------------
    // Powerups / damage state locking
    // -------------------------------------------------------------------------

    // C++: virtual bool Wants_Powerups() { return Is_Human_Controlled(); }
    open fun wantsPowerups(): Boolean = isHumanControlled()

    // C++: virtual bool Allow_Special_Damage_State_Lock() { return !Is_Human_Controlled(); }
    open fun allowSpecialDamageStateLock(): Boolean = !isHumanControlled()

    // -------------------------------------------------------------------------
    // Type identification
    // -------------------------------------------------------------------------

    // C++: virtual SoldierGameObj* As_SoldierGameObj() { return this; }
    open fun asSoldierGameObj(): SoldierGameObj = this

    // -------------------------------------------------------------------------
    // Stealth
    // -------------------------------------------------------------------------

    // C++: virtual float Get_Stealth_Fade_Distance() const
    override fun getStealthFadeDistance(): Float =
        GlobalSettingsDef.getGlobalSettings().getMpStealthDistanceHuman()

    // -------------------------------------------------------------------------
    // Ghost collision
    // -------------------------------------------------------------------------

    // C++: void Enable_Ghost_Collision(bool)
    fun enableGhostCollision(onoff: Boolean) {
        val isGhost = (peekPhysicalObject()?.getCollisionGroup() == SOLDIER_GHOST_COLLISION_GROUP)
        if (onoff == isGhost) return
        peekPhysicalObject()?.setCollisionGroup(
            if (onoff) SOLDIER_GHOST_COLLISION_GROUP else SOLDIER_COLLISION_GROUP
        )
    }

    // C++: bool Is_Safe_To_Disable_Ghost_Collision(const Vector3&)
    fun isSafeToDisableGhostCollision(currPos: Vector3): Boolean {
        val boxPos  = currPos + Vector3(0f, 0f, HUMAN_HALF_HEIGHT)
        val box     = AABoxClass(boxPos, PERSONAL_SPACE_BOX_SIZE)
        val objList = NonRefPhysListClass()
        PhysicsSceneClass.getInstance().collectObjects(box, false, true, objList)

        for (physObj in objList) {
            val gameObj = physObj.asHumanPhysClass()?.getObserver()?.let {
                (it as? CombatPhysObserverClass)?.asPhysicalGameObj()
            }
            if (gameObj != null && gameObj !== this) {
                val soldierObj = gameObj.asSoldierGameObj()
                if (soldierObj != null && !soldierObj.isDestroyed()) {
                    val blockPos = gameObj.getPosition()
                    val blockBox = AABoxClass(blockPos + Vector3(0f, 0f, 1f), Vector3(0.3f, 0.3f, 1f))
                    if (CollisionMath.overlapTest(box, blockBox) != CollisionMath.OUTSIDE) {
                        return false
                    }
                }
            }
        }
        return true
    }

    // C++: bool Is_Soldier_Blocked(const Vector3&)
    fun isSoldierBlocked(currPos: Vector3): Boolean {
        if ((getAction() != null && getAction()!!.isBusy()) || isDestroyed()) return false

        val boxPos  = currPos + Vector3(0f, 0f, HUMAN_HALF_HEIGHT)
        val box     = AABoxClass(boxPos, PERSONAL_SPACE_BOX_SIZE)
        val objList = NonRefPhysListClass()
        PhysicsSceneClass.getInstance().collectObjects(box, false, true, objList)

        val myId      = getId()
        var smallestId = myId

        for (physObj in objList) {
            val gameObj = physObj.asHumanPhysClass()?.getObserver()?.let {
                (it as? CombatPhysObserverClass)?.asPhysicalGameObj()
            }
            if (gameObj != null && gameObj !== this) {
                val soldierObj = gameObj.asSoldierGameObj()
                if (soldierObj != null && !soldierObj.isDestroyed()) {
                    val blockPos = gameObj.getPosition()
                    val blockBox = AABoxClass(blockPos + Vector3(0f, 0f, 1f), Vector3(0.3f, 0.3f, 1f))
                    if (CollisionMath.overlapTest(box, blockBox) != CollisionMath.OUTSIDE) {
                        val smartObj = gameObj.asSmartGameObj()
                        if (smartObj != null && !smartObj.isHumanControlled()) {
                            val action = smartObj.getAction()
                            val destPos = action?.getParameters()?.moveLocation ?: blockPos
                            val distance = (blockPos - destPos).length()
                            if (distance > (action?.getParameters()?.moveArrivedDistance ?: 0f)) {
                                val currId = gameObj.getId()
                                if (currId < smallestId) {
                                    smallestId = currId
                                }
                            }
                        }
                    }
                }
            }
        }
        return smallestId != myId
    }

    // -------------------------------------------------------------------------
    // Visibility (Can_See)
    // -------------------------------------------------------------------------

    // C++: bool Can_See(SoldierGameObj*)
    fun canSee(pSoldier: SoldierGameObj): Boolean {
        val rayStart = getBullseyePosition()
        val rayEnd   = pSoldier.getBullseyePosition()
        val path = rayEnd - rayStart
        var adjustedStart = rayStart
        if (path.length() > 1f) {
            val offset = path.normalized()
            adjustedStart = rayStart + offset
        }

        val ray = LineSegClass(adjustedStart, rayEnd)
        val result = CastResultStruct()
        val raytest = PhysRayCollisionTestClass(ray, result, BULLET_COLLISION_GROUP, COLLISION_TYPE_PHYSICAL)
        CombatManager.getScene()?.castRay(raytest)

        if (raytest.collidedPhysObj != null) {
            val observer = raytest.collidedPhysObj!!.getObserver()
            val blocker = if (observer != null) {
                (observer as? CombatPhysObserverClass)?.asPhysicalGameObj()?.asSmartGameObj()
            } else null
            if (blocker != null && blocker.controlOwner == pSoldier.controlOwner) {
                return true
            }
        }
        return false
    }

    // -------------------------------------------------------------------------
    // Skeleton
    // -------------------------------------------------------------------------

    // C++: void Adjust_Skeleton(float, float)
    fun adjustSkeleton(height: Float, width: Float) {
        // C++: only adjust male skeletons (HTree name[2] == 'A')
        val robj = peekModel() as? Animatable3DObjClass ?: return
        val htree = robj.getHTree() ?: return
        if (htree.getName().getOrElse(2) { ' ' } != 'A') return
        // FIXME: WW3DAssetManager::Get_HTree interpolation — requires full asset manager
    }

    // -------------------------------------------------------------------------
    // Fly mode
    // -------------------------------------------------------------------------

    // C++: void Toggle_Fly_Mode()
    fun toggleFlyMode() {
        inFlyMode = !inFlyMode
        if (inFlyMode) {
            humanState.setState(HumanStateClass.DEBUG_FLY)
            peekPhysicalObject()?.setCollisionGroup(UNCOLLIDEABLE_GROUP)
        } else {
            humanState.setState(HumanStateClass.UPRIGHT)
            peekPhysicalObject()?.setCollisionGroup(SOLDIER_COLLISION_GROUP)
        }
    }

    // -------------------------------------------------------------------------
    // Speech / dialogue
    // -------------------------------------------------------------------------

    // C++: void Prepare_Speech_Framework()
    fun prepareSpeechFramework() {
        if (speechAnim == null) {
            headModel = findHeadModel()
            if (headModel != null) {
                val htree = headModel!!.getHTree()
                if (htree != null) {
                    val skeletonName = htree.getName()
                    speechAnim = DynamicSpeechAnimClass(skeletonName)
                    generateIdleFacialAnimTimer = 0f
                }
            }
        }
    }

    // C++: void Say_Dialogue(int)
    fun sayDialogue(dialogId: Int) {
        val dlg = dialogList.getOrNull(dialogId) ?: return
        val conversationId = dlg.getConversation()
        if (conversationId > 0) {
            val conversation = ConversationMgrClass.startConversation(this, conversationId, true)
            // conversation released immediately (ref counted in C++)
        }
    }

    // C++: static float Say_Dynamic_Dialogue(int, SoldierGameObj*, AudibleSoundClass**)
    // Note: static in C++ → companion object function in Kotlin
    // FIXME: full implementation requires TranslateDBClass and WWAudioClass — partial stub
    fun sayDynamicDialogue(textId: Int, speaker: SoldierGameObj? = null): Float {
        val translateObj = TranslateDBClass.findObject(textId) ?: return 2.0f
        var duration = 2.0f
        val soundDefId = translateObj.getSoundId()
        speaker?.stopCurrentSpeech()
        if (soundDefId > 0) {
            val speech = WWAudioClass.getInstance().createSound(soundDefId)
            if (speech != null) {
                duration = speech.getDuration() / 1000.0f
                if (speaker != null) {
                    speech.setTransform(speaker.getTransform())
                    speech.addToScene()
                    speaker.currentSpeech = speech
                } else {
                    speech.play()
                }
            }
        }
        speaker?.generaIdleFacialAnimTimer(duration + 1.0f)
        return duration
    }

    // C++: void Stop_Current_Speech()
    fun stopCurrentSpeech() {
        currentSpeech?.let {
            generateIdleFacialAnimTimer = 0f
            it.stop()
            it.removeFromScene()
            currentSpeech = null
        }
    }

    // C++: RenderObjClass* Find_Head_Model()
    protected fun findHeadModel(): RenderObjClass? {
        // FIXME: full HLOD head model search — requires model/bone system
        return null
    }

    // C++: void Set_Emot_Icon(const char*, float)
    fun setEmotIcon(modelName: String?, duration: Float) {
        emotIconModel?.let {
            if (it.isInScene()) it.remove()
            emotIconModel = null
        }
        if (modelName != null) {
            val newModel = createRenderObjFromFilename(modelName)
            if (newModel != null) {
                val name = newModel.getName()
                val animName = "$name.$name"
                // FIXME: WW3DAssetManager anim lookup — requires asset manager
                val tm = getTransform()
                tm.setTranslation(tm.getTranslation() + Vector3(0f, 0f, EMOT_ICON_HEIGHT))
                newModel.setTransform(tm)
                CombatManager.getScene()?.addRenderObject(newModel)
                emotIconModel = newModel
                emotIconTimer = duration
            }
        }
    }

    // C++: DynamicSpeechAnimClass* Get_Facial_Anim() { return SpeechAnim; }
    fun getFacialAnim(): DynamicSpeechAnimClass? = speechAnim

    // -------------------------------------------------------------------------
    // First person hands
    // -------------------------------------------------------------------------

    // C++: const char* Get_First_Person_Hands_Model_Name()
    fun getFirstPersonHandsModelName(): String = getSoldierDefinition().firstPersonHands

    // -------------------------------------------------------------------------
    // Special damage
    // -------------------------------------------------------------------------

    // C++: void Set_Special_Damage_Mode(SpecialDamageType, ArmedGameObj*)
    fun setSpecialDamageMode(mode: Int, damager: ArmedGameObj? = null) {
        if (mode == ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE) {
            // C++: remove emitters, release effect, reset state
            specialDamageEffect?.let {
                it.setTargetParameter(0f)
                it.enableRemoveOnComplete(true)
                specialDamageEffect = null
            }
            if (!isHumanControlled()) {
                val s = humanState.getState()
                if (s >= HumanStateClass.ON_FIRE && s <= HumanStateClass.ON_CNC_CHEM) {
                    humanState.setState(HumanStateClass.UPRIGHT)
                }
            }
            specialDamageDamager.set(null)
        } else {
            if (mode != specialDamageMode) {
                setSpecialDamageMode(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE)

                if (allowSpecialDamageStateLock() && mode != ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_SUPER_FIRE) {
                    humanState.setState(
                        mode - ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE + HumanStateClass.ON_FIRE
                    )
                }

                // C++: Add electric effect
                if (mode == ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_ELECTRIC) {
                    specialDamageEffect = CombatMaterialEffectManager.getElectrocutionEffect()
                    specialDamageEffect?.let {
                        it.setTargetParameter(0.49f)
                        peekHumanPhys()?.addEffectToMe(it)
                    }
                }

                specialDamageDamager.set(damager)
            }
        }

        specialDamageMode  = mode
        specialDamageTimer = ArmorWarheadManager.getSpecialDamageDuration(specialDamageMode)
    }

    // -------------------------------------------------------------------------
    // Perturb
    // -------------------------------------------------------------------------

    // C++: void Perturb_Position(float)
    fun perturbPosition(maxPerturb: Float = 5f) {
        val initialPos = getPosition()
        val newPos = Vector3(initialPos.x, initialPos.y, initialPos.z)
        val succeeded = peekHumanPhys()?.findTeleportLocation(initialPos, maxPerturb, newPos) ?: false
        if (succeeded) {
            setPosition(newPos)
        }
    }

    // -------------------------------------------------------------------------
    // Elevator / vis
    // -------------------------------------------------------------------------

    // C++: bool Is_In_Elevator()
    fun isInElevator(): Boolean {
        val hp = peekHumanPhys() ?: return false
        return hp.peekCarrierObject()?.asElevatorPhysClass() != null
    }

    // C++: int Tally_Vis_Visible_Soldiers()
    fun tallyVisVisibleSoldiers(): Int {
        val position = getPosition() + Vector3(0f, 0f, 1.5f)
        val pvs = CombatManager.getScene()?.getVisTable(position) ?: return -1
        var count = 0
        for (smartObj in GameObjManager.getSmartGameObjs()) {
            val soldier = smartObj.asSoldierGameObj() ?: continue
            val physObj = soldier.peekPhysicalObject() ?: continue
            if (pvs.getBit(physObj.getVisObjectId())) count++
        }
        return count
    }

    // -------------------------------------------------------------------------
    // Network position interpretation
    // -------------------------------------------------------------------------

    // C++: void Interpret_Sc_Position_Data(const Vector3&)
    fun interpretScPositionData(scPosition: Vector3) {
        if (getState() == HumanStateClass.TRANSITION) {
            peekHumanPhys()?.networkStateUpdate(scPosition, Vector3(0f, 0f, 0f))
        } else {
            var position = scPosition
            if (isInElevator()) {
                position = Vector3(scPosition.x, scPosition.y, getPosition().z)
            }
            peekHumanPhys()?.networkStateUpdate(position, Vector3(0f, 0f, 0f))
        }
    }

    // C++: void Interpret_Sc_State_Data(...)
    fun interpretScStateData(
        state: Int,
        subState: Int,
        transName: String,
        velocity: Vector3,
        scPosition: Vector3
    ) {
        if (getState() != state || getSubState() != subState) {
            val skip1 = (getState() == HumanStateClass.TRANSITION && state == HumanStateClass.TRANSITION_COMPLETE)
            val skip2 = (getState() == HumanStateClass.UPRIGHT && state == HumanStateClass.LAND)
            if (!skip1 && !skip2) {
                when (state) {
                    HumanStateClass.TRANSITION -> {
                        startTransitionAnimation(transName, null)
                        animationName = transName
                    }
                    HumanStateClass.ANIMATION -> {
                        animationName = transName
                        humanState.startScriptedAnimation(animationName, true, false)
                    }
                    HumanStateClass.IN_VEHICLE -> {
                        humanState.forceAnimation(transName, false)
                        animationName = transName
                    }
                }
                humanState.setState(state, subState)
            }
        }

        if (state == HumanStateClass.AIRBORNE) {
            peekHumanPhys()?.setInContact(false)
            peekHumanPhys()?.setVelocity(velocity)
            peekHumanPhys()?.setPosition(scPosition)
        }
    }

    // -------------------------------------------------------------------------
    // Information / description
    // -------------------------------------------------------------------------

    // C++: virtual void Get_Information(StringClass&)
    override fun getInformation(): String {
        val sb = StringBuilder(super.getInformation())
        humanState.getInformation(sb)
        val innate = getInnateController()
        if (innate != null) {
            innate.getInformation(sb)
            val disabled = StringBuilder()
            if (!isInnateEnabled(SOLDIER_INNATE_EVENT_BULLET_HEARD))   disabled.append("B")
            if (!isInnateEnabled(SOLDIER_INNATE_EVENT_GUNSHOT_HEARD))  disabled.append("G")
            if (!isInnateEnabled(SOLDIER_INNATE_EVENT_FOOTSTEP_HEARD)) disabled.append("F")
            if (!isInnateEnabled(SOLDIER_INNATE_EVENT_ENEMY_SEEN))     disabled.append("S")
            if (!isInnateEnabled(SOLDIER_INNATE_ACTIONS))              disabled.append("A")
            if (disabled.isNotEmpty()) {
                sb.append("Disabled:$disabled\n")
            }
        }
        return sb.toString()
    }

    // C++: virtual void Get_Description(StringClass&)
    override fun getDescription(): String {
        val sb = StringBuilder()
        sb.append("ID:    ${getId()}\n")
        sb.append("NAME:  ${definition?.name ?: ""}\n")
        sb.append("TEAM:  ${playerType}\n")
        sb.append("CONTR: ${controlOwner}\n")
        val pos = getPosition()
        sb.append("POS:   ${pos.x}, ${pos.y}, ${pos.z}\n")
        val tgt = targeting
        sb.append("TGT:   ${tgt.x}, ${tgt.y}, ${tgt.z}\n")
        val vel = getVelocity()
        sb.append("VEL:   ${vel.x}, ${vel.y}, ${vel.z}\n")
        val w = getWeapon()
        if (w != null) {
            sb.append("WEAP:  ${w.getName()}\n")
            sb.append("TRNDS: ${w.getTotalRounds()}\n")
        }
        sb.append("HLTH:  ${defenseObject.health}\n")
        sb.append("HMAX:  ${defenseObject.healthMax}\n")
        sb.append("STATE: ${getStateName()}\n")
        sb.append("ANIM:  ${animationName}\n")
        vehicle?.let { sb.append("VEH:   ${it.getId()}\n") }
        sb.append("#OBSV: ${getObservers().size}\n")
        for (obs in getObservers()) sb.append("       ${obs.getName()}\n")
        sb.append("INNAT: ${isInnateEnabled()}\n")
        sb.append("HIB:   ${isHibernating()}\n")
        sb.append("stlth: ${isStealthEnabled()}\n")
        sb.append("   on: ${isStealthed()}\n")
        return sb.toString()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    // C++: void Handle_Legs()
    private fun handleLegs() {
        val doSteps = if (isOnLadder()) {
            control.getAnalog(ControlClass.ANALOG_MOVE_FORWARD) != 0f
        } else {
            control.getAnalog(ControlClass.ANALOG_MOVE_FORWARD) != 0f ||
            control.getAnalog(ControlClass.ANALOG_MOVE_LEFT)    != 0f
        }

        if (!isSniping() && !inFlyMode && CombatManager.isGameplayPermitted() && doSteps && controlEnabled) {
            val legMode = humanState.getLegMode()
            if (legMode != lastLegMode) {
                val myType = when {
                    isCrouched() -> SurfaceEffectsManager.HITTER_TYPE_FOOTSTEP_CROUCHED
                    isSlow()     -> SurfaceEffectsManager.HITTER_TYPE_FOOTSTEP_WALK
                    else         -> SurfaceEffectsManager.HITTER_TYPE_FOOTSTEP_RUN
                }
                val groundType = if (isOnLadder()) {
                    SURFACE_TYPE_LIGHT_METAL
                } else {
                    peekHumanPhys()?.getContactSurfaceType() ?: 0
                }
                val tm = getTransform()
                if (control.getAnalog(ControlClass.ANALOG_MOVE_FORWARD) != 0f) {
                    tm.rotateZ(DEG_TO_RADF_NEG_90)
                }
                val offset = if (legMode) -0.15f else 0.1f
                tm.translate(Vector3(offset, 0.3f, 0f))
                val allowEmitters = !isStealthed()
                SurfaceEffectsManager.applyEffect(groundType, myType, tm, null, this, true, allowEmitters)
                lastLegMode = legMode
            }
        }
    }

    // C++: void Handle_Head_look()
    private fun handleHeadLook() {
        // FIXME: full head bone capture — requires model/bone system (head_bone, neck_bone)
        if (headLookDuration > 0f) {
            headLookDuration -= TimeManager.getFrameSeconds()
            val returning = headLookDuration < 0f

            val desiredHeadRotation = Vector3(0f, 0f, 0f)
            if (!returning) {
                if (headLookAngle.length() > 0.001f) {
                    headLookAngleTimer -= TimeManager.getFrameSeconds()
                    if (headLookAngleTimer < 0f) {
                        headLookAngle = Vector3(
                            FreeRandom.getFloat(-HEAD_TURN_LIMIT, HEAD_TURN_LIMIT),
                            FreeRandom.getFloat(-HEAD_TILT_LIMIT, HEAD_TILT_LIMIT),
                            0f
                        )
                        headLookAngleTimer = FreeRandom.getFloat(2f, 5f)
                    }
                }
                // target-look path deferred (requires bone transforms) — FIXME
            }

            val maxTurn = HEAD_TURN_RATE * TimeManager.getFrameSeconds()
            val maxTilt = HEAD_TILT_RATE * TimeManager.getFrameSeconds()
            headRotation.x += (desiredHeadRotation.x - headRotation.x).coerceIn(-maxTurn, maxTurn)
            headRotation.z += (desiredHeadRotation.z - headRotation.z).coerceIn(-maxTilt, maxTilt)

            headRotation.z = 0f
            if (returning && headRotation.length() > 0.001f) {
                headLookDuration = 0.0001f
            }
        }
    }

    // C++: void Update_Locked_Facing()
    private fun updateLockedFacing() {
        val facingObj = facingObject.get() as? PhysicalGameObj ?: return
        var pos = facingObj.getPosition()
        if (facingObj.asSoldierGameObj() != null) {
            pos = Vector3(pos.x, pos.y, pos.z + SOLDIER_HEIGHT_FOR_LOOK)
        }
        lookAt(pos, 100.0f)
        if (facingAllowBodyTurn) {
            internalSetTargeting(pos, false)
        }
    }

    // C++: void Update_Back_Gun()
    private fun updateBackGun() {
        val nextWeapon = weaponBag.getNextWeapon()
        if (nextWeapon != null && nextWeapon !== getWeapon()) {
            setBackWeaponModel(nextWeapon.getBackModelName())
        } else {
            setBackWeaponModel(null)
        }
    }

    // C++: void Set_Back_Weapon_Model(const char*)
    protected fun setBackWeaponModel(modelName: String?) {
        backWeaponRenderModel?.let {
            peekModel()?.removeSubObject(it)
            backWeaponRenderModel = null
        }
        if (!modelName.isNullOrEmpty()) {
            val strippedName = getRenderObjNameFromFilename(modelName)
            var robj = findRenderObj(strippedName)
            if (robj == null) {
                robj = createRenderObjFromFilename(modelName)
                if (robj != null) addRenderObj(robj)
            }
            if (robj != null) {
                backWeaponRenderModel = robj
                peekModel()?.addSubObjectToBone(robj, BACK_GUN_BONE_NAME)
            }
        }
    }

    // C++: void Set_Back_Flag_Model(const char*, const Vector3&)
    protected fun setBackFlagModel(modelName: String?, tint: Vector3 = Vector3(0f, 0f, 0f)) {
        backFlagRenderModel?.let {
            peekModel()?.removeSubObject(it)
            backFlagRenderModel = null
        }
        if (!modelName.isNullOrEmpty()) {
            val robj = createRenderObjFromFilename(modelName)
            if (robj != null) {
                peekModel()?.addSubObjectToBone(robj, BACK_GUN_BONE_NAME)
            }
        }
    }

    // C++: void Update_Healing_Effect()
    private fun updateHealingEffect() {
        healingEffect?.let { he ->
            if (he.getTargetParameter() >= 0.49f && he.getParameter() >= 0.49f) {
                he.setTargetParameter(0f)
            }
            if (he.getTargetParameter() == 0f && he.getParameter() == 0f) {
                peekHumanPhys()?.removeEffectFromMe(he)
                healingEffect = null
            }
        }
    }

    // C++: void Add_RenderObj(RenderObjClass*)
    fun addRenderObj(obj: RenderObjClass) {
        renderObjList.add(obj)
    }

    // C++: RenderObjClass* Find_RenderObj(const char*)
    fun findRenderObj(name: String): RenderObjClass? =
        renderObjList.firstOrNull { it.getName().equals(name, ignoreCase = true) }

    // C++: void Reset_RenderObjs()
    fun resetRenderObjs() { renderObjList.clear() }

    // C++: int Check() — anti-cheat CRC
    private fun computeCheck(): Int {
        // FIXME: CRC_Memory based check — not ported (references obfuscated filelist)
        return 0
    }

    // C++: helper — get ouch type from human state
    private fun getOuchType(direction: Vector3, collisionBoxName: String?): Int =
        humanState.getOuchType(direction, collisionBoxName)

    // C++: helper — generate idle facial anim timer setter
    private fun generaIdleFacialAnimTimer(value: Float) {
        generateIdleFacialAnimTimer = value
    }

    // -------------------------------------------------------------------------
    // Companion object — chunk IDs / micro chunk IDs / constants
    // -------------------------------------------------------------------------

    companion object {
        // C++: APPPACKETTYPE_SOLDIER
        const val APPPACKETTYPE_SOLDIER = 1

        // C++: GUN_BONE_NAME, BACK_GUN_BONE_NAME
        const val GUN_BONE_NAME      = "GUNBONE"
        const val BACK_GUN_BONE_NAME = "BACKGUNBONE"

        // C++: EMOT_ICON_HEIGHT
        const val EMOT_ICON_HEIGHT = 2.0f

        // C++: CORPSE_PERSIST_TIME (from humane state .cpp typically ~120 seconds)
        const val CORPSE_PERSIST_TIME = 120f

        // C++: DIALOG_MAX = 20
        const val DIALOG_MAX = 20

        // C++: head look limits
        val HEAD_TURN_LIMIT = kotlin.math.PI.toFloat() * 70f / 180f   // DEG_TO_RAD(70)
        val HEAD_TILT_LIMIT = kotlin.math.PI.toFloat() * 20f / 180f   // DEG_TO_RAD(20)
        val HEAD_TURN_RATE  = kotlin.math.PI.toFloat() * 2f           // DEG_TO_RAD(360) / 2
        val HEAD_TILT_RATE  = kotlin.math.PI.toFloat()                // DEG_TO_RAD(180) / 2

        // C++: CLIMB_SCALE = 0.3f
        const val CLIMB_SCALE = 0.3f

        // C++: AMBUSH_DAMAGE_SCALE = 1
        const val AMBUSH_DAMAGE_SCALE = 1f

        // C++: TILT_DOWN_SPEED = 4.0
        const val TILT_DOWN_SPEED = 4.0f

        // C++: physics constants
        const val PERSONAL_SPACE_BOX_SIZE_XY = 1.5f
        const val PERSONAL_SPACE_BOX_SIZE_Z  = 1.0f
        val PERSONAL_SPACE_BOX_SIZE = Vector3(1.5f, 1.5f, 1.0f)
        const val HUMAN_HALF_HEIGHT = 1.0f
        const val SOLDIER_HEIGHT_FOR_LOOK = 1.7f

        // C++: DEG_TO_RADF values used inline
        val DEG_TO_RADF_NEG_180 = -kotlin.math.PI.toFloat()
        val DEG_TO_RADF_POS_180 =  kotlin.math.PI.toFloat()
        val DEG_TO_RADF_NEG_90  = -kotlin.math.PI.toFloat() / 2f
        val DEG_20_RAD          =  kotlin.math.PI.toFloat() * 20f / 180f

        const val WWMATH_EPSILON = 0.0001f

        // Innate event bits — C++: SoldierInnateEvents enum
        const val SOLDIER_INNATE_EVENT_ENEMY_SEEN     = 1 shl 0
        const val SOLDIER_INNATE_EVENT_FOOTSTEP_HEARD = 1 shl 1
        const val SOLDIER_INNATE_EVENT_GUNSHOT_HEARD  = 1 shl 2
        const val SOLDIER_INNATE_EVENT_BULLET_HEARD   = 1 shl 3
        const val SOLDIER_INNATE_ACTIONS              = 1 shl 4

        // AI state constants — C++: SoldierAIState enum
        const val AI_STATE_IDLE            = 0
        const val AI_STATE_SECONDARY_IDLE  = 1
        const val AI_STATE_SEARCH          = 2

        // Dialogue IDs — C++: DialogueIDs from dialogue.h
        const val DIALOG_ON_TAKE_DAMAGE_FROM_ENEMY  = 0
        const val DIALOG_ON_TAKE_DAMAGE_FROM_FRIEND = 1
        const val DIALOG_ON_KILLED_ENEMY            = 2
        const val DIALOG_ON_KILLED_FRIEND           = 3

        // Class ID constant
        const val CLASSID_GAME_OBJECT_DEF_SOLDIER = 0x3001

        // Collision groups (from combatchunkid.h / physicalgameobj.h)
        const val SOLDIER_COLLISION_GROUP       = 1   // C++: SOLDIER_COLLISION_GROUP
        const val SOLDIER_GHOST_COLLISION_GROUP = 2   // C++: SOLDIER_GHOST_COLLISION_GROUP
        const val UNCOLLIDEABLE_GROUP           = 0   // C++: UNCOLLIDEABLE_GROUP
        const val BULLET_COLLISION_GROUP        = 3   // C++: BULLET_COLLISION_GROUP
        const val COLLISION_TYPE_PHYSICAL       = 1   // C++: COLLISION_TYPE_PHYSICAL

        // Surface types
        const val SURFACE_TYPE_UNDERWATER_DIRT  = 19  // C++: SURFACE_TYPE_UNDERWATER_DIRT
        const val SURFACE_TYPE_DEFAULT          = 0   // C++: SURFACE_TYPE_DEFAULT
        const val SURFACE_TYPE_LIGHT_METAL      = 6   // C++: SURFACE_TYPE_LIGHT_METAL
        const val SURFACE_TYPE_WATER_PERMEABLE  = 23  // C++: SURFACE_TYPE_WATER_PERMEABLE

        // Weapon styles
        const val WEAPON_HOLD_STYLE_C4     = 4   // C++: WEAPON_HOLD_STYLE_C4
        const val WEAPON_HOLD_STYLE_BEACON = 5   // C++: WEAPON_HOLD_STYLE_BEACON

        // Class IDs
        const val CLASSID_DEF_EXPLOSION = 0x8007 // C++: CLASSID_DEF_EXPLOSION
        const val CLASSID_DEF_WEAPON    = 0x9000 // C++: CLASSID_DEF_WEAPON

        // Conversation
        const val ACTION_COMPLETE_CONVERSATION_INTERRUPTED = 5  // C++: ACTION_COMPLETE_CONVERSATION_INTERRUPTED

        // ---- Chunk IDs (SoldierGameObj enum, base = 909991656) ----
        // C++:
        //   CHUNKID_PARENT                    = 909991656
        //   CHUNKID_VARIABLES                 = 909991657
        //   CHUNKID_WEAPON_ANIM               = 909991658
        //   CHUNKID_HUMAN_STATE               = 909991659
        //   XXXCHUNKID_C4_TIMER               = 909991660
        //   XXXCHUNKID_WEAPON_MODEL           = 909991661
        //   CHUNKID_TRANSITION_COMPLETION_DATA= 909991662
        //   CHUNKID_DIALOG_ENTRY              = 909991663
        //   CHUNKID_RENDER_OBJS               = 909991664
        //   CHUNKID_SPECIAL_DAMAGE_DAMAGER    = 909991665
        private const val CHUNKID_PARENT                     = 909991656
        private const val CHUNKID_VARIABLES                  = 909991657
        private const val CHUNKID_WEAPON_ANIM                = 909991658
        private const val CHUNKID_HUMAN_STATE                = 909991659
        private const val XXXCHUNKID_C4_TIMER                = 909991660
        private const val XXXCHUNKID_WEAPON_MODEL            = 909991661
        private const val CHUNKID_TRANSITION_COMPLETION_DATA = 909991662
        private const val CHUNKID_DIALOG_ENTRY               = 909991663
        private const val CHUNKID_RENDER_OBJS                = 909991664
        private const val CHUNKID_SPECIAL_DAMAGE_DAMAGER     = 909991665

        // ---- Micro chunk IDs (MICROCHUNKID starting at 1) ----
        // C++:
        //   MICROCHUNKID_DETONATE_C4              = 1
        //   MICROCHUNKID_LEG_FACING               = 2
        //   MICROCHUNKID_SYNC_LEGS                = 3
        //   MICROCHUNKID_ANIMATION_NAME           = 4
        //   MICROCHUNKID_VEHICLE                  = 5
        //   XXX_MICROCHUNKID_TRANSITION           = 6
        //   XXX_MICROCHUNKID_FORCE_FACING         = 7
        //   MICROCHUNKID_INNATE_ENABLE_BITS       = 8
        //   MICROCHUNKID_INNATE_OBSERVER_PTR      = 9
        //   MICROCHUNKID_LAST_LEG_MODE            = 10
        //   MICROCHUNKID_HEAD_LOOK_DURATION       = 11
        //   MICROCHUNKID_HEAD_ROTATION            = 12
        //   MICROCHUNKID_LOOK_TARGET              = 13
        //   XXXMICROCHUNKID_FLAME_TIMER           = 14
        //   MICROCHUNKID_KEY_RING                 = 15
        //   MICROCHUNKID_AI_STATE                 = 16
        //   XXX_MICROCHUNKID_IN_CONVERSATION      = 17
        //   MICROCHUNKID_LOOK_ANGLE               = 18
        //   MICROCHUNKID_LOOK_ANGLE_TIMER         = 19
        //   XXX_MICROCHUNKID_ACTIVE_CONVERSATION  = 20
        //   MICROCHUNKID_WEAPON_MODEL             = 21
        //   MICROCHUNKID_SPECIAL_DAMAGE_MODE      = 22
        //   MICROCHUNKID_SPECIAL_DAMAGE_TIMER     = 23
        //   MICROCHUNKID_IS_USING_GHOST_COLLISION = 24
        private const val MICROCHUNKID_DETONATE_C4              = 1
        private const val MICROCHUNKID_LEG_FACING               = 2
        private const val MICROCHUNKID_SYNC_LEGS                = 3
        private const val MICROCHUNKID_ANIMATION_NAME           = 4
        private const val MICROCHUNKID_VEHICLE                  = 5
        // 6 = XXX_MICROCHUNKID_TRANSITION (legacy)
        // 7 = XXX_MICROCHUNKID_FORCE_FACING (legacy)
        private const val MICROCHUNKID_INNATE_ENABLE_BITS       = 8
        private const val MICROCHUNKID_INNATE_OBSERVER_PTR      = 9
        private const val MICROCHUNKID_LAST_LEG_MODE            = 10
        private const val MICROCHUNKID_HEAD_LOOK_DURATION       = 11
        private const val MICROCHUNKID_HEAD_ROTATION            = 12
        private const val MICROCHUNKID_LOOK_TARGET              = 13
        // 14 = XXXMICROCHUNKID_FLAME_TIMER (legacy)
        private const val MICROCHUNKID_KEY_RING                 = 15
        private const val MICROCHUNKID_AI_STATE                 = 16
        // 17 = XXX_MICROCHUNKID_IN_CONVERSATION (legacy)
        private const val MICROCHUNKID_LOOK_ANGLE               = 18
        private const val MICROCHUNKID_LOOK_ANGLE_TIMER         = 19
        // 20 = XXX_MICROCHUNKID_ACTIVE_CONVERSATION (legacy)
        private const val MICROCHUNKID_WEAPON_MODEL             = 21
        private const val MICROCHUNKID_SPECIAL_DAMAGE_MODE      = 22
        private const val MICROCHUNKID_SPECIAL_DAMAGE_TIMER     = 23
        private const val MICROCHUNKID_IS_USING_GHOST_COLLISION = 24
    }
}
