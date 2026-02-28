package ccr.server.net

import ccr.net.replication.NetworkObject
import ccr.server.defs.AirStripGameObjDef

// C++: AirStripGameObj : public VehicleFactoryGameObj (airstripgameobj.h / airstripgameobj.cpp)
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj
//            → VehicleFactoryGameObj → AirStripGameObj
class AirStripGameObj() : VehicleFactoryGameObj() {

    // C++: float CinematicStartTimer (initialized to 0)
    protected var cinematicStartTimer: Float = 0f

    // C++: float EndTimer (declared in header; not used by AirStrip-level code, parent has own EndTimer)
    // Uses parent VehicleFactoryGameObj.endTimer — no separate field needed

    // C++: float DisplayVehicleTimer (initialized to UNITIALIZED_TIMER)
    protected var displayVehicleTimer: Float = UNINITIALIZED_TIMER

    // C++: float ClearDropoffZoneTimer (initialized to UNITIALIZED_TIMER)
    protected var clearDropoffZoneTimer: Float = UNINITIALIZED_TIMER

    // C++: bool IsCinematicStarted (initialized to false)
    protected var isCinematicStarted: Boolean = false

    // C++: PhysicalGameObj* CinematicObject (initialized to NULL)
    protected var cinematicObject: PhysicalGameObj? = null

    // C++: AirStripGameObj() — constructor body is empty; fields initialized above
    init {
    }

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
            name = "airstrip_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: ~AirStripGameObj() — destructor body is empty
    override fun destruct() {
        super.destruct()
    }

