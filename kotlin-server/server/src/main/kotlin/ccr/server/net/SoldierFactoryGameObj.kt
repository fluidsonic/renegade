package ccr.server.net

import ccr.server.defs.SoldierFactoryGameObjDef

// C++: SoldierFactoryGameObj : public BuildingGameObj (soldierfactorygameobj.h)
// Hierarchy: NetworkObject → BaseGameObj → ScriptableGameObj → DamageableGameObj →
//            BuildingGameObj → SoldierFactoryGameObj
//
// SoldierFactoryGameObj adds no new member fields. Its only behaviour on top of
// BuildingGameObj is to register/revoke the "can generate soldiers" capability on
// the base controller via CnC_Initialize and On_Destroyed.
class SoldierFactoryGameObj() : BuildingGameObj() {

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
            name = "soldierfactory_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: virtual void Init(void) — calls Init(Get_Definition())
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const SoldierFactoryGameObjDef& definition)
    fun init(definition: SoldierFactoryGameObjDef) {
        // C++: BuildingGameObj::Init(definition)
        super.init(definition)
    }

    // C++: const SoldierFactoryGameObjDef& Get_Definition() const
    override fun getDefinition(): SoldierFactoryGameObjDef =
        definition as SoldierFactoryGameObjDef

    // C++: SoldierFactoryGameObj* As_SoldierFactoryGameObj() { return this; }
    // → handled by Kotlin type system

    // -------------------------------------------------------------------------
    // C++: virtual bool Save(ChunkSaveClass& csave)
    // -------------------------------------------------------------------------
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
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
                else -> error("Unrecognized SoldierFactoryGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        return true
    }

    // C++: void Load_Variables(ChunkLoadClass& cload)
    // The switch block is entirely commented out in C++ — just loop and close.
    private fun loadVariables(cload: ChunkLoadClass) {
        while (cload.openMicroChunk()) {
            // C++: switch is commented out — no micro-chunks are defined
            cload.closeMicroChunk()
        }
    }

    // -------------------------------------------------------------------------
    // C++: void CnC_Initialize(BaseControllerClass* base)
    // -------------------------------------------------------------------------
    override fun cncInitialize(base: BaseControllerClass) {
        // C++: BuildingGameObj::CnC_Initialize(base)
        super.cncInitialize(base)

        // C++: if (BaseController != NULL) BaseController->Set_Can_Generate_Soldiers(true)
        if (baseController != null) {
            baseController!!.setCanGenerateSoldiers(true)
        }
    }

    // -------------------------------------------------------------------------
    // C++: virtual void On_Destroyed()
    // -------------------------------------------------------------------------
    override fun onDestroyed() {
        // C++: BuildingGameObj::On_Destroyed()
        super.onDestroyed()

        // C++: if (BaseController != NULL && CombatManager::I_Am_Server())
        //          BaseController->Set_Can_Generate_Soldiers(false)
        // (always server — omit I_Am_Server guard per guide)
        if (baseController != null) {
            baseController!!.setCanGenerateSoldiers(false)
        }
    }

    companion object {
        // C++: enum { CHUNKID_PARENT = 0x02211154, CHUNKID_VARIABLES, ... }
        private const val CHUNKID_PARENT    = 0x02211154
        private const val CHUNKID_VARIABLES = 0x02211155

        // C++: MICROCHUNKID_UNUSED = 1 (switch entirely commented out — unused)
        @Suppress("unused")
        private const val MICROCHUNKID_UNUSED = 1
    }
}
