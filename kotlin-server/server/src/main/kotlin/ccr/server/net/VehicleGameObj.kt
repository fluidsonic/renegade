package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Quaternion
import ccr.math.Vector3
import ccr.math.degToRadF
import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import ccr.server.GameObjManager
import ccr.server.defs.VehicleGameObjDef

// C++: VehicleGameObj : public SmartGameObj
// Full hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//                 → PhysicalGameObj → ArmedGameObj → SmartGameObj → VehicleGameObj
open class VehicleGameObj() : SmartGameObj() {

    // -------------------------------------------------------------------------
    // Fields matching C++ VehicleGameObj members (vehicle.h / vehicle.cpp)
    // -------------------------------------------------------------------------

    // C++: Sound3DClass* Sound (initialized to NULL)
    var sound: Any? = null  // Sound3DClass — FIXME: Sound3DClass not yet ported

    // C++: int EngineSoundState (initialized to ENGINE_SOUND_STATE_OFF)
    var engineSoundState: Int = ENGINE_SOUND_STATE_OFF

    // C++: AudibleSoundClass* CachedEngineSound (initialized to NULL)
    var cachedEngineSound: Any? = null  // AudibleSoundClass — FIXME: AudibleSoundClass not yet ported

    // C++: int TurretBone (initialized to 0)
    var turretBone: Int = 0

    // C++: int BarrelBone (initialized to 0)
    var barrelBone: Int = 0

    // C++: float TurretTurn (initialized to 0)
    var turretTurn: Float = 0f

    // C++: float BarrelTilt (initialized to 0)
    var barrelTilt: Float = 0f

    // C++: float BarrelOffset (initialized to 0) — for vehicles with offset turrets
    var barrelOffset: Float = 0f

    // C++: bool TransitionsEnabled (initialized to true)
    var transitionsEnabled: Boolean = true

    // C++: bool HasEnterTransitions (initialized to false)
    var hasEnterTransitions: Boolean = false

    // C++: bool HasExitTransitions (initialized to false)
    var hasExitTransitions: Boolean = false

    // C++: bool VehicleDelivered (initialized to false)
    var vehicleDelivered: Boolean = false

    // C++: bool DriverIsGunner (initialized to DefaultDriverIsGunner in constructor)
    var driverIsGunner: Boolean = false

    // C++: VectorClass<PersistantSurfaceEmitterClass*> WheelSurfaceEmitters — FIXME: not yet ported
    // C++: PersistantSurfaceSoundClass* WheelSurfaceSound (initialized to NULL) — FIXME: not yet ported

    // C++: VectorClass<SoldierGameObj*> SeatOccupants
    // One slot per seat; null means empty. Resized in init() to definition.NumSeats.
    val seatOccupants: MutableList<SoldierGameObj?> = mutableListOf()

    // C++: int OccupiedSeats (initialized to 0)
    var occupiedSeats: Int = 0

    // C++: DynamicVectorClass<TransitionInstanceClass*> TransitionInstances — FIXME: not yet ported
    // val transitionInstances = mutableListOf<TransitionInstanceClass>()

    // C++: GameObjReference LockOwner
    val lockOwner: GameObjReference = GameObjReference()

    // C++: float LockTimer (initialized to 0)
    var lockTimer: Float = 0f

    // C++: static bool DefaultDriverIsGunner
    // C++: static bool CameraLockedToTurret
    // → companion object below

    // -------------------------------------------------------------------------
    // Constructor / Destructor
    // -------------------------------------------------------------------------

    init {
        // C++: VehicleGameObj() : ... DriverIsGunner(DefaultDriverIsGunner) { Set_App_Packet_Type(...) }
        driverIsGunner = defaultDriverIsGunner
        setAppPacketType(APPPACKETTYPE_VEHICLE)
    }

    // -------------------------------------------------------------------------
    // Server-port direct fields — used by secondary constructor and exportFrequent
    // when there is no real physObj (e.g. in tests).
    // -------------------------------------------------------------------------

    // Vehicle type constant — read from definition in normal flow; set directly in secondary ctor.
    var vehicleTypeOverride: Int = VEHICLE_TYPE_CAR

    // Physics state — read from VehiclePhysClass in normal flow; set directly in secondary ctor.
    var isEngineOn: Boolean = true
    var quaternion: ccr.math.Quaternion = ccr.math.Quaternion.IDENTITY
    @get:JvmName("velocityField") @set:JvmName("setVelocityField")
    var velocity: Vector3 = Vector3(0f, 0f, 0f)
    var angularVelocity: Vector3 = Vector3(0f, 0f, 0f)

    // Total rounds direct field — used when weaponBag is empty (secondary ctor with no weapons)
    var totalRoundsOverride: Int = 0

    // Secondary constructor for tests and server-side vehicle spawning.
    constructor(
        definitionId: Int,
        position: Vector3 = Vector3(0f, 0f, 0f),
        facing: Float = 0f,
        modelName: String = "",
        animName: String = "",
        health: Float = 0f,
        shieldStrength: Float = 0f,
        controlOwner: Int = 0,
        team: Int = PLAYERTYPE_NEUTRAL,
        vehicleType: Int = VEHICLE_TYPE_CAR,
        seatCount: Int = 1,
        lockOwnerId: Int = 0,
        lockTimer: Float = 0f,
        vehicleDelivered: Boolean = false,
        isEngineOn: Boolean = true,
        quaternion: ccr.math.Quaternion = ccr.math.Quaternion.IDENTITY,
        velocity: Vector3 = Vector3(0f, 0f, 0f),
        angularVelocity: Vector3 = Vector3(0f, 0f, 0f),
        totalRounds: Int = 0,
        driverIsGunner: Boolean = false,
        seatOccupantIds: List<Int> = emptyList(),
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "vehicle_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.facing = facing
        this.modelName = modelName
        this.animName = animName
        defenseObject.health = health
        defenseObject.shieldStrength = shieldStrength
        this.controlOwner = controlOwner
        playerType = team
        vehicleTypeOverride = vehicleType
        repeat(seatCount) { this.seatOccupants.add(null) }
        if (lockOwnerId != 0) {
            // Store lock owner ID directly (no GameObj lookup available in tests)
            this.lockTimer = lockTimer
        }
        this.vehicleDelivered = vehicleDelivered
        this.isEngineOn = isEngineOn
        this.quaternion = quaternion
        this.velocity = velocity
        this.angularVelocity = angularVelocity
        totalRoundsOverride = totalRounds
        this.driverIsGunner = driverIsGunner
        // Note: lockOwnerId is stored for exportCreation via a synthetic ref
        if (lockOwnerId != 0) {
            // We encode lock owner by writing its ID; store in lockTimer only, since
            // GameObjReference needs a real object. Use a special approach in exportCreation.
            _lockOwnerIdForExport = lockOwnerId
        }
    }

    // Direct storage for lock owner ID when set via secondary constructor (no GameObj available)
    internal var _lockOwnerIdForExport: Int = 0

