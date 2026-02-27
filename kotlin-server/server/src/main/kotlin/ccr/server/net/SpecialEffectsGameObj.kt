package ccr.server.net

import ccr.server.defs.SpecialEffectsGameObjDef

// C++: SpecialEffectsGameObj : public PhysicalGameObj (specialeffectsgameobj.cpp)
// Hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj
//            → PhysicalGameObj → SpecialEffectsGameObj
open class SpecialEffectsGameObj() : PhysicalGameObj() {

    // C++: float LifeRemaining (initialized to -1.0F)
    var lifeRemaining: Float = -1.0f

    // C++: bool IsInitialized (initialized to false)
    var isInitialized: Boolean = false

    init {
        // C++: SpecialEffectsGameObj() — no additional init beyond field defaults
    }

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: ccr.math.Vector3 = ccr.math.Vector3(),
        modelName: String = "",
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "sfx_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.modelName = modelName
    }

    // C++: ~SpecialEffectsGameObj() — no extra cleanup
    override fun destruct() {
        super.destruct()
    }

    // C++: virtual void Init()
    override fun init() {
        init(getSpecialEffectsDefinition())
    }

    // C++: void Init(const SpecialEffectsGameObjDef & definition)
    fun init(definition: SpecialEffectsGameObjDef) {
        super.init(definition)

        // C++: Make sure collisions are turned off on this effect
        val physObj = peekPhysicalObject()
        if (physObj != null) {
            physObj.setCollisionGroup(UNCOLLIDEABLE_GROUP)
        }
    }

    // C++: const SpecialEffectsGameObjDef & Get_Definition() const
    fun getSpecialEffectsDefinition(): SpecialEffectsGameObjDef =
        definition as SpecialEffectsGameObjDef

    // C++: void Do_Effect()
    protected fun doEffect() {
        // C++: Make sure we have a render object
        val model = peekModel() ?: return

        // C++: Should we play an animation?
        val animName = getSpecialEffectsDefinition().animationName
        if (animName.isNotEmpty()) {
            // C++: HAnimClass* animation = WW3DAssetManager::Get_Instance()->Get_HAnim(anim_name)
            //      if (animation != NULL) { LifeRemaining = animation->Get_Total_Time(); ... }
            val animation = WW3DAssetManager.getInstance().getHAnim(animName)
            if (animation != null) {
                lifeRemaining = animation.getTotalTime()
                (model as? Animatable3DObjClass)?.setAnimation(animation, 0f, RenderObjClass.ANIM_MODE_ONCE)
                // REF_PTR_RELEASE(animation) — GC handles in Kotlin
            }
        }

        // C++: Should we play a sound?
        if (getSpecialEffectsDefinition().soundDefId > 0) {
            WWAudioClass.getInstance().createInstantSound(
                getSpecialEffectsDefinition().soundDefId,
                getTransform(),
            )
        }
    }

    // C++: void Think()
    override fun think() {
        // C++: Kick off the special effect
        if (!isInitialized) {
            doEffect()
            isInitialized = true
        }

        // C++: Check to see if we should make ourselves go away
        if (lifeRemaining >= 0f) {
            lifeRemaining -= TimeManager.getFrameSeconds()
            if (lifeRemaining <= 0f) {
                setDeletePending()
            }
        }
    }

    // C++: bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        saveVariables(csave)
        csave.endChunk()

        return true
    }

    // C++: void Save_Variables(ChunkSaveClass & csave)
    protected fun saveVariables(csave: ChunkSaveClass) {
        csave.writeMicroChunk(VARID_LIFE_REMAINING, lifeRemaining)
        csave.writeMicroChunk(VARID_IS_INITIALIZED, isInitialized)
    }

    // C++: bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_VARIABLES -> loadVariables(cload)
                else -> error("Unrecognized SpecialEffectsGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass & cload)
    protected fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                VARID_LIFE_REMAINING -> lifeRemaining = cload.readFloat()
                VARID_IS_INITIALIZED -> isInitialized = cload.readBool()
                else -> error("Unrecognized SpecialEffectsGameObj variable chunk ID: ${cload.curMicroChunkId}")
            }
            cload.closeMicroChunk()
        }
    }

    companion object {
        // C++: CHUNKID_GAME_OBJECT_SPECIAL_EFFECTS = 0x0004012F (combatchunkid.h)
        const val NETWORK_CLASS_ID: Int = 0x0004012F  // CHUNKID_GAME_OBJECT_SPECIAL_EFFECTS

        // C++: specialeffectsgameobj.cpp local enum starting at CHUNKID_PARENT = 0x09010236
        private const val CHUNKID_PARENT    = 0x09010236
        private const val CHUNKID_VARIABLES = 0x09010237

        // C++: micro-chunk IDs for Save_Variables / Load_Variables
        private const val VARID_LIFE_REMAINING = 1
        private const val VARID_IS_INITIALIZED = 2

        // C++: UNCOLLIDEABLE_GROUP = 1 (combat.h typedef enum, combat.h:42) — collides with nothing
        private const val UNCOLLIDEABLE_GROUP = 1
    }
}
