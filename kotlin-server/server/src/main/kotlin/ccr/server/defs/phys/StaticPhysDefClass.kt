package ccr.server.defs.phys

import ccr.server.defs.PhysDefClass
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of StaticPhysDefClass (wwphys/staticphys.h).
 *
 * Inheritance: StaticPhysDefClass -> PhysDefClass -> DefinitionClass
 *
 * C++ defaults: IsNonOccluder(true)
 *
 * Chunk layout inside OBJDATA:
 *   [STATICPHYSDEF_CHUNK_PHYSDEF    = 0x01070002]  -> parent PhysDefClass
 *     [PHYSDEF_CHUNK_DEFINITION     = 0x055FFE07]  -> DefinitionClass (name, id)
 *     [PHYSDEF_CHUNK_VARIABLES      = 0x055FFE08]  -> ModelName, IsPreLit
 *   [STATICPHYSDEF_CHUNK_VARIABLES  = 0x01070003]  -> IsNonOccluder
 */
open class StaticPhysDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
    val isNonOccluder: Boolean = true,
) : PhysDefClass(name, id, chunkId, modelName, isPreLit) {

    companion object {
        /** CLASSID_STATICPHYSDEF = CLASSID_PHYSICS(0x9000) + 7 */
        const val CHUNK_ID: UInt = 0x00020508u  // PHYSICS_CHUNKID_STATICPHYSDEF
    }
}

// Chunk IDs from staticphys.cpp local enum
private const val STATICPHYSDEF_CHUNK_PHYSDEF = 0x01070002u
private const val STATICPHYSDEF_CHUNK_VARIABLES = 0x01070003u

// Micro-chunk IDs
private const val STATICPHYSDEF_VARIABLE_ISNONOCCLUDER = 0x00

fun parseStaticPhysDefClass(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): StaticPhysDefClass {
    // PhysDefClass fields (nested inside STATICPHYSDEF_CHUNK_PHYSDEF)
    val physDefChunk = objDataReader.findChunk(STATICPHYSDEF_CHUNK_PHYSDEF)
    val physDefVars = physDefChunk?.findChunk(PhysDefClass.CHUNK_VARIABLES)

    val modelName = physDefVars?.readMicroString(PhysDefClass.VARIABLE_MODELNAME) ?: "NULL"
    val isPreLit = physDefVars?.readMicroBool(PhysDefClass.VARIABLE_ISPRELIT) ?: false

    // StaticPhysDefClass own fields
    val vars = objDataReader.findChunk(STATICPHYSDEF_CHUNK_VARIABLES)
    val isNonOccluder = vars?.readMicroBool(STATICPHYSDEF_VARIABLE_ISNONOCCLUDER) ?: true

    return StaticPhysDefClass(
        name = name,
        id = id,
        chunkId = chunkId,
        modelName = modelName,
        isPreLit = isPreLit,
        isNonOccluder = isNonOccluder,
    )
}
