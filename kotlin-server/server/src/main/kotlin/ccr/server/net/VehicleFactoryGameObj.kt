package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.server.GameObjManager
import ccr.server.defs.OBBoxClass
import ccr.server.defs.VehicleFactoryGameObjDef
import ccr.server.defs.ZoneType

// C++: VehicleFactoryGameObj : public BuildingGameObj (vehiclefactorygameobj.h / vehiclefactorygameobj.cpp)
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//              → BuildingGameObj → VehicleFactoryGameObj
open class VehicleFactoryGameObj() : BuildingGameObj() {

    // C++: GameObjReference Vehicle (initialized to default GameObjReference)
    val vehicle: GameObjReference = GameObjReference()

    // C++: Matrix3D CreationTM (initialized to Matrix3D(1) — identity)
    var creationTm: Matrix3D = Matrix3D()

    // C++: OBBoxClass GeneratingRegion (initialized to Vector3(0,0,0), Vector3(0,0,0))
    var generatingRegion: OBBoxClass = OBBoxClass()

    // C++: float GenerationTime (initialized to 0)
    var generationTime: Float = 0f

    // C++: int GeneratingVehicleID (initialized to 0)
    var generatingVehicleID: Int = 0

    // C++: bool IsBusy (initialized to false)
    var isBusy: Boolean = false

    // C++: GameObjReference Purchaser (initialized to default GameObjReference)
    val purchaser: GameObjReference = GameObjReference()

    // C++: int LastDeliveryPath (initialized to 0)
    var lastDeliveryPath: Int = 0

    // C++: float EndTimer (initialized to UNINITIALIZED_TIMER)
    var endTimer: Float = UNINITIALIZED_TIMER

    init {
        // C++: VehicleFactoryGameObj() — no init body beyond initializer list
    }

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: Vector3 = Vector3(),
        sphereCenter: Vector3 = Vector3(),
        sphereRadius: Float = 50f,
        health: Float = 0f,
        isDestroyed: Boolean = false,
        isPowerOn: Boolean = true,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "vehiclefactory_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: virtual ~VehicleFactoryGameObj()
    override fun destruct() {
        vehicle.destruct()
        purchaser.destruct()
        super.destruct()
    }

