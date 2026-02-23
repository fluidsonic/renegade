package ccr.server.net

import ccr.net.replication.NetworkObject
import ccr.server.GameObjManager
import ccr.server.defs.BaseGameObjDef

// C++: BaseGameObj : public PersistClass, public NetworkObjectClass
abstract class BaseGameObj : NetworkObject() {

    override val networkClassId: Int = 1000  // C++: NETCLASSID_GAMEOBJ

    // C++: const BaseGameObjDef* Definition (initialized to NULL)
    var definition: BaseGameObjDef? = null

    // C++: IsPostThinkAllowed (initialized to false)
    var isPostThinkAllowed: Boolean = false
        private set

    // C++: EnableCinematicFreeze (initialized to true)
    var enableCinematicFreeze: Boolean = true

    // C++: virtual void Init() = 0
    abstract fun init()

    // C++: void Init(const BaseGameObjDef & definition)
    fun init(definition: BaseGameObjDef) {
        this.definition = definition
    }

    // C++: virtual bool Save(ChunkSaveClass & csave)
    open fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_IS_PENDING_DELETE, isDeletePending)
        csave.writeMicroChunk(MICROCHUNKID_DEFINITION_ID, definition!!.id.toInt())
        csave.writeMicroChunk(MICROCHUNKID_INSTANCE_ID, networkId)
        csave.writeMicroChunk(MICROCHUNKID_ENABLE_CINEMATIC_FREEZE, enableCinematicFreeze)
        csave.endChunk()
        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    open fun load(cload: ChunkLoadClass): Boolean {
        var id = 0
        cload.openChunk()
        while (cload.openMicroChunk()) {
            when (cload.curMicroChunkId) {
                MICROCHUNKID_IS_PENDING_DELETE -> { if (cload.readBool()) setDeletePending() }
                MICROCHUNKID_INSTANCE_ID       -> { id = cload.readInt() }
                MICROCHUNKID_DEFINITION_ID     -> { definition = DefinitionMgrClass.findDefinition(cload.readInt()) as? BaseGameObjDef }
                MICROCHUNKID_ENABLE_CINEMATIC_FREEZE -> { enableCinematicFreeze = cload.readBool() }
                else -> error("Unrecognized BaseGameObj variable chunk ID: ${cload.curMicroChunkId}")
            }
            cload.closeMicroChunk()
        }
        cload.closeChunk()
        if (id == 0) {
            if (networkId == 0) setNetworkId(NetworkObjectManager.getNewDynamicId())
        } else {
            setNetworkId(id)
        }
        return true
    }

    init {
        GameObjManager.add(this)
        setObjectDirtyBit(BIT_CREATION, true)
    }

    // C++: virtual void Delete() { delete this; }
    // FIXME: should call destruct() to mirror delete this → ~BaseGameObj()
    override fun delete() {}

    // C++: ~BaseGameObj() — calls GameObjManager::Remove(this)
    // FIXME: not called yet — wire into deletion pipeline
    override fun destruct() {
        GameObjManager.remove(this)
        super.destruct()
    }

    // C++: virtual void Think() { IsPostThinkAllowed = true; }
    open fun think() {
        isPostThinkAllowed = true
    }

    // C++: virtual void Post_Think() {}
    open fun postThink() {}

    // C++: virtual bool Is_Hibernating() { return false; }
    open fun isHibernating(): Boolean = false

    companion object {
        private const val CHUNKID_VARIABLES                    = 910991407
        private const val MICROCHUNKID_DEFINITION_ID           = 2
        private const val MICROCHUNKID_INSTANCE_ID             = 3
        private const val MICROCHUNKID_IS_PENDING_DELETE       = 4
        private const val MICROCHUNKID_ENABLE_CINEMATIC_FREEZE = 5
    }
}
