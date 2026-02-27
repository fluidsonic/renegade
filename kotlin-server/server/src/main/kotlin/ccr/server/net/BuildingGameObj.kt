package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import ccr.server.GameObjManager
import ccr.server.defs.BuildingGameObjDef
import ccr.server.level.Sphere

// C++: BuildingGameObj : public DamageableGameObj, public CombatPhysObserverClass
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj → BuildingGameObj
// NOTE: BuildingGameObj does NOT extend PhysicalGameObj — it has its own Position and CollectionSphere.
//
// CombatPhysObserverClass is the observer interface for static physics objects (meshes, aggregates).
// The server needs this for damage routing (Apply_Damage_Building) and state management.
open class BuildingGameObj() : DamageableGameObj(), CombatPhysObserverClass {

    // C++: Vector3 Position (initialized to 0,0,0)
    // @JvmName avoids clash with override fun getPosition() / fun setPosition() below
    @get:JvmName("positionField") @set:JvmName("setPositionField")
    var position: Vector3 = Vector3(0f, 0f, 0f)

    // C++: bool IsPowerOn (initialized to true)
    var isPowerOn: Boolean = true

    // C++: int CurrentState (initialized to -1, then Update_State() called in constructor)
    var currentState: Int = -1

    // C++: AudibleSoundClass* CurrentAnnouncement (initialized to NULL)
    var currentAnnouncement: AudibleSoundClass? = null

    // C++: SphereClass AnnouncementSphere (initialized to center=(0,0,0), radius=1.0f)
    var announcementSphere: Sphere = Sphere(Vector3(0f, 0f, 0f), 1.0f)

    // C++: SphereClass CollectionSphere (initialized to center=(0,0,0), radius=50.0f)
    var collectionSphere: Sphere = Sphere(Vector3(0f, 0f, 0f), 50.0f)

    // Convenience: expose sphere radius for GameObjManager.updateBuildingCollectionSpheres
    var sphereRadius: Float
        get() = collectionSphere.radius
        set(value) { collectionSphere = Sphere(collectionSphere.center, value) }

    // C++: RefMultiListClass<StaticPhysClass> InteriorMeshes
    val interiorMeshes: MutableList<StaticPhysClass> = mutableListOf()

    // C++: RefMultiListClass<StaticPhysClass> ExteriorMeshes
    val exteriorMeshes: MutableList<StaticPhysClass> = mutableListOf()

    // C++: RefMultiListClass<BuildingAggregateClass> Aggregates
    val aggregates: MutableList<BuildingAggregateClass> = mutableListOf()

    // C++: RefMultiListClass<LightPhysClass> PowerOnLights
    val powerOnLights: MutableList<LightPhysClass> = mutableListOf()

    // C++: RefMultiListClass<LightPhysClass> PowerOffLights
    val powerOffLights: MutableList<LightPhysClass> = mutableListOf()

    // C++: BuildingMonitorClass* BuildingMonitor (initialized to NULL)
    protected var buildingMonitor: BuildingMonitorClass? = null

    // C++: BaseControllerClass* BaseController (initialized to NULL)
    var baseController: BaseControllerClass? = null

    // C++: bool IsDestroyed (initialized to false)
    var isDestroyed: Boolean = false

    // MCT alternate armor type save ID (from BuildingGameObjDef.mctSkin).
    // Used by C4GameObj.detonate() when stuckMct = true.
    var mctSkinSaveId: Int = 0

    init {
        // C++: GameObjManager::Add_Building(this)
        GameObjManager.addBuilding(this)
        // C++: Update_State() — called in constructor body after initializer list
        updateState()
        // C++: Set_App_Packet_Type(APPPACKETTYPE_BUILDING)
        appPacketType = APPPACKETTYPE_BUILDING.toByte()
    }

    // Secondary constructor for tests and server-side level loading — bypasses the full Init() pipeline.
    constructor(
        definitionId: Int,
        position: Vector3 = Vector3(0f, 0f, 0f),
        sphereCenter: Vector3 = Vector3(0f, 0f, 0f),
        sphereRadius: Float = 50f,
        health: Float = 0f,
        isDestroyed: Boolean = false,
        isPowerOn: Boolean = true,
        currentState: Int = 0,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "building_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        this.currentState = currentState
        defenseObject.health = health
    }