    // C++: ~VehicleGameObj()
    override fun destruct() {
        val physObj = peekPhysicalObject()
        if (physObj != null && CombatManager.getScene() != null) {
            // Make sure the exiters don't hit me
            physObj.setCollisionGroup(UNCOLLIDEABLE_GROUP)
            // Eject all occupants around the vehicle so they don't intersect
            val vehiclePos = physObj.getPosition()
            for (i in seatOccupants.indices) {
                seatOccupants[i]?.exitDestroyedVehicle(i, vehiclePos)
                seatOccupants[i] = null
            }
        } else {
            for (i in seatOccupants.indices) {
                seatOccupants[i]?.exitVehicle()
                seatOccupants[i] = null
            }
        }

        destroyTransitions()

        // C++: Sound->Remove_From_Scene(); Sound->Release_Ref(); Sound = NULL
        sound = null  // GC handles

        // C++: CachedEngineSound->Remove_From_Scene(); REF_PTR_RELEASE(CachedEngineSound)
        cachedEngineSound = null  // GC handles

        releaseTurretBones()
        shutdownWheelEffects()

        super.destruct()
    }

    // -------------------------------------------------------------------------
    // Init / Copy_Settings
    // -------------------------------------------------------------------------

    // C++: virtual void Init() — used for re-init path; vehicles are too complex to re-init
    override fun init() {
        driverIsGunner = defaultDriverIsGunner
        // C++: "Vehicles are too complex to re-init" — no-op
    }

    // C++: void Init(const VehicleGameObjDef & definition)
    fun init(definition: VehicleGameObjDef) {
        driverIsGunner = defaultDriverIsGunner
        super.init(definition)

        // C++: SeatOccupants.Resize(definition.NumSeats) + zero-fill
        val numSeats = definition.numSeats
        seatOccupants.clear()
        repeat(numSeats) { seatOccupants.add(null) }

        acquireTurretBones()
        initWheelEffects()
        createAndDestroyTransitions()
        updateDamageMeshes()

        // C++: if (type == VEHICLE_TYPE_TURRET) Set_App_Packet_Type(APPPACKETTYPE_TURRET)
        if (definition.type.value == VEHICLE_TYPE_TURRET) {
            setAppPacketType(APPPACKETTYPE_TURRET)
        }
    }

    // C++: const VehicleGameObjDef & Get_Definition() const
    fun getDefinition(): VehicleGameObjDef = definition as VehicleGameObjDef

    // -------------------------------------------------------------------------
    // Save / Load
    // -------------------------------------------------------------------------

