package ccr.server.net

import ccr.math.Vector3
import ccr.server.defs.ScriptableGameObjDef

// C++: ScriptableGameObj : public BaseGameObj, public ReferenceableGameObj, public AudioCallbackClass
// AudioCallbackClass omitted — not relevant for server port.
abstract class ScriptableGameObj : BaseGameObj() {

    // C++: ReferenceableGameObj(this) — base class initializer
    val referenceableGameObj: ReferenceableGameObj = ReferenceableGameObj(this)

    // C++: bool ObserverCreatedPending (initialized to false)
    protected var observerCreatedPending: Boolean = false

    // C++: GameObjObserverList Observers
    // @JvmName avoids clash with explicit fun getObservers() below
    @get:JvmName("observersMutableList")
    protected val observers: MutableList<GameObjObserverClass> = mutableListOf()

    // C++: DynamicVectorClass<GameObjObserverTimerClass*> ObserverTimerList
    protected val observerTimerList: MutableList<GameObjObserverTimerClass> = mutableListOf()

    // C++: DynamicVectorClass<GameObjCustomTimerClass*> CustomTimerList
    protected val customTimerList: MutableList<GameObjCustomTimerClass> = mutableListOf()

    // C++: ~ScriptableGameObj() — removes all observers and deletes timer lists
    override fun destruct() {
        referenceableGameObj.destruct()
        removeAllObservers()
        observerTimerList.clear()
        customTimerList.clear()
        super.destruct()
    }

    // C++: void Init(const ScriptableGameObjDef & definition)
    fun init(definition: ScriptableGameObjDef) {
        super.init(definition)
        copySettings(definition)
    }

    // C++: void Copy_Settings(const ScriptableGameObjDef & definition)
    fun copySettings(definition: ScriptableGameObjDef) {
        for (i in 0 until definition.scriptNameList.size) {
            val script = ScriptManager.createScript(definition.scriptNameList[i]) ?: continue
            script.setParametersString(definition.scriptParameterList[i])
            insertObserver(script)
        }
    }

    // C++: void Re_Init(const ScriptableGameObjDef & definition)
    fun reInit(definition: ScriptableGameObjDef) {
        removeAllObservers()
        copySettings(definition)
        super.init(definition)
    }

    // C++: virtual void Post_Re_Init()
    open fun postReInit() {
        startObservers()
    }

    // C++: const ScriptableGameObjDef & Get_Definition() const
    // FIXME: definition is nullable — wire proper non-null guarantee when init() is always called before use
    fun getScriptableDefinition(): ScriptableGameObjDef = definition as ScriptableGameObjDef

    // C++: virtual void Set_Delete_Pending()
    override fun setDeletePending() {
        if (!isDeletePending) {
            // C++: if (CombatManager::Are_Observers_Active()) — always true on server
            for (observer in observers) {
                observer.destroyed(this)
            }
            // C++: if (this == CombatManager::Get_The_Star()) CombatManager::Star_Killed()
            // FIXME: wire Star_Killed when CombatManager is ported
            super.setDeletePending()
        }
    }