    // C++: virtual void Init(void)
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const AirStripGameObjDef& definition)
    fun init(definition: AirStripGameObjDef) {
        // C++: VehicleFactoryGameObj::Init(definition)
        super.init(definition)
    }

    // C++: const AirStripGameObjDef& Get_Definition() const
    override fun getDefinition(): AirStripGameObjDef = definition as AirStripGameObjDef

    // C++: AirStripGameObj* As_AirStripGameObj() { return this; }
    // → handled by Kotlin type system: `this is AirStripGameObj`

    // -------------------------------------------------------------------------
    // C++: bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        // C++: csave.Begin_Chunk(CHUNKID_PARENT); VehicleFactoryGameObj::Save(csave); csave.End_Chunk()
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        // C++: csave.Begin_Chunk(CHUNKID_VARIABLES); csave.End_Chunk() — empty variables chunk
        csave.beginChunk(CHUNKID_VARIABLES)
        csave.endChunk()

        return true
    }

    // -------------------------------------------------------------------------
    // C++: bool Load(ChunkLoadClass& cload)
    // -------------------------------------------------------------------------
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_VARIABLES -> loadVariables(cload)
                else              -> error("Unrecognized AirStrip chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass& cload) — switch body is commented out in C++;
    // just drains all micro-chunks without reading anything.
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            // C++: switch was entirely commented out — all micro-chunks silently discarded
            cload.closeMicroChunk()
        }
    }

    // -------------------------------------------------------------------------
    // C++: void CnC_Initialize(BaseControllerClass* base)
    // -------------------------------------------------------------------------
    override fun cncInitialize(base: BaseControllerClass) {
        // C++: VehicleFactoryGameObj::CnC_Initialize(base)
        super.cncInitialize(base)
    }

    // -------------------------------------------------------------------------
    // C++: void Think(void)
    // -------------------------------------------------------------------------
    override fun think() {
        // C++: Begin the cinematic if necessary
        // C++: if (IsDestroyed == false && GeneratingVehicleID != 0)
        if (!isDestroyed && generatingVehicleID != 0) {
            // C++: if (IsCinematicStarted == false)
            if (!isCinematicStarted) {
                // C++: CinematicStartTimer -= TimeManager::Get_Frame_Seconds()
                cinematicStartTimer -= TimeManager.getFrameSeconds()
                if (cinematicStartTimer <= 0f) {
                    // C++: Start_Cinematic()
                    startCinematic()
                    // C++: ClearDropoffZoneTimer = 0.8f * Get_Definition().CinematicLengthToDropOff
                    clearDropoffZoneTimer = 0.8f * getDefinition().cinematicLengthToDropOff
                }
            }
        }

        // C++: if ((GeneratingVehicleID != 0) && (IsCinematicStarted))
        if (generatingVehicleID != 0 && isCinematicStarted) {

            // C++: if (DisplayVehicleTimer > UNITIALIZED_TIMER)
            if (displayVehicleTimer > UNINITIALIZED_TIMER) {
                // C++: DisplayVehicleTimer -= TimeManager::Get_Frame_Seconds()
                displayVehicleTimer -= TimeManager.getFrameSeconds()
                if (displayVehicleTimer < 0f) {

                    // C++: Display the vehicle
                    // C++: if (Vehicle != NULL) { Peek_Model()->Set_Hidden(false); Set_Object_Dirty_Bit(BIT_RARE, true) }
                    val vehicleObj = vehicle?.get()?.asPhysicalGameObj()
                    if (vehicleObj != null) {
                        vehicleObj.peekModel()?.setHidden(false)
                        vehicleObj.setObjectDirtyBit(NetworkObject.BIT_RARE, true)
                    }
                    displayVehicleTimer = UNINITIALIZED_TIMER

                    // C++: Deliver_Vehicle()
                    deliverVehicle()
                }
            }

            // C++: if (ClearDropoffZoneTimer > UNITIALIZED_TIMER)
            if (clearDropoffZoneTimer > UNINITIALIZED_TIMER) {
                // C++: ClearDropoffZoneTimer -= TimeManager::Get_Frame_Seconds()
                clearDropoffZoneTimer -= TimeManager.getFrameSeconds()
                if (clearDropoffZoneTimer < 0f) {
                    // C++: Destroy_Blocking_Objects()
                    destroyBlockingObjects()
                    clearDropoffZoneTimer = UNINITIALIZED_TIMER
                }
            }
        }

        // C++: VehicleFactoryGameObj::Think()
        super.think()
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Begin_Generation(void)
    // Called from VehicleFactoryGameObj::Request_Vehicle when generation starts.
    // -------------------------------------------------------------------------
    override fun beginGeneration() {
        // C++: CinematicStartTimer = GenerationTime - Get_Definition().CinematicLengthToDropOff
        cinematicStartTimer = generationTime - getDefinition().cinematicLengthToDropOff
        // C++: IsCinematicStarted = false
        isCinematicStarted = false
    }

    // -------------------------------------------------------------------------
    // C++: void Start_Cinematic(void)
    // -------------------------------------------------------------------------
    protected fun startCinematic() {
        // C++: if (CombatManager::I_Am_Server() == false) return — always server, omit guard

        // C++: VehicleGameObj* vehicle = Create_Vehicle()
        val vehicle = createVehicle()
        if (vehicle != null) {

            // C++: Hide the vehicle until later
            // C++: if (vehicle->Peek_Model() != NULL) { vehicle->Peek_Model()->Set_Hidden(true); Set_Object_Dirty_Bit(BIT_RARE, true) }
            if (vehicle.peekModel() != null) {
                vehicle.peekModel()!!.setHidden(true)
                vehicle.setObjectDirtyBit(NetworkObject.BIT_RARE, true)
            }

            // C++: Lock the vehicle
            // C++: if (Purchaser != NULL) vehicle->Lock_Vehicle(Purchaser, VEHICLE_LOCK_TIME)
            val purchaserObj = purchaser?.get()
            if (purchaserObj != null) {
                vehicle.lockVehicle(purchaserObj, VEHICLE_LOCK_TIME)
            }

            // C++: Create the cinematic controller
            // C++: CinematicObject = ObjectLibraryManager::Create_Object(Get_Definition().CinematicDefID)
            cinematicObject = ObjectLibraryManager.createObject(getDefinition().cinematicDefId) as? PhysicalGameObj
            val cinObj = cinematicObject
            if (cinObj != null) {
                // C++: CinematicObject->Start_Observers()
                cinObj.startObservers()

                // C++: Position the cinematic controller in the world
                // C++: Matrix3D test_tm = CreationTM; CinematicObject->Set_Transform(test_tm)
                cinObj.setTransform(creationTm)

                // C++: Try to find the "Test_Cinematic" script observer
                // C++: const GameObjObserverList& script_list = CinematicObject->Get_Observers()
                // C++: for (int index = 0; ...) { if lstrcmpi(script->Get_Name(), "Test_Cinematic") == 0 ... }
                for (script in cinObj.getObservers()) {
                    if (script.getName().equals("Test_Cinematic", ignoreCase = true)) {
                        // C++: int slot = 10000 + Get_Definition().CinematicSlotIndex
                        // C++: script->Custom(this, slot, vehicle->Get_ID(), this)
                        val slot = 10000 + getDefinition().cinematicSlotIndex
                        script.custom(this, slot, vehicle.getId(), this)
                        break
                    }
                }
            }
        }

        // C++: IsCinematicStarted = true
        isCinematicStarted = true
        // C++: DisplayVehicleTimer = Get_Definition().CinematicLengthToVehicleDisplay
        displayVehicleTimer = getDefinition().cinematicLengthToVehicleDisplay
    }

    // C++: void Attach_Vehicle(void) — declared in header; no body shown in cpp
    protected fun attachVehicle() {
    }

    companion object {
        // C++: static const float UNITIALIZED_TIMER = -100.0F (airstripgameobj.cpp)
        private const val UNINITIALIZED_TIMER = -100f

        // C++: static const float VEHICLE_LOCK_TIME = 30.0f (airstripgameobj.cpp)
        private const val VEHICLE_LOCK_TIME = 30f

        // AirStripGameObj Save/Load chunk IDs (airstripgameobj.cpp local enum)
        // enum { CHUNKID_PARENT = 0x0219043, CHUNKID_VARIABLES, MICROCHUNKID_UNUSED = 1 }
        private const val CHUNKID_PARENT    = 0x0219043
        private const val CHUNKID_VARIABLES = 0x0219044

        // AirStripGameObj micro-chunk IDs (airstripgameobj.cpp local enum)
        // MICROCHUNKID_UNUSED = 1 — the entire switch in Load_Variables is commented out in C++;
        // no micro-chunks are defined or read for AirStripGameObj.
    }
}
