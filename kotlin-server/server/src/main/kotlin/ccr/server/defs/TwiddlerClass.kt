package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * C++: TwiddlerClass in wwsaveload/twiddler.h
 * Hierarchy: DefinitionClass → TwiddlerClass (leaf)
 *
 * A twiddler randomly selects from a list of definition IDs to create objects.
 * m_IndirectClassID constrains which class of definitions the list contains.
 */
class TwiddlerClass(
    name: String,
    id: UInt,
    classId: UInt,
    val indirectClassID: UInt = 0u,
    val definitionList: List<Int> = emptyList(),
) : DefinitionClass(name, id, classId) {

    companion object {
        // Chunk IDs from twiddler.cpp
        private const val CHUNKID_VARIABLES = 0x00000100u
        private const val CHUNKID_BASE_CLASS = 0x00000200u

        // Base DefinitionClass micro-chunk IDs
        private const val DEF_CHUNKID_VARIABLES = 0x00000100u
        private const val VARID_INSTANCEID = 0x01
        private const val VARID_NAME = 0x03

        // Twiddler variable IDs from twiddler.cpp
        private const val VARID_DEFINTION_ID = 0x01
        private const val VARID_INDIRECT_CLASSID = 0x02

        fun load(classId: UInt, objDataChunk: ChunkReader): TwiddlerClass? {
            // TwiddlerClass::Save writes CHUNKID_VARIABLES first, then CHUNKID_BASE_CLASS.
            // CHUNKID_BASE_CLASS contains DefinitionClass::Save (id + name).
            val baseChunk = objDataChunk.findChunk(CHUNKID_BASE_CLASS) ?: return null
            val baseVarsChunk = baseChunk.findChunkRecursive(DEF_CHUNKID_VARIABLES) ?: return null

            val idBytes = baseVarsChunk.findMicroChunk(VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = baseVarsChunk.findMicroChunk(VARID_NAME) ?: return null
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val nameLen = if (nullIdx < 0) nameBytes.size else nullIdx
            val name = String(nameBytes, 0, nameLen, Charsets.ISO_8859_1)

            // Parse twiddler-specific variables.
            // CHUNKID_VARIABLES (0x100) contains micro-chunks:
            //   VARID_INDIRECT_CLASSID (0x02) — single uint32
            //   VARID_DEFINTION_ID (0x01) — repeated, one per definition in the list
            //
            // Note: Both CHUNKID_VARIABLES and DEF_CHUNKID_VARIABLES are 0x100.
            // TwiddlerClass writes its own CHUNKID_VARIABLES as a direct child of OBJDATA,
            // while DefinitionClass's CHUNKID_VARIABLES is nested inside CHUNKID_BASE_CLASS.
            // findChunk returns the FIRST match (twiddler's own), which is correct.
            val varsChunk = objDataChunk.findChunk(CHUNKID_VARIABLES)

            var indirectClassID = 0u
            val definitionList = mutableListOf<Int>()

            varsChunk?.forEachMicroChunk { mcId, mcData ->
                when (mcId) {
                    VARID_INDIRECT_CLASSID -> {
                        if (mcData.size >= 4) {
                            indirectClassID = ByteBuffer.wrap(mcData, 0, 4)
                                .order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
                        }
                    }
                    VARID_DEFINTION_ID -> {
                        if (mcData.size >= 4) {
                            definitionList += ByteBuffer.wrap(mcData, 0, 4)
                                .order(ByteOrder.LITTLE_ENDIAN).int
                        }
                    }
                }
            }

            return TwiddlerClass(
                name = name,
                id = id,
                classId = classId,
                indirectClassID = indirectClassID,
                definitionList = definitionList,
            )
        }
    }
}
