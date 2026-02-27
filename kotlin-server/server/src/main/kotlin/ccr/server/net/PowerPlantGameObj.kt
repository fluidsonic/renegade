package ccr.server.net


// C++: PowerPlantGameObj (powerplant.cpp) — extends BuildingGameObj.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj → PowerPlantGameObj
class PowerPlantGameObj() : BuildingGameObj() {

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
            name = "powerplant_$definitionId", id = definitionId.toUInt(), chunkId = 0u
        )
        this.position = position
        this.collectionSphere = ccr.server.level.Sphere(sphereCenter, sphereRadius)
        this.isDestroyed = isDestroyed
        this.isPowerOn = isPowerOn
        defenseObject.health = health
    }

    // C++: PowerPlantGameObj::On_Destroyed — triggers base power check
    override fun onDestroyed() {
        super.onDestroyed()
        val ctrl = baseController ?: return
        val powerPlants = ctrl.getBuildings().filterIsInstance<PowerPlantGameObj>()
        ctrl.checkBasePower(powerPlants)
    }

    // C++: PowerPlantGameObj::Save — CHUNKID_PARENT=0x02211154, CHUNKID_VARIABLES=0x02211155
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()
        csave.beginChunk(CHUNKID_VARIABLES)
        csave.endChunk()
        return true
    }

    // C++: PowerPlantGameObj::Load
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_VARIABLES -> { /* no variables currently */ }
            }
            cload.closeChunk()
        }
        return true
    }

    companion object {
        private const val CHUNKID_PARENT    = 0x02211154
        private const val CHUNKID_VARIABLES = 0x02211155
    }
}