    // C++: virtual bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        // C++: Destroy transitions before saving (no matched reference to them after)
        destroyTransitions()

        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_TURRET_TURN,         turretTurn)
        csave.writeMicroChunk(MICROCHUNKID_BARREL_TILT,         barrelTilt)
        csave.writeMicroChunk(MICROCHUNKID_TRANSITIONS_ENABLED, transitionsEnabled)
        csave.writeMicroChunk(MICROCHUNKID_OCCUPIED_SEATS,      occupiedSeats)
        val numSeats = seatOccupants.size
        csave.writeMicroChunk(MICROCHUNKID_NUM_SEATS,           numSeats)
        csave.endChunk()

        // C++: if (num_seats != 0) { begin CHUNKID_SEAT_LIST; write raw pointer array }
        if (numSeats != 0) {
            csave.beginChunk(CHUNKID_SEAT_LIST)
            for (occupant in seatOccupants) {
                csave.writeInt(occupant?.networkId ?: 0)
            }
            csave.endChunk()
        }

        // C++: Create_And_Destroy_Transitions() at end of Save
        createAndDestroyTransitions()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        var numSeats = 0

        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)

                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_TURRET_BONE         -> turretBone        = cload.readInt()
                            MICROCHUNKID_BARREL_BONE         -> barrelBone        = cload.readInt()
                            MICROCHUNKID_TURRET_TURN         -> turretTurn        = cload.readFloat()
                            MICROCHUNKID_BARREL_TILT         -> barrelTilt        = cload.readFloat()
                            MICROCHUNKID_TRANSITIONS_ENABLED -> transitionsEnabled = cload.readBool()
                            MICROCHUNKID_OCCUPIED_SEATS      -> occupiedSeats     = cload.readInt()
                            MICROCHUNKID_NUM_SEATS           -> numSeats          = cload.readInt()
                            else -> error("Unrecognized VehicleGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }

                CHUNKID_SEAT_LIST -> {
                    // C++: raw pointer array for each seat.
                    // if (num_seats == 0 || num_seats != Get_Definition().NumSeats) → legacy, use def size
                    val defNumSeats = (definition as? VehicleGameObjDef)?.numSeats ?: 0
                    if (numSeats == 0 || numSeats != defNumSeats) {
                        // C++: break (legacy) — seats already zero-filled above, skip chunk
                        seatOccupants.clear()
                        repeat(defNumSeats) { seatOccupants.add(null) }
                        cload.skipChunk()
                    } else {
                        // C++: cload.Read(&SeatOccupants[0], num_seats * sizeof(ptr)) + REQUEST_POINTER_REMAP
                        // In Kotlin: skip the raw pointer bytes (they were 32-bit Win32 pointers, not valid here)
                        // FIXME: REQUEST_POINTER_REMAP for SeatOccupants — post-load callback resolves by ID
                        seatOccupants.clear()
                        repeat(numSeats) { seatOccupants.add(null) }
                        cload.skipChunk()
                    }
                }

                XXXCHUNKID_ANIM_CONTROL_OLD        -> cload.skipChunk()
                XXXCHUNKID_TRANSITION_LIST_OLD     -> cload.skipChunk()

                else -> error("Unrecognized VehicleGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        return true
    }

    // C++: virtual void On_Post_Load()
    override fun onPostLoad() {
        driverIsGunner = defaultDriverIsGunner

        initWheelEffects()
        acquireTurretBones()
        super.onPostLoad()
        createAndDestroyTransitions()
        updateDamageMeshes()

        // C++: if AnimControl != NULL && AnimControl->Peek_Model() == NULL → Set_Model(Peek_Model())
        val anim = getAnimControl()
        if (anim != null && anim.peekModel() == null) {
            anim.setModel(peekModel())
        }

        // C++: if (type == VEHICLE_TYPE_TURRET) Set_App_Packet_Type(APPPACKETTYPE_TURRET)
        val def = definition as? VehicleGameObjDef
        if (def != null && def.type.value == VEHICLE_TYPE_TURRET) {
            setAppPacketType(APPPACKETTYPE_TURRET)
        }
    }

    // -------------------------------------------------------------------------
    // Think / Post_Think
    // -------------------------------------------------------------------------

    // C++: virtual void Think()
    override fun think() {
        applyControl()        // C++: Apply_Control() before super
        updateTransitions()

        super.think()         // SmartGameObj::Think()

        updateSoundEffects()
        updateWheelEffects()

        // C++: Update the lock status
        if (lockTimer > 0f) {
            lockTimer -= TimeManager.getFrameSeconds()
        } else {
            lockOwner.set(null)
        }

        // C++: Unstealth if we don't have any occupants, and we aren't in single play
        if (stealthEffect != null) {
            if (getOccupantCount() == 0 /* && !IS_MISSION */ ) {
                stealthEffect!!.enableStealth(false)
            }
        }
    }

    // C++: virtual void Post_Think()
    override fun postThink() {
        val model = peekModel()
        check(model != null)

        // C++: for each seat occupant, teleport to SEAT bone
        for (i in seatOccupants.indices) {
            val occupant = seatOccupants[i] ?: continue
            val boneName = "SEAT$i"
            val seatBoneIndex = model.getBoneIndex(boneName)
            if (seatBoneIndex != -1) {
                val seat = model.getBoneTransform(seatBoneIndex)
                if (!occupant.inTransition()) {
                    occupant.setTransform(seat)
                }
            }
        }

        super.postThink()   // SmartGameObj::Post_Think()

        // C++: Weapon fire animation
        val weapon = getWeapon()
        val def = definition as? VehicleGameObjDef
        if (weapon != null && def != null && def.fire0Anim.isNotEmpty()) {
            weapon.resetAnimUpdate()
            when (weapon.getAnimState()) {
                WEAPON_ANIM_NOT_FIRING -> setAnimation(null)
                WEAPON_ANIM_FIRING_0   -> setAnimation(def.fire0Anim)
                WEAPON_ANIM_FIRING_1   -> {
                    val fire1 = def.fire1Anim
                    setAnimation(if (fire1.isNotEmpty()) fire1 else def.fire0Anim)
                }
            }
        }
    }

    // C++: virtual void Startup()
    override fun startup() {
        createAndDestroyTransitions()
    }

    // -------------------------------------------------------------------------
    // Control
    // -------------------------------------------------------------------------

    // C++: virtual void Apply_Control()
    override fun applyControl() {
        val driver = getDriver()
        var gunner = getGunner()

        if (gunner == null || driverIsGunner) {
            gunner = driver
        }

        if (!CombatManager.isGameplayPermitted()) {
            clearControl()
            controller.reset()
            (physObj?.asVehiclePhysClass())?.setVelocity(Vector3(0f, 0f, 0f))
            return
        }

        var targetSteering = false

        // C++: If I have a driver, use his control
        if (driver != null && !driver.inTransition() && driver.isHumanControlled()) {
            control.copyFrom(driver.control)
            targetSteering = true
        }

        // C++: If I have a gunner (and he is not the driver), use his weapon controls
        if (gunner != null && gunner !== driver && !gunner.inTransition() && !driverIsGunner) {
            control.setBoolean(
                ControlClass.BooleanControl.WEAPON_FIRE_PRIMARY,
                gunner.control.getBoolean(ControlClass.BooleanControl.WEAPON_FIRE_PRIMARY)
            )
            control.setBoolean(
                ControlClass.BooleanControl.WEAPON_FIRE_SECONDARY,
                gunner.control.getBoolean(ControlClass.BooleanControl.WEAPON_FIRE_SECONDARY)
            )
            control.setBoolean(
                ControlClass.BooleanControl.WEAPON_RELOAD,
                gunner.control.getBoolean(ControlClass.BooleanControl.WEAPON_RELOAD)
            )
        }

        // C++: Vehicles never change weapons
        control.setBoolean(ControlClass.BooleanControl.WEAPON_NEXT, false)
        control.setBoolean(ControlClass.BooleanControl.WEAPON_PREV, false)

        // C++: Target steering
        if (targetSteering && useTargetSteering) {
            if (control.getAnalog(ControlClass.AnalogControl.MOVE_FORWARD) >= 0f) {
                val targetPos = getTargetingPos()
                val objSpaceTarget = Vector3()
                getTransform().inverseTransformVector(targetPos, objSpaceTarget)
                val targetDirection = Math.atan2(
                    objSpaceTarget.y.toDouble(), objSpaceTarget.x.toDouble()
                ).toFloat()
                val TARGET_STRAFE_ANGLE = degToRadF(50f)
                val adjustedDirection = targetDirection + TARGET_STRAFE_ANGLE *
                    control.getAnalog(ControlClass.AnalogControl.TURN_LEFT)
                val fwd = control.getAnalog(ControlClass.AnalogControl.MOVE_FORWARD)
                val turnAmount = adjustedDirection / degToRadF(60f) * fwd
                control.setAnalog(ControlClass.AnalogControl.TURN_LEFT, turnAmount)
            } else {
                if (control.getAnalog(ControlClass.AnalogControl.MOVE_FORWARD) > 0f) {
                    val cur = control.getAnalog(ControlClass.AnalogControl.TURN_LEFT)
                    control.setAnalog(ControlClass.AnalogControl.TURN_LEFT, 0.5f * cur)
                }
            }
        }

        // C++: Clamp turning to [-1, 1]
        val turn = control.getAnalog(ControlClass.AnalogControl.TURN_LEFT)
        control.setAnalog(ControlClass.AnalogControl.TURN_LEFT, turn.coerceIn(-1f, 1f))

        super.applyControl()
    }

    // C++: virtual int Get_Weapon_Control_Owner()
    override fun getWeaponControlOwner(): Int {
        val gunner = getGunner()
        if (gunner != null && !driverIsGunner) {
            return gunner.controlOwner
        }
        val driver = getDriver()
        if (driver != null) {
            return driver.controlOwner
        }
        return controlOwner
    }

    // -------------------------------------------------------------------------
    // Weapon / Targeting
    // -------------------------------------------------------------------------

    // C++: virtual bool Set_Targeting(const Vector3& pos, bool do_tilt)
    override fun setTargeting(pos: Vector3, doTilt: Boolean): Boolean {
        var ready = true

        super.setTargeting(pos, doTilt)

        var relativeTurn = 0f
        var relativeTilt = 0f

        if (turretBone != 0) {
            val model = peekModel()!!
            val turretBase = model.getBoneTransform(turretBone)
            val turretSpaceTarget = Vector3()
            turretBase.inverseTransformVector(pos, turretSpaceTarget)

            relativeTurn = Math.atan2(
                turretSpaceTarget.y.toDouble(), turretSpaceTarget.x.toDouble()
            ).toFloat()

            if (barrelOffset != 0f) {
                turretSpaceTarget.z = 0f
                val barrelOffsetAngle = Math.atan2(
                    barrelOffset.toDouble(), turretSpaceTarget.length().toDouble()
                ).toFloat()
                relativeTurn -= barrelOffsetAngle
            }
        }

        val def = getDefinition()
        val weaponTurnRate = def.armed.weaponTurnRate

        val maxMoveTurn = weaponTurnRate * TimeManager.getFrameSeconds()
        if (weaponTurnRate < degToRadF(1000f)) {
            turretTurn += relativeTurn.coerceIn(-maxMoveTurn, maxMoveTurn)
            if (Math.abs(relativeTurn) > Math.abs(maxMoveTurn)) ready = false
        } else {
            turretTurn += relativeTurn
        }
        turretTurn = turretTurn.coerceIn(def.armed.weaponTurnMin, def.armed.weaponTurnMax)

        val maxMoveTilt = def.armed.weaponTiltRate * TimeManager.getFrameSeconds()
        if (doTilt) {
            if (turretBone != 0 && barrelBone != 0 && Math.abs(relativeTurn) < degToRadF(80f)) {
                val model = peekModel()!!
                val barrelBase = model.getBoneTransform(barrelBone)
                val barrelSpaceTarget = Vector3()
                barrelBase.inverseTransformVector(pos, barrelSpaceTarget)
                val dist = barrelSpaceTarget.length()
                if (dist != 0f) {
                    relativeTilt = Math.asin(
                        (barrelSpaceTarget.z / dist).toDouble().coerceIn(-1.0, 1.0)
                    ).toFloat()
                }
            }
            if (def.armed.weaponTiltRate < degToRadF(1000f)) {
                barrelTilt += relativeTilt.coerceIn(-maxMoveTilt, maxMoveTilt)
                if (Math.abs(relativeTilt) > Math.abs(maxMoveTilt)) ready = false
            } else {
                barrelTilt += relativeTilt
            }
        }
        barrelTilt = barrelTilt.coerceIn(def.armed.weaponTiltMin, def.armed.weaponTiltMax)

        updateTurret(turretTurn, barrelTilt)

        // C++: fast-turning vehicles may recurse to get correct bone position
        if (weaponTurnRate > degToRadF(1000f) &&
            (Math.abs(relativeTurn) >= degToRadF(2f) || Math.abs(relativeTilt) > degToRadF(2f))) {
            // FIXME: recursive call limited to 3 — not ported here; single-pass sufficient on server
            ready = true
        }

        return ready
    }

    // C++: virtual bool Use_2D_Aiming() { return Get_Definition().Aim2D; }
    fun use2DAiming(): Boolean = getDefinition().aim2D

    // C++: virtual Matrix3D Get_Look_Transform()
    override fun getLookTransform(): ccr.math.Matrix3D {
        return if (getDefinition().sightDownMuzzle) {
            getMuzzle()
        } else {
            super.getLookTransform()
        }
    }

    // C++: bool Has_Turret() { return TurretBone != 0; }
    fun hasTurret(): Boolean = turretBone != 0

    // -------------------------------------------------------------------------
    // Type identification
    // -------------------------------------------------------------------------

    // C++: virtual VehicleGameObj* As_VehicleGameObj() { return this; }
    // (In Kotlin, use `this is VehicleGameObj` at call sites — no explicit method needed)

    // C++: virtual bool Is_Aircraft() { return type == VEHICLE_TYPE_FLYING; }
    fun isAircraft(): Boolean = getDefinition().type.value == VEHICLE_TYPE_FLYING

    // C++: virtual bool Is_Turret() { return type == VEHICLE_TYPE_TURRET; }
    fun isTurret(): Boolean = getDefinition().type.value == VEHICLE_TYPE_TURRET

    // C++: virtual int Get_Player_Type() const — override via computed property getter
    // NOTE: `seatOccupants` is a non-null val but may be JVM-null during superclass construction
    // (Kotlin dispatches virtual methods at construction time; C++ does not). Guard with try/catch.
    override var playerType: Int
        get() {
            // If they have an occupant, they are that occupant's team.
            try {
                for (occupant in seatOccupants) {
                    if (occupant != null) return occupant.playerType
                }
            } catch (_: NullPointerException) {
                // seatOccupants not yet initialized during superclass construction
            }
            return super.playerType
        }
        set(value) { super.playerType = value }

    // -------------------------------------------------------------------------
    // Occupants
    // -------------------------------------------------------------------------

    // C++: void Add_Occupant(SoldierGameObj* occupant) — adds to lowest empty seat
    fun addOccupant(occupant: SoldierGameObj) {
        val def = getDefinition()
        for (i in 0 until def.numSeats) {
            if (seatOccupants.getOrNull(i) == null) {
                addOccupant(occupant, i)
                break
            }
        }
    }

    // C++: void Add_Occupant(SoldierGameObj* occupant, int seat_id)
    fun addOccupant(occupant: SoldierGameObj, seatId: Int) {
        if (seatOccupants.getOrNull(seatId) != null) {
            // C++: Debug_Say — seat already occupied
            return
        }

        if (findSeat(occupant) != -1) {
            // C++: Debug_Say — soldier already in vehicle
        }

        if (seatId == DRIVER_SEAT) {
            driverIsGunner = defaultDriverIsGunner
        }

        seatOccupants[seatId] = occupant
        occupiedSeats++
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)

        // C++: choose vehicle anim based on vehicle type
        val def = getDefinition()
        val animName = if (def.type.value == VEHICLE_TYPE_BIKE) {
            "S_A_HUMAN.H_A_V20A"
        } else {
            "S_A_HUMAN.H_A_V10A"
        }
        occupant.enterVehicle(this, animName)

        // C++: Create_And_Destroy_Transitions(); Enable_Engine(true)
        createAndDestroyTransitions()
        enableEngine(true)

        // C++: if OccupantsVisible == false → hide occupant model
        if (!def.occupantsVisible) {
            occupant.peekModel()?.setHidden(true)
        }

        // C++: Notify observers — CUSTOM_EVENT_VEHICLE_ENTERED
        for (observer in observers.toList()) {
            observer.custom(this, CUSTOM_EVENT_VEHICLE_ENTERED, seatId, occupant)
        }

        // C++: if (occupant == COMBAT_STAR) EncyclopediaMgrClass::Reveal_Object(this)
        // FIXME: EncyclopediaMgrClass::Reveal_Object — not yet ported

        // C++: Get_Action()->Reset(1)
        action.reset(1)

        // C++: Unlock the vehicle
        lockTimer = 0f
        lockOwner.set(null)
    }

    // C++: void Remove_Occupant(SoldierGameObj* occupant)
    fun removeOccupant(occupant: SoldierGameObj) {
        val seatNum = findSeat(occupant)
        if (seatNum == -1) {
            return
        }

        val def = getDefinition()
        // C++: if OccupantsVisible == false → unhide occupant model
        if (!def.occupantsVisible) {
            val model = seatOccupants[seatNum]?.peekModel()
            model?.setHidden(false)
        }

        seatOccupants[seatNum]!!.exitVehicle()
        seatOccupants[seatNum] = null
        occupiedSeats--

        // C++: in MP, empty vehicles are neutral
        // if (!IS_MISSION && occupiedSeats == 0) setPlayerType(PLAYERTYPE_NEUTRAL)
        if (occupiedSeats == 0) {
            playerType = PLAYERTYPE_NEUTRAL
        }

        setObjectDirtyBit(NetworkObject.BIT_RARE, true)

        createAndDestroyTransitions()

        // C++: if (seat_num == 0) Enable_Engine(false)
        if (seatNum == DRIVER_SEAT) {
            enableEngine(false)
        }

        // C++: Notify observers — CUSTOM_EVENT_VEHICLE_EXITED
        for (observer in observers.toList()) {
            observer.custom(this, CUSTOM_EVENT_VEHICLE_EXITED, seatNum, occupant)
        }
    }

    // C++: bool Contains_Occupant(SoldierGameObj* occupant)
    fun containsOccupant(occupant: SoldierGameObj): Boolean {
        return seatOccupants.any { it === occupant }
    }

    // C++: int Get_Occupant_Count()
    fun getOccupantCount(): Int = seatOccupants.count { it != null }

    // C++: int Find_Seat(SoldierGameObj* occupant)
    fun findSeat(occupant: SoldierGameObj): Int {
        for (i in seatOccupants.indices) {
            if (seatOccupants[i] === occupant) return i
        }
        return -1
    }

    // C++: SoldierGameObj* Get_Driver()
    fun getDriver(): SoldierGameObj? {
        return if (seatOccupants.size > DRIVER_SEAT) seatOccupants[DRIVER_SEAT] else null
    }

    // C++: SoldierGameObj* Get_Gunner()
    fun getGunner(): SoldierGameObj? {
        return if (seatOccupants.size > GUNNER_SEAT) seatOccupants[GUNNER_SEAT] else null
    }

    // C++: SoldierGameObj* Get_Actual_Gunner()
    fun getActualGunner(): SoldierGameObj? {
        val gunner = getGunner()
        if (gunner != null && !driverIsGunner) return gunner
        return getDriver()
    }

    // C++: virtual bool Is_Entry_Permitted(SoldierGameObj* p_soldier)
    open fun isEntryPermitted(soldier: SoldierGameObj): Boolean {
        if (!CombatManager.isGameplayPermitted()) return false

        val playerType = soldier.getPlayerType()
        for (occupant in seatOccupants) {
            if (occupant != null) {
                val seatedPt = occupant.getPlayerType()
                if (seatedPt != playerType ||
                    (seatedPt != PLAYERTYPE_NOD && seatedPt != PLAYERTYPE_GDI)) {
                    return false
                }
            }
        }

        // C++: If there are no entry transitions, entry is not possible
        // FIXME: TransitionInstances check — TransitionInstanceClass not yet ported; assume always permitted

        // C++: If the vehicle is locked and the soldier is not the lock owner, entry not permitted
        if (isLocked() && soldier !== lockOwner.get()) return false

        return true
    }

    // C++: void Passenger_Entering()
    fun passengerEntering() {
        val def = getDefinition()
        val anim = "V_${def.typeName}L1.M_${def.typeName}CL"
        setAnimation(anim)
        getAnimControl()?.setMode(ANIM_MODE_ONCE)
    }

    // C++: void Passenger_Exiting()
    fun passengerExiting() {
        val def = getDefinition()
        val anim = "V_${def.typeName}L1.M_${def.typeName}OP"
        setAnimation(anim)
        getAnimControl()?.setMode(ANIM_MODE_ONCE)
        control.clearControl()
    }

    // C++: void Set_Vehicle_Delivered()
    fun setVehicleDelivered() {
        vehicleDelivered = true
        setObjectDirtyBit(NetworkObject.BIT_RARE, true)
    }

    // -------------------------------------------------------------------------
    // Network Export / Import
    // -------------------------------------------------------------------------

    // C++: virtual void Export_Creation(BitStreamClass & packet)
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)

        // C++: Send the lock status to the client
        // Use _lockOwnerIdForExport when lockOwner ref has no real object (secondary ctor path)
        val lockOwnerId = lockOwner.get()?.networkId ?: _lockOwnerIdForExport
        packet.addInt(lockOwnerId)
        if (lockOwnerId != 0) {
            packet.addFloat(lockTimer, BITPACK_VEHICLE_LOCK_TIMER)
        }
    }

    // C++: virtual void Import_Creation(BitStreamClass & packet)
    override fun importCreation(packet: BitStream) {
        super.importCreation(packet)

        val lockOwnerId = packet.getInt()
        if (lockOwnerId != 0) {
            lockOwner.set(GameObjManager.findPhysicalGameObj(lockOwnerId))
            lockTimer = packet.getFloat(BITPACK_VEHICLE_LOCK_TIMER)
        }
    }

    // C++: virtual void Export_Rare(BitStreamClass & packet)
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)

        // C++: Export the seat occupants
        for (occupant in seatOccupants) {
            if (occupant == null) {
                packet.addInt(NO_OCCUPANT)
            } else {
                packet.addInt(occupant.networkId)
            }
        }

        packet.addBool(vehicleDelivered)
    }

    // C++: virtual void Import_Rare(BitStreamClass & packet)
    override fun importRare(packet: BitStream) {
        super.importRare(packet)

        // C++: Update the seat occupants
        for (i in seatOccupants.indices) {
            val occupantId = packet.getInt()
            if (occupantId == NO_OCCUPANT) {
                // Remove the occupant from the seat if present
                val current = seatOccupants[i]
                if (current != null) removeOccupant(current)
            } else {
                val current = seatOccupants[i]
                if (current == null || current.networkId != occupantId) {
                    val obj = GameObjManager.findSmartGameObj(occupantId)
                    if (current != null) removeOccupant(current)
                    if (obj != null && obj is SoldierGameObj) {
                        if (seatOccupants[i] == null) addOccupant(obj, i)
                    }
                }
            }
        }

        val wasDelivered = vehicleDelivered
        vehicleDelivered = packet.getBool()

        if (!wasDelivered && vehicleDelivered) {
            val base = BaseControllerClass.findBase(getPlayerType())
            base?.onVehicleDelivered(this)
        }
    }

    // C++: virtual void Export_Frequent(BitStreamClass & packet)
    override fun exportFrequent(packet: BitStream) {
        applyControl()   // C++: Make sure we have our driver's controls

        // C++: Vehicles never change their weapon type; write total rounds
        val totalRoundsVal = getWeapon()?.getTotalRounds() ?: totalRoundsOverride
        packet.addInt(totalRoundsVal)

        // Determine vehicle type: from definition if available, else from direct override field
        val vehicleTypeVal = (definition as? ccr.server.defs.VehicleGameObjDef)?.type?.value
            ?: vehicleTypeOverride

        when (vehicleTypeVal) {
            VEHICLE_TYPE_BIKE,
            VEHICLE_TYPE_TANK,
            VEHICLE_TYPE_CAR,
            VEHICLE_TYPE_FLYING -> {
                val vehiclePhys = physObj?.asVehiclePhysClass()
                if (vehiclePhys != null) {
                    val pos = vehiclePhys.getPosition()
                    val q = vehiclePhys.getOrientation()
                    val vel = vehiclePhys.getVelocity()
                    val angVel = vehiclePhys.getAngularVelocity()
                    val engineOn = vehiclePhys.isEngineEnabled()

                    packet.addBool(engineOn)
                    packet.addFloat(pos.x, BITPACK_WORLD_POSITION_X)
                    packet.addFloat(pos.y, BITPACK_WORLD_POSITION_Y)
                    packet.addFloat(pos.z, BITPACK_WORLD_POSITION_Z)
                    packet.addFloat(q.x, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(q.y, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(q.z, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(q.w, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(vel.x, BITPACK_VEHICLE_VELOCITY)
                    packet.addFloat(vel.y, BITPACK_VEHICLE_VELOCITY)
                    packet.addFloat(vel.z, BITPACK_VEHICLE_VELOCITY)
                    packet.addFloat(angVel.x, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                    packet.addFloat(angVel.y, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                    packet.addFloat(angVel.z, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                } else {
                    // No physObj — use direct override fields (secondary constructor path)
                    packet.addBool(isEngineOn)
                    packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
                    packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
                    packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)
                    packet.addFloat(quaternion.x, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(quaternion.y, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(quaternion.z, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(quaternion.w, BITPACK_VEHICLE_QUATERNION)
                    packet.addFloat(velocity.x, BITPACK_VEHICLE_VELOCITY)
                    packet.addFloat(velocity.y, BITPACK_VEHICLE_VELOCITY)
                    packet.addFloat(velocity.z, BITPACK_VEHICLE_VELOCITY)
                    packet.addFloat(angularVelocity.x, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                    packet.addFloat(angularVelocity.y, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                    packet.addFloat(angularVelocity.z, BITPACK_VEHICLE_ANGULAR_VELOCITY)
                }
            }
            VEHICLE_TYPE_TURRET -> {
                // C++: nothing extra for turrets
            }
        }

        packet.addBool(driverIsGunner)

        super.exportFrequent(packet)   // SmartGameObj::Export_Frequent
    }

    // C++: virtual void Import_Frequent(BitStreamClass & packet)
    override fun importFrequent(packet: BitStream) {
        val rounds = packet.getInt()
        getWeapon()?.setTotalRounds(rounds)

        val def = getDefinition()
        when (def.type.value) {
            VEHICLE_TYPE_BIKE,
            VEHICLE_TYPE_CAR,
            VEHICLE_TYPE_TANK,
            VEHICLE_TYPE_FLYING -> {
                val vehiclePhys = physObj?.asVehiclePhysClass()
                if (vehiclePhys != null) {
                    val isEngineOn = packet.getBool()
                    val posX = packet.getFloat(BITPACK_WORLD_POSITION_X)
                    val posY = packet.getFloat(BITPACK_WORLD_POSITION_Y)
                    val posZ = packet.getFloat(BITPACK_WORLD_POSITION_Z)
                    val qX = packet.getFloat(BITPACK_VEHICLE_QUATERNION)
                    val qY = packet.getFloat(BITPACK_VEHICLE_QUATERNION)
                    val qZ = packet.getFloat(BITPACK_VEHICLE_QUATERNION)
                    val qW = packet.getFloat(BITPACK_VEHICLE_QUATERNION)
                    val velX = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
                    val velY = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
                    val velZ = packet.getFloat(BITPACK_VEHICLE_VELOCITY)
                    val angVelX = packet.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY)
                    val angVelY = packet.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY)
                    val angVelZ = packet.getFloat(BITPACK_VEHICLE_ANGULAR_VELOCITY)

                    val pos = Vector3(posX, posY, posZ)
                    val q = Quaternion(qX, qY, qZ, qW).normalized()
                    val vel = Vector3(velX, velY, velZ)
                    val angVel = Vector3(angVelX, angVelY, angVelZ)

                    // C++: if (COMBAT_STAR && COMBAT_STAR->Get_Vehicle() == this) → latency update
                    //       else → interpolate update
                    vehiclePhys.networkInterpolateStateUpdate(pos, q, vel, angVel, 0.1f)
                    vehiclePhys.enableEngine(isEngineOn)
                }
            }
            VEHICLE_TYPE_TURRET -> {
                // C++: nothing extra for turrets
            }
        }

        driverIsGunner = packet.getBool()

        super.importFrequent(packet)   // SmartGameObj::Import_Frequent
    }

    // C++: virtual void Import_State_Cs(BitStreamClass & packet)
    override fun importStateCs(packet: BitStream) {
        super.importStateCs(packet)
    }

    // C++: virtual void Export_State_Cs(BitStreamClass & packet)
    override fun exportStateCs(packet: BitStream) {
        super.exportStateCs(packet)
    }

    // -------------------------------------------------------------------------
    // Physics — Peek_Vehicle_Phys
    // -------------------------------------------------------------------------

    // C++: VehiclePhysClass* Peek_Vehicle_Phys() const
    fun peekVehiclePhys(): Any? = physObj?.asVehiclePhysClass()   // VehiclePhysClass

    // -------------------------------------------------------------------------
    // Velocity
    // -------------------------------------------------------------------------

    // C++: void Get_Velocity(Vector3 & vel)
    override fun getVelocity(): Vector3 {
        val vp = physObj?.asVehiclePhysClass()
        return if (vp != null) vp.getVelocity() else Vector3(0f, 0f, 0f)
    }

    // C++: void Set_Velocity(Vector3 & vel)
    fun setVelocity(vel: Vector3) {
        physObj?.asVehiclePhysClass()?.setVelocity(vel)
    }

    // -------------------------------------------------------------------------
    // Damage
    // -------------------------------------------------------------------------

    // C++: virtual void Apply_Damage(const OffenseObjectClass&, float, int)
    override fun applyDamage(damager: OffenseObjectClass, scale: Float, alternateSkin: Int) {
        val startingHealth = defenseObject.health

        super.applyDamage(damager, scale, alternateSkin)

        // C++: Stats — if vehicle was just destroyed
        if (startingHealth > 0f && defenseObject.health <= 0f) {
            val owner = damager.getOwner()
            val ownerSoldier = owner as? SoldierGameObj
            ownerSoldier?.playerData?.statsAddVehicleDestroyed()
        }

        updateDamageMeshes()
    }

    // C++: void Update_Damage_Meshes()
    fun updateDamageMeshes() {
        val healthMax = defenseObject.healthMax
        val model = peekModel() ?: return
        if (healthMax <= 0f) return

        val healthFrac = defenseObject.health / healthMax
        val showDamage25 = healthFrac < 0.25f
        val showDamage50 = healthFrac < 0.50f
        val showDamage75 = healthFrac < 0.75f

        val boneCount = model.getNumBones()
        for (index in 0 until boneCount) {
            val boneName = model.getBoneName(index)
            when {
                boneName.startsWith("DAMAGE25", ignoreCase = true) ->
                    setSubobjectVisibility(model, index, showDamage25)
                boneName.startsWith("DAMAGE50", ignoreCase = true) ->
                    setSubobjectVisibility(model, index, showDamage50)
                boneName.startsWith("DAMAGE75", ignoreCase = true) ->
                    setSubobjectVisibility(model, index, showDamage75)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Object_Expired — vehicles explode when flipped
    // -------------------------------------------------------------------------

    // C++: virtual ExpirationReactionType Object_Expired(PhysClass* observed_obj)
    override fun objectExpired(observedObj: PhysClass): ExpirationReactionType {
        // C++: Only on the server
        val def = getDefinition()
        if (def.physical.killedExplosion != 0 && !isDeletePending) {
            val pos = getPosition()
            ExplosionManager.serverExplode(def.physical.killedExplosion, pos, null)
        }

        if (!isDeletePending) {
            for (observer in observers.toList()) {
                observer.killed(this, null)
            }
        }

        return super.objectExpired(observedObj)
    }

    // -------------------------------------------------------------------------
    // Engine control
    // -------------------------------------------------------------------------

    // C++: bool Is_Engine_Enabled() const
    fun isEngineEnabled(): Boolean = physObj?.asVehiclePhysClass()?.isEngineEnabled() ?: false

    // C++: void Enable_Engine(bool onoff)
    fun enableEngine(onoff: Boolean) {
        physObj?.asVehiclePhysClass()?.enableEngine(onoff)
    }

    // -------------------------------------------------------------------------
    // Lock feature
    // -------------------------------------------------------------------------

    // C++: void Lock_Vehicle(ScriptableGameObj* lockowner, float locktime)
    fun lockVehicle(lockOwnerObj: ScriptableGameObj?, lockTime: Float) {
        lockOwner.set(lockOwnerObj)
        lockTimer = lockTime
    }

    // C++: bool Is_Locked() { return LockOwner.Get_Ptr() != NULL && LockTimer > 0 }
    fun isLocked(): Boolean = lockOwner.get() != null && lockTimer > 0f

    // C++: ScriptableGameObj* Get_Lock_Owner() { return LockOwner.Get_Ptr(); }
    fun getLockOwner(): ScriptableGameObj? = lockOwner.get() as? ScriptableGameObj

    // -------------------------------------------------------------------------
    // DriverIsGunner
    // -------------------------------------------------------------------------

    // C++: bool Get_Driver_Is_Gunner() { return DriverIsGunner; } — use property driverIsGunner directly

    // C++: void Toggle_Driver_Is_Gunner() { DriverIsGunner = !DriverIsGunner; }
    fun toggleDriverIsGunner() { driverIsGunner = !driverIsGunner }

    // -------------------------------------------------------------------------
    // Stealth
    // -------------------------------------------------------------------------

    // C++: virtual float Get_Stealth_Fade_Distance() const
    override fun getStealthFadeDistance(): Float {
        // C++: IS_MISSION ? singleplayer : multiplayer stealth distance
        return GlobalSettingsDef.getGlobalSettings()?.getMpStealthDistanceVehicle() ?: super.getStealthFadeDistance()
    }

    // -------------------------------------------------------------------------
    // Filter distance
    // -------------------------------------------------------------------------

    // C++: virtual float Get_Filter_Distance() const
    override fun getFilterDistance(): Float {
        val def = getDefinition()
        if (def.type.value == VEHICLE_TYPE_TURRET) {
            val weapon = getWeapon()
            var range = def.smart.sightRange
            if (weapon != null) {
                range = minOf(weapon.getRange(), range)
            }
            return range
        }
        return super.getFilterDistance()
    }

    // -------------------------------------------------------------------------
    // Ignore / Unignore occupants for raycasts
    // -------------------------------------------------------------------------

    // C++: void Ignore_Occupants()
    fun ignoreOccupants() {
        for (occupant in seatOccupants) {
            occupant?.peekPhysicalObject()?.incIgnoreCounter()
        }
    }

    // C++: void Unignore_Occupants()
    fun unignoreOccupants() {
        for (occupant in seatOccupants) {
            occupant?.peekPhysicalObject()?.decIgnoreCounter()
        }
    }

    // -------------------------------------------------------------------------
    // Profile
    // -------------------------------------------------------------------------

    // C++: const char* Get_Profile() { return Get_Definition().Profile; }
    fun getProfile(): String = getDefinition().profile

    // -------------------------------------------------------------------------
    // Turn radius
    // -------------------------------------------------------------------------

    // C++: float Get_Turn_Radius() const { return Get_Definition().TurnRadius; }
    fun getTurnRadius(): Float = getDefinition().turnRadius

    // -------------------------------------------------------------------------
    // Vehicle name
    // -------------------------------------------------------------------------

    // C++: const WCHAR* Get_Vehicle_Name()
    fun getVehicleName(): String? {
        val nameId = getDefinition().vehicleNameId
        if (nameId != 0) {
            return TranslateDBClass.getString(nameId)
        }
        return TranslateDBClass.getString(IDS_HUD_VEHICLE_NAME)
    }

    // -------------------------------------------------------------------------
    // Description
    // -------------------------------------------------------------------------

    // C++: virtual void Get_Description(StringClass & description)
    override fun getDescription(): String {
        val sb = StringBuilder()
        sb.append("ID:    ${networkId}\n")
        sb.append("NAME:  ${definition?.name ?: ""}\n")
        sb.append("TEAM:  ${getPlayerType()}\n")
        sb.append("CONTR: $controlOwner\n")
        val pos = getPosition()
        sb.append("POS:   ${pos.x}, ${pos.y}, ${pos.z}\n")
        val tgt = getTargetingPos()
        sb.append("TGT:   ${tgt.x}, ${tgt.y}, ${tgt.z}\n")
        val vel = getVelocity()
        sb.append("VEL:   ${vel.x}, ${vel.y}, ${vel.z}\n")
        sb.append("SPD:   ${vel.length()}\n")
        val weapon = getWeapon()
        if (weapon != null) {
            sb.append("WEAP:  ${weapon.getName()}\n")
            sb.append("RNDS:  ${weapon.getTotalRounds()}\n")
        }
        sb.append("HLTH:  ${defenseObject.health}\n")
        val driver = getDriver()
        if (driver != null) sb.append("DRVR:  ${driver.networkId}\n")
        val gunner = getGunner()
        if (gunner != null) sb.append("GUNR:  ${gunner.networkId}\n")
        sb.append("HIB:   ${isHibernating()}\n")
        sb.append("stlth: ${stealthEnabled}\n")
        sb.append("   on: ${isStealthed()}\n")
        return sb.toString()
    }

    // -------------------------------------------------------------------------
    // Transition helpers (mostly physics/rendering — wired as stubs)
    // -------------------------------------------------------------------------

    // C++: void Destroy_Transitions()
    private fun destroyTransitions() {
        hasEnterTransitions = false
        hasExitTransitions = false
        // FIXME: TransitionManager::Destroy + TransitionInstances.Delete — not yet ported
        // FIXME: TransitionManager::Destroy_Pending() — not yet ported
    }

    // C++: void Create_And_Destroy_Transitions()
    private fun createAndDestroyTransitions() {
        val def = getDefinition()
        // C++: manage enter transitions
        val shouldHaveEnter = (occupiedSeats < def.numSeats) && transitionsEnabled
        if (shouldHaveEnter != hasEnterTransitions) {
            if (shouldHaveEnter) {
                createNewTransitions(TRANSITION_TYPE_VEHICLE_ENTER)
                hasEnterTransitions = true
            } else {
                removeTransitions(TRANSITION_TYPE_VEHICLE_ENTER)
                hasEnterTransitions = false
            }
        }

        // C++: manage exit transitions
        val shouldHaveExit = (occupiedSeats > 0) && transitionsEnabled
        if (shouldHaveExit != hasExitTransitions) {
            if (shouldHaveExit) {
                createNewTransitions(TRANSITION_TYPE_VEHICLE_EXIT)
                hasExitTransitions = true
            } else {
                removeTransitions(TRANSITION_TYPE_VEHICLE_EXIT)
                hasExitTransitions = false
            }
        }
    }

    // C++: void Remove_Transitions(TransitionDataClass::StyleType transition_type)
    private fun removeTransitions(transitionType: Int) {
        // FIXME: TransitionManager — not yet ported
    }

    // C++: void Create_New_Transitions(TransitionDataClass::StyleType transition_type)
    private fun createNewTransitions(transitionType: Int) {
        // FIXME: TransitionManager — not yet ported
    }

    // C++: void Update_Transitions()
    private fun updateTransitions() {
        // FIXME: update TransitionInstances parent transform — not yet ported
    }

    // C++: void Script_Enable_Transitions(bool enable)
    fun scriptEnableTransitions(enable: Boolean) {
        transitionsEnabled = enable
        createAndDestroyTransitions()
    }

    // -------------------------------------------------------------------------
    // Turret helpers
    // -------------------------------------------------------------------------

    // C++: void Aquire_Turret_Bones()
    private fun acquireTurretBones() {
        val model = peekModel() ?: return

        if (turretBone == 0) {
            turretBone = model.getBoneIndex("turret")
            if (turretBone != 0) {
                model.captureBone(turretBone)
                if (!model.isBoneCaptured(turretBone)) {
                    turretBone = 0
                }
            }
        }

        barrelOffset = 0f

        if (barrelBone == 0) {
            barrelBone = model.getBoneIndex("barrel")
            if (barrelBone != 0) {
                model.captureBone(barrelBone)
                if (!model.isBoneCaptured(barrelBone)) {
                    barrelBone = 0
                }
            }

            if (turretBone != 0 && barrelBone != 0) {
                val turretBase = model.getBoneTransform(turretBone)
                val barrelPos = model.getBoneTransform(barrelBone).getTranslation()
                val turretSpaceBarrel = Vector3()
                turretBase.inverseTransformVector(barrelPos, turretSpaceBarrel)
                barrelOffset = turretSpaceBarrel.y
                if (Math.abs(barrelOffset) < 0.1f) barrelOffset = 0f
            }
        }
    }

    // C++: void Release_Turret_Bones()
    private fun releaseTurretBones() {
        val model = peekModel() ?: return

        if (turretBone != 0 && model.isBoneCaptured(turretBone)) {
            model.releaseBone(turretBone)
            turretBone = 0
        }

        if (barrelBone != 0 && model.isBoneCaptured(barrelBone)) {
            model.releaseBone(barrelBone)
            barrelBone = 0
        }

        barrelOffset = 0f
    }

    // C++: void Update_Turret(float weapon_turn, float weapon_tilt)
    private fun updateTurret(weaponTurn: Float, weaponTilt: Float) {
        if (turretBone != 0) {
            val model = peekModel()!!
            val facing = Matrix3D.identity()
            facing.rotateZ(weaponTurn)
            if (barrelBone == 0) {
                facing.rotateY(-weaponTilt)
            }
            model.controlBone(turretBone, facing)
        }

        if (barrelBone != 0) {
            val model = peekModel()!!
            val facing = Matrix3D.identity()
            facing.rotateY(-weaponTilt)
            model.controlBone(barrelBone, facing)
        }
    }

    // -------------------------------------------------------------------------
    // Sound effects (engine)
    // -------------------------------------------------------------------------

    // C++: void Update_Sound_Effects()
    private fun updateSoundEffects() {
        if (physObj?.asVehiclePhysClass() == null) return

        // FIXME: engine sound state machine — AudibleSoundClass not yet ported
        // The state machine transitions between ENGINE_SOUND_STATE_* based on
        // whether the engine is enabled and whether the cached sound has stopped.
    }

    // C++: void Change_Engine_Sound_State(int new_state)
    private fun changeEngineSoundState(newState: Int) {
        // FIXME: AudibleSoundClass not yet ported
        cachedEngineSound = null
        engineSoundState = newState
    }

    // C++: void Update_Engine_Sound_Pitch()
    private fun updateEngineSoundPitch() {
        // FIXME: AudibleSoundClass not yet ported
    }

    // -------------------------------------------------------------------------
    // Wheel effects
    // -------------------------------------------------------------------------

    // C++: void Init_Wheel_Effects()
    private fun initWheelEffects() {
        // FIXME: SurfaceEffectsManager::Create_Persistant_Emitter / Create_Persistant_Sound — not yet ported
    }

    // C++: void Shutdown_Wheel_Effects()
    private fun shutdownWheelEffects() {
        // FIXME: SurfaceEffectsManager::Destroy_Persistant_Emitter / Destroy_Persistant_Sound — not yet ported
    }

    // C++: void Update_Wheel_Effects()
    private fun updateWheelEffects() {
        // FIXME: SurfaceEffectsManager wheel emitter updates — not yet ported
    }

    // -------------------------------------------------------------------------
    // Damage mesh helper
    // -------------------------------------------------------------------------

    // C++: static void Set_Subobject_Visibility(RenderObjClass*, int bone_index, bool show)
    private fun setSubobjectVisibility(model: RenderObjClass, boneIndex: Int, show: Boolean) {
        val count = model.getNumSubObjectsOnBone(boneIndex)
        for (i in 0 until count) {
            val subObj = model.getSubObjectOnBone(i, boneIndex) ?: continue
            subObj.setHidden(!show)
        }
    }

    // -------------------------------------------------------------------------
    // Precision (static, called once at startup)
    // -------------------------------------------------------------------------

    // C++: static void Set_Precision()
    // Called at startup to register bitpack encoder precision values.
    // Note: in the Kotlin server, precision is registered at the BitStream level.

    // -------------------------------------------------------------------------
    // Companion object — constants and static members
    // -------------------------------------------------------------------------

    companion object {
        // C++: VehicleType enum
        const val VEHICLE_TYPE_CAR    = 0
        const val VEHICLE_TYPE_TANK   = 1
        const val VEHICLE_TYPE_BIKE   = 2
        const val VEHICLE_TYPE_FLYING = 3
        const val VEHICLE_TYPE_TURRET = 4

        // C++: Engine sound states
        const val ENGINE_SOUND_STATE_STARTING = 0
        const val ENGINE_SOUND_STATE_RUNNING  = 1
        const val ENGINE_SOUND_STATE_STOPPING = 2
        const val ENGINE_SOUND_STATE_OFF      = 3

        // C++: Seat indices
        private const val DRIVER_SEAT = 0
        private const val GUNNER_SEAT = 1

        // C++: static bool DefaultDriverIsGunner = false
        var defaultDriverIsGunner: Boolean = false

        // C++: static bool CameraLockedToTurret = false
        var cameraLockedToTurret: Boolean = false

        // C++: bool _Use_Target_Steering = false
        var useTargetSteering: Boolean = false

        // C++: static bool Toggle_Target_Steering()
        fun toggleTargetSteering(): Boolean {
            useTargetSteering = !useTargetSteering
            return useTargetSteering
        }

        // C++: static void Set_Target_Steering(bool onoff)
        fun setTargetSteering(onoff: Boolean) { useTargetSteering = onoff }

        // C++: static bool Is_Target_Steering()
        fun isTargetSteering(): Boolean = useTargetSteering

        // C++: static void Set_Default_Driver_Is_Gunner(bool flag) — use property defaultDriverIsGunner directly
        // C++: static void Set_Camera_Locked_To_Turret(bool flag) — use property cameraLockedToTurret directly
        // C++: static bool Get_Camera_Locked_To_Turret() — use property cameraLockedToTurret directly

        // Sentinel value for empty seat in Export_Rare
        private const val NO_OCCUPANT = -1

        // Transition type constants (C++: TransitionDataClass::StyleType)
        // FIXME: wire exact enum values from transition.h when TransitionManager is ported
        private const val TRANSITION_TYPE_VEHICLE_ENTER = 6   // C++: VEHICLE_ENTER
        private const val TRANSITION_TYPE_VEHICLE_EXIT  = 7   // C++: VEHICLE_EXIT

        // Custom event constants (C++: combattype.h)
        private const val CUSTOM_EVENT_VEHICLE_ENTERED = 10004
        private const val CUSTOM_EVENT_VEHICLE_EXITED  = 10005

        // App packet types (C++: apppackettypes.h)
        private const val APPPACKETTYPE_VEHICLE = 7
        private const val APPPACKETTYPE_TURRET  = 9

        // Collision group for destruct (C++: UNCOLLIDEABLE_GROUP)
        private const val UNCOLLIDEABLE_GROUP = 0

        // Weapon animation states (C++: WeaponClass)
        private const val WEAPON_ANIM_NOT_FIRING = 0
        private const val WEAPON_ANIM_FIRING_0   = 1
        private const val WEAPON_ANIM_FIRING_1   = 2

        // Animation mode
        private const val ANIM_MODE_ONCE = 2

        // IDS_HUD_VEHICLE_NAME placeholder
        private const val IDS_HUD_VEHICLE_NAME = 0

        // ----------------------------------------------------------------
        // VehicleGameObj Save/Load chunk IDs
        // C++ enum starting at 923991632 (vehicle.cpp)
        // ----------------------------------------------------------------
        private const val CHUNKID_PARENT                    = 923991632
        private const val CHUNKID_VARIABLES                 = 923991633
        private const val XXXCHUNKID_ANIM_CONTROL_OLD       = 923991634
        private const val XXXCHUNKID_TRANSITION_LIST_OLD    = 923991635
        private const val CHUNKID_SEAT_LIST                 = 923991636

        // Micro chunk IDs inside CHUNKID_VARIABLES
        private const val MICROCHUNKID_TURRET_BONE              = 1
        private const val MICROCHUNKID_BARREL_BONE              = 2
        private const val XXXMICROCHUNKID_PHYSICAL_OBJECT       = 3  // legacy pointer — skip
        private const val MICROCHUNKID_TRANSITION_LIST_ENTRY_OLD = 4  // legacy — skip
        private const val MICROCHUNKID_TURRET_TURN              = 5
        private const val MICROCHUNKID_BARREL_TILT              = 6
        private const val MICROCHUNKID_TRANSITIONS_ENABLED      = 7
        private const val MICROCHUNKID_OCCUPIED_SEATS           = 8
        private const val MICROCHUNKID_NUM_SEATS                = 9
    }
}
