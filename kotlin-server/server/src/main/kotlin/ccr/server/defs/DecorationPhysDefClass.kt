package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of DecorationPhysDefClass (wwphys/decophys.h).
 *
 * Inheritance: DecorationPhysDefClass → DynamicPhysDefClass → PhysDefClass → DefinitionClass
 *
 * This class has no own saved variables. It only delegates to its parent class chain.
 *
 * Chunk layout inside OBJDATA:
 *   [DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF = 0x01070004]  → parent DynamicPhysDefClass
 *     [DYNAMICPHYSDEF_CHUNK_PHYSDEF]                        → parent PhysDefClass
 *       [PHYSDEF_CHUNK_VARIABLES]                            → ModelName, IsPreLit
 *   (legacy: DECORATIONPHYSDEF_CHUNK_PHYSDEF = 0x01070003 → PhysDefClass directly)
 */
open class DecorationPhysDefClass(
    name: String,
    id: UInt,
    classId: UInt,
    modelName: String = "NULL",
    isPreLit: Boolean = false,
) : DynamicPhysDefClass(name, id, classId, modelName, isPreLit) {

    companion object {
        /** CLASSID_DECOPHYSDEF = CLASSID_PHYSICS + 0 = 0x9000 */
        const val CLASS_ID: UInt = 0x9000u

        // Chunk IDs from decophys.cpp
        internal const val DECORATIONPHYSDEF_CHUNK_PHYSDEF = 0x01070003u         // old parent (PhysDefClass direct)
        internal const val DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF = 0x01070004u  // current parent (DynamicPhysDefClass)

        /**
         * Parses the PhysDefClass fields from within a DecorationPhysDefClass chunk context.
         * Handles both new format (DynamicPhysDefClass wrapper) and old format (PhysDefClass direct).
         * Returns (modelName, isPreLit).
         */
        internal fun parseParentFields(decoChunk: ChunkReader): Pair<String, Boolean> {
            val dynPhysChunk = decoChunk.findChunk(DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF)
            val physDefChunk = dynPhysChunk?.findChunk(DynamicPhysDefClass.DYNAMICPHYSDEF_CHUNK_PHYSDEF)
                ?: decoChunk.findChunk(DECORATIONPHYSDEF_CHUNK_PHYSDEF)
            return if (physDefChunk != null) PhysDefClass.parseFields(physDefChunk) else ("NULL" to false)
        }
    }
}

fun parseDecorationPhysDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): DecorationPhysDefClass {
    val (modelName, isPreLit) = DecorationPhysDefClass.parseParentFields(objDataReader)

    return DecorationPhysDefClass(
        name = name,
        id = id,
        classId = classId,
        modelName = modelName,
        isPreLit = isPreLit,
    )
}
