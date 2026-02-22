package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DefinitionEntry(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
)

/**
 * Parses an Objects.DDB binary blob (from always.dat → Objects.DDB) and returns
 * all definition entries found inside.
 *
 * Objects.DDB structure (from wwsaveload/definitionmgr.cpp):
 *   [CHUNKID_SAVELOAD_DEFMGR=0x101]
 *     [CHUNKID_VARIABLES=0x100]  ← skip
 *     [CHUNKID_OBJECTS=0x101]
 *       [<chunkId>] for each definition
 *         [CHUNKID_VARIABLES=0x100]
 *           micro[0x01] = uint32 LE definition ID
 *           micro[0x03] = null-terminated name string
 */
// From wwsaveload/persistfactory.h: SimplePersistFactoryClass internal chunk IDs.
// DefinitionMgrClass::Save calls factory.Save(csave, def) which wraps each definition in:
//   [SIMPLEFACTORY_CHUNKID_OBJPOINTER = 0x00100100]  4-byte old pointer (for pointer fixup)
//   [SIMPLEFACTORY_CHUNKID_OBJDATA    = 0x00100101]  def->Save() output: contains CHUNKID_VARIABLES
private const val SIMPLEFACTORY_CHUNKID_OBJDATA = 0x00100101u

fun readDefinitions(ddbData: ByteArray): List<DefinitionEntry> {
    val CHUNKID_SAVELOAD_DEFMGR = 0x00000101u
    val CHUNKID_OBJECTS = 0x00000101u
    // DefinitionClass::Save_Variables writes micro-chunks inside CHUNKID_VARIABLES (0x100).
    // This chunk is nested several levels deep inside OBJDATA (the full save chain for each
    // derived definition class wraps it in per-class parent chunks before reaching the base).
    val CHUNKID_VARIABLES = 0x00000100u
    val VARID_INSTANCEID = 0x01
    val VARID_NAME = 0x03

    val outerReader = ChunkReader(ddbData)

    val defMgrChunk = outerReader.findChunk(CHUNKID_SAVELOAD_DEFMGR)
        ?: return emptyList()

    val objectsChunk = defMgrChunk.findChunk(CHUNKID_OBJECTS)
        ?: return emptyList()

    val entries = mutableListOf<DefinitionEntry>()

    objectsChunk.forEachChunk { chunkId, _, defChunkReader ->
        // Factory wraps definition in OBJPOINTER + OBJDATA — navigate into OBJDATA first.
        val objDataChunk = defChunkReader.findChunk(SIMPLEFACTORY_CHUNKID_OBJDATA)
            ?: return@forEachChunk

        // Each derived class (SoldierGameObjDef → SmartGameObjDef → … → DefinitionClass)
        // wraps the parent Save in its own chunk, so CHUNKID_VARIABLES (0x100) from
        // DefinitionClass is deeply nested.  Search recursively.
        val variablesChunk = objDataChunk.findChunkRecursive(CHUNKID_VARIABLES)
            ?: return@forEachChunk

        val idBytes = variablesChunk.findMicroChunk(VARID_INSTANCEID)
        val nameBytes = variablesChunk.findMicroChunk(VARID_NAME)

        if (idBytes == null || nameBytes == null) return@forEachChunk
        if (idBytes.size < 4) return@forEachChunk

        val id = ByteBuffer.wrap(idBytes, 0, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
            .toUInt()

        val nullIndex = nameBytes.indexOfFirst { it == 0.toByte() }
        val nameLength = if (nullIndex < 0) nameBytes.size else nullIndex
        val name = String(nameBytes, 0, nameLength, Charsets.ISO_8859_1)

        if (name.isEmpty()) return@forEachChunk

        entries += DefinitionEntry(name = name, id = id, chunkId = chunkId)
    }

    return entries.sortedBy { it.name }
}