    // C++: virtual bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_REFERENCEABLE)
        referenceableGameObj.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_SCRIPTABLE_VARIABLES)
        // FIXME: write referenceableGameObj this-pointer via MICROCHUNKID_REFERENCEABLE_PTR when pointer remap is ported
        for (observer in observers) {
            csave.writeMicroChunk(MICROCHUNKID_GAME_OBJ_OBSERVER_PTR, observer)
        }
        csave.writeMicroChunk(MICROCHUNKID_OBSERVER_CREATED_PENDING, observerCreatedPending)
        csave.endChunk()

        for (timer in observerTimerList) {
            csave.beginChunk(CHUNKID_OBSERVER_TIMER)
            timer.save(csave)
            csave.endChunk()
        }

        for (timer in customTimerList) {
            csave.beginChunk(CHUNKID_CUSTOM_TIMER)
            timer.save(csave)
            csave.endChunk()
        }

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        check(observers.isEmpty())
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT        -> super.load(cload)
                CHUNKID_REFERENCEABLE -> referenceableGameObj.load(cload)
                CHUNKID_SCRIPTABLE_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            // FIXME: MICROCHUNKID_REFERENCEABLE_PTR — pointer remap not ported
                            MICROCHUNKID_OBSERVER_CREATED_PENDING -> { observerCreatedPending = cload.readBool() }
                            MICROCHUNKID_GAME_OBJ_OBSERVER_PTR    -> { observers.add(cload.readPtr() as GameObjObserverClass) }
                            else -> error("Unrecognized ScriptableGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_OBSERVER_TIMER -> {
                    val timer = GameObjObserverTimerClass()
                    timer.load(cload)
                    observerTimerList.add(timer)
                }
                CHUNKID_CUSTOM_TIMER -> {
                    val timer = GameObjCustomTimerClass()
                    timer.load(cload)
                    customTimerList.add(timer)
                }
                else -> error("Unrecognized ScriptableGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        // FIXME: REQUEST_POINTER_REMAP for each observer — pointer remap not ported
        // FIXME: SaveLoadSystemClass::Register_Pointer(referenceable_ptr, referenceableGameObj)
        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        return true
    }

    // C++: virtual void Think()
    override fun think() {
        if (isAlwaysDirty()) {
            setObjectDirtyBit(BIT_FREQUENT, true)
        }
        if (observerCreatedPending) {
            startObservers()
            observerCreatedPending = false
        }
        super.think()
    }

    // C++: virtual void Post_Think()
    // FIXME: explore CopyOnWriteArrayList for observerTimerList/customTimerList to get identical C++ concurrent-modification behavior
    override fun postThink() {
        super.postThink()
        for (i in observerTimerList.indices.reversed()) {
            if (observerTimerList[i].update()) {
                val timer = observerTimerList[i]
                check(timer.observerId != 0)
                for (observer in observers.toList()) {
                    if (observer.getId() == timer.observerId) {
                        observer.timerExpired(this, timer.timerId)
                    }
                }
                observerTimerList.remove(timer)
            }
        }
        for (i in customTimerList.indices.reversed()) {
            if (customTimerList[i].update()) {
                val timer = customTimerList[i]
                for (observer in observers.toList()) {
                    observer.custom(this, timer.type, timer.param, timer.sender)
                }
                customTimerList.remove(timer)
            }
        }
    }

    // C++: virtual void Get_Position(Vector3 * set_pos) const = 0
    abstract fun getPosition(): Vector3

    // C++: virtual bool Is_Always_Dirty() { return true; }
    open fun isAlwaysDirty(): Boolean = true

    // C++: virtual void Export_Creation(BitStreamClass & packet)
    override fun exportCreation(packet: ccr.net.bitstream.BitStream) {
        super.exportCreation(packet)
    }

    // C++: virtual void Import_Creation(BitStreamClass & packet)
    override fun importCreation(packet: ccr.net.bitstream.BitStream) {
        super.importCreation(packet)
        removeAllObservers()
    }

    // C++: void Add_Observer(GameObjObserverClass*)
    fun addObserver(observer: GameObjObserverClass) {
        insertObserver(observer)
        // FIXME: wire CombatManager::Are_Observers_Active() when CombatManager is ported
        if (CombatManager.areObserversActive()) {
            observer.created(this)
        }
    }

    // C++: void Insert_Observer(GameObjObserverClass*) — adds without calling Created
    fun insertObserver(observer: GameObjObserverClass) {
        observer.attach(this)
        observers.add(observer)
    }

    // C++: void Remove_Observer(GameObjObserverClass*)
    fun removeObserver(observer: GameObjObserverClass) {
        observers.remove(observer)
        observer.detach(this)
    }

    // C++: void Remove_All_Observers()
    fun removeAllObservers() {
        while (observers.isNotEmpty()) {
            removeObserver(observers[0])
        }
    }

    // C++: const GameObjObserverList & Get_Observers()
    fun getObservers(): List<GameObjObserverClass> = observers

    // C++: virtual void Get_Information(StringClass&) — base returns empty string
    open fun getInformation(): String = ""

    // C++: virtual void On_Post_Load()
    override open fun onPostLoad() {
        super.onPostLoad()
        // FIXME: remove NULL observers after pointer remap — wire when pointer remap system is ported
        // FIXME: only set if CombatManager::Is_First_Load() — wire when CombatManager is ported
        observerCreatedPending = true
    }

    // C++: void Start_Observers()
    // FIXME: explore CopyOnWriteArrayList for observers to get identical C++ concurrent-modification behavior
    fun startObservers() {
        for (observer in observers.toList()) {
            observer.created(this)
        }
    }

    // C++: void Start_Observer_Timer(int observer_id, float duration, int timer_id)
    fun startObserverTimer(observerId: Int, duration: Float, timerId: Int) {
        observerTimerList.add(GameObjObserverTimerClass(observerId, duration, timerId))
    }

    // C++: void Start_Custom_Timer(ScriptableGameObj* from, float delay, int type, int param)
    fun startCustomTimer(from: ScriptableGameObj?, delay: Float, type: Int, param: Int) {
        customTimerList.add(GameObjCustomTimerClass(from, delay, type, param))
    }

    companion object {
        private const val CHUNKID_PARENT          = 627001122
        internal const val CHUNKID_SCRIPTABLE_VARIABLES = 627001123
        private const val CHUNKID_REFERENCEABLE   = 627001124
        private const val CHUNKID_CUSTOM_TIMER    = 627001125
        private const val CHUNKID_OBSERVER_TIMER  = 627001126

        private const val MICROCHUNKID_REFERENCEABLE_PTR          = 1
        private const val MICROCHUNKID_GAME_OBJ_OBSERVER_PTR      = 2
        private const val MICROCHUNKID_OBSERVER_CREATED_PENDING    = 3
    }
}
