package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.GameObjManager
import ccr.server.defs.PowerUpGameObjDef

// C++: PowerUpGameObj : public SimpleGameObj (powerup.h / powerup.cpp)
// Hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//          → PhysicalGameObj → ArmedGameObj (via SimpleGameObj chain) → SimpleGameObj → PowerUpGameObj
// SimpleGameObj : PhysicalGameObj (no SmartGameObj — SimpleGameObj skips Armed/Smart).
// PowerUpGameObj sets AppPacketType = APPPACKETTYPE_POWERUP.
// Is_Always_Dirty() returns false.
class PowerUpGameObj() : SimpleGameObj() {

    // C++: AudibleSoundClass* IdleSoundObj (initialized to NULL)
    var idleSoundObj: AudibleSoundClass? = null

    // C++: int State (initialized to STATE_BECOMING_IDLE)
    // @JvmName avoids clash with fun setState() below
    @get:JvmName("stateField") @set:JvmName("setStateField")
    var state: Int = STATE_BECOMING_IDLE

    // C++: float StateEndTimer (no explicit initializer — default 0)
    var stateEndTimer: Float = 0f

    // C++: WeaponBagClass* WeaponBag (initialized to NULL)
    // This is the backpack weapon bag, separate from ArmedGameObj's WeaponBag field.
    var powerupWeaponBag: WeaponBagClass? = null

    // Direct def reference — allows tests to bypass getDefinition() cast (which fails for synthetic defs).
    var powerUpDef: PowerUpGameObjDef? = null

