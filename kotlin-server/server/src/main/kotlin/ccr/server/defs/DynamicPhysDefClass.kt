package ccr.server.defs

/**
 * Kotlin representation of DynamicPhysDefClass (wwphys/dynamicphys.h).
 * Inherits from PhysDefClass. Has no own saved fields.
 *
 * C++ chunk layout: wraps PhysDefClass::Save in DYNAMICPHYSDEF_CHUNK_PHYSDEF.
 */
open class DynamicPhysDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
) : PhysDefClass(name, id, chunkId, modelName, isPreLit) {

    companion object {
        // Chunk ID from dynamicphys.cpp: wraps PhysDefClass data
        internal const val DYNAMICPHYSDEF_CHUNK_PHYSDEF = 813001104u // 0x307_56990
    }
}
