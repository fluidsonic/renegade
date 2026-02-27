package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.GameObjManager
import ccr.server.defs.AmmoDefinitionClass
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_PROXIMITY
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_REMOTE
import ccr.server.defs.AmmoDefinitionClass.Companion.AMMO_TYPE_C4_TIMED
import ccr.server.defs.C4GameObjDef

// C++: C4GameObj : public SimpleGameObj (c4.h / c4.cpp)
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → C4GameObj
// C++ constructor: Set_App_Packet_Type(APPPACKETTYPE_C4)
//
// Constructor params exist for test API compatibility; in C++ these fields are set by Init_C4() separately.
@Suppress("UNUSED_PARAMETER")
class C4GameObj(
    definitionId: Int = 0,
    position: Vector3 = Vector3(),
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    // C4-specific rare fields — exposed as constructor params for test API compatibility;
    // the live runtime values are stored in the var fields below.
    ammoDef: Int = 0,
    ownerId: Int = 0,
    velX: Float = 0f,
    velY: Float = 0f,
    velZ: Float = 0f,
    stuck: Boolean = false,
    stuckPosX: Float = 0f,
    stuckPosY: Float = 0f,
    stuckPosZ: Float = 0f,
    stuckMct: Boolean = false,
    stuckToObject: Boolean = false,
    stuckObjectId: Int = 0,
    stuckOffsetX: Float = 0f,
    stuckOffsetY: Float = 0f,
    stuckOffsetZ: Float = 0f,
    stuckBone: Int = 0,
    stuckStaticAnim: Boolean = false,
    stuckStaticAnimObjId: Int = 0,
) : SimpleGameObj() {

    // ---
    // C++ fields matching the C++ member variable declarations in c4.h
    // ---

    // C++: float Timer
    var timer: Float = 0f

    // C++: GameObjReference Owner
    val owner: GameObjReference = GameObjReference()

    // C++: void* OwnerBackup (NULL initially)
    var ownerBackup: PlayerDataClass? = null

    // C++: const AmmoDefinitionClass* AmmoDefinition (NULL initially)
    var ammoDefinition: AmmoDefinitionClass? = null

    // C++: int DetonationMode
    var detonationMode: Int = 0

    // C++: bool Stuck (false initially)
    var stuck: Boolean = stuck

    // C++: bool StuckToObject (false initially)
    var stuckToObject: Boolean = stuckToObject

    // C++: GameObjReference StuckObject
    val stuckObject: GameObjReference = GameObjReference()

    // C++: Vector3 StuckOffset
    var stuckOffset: Vector3 = Vector3(stuckOffsetX, stuckOffsetY, stuckOffsetZ)

    // C++: int StuckBone (0 initially)
    var stuckBone: Int = stuckBone

    // C++: StaticAnimPhysClass* StuckStaticAnimObj (NULL initially)
    var stuckStaticAnimObj: StaticAnimPhysClass? = null

    // C++: bool StuckMCT (false initially)
    var stuckMct: Boolean = stuckMct

    // C++: float Age (0 initially)
    var age: Float = 0f

    // ---
    // Wire/export fields — used in Export_Rare / Import_Rare
    // These correspond to additional derived values in the original C++ that are computed at
    // export time from other fields (velocity, stuck position, stuck object ID, etc.)
    // ---

    // C++: vel from ProjectileClass::Get_Velocity in Export_Rare
    var velX: Float = velX
    var velY: Float = velY
    var velZ: Float = velZ

    // Ammo def ID used when ammoDefinition is null
    var ammoDef: Int = ammoDef

    // Owner ID used when Owner ref is null
    var ownerId: Int = ownerId

    // Stuck position — used as fallback when physObj is null
    var stuckPosX: Float = stuckPosX
    var stuckPosY: Float = stuckPosY
    var stuckPosZ: Float = stuckPosZ

    // Stuck object network ID — used in Export_Rare when StuckObject ref cannot resolve
    var stuckObjectId: Int = stuckObjectId

    // Static anim fields for Export/Import_Rare
    var stuckStaticAnim: Boolean = stuckStaticAnim
    var stuckStaticAnimObjId: Int = stuckStaticAnimObjId

    // C++: C4GameObj() — constructor body: Set_App_Packet_Type(APPPACKETTYPE_C4)
    init {
        // C++: Set_App_Packet_Type(APPPACKETTYPE_C4) — APPPACKETTYPE_C4 = 4 (apppackettypes.h)
        appPacketType = APPPACKETTYPE_C4

        // Set fields from constructor params (position, modelName, etc.)
        this.position = position
        this.facing = facing
        this.modelName = modelName
        this.animName = animName
        this.health = health
        this.shieldStrength = shieldStrength
        this.shieldType = shieldType
    }

    // C++: ~C4GameObj() { REF_PTR_RELEASE(StuckStaticAnimObj); }
    override fun destruct() {
        stuckStaticAnimObj = null  // GC handles — C++: REF_PTR_RELEASE(StuckStaticAnimObj)
        owner.destruct()
        stuckObject.destruct()
        super.destruct()
    }

    // C++: virtual void Init()
    override fun init() {
        init(getC4Definition())
    }

    // C++: void Init(const C4GameObjDef & definition)
    fun init(definition: C4GameObjDef) {
        super.init(definition)
        // C++: Peek_Physical_Object()->Set_Collision_Group(TERRAIN_AND_BULLET_COLLISION_GROUP)
        // C++: Peek_Physical_Object()->Set_Collision_Group(DEFAULT_COLLISION_GROUP)
        physObj?.setCollisionGroup(DEFAULT_COLLISION_GROUP)
    }

    // C++: const C4GameObjDef & Get_Definition() const
    fun getC4Definition(): C4GameObjDef = definition as C4GameObjDef

    // C++: C4GameObj* As_C4GameObj() { return this; }
    fun asC4GameObj(): C4GameObj = this

    // C++: SoldierGameObj* Get_Owner() const { return (SoldierGameObj*)Owner.Get_Ptr(); }
    fun getOwner(): SoldierGameObj? = owner.get() as? SoldierGameObj

    // C++: ScriptableGameObj* Get_Stuck_Object()
    fun getStuckObject(): ScriptableGameObj? = stuckObject.get()

    // C++: void Init_C4(const AmmoDefinitionClass * def, SoldierGameObj *owner, int detonation_mode, const Matrix3D & tm)
    fun initC4(def: AmmoDefinitionClass, ownerObj: SoldierGameObj?, detonationMode: Int, tm: Matrix3D) {
        ammoDefinition = def

        if (def.modelFilename.isNotEmpty()) {
            physObj?.setModelByName(def.modelFilename)
        }

        owner.set(ownerObj)
        this.detonationMode = detonationMode
        setTransform(tm)
        stuck = false
        stuckMct = false
        stuckToObject = false
        physObj?.setCollisionGroup(DEFAULT_COLLISION_GROUP)
        ownerBackup = null

        if (ownerObj != null) {
            playerType = ownerObj.playerType
            ownerBackup = ownerObj.playerData

            // C++: if (CombatManager::I_Am_Server() && !IS_MISSION) — server always, never mission
            maintainC4Limit(playerType)
        }

        val type = def.ammoType

        if (type != AMMO_TYPE_C4_REMOTE) {
            // C++: Setup Arming Timer
            var time = def.c4TriggerTime1
            if (detonationMode == 2) time = def.c4TriggerTime2
            if (detonationMode == 3) time = def.c4TriggerTime3
            timer = time
        }

        // C++: sound_id setup — pick based on detonation mode (all three read sound1 per C++ code)
        val soundId: Int = def.c4TimingSound1Id
        // (detonationMode 2 and 3 also read sound1 per C++ bug)

        if (soundId != 0) {
            // C++: WWAudioClass::Get_Instance()->Create_Instant_Sound(sound_id, Get_Transform(), owner_ref)
            // FIXME: wire WWAudioClass.createInstantSound(soundId, getTransform(), owner) when audio is ported
        }

        // C++: ProjectileClass * po = Peek_Physical_Object()->As_ProjectileClass()
        // if (po) po->Set_Velocity(tm.Get_X_Vector() * Get_Definition().ThrowVelocity)
        val po = physObj?.asProjectileClass()
        if (po != null) {
            val throwVelocity = getC4Definition().throwVelocity
            po.setVelocity(tm.getXVector() * throwVelocity)
        }

        // C++: Set_Object_Dirty_Bit(NetworkObjectClass::BIT_RARE, true)
        setObjectDirtyBit(BIT_RARE, true)
    }

    // C++: virtual CollisionReactionType Collision_Occurred(const CollisionEventClass & event)
    override fun collisionOccurred(event: CollisionEventClass): CollisionReactionType {
        if (!stuck) {
            // C++: figure out who/what/where we hit
            var other: PhysicalGameObj? = null
            var building: BuildingGameObj? = null
            val hitProjectile = event.otherObj?.asProjectileClass() != null

            val observer = event.otherObj?.getObserver()
            if (observer != null) {
                other = (observer as? CombatPhysObserverClass)?.asPhysicalGameObj()
                building = (observer as? CombatPhysObserverClass)?.asBuildingGameObj()
            }

            restoreOwner()

            // C++: Ignore my owner and my owner's vehicle
            val currentOwner = getOwner()
            if (other != null && currentOwner != null) {
                val vehicle = other as? VehicleGameObj
                if (vehicle != null && vehicle === currentOwner.vehicle) {
                    return CollisionReactionType.NO_BOUNCE
                }
                if (other === currentOwner) {
                    return CollisionReactionType.NO_BOUNCE
                }

                // C++: Stick to game object
                stuck = true
                stuckToObject = true
                stuckObject.set(other)

                stuckBone = 0
                val parentModel = other.peekModel()
                if (parentModel != null) {
                    stuckBone = parentModel.getSubObjectBoneIndex(event.collidedRenderObj)
                }

                val myPos = getPosition()
                val boneTm = parentModel?.getBoneTransform(stuckBone)
                if (boneTm != null) {
                    // C++: Matrix3D::Inverse_Transform_Vector(parent_model->Get_Bone_Transform(StuckBone), my_pos, &StuckOffset)
                    stuckOffset = boneTm.inverseTransformVector(myPos)
                } else {
                    stuckOffset = myPos
                }
                physObj?.enableUserControl(true)

                setObjectDirtyBit(BIT_RARE, true)
                return CollisionReactionType.STOP_MOTION

            } else if (building != null) {
                // C++: Stick to the building
                stuck = true
                stuckObject.set(building)
                stuckMct = false

                // C++: Check for MCT collision
                if (event.otherObj?.getFactory()?.chunkId == PHYSICS_CHUNKID_BUILDINGAGGREGATE) {
                    val aggregate = event.otherObj as? BuildingAggregateClass
                    if (aggregate?.isMct() == true) {
                        stuckMct = true
                    }
                }

                physObj?.enableUserControl(true)
                setObjectDirtyBit(BIT_RARE, true)
                return CollisionReactionType.STOP_MOTION

            } else if (other == null && !hitProjectile) {
                // C++: if this is a static anim, try to stick to it
                val staticAnim = event.otherObj?.asStaticAnimPhysClass()
                if (staticAnim != null) {
                    stuckStaticAnimObj = staticAnim

                    stuckBone = 0
                    val staticModel = staticAnim.peekModel()
                    if (staticModel != null) {
                        stuckBone = staticModel.getSubObjectBoneIndex(event.collidedRenderObj)
                        val myPos = getPosition()
                        stuckOffset = staticModel.getBoneTransform(stuckBone).inverseTransformVector(myPos)
                    }
                }

                // C++: If we hit permeable, pass through
                if (event.collisionResult != null &&
                    SurfaceEffectsManager.isSurfacePermeable(event.collisionResult.surfaceType)) {
                    return CollisionReactionType.NO_BOUNCE
                }

                // C++: Stick to terrain
                physObj?.enableUserControl(true)
                stuck = true

                setObjectDirtyBit(BIT_RARE, true)
                return CollisionReactionType.STOP_MOTION
            }
        }

        return CollisionReactionType.NO_BOUNCE
    }

    // C++: virtual void Think()
    override fun think() {
        super.think()

        // C++: if (!CombatManager::I_Am_Server()) return — always server, omit guard per guide

        age += TimeManager.getFrameSeconds()

        val def = ammoDefinition ?: return

        val type = def.ammoType

        restoreOwner()

        if (type == AMMO_TYPE_C4_REMOTE) {
            val currentOwner = getOwner()
            if (currentOwner == null) {
                defuse()
            } else if (currentOwner.detonateC4) {
                detonate()
            }
        }

        if (type == AMMO_TYPE_C4_TIMED) {
            // C++: Timer -= TimeManager::Get_Frame_Seconds(); if (Timer <= 0) Detonate()
            timer -= TimeManager.getFrameSeconds()
            if (timer <= 0f) {
                detonate()
            }
        }

        if (type == AMMO_TYPE_C4_PROXIMITY) {
            timer -= TimeManager.getFrameSeconds()
            if (timer <= 0f) {
                // C++: Timer += 0.25f — check every 1/4 second
                timer += 0.25f

                val triggerRange: Float = when (detonationMode) {
                    2    -> def.c4TriggerRange2
                    3    -> def.c4TriggerRange3
                    else -> def.c4TriggerRange1
                }

                val c4Pos = getPosition()
                for (obj in GameObjManager.getSmartGameObjs()) {
                    if (isEnemy(obj)) {
                        val objPos = obj.getPosition()
                        val range = (objPos - c4Pos).length()
                        if (range <= triggerRange) {
                            detonate()
                        }
                    }
                }
            }
        }
    }

    // C++: virtual void Post_Think()
    override fun postThink() {
        super.postThink()

        // C++: Follow your stuck object
        if (stuck) {
            val stuckTarget = stuckObject.get()
            if (stuckTarget != null) {
                val obj = stuckTarget as? PhysicalGameObj
                if (obj != null) {
                    val parentModel = obj.peekModel()
                    val pos: Vector3 = if (parentModel != null) {
                        parentModel.getBoneTransform(stuckBone) * stuckOffset
                    } else {
                        obj.getTransform() * stuckOffset
                    }
                    setPosition(pos)

                    // C++: if obj is a soldier in a vehicle, hide the C4
                    val stuckSoldier = obj as? SoldierGameObj
                    if (stuckSoldier != null) {
                        val hide = stuckSoldier.vehicle != null
                        peekModel()?.setHidden(hide)
                    }
                }
            } else if (stuckStaticAnimObj != null) {
                // C++: pos = StuckStaticAnimObj->Peek_Model()->Get_Bone_Transform(StuckBone) * StuckOffset
                val staticModel = stuckStaticAnimObj!!.peekModel()
                if (staticModel != null) {
                    val pos = staticModel.getBoneTransform(stuckBone) * stuckOffset
                    setPosition(pos)
                }
            } else {
                // C++: Delete without exploding if my object is gone
                if (stuckToObject) {
                    setDeletePending()
                }
            }
        }
    }

    // C++: virtual bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        val ammoDefId = ammoDefinition?.id?.toInt() ?: 0
        csave.writeMicroChunk(MICROCHUNKID_AMMO_DEF_ID,               ammoDefId)
        csave.writeMicroChunk(MICROCHUNKID_DETONATION_MODE,           detonationMode)
        csave.writeMicroChunk(MICROCHUNKID_TIMER,                     timer)
        csave.writeMicroChunk(MICROCHUNKID_STUCK,                     stuck)
        csave.writeMicroChunk(MICROCHUNKID_STUCK_OFFSET,              stuckOffset)
        csave.writeMicroChunk(MICROCHUNKID_STUCK_MCT,                 stuckMct)
        csave.writeMicroChunk(MICROCHUNKID_STUCK_BONE,                stuckBone)
        csave.writeMicroChunk(MICROCHUNKID_STUCK_TO_OBJECT,           stuckToObject)
        val staticAnimObj = stuckStaticAnimObj
        if (staticAnimObj != null) {
            val id = staticAnimObj.getId()
            csave.writeMicroChunk(MICROCHUNKID_STUCK_STATIC_ANIM_OBJ_ID, id)
        }
        csave.writeMicroChunk(MICROCHUNKID_AGE, age)
        csave.endChunk()

        if (owner.get() != null) {
            csave.beginChunk(CHUNKID_OWNER)
            owner.save(csave)
            csave.endChunk()
        }

        if (stuckObject.get() != null) {
            csave.beginChunk(CHUNKID_STUCK_OBJECT)
            stuckObject.save(csave)
            csave.endChunk()
        }

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        stuckStaticAnimObj = null
        var staticAnimObjId: UInt = 0xFFFFFFFFu

        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)

                CHUNKID_VARIABLES -> {
                    var ammoDefId = 0
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_AMMO_DEF_ID               -> ammoDefId      = cload.readInt()
                            MICROCHUNKID_DETONATION_MODE           -> detonationMode = cload.readInt()
                            MICROCHUNKID_TIMER                     -> timer          = cload.readFloat()
                            MICROCHUNKID_STUCK                     -> stuck          = cload.readBool()
                            MICROCHUNKID_STUCK_OFFSET              -> stuckOffset    = cload.readVector3()
                            MICROCHUNKID_STUCK_MCT                 -> stuckMct       = cload.readBool()
                            MICROCHUNKID_STUCK_BONE                -> stuckBone      = cload.readInt()
                            MICROCHUNKID_STUCK_STATIC_ANIM_OBJ_ID -> staticAnimObjId = cload.readUInt()
                            MICROCHUNKID_STUCK_TO_OBJECT           -> stuckToObject  = cload.readBool()
                            MICROCHUNKID_AGE                       -> age            = cload.readFloat()
                            else -> error("Unrecognized C4GameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                    // C++: AmmoDefinition = WeaponManager::Find_Ammo_Definition(ammo_def_id)
                    ammoDefinition = WeaponManager.findAmmoDefinition(ammoDefId)
                }

                CHUNKID_OWNER        -> owner.load(cload)
                CHUNKID_STUCK_OBJECT -> stuckObject.load(cload)

                else -> error("Unrecognized C4GameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // C++: Resolve StaticAnimPhysClass pointer from saved ID
        if (staticAnimObjId != 0xFFFFFFFFu) {
            // C++: StaticPhysClass * pobj = PhysicsSceneClass::Get_Instance()->Get_Static_Object_By_ID(staticAnimObjId)
            // FIXME: wire PhysicsSceneClass.getInstance().getStaticObjectById(staticAnimObjId) when ported
        }

        return true
    }

    // C++: virtual void Completely_Damaged(const OffenseObjectClass & damager)
    override fun completelyDamaged(damager: OffenseObjectClass) {
        defuse()
    }

    // C++: void Get_Information(StringClass & string)
    override fun getInformation(): String {
        val sb = StringBuilder(super.getInformation())
        val def = ammoDefinition
        if (def != null && def.ammoType == AMMO_TYPE_C4_TIMED) {
            sb.append("Timer: %.1f\n".format(timer.toDouble()))
        }
        return sb.toString()
    }

    // C++: virtual void Export_Rare(BitStreamClass & packet)
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)

        // C++: int ammo_def_id = AmmoDefinition != NULL ? AmmoDefinition->Get_ID() : 0
        val ammoDefId = ammoDefinition?.id?.toInt() ?: ammoDef
        packet.addInt(ammoDefId)

        // C++: int owner_id = Get_Owner() ? Get_Owner()->Get_ID() : 0
        val ownerNetId = (owner.get() as? PhysicalGameObj)?.networkId ?: ownerId
        packet.addInt(ownerNetId)

        // C++: vel from ProjectileClass if available; otherwise stored velX/Y/Z
        packet.addFloat(velX, BITPACK_VEHICLE_VELOCITY)
        packet.addFloat(velY, BITPACK_VEHICLE_VELOCITY)
        packet.addFloat(velZ, BITPACK_VEHICLE_VELOCITY)

        // C++: Synchronize the stuck state of C4
        packet.addBool(stuck)
        if (stuck) {
            // C++: pos from Get_Position() (derived from physObj or stored)
            val pos = if (physObj != null) getPosition() else Vector3(stuckPosX, stuckPosY, stuckPosZ)
            packet.addFloat(pos.x, BITPACK_WORLD_POSITION_X)
            packet.addFloat(pos.y, BITPACK_WORLD_POSITION_Y)
            packet.addFloat(pos.z, BITPACK_WORLD_POSITION_Z)

            packet.addBool(stuckMct)
            packet.addBool(stuckToObject)

            val stuckObjNetId = (stuckObject.get() as? PhysicalGameObj)?.networkId ?: stuckObjectId
            packet.addInt(stuckObjNetId)

            if (stuckToObject) {
                // C++: StuckOffset using velocity packing
                packet.addFloat(stuckOffset.x, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(stuckOffset.y, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(stuckOffset.z, BITPACK_VEHICLE_VELOCITY)
                packet.addInt(stuckBone)
            }

            val hasStuckStaticAnim = stuckStaticAnimObj != null || stuckStaticAnim
            packet.addBool(hasStuckStaticAnim)
            if (hasStuckStaticAnim) {
                val staticAnimId = stuckStaticAnimObj?.getId() ?: stuckStaticAnimObjId
                packet.addInt(staticAnimId)
            }
        }
    }

    // C++: virtual void Import_Rare(BitStreamClass & packet)
    override fun importRare(packet: BitStream) {
        super.importRare(packet)

        val ammoDefId = packet.getInt()
        ammoDef = ammoDefId
        if (ammoDefId != 0) {
            ammoDefinition = WeaponManager.findAmmoDefinition(ammoDefId)
            val def = ammoDefinition
            if (def != null && def.modelFilename.isNotEmpty()) {
                physObj?.setModelByName(def.modelFilename)
            }
        }

        val ownerNetId = packet.getInt()
        ownerId = ownerNetId
        if (ownerNetId != 0) {
            owner.set(GameObjManager.findSmartGameObj(ownerNetId))
        } else {
            owner.set(null)
        }

        velX = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
        velY = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
        velZ = packet.getFloat(BITPACK_VEHICLE_VELOCITY)

        // C++: if (po) po->Set_Velocity(vel)
        val po = physObj?.asProjectileClass()
        if (po != null) {
            po.setVelocity(Vector3(velX, velY, velZ))
        }

        // C++: Synchronize the stuck state of C4
        stuck = packet.getBool()
        if (stuck) {
            physObj?.enableUserControl(true)

            // C++: Update position
            val posX = packet.getFloat(BITPACK_WORLD_POSITION_X)
            val posY = packet.getFloat(BITPACK_WORLD_POSITION_Y)
            val posZ = packet.getFloat(BITPACK_WORLD_POSITION_Z)
            stuckPosX = posX; stuckPosY = posY; stuckPosZ = posZ

            // C++: if ((local_pos - pos).Length2() > 0.5 * 0.5) po->Set_Position(pos)
            if (physObj != null) {
                val localPos = getPosition()
                val dx = localPos.x - posX; val dy = localPos.y - posY; val dz = localPos.z - posZ
                if (dx * dx + dy * dy + dz * dz > 0.5f * 0.5f) {
                    val stuckPo = physObj?.asProjectileClass()
                    stuckPo?.setPosition(Vector3(posX, posY, posZ))
                        ?: setPosition(Vector3(posX, posY, posZ))
                }
            }

            stuckMct = packet.getBool()
            stuckToObject = packet.getBool()

            val stuckObjId = packet.getInt()
            stuckObjectId = stuckObjId
            stuckObject.set(GameObjManager.findScriptableGameObj(stuckObjId))

            if (stuckToObject) {
                val ox = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
                val oy = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
                val oz = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
                stuckOffset = Vector3(ox, oy, oz)
                stuckBone    = packet.getInt()
            }

            val hasStuckStaticAnim = packet.getBool()
            stuckStaticAnim = hasStuckStaticAnim
            if (hasStuckStaticAnim) {
                val staticAnimId = packet.getInt().toUInt()
                stuckStaticAnimObjId = staticAnimId.toInt()
                if (staticAnimId != 0xFFFFFFFFu) {
                    // C++: StaticPhysClass * pobj = PhysicsSceneClass::Get_Instance()->Get_Static_Object_By_ID(staticAnimId)
                    // FIXME: wire PhysicsSceneClass.getInstance().getStaticObjectById(staticAnimId) when ported
                }
            }
        }
    }

    // C++: void Defuse()
    fun defuse() {
        // C++: if (CombatManager::I_Am_Server()) — always server

        restoreOwner()

        // C++: find "KilledC4" ammo definition and trigger its explosion
        val disarmedAmmo = WeaponManager.findAmmoDefinitionByName("KilledC4")
        if (disarmedAmmo != null && disarmedAmmo.explosionDefId != 0) {
            val ownerNetId = getOwner()?.networkId ?: 0
            val pos = if (physObj != null) getTransform().getTranslation() else Vector3(stuckPosX, stuckPosY, stuckPosZ)
            ExplosionManager.serverExplode(disarmedAmmo.explosionDefId, pos, ownerNetId)
        }

        setDeletePending()
    }

    // C++: void Detonate()
    fun detonate() {
        // C++: if (CombatManager::I_Am_Server()) — always server

        restoreOwner()

        val def = ammoDefinition
        if (def != null && def.explosionDefId != 0) {
            val ownerNetId = getOwner()?.networkId ?: 0
            val pos = if (physObj != null) getTransform().getTranslation() else Vector3(stuckPosX, stuckPosY, stuckPosZ)
            var forceVictim: DamageableGameObj? = null
            if (stuck && stuckToObject) {
                forceVictim = stuckObject.get() as? DamageableGameObj
            }
            // C++: ExplosionManager::Server_Explode(ExplosionDefID, Get_Transform().Get_Translation(), owner_id, force_victim)
            ExplosionManager.serverExplode(def.explosionDefId, pos, ownerNetId, forceVictim)
        }

        // C++: If I am stuck to a building, apply damage to that building
        if (stuck) {
            val stuckBuilding = stuckObject.get() as? BuildingGameObj
            if (stuckBuilding != null) {
                val def2 = ammoDefinition
                if (def2 != null) {
                    // C++: ExplosionManager::Explosion_Damage_Building(ExplosionDefID, building, StuckMCT, Get_Owner())
                    ExplosionManager.explosionDamageBuilding(def2.explosionDefId, stuckBuilding, stuckMct, getOwner())
                }
            }
        }

        setDeletePending()
    }

    // C++: void Restore_Owner()
    // If Owner ref is NULL but OwnerBackup is set and ammo type isn't REMOTE,
    // scan smart game obj list to find an obj with matching player data.
    private fun restoreOwner() {
        if (getOwner() == null && ownerBackup != null &&
            ammoDefinition != null && ammoDefinition!!.ammoType != AMMO_TYPE_C4_REMOTE) {
            // C++: scan GameObjManager::Get_Smart_Game_Obj_List for obj->Get_Player_Data() == OwnerBackup
            for (obj in GameObjManager.getSmartGameObjs()) {
                if (obj.playerData === ownerBackup) {
                    owner.set(obj)
                    break
                }
            }

            if (getOwner() == null) {
                ownerBackup = null
                defuse()
            }
        }
    }

    companion object {
        // C++: #define C4_LIMIT 30
        const val C4_LIMIT = 30

        // C++: APPPACKETTYPE_C4 from apppackettypes.h — 4th entry (after UNKNOWN=0, SIMPLE=1, BUILDING=2, BASECONTROLLER=3)
        const val APPPACKETTYPE_C4: Byte = 4

        // C++: c4.cpp local enum starting at 922991750
        private const val CHUNKID_PARENT                         = 922991750
        private const val CHUNKID_VARIABLES                      = 922991751
        // 922991752 = XXXCHUNKID_C4_TIMER (legacy, never written)
        private const val CHUNKID_OWNER                          = 922991753
        private const val CHUNKID_STUCK_OBJECT                   = 922991754

        // C++: micro-chunk IDs (sequential starting at 1 after two legacy entries)
        // XXXXMICROCHUNKID_PARAMS_NAME  = 1 (legacy, two consecutive)
        // XXXXXMICROCHUNKID_PARAMS_NAME = 2 (legacy)
        private const val MICROCHUNKID_AMMO_DEF_ID                = 3
        private const val MICROCHUNKID_DETONATION_MODE            = 4
        private const val MICROCHUNKID_TIMER                      = 5
        private const val MICROCHUNKID_STUCK                      = 6
        private const val MICROCHUNKID_STUCK_OFFSET               = 7
        private const val MICROCHUNKID_STUCK_MCT                  = 8
        private const val MICROCHUNKID_STUCK_BONE                 = 9
        private const val MICROCHUNKID_STUCK_STATIC_ANIM_OBJ_ID   = 10
        private const val MICROCHUNKID_STUCK_TO_OBJECT            = 11
        private const val MICROCHUNKID_AGE                        = 12

        // C++: PHYSICS_CHUNKID_BUILDINGAGGREGATE — used to detect MCT collision
        private const val PHYSICS_CHUNKID_BUILDINGAGGREGATE: UInt = 0x0004011Au

        // C++: static void Maintain_C4_Limit(int player_type)
        // Removes the oldest non-timed C4 of the given player_type if count > C4_LIMIT.
        fun maintainC4Limit(playerType: Int) {
            // C++: if (!CombatManager::I_Am_Server() || IS_MISSION) return — server always, never mission

            var oldestC4: C4GameObj? = null
            var count = 0

            for (obj in GameObjManager.getGameObjs()) {
                val c4 = (obj as? PhysicalGameObj)?.asC4GameObj() ?: continue
                val def = c4.ammoDefinition ?: continue
                if (c4.playerType == playerType && def.ammoType != AMMO_TYPE_C4_TIMED) {
                    count++
                    if (oldestC4 == null || c4.age > oldestC4.age) {
                        oldestC4 = c4
                    }
                }
            }

            if (count > C4_LIMIT && oldestC4 != null) {
                oldestC4.defuse()
            }
        }
    }
}
