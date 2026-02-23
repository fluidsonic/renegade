package ccr.server.net

// C++: ReferenceableClass<ScriptableGameObj> (reflist.h)
// Embedded in ScriptableGameObj — tracks all GameObjReference instances pointing to it.
// On destruct(), nulls them all to prevent dangling references.
class ReferenceableGameObj(private val owner: ScriptableGameObj) {

    // C++: ReferencerClass *ReferencerListHead
    internal var referencerListHead: GameObjReference? = null

    // C++: T* Get_Data()
    fun getData(): ScriptableGameObj = owner

    internal fun addReference(ref: GameObjReference) {
        ref.nextInTargetList = referencerListHead
        referencerListHead = ref
    }

    internal fun removeReference(ref: GameObjReference) {
        var prev: GameObjReference? = null
        var curr = referencerListHead
        while (curr != null) {
            if (curr === ref) {
                if (prev == null) referencerListHead = curr.nextInTargetList
                else prev.nextInTargetList = curr.nextInTargetList
                ref.nextInTargetList = null
                return
            }
            prev = curr
            curr = curr.nextInTargetList
        }
    }

    companion object {
        private const val CHUNKID_REF_VARIABLES = 913991844  // C++: reflist.h
    }

    // C++: ReferenceableClass::Save — writes this-pointer for save/load pointer remapping
    fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_REF_VARIABLES)
        // FIXME: write this-pointer via MICROCHUNKID_PTR when pointer remap system is ported
        csave.endChunk()
        return true
    }

    // C++: ReferenceableClass::Load — registers old→new pointer mapping
    fun load(cload: ChunkLoadClass): Boolean {
        cload.openChunk()
        while (cload.openMicroChunk()) {
            // FIXME: handle MICROCHUNKID_PTR when pointer remap system is ported
            cload.closeMicroChunk()
        }
        cload.closeChunk()
        return true
    }

    // C++: ~ReferenceableClass() — null all references
    fun destruct() {
        var curr = referencerListHead
        while (curr != null) {
            val next = curr.nextInTargetList
            curr.nullify()
            curr = next
        }
        referencerListHead = null
    }
}

// C++: ReferencerClass / GameObjReference (reflist.h)
// Holds a nullable reference to a ScriptableGameObj.
// When the target is destroyed, this reference is automatically nulled.
class GameObjReference {

    // C++: ReferenceableClass<ScriptableGameObj> *ReferenceTarget
    private var target: ReferenceableGameObj? = null

    // C++: ReferencerClass *TargetReferencerListNext — intrusive linked list
    internal var nextInTargetList: GameObjReference? = null

    constructor()

    // C++: ReferencerClass(const ScriptableGameObj* target)
    constructor(target: ScriptableGameObj?) { set(target) }

    // C++: ~ReferencerClass() { operator=(NULL); }
    fun destruct() { set(null) }

    // C++: operator=(const ScriptableGameObj*)
    fun set(obj: ScriptableGameObj?) {
        target?.removeReference(this)
        target = obj?.referenceableGameObj
        target?.addReference(this)
    }

    // C++: ScriptableGameObj* Get_Ptr() — ReferenceTarget ? ReferenceTarget->Get_Data() : NULL
    fun get(): ScriptableGameObj? = target?.getData()

    // C++: operator ScriptableGameObj*()
    operator fun invoke(): ScriptableGameObj? = get()

    // Called by ReferenceableGameObj.destruct() — nulls without unregistering
    internal fun nullify() {
        target = null
        nextInTargetList = null
    }
}