    // C++: virtual ~BuildingGameObj()
    override fun destruct() {
        // C++: Stop_Current_Announcement()
        stopCurrentAnnouncement()

        // C++: if (BuildingMonitor != NULL) { Remove_Observer(BuildingMonitor); delete BuildingMonitor; BuildingMonitor = NULL; }
        buildingMonitor?.let {
            removeObserver(it)
            buildingMonitor = null
        }

        // C++: GameObjManager::Remove_Building(this)
        GameObjManager.removeBuilding(this)
        // C++: Reset_Components()
        resetComponents()

        super.destruct()
    }

    // C++: virtual void Init(void) — must implement BaseGameObj abstract init()
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const BuildingGameObjDef& definition)
    fun init(definition: BuildingGameObjDef) {
        super.init(definition)
        mctSkinSaveId = definition.mctSkin
    }

    // C++: const BuildingGameObjDef& Get_Definition() const
    open fun getDefinition(): BuildingGameObjDef = definition as BuildingGameObjDef

    // C++: virtual BuildingGameObj* As_BuildingGameObj() { return this; }
    // C++: virtual PowerPlantGameObj* As_PowerPlantGameObj() { return NULL; } etc.
    // → handled by Kotlin type system: `this is PowerPlantGameObj`, etc.

    // C++: void Get_Position(Vector3* pos) const { *pos = Position; }
    override fun getPosition(): Vector3 = position

    // C++: void Set_Position(const Vector3& pos) { Position = pos; CollectionSphere.Center = pos; }
    fun setPosition(pos: Vector3) {
        position = pos
        collectionSphere = Sphere(pos, collectionSphere.radius)
    }

    // C++: virtual bool Get_World_Position(Vector3& pos) const { pos = Position; return true; }
    override fun getWorldPosition(): Vector3 = position

    // C++: void Get_Collection_Sphere / Set_Collection_Sphere — covered by var collectionSphere

    // -------------------------------------------------------------------------
    // C++: virtual void Apply_Damage(const OffenseObjectClass& damager, float scale=1.0, int alternate_skin=-1)
    // -------------------------------------------------------------------------
    override fun applyDamage(damager: OffenseObjectClass, scale: Float, alternateSkin: Int) {
        // C++: if (DefenseObject.Get_Health() <= 0) return;
        if (defenseObject.health <= 0f) return

        // C++: if (!CombatManager::Is_Gameplay_Permitted()) return;
        // (always permitted on server — omit guard per guide)

        // C++: if (!CanRepairBuildings && DefenseObject.Is_Repair(damager)) return;
        if (!canRepairBuildings && defenseObject.isRepair(damager)) return

        val oldHealth = defenseObject.health

        // C++: DamageableGameObj::Apply_Damage(damager, scale, alternate_skin)
        super.applyDamage(damager, scale, alternateSkin)

        // C++: if (old_health != Get_Defense_Object()->Get_Health()) Set_Object_Dirty_Bit(BIT_OCCASIONAL, true)
        if (oldHealth != defenseObject.health) {
            setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
        }

        // C++: Stats — if health <= 0, record building destroyed stat for killing soldier
        if (defenseObject.health <= 0f) {
            val owner = damager.owner
            if (owner is SoldierGameObj) {
                owner.playerData?.statsAddBuildingDestroyed()
            }

            // C++: EncyclopediaMgrClass::Reveal_Object(this)
            // FIXME: EncyclopediaMgrClass::Reveal_Object not yet ported
        }

        // C++: Update_State()
        updateState()
    }

    // C++: void Apply_Damage_Building(const OffenseObjectClass& offense, StaticPhysClass* component)
    fun applyDamageBuilding(offense: OffenseObjectClass, component: StaticPhysClass) {
        // C++: ArmorType skin = DefenseObject.Get_Skin();
        var skin = defenseObject.skin
        // C++: if MCT BuildingAggregate, use MCTSkin
        if (component.isFromFactory(PHYSICS_CHUNKID_BUILDINGAGGREGATE)) {
            if ((component as? BuildingAggregateClass)?.isMct() == true) {
                skin = getDefinition().mctSkin
            }
        }
        // C++: Apply_Damage(offense, 1.0, skin)
        applyDamage(offense, 1.0f, skin)
    }

    // C++: void Apply_Damage_Building(const OffenseObjectClass& offense, bool mct_damage)
    fun applyDamageBuilding(offense: OffenseObjectClass, mctDamage: Boolean) {
        var skin = defenseObject.skin
        if (mctDamage) {
            skin = getDefinition().mctSkin
        }
        applyDamage(offense, 1.0f, skin)
    }

    // C++: void Set_Normalized_Health(float health)
    fun setNormalizedHealth(health: Float) {
        // C++: DefenseObject.Set_Health(health * DefenseObject.Get_Health_Max())
        defenseObject.setHealth(health * defenseObject.healthMax)
        updateState()
    }

    // C++: void Enable_Power(bool onoff)
    fun enablePower(onoff: Boolean) {
        // C++: if (IsPowerOn != onoff) — notify observers BEFORE setting IsPowerOn
        if (isPowerOn != onoff) {
            // C++: observer_list[index]->Custom(this, CUSTOM_EVENT_BUILDING_POWER_CHANGED, onoff, NULL)
            for (observer in observers.toList()) {
                observer.custom(this, CUSTOM_EVENT_BUILDING_POWER_CHANGED, if (onoff) 1 else 0, null)
            }
        }

        isPowerOn = onoff
        updateState()

        // C++: if (CombatManager::I_Am_Server()) Set_Object_Dirty_Bit(BIT_RARE, true)
        // (always server — omit guard per guide)
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    // C++: bool Is_Power_Enabled() const { return IsPowerOn; }
    // → covered by var isPowerOn

    // -------------------------------------------------------------------------
    // C++: void Update_State(bool force_update = false)
    // -------------------------------------------------------------------------
    fun updateState(forceUpdate: Boolean = false) {
        val healthPercentage = if (defenseObject.healthMax > 0f) {
            100.0f * defenseObject.health / defenseObject.healthMax
        } else {
            0f
        }
        val healthState = percentageToHealthState(healthPercentage)
        val newState = composeState(healthState, isPowerOn)

        if ((newState != currentState) || forceUpdate) {
            val oldHealthState = getHealthState(currentState)

            if (healthState != oldHealthState) {
                // C++: CUSTOM_EVENT_BUILDING_DAMAGED / CUSTOM_EVENT_BUILDING_REPAIRED
                val event = if (healthState < oldHealthState) {
                    CUSTOM_EVENT_BUILDING_REPAIRED
                } else {
                    CUSTOM_EVENT_BUILDING_DAMAGED
                }
                for (observer in observers.toList()) {
                    observer.custom(this, event, healthPercentage.toInt(), null)
                }
            }

            currentState = newState

            // C++: if (CombatManager::I_Am_Server()) Set_Object_Dirty_Bit(BIT_RARE, true)
            // (always server — omit guard per guide)
            setObjectDirtyBit(NetworkObject.BIT_RARE, true)

            // C++: inform all aggregates of the new state
            for (aggregate in aggregates) {
                aggregate.setCurrentState(currentState, forceUpdate)
            }

            // C++: interior meshes: alternate materials if power off OR health==0
            val enableInteriorAlt = (healthState == HEALTH_0) || !isPowerOn
            enableAlternateMaterials(interiorMeshes, enableInteriorAlt)

            // C++: exterior meshes: alternate materials if destroyed
            enableAlternateMaterials(exteriorMeshes, healthState == HEALTH_0)

            // C++: Lights — power-on lights disabled when powered off or destroyed (only if power-off lights exist)
            val disablePowerOnLights = (!isPowerOn || healthState == HEALTH_0) && powerOffLights.isNotEmpty()
            for (light in powerOnLights) {
                light.setDisabled(disablePowerOnLights)
            }
            val disablePowerOffLights = !disablePowerOnLights
            for (light in powerOffLights) {
                light.setDisabled(disablePowerOffLights)
            }

            // C++: PhysicsSceneClass::Get_Instance()->Invalidate_Lighting_Caches(light_bounds)
            // FIXME: Invalidate_Lighting_Caches not yet ported
        }
    }

    // -------------------------------------------------------------------------
    // C++: virtual void CnC_Initialize(BaseControllerClass* base)
    // -------------------------------------------------------------------------
    open fun cncInitialize(base: BaseControllerClass) {
        baseController = base

        // C++: BuildingMonitor = new BuildingMonitorClass; BuildingMonitor->Set_Building(this); Add_Observer(BuildingMonitor)
        buildingMonitor = BuildingMonitorClass(this)
        addObserver(buildingMonitor!!)
    }

    // C++: virtual void On_Destroyed()
    open fun onDestroyed() {
        isDestroyed = true

        // C++: if (BaseController != NULL) BaseController->On_Building_Destroyed(this)
        baseController?.onBuildingDestroyed(this)

        // C++: if (CombatManager::I_Am_Server()) Set_Object_Dirty_Bit(BIT_RARE, true)
        // (always server — omit guard per guide)
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)

        // C++: EncyclopediaMgrClass::Reveal_Object(this)
        // FIXME: EncyclopediaMgrClass::Reveal_Object not yet ported
    }

    // C++: virtual void On_Damaged()
    open fun onDamaged() {
        // C++: if (BaseController != NULL) BaseController->On_Building_Damaged(this)
        baseController?.onBuildingDamaged(this)
    }

    // C++: bool Is_Destroyed() const { return IsDestroyed; }
    // → covered by var isDestroyed

    // C++: bool Is_GDI() { return Get_Player_Type() == PLAYERTYPE_GDI; }
    fun isGdi(): Boolean = playerType == PLAYERTYPE_GDI

    // C++: bool Is_Nod() { return Get_Player_Type() == PLAYERTYPE_NOD; }
    fun isNod(): Boolean = playerType == PLAYERTYPE_NOD

    // C++: virtual bool Is_Tagged() { return false; }
    override fun isTagged(): Boolean = false

    // -------------------------------------------------------------------------
    // C++: void Play_Announcement(int text_id, bool broadcast)
    // -------------------------------------------------------------------------
    fun playAnnouncement(textId: Int, broadcast: Boolean) {
        stopCurrentAnnouncement()

        // C++: TDBObjClass* translate_obj = TranslateDBClass::Find_Object(text_id)
        val translateObj = TranslateDBClass.findObject(textId) ?: return

        val string = translateObj.getString()
        val soundDefId = translateObj.getSoundId()
        var duration = 2.0f

        var displayText = true

        if (soundDefId > 0) {
            // C++: CurrentAnnouncement = WWAudioClass::Get_Instance()->Create_Sound(sound_def_id)
            currentAnnouncement = WWAudioClass.getInstance().createSound(soundDefId)
            currentAnnouncement?.let { snd ->
                duration = snd.getDuration() / 1000.0f

                if (!broadcast) {
                    snd.setDropOffRadius(announcementSphere.radius)
                    snd.asSound3DClass()?.setMaxVolRadius(announcementSphere.radius * 0.9f)
                    snd.setTransform(announcementSphere.center)
                    displayText = !snd.isSoundCulled()
                }

                snd.addToScene()
            }
        }

        // C++: if (display_text && string) Add message to window
        if (displayText && string != null) {
            val messageDuration = maxOf(duration, 5.0f)
            CombatManager.getMessageWindow().addMessage(string, Vector3(1f, 1f, 1f), null, messageDuration)
        }
    }

    // C++: void Stop_Current_Announcement()
    fun stopCurrentAnnouncement() {
        currentAnnouncement?.let {
            it.stop()
            it.removeFromScene()
            currentAnnouncement = null
        }
    }

    // -------------------------------------------------------------------------
    // C++: bool Name_Prefix_Matches_This_Building(const char* name)
    // -------------------------------------------------------------------------
    fun namePrefixMatchesThisBuilding(name: String): Boolean {
        val prefix = getDefinition().meshPrefix
        if (prefix.isEmpty()) return false
        val dotIdx = name.indexOf('.')
        val meshName = if (dotIdx >= 0) name.substring(dotIdx + 1) else name
        return meshName.startsWith(prefix, ignoreCase = true)
    }

    // C++: static bool Is_Interior_Mesh_Name(const char* name) — return strchr(name,'#') != NULL
    fun isInteriorMeshName(name: String): Boolean = '#' in name

    // C++: static bool Is_Exterior_Mesh_Name(const char* name) — return strchr(name,'^') != NULL
    fun isExteriorMeshName(name: String): Boolean = '^' in name

    // C++: const char* Get_Name_Prefix() { return Get_Definition().MeshPrefix; }
    fun getNamePrefix(): String = getDefinition().meshPrefix

    // -------------------------------------------------------------------------
    // C++: void Collect_Building_Components()
    // -------------------------------------------------------------------------
    fun collectBuildingComponents() {
        resetComponents()

        // C++: Iterate all static objects in the scene
        val scene = CombatManager.getScene()
        if (scene != null) {
            for (obj in scene.getStaticObjects()) {
                val staticPhys = obj.asStaticPhysClass() ?: continue
                val model = staticPhys.peekModel() ?: continue
                val objName = model.getName()

                if (staticPhys.isFromFactory(PHYSICS_CHUNKID_BUILDINGAGGREGATE)) {
                    // Aggregate
                    if (namePrefixMatchesThisBuilding(objName)) {
                        val objPos = model.getBoundingBox().center
                        val dist2 = (collectionSphere.center - objPos).length2()
                        val radius2 = collectionSphere.radius * collectionSphere.radius

                        if (dist2 <= radius2) {
                            val existingBuilding = (obj.getObserver() as? CombatPhysObserverClass) as? BuildingGameObj
                            if (existingBuilding != null) {
                                val existingDist2 = (existingBuilding.collectionSphere.center - objPos).length2()
                                if (dist2 < existingDist2) {
                                    existingBuilding.removeAggregate(obj as BuildingAggregateClass)
                                    addAggregate(obj as BuildingAggregateClass)
                                }
                            } else {
                                addAggregate(obj as BuildingAggregateClass)
                            }
                        }
                    }
                } else if (isInteriorMeshName(objName) || isExteriorMeshName(objName)) {
                    // Mesh
                    if (namePrefixMatchesThisBuilding(objName)) {
                        val objPos = model.getBoundingBox().center
                        val dist2 = (collectionSphere.center - objPos).length2()
                        val radius2 = collectionSphere.radius * collectionSphere.radius

                        if (dist2 <= radius2) {
                            val existingBuilding = (obj.getObserver() as? CombatPhysObserverClass) as? BuildingGameObj
                            if (existingBuilding != null) {
                                val existingDist2 = (existingBuilding.collectionSphere.center - objPos).length2()
                                if (dist2 < existingDist2) {
                                    existingBuilding.removeMesh(staticPhys)
                                    addMesh(staticPhys)
                                }
                            } else {
                                addMesh(staticPhys)
                            }
                        }
                    }
                }
            }

            // C++: Iterate static lights
            for (light in scene.getStaticLights()) {
                val lightPhys = light.asLightPhysClass() ?: continue
                if (namePrefixMatchesThisBuilding(lightPhys.getName())) {
                    val lightPos = lightPhys.getPosition()
                    val dist2 = (collectionSphere.center - lightPos).length2()
                    if (dist2 <= collectionSphere.radius * collectionSphere.radius) {
                        addLight(lightPhys)
                    }
                }
            }
        }

        // C++: Initialize_Building(); Update_State(true)
        initializeBuilding()
        updateState(forceUpdate = true)
    }

    // -------------------------------------------------------------------------
    // C++: void Initialize_Building()
    // -------------------------------------------------------------------------
    private fun initializeBuilding() {
        // C++: Generate a bounding box from all interior meshes → compute AnnouncementSphere
        var minX = position.x; var maxX = position.x
        var minY = position.y; var maxY = position.y
        var minZ = position.z; var maxZ = position.z

        for (mesh in interiorMeshes) {
            val box = mesh.peekModel()?.getBoundingBox() ?: continue
            val c = box.center
            val e = box.extent
            minX = minOf(minX, c.x - e.x); maxX = maxOf(maxX, c.x + e.x)
            minY = minOf(minY, c.y - e.y); maxY = maxOf(maxY, c.y + e.y)
            minZ = minOf(minZ, c.z - e.z); maxZ = maxOf(maxZ, c.z + e.z)
        }

        val center = Vector3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
        val extentX = (maxX - minX) / 2f
        val extentY = (maxY - minY) / 2f
        val extentZ = (maxZ - minZ) / 2f
        val radius = maxOf(extentX, extentY, extentZ)
        announcementSphere = Sphere(center, radius)

        // C++: BaseControllerClass* base = BaseControllerClass::Find_Base(Get_Player_Type()); if (base) base->Add_Building(this)
        BaseControllerClass.findBase(playerType)?.addBuilding(this)
    }

    // -------------------------------------------------------------------------
    // C++: void Reset_Components()
    // -------------------------------------------------------------------------
    private fun resetComponents() {
        for (mesh in interiorMeshes) mesh.setObserver(null)
        for (mesh in exteriorMeshes) mesh.setObserver(null)
        for (agg in aggregates)       agg.setObserver(null)
        interiorMeshes.clear()
        exteriorMeshes.clear()
        powerOnLights.clear()
        powerOffLights.clear()
        aggregates.clear()
    }

    // C++: void Add_Mesh(StaticPhysClass* terrain)
    private fun addMesh(terrain: StaticPhysClass) {
        val name = terrain.peekModel()?.getName() ?: return
        when {
            isInteriorMeshName(name) -> {
                check(terrain.getObserver() == null)
                interiorMeshes.add(terrain)
                terrain.setObserver(this)
            }
            isExteriorMeshName(name) -> {
                check(terrain.getObserver() == null)
                exteriorMeshes.add(terrain)
                terrain.setObserver(this)
            }
        }
    }

    // C++: void Remove_Mesh(StaticPhysClass* terrain)
    fun removeMesh(terrain: StaticPhysClass) {
        val name = terrain.peekModel()?.getName() ?: return
        when {
            isInteriorMeshName(name) -> { terrain.setObserver(null); interiorMeshes.remove(terrain) }
            isExteriorMeshName(name) -> { terrain.setObserver(null); exteriorMeshes.remove(terrain) }
        }
    }

    // C++: void Add_Aggregate(BuildingAggregateClass* aggregate)
    private fun addAggregate(aggregate: BuildingAggregateClass) {
        aggregates.add(aggregate)
        check(aggregate.getObserver() == null)
        aggregate.setObserver(this)
    }

    // C++: void Remove_Aggregate(BuildingAggregateClass* aggregate)
    fun removeAggregate(aggregate: BuildingAggregateClass) {
        aggregate.setObserver(null)
        aggregates.remove(aggregate)
    }

    // C++: void Add_Light(LightPhysClass* light)
    private fun addLight(light: LightPhysClass) {
        // C++: if (light->Get_Group_ID() == 0) PowerOnLights else PowerOffLights
        if (light.getGroupId() == 0) {
            powerOnLights.add(light)
        } else {
            powerOffLights.add(light)
        }
    }

    // C++: void Enable_Alternate_Materials(RefMultiListClass<StaticPhysClass>& models, bool onoff)
    private fun enableAlternateMaterials(models: List<StaticPhysClass>, onoff: Boolean) {
        for (mesh in models) enableAlternateMaterials(mesh.peekModel(), onoff)
    }

    // C++: void Enable_Alternate_Materials(RenderObjClass* model, bool onoff)
    private fun enableAlternateMaterials(model: RenderObjClass?, onoff: Boolean) {
        if (model == null) return
        for (i in 0 until model.getNumSubObjects()) {
            enableAlternateMaterials(model.getSubObject(i), onoff)
        }
        if (model.classId == RenderObjClass.CLASSID_MESH) {
            model.asMeshClass()?.getModel()?.enableAlternateMaterialDescription(onoff)
        }
    }

    // C++: void Find_Closest_Poly(const Vector3& pos, float* distance2)
    fun findClosestPoly(pos: Vector3): Float {
        var distance2 = 9999.0f
        for (mesh in interiorMeshes) distance2 = findClosestPolyForModel(mesh.peekModel(), pos, distance2)
        for (mesh in exteriorMeshes) distance2 = findClosestPolyForModel(mesh.peekModel(), pos, distance2)
        return distance2
    }

    // C++: void Find_Closest_Poly_For_Model(RenderObjClass* model, const Vector3& pos, float* distance2)
    private fun findClosestPolyForModel(model: RenderObjClass?, pos: Vector3, distance2In: Float): Float {
        if (model == null) return distance2In
        var distance2 = distance2In
        for (i in 0 until model.getNumSubObjects()) {
            distance2 = findClosestPolyForModel(model.getSubObject(i), pos, distance2)
        }
        if (model.classId == RenderObjClass.CLASSID_MESH) {
            val meshModel = model.asMeshClass()?.peekModel()
            if (meshModel != null) {
                val tris = meshModel.getPolygonArray()
                val verts = meshModel.getVertexArray()
                for (i in tris.indices) {
                    val polyCenter = (verts[tris[i][0]] + verts[tris[i][1]] + verts[tris[i][2]]) * (1f / 3f)
                    val delta = polyCenter - pos
                    val dist2 = delta.length2()
                    if (dist2 < distance2) distance2 = dist2
                }
            }
        }
        return distance2
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Get_Description(StringClass& description)
    // -------------------------------------------------------------------------
    override fun getDescription(): String {
        val sb = StringBuilder()
        sb.append("ID:    $networkId\n")
        sb.append("Name:  ${getDefinition().name}\n")
        sb.append("Team:  $playerType\n")
        sb.append("POS:   ${position.x}, ${position.y}, ${position.z}\n")
        sb.append("HLTH:  ${defenseObject.health}\n")
        sb.append("Destr: $isDestroyed\n")
        sb.append("Power: $isPowerOn\n")
        return sb.toString()
    }

    // -------------------------------------------------------------------------
    // C++: virtual bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_POSITION,          position)
        csave.writeMicroChunk(MICROCHUNKID_ISPOWERON,         isPowerOn)
        csave.writeMicroChunk(MICROCHUNKID_COLLECTION_SPHERE, collectionSphere)
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
                            MICROCHUNKID_POSITION          -> position          = cload.readVector3()
                            MICROCHUNKID_ISPOWERON         -> isPowerOn         = cload.readBool()
                            MICROCHUNKID_COLLECTION_SPHERE -> collectionSphere  = cload.readSphere()
                            else -> error("Unrecognized BuildingGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                else -> error("Unrecognized BuildingGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // C++: Fix player_type (legacy) — Set_Player_Type(Get_Definition().DefaultPlayerType)
        playerType = getDefinition().defaultPlayerType

        // C++: Hack to fix legacy CollectionSphere.Center == 0,0,0 → use Position instead
        if (collectionSphere.center.x == 0f && collectionSphere.center.y == 0f && collectionSphere.center.z == 0f) {
            collectionSphere = Sphere(position, collectionSphere.radius)
        }

        return true
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Export_Rare(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun exportRare(packet: BitStream) {
        // C++: DamageableGameObj::Export_Rare(packet)
        super.exportRare(packet)

        // C++: packet.Add(IsDestroyed); packet.Add(IsPowerOn); packet.Add(CurrentState, BITPACK_BUILDING_STATE)
        packet.addBool(isDestroyed)
        packet.addBool(isPowerOn)
        packet.addInt(currentState, BITPACK_BUILDING_STATE)
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Import_Rare(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun importRare(packet: BitStream) {
        // C++: DamageableGameObj::Import_Rare(packet)
        super.importRare(packet)

        // C++: Read the state information from the server
        val isDestroyedNew  = packet.getBool()
        val isPowerOnNew    = packet.getBool()
        val currentStateNew = packet.getInt(BITPACK_BUILDING_STATE)

        // C++: if (is_power_on != IsPowerOn) Enable_Power(is_power_on)
        if (isPowerOnNew != isPowerOn) {
            enablePower(isPowerOnNew)
        }

        // C++: if (is_destroyed) Get_Defense_Object()->Set_Health(0.0f)
        if (isDestroyedNew) {
            defenseObject.setHealth(0f)
        }

        // C++: if (current_state != CurrentState) Update_State()
        if (currentStateNew != currentState) {
            updateState()
        }

        // C++: if (is_destroyed && IsDestroyed == false) On_Destroyed()
        if (isDestroyedNew && !isDestroyed) {
            onDestroyed()
        }
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Export_Creation(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun exportCreation(packet: BitStream) {
        // C++: DamageableGameObj::Export_Creation(packet)
        super.exportCreation(packet)

        // C++: definitionId written by NetworkGameObjectFactory.prepPacket() before Export_Creation

        // C++: packet.Add(position.X/Y/Z, BITPACK_WORLD_POSITION_X/Y/Z)
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)

        // C++: packet.Add(CollectionSphere.Center.X/Y/Z, ...) + packet.Add(CollectionSphere.Radius, ...)
        packet.addFloat(collectionSphere.center.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(collectionSphere.center.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(collectionSphere.center.z, BITPACK_WORLD_POSITION_Z)
        packet.addFloat(collectionSphere.radius, BITPACK_BUILDING_RADIUS)
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Import_Creation(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun importCreation(packet: BitStream) {
        // C++: DamageableGameObj::Import_Creation(packet)
        super.importCreation(packet)

        // C++: Read position
        val posX = packet.getFloat(BITPACK_WORLD_POSITION_X)
        val posY = packet.getFloat(BITPACK_WORLD_POSITION_Y)
        val posZ = packet.getFloat(BITPACK_WORLD_POSITION_Z)
        setPosition(Vector3(posX, posY, posZ))

        // C++: Read CollectionSphere
        val sphereCenterX = packet.getFloat(BITPACK_WORLD_POSITION_X)
        val sphereCenterY = packet.getFloat(BITPACK_WORLD_POSITION_Y)
        val sphereCenterZ = packet.getFloat(BITPACK_WORLD_POSITION_Z)
        val sphereRad = packet.getFloat(BITPACK_BUILDING_RADIUS)
        collectionSphere = Sphere(Vector3(sphereCenterX, sphereCenterY, sphereCenterZ), sphereRad)

        // C++: Collect_Building_Components(); Update_State(); Initialize_Building()
        collectBuildingComponents()
        updateState()
        initializeBuilding()
    }

    // -------------------------------------------------------------------------
    // C++: virtual void completelyDamaged(const OffenseObjectClass& damager)
    // Called from DamageableGameObj::Apply_Damage when health reaches 0.
    // onDestroyed() is called via BuildingMonitorClass::Killed() observer callback,
    // which fires when DamageableGameObj fires observer.killed() from applyDamage.
    // -------------------------------------------------------------------------
    override fun completelyDamaged(damager: OffenseObjectClass) {
        // BuildingMonitor observer handles: it calls onDestroyed() via Killed() callback
    }

    companion object {
        // C++: static bool CanRepairBuildings = true
        var canRepairBuildings: Boolean = true

        // C++: BuildingGameObj Save/Load chunk IDs
        // enum { CHUNKID_PARENT = 207011120, CHUNKID_VARIABLES, ... }
        private const val CHUNKID_PARENT    = 207011120
        private const val CHUNKID_VARIABLES = 207011121

        private const val MICROCHUNKID_POSITION          = 1
        private const val MICROCHUNKID_ISPOWERON         = 2
        private const val MICROCHUNKID_COLLECTION_SPHERE = 3

        // C++: APPPACKETTYPE_BUILDING (apppackettypes.h) — enum value 59
        private const val APPPACKETTYPE_BUILDING = 59

        // C++: PHYSICS_CHUNKID_BUILDINGAGGREGATE (wwphysids.h) — factory chunk for BuildingAggregateClass
        private const val PHYSICS_CHUNKID_BUILDINGAGGREGATE = 0xDC2F95

        // C++: BuildingStateClass health state constants (buildingstate.h)
        const val HEALTH_100 = 0
        const val HEALTH_75  = 1
        const val HEALTH_50  = 2
        const val HEALTH_25  = 3
        const val HEALTH_0   = 4

        // C++: BuildingStateClass::STATE_COUNT = 10
        const val STATE_COUNT = 10

        // C++: BuildingStateClass::Percentage_To_Health_State(float health)
        fun percentageToHealthState(pct: Float): Int = when {
            pct <= 0f  -> HEALTH_0
            pct <= 25f -> HEALTH_25
            pct <= 50f -> HEALTH_50
            pct <= 75f -> HEALTH_75
            else       -> HEALTH_100
        }

        // C++: BuildingStateClass::Compose_State(int health_state, bool power_onoff)
        // Power off states are offset by 5: 0..4 = power on, 5..9 = power off
        fun composeState(healthState: Int, powerOn: Boolean): Int =
            healthState + if (!powerOn) 5 else 0

        // C++: BuildingStateClass::Get_Health_State(int building_state)
        fun getHealthState(buildingState: Int): Int {
            // Handle -1 initial state → treat as HEALTH_100
            if (buildingState < 0) return HEALTH_100
            return buildingState % 5
        }

        // C++: BuildingStateClass::Is_Power_On(int building_state)
        fun isPowerOnState(buildingState: Int): Boolean {
            if (buildingState < 0) return true
            return buildingState < 5
        }

        // C++: Custom event IDs (gameobjobserver.h)
        // CUSTOM_EVENT_SYSTEM_FIRST = 1000000000
        // CUSTOM_EVENT_BUILDING_POWER_CHANGED = 1000000002
        // CUSTOM_EVENT_BUILDING_DAMAGED = 1000000026
        // CUSTOM_EVENT_BUILDING_REPAIRED = 1000000027
        const val CUSTOM_EVENT_BUILDING_POWER_CHANGED = 1000000002
        const val CUSTOM_EVENT_BUILDING_DAMAGED       = 1000000026
        const val CUSTOM_EVENT_BUILDING_REPAIRED      = 1000000027
    }
}
