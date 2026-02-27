package ccr.server.net

import ccr.server.defs.SimpleGameObjDef

// C++: SimpleGameObj : public PhysicalGameObj (simplegameobj.h/cpp)
open class SimpleGameObj() : PhysicalGameObj() {

    init {
        // C++: SimpleGameObj() { Set_App_Packet_Type(APPPACKETTYPE_SIMPLE); }
        // APPPACKETTYPE_SIMPLE = 1 (second entry in apppackettypes.h enum, after APPPACKETTYPE_UNKNOWN)
        appPacketType = APPPACKETTYPE_SIMPLE
    }

    // Secondary constructor for tests — bypasses Init() / definition pipeline.
    constructor(
        definitionId: Int,
        position: ccr.math.Vector3 = ccr.math.Vector3(),
        modelName: String = "",
        animName: String = "",
        health: Float = 0f,
    ) : this() {
        definition = ccr.server.defs.BaseGameObjDef(
            name = "simple_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.modelName = modelName
        this.animName = animName
        defenseObject.health = health
    }

    // C++: ~SimpleGameObj() — empty destructor
    override fun destruct() {
        super.destruct()
    }

    // C++: virtual void Init()
    override fun init() {
        init(getSimpleDefinition())
    }

    // C++: void Init(const SimpleGameObjDef & definition)
    fun init(definition: SimpleGameObjDef) {
        super.init(definition)
    }

    // C++: const SimpleGameObjDef & Get_Definition() const
    fun getSimpleDefinition(): SimpleGameObjDef = definition as SimpleGameObjDef

    // C++: virtual SimpleGameObj * As_SimpleGameObj() { return this; }
    open fun asSimpleGameObj(): SimpleGameObj = this

    // C++: bool Is_Hidden_Object() { return Get_Definition().IsHiddenObject; }
    fun isHiddenObject(): Boolean = getSimpleDefinition().isHiddenObject

    // C++: virtual bool Is_Always_Dirty() { return false; }
    override open fun isAlwaysDirty(): Boolean = false

    // C++: virtual bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)
                else -> error("Unrecognized SimpleGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this) — pointer remap not ported
        return true
    }

    // C++: virtual void On_Post_Load()
    override fun onPostLoad() {
        super.onPostLoad()

        // NOTE: the On_Post_Load function is only run when loading a level in the game engine
        // so we can put game-specific behavior into this function without messing up the level editor.
        if (getSimpleDefinition().isEditorObject) {
            // C++: Peek_Physical_Object()->Set_Model_By_Name("NULL")
            physObj!!.setModelByName("NULL")
            // C++: if (Get_Anim_Control() != NULL) Get_Anim_Control()->Set_Model(Peek_Model())
            animControl?.setModel(physObj!!.peekModel()!!)
        }

        if (isHiddenObject()) {
            // C++: RenderObjClass * model = Peek_Physical_Object()->Peek_Model(); if (model) model->Set_Hidden(true)
            physObj?.peekModel()?.setHidden(true)
        }
    }

    companion object {
        // C++: apppackettypes.h — APPPACKETTYPE_SIMPLE = 1 (second entry after APPPACKETTYPE_UNKNOWN = 0)
        const val APPPACKETTYPE_SIMPLE: Byte = 1

        // C++: simplegameobj.cpp local enum — CHUNKID_PARENT = 927991712
        private const val CHUNKID_PARENT       = 927991712
        // C++: CHUNKID_VARIABLES              = 927991713 (not written in save, but present in load for legacy)
        private const val CHUNKID_VARIABLES    = 927991713
        // C++: XXXCHUNKID_ANIM_CONTROL        = 927991714 (legacy, XXX prefix — not used)
        private const val XXXCHUNKID_ANIM_CONTROL = 927991714
        // C++: XXXMICROCHUNKID_PHYSOBJ        = 1 (legacy micro chunk)
        private const val XXXMICROCHUNKID_PHYSOBJ = 1
    }
}
