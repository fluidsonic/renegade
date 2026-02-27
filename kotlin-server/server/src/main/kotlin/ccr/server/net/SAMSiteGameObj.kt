package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.GameObjManager
import ccr.server.defs.SAMSiteGameObjDef

// C++: SAMSiteGameObj : public SmartGameObj (samsite.h / samsite.cpp)
// Full hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//                             → PhysicalGameObj → ArmedGameObj → SmartGameObj → SAMSiteGameObj
class SAMSiteGameObj() : SmartGameObj() {

    // C++: int TurretBone (initialized to -1)
    protected var turretBone: Int = -1

    // C++: int BarrelBone (initialized to -1)
    protected var barrelBone: Int = -1

    // C++: int State (initialized to SAMSITE_STATE_DOWN)
    protected var state: Int = SAMSITE_STATE_DOWN

    // C++: float Timer (uninitialized in C++ ctor; effectively 0 until set by state machine)
    protected var timer: Float = 0f

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: ccr.math.Vector3 = ccr.math.Vector3(),
        modelName: String = "",
        team: Int = PLAYERTYPE_NEUTRAL,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "samsite_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.modelName = modelName
        playerType = team
    }

    // C++: SAMSiteGameObj::Init(void) — delegates to Init(definition)
    override fun init() {
        init(getDefinition() as SAMSiteGameObjDef)
    }

    // C++: SAMSiteGameObj::Init(const SAMSiteGameObjDef&)
    fun init(definition: SAMSiteGameObjDef) {
        super.init(definition)
    }

    // C++: const SAMSiteGameObjDef& SAMSiteGameObj::Get_Definition() const
    fun getSamSiteDefinition(): SAMSiteGameObjDef = getDefinition() as SAMSiteGameObjDef

    // C++: SAMSiteGameObj::~SAMSiteGameObj — releases captured bones
    override fun destruct() {
        val model = peekModel()
        if (model != null) {
            if (turretBone != -1) {
                if (model.isBoneCaptured(turretBone)) {
                    model.releaseBone(turretBone)
                    turretBone = -1
                }
            }
            if (barrelBone != -1) {
                if (model.isBoneCaptured(barrelBone)) {
                    model.releaseBone(barrelBone)
                    barrelBone = -1
                }
            }
        }
        super.destruct()
    }

    // C++: void SAMSiteGameObj::Update_Turret(float weapon_turn, float weapon_tilt)
    protected fun updateTurret(weaponTurn: Float, weaponTilt: Float) {
        val model = peekModel()!!

        if (turretBone == -1) {
            turretBone = model.getBoneIndex("turret")
            if (turretBone != -1) {
                model.captureBone(turretBone)
                if (!model.isBoneCaptured(turretBone)) {
                    turretBone = -1
                }
            }
        }

        if (barrelBone == -1) {
            barrelBone = model.getBoneIndex("barrel")
            if (barrelBone != -1) {
                model.captureBone(barrelBone)
                if (!model.isBoneCaptured(barrelBone)) {
                    barrelBone = -1
                }
            }
        }

        if (turretBone != -1) {
            val facing = Matrix3D()
            facing.rotateZ(weaponTurn)
            if (barrelBone == -1) {  // if no barrel bone, tilt on turret
                facing.rotateY(-weaponTilt)  // neg rotate y tilts up
            }
            model.controlBone(turretBone, facing)
        }

        if (barrelBone != -1) {
            val facing = Matrix3D()
            facing.rotateY(-weaponTilt)  // neg rotate y tilts up
            model.controlBone(barrelBone, facing)
        }
    }

    // C++: bool SAMSiteGameObj::Set_Targeting(const Vector3& target_pos, bool do_tilt)
    override fun setTargeting(pos: Vector3, doTilt: Boolean): Boolean {
        // Should add slow turning
        super.setTargeting(pos, doTilt)

        val model = peekModel()!!
        val muzzlePos = model.getBoneTransform("muzzle").getTranslation()
        val tm = Matrix3D.objLookAt(muzzlePos, pos, 0f)

        val weaponTilt = -tm.getYRotation()
        tm.rotateY(weaponTilt)
        val weaponTurn = tm.getZRotation()

        updateTurret(weaponTurn, if (doTilt) weaponTilt else 0f)

        return true
    }

    // C++: void SAMSiteGameObj::Generate_Control()
    override fun generateControl() {
        if (state == SAMSITE_STATE_UP) {
            super.generateControl()
        }
    }

    // C++: void SAMSiteGameObj::Think()
    override fun think() {
        var target: SmartGameObj? = null

        // Find nearest enemy aircraft
        val myPos = getPosition()
        for (obj in GameObjManager.getSmartGameObjs()) {
            val vehicle = obj as? VehicleGameObj ?: continue
            if (!vehicle.isAircraft()) continue
            target = vehicle
        }

        if (target != null) {
            val parameters = ActionParamsStruct()
            parameters.priority = 100
            parameters.attackObject = target
            parameters.attackRange = 40f
            parameters.moveLocation = myPos
            parameters.moveArrivedDistance = 10000f
            action.attack(parameters)
        } else {
            action.reset(100)
        }

        super.think()  // C++: SmartGameObj::Think()

        if (state == SAMSITE_STATE_DOWN) {
            if (target != null) {
                setAnimation("B_samsiteL1.M_samrise")
                getAnimControl()!!.setMode(ANIM_MODE_ONCE, 0)
                state = SAMSITE_STATE_RISING
            }
        } else if (state == SAMSITE_STATE_UP) {
            timer -= TimeManager.getFrameSeconds()
            if (timer <= 0f) {
                setAnimation("B_samsitel1.M_samdrop")
                getAnimControl()!!.setMode(ANIM_MODE_ONCE, 0)
                state = SAMSITE_STATE_DROPPING
            }
        } else {
            // If rising or dropping, check for completion, then set timer and state
            if (getAnimControl()!!.isComplete()) {
                state = if (state == SAMSITE_STATE_RISING) SAMSITE_STATE_UP else SAMSITE_STATE_DOWN
                timer = 5f
            }
        }
    }

    // C++: virtual bool SAMSiteGameObj::Save(ChunkSaveClass&)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_STATE, state)
        csave.writeMicroChunk(MICROCHUNKID_TIMER, timer)
        csave.endChunk()

        return true
    }

    // C++: virtual bool SAMSiteGameObj::Load(ChunkLoadClass&)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)
                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_STATE -> state = cload.readInt()
                            MICROCHUNKID_TIMER -> timer = cload.readFloat()
                            else -> error("Unrecognized SAMSiteGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                // Legacy chunks — present in old saves, no data to read
                XXXCHUNKID_SAM_TIMER    -> cload.skipChunk()
                XXXCHUNKID_ANIM_CONTROL -> cload.skipChunk()
                else -> error("Unrecognized SAMSiteGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: virtual void SAMSiteGameObj::Import_Frequent(BitStreamClass&)
    override fun importFrequent(packet: BitStream) {
        super.importFrequent(packet)
    }

    // C++: virtual void SAMSiteGameObj::Export_Frequent(BitStreamClass&)
    override fun exportFrequent(packet: BitStream) {
        super.exportFrequent(packet)
    }

    companion object {
        // C++: enum (samsite.cpp) — chunk IDs for SAMSiteGameObj save/load
        // Sequential starting from CHUNKID_PARENT = 922991750
        private const val CHUNKID_PARENT              = 922991750
        private const val CHUNKID_VARIABLES           = 922991751
        private const val XXXCHUNKID_SAM_TIMER        = 922991752  // legacy — not written in save
        private const val XXXCHUNKID_ANIM_CONTROL     = 922991753  // legacy — not written in save

        // C++: micro chunk IDs
        @Suppress("unused")
        private const val XXXMICROCHUNKID_PHYSOBJ     = 1  // legacy pointer remap — skipped in load
        private const val MICROCHUNKID_STATE          = 2
        private const val MICROCHUNKID_TIMER          = 3

        // C++: typedef enum { SAMSITE_STATE_DOWN=0, SAMSITE_STATE_UP=1, SAMSITE_STATE_RISING=2, SAMSITE_STATE_DROPPING=3 }
        const val SAMSITE_STATE_DOWN     = 0
        const val SAMSITE_STATE_UP       = 1
        const val SAMSITE_STATE_RISING   = 2
        const val SAMSITE_STATE_DROPPING = 3
    }
}
