package ccr.server.net

import ccr.math.Matrix3D
import ccr.server.defs.TransitionGameObjDef

// C++: TransitionGameObj : public BaseGameObj
// Hierarchy: NetworkObject → BaseGameObj → TransitionGameObj
class TransitionGameObj : BaseGameObj() {

    // C++: DynamicVectorClass<TransitionInstanceClass*> TransitionInstances
    var transitionInstances: MutableList<TransitionInstanceClass> = mutableListOf()

    // C++: Matrix3D TM (initialized to identity)
    var tm: Matrix3D = Matrix3D()

    // C++: int LadderIndex (initialized to -1 via constructor initializer list)
    // @JvmName avoids clash with fun getLadderIndex() / fun setLadderIndex() below
    @get:JvmName("ladderIndexField") @set:JvmName("setLadderIndexField")
    var ladderIndex: Int = -1

    // C++: ~TransitionGameObj() — calls Destroy_Transitions()
    override fun destruct() {
        destroyTransitions()
        super.destruct()
    }

    // C++: virtual void Init()
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const TransitionGameObjDef & definition)
    fun init(definition: TransitionGameObjDef) {
        super.init(definition)
        createTransitions()
    }

    // C++: const TransitionGameObjDef & Get_Definition() const
    fun getDefinition(): TransitionGameObjDef = definition as TransitionGameObjDef

    // C++: virtual bool Save(ChunkSaveClass&)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_TRANSFORM,    tm)
        csave.writeMicroChunk(MICROCHUNKID_LADDER_INDEX, ladderIndex)
        csave.endChunk()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass&)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)
                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_TRANSFORM    -> tm          = cload.readMatrix3D()
                            MICROCHUNKID_LADDER_INDEX -> ladderIndex = cload.readInt()
                            else -> error("Unrecognized TransitionGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                else -> error("Unrecognized TransitionGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this) — pointer remap not ported
        return true
    }

    // C++: virtual void On_Post_Load()
    override fun onPostLoad() {
        super.onPostLoad()
        createTransitions()
    }

    // C++: void Set_Transform(const Matrix3D & tm)
    fun setTransform(tm: Matrix3D) {
        this.tm = tm
    }

    // C++: const Matrix3D & Get_Transform() const
    fun getTransform(): Matrix3D = tm

    // C++: void Create_Transitions()
    fun createTransitions() {
        destroyTransitions()

        // Only create the transitions if they haven't already been created
        check(transitionInstances.isEmpty())
        if (transitionInstances.isEmpty()) {
            val transDataList = getDefinition().transitions
            val currentTm = getTransform()

            for (transData in transDataList) {
                // make new instance
                val trans = TransitionInstanceClass(transData)
                // setup
                trans.setParentTransform(currentTm)
                // add to our list
                transitionInstances.add(trans)
                // add to master list
                TransitionManager.add(trans)
            }
        }

        // Ensure all the transition instances know what ladder they belong to
        setLadderIndex(ladderIndex)
    }

    // C++: void Destroy_Transitions()
    fun destroyTransitions() {
        while (transitionInstances.isNotEmpty()) {
            TransitionManager.destroy(transitionInstances[0])
            transitionInstances.removeAt(0)
        }

        // They get put in a list, so flush the list.
        TransitionManager.destroyPending()
    }

    // C++: void Update_Transitions()
    fun updateTransitions() {
        val currentTm = getTransform()

        // Update all transitions
        for (trans in transitionInstances) {
            trans.setParentTransform(currentTm)
            trans.setLadderIndex(ladderIndex)
        }
    }

    // C++: int Get_Transition_Count() const
    fun getTransitionCount(): Int = transitionInstances.size

    // C++: TransitionInstanceClass* Get_Transition(int index)
    fun getTransition(index: Int): TransitionInstanceClass = transitionInstances[index]

    // C++: int Get_Ladder_Index()
    fun getLadderIndex(): Int = ladderIndex

    // C++: void Set_Ladder_Index(int ladder_index)
    fun setLadderIndex(ladderIndex: Int) {
        this.ladderIndex = ladderIndex

        // Update all transitions
        for (trans in transitionInstances) {
            trans.setLadderIndex(ladderIndex)
        }
    }

    companion object {
        // Chunk IDs from transitiongameobj.cpp (TransitionGameObj)
        private const val CHUNKID_PARENT    = 1111991206
        private const val CHUNKID_VARIABLES = 1111991207

        // Micro chunk IDs from transitiongameobj.cpp
        private const val MICROCHUNKID_TRANSFORM    = 1
        private const val MICROCHUNKID_LADDER_INDEX = 2
    }
}
