package ccr.server.net

import ccr.server.defs.ComCenterGameObjDef

// C++: ComCenterGameObj : public BuildingGameObj
// C++ hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj → BuildingGameObj → ComCenterGameObj
class ComCenterGameObj() : BuildingGameObj() {

    // C++: ComCenterGameObj() — empty constructor body; no additional fields

    init {
        // C++: no constructor body beyond default initialization
    }

    // Secondary constructor for tests — chains to BuildingGameObj secondary constructor.
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
            name = "comcenter_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: ~ComCenterGameObj() — empty destructor body
    override fun destruct() {
        super.destruct()
    }

    // C++: virtual void Init(void)
    override fun init() {
        init(getComCenterDefinition())
    }

    // C++: void Init(const ComCenterGameObjDef& definition)
    fun init(definition: ComCenterGameObjDef) {
        super.init(definition)
    }

    // C++: const ComCenterGameObjDef& Get_Definition() const
    fun getComCenterDefinition(): ComCenterGameObjDef = definition as ComCenterGameObjDef

    // C++: ComCenterGameObj* As_ComCenterGameObj() { return this; }
    // → handled by Kotlin type system: `this is ComCenterGameObj`

    // C++: virtual bool Save(ChunkSaveClass& csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.endChunk()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass& cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_VARIABLES -> loadVariables(cload)
                else -> error("Unrecognized ComCenterGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass& cload)
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                MICROCHUNKID_UNUSED -> { /* no-op: legacy unused micro-chunk */ }
                else -> error("Unrecognized ComCenterGameObj variable chunk ID: ${cload.curMicroChunkId}")
            }
            cload.closeMicroChunk()
        }
    }

    // C++: void On_Destroyed()
    override fun onDestroyed() {
        super.onDestroyed()

        // C++: if (BaseController != NULL && CombatManager::I_Am_Server()) BaseController->Check_Radar()
        // (always server — omit I_Am_Server guard per guide)
        baseController?.checkRadar()
    }

    companion object {
        // C++: comcentergameobj.cpp enum { CHUNKID_PARENT = 0x02211154, CHUNKID_VARIABLES, ... }
        private const val CHUNKID_PARENT    = 0x02211154
        private const val CHUNKID_VARIABLES = 0x02211155

        private const val MICROCHUNKID_UNUSED = 1
    }
}
