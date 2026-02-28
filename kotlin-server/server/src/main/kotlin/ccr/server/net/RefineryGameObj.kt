package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.server.GameObjManager
import ccr.server.defs.OBBoxClass
import ccr.server.defs.RefineryGameObjDef
import ccr.server.defs.ZoneType

// C++: RefineryGameObj : public BuildingGameObj (refinerygameobj.h / refinerygameobj.cpp)
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj → BuildingGameObj → RefineryGameObj
class RefineryGameObj() : BuildingGameObj() {

    // C++: HarvesterClass* Harvester (initialized to NULL)
    // @JvmName avoids clash with fun getHarvester() / fun setHarvester() below
    @get:JvmName("harvesterField") @set:JvmName("setHarvesterField")
    var harvester: HarvesterClass? = null  // C++: HarvesterClass* Harvester

    // C++: OBBoxClass TiberiumField (initialized to Vector3(0,0,0), Vector3(0,0,0))
    var tiberiumField: OBBoxClass = OBBoxClass()  // C++: OBBoxClass TiberiumField

    // C++: bool IsHarvesterDocked (initialized to false)
    var isHarvesterDocked: Boolean = false  // C++: bool IsHarvesterDocked

    // C++: float UnloadTimer (initialized to 0)
    var unloadTimer: Float = 0f  // C++: float UnloadTimer

    // C++: int UnloadAnimationID (initialized to 0)
    var unloadAnimationID: Int = 0  // C++: int UnloadAnimationID

    // C++: float TotalFunds (initialized to 0)
    var totalFunds: Float = 0f  // C++: float TotalFunds

    // C++: float FundsPerSecond (initialized to 0)
    var fundsPerSecond: Float = 0f  // C++: float FundsPerSecond

    // C++: float DistributionTimer (initialized to 0)
    var distributionTimer: Float = 0f  // C++: float DistributionTimer

    // C++: Matrix3D DockTM (initialized to identity: Matrix3D(1))
    // @JvmName avoids clash with fun getDockTM() / fun setDockTM() below
    @get:JvmName("dockTMField") @set:JvmName("setDockTMField")
    var dockTM: Matrix3D = Matrix3D.IDENTITY  // C++: Matrix3D DockTM

    // C++: AudibleSoundClass* MoneyTrickleSound
    var moneyTrickleSound: AudibleSoundClass? = null  // C++: AudibleSoundClass* MoneyTrickleSound

