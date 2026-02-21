package ccr.server.defs.combat

import ccr.server.defs.forEachMicroChunk
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of CharacterClassSettingsDefClass (Combat/characterclasssettings.cpp).
 *
 * Stores cost and soldier-definition-ID lookup tables indexed by character class, rank, and team.
 *
 * C++ arrays: CostTable[CLASS_COUNT][RANK_COUNT][TEAM_COUNT], DefinitionTable[same].
 *
 * Class indices: 0=Minigunner, 1=RocketSoldier, 2=Grenadier, 3=Engineer, 4=FlameThrower, 5=Mutant
 * Rank indices:  0=Enlisted, 1=Officer, 2=SpecialForces, 3=Boss
 * Team indices:  0=GDI, 1=NOD
 */
data class CharacterClassSettingsDefClass(
    val name: String,
    val id: UInt,
    val classId: UInt,
    /** costTable[classIndex][rankIndex][teamIndex] — purchase cost for a character. */
    val costTable: List<List<List<Int>>> = defaultTable(),
    /** definitionTable[classIndex][rankIndex][teamIndex] — soldier definition ID. */
    val definitionTable: List<List<List<Int>>> = defaultTable(),
) {
    companion object {
        const val CLASS_ID: UInt = 0xF006u // CLASSID_GLOBAL_SETTINGS_DEF_CHAR_CLASS

        const val CLASS_COUNT = 6
        const val RANK_COUNT = 4
        const val TEAM_COUNT = 2
    }
}

private fun defaultTable(): List<List<List<Int>>> =
    List(CharacterClassSettingsDefClass.CLASS_COUNT) {
        List(CharacterClassSettingsDefClass.RANK_COUNT) {
            List(CharacterClassSettingsDefClass.TEAM_COUNT) { 0 }
        }
    }

// Chunk IDs from characterclasssettings.cpp local enum
private const val CHUNKID_VARIABLES = 0x12021028u

// VARID block layout (each block = 6 classes * 4 ranks = 24 entries):
//   1-24:  Cost GDI
//   25-48: Cost NOD
//   49-72: DefID GDI
//   73-96: DefID NOD
private const val VARID_COST_GDI_BASE = 1
private const val VARID_COST_NOD_BASE = 25
private const val VARID_DEFID_GDI_BASE = 49
private const val VARID_DEFID_NOD_BASE = 73
private const val BLOCK_SIZE = 24 // CLASS_COUNT * RANK_COUNT

fun parseCharacterClassSettingsDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): CharacterClassSettingsDefClass? {
    val varsReader = objDataReader.findChunk(CHUNKID_VARIABLES) ?: return null

    val costTable = Array(CharacterClassSettingsDefClass.CLASS_COUNT) {
        Array(CharacterClassSettingsDefClass.RANK_COUNT) {
            IntArray(CharacterClassSettingsDefClass.TEAM_COUNT)
        }
    }
    val defTable = Array(CharacterClassSettingsDefClass.CLASS_COUNT) {
        Array(CharacterClassSettingsDefClass.RANK_COUNT) {
            IntArray(CharacterClassSettingsDefClass.TEAM_COUNT)
        }
    }

    varsReader.forEachMicroChunk { varId, data ->
        if (data.size < 4) return@forEachMicroChunk
        val value = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int

        val (table, teamIdx, offset) = when (varId) {
            in VARID_COST_GDI_BASE until VARID_COST_GDI_BASE + BLOCK_SIZE ->
                Triple(costTable, 0, varId - VARID_COST_GDI_BASE)
            in VARID_COST_NOD_BASE until VARID_COST_NOD_BASE + BLOCK_SIZE ->
                Triple(costTable, 1, varId - VARID_COST_NOD_BASE)
            in VARID_DEFID_GDI_BASE until VARID_DEFID_GDI_BASE + BLOCK_SIZE ->
                Triple(defTable, 0, varId - VARID_DEFID_GDI_BASE)
            in VARID_DEFID_NOD_BASE until VARID_DEFID_NOD_BASE + BLOCK_SIZE ->
                Triple(defTable, 1, varId - VARID_DEFID_NOD_BASE)
            else -> return@forEachMicroChunk
        }

        val classIdx = offset / CharacterClassSettingsDefClass.RANK_COUNT
        val rankIdx = offset % CharacterClassSettingsDefClass.RANK_COUNT
        table[classIdx][rankIdx][teamIdx] = value
    }

    return CharacterClassSettingsDefClass(
        name = name,
        id = id,
        classId = classId,
        costTable = costTable.map { cls -> cls.map { rank -> rank.toList() } },
        definitionTable = defTable.map { cls -> cls.map { rank -> rank.toList() } },
    )
}
