package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.defs.OBBoxClass
import ccr.server.defs.RepairBayGameObjDef
import ccr.server.defs.ZoneType

// C++: RepairBayGameObj : public BuildingGameObj
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//              → BuildingGameObj → RepairBayGameObj
class RepairBayGameObj() : BuildingGameObj() {

    // C++: float RepairTimer (initialized to 0)
    var repairTimer: Float = 0f

    // C++: OBBoxClass RepairZone (initialized to center=(0,0,0), extent=(0,0,0))
    var repairZone: OBBoxClass = OBBoxClass()

    // C++: int RepairAnimationID (initialized to 0)
    var repairAnimationID: Int = 0

    // C++: bool IsReparing (initialized to false)
    var isReparing: Boolean = false

    // C++: DynamicVectorClass<GameObjReference> VehicleList
    val vehicleList: MutableList<GameObjReference> = mutableListOf()

    // C++: StaticPhysClass* RepairMesh (initialized to NULL)
    var repairMesh: StaticPhysClass? = null

    // C++: SimpleGameObj* ArcObjects[ARC_OBJ_COUNT] (initialized to 0/NULL)
    val arcObjects: Array<SimpleGameObj?> = arrayOfNulls(ARC_OBJ_COUNT)

    // C++: float ArcLifeRemaining[ARC_OBJ_COUNT] (initialized to 0)
    val arcLifeRemaining: FloatArray = FloatArray(ARC_OBJ_COUNT) { 0f }

    // C++: Matrix3D EndTM (uninitialized — set in CnC_Initialize)
    var endTM: ccr.math.Matrix3D = ccr.math.Matrix3D.IDENTITY

    // C++: Matrix3D Bones[BONE_COUNT] (uninitialized — set in CnC_Initialize)
    val bones: Array<ccr.math.Matrix3D> = Array(BONE_COUNT) { ccr.math.Matrix3D.IDENTITY }

    init {
        // C++: RepairBayGameObj() : RepairTimer(0), RepairZone(...), RepairAnimationID(0),
        //   IsReparing(false), RepairMesh(NULL) { memset(ArcObjects, 0, ...); for(...) ArcLifeRemaining[i]=0; }
        // — all field defaults cover the constructor body above
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
        repairZoneCenter: Vector3 = Vector3(),
        repairZoneExtent: Vector3 = Vector3(),
        repairZoneFacing: Float = 0f,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "repairbay_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
        // Build repairZone basis from facing angle and set zone (OBBoxClass.basis is FloatArray(9) row-major 3x3)
        val c = kotlin.math.cos(repairZoneFacing.toDouble()).toFloat()
        val s = kotlin.math.sin(repairZoneFacing.toDouble()).toFloat()
        val basisArr = floatArrayOf(c, -s, 0f,  s, c, 0f,  0f, 0f, 1f)
        repairZone = OBBoxClass(center = repairZoneCenter, extent = repairZoneExtent, basis = basisArr)
    }

    // C++: ~RepairBayGameObj()
    override fun destruct() {
        // C++: for (int index = 0; index < ARC_OBJ_COUNT; index++) ArcObjects[index] = NULL;
        for (index in 0 until ARC_OBJ_COUNT) {
            arcObjects[index] = null
        }
        super.destruct()
    }

    // C++: virtual void Init(void)
    override fun init() {
        // C++: BuildingGameObj::Init(Get_Definition())
        // Cast to BuildingGameObjDef to resolve init() overload ambiguity in the hierarchy
        super.init(getRepairBayDefinition() as ccr.server.defs.BuildingGameObjDef)
    }

    // C++: const RepairBayGameObjDef& Get_Definition() const
    fun getRepairBayDefinition(): RepairBayGameObjDef = definition as RepairBayGameObjDef

    // C++: RepairBayGameObj* As_RepairBayGameObj() { return this; }
    // → handled by Kotlin type system: `this is RepairBayGameObj`

