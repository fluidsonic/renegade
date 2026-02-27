package ccr.server.net

// C++: class WarFactoryGameObj : public VehicleFactoryGameObj
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj
//            → VehicleFactoryGameObj → WarFactoryGameObj
class WarFactoryGameObj() : VehicleFactoryGameObj() {

    // C++: int CreationAnimationID (initialized to 0 in constructor initializer list)
    protected var creationAnimationID: Int = 0

    // C++: float CreationFinishedTimer (initialized to UNITIALIZED_TIMER in constructor initializer list)
    protected var creationFinishedTimer: Float = UNINITIALIZED_TIMER

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: ccr.math.Vector3 = ccr.math.Vector3(),
        sphereCenter: ccr.math.Vector3 = ccr.math.Vector3(),
        sphereRadius: Float = 50f,
        health: Float = 0f,
        isDestroyed: Boolean = false,
        isPowerOn: Boolean = true,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "warfactory_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: WarFactoryGameObj::Init(void) — delegates to Init(Get_Definition())
    override fun init() {
        init(getDefinition())
    }

    // C++: void WarFactoryGameObj::Init(const WarFactoryGameObjDef& definition)
    fun init(definition: ccr.server.defs.WarFactoryGameObjDef) {
        super.init(definition)
    }

    // C++: const WarFactoryGameObjDef& Get_Definition() const
    override fun getDefinition(): ccr.server.defs.WarFactoryGameObjDef =
        definition as ccr.server.defs.WarFactoryGameObjDef

    // C++: WarFactoryGameObj* As_WarFactoryGameObj() { return this; }
    // → handled by Kotlin type system: `this is WarFactoryGameObj`

    // -------------------------------------------------------------------------
    // C++: virtual bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        // C++: no micro-chunks written — CHUNKID_VARIABLES is empty
        csave.endChunk()