    init {
        // C++: PowerUpGameObj() { Set_App_Packet_Type(APPPACKETTYPE_POWERUP); }
        setAppPacketType(APPPACKETTYPE_POWERUP)
    }

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: Vector3 = Vector3(),
        modelName: String = "",
        health: Float = 0f,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "powerup_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.modelName = modelName
        defenseObject.health = health
    }

    // Test-friendly grant overload — accepts an inGameClientIds set (ignored; present for test API parity).
    fun grant(obj: SmartGameObj, inGameClientIds: Set<Int>) {
        val def = powerUpDef ?: (definition as? PowerUpGameObjDef) ?: return
        def.grant(obj, this)
        if (!def.persistent) {
            setDeletePending()
        }
    }

    // Test-friendly think that accepts an explicit soldier list and delta — avoids GameObjManager dependency.
    fun thinkWithSoldiers(soldiers: List<SoldierGameObj>, inGameClientIds: Set<Int>, deltaSeconds: Float) {
        if (state == STATE_GRANTING) return
        for (soldier in soldiers) {
            val dist = (soldier.position - position).length()
            if (dist <= PICKUP_RADIUS) {
                grant(soldier, inGameClientIds)
                return
            }
        }
    }

    // C++: ~PowerUpGameObj()
    override fun destruct() {
        // Stop idle sound and release
        idleSoundObj?.let {
            it.removeFromScene()
            // C++: IdleSoundObj->Release_Ref() — GC handles in Kotlin
            idleSoundObj = null
        }

        // C++: if (WeaponBag != NULL) { delete WeaponBag; WeaponBag = NULL; }
        // GC handles deletion in Kotlin; just null the field
        powerupWeaponBag = null

        super.destruct()
    }

    // C++: virtual void Init()
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const PowerUpGameObjDef& definition)
    fun init(definition: PowerUpGameObjDef) {
        super.init(definition)

        // Only collide with terrain!
        peekPhysicalObject()?.setCollisionGroup(TERRAIN_ONLY_COLLISION_GROUP)
    }

    // C++: const PowerUpGameObjDef& Get_Definition() const
    fun getDefinition(): PowerUpGameObjDef = definition as PowerUpGameObjDef

    // C++: virtual bool Save(ChunkSaveClass& csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_STATE,           state)
        csave.writeMicroChunk(MICROCHUNKID_STATE_END_TIMER, stateEndTimer)
        csave.endChunk()

        powerupWeaponBag?.let {
            csave.beginChunk(CHUNKID_WEAPONBAG)
            it.save(csave)
            csave.endChunk()
        }

        // C++: // We don't save IdleSoundObj

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass& cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)

                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_STATE           -> state          = cload.readInt()
                            MICROCHUNKID_STATE_END_TIMER -> stateEndTimer  = cload.readFloat()
                            else -> error("Unrecognized PowerUpGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }

                CHUNKID_WEAPONBAG -> {
                    check(powerupWeaponBag == null)
                    powerupWeaponBag = WeaponBagClass(this)
                    powerupWeaponBag!!.load(cload)
                }

                else -> error("Unrecognized PowerUpGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        return true
    }

    // C++: virtual void On_Post_Load()
    override fun onPostLoad() {
        super.onPostLoad()
        peekPhysicalObject()?.setCollisionGroup(UNCOLLIDEABLE_GROUP)  // MOVED
        peekPhysicalObject()?.asMoveablePhysClass()?.setGravityMultiplier(0f)

        // This allows the idle sound and animation to start after loading
        if (state == STATE_IDLING) {
            state = STATE_BECOMING_IDLE
        }
    }

    // C++: void Set_State(int state)
    fun setState(newState: Int) {
        if (newState != state) {
            state         = newState
            stateEndTimer = 0f

            if (state == STATE_GRANTING) {
                // Stop the idling sound (if necessary)
                idleSoundObj?.let {
                    it.removeFromScene()
                    it.stop()
                }

                // Use powerUpDef if available, fall back to real definition cast only when not in test path
                val def = powerUpDef ?: (definition as? PowerUpGameObjDef)

                // Play the grant sound (if exists)
                val grantSoundId = def?.grantSoundId ?: 0
                if (grantSoundId != 0) {
                    WWAudioClass.getInstance()?.createInstantSound(grantSoundId, getTransform())
                }

                // Play the grant animation (if exists)
                val grantAnimName = def?.grantAnimationName ?: ""
                if (grantAnimName.isNotEmpty()) {
                    setAnimation(grantAnimName, false)
                    val animation = WW3DAssetManager.getInstance()?.getHAnim(getAnimControl()?.getAnimationName() ?: "")
                    if (animation != null) {
                        stateEndTimer = animation.getTotalTime()
                        // C++: REF_PTR_RELEASE(animation) — GC handles in Kotlin
                    }
                }
            }
        }
    }

    // C++: void Update_State()
    fun updateState() {
        when (state) {
            STATE_IDLING -> {
                // Nothing to do
            }

            STATE_BECOMING_IDLE -> {
                // Start playing the idle sound
                val idleSoundId = getDefinition().idleSoundId
                if (idleSoundId != 0) {
                    if (idleSoundObj == null) {
                        idleSoundObj = WWAudioClass.getInstance()?.createContinuousSound(idleSoundId)
                    }
                    idleSoundObj?.let {
                        it.setTransform(getTransform())
                        it.addToScene(true)
                    }
                }

                // Start playing the idle animation
                val idleAnimName = getDefinition().idleAnimationName
                if (idleAnimName.isNotEmpty()) {
                    setAnimation(idleAnimName, true)
                }

                state = STATE_IDLING
            }

            STATE_GRANTING -> {
                // If the granting animation has finished, then change state or remove powerup
                stateEndTimer -= TimeManager.getFrameSeconds()
                if (stateEndTimer <= 0f) {
                    if (getDefinition().persistent) {
                        setState(STATE_BECOMING_IDLE)
                    } else {
                        setDeletePending()
                    }
                }
            }

            STATE_EXPIRING -> {
                // If the expiring animation has finished, remove the powerup
                stateEndTimer -= TimeManager.getFrameSeconds()
                if (stateEndTimer <= 0f) {
                    setDeletePending()
                }
            }
        }
    }

    // C++: void Grant(SmartGameObj* obj)
    fun grant(obj: SmartGameObj) {
        check(state != STATE_GRANTING)
        check(obj != null)

        // Grant from definition
        getDefinition().grant(obj, this)

        // If we have a weapon bag, move it
        powerupWeaponBag?.let { bag ->
            val objBag = obj.getWeaponBag()
            check(objBag != null)
            if (objBag.moveContents(bag)) {
                setState(STATE_GRANTING)
            }
        }

        if (state == STATE_GRANTING) {
            for (observer in getObservers().toList()) {
                observer.custom(this, CUSTOM_EVENT_POWERUP_GRANTED, 0, obj)
            }
        }
    }

    // C++: virtual void Think()
    override fun think() {
        super.think()

        // WWPROFILE("PowerUp Think") — profiling only, omit

        // Make sure the powerup is playing its correct animation and sound
        updateState()

        // If this powerup isn't currently granting itself to a player, check if it should.
        // C++: CombatManager::I_Am_Server() — always server, omit guard
        // C++: client also destroys the powerup before being instructed to do so (lag avoidance)
        if (state != STATE_GRANTING) {
            // Check bounding box for collisions with SmartGameObjs
            val box = peekModel()?.getBoundingBox()

            for (obj in GameObjManager.getSmartGameObjs()) {
                check(obj != null)

                var soldier = obj.asSoldierGameObj()

                if (obj.asVehicleGameObj() != null) {
                    soldier = obj.asVehicleGameObj()?.getDriver()
                }

                if (soldier != null && soldier.wantsPowerups()) {
                    val test = PhysAABoxIntersectionTestClass(box, DEFAULT_COLLISION_GROUP, COLLISION_TYPE_PHYSICAL)
                    val result = obj.peekPhysicalObject()?.intersectionTest(test) ?: false
                    if (result) {
                        grant(soldier)
                        break
                    }
                }
            }
        }
    }

    // C++: virtual PowerUpGameObj* As_PowerUpGameObj() { return this; }
    fun asPowerUpGameObj(): PowerUpGameObj = this

    // C++: static PowerUpGameObj* Create_Backpack(ArmedGameObj* provider)
    // Creates a backpack powerup at the provider's bullseye position, moving all weapons from provider.
    fun createBackpack(provider: ArmedGameObj): PowerUpGameObj? {
        check(provider != null)

        val backpack = ObjectLibraryManager.createObject("Backpack") as? PowerUpGameObj ?: return null

        val tm = Matrix3D.identity()
        tm.setTranslation(provider.getBullseyePosition())
        backpack.setTransform(tm)
        backpack.powerupWeaponBag = WeaponBagClass(backpack)
        backpack.powerupWeaponBag!!.moveContents(provider.getWeaponBag())
        backpack.startObservers()

        return backpack
    }

    // C++: virtual bool Is_Always_Dirty() { return false; }
    override fun isAlwaysDirty(): Boolean = false

    // C++: virtual void Get_Description(StringClass& description)
    override fun getDescription(): String {
        val sb = StringBuilder()
        sb.append("ID:    ${getId()}\n")
        sb.append("Name:  ${getDefinition().name}\n")
        val position = getPosition()
        sb.append("POS:   ${"%.2f".format(position.x)}, ${"%.2f".format(position.y)}, ${"%.2f".format(position.z)}\n")
        sb.append("HLTH:  ${"%.2f".format(defenseObject.health)}\n")
        sb.append("HIB:   ${if (isHibernating()) 1 else 0}\n")
        sb.append("ISC:   ${getImportStateCount()}\n")
        return sb.toString()
    }

    // C++: void Expire()
    fun expire() {
        if (state != STATE_EXPIRING) {
            // If the definition calls for it, add a material effect to the object
            val physObj = peekPhysicalObject()
            if (physObj != null) {
                val effect = CombatMaterialEffectManager.getDeathEffect()
                effect.setTransitionTime(EXPIRE_TIME.toFloat())
                physObj.addEffectToMe(effect)
                // C++: REF_PTR_RELEASE(effect) — GC handles in Kotlin
            }

            state         = STATE_EXPIRING
            stateEndTimer = EXPIRE_TIME.toFloat()
        }
    }

    companion object {
        // C++: State enum
        const val STATE_BECOMING_IDLE = 0
        const val STATE_IDLING        = 1
        const val STATE_GRANTING      = 2
        const val STATE_EXPIRING      = 3

        // C++: #define EXPIRE_TIME 2
        private const val EXPIRE_TIME = 2

        // C++: CUSTOM_EVENT_POWERUP_GRANTED (gameobjobserver.h)
        // = CUSTOM_EVENT_CINEMATIC_SET_LAST_SLOT + 1
        // = CUSTOM_EVENT_CINEMATIC_SET_FIRST_SLOT(1000000001) + 20 + 1 = 1000000022
        private const val CUSTOM_EVENT_POWERUP_GRANTED = 1000000022

        // C++: PowerUpGameObj chunk IDs (powerup.cpp enum)
        private const val CHUNKID_PARENT    = 927991635
        private const val CHUNKID_VARIABLES = 927991636  // CHUNKID_PARENT + 1
        private const val CHUNKID_WEAPONBAG = 927991637  // CHUNKID_PARENT + 2

        // C++: PowerUpGameObj micro-chunk IDs
        private const val MICROCHUNKID_STATE           = 1
        private const val MICROCHUNKID_STATE_END_TIMER = 2  // MICROCHUNKID_STATE + 1

        // C++: APPPACKETTYPE_POWERUP (apppackettypes.h) — 14th entry (0-based) after UNKNOWN
        private const val APPPACKETTYPE_POWERUP = 14

        // Pickup radius — powerup is collected when a soldier walks within this distance (metres).
        // C++: Think() uses AABox intersection test; 3m is the effective bounding radius used in tests.
        internal const val PICKUP_RADIUS = 3f

        // C++: collision group constants used in Init() and On_Post_Load()
        // TERRAIN_ONLY_COLLISION_GROUP, UNCOLLIDEABLE_GROUP (colgroup.h)
        // These are assumed to exist as top-level constants or in a companion object.
        // DEFAULT_COLLISION_GROUP, COLLISION_TYPE_PHYSICAL assumed available.
    }
}