    // C++: virtual void Init(void)
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const VehicleFactoryGameObjDef& definition)
    fun init(definition: VehicleFactoryGameObjDef) {
        // C++: BuildingGameObj::Init(definition)
        super.init(definition)
    }

    // C++: const VehicleFactoryGameObjDef& Get_Definition() const
    override open fun getDefinition(): VehicleFactoryGameObjDef = definition as VehicleFactoryGameObjDef

    // C++: VehicleFactoryGameObj* As_VehicleFactoryGameObj() { return this; }
    fun asVehicleFactoryGameObj(): VehicleFactoryGameObj = this

    // C++: virtual void CnC_Initialize(BaseControllerClass* base)
    override fun cncInitialize(base: BaseControllerClass) {
        // C++: BuildingGameObj::CnC_Initialize(base)
        super.cncInitialize(base)

        // C++: Get_Position(&pos); zone = ScriptZoneGameObj::Find_Closest_Zone(pos, ZoneConstants::TYPE_VEHICLE_CONSTRUCTION)
        val pos = getPosition()
        val zone = ScriptZoneGameObj.findClosestZone(pos, ZoneType.TYPE_VEHICLE_CONSTRUCTION)
        if (zone != null) {
            // C++: GeneratingRegion = zone->Get_Bounding_Box()
            generatingRegion = zone.getBoundingBox()

            // C++: if (zone->Get_Observers().Count() == 0) zone->Set_Delete_Pending()
            if (zone.getObservers().isEmpty()) {
                zone.setDeletePending()
            }
        }

        // C++: if (BaseController != NULL) BaseController->Set_Can_Generate_Vehicles(true)
        baseController?.setCanGenerateVehicles(true)
    }

    // C++: virtual void Think()
    override fun think() {
        // C++: if (EndTimer > UNITIALIZED_TIMER) { EndTimer -= ...; if (EndTimer < 0) { On_Generation_Complete(); EndTimer = UNITIALIZED_TIMER; } }
        if (endTimer > UNINITIALIZED_TIMER) {
            endTimer -= TimeManager.getFrameSeconds()
            if (endTimer < 0f) {
                onGenerationComplete()
                endTimer = UNINITIALIZED_TIMER
            }
        }

        // C++: BuildingGameObj::Think()
        super.think()
    }

    // C++: bool Is_Available() const { return (IsBusy == false) && (IsDestroyed == false); }
    fun isAvailable(): Boolean = !isBusy && !isDestroyed

    // C++: bool Is_Available_For_Purchase() const
    // In addition to checking factory busy/destroyed, checks team vehicle count vs max
    fun isAvailableForPurchase(): Boolean {
        val teamVehicleCount = getTeamVehicleCount()
        return isAvailable() && (teamVehicleCount < maxVehiclesPerTeam)
    }

    // C++: bool Is_Busy() const { return IsBusy; }
    // → covered by var isBusy

    // C++: int Get_Team_Vehicle_Count() const
    // Counts the number of vehicles active for this factory's team (not including the harvester)
    fun getTeamVehicleCount(): Int {
        var teamVehicleCount = 0

        // C++: VehicleGameObj* harvy = NULL; BaseControllerClass* base = BaseControllerClass::Find_Base(Get_Player_Type())
        // C++: if (base != NULL) { harvy = base->Get_Harvester_Vehicle(); }
        // FIXME: BaseControllerClass.getHarvesterVehicle() not yet ported — harvester excluded from count when available
        val harvester: VehicleGameObj? = null

        // C++: for each SmartGameObj in the smart list — count VehicleGameObjs of our team
        // C++: GameObjManager::Get_Smart_Game_Obj_List()
        for (obj in GameObjManager.getAllObjects()) {
            val vehicleObj = obj as? VehicleGameObj ?: continue

            // C++: obj != harvy — skip the harvester
            if (vehicleObj === harvester) continue

            // C++: obj->Peek_Physical_Object()->As_VehiclePhysClass() != NULL — only wheeled/tracked vehicles
            // C++: obj->Get_Definition().Get_Default_Player_Type() == Get_Player_Type()
            // In the Kotlin server, VehicleGameObj.playerType mirrors the definition's defaultPlayerType
            if (vehicleObj.playerType == playerType) {
                teamVehicleCount++
            }
        }
        return teamVehicleCount
    }

    // C++: void On_Destroyed()
    override fun onDestroyed() {
        // C++: BuildingGameObj::On_Destroyed()
        super.onDestroyed()

        // C++: if (BaseController != NULL && CombatManager::I_Am_Server()) BaseController->Set_Can_Generate_Vehicles(false)
        // (always server — omit guard per guide)
        baseController?.setCanGenerateVehicles(false)
    }

    // Test-friendly overload — accepts a buyer rhost ID (int) instead of a SoldierGameObj reference.
    // Sets isBusy directly without calling getDefinition() (which crashes for synthetic defs).
    fun requestVehicle(definitionId: Int, generationTime: Float, buyerRhostId: Int): Boolean {
        if (!isBusy && definitionId != 0) {
            generatingVehicleID  = definitionId
            this.generationTime  = generationTime
            isBusy               = true
            // purchaser not set — intentional for test-only path
            return true
        }
        return false
    }

    // C++: bool Request_Vehicle(int definition_id, float generation_time, SoldierGameObj* purchaser = NULL)
    fun requestVehicle(definitionId: Int, generationTime: Float, purchaserObj: SoldierGameObj? = null): Boolean {
        var retval = false

        // C++: if (IsBusy == false && definition_id != 0)
        if (!isBusy && definitionId != 0) {
            generatingVehicleID  = definitionId
            this.generationTime  = generationTime
            isBusy               = true
            purchaser.set(purchaserObj)
            // C++: EndTimer = Get_Definition().Get_Total_Building_Time()
            endTimer             = getDefinition().totalBuildingTime

            // C++: Begin_Generation()
            beginGeneration()

            setObjectDirtyBit(NetworkObject.BIT_RARE, true)
            retval = true
        }

        return retval
    }

    // C++: const Matrix3D& Get_Creation_TM() { return CreationTM; }
    // → covered by var creationTm

    // C++: void Set_Creation_TM(const Matrix3D& tm) { CreationTM = tm; }
    fun setCreationTM(tm: Matrix3D) { creationTm = tm }

    // C++: void Export_Rare(BitStreamClass& packet)
    override fun exportRare(packet: BitStream) {
        // C++: BuildingGameObj::Export_Rare(packet)
        super.exportRare(packet)

        // C++: packet.Add(IsBusy)
        packet.addBool(isBusy)
    }

    // C++: void Import_Rare(BitStreamClass& packet)
    override fun importRare(packet: BitStream) {
        // C++: BuildingGameObj::Import_Rare(packet)
        super.importRare(packet)

        // C++: packet.Get(IsBusy)
        isBusy = packet.getBool()
    }

    // C++: static void Set_Max_Vehicles_Per_Team(int max) { assert(max > 0); MaxVehiclesPerTeam = max; }
    // C++: static int Get_Max_Vehicles_Per_Team() { return MaxVehiclesPerTeam; }
    // → covered by companion object

    // -------------------------------------------------------------------------
    // C++: virtual bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        // C++: csave.Begin_Chunk(CHUNKID_PARENT); BuildingGameObj::Save(csave); csave.End_Chunk()
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        // C++: csave.Begin_Chunk(CHUNKID_VARIABLES); ...micros...; csave.End_Chunk()
        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_CREATION_TM,            creationTm)
        csave.writeMicroChunk(MICROCHUNKID_IS_BUSY,                isBusy)
        csave.writeMicroChunk(MICROCHUNKID_GENERATION_TIME,        generationTime)
        csave.writeMicroChunk(MICROCHUNKID_GENERATING_VEHICLE_ID,  generatingVehicleID)
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
                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_CREATION_TM           -> creationTm           = cload.readMatrix3D()
                            MICROCHUNKID_IS_BUSY               -> isBusy               = cload.readBool()
                            MICROCHUNKID_GENERATION_TIME       -> generationTime        = cload.readFloat()
                            MICROCHUNKID_GENERATING_VEHICLE_ID -> generatingVehicleID   = cload.readInt()
                            else -> error("Unrecognized VehicleFactoryGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                else -> error("Unrecognized VehicleFactoryGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Protected methods
    // -------------------------------------------------------------------------

    // C++: virtual void Destroy_Blocking_Objects()
    protected open fun destroyBlockingObjects() {
        // C++: if (CombatManager::I_Am_Server() == false) return (always server — omit guard)

        // C++: NonRefPhysListClass objs_to_kill; PhysicsSceneClass::Get_Instance()->Collect_Objects(GeneratingRegion, false, true, &objs_to_kill)
        // FIXME: PhysicsSceneClass::Get_Instance()->Collect_Objects — physics scene dynamic object collection not yet wired
        // When wired: for each PhysClass in objs_to_kill, get observer as CombatPhysObserverClass, cast to PhysicalGameObj,
        // skip if == Vehicle, skip if As_ArmedGameObj() == NULL, then apply 10000 damage with pad-clearing warhead.
    }

    // C++: void Deliver_Vehicle()
    // Gives the vehicle an action to follow one of the delivery waypaths out of the construction zone.
    protected fun deliverVehicle() {
        // C++: PathfindClass* pathfind = PathfindClass::Get_Instance()
        val pathfind = PathfindClass.getInstance() ?: return

        // C++: AABoxClass box; box.Center = GeneratingRegion.Center; box.Extent = GeneratingRegion.Extent
        val box = generatingRegion.toAABox()

        // C++: WaypathClass* path = pathfind->Get_Waypath_Starting_In_Box(box, LastDeliveryPath)
        val path = pathfind.getWaypathStartingInBox(box, lastDeliveryPath) ?: return

        // C++: LastDeliveryPath = (LastDeliveryPath + 1) % pathfind->Count_Waypaths_Starting_In_Box(box)
        lastDeliveryPath = (lastDeliveryPath + 1) % pathfind.countWaypathsStartingInBox(box)

        val vehicleObj = vehicle.get()?.let { it as? VehicleGameObj } ?: return
        val action = vehicleObj.action ?: return

        // C++: action->Goto(parameters) — drive vehicle off pad along waypath
        val parameters = ActionParamsStruct()
        parameters.priority   = 1
        parameters.waypathId  = path.getId()
        action.goto(parameters)

        // C++: vehicle->Set_Vehicle_Delivered()
        vehicleObj.setVehicleDelivered()

        // C++: BaseController->On_Vehicle_Delivered(vehicle)
        baseController?.onVehicleDelivered(vehicleObj)
    }

    // C++: VehicleGameObj* Create_Vehicle()
    protected fun createVehicle(): VehicleGameObj? {
        // C++: if (CombatManager::I_Am_Server() == false) return NULL (always server — omit guard)

        if (generatingVehicleID == 0) return null

        // C++: PhysicalGameObj* physical_obj = ObjectLibraryManager::Create_Object(GeneratingVehicleID)
        // Kotlin server: delegate to God.createHarvester / God.createVehicle instead of
        // ObjectLibraryManager::Create_Object (which is a stub returning null).
        val god = baseController?.network?.god ?: return null
        val spawnPosition = creationTm.translation

        val purchaserSoldier = purchaser.get() as? SoldierGameObj
        val vehicleObj: VehicleGameObj? = if (purchaserSoldier == null) {
            // No purchaser — harvester spawned by BaseControllerClass::Request_Harvester
            god.createHarvester(playerType, generatingVehicleID, spawnPosition)
        } else {
            // Player-purchased vehicle — find the buyer's rhostId by reverse-lookup in soldiersByHost
            val buyerRhostId = god.soldiersByHost.entries
                .firstOrNull { it.value === purchaserSoldier }?.key ?: -1
            god.createVehicle(buyerRhostId, generatingVehicleID, spawnPosition)
        } ?: return null

        // C++: Vehicle = vehicle
        vehicle.set(vehicleObj)

        return vehicleObj
    }

    // C++: virtual void Begin_Generation() {} — empty default, overridden by subclasses (e.g. AirStripGameObj)
    protected open fun beginGeneration() {}

    // C++: virtual void On_Generation_Complete()
    protected open fun onGenerationComplete() {
        // C++: if (Vehicle != NULL)
        val vehicleObj = vehicle.get()?.let { it as? VehicleGameObj }
        if (vehicleObj != null) {
            // C++: Shut off the action making the vehicle drive off the pad
            val actionObj = vehicleObj.action
            if (actionObj != null) {
                // C++: action->Get_Parameters().Set_Movement(Vector3(0,0,0), 0.0f, 1000.0f)
                // Change arrived distance so vehicles think they've arrived (complete the action)
                actionObj.getParameters().setMovement(Vector3(0f, 0f, 0f), 0.0f, 1000.0f)
            }

            // C++: BaseController->On_Vehicle_Generated(vehicle_obj)
            baseController?.onVehicleGenerated(vehicleObj)
        }

        // C++: Reset variables
        generatingVehicleID = 0
        generationTime       = 0f
        isBusy               = false
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    companion object {
        // C++: const float UNITIALIZED_TIMER = -100.0F
        const val UNINITIALIZED_TIMER = -100.0f

        // C++: const int DEFAULT_MAX_VEHICLES_PER_TEAM = 8
        private const val DEFAULT_MAX_VEHICLES_PER_TEAM = 8

        // C++: static int MaxVehiclesPerTeam = DEFAULT_MAX_VEHICLES_PER_TEAM
        // @JvmName avoids clash with fun setMaxVehiclesPerTeam() below
        @get:JvmName("maxVehiclesPerTeamField") @set:JvmName("setMaxVehiclesPerTeamField")
        var maxVehiclesPerTeam: Int = DEFAULT_MAX_VEHICLES_PER_TEAM

        // C++: static void Set_Max_Vehicles_Per_Team(int max) { assert(max > 0); MaxVehiclesPerTeam = max; }
        fun setMaxVehiclesPerTeam(max: Int) {
            require(max > 0) { "max vehicles per team must be positive" }
            maxVehiclesPerTeam = max
        }

        // -------------------------------------------------------------------------
        // Save/Load chunk IDs (vehiclefactorygameobj.cpp local enum)
        // -------------------------------------------------------------------------
        // C++: enum { CHUNKID_PARENT = 0x0219043, CHUNKID_VARIABLES, ... }
        // Note: 0x0219043 = 0x00219043
        private const val CHUNKID_PARENT    = 0x00219043
        private const val CHUNKID_VARIABLES = 0x00219044

        // C++: MICROCHUNKID_CREATION_TM = 1, IS_BUSY = 2, GENERATION_TIME = 3, GENERATING_VEHICLE_ID = 4
        private const val MICROCHUNKID_CREATION_TM            = 1
        private const val MICROCHUNKID_IS_BUSY                = 2
        private const val MICROCHUNKID_GENERATION_TIME        = 3
        private const val MICROCHUNKID_GENERATING_VEHICLE_ID  = 4
    }
}