    // -------------------------------------------------------------------------
    // C++: virtual bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        // C++: empty CHUNKID_VARIABLES block (no micro-chunks written)
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
                else -> error("Unrecognized RepairBayGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass& cload)
    private fun loadVariables(cload: ChunkLoadClass) {
        // C++: the switch is commented out — just drain micro-chunks
        while (cload.openMicroChunk()) {
            // MICROCHUNKID_UNUSED = 1 — no cases handled
            cload.closeMicroChunk()
        }
    }

    // -------------------------------------------------------------------------
    // C++: void CnC_Initialize(BaseControllerClass* base)
    // -------------------------------------------------------------------------
    override fun cncInitialize(base: BaseControllerClass) {
        // C++: BuildingGameObj::CnC_Initialize(base)
        super.cncInitialize(base)

        // C++: Get the building's "position"
        val pos = getPosition()

        // C++: Find the closest repair bay zone to the building
        val zone = ScriptZoneGameObj.findClosestZone(pos, ZoneType.TYPE_VEHICLE_REPAIR)
        if (zone != null) {
            // C++: RepairZone = zone->Get_Bounding_Box()
            repairZone = zone.getBoundingBox()

            // C++: if (zone->Get_Observers().Count() == 0) zone->Set_Delete_Pending()
            if (zone.getObservers().isEmpty()) {
                zone.setDeletePending()
            }
        }

        // C++: Try to find the mesh that contains the repair fx
        //   RefMultiListIterator<BuildingAggregateClass> mesh_iterator(&Aggregates);
        //   look for model named "rep^NOD_fx" (case-insensitive)
        for (aggregate in aggregates) {
            val physObj = aggregate as? StaticPhysClass ?: continue
            val model = physObj.peekModel() ?: continue
            if (model.getName().equals("rep^NOD_fx", ignoreCase = true)) {
                repairMesh = physObj
                break
            }
        }

        // C++: Find the closest repairing static anim phys
        var closest2 = 99999.0f
        val scene = CombatManager.getScene()
        if (scene != null) {
            for (obj in scene.getStaticObjects()) {
                val animPhysObj = obj.asStaticAnimPhysClass() ?: continue
                if (animPhysObj.getDefinition()?.id == getRepairBayDefinition().repairingStaticAnimDefId.toUInt()) {
                    val animPos = Vector3(0f, 0f, 0f)
                    animPhysObj.getPosition(animPos)
                    val dist2 = (animPos - pos).length2()
                    if (dist2 < closest2) {
                        closest2 = dist2
                        repairAnimationID = animPhysObj.getId()
                    }
                }
            }
        }

        // C++: Create the SimpleGameObj's for the repair-arc effect
        for (index in 0 until ARC_OBJ_COUNT) {
            arcObjects[index] = ObjectLibraryManager.createObject("Arc Effect") as? SimpleGameObj
            val physObj = arcObjects[index]?.peekPhysicalObject()
            if (physObj != null && physObj.peekModel() != null) {
                physObj.peekModel()!!.setHidden(true)
            }
            arcLifeRemaining[index] = 0f
        }

        // C++: Get information about the model (bone transforms)
        val arcModel = arcObjects[0]?.peekPhysicalObject()?.peekModel()
        if (arcModel != null) {
            // C++: Store the original transforms of each bone
            for (index in 0 until BONE_COUNT) {
                bones[index] = arcModel.getBoneTransform(BONE_NAMES[index])
            }
            // C++: Lookup the original transform of the end-bone
            val endIndex = arcModel.getBoneIndex("bone_end")
            endTM = arcModel.getBoneTransform(endIndex)
        }
    }

    // -------------------------------------------------------------------------
    // C++: virtual void Think(void)
    // -------------------------------------------------------------------------
    override fun think() {
        // C++: if (IsDestroyed == false)
        if (!isDestroyed) {
            val frameSeconds = TimeManager.getFrameSeconds()
            repairTimer -= frameSeconds
            if (repairTimer <= 0f) {
                repairTimer = 0.5f

                // C++: bool is_repairing = Repair_Vehicle()
                val isRepairingNow = repairVehicle()

                // C++: if (is_repairing != IsReparing) IsReparing = is_repairing
                if (isRepairingNow != isReparing) {
                    isReparing = isRepairingNow
                }
            }

            if (isReparing) {
                updateRepairingAnimations()
            }
        }

        // C++: Hide any arc-effects that have finished
        for (index in 0 until ARC_OBJ_COUNT) {
            if (arcLifeRemaining[index] > 0f) {
                arcLifeRemaining[index] -= TimeManager.getFrameSeconds()
                if (arcLifeRemaining[index] <= 0f) {
                    val physObj = arcObjects[index]?.peekPhysicalObject()
                    physObj?.peekModel()?.setHidden(true)
                }
            }
        }

        // C++: BuildingGameObj::Think()
        super.think()
    }

    // -------------------------------------------------------------------------
    // C++: bool Repair_Vehicle(void)
    // -------------------------------------------------------------------------
    private fun repairVehicle(): Boolean {
        var isRepairing = false
        vehicleList.clear()

        // C++: Collect the dynamic physics objects in the repair zone
        //   PhysicsSceneClass::Get_Instance()->Collect_Objects(RepairZone, false, true, &objs_to_repair)
        val scene = CombatManager.getScene()
        val objsToRepair: List<PhysClass> = scene?.collectObjects(repairZone, false, true) ?: emptyList()

        // C++: Loop over all the objects
        for (physObj in objsToRepair) {
            val gameObj: PhysicalGameObj?
            // C++: if (it.Peek_Obj()->Get_Observer() != NULL)
            //   gameobj = ((CombatPhysObserverClass*)it.Peek_Obj()->Get_Observer())->As_PhysicalGameObj()
            val observer = physObj.getObserver()
            gameObj = if (observer is CombatPhysObserverClass) {
                observer.asPhysicalGameObj()
            } else {
                null
            }

            if (gameObj != null) {
                // C++: VehicleGameObj* vehicle = gameobj->As_VehicleGameObj()
                val vehicle = gameObj as? VehicleGameObj
                if (vehicle != null) {
                    // C++: Check to ensure the driver is of the right team
                    val driver = vehicle.getDriver()
                    if (driver != null && driver.playerType == baseController?.playerType) {

                        val shieldMax = vehicle.defenseObject.shieldStrengthMax
                        val healthMax = vehicle.defenseObject.healthMax
                        val currHealth = vehicle.defenseObject.health
                        val currShield = vehicle.defenseObject.shieldStrength

                        // C++: Avoid divide-by-zero
                        val safeHealthMax = maxOf(healthMax, WWMATH_EPSILON)
                        val safeShieldMax = maxOf(shieldMax, WWMATH_EPSILON)

                        // C++: Calculate how much health and shield to restore
                        val vehicleCost = 1000.0f  // C++: 1000.0F (hardcoded)

                        val repairRate = getRepairBayDefinition().repairPerSec * 0.5f
                        val totalDamage = (safeHealthMax - currHealth) + (safeShieldMax - currShield)
                        val damagePoints = minOf(repairRate, totalDamage).toInt()
                        val repairCostPerPt = vehicleCost / (safeHealthMax + safeShieldMax)

                        // C++: int available_funds = 1000 (hardcoded)
                        val availableFunds = 1000

                        var pointsRestored = (availableFunds / repairCostPerPt).toInt()
                        pointsRestored = maxOf(pointsRestored, 0)
                        pointsRestored = minOf(pointsRestored, damagePoints)

                        val healthRestored = minOf((safeHealthMax - currHealth).toInt(), pointsRestored)
                        val shieldRestored = minOf((safeShieldMax - currShield).toInt(), pointsRestored - healthRestored)

                        // C++: Debit the player's account — commented out in C++
                        isRepairing = true

                        // C++: Repair the vehicle's health if the player has enough money
                        if (healthRestored > 0) {
                            // C++: if (CombatManager::I_Am_Server()) — always server; omit guard per guide
                            vehicle.defenseObject.addHealth(healthRestored.toFloat())
                            isRepairing = true
                        }

                        // C++: Repair the vehicle's shield if the player has enough money
                        if (shieldRestored > 0) {
                            // C++: if (CombatManager::I_Am_Server()) — always server; omit guard per guide
                            vehicle.defenseObject.addShieldStrength(shieldRestored.toFloat())
                            isRepairing = true
                        }

                        // C++: Add this vehicle to the list of vehicles being repaired
                        if (isRepairing) {
                            vehicleList.add(GameObjReference(vehicle))
                        }
                    }
                }
            }
        }

        return isRepairing
    }

    // -------------------------------------------------------------------------
    // C++: void Play_Repairing_Animation(bool onoff)
    // -------------------------------------------------------------------------
    private fun playRepairingAnimation(onoff: Boolean) {
        // C++: Lookup the static animation object we need to play
        val staticPhysObj = CombatManager.getScene()?.findStaticObject(repairAnimationID) ?: return
        val animPhysObj = staticPhysObj.asStaticAnimPhysClass() ?: return

        // C++: Configure the animation
        val animMgr = animPhysObj.getAnimationManager()
        animMgr.setAnimationMode(AnimCollisionManagerClass.ANIMATE_TARGET)

        // C++: Either play the animation forward or backward
        if (onoff && animMgr.getTargetFrame() == 0f) {
            animMgr.setTargetFrameEnd()
        } else if (!onoff && animMgr.getTargetFrame() != 0f) {
            animMgr.setTargetFrame(0f)
        }
    }

    // -------------------------------------------------------------------------
    // C++: void Update_Repairing_Animations(void)
    // -------------------------------------------------------------------------
    private fun updateRepairingAnimations() {
        // C++: Loop over all the vehicles that are currently being repaired
        for (index in 0 until vehicleList.size) {
            val gameObj = vehicleList[index].get()
            if (gameObj != null) {
                val physicalGameObj = gameObj as? PhysicalGameObj ?: continue
                val model = physicalGameObj.peekModel()
                if (model != null) {
                    // C++: if (FreeRandom.Get_Int(4) == 1) Emit_Welding_Arc(model)
                    if (FreeRandom.getInt(4) == 1) {
                        emitWeldingArc(model)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // C++: void Emit_Welding_Arc(RenderObjClass* vehicle_model)
    // -------------------------------------------------------------------------
    private fun emitWeldingArc(vehicleModel: RenderObjClass) {
        val localRepairMesh = repairMesh ?: return

        // C++: Pick a random bone to emit from the repair mesh
        var startpoint = getPosition()
        val aggregateModel = localRepairMesh.peekModel()
        if (aggregateModel != null) {
            val count = 8  // C++: hardcoded 8
            val boneIndex = FreeRandom.getInt(count) + 1
            val boneName = "REP^NODRIM_FX$boneIndex"
            startpoint = aggregateModel.getBoneTransform(boneName).translation
        }

        // C++: Default endpoint to vehicle's position
        var endpoint = vehicleModel.getTransform().translation

        // C++: Find a random mesh inside the vehicle's model
        val mesh = findRandomMesh(vehicleModel)
        if (mesh != null) {
            val meshModel = mesh.getModel()
            if (meshModel != null) {
                val vertexCount = meshModel.getVertexCount()
                if (vertexCount > 0) {
                    val randomVertIndex = FreeRandom.getInt(vertexCount)
                    endpoint = mesh.getTransform().transformPoint(meshModel.getVertexArray()[randomVertIndex])
                }
            }
        }

        // C++: Try to find an available welding-arc
        for (index in 0 until ARC_OBJ_COUNT) {
            if (arcLifeRemaining[index] <= 0f && FreeRandom.getInt(4) == 1) {
                val model = arcObjects[index]?.peekPhysicalObject()?.peekModel() ?: continue

                // C++: Make the object "look" at its endpoint
                val startTm = ccr.math.Matrix3D.objLookAt(startpoint, endpoint, 0f)
                model.setTransform(startTm)

                // C++: Scale the bones to fit between start and endpoints
                for (boneIndex in 0 until BONE_COUNT) {
                    val boneLocalX = bones[boneIndex].translation.x
                    val endLocalX = endTM.translation.x
                    val percent = if (endLocalX != 0f) kotlin.math.abs(boneLocalX / endLocalX) else 0f
                    val newPos = startpoint + (endpoint - startpoint) * percent

                    // C++: world_space_pos = start_tm * Bones[bone_index].Get_Translation()
                    val worldSpacePos = startTm.transformPoint(bones[boneIndex].translation)
                    val worldSpaceOffset = newPos - worldSpacePos
                    val boneTm = ccr.math.Matrix3D(
                        m03 = worldSpaceOffset.x, m13 = worldSpaceOffset.y, m23 = worldSpaceOffset.z
                    )

                    val boneId = model.getBoneIndex(BONE_NAMES[boneIndex])
                    model.captureBone(boneId)
                    model.controlBone(boneId, boneTm, true)
                }

                // C++: Control the ending bone
                val worldSpaceEndPos = startTm.transformPoint(endTM.translation)
                val worldSpaceOffset = endpoint - worldSpaceEndPos
                val endTmControl = ccr.math.Matrix3D(
                    m03 = worldSpaceOffset.x, m13 = worldSpaceOffset.y, m23 = worldSpaceOffset.z
                )
                val endBoneId = model.getBoneIndex("bone_end")
                model.captureBone(endBoneId)
                model.controlBone(endBoneId, endTmControl, true)

                // C++: Start the animation
                arcLifeRemaining[index] = 3.0f
                val anim = model.peekAnimation()
                if (anim != null) {
                    model.setAnimation(anim, 0f, RenderObjClass.ANIM_MODE_ONCE)
                    arcLifeRemaining[index] = anim.getTotalTime()
                }

                // C++: Show the model
                model.setHidden(false)
            }
        }
    }

    // -------------------------------------------------------------------------
    // C++ local function: MeshClass* Find_Random_Mesh(RenderObjClass* model)
    // -------------------------------------------------------------------------
    private fun findRandomMesh(model: RenderObjClass): MeshClass? {
        // C++: Is this model already a mesh?
        var mesh: MeshClass? = null
        if (model.classId == RenderObjClass.CLASSID_MESH) {
            mesh = model.asMeshClass()
        }

        // C++: Pick a random start index in the list
        val count = model.getNumSubObjects()
        val startIndex = FreeRandom.getInt(count)

        // C++: Loop over all the objects
        var index = 0
        while (mesh == null && index < count) {
            val realIndex = (startIndex + index) % count
            val subObj = model.getSubObject(realIndex)
            if (subObj != null) {
                mesh = findRandomMesh(subObj)
            }
            index++
        }

        return mesh
    }

    // -------------------------------------------------------------------------
    // C++: void Export_Creation(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun exportCreation(packet: BitStream) {
        // C++: BuildingGameObj::Export_Creation(packet)
        super.exportCreation(packet)

        // C++: Send the repair zone's position and size
        packet.addFloat(repairZone.center.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(repairZone.center.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(repairZone.center.z, BITPACK_WORLD_POSITION_Z)
        packet.addFloat(repairZone.extent.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(repairZone.extent.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(repairZone.extent.z, BITPACK_WORLD_POSITION_Z)

        // C++: float facing = RepairZone.Basis.Get_Z_Rotation()
        // The OBBoxClass basis is a 3x3 matrix stored as FloatArray(9) row-major.
        // Get_Z_Rotation() = atan2(basis[3], basis[0]) — i.e. atan2(m10, m00)
        val facing = getZRotationFromBasis(repairZone.basis)
        packet.addFloat(facing)
    }

    // -------------------------------------------------------------------------
    // C++: void Import_Creation(BitStreamClass& packet)
    // -------------------------------------------------------------------------
    override fun importCreation(packet: BitStream) {
        // C++: BuildingGameObj::Import_Creation(packet)
        super.importCreation(packet)

        // C++: Get the repair zone's position and size
        val centerX = packet.getFloat(BITPACK_WORLD_POSITION_X)
        val centerY = packet.getFloat(BITPACK_WORLD_POSITION_Y)
        val centerZ = packet.getFloat(BITPACK_WORLD_POSITION_Z)
        val extentX = packet.getFloat(BITPACK_WORLD_POSITION_X)
        val extentY = packet.getFloat(BITPACK_WORLD_POSITION_Y)
        val extentZ = packet.getFloat(BITPACK_WORLD_POSITION_Z)

        // C++: Get the zone's facing
        val facing = packet.getFloat()

        // C++: RepairZone.Basis.Rotate_Z(facing)
        val newBasis = rotateBasisZ(repairZone.basis, facing)
        repairZone = OBBoxClass(
            center = Vector3(centerX, centerY, centerZ),
            extent = Vector3(extentX, extentY, extentZ),
            basis  = newBasis,
        )
    }

    companion object {
        // C++: enum { ARC_OBJ_COUNT = 4, BONE_COUNT = 6 }
        const val ARC_OBJ_COUNT = 4
        const val BONE_COUNT    = 6

        // C++: const char* BoneNames[BONE_COUNT]
        val BONE_NAMES = arrayOf(
            "BONE02",
            "BONE03",
            "BONE04",
            "BONE05",
            "BONE06",
            "BONE07",
        )

        // C++: CHUNKID_PARENT = 0x0219043, CHUNKID_VARIABLES
        private const val CHUNKID_PARENT    = 0x0219043
        private const val CHUNKID_VARIABLES = 0x0219044

        // C++: MICROCHUNKID_UNUSED = 1
        private const val MICROCHUNKID_UNUSED = 1

        // C++: WWMATH_EPSILON (wwmath.h)
        private const val WWMATH_EPSILON = 0.0001f

        /**
         * C++: Matrix3D::Get_Z_Rotation() — atan2(m10, m00)
         * For a 3x3 basis stored as FloatArray(9) row-major: [m00,m01,m02, m10,m11,m12, m20,m21,m22]
         */
        fun getZRotationFromBasis(basis: FloatArray): Float {
            if (basis.size < 4) return 0f
            val m00 = basis[0]
            val m10 = basis[3]
            return kotlin.math.atan2(m10, m00)
        }

        /**
         * C++: Matrix3D::Rotate_Z(float angle) — applies Z-axis rotation to the basis.
         * basis is a 3x3 row-major FloatArray(9): [m00,m01,m02, m10,m11,m12, m20,m21,m22]
         */
        fun rotateBasisZ(basis: FloatArray, angle: Float): FloatArray {
            val c = kotlin.math.cos(angle)
            val s = kotlin.math.sin(angle)
            val rot = FloatArray(9)
            // Rotation matrix Rz: [[c,-s,0],[s,c,0],[0,0,1]]
            // Result = Rz * basis
            rot[0] = c * basis[0] - s * basis[3]
            rot[1] = c * basis[1] - s * basis[4]
            rot[2] = c * basis[2] - s * basis[5]
            rot[3] = s * basis[0] + c * basis[3]
            rot[4] = s * basis[1] + c * basis[4]
            rot[5] = s * basis[2] + c * basis[5]
            rot[6] = basis[6]
            rot[7] = basis[7]
            rot[8] = basis[8]
            return rot
        }
    }
}