    init {
        // C++: MoneyTrickleSound = WWAudioClass::Get_Instance()->Create_Sound("Money_Trickle")
        moneyTrickleSound = WWAudioClass.getInstance().createSound("Money_Trickle")
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
            name = "refinery_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: virtual ~RefineryGameObj()
    override fun destruct() {
        // C++: if (MoneyTrickleSound != NULL) { MoneyTrickleSound->Stop(); REF_PTR_RELEASE(MoneyTrickleSound); }
        val snd = moneyTrickleSound
        if (snd != null) {
            snd.stop()
            moneyTrickleSound = null
        }
        super.destruct()
    }

    // C++: virtual void Init(void) — implements BuildingGameObj abstract init()
    override fun init() {
        init(getRefineryDefinition())
    }

    // C++: void Init(const RefineryGameObjDef& definition)
    fun init(definition: RefineryGameObjDef) {
        // C++: BuildingGameObj::Init(definition)
        super.init(definition)
    }

    // C++: const RefineryGameObjDef& Get_Definition() const
    // Note: BuildingGameObj.getDefinition() is not open; this shadows it with a more specific return type.
    @Suppress("UNCHECKED_CAST")
    fun getRefineryDefinition(): RefineryGameObjDef = definition as RefineryGameObjDef

    // C++: RefineryGameObj* As_RefineryGameObj() { return this; }
    fun asRefineryGameObj(): RefineryGameObj = this

    // C++: bool Needs_Harvester() const { return (!IsDestroyed) && (Harvester == NULL); }
    fun needsHarvester(): Boolean = (!isDestroyed) && (harvester == null)

    // C++: int Get_Harvester_Def_ID() const { return Get_Definition().HarvesterDefID; }
    fun getHarvesterDefId(): Int = getRefineryDefinition().harvesterDefId

    // C++: HarvesterClass* Get_Harvester() { return Harvester; }
    fun getHarvester(): HarvesterClass? = harvester

    // C++: void Set_Harvester(HarvesterClass* harvester) { Harvester = harvester; }
    fun setHarvester(h: HarvesterClass?) { harvester = h }

    // C++: bool Get_Is_Harvester_Docked() { return IsHarvesterDocked; }
    // covered by var isHarvesterDocked

    // C++: const Matrix3D& Get_Dock_TM() const { return DockTM; }
    fun getDockTM(): Matrix3D = dockTM

    // C++: void Set_Dock_TM(const Matrix3D& tm) { DockTM = tm; }
    fun setDockTM(tm: Matrix3D) { dockTM = tm }

    // -------------------------------------------------------------------------
    // C++: void Set_Is_Harvester_Docked(bool flag)
    // -------------------------------------------------------------------------
    fun setIsHarvesterDocked(flag: Boolean) {
        isHarvesterDocked = flag

        // C++: if (CombatManager::I_Am_Server()) Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true)
        // (always server — omit guard per guide)
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    // -------------------------------------------------------------------------
    // C++: VehicleGameObj* Get_Harvester_Vehicle()
    // -------------------------------------------------------------------------
    fun getHarvesterVehicle(): VehicleGameObj? {
        // C++: if (Harvester != NULL) vehicle = Harvester->Get_Vehicle()
        return harvester?.getVehicle()
    }

    // -------------------------------------------------------------------------
    // C++: void Set_Harvester_Vehicle(VehicleGameObj* harvester)
    // -------------------------------------------------------------------------
    fun setHarvesterVehicle(vehicleGameObj: VehicleGameObj) {
        // C++: Vector3 dock_location = DockTM.Get_Translation()
        val dockLocation = dockTM.translation
        // C++: Vector3 dock_entrance = dock_location + (DockTM.Get_X_Vector() * 10.0F)
        // In row-major ccr.math.Matrix3D: X column vector = (m00, m10, m20)
        val xVec = Vector3(dockTM.m00, dockTM.m10, dockTM.m20)
        val dockEntrance = dockLocation + xVec * 10.0f

        // C++: Harvester = new HarvesterClass; configure; harvester->Add_Observer(Harvester); Initialize()
        val h = HarvesterClass()
        h.setRefinery(this)
        h.setDockLocation(dockLocation)
        h.setDockEntrance(dockEntrance)
        h.setTiberiumRegion(tiberiumField)
        h.setHarvestAnim("V_NOD_HARVESTER.V_NOD_HARVESTER")
        vehicleGameObj.addObserver(h)
        h.initialize()
        harvester = h
    }

    // -------------------------------------------------------------------------
    // C++: void On_Harvester_Docked()
    // -------------------------------------------------------------------------
    fun onHarvesterDocked() {
        // C++: UnloadTimer = Get_Definition().UnloadTime
        unloadTimer = getRefineryDefinition().unloadTime
        // C++: TotalFunds = Get_Definition().FundsGathered
        totalFunds = getRefineryDefinition().fundsGathered
        // C++: FundsPerSecond = TotalFunds / UnloadTimer
        fundsPerSecond = if (unloadTimer > 0f) totalFunds / unloadTimer else 0f

        // C++: Set_Is_Harvester_Docked(true)
        setIsHarvesterDocked(true)
    }

    // -------------------------------------------------------------------------
    // C++: void On_Harvester_Damaged(VehicleGameObj*)
    // -------------------------------------------------------------------------
    fun onHarvesterDamaged(harv: VehicleGameObj) {
        // C++: if (BaseController) BaseController->On_Vehicle_Damaged(harvester)
        // FIXME: BaseControllerClass.onVehicleDamaged() not yet ported
    }

    // -------------------------------------------------------------------------
    // C++: void On_Harvester_Destroyed(VehicleGameObj*)
    // -------------------------------------------------------------------------
    fun onHarvesterDestroyed(harv: VehicleGameObj) {
        // C++: if (BaseController) BaseController->On_Vehicle_Destroyed(harvester)
        // FIXME: BaseControllerClass.onVehicleDestroyed() not yet ported
    }

    // -------------------------------------------------------------------------
    // C++: void Play_Unloading_Animation(bool onoff)
    // -------------------------------------------------------------------------
    fun playUnloadingAnimation(onoff: Boolean) {
        // C++: StaticPhysClass* static_phys_obj = PhysicsSceneClass::Get_Instance()->Find_Static_Object(UnloadAnimationID)
        val staticPhysObj = CombatManager.getScene()?.findStaticObject(unloadAnimationID) ?: return
        val animPhysObj = staticPhysObj.asStaticAnimPhysClass() ?: return

        // C++: AnimCollisionManagerClass& anim_mgr = anim_phys_obj->Get_Animation_Manager()
        // C++: anim_mgr.Set_Animation_Mode(AnimCollisionManagerClass::ANIMATE_TARGET)
        val animMgr = animPhysObj.getAnimationManager()
        animMgr.setAnimationMode(AnimCollisionManagerClass.ANIMATE_TARGET)

        // C++: if (onoff) anim_mgr.Set_Target_Frame_End() else anim_mgr.Set_Target_Frame(0)
        if (onoff) {
            animMgr.setTargetFrameEnd()
        } else {
            animMgr.setTargetFrame(0)
        }
    }

    // -------------------------------------------------------------------------
    // C++: void Manage_Money_Trickle_Sound()
    // -------------------------------------------------------------------------
    fun manageMoneyTrickleSound() {
        val snd = moneyTrickleSound ?: return

        var sameTeam = false

        // C++: if (COMBAT_STAR != NULL && BaseController != NULL)
        //        same_team = (COMBAT_STAR->Get_Player_Type() == BaseController->Get_Player_Type())
        // FIXME: CombatManager::Get_The_Star() not yet ported
        val combatStar = CombatManager.getTheStar()
        val ctrl = baseController
        if (combatStar != null && ctrl != null) {
            sameTeam = (combatStar.playerType == ctrl.playerType)
        }

        if (sameTeam) {
            val playSound = !isDestroyed && (defenseObject.health > 0f) && isHarvesterDocked

            if (playSound) {
                if (!snd.isPlaying()) {
                    snd.play()
                }
            } else {
                if (snd.isPlaying()) {
                    snd.stop()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // C++: void CnC_Initialize(BaseControllerClass* base)
    // -------------------------------------------------------------------------
    override fun cncInitialize(base: BaseControllerClass) {
        // C++: BuildingGameObj::CnC_Initialize(base)
        super.cncInitialize(base)

        // C++: Vector3 pos; Get_Position(&pos)
        val pos = getPosition()

        // C++: find the team-specific tiberium zone, then fall back to any tiberium zone
        var zone: ScriptZoneGameObj? = null
        if (playerType == PLAYERTYPE_GDI) {
            zone = ScriptZoneGameObj.findClosestZone(pos, ZoneType.TYPE_GDI_TIB_FIELD)
        } else {
            zone = ScriptZoneGameObj.findClosestZone(pos, ZoneType.TYPE_NOD_TIB_FIELD)
        }

        if (zone == null) {
            zone = ScriptZoneGameObj.findClosestZone(pos, ZoneType.TYPE_TIBERIUM_FIELD)
        }

        if (zone != null) {
            // C++: TiberiumField = zone->Get_Bounding_Box()
            tiberiumField = zone.getBoundingBox()

            // C++: if (zone->Get_Observers().Count() == 0) zone->Set_Delete_Pending()
            if (zone.getObservers().isEmpty()) {
                zone.setDeletePending()
            }
        }

        // C++: Find the closest unloading static anim phys (name contains "REF_TIB_DUMP")
        var closest2 = 99999.0f
        val scene = CombatManager.getScene()
        if (scene != null) {
            for (obj in scene.getStaticObjects()) {
                val animPhysObj = obj.asStaticAnimPhysClass() ?: continue
                val model = animPhysObj.peekModel() ?: continue
                val name = model.getName().uppercase()
                if ("REF_TIB_DUMP" in name) {
                    val animPos = animPhysObj.getPosition()
                    val dx = animPos.x - pos.x
                    val dy = animPos.y - pos.y
                    val dz = animPos.z - pos.z
                    val dist2 = dx * dx + dy * dy + dz * dz
                    if (dist2 < closest2) {
                        closest2 = dist2
                        unloadAnimationID = animPhysObj.getId()
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // C++: void Think()
    // -------------------------------------------------------------------------
    override fun think() {
        // C++: Manage_Money_Trickle_Sound()
        manageMoneyTrickleSound()

        // C++: if (IsDestroyed == false && CombatManager::I_Am_Server())
        // (always server — omit guard per guide)
        if (!isDestroyed) {

            // C++: if (Harvester == NULL && DefenseObject.Get_Health() > 0.0f && CombatManager::Is_Gameplay_Permitted())
            if (harvester == null && defenseObject.health > 0f) {
                // C++: Set_Is_Harvester_Docked(false)
                setIsHarvesterDocked(false)
                // C++: BaseController->Request_Harvester(Get_Definition().HarvesterDefID)
                baseController?.requestHarvester(getRefineryDefinition().harvesterDefId)

            } else if (isHarvesterDocked) {

                // C++: float logical_seconds = TimeManager::Get_Frame_Seconds()
                var logicalSeconds = TimeManager.getFrameSeconds()
                // C++: logical_seconds /= BaseController->Get_Operation_Time_Factor()
                val ctrl = baseController
                if (ctrl != null) {
                    logicalSeconds /= ctrl.operationTimeFactor
                }

                // C++: int funds = int(logical_seconds * FundsPerSecond); funds = min(funds, (int)TotalFunds)
                var funds = (logicalSeconds * fundsPerSecond).toInt()
                funds = minOf(funds, totalFunds.toInt())
                println("[REFINERY] docked: logicalSeconds=$logicalSeconds fundsPerSecond=$fundsPerSecond funds=$funds totalFunds=$totalFunds baseController=${baseController != null}")
                if (funds > 0) {
                    totalFunds -= funds.toFloat()
                    // C++: BaseController->Distribute_Funds_To_Each_Teammate(funds)
                    baseController?.distributeFundsToEachTeammate(funds, GameObjManager.getStarList())
                }

                // C++: UnloadTimer -= logical_seconds
                unloadTimer -= logicalSeconds
                if (unloadTimer <= 0f) {

                    // C++: Deposit any remaining cash
                    if (totalFunds > 0f) {
                        // C++: BaseController->Distribute_Funds_To_Each_Teammate((int)TotalFunds)
                        baseController?.distributeFundsToEachTeammate(
                            totalFunds.toInt(), GameObjManager.getStarList()
                        )
                        totalFunds = 0f
                    }

                    // C++: Harvester->Go_Harvest()
                    harvester?.goHarvest()
                    // C++: Set_Is_Harvester_Docked(false)
                    setIsHarvesterDocked(false)
                    // C++: Play_Unloading_Animation(false)
                    playUnloadingAnimation(false)
                }
            }

            // C++: if (CombatManager::Is_Gameplay_Permitted() && IsHarvesterDocked == false)
            if (!isHarvesterDocked) {

                // C++: DistributionTimer -= TimeManager::Get_Frame_Seconds()
                distributionTimer -= TimeManager.getFrameSeconds()
                if (distributionTimer <= 0f) {
                    distributionTimer = 1.0f

                    // C++: float funds = Get_Definition().FundsDistributedPerSec / BaseController->Get_Operation_Time_Factor()
                    var funds = getRefineryDefinition().fundsDistributedPerSec
                    val ctrl = baseController
                    if (ctrl != null) {
                        funds /= ctrl.operationTimeFactor
                    }

                    // C++: BaseController->Distribute_Funds_To_Each_Teammate((int)funds)
                    val fundsInt = funds.toInt()
                    println("[REFINERY] periodic: funds=$fundsInt baseController=${baseController != null} stars=${GameObjManager.getStarList().size}")
                    baseController?.distributeFundsToEachTeammate(fundsInt, GameObjManager.getStarList())
                }
            }

            // C++: if (Harvester != NULL) Harvester->Think()
            harvester?.think()
        }

        // C++: BuildingGameObj::Think()
        super.think()
    }

    // -------------------------------------------------------------------------
    // C++: virtual void On_Destroyed()
    // -------------------------------------------------------------------------
    override fun onDestroyed() {
        // C++: BuildingGameObj::On_Destroyed()
        super.onDestroyed()

        // C++: if (Harvester != NULL) — kill the harvester
        if (harvester != null) {
            val harv = getHarvesterVehicle()
            if (harv != null) {
                // C++: OffenseObjectClass default_damager; harvy->Completely_Damaged(default_damager)
                val defaultDamager = OffenseObjectClass()
                harv.completelyDamaged(defaultDamager)
            }
        }

        // C++: Play_Unloading_Animation(false)
        playUnloadingAnimation(false)
    }

    // -------------------------------------------------------------------------
    // C++: virtual bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        // C++: csave.Begin_Chunk(CHUNKID_PARENT); BuildingGameObj::Save(csave); csave.End_Chunk()
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        // C++: csave.Begin_Chunk(CHUNKID_VARIABLES); WRITE_MICRO_CHUNK ...; csave.End_Chunk()
        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_TIBERIUM_FIELD,      tiberiumField)
        csave.writeMicroChunk(MICROCHUNKID_IS_HARVESTER_DOCKED, isHarvesterDocked)
        csave.writeMicroChunk(MICROCHUNKID_UNLOAD_TIMER,        unloadTimer)
        csave.writeMicroChunk(MICROCHUNKID_DOCK_TM,             dockTM)
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
                else -> error("Unrecognized RefineryGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass& cload)
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                MICROCHUNKID_TIBERIUM_FIELD      -> tiberiumField      = cload.readOBBox()
                MICROCHUNKID_IS_HARVESTER_DOCKED -> isHarvesterDocked  = cload.readBool()
                MICROCHUNKID_UNLOAD_TIMER        -> unloadTimer        = cload.readFloat()
                MICROCHUNKID_DOCK_TM             -> dockTM             = cload.readMatrix3D()
                else -> error("Unrecognized RefineryGameObj variable chunk ID: ${cload.curMicroChunkId}")
            }
            cload.closeMicroChunk()
        }
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Export_Rare(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun exportRare(packet: BitStream) {
        // C++: BuildingGameObj::Export_Rare(packet)
        super.exportRare(packet)

        // C++: packet.Add(IsHarvesterDocked)
        packet.addBool(isHarvesterDocked)
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Import_Rare(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun importRare(packet: BitStream) {
        // C++: BuildingGameObj::Import_Rare(packet)
        super.importRare(packet)

        // C++: packet.Get(IsHarvesterDocked)
        isHarvesterDocked = packet.getBool()
    }

    companion object {
        // C++: Save/Load chunk IDs from refinerygameobj.cpp local enum
        // enum { CHUNKID_PARENT = 0x0219043, CHUNKID_VARIABLES, ... }
        private const val CHUNKID_PARENT    = 0x0219043
        private const val CHUNKID_VARIABLES = 0x0219044

        // C++: Micro-chunk IDs inside CHUNKID_VARIABLES
        // MICROCHUNKID_TIBERIUM_FIELD       = 1
        // MICROCHUNKID_HARVESTER            = 2  (pointer, not saved in Kotlin)
        // MICROCHUNKID_IS_HARVESTER_DOCKED  = 3
        // MICROCHUNKID_UNLOAD_TIMER         = 4
        // XXXMICROCHUNKID_UNLOAD_DOOR_ID    = 5  (legacy, removed from C++ source)
        // MICROCHUNKID_DOCK_TM              = 6  (sequential after the removed entry)
        private const val MICROCHUNKID_TIBERIUM_FIELD       = 1
        // MICROCHUNKID_HARVESTER            = 2  // pointer — not saved in Kotlin
        private const val MICROCHUNKID_IS_HARVESTER_DOCKED  = 3
        private const val MICROCHUNKID_UNLOAD_TIMER         = 4
        // XXXMICROCHUNKID_UNLOAD_DOOR_ID    = 5  // legacy, removed
        private const val MICROCHUNKID_DOCK_TM              = 6
    }
}