        return true
    }

    // -------------------------------------------------------------------------
    // C++: virtual bool Load(ChunkLoadClass& cload)
    // -------------------------------------------------------------------------
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_VARIABLES -> loadVariables(cload)
                else -> error("Unrecognized WarFactoryGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // -------------------------------------------------------------------------
    // C++: void WarFactoryGameObj::CnC_Initialize(BaseControllerClass* base)
    // -------------------------------------------------------------------------
    override fun cncInitialize(base: BaseControllerClass) {
        super.cncInitialize(base)

        // C++: Get the building's "position"
        val pos = getPosition()

        // C++: Find the closest creation static anim phys (WEP#CONSTRUCT model name)
        var closest2 = 99999.0f
        val scene = CombatManager.getScene()
        if (scene != null) {
            for (obj in scene.getStaticObjects()) {
                val animPhysObj = obj.asStaticAnimPhysClass() ?: continue
                val model = animPhysObj.peekModel() ?: continue

                val name = model.getName().uppercase()
                if (name.contains("WEP#CONSTRUCT")) {
                    val animPos = animPhysObj.getPosition()
                    val diff = animPos - pos
                    val dist2 = diff.length2()
                    if (dist2 < closest2) {
                        closest2 = dist2
                        creationAnimationID = animPhysObj.getId()
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // C++: void WarFactoryGameObj::Think(void)
    // -------------------------------------------------------------------------
    override fun think() {
        // C++: if (IsDestroyed == false && GeneratingVehicleID != 0)
        if (!isDestroyed && generatingVehicleID != 0) {

            // C++: if (CreationFinishedTimer > UNITIALIZED_TIMER)
            if (creationFinishedTimer > UNINITIALIZED_TIMER) {
                creationFinishedTimer -= TimeManager.getFrameSeconds()

                if (creationFinishedTimer <= 0f) {
                    creationFinishedTimer = UNINITIALIZED_TIMER

                    // C++: VehicleGameObj* vehicle = Create_Vehicle()
                    val vehicle = createVehicle()
                    if (vehicle != null) {
                        var newTm = creationTm

                        // C++: Adjust vehicle's transform to ensure it's not embedded in the ground
                        val vehiclePhys = vehicle.peekVehiclePhys() as? VehiclePhysClassStub
                        if (vehiclePhys != null) {
                            val height = vehiclePhys.computeApproximateRideHeight()
                            newTm = newTm.translated(0.0f, 0.0f, height)
                            vehicle.setTransform(newTm)
                        }

                        // C++: Lock the vehicle to anyone but the purchaser
                        val purch = purchaser.get()
                        if (purch != null) {
                            vehicle.lockVehicle(purch as SoldierGameObj, WARFACTORY_LOCK_TIME)
                        }

                        // C++: Destroy any game object that's in our way
                        destroyBlockingObjects()

                        // C++: Tell the vehicle to drive to one of the delivery points
                        deliverVehicle()
                    }

                    // C++: Play the creation animation backwards to restore the state of the factory
                    playCreationAnimation(false)
                }
            }
        }

        // C++: VehicleFactoryGameObj::Think() — called at the end
        super.think()
    }

    // -------------------------------------------------------------------------
    // C++: void WarFactoryGameObj::Play_Creation_Animation(bool onoff)
    // -------------------------------------------------------------------------
    // C++: protected
    protected fun playCreationAnimation(onoff: Boolean) {
        // C++: Lookup the static animation object we need to play
        val scene = CombatManager.getScene()
        val staticPhysObj = scene?.findStaticObject(creationAnimationID) ?: return
        val animPhysObj = staticPhysObj.asStaticAnimPhysClass() ?: return

        // C++: Configure the animation
        val animMgr = animPhysObj.getAnimationManager()
        animMgr.setAnimationMode(AnimCollisionManagerClass.ANIMATE_TARGET)

        // C++: Either play the animation forward or backward
        if (onoff) {
            animMgr.setTargetFrameEnd()
        } else {
            animMgr.setTargetFrame(0)
        }

        staticPhysObj.enableIsStateDirty(true)
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Begin_Generation(void)
    // -------------------------------------------------------------------------
    // C++: protected override of VehicleFactoryGameObj::Begin_Generation
    override fun beginGeneration() {
        playCreationAnimation(true)

        creationFinishedTimer = 2.0f

        // C++: Lookup the static animation object for the ending animation
        val scene = CombatManager.getScene()
        val staticPhysObj = scene?.findStaticObject(creationAnimationID)
        if (staticPhysObj != null) {
            val animPhysObj = staticPhysObj.asStaticAnimPhysClass()
            if (animPhysObj != null) {
                // C++: Calculate how long to wait before we start playing the end animations
                val animMgr = animPhysObj.getAnimationManager()
                creationFinishedTimer = (animMgr.peekAnimation()?.getTotalTime() ?: 0f) + 2.0f
            }
        }
    }

    // -------------------------------------------------------------------------
    // C++: void WarFactoryGameObj::Load_Variables(ChunkLoadClass& cload)
    // -------------------------------------------------------------------------
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            // C++: switch commented out — no variables to load
            // MICROCHUNKID_UNUSED = 1 (never written)
            cload.closeMicroChunk()
        }
    }

    companion object {
        // C++: const float WARFACTORY_LOCK_TIME = 26.0f
        const val WARFACTORY_LOCK_TIME = 26.0f

        // C++: float UNITIALIZED_TIMER = -100.0F (file-scope constant in warfactorygameobj.cpp)
        private const val UNINITIALIZED_TIMER = -100.0f

        // C++: local enum in warfactorygameobj.cpp
        // enum { CHUNKID_PARENT = 0x0219043, CHUNKID_VARIABLES, MICROCHUNKID_UNUSED = 1 }
        private const val CHUNKID_PARENT    = 0x0219043
        private const val CHUNKID_VARIABLES = 0x0219044
        private const val MICROCHUNKID_UNUSED = 1

        // C++: CHUNKID_GAME_OBJECT_WARFACTORY = 0x00040141 (combatchunkid.h)
        const val CHUNK_ID: UInt = 0x00040141u
    }
}
