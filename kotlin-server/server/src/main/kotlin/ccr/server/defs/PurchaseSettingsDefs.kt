package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A single entry in a PurchaseSettingsDefClass list.
 */
data class PurchaseEntry(
    val cost: Int = 0,
    val definitionId: Int = 0,
    val nameId: Int = 0,
    val texture: String = "",
    val alternateDefinitions: List<Int> = emptyList(),
    val alternateTextures: List<String> = emptyList(),
)

/**
 * Kotlin representation of PurchaseSettingsDefClass (Combat/purchasesettings.h).
 *
 * Defines the items available in the purchase terminal for a specific team+type combination.
 *
 * C++ defaults: Team(TEAM_GDI=0), Type(TYPE_CLASSES=0), all arrays zeroed.
 */
data class PurchaseSettingsDefClass(
    val id: UInt,
    val name: String,
    val team: Int = TEAM_GDI,
    val type: Int = TYPE_CLASSES,
    val entries: List<PurchaseEntry> = List(MAX_ENTRIES) { PurchaseEntry() },
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040607u  // CHUNKID_GLOBAL_SETTINGS_DEF_PURCHASE

        // Team enum
        const val TEAM_GDI = 0
        const val TEAM_NOD = 1
        const val TEAM_MUTANT_GDI = 2
        const val TEAM_MUTANT_NOD = 3

        // Type enum
        const val TYPE_CLASSES = 0
        const val TYPE_VEHICLES = 1
        const val TYPE_EQUIPMENT = 2
        const val TYPE_SECRET_CLASSES = 3
        const val TYPE_SECRET_VEHICLES = 4

        const val MAX_ENTRIES = 10
        const val MAX_ALTERNATES = 3

        // Chunk IDs
        private const val CHUNKID_PARENT = 0x08071203u
        private const val CHUNKID_VARIABLES = 0x08071204u

        // Base DefinitionClass
        private const val BASE_CHUNKID_VARIABLES = 0x00000100u
        private const val BASE_VARID_INSTANCEID = 0x01
        private const val BASE_VARID_NAME = 0x03

        // Micro-chunk variable IDs
        private const val VARID_TEAM = 1
        private const val VARID_TYPE = 2
        // 3 = XXX_VARID_ROW (deprecated), 4 = XXX_VARID_COL (deprecated)
        private const val VARID_COST = 5
        private const val VARID_DEFINITION = 6
        private const val VARID_TEXTURE_NAME = 7
        private const val VARID_NAME = 8
        private const val VARID_INDEX = 9
        private const val VARID_ALT_INDEX = 10
        private const val VARID_ALT_TEXTURE_NAME = 11
        private const val VARID_ALT_DEFINITION = 12

        fun load(objDataChunk: ChunkReader): PurchaseSettingsDefClass? {
            // Parse base DefinitionClass fields
            val parentChunk = objDataChunk.findChunk(CHUNKID_PARENT) ?: return null
            val baseVarsChunk = parentChunk.findChunkRecursive(BASE_CHUNKID_VARIABLES) ?: return null

            val idBytes = baseVarsChunk.findMicroChunk(BASE_VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = baseVarsChunk.findMicroChunk(BASE_VARID_NAME) ?: return null
            val name = decodeString(nameBytes)

            // Parse variables
            val vars = objDataChunk.findChunk(CHUNKID_VARIABLES)
                ?: return PurchaseSettingsDefClass(id = id, name = name)

            var team = TEAM_GDI
            var type = TYPE_CLASSES

            val costs = IntArray(MAX_ENTRIES)
            val definitions = IntArray(MAX_ENTRIES)
            val names = IntArray(MAX_ENTRIES)
            val textures = Array(MAX_ENTRIES) { "" }
            val altDefs = Array(MAX_ENTRIES) { IntArray(MAX_ALTERNATES) }
            val altTextures = Array(MAX_ENTRIES) { Array(MAX_ALTERNATES) { "" } }

            var entryIndex = 0
            var altIndex = 0

            vars.forEachMicroChunk { varId, data ->
                when (varId) {
                    VARID_TEAM -> if (data.size >= 4) team = readInt(data)
                    VARID_TYPE -> if (data.size >= 4) type = readInt(data)
                    VARID_INDEX -> if (data.size >= 4) entryIndex = readInt(data)
                    VARID_ALT_INDEX -> if (data.size >= 4) altIndex = readInt(data)
                    VARID_COST -> if (data.size >= 4 && entryIndex in 0 until MAX_ENTRIES)
                        costs[entryIndex] = readInt(data)
                    VARID_DEFINITION -> if (data.size >= 4 && entryIndex in 0 until MAX_ENTRIES)
                        definitions[entryIndex] = readInt(data)
                    VARID_NAME -> if (data.size >= 4 && entryIndex in 0 until MAX_ENTRIES)
                        names[entryIndex] = readInt(data)
                    VARID_TEXTURE_NAME -> if (entryIndex in 0 until MAX_ENTRIES)
                        textures[entryIndex] = decodeString(data)
                    VARID_ALT_DEFINITION -> if (data.size >= 4 && entryIndex in 0 until MAX_ENTRIES && altIndex in 0 until MAX_ALTERNATES)
                        altDefs[entryIndex][altIndex] = readInt(data)
                    VARID_ALT_TEXTURE_NAME -> if (entryIndex in 0 until MAX_ENTRIES && altIndex in 0 until MAX_ALTERNATES)
                        altTextures[entryIndex][altIndex] = decodeString(data)
                }
            }

            val entries = (0 until MAX_ENTRIES).map { i ->
                PurchaseEntry(
                    cost = costs[i],
                    definitionId = definitions[i],
                    nameId = names[i],
                    texture = textures[i],
                    alternateDefinitions = altDefs[i].toList(),
                    alternateTextures = altTextures[i].toList(),
                )
            }

            return PurchaseSettingsDefClass(
                id = id,
                name = name,
                team = team,
                type = type,
                entries = entries,
            )
        }
    }
}


/**
 * A single enlisted entry in a TeamPurchaseSettingsDefClass.
 */
data class TeamPurchaseEntry(
    val definitionId: Int = 0,
    val nameId: Int = 0,
    val texture: String = "",
)

/**
 * Kotlin representation of TeamPurchaseSettingsDefClass (Combat/teampurchasesettings.h).
 *
 * Defines the team-level purchase settings (beacon, supply, enlisted characters).
 *
 * C++ defaults: Team(TEAM_GDI=0), BeaconNameID(0), BeaconDefinitionID(0),
 *               BeaconCost(0), SupplyNameID(0), all arrays zeroed.
 */
data class TeamPurchaseSettingsDefClass(
    val id: UInt,
    val name: String,
    val team: Int = TeamPurchaseSettingsDefClass.TEAM_GDI,
    // Beacon settings
    val beaconDefinitionId: Int = 0,
    val beaconNameId: Int = 0,
    val beaconCost: Int = 0,
    val beaconTextureName: String = "",
    // Supply settings
    val supplyNameId: Int = 0,
    val supplyTextureName: String = "",
    // Enlisted character entries
    val entries: List<TeamPurchaseEntry> = List(MAX_ENTRIES) { TeamPurchaseEntry() },
) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040608u  // CHUNKID_GLOBAL_SETTINGS_DEF_TEAM_PURCHASE

        const val TEAM_GDI = 0
        const val TEAM_NOD = 1

        const val MAX_ENTRIES = 4

        // Chunk IDs
        private const val CHUNKID_PARENT = 0x10231215u
        private const val CHUNKID_VARIABLES = 0x10231216u

        // Base DefinitionClass
        private const val BASE_CHUNKID_VARIABLES = 0x00000100u
        private const val BASE_VARID_INSTANCEID = 0x01
        private const val BASE_VARID_NAME = 0x03

        // Micro-chunk variable IDs
        private const val VARID_TEAM = 1
        private const val VARID_DEFINITION = 2
        private const val VARID_TEXTURE_NAME = 3
        private const val VARID_NAME = 4
        private const val VARID_INDEX = 5
        private const val VARID_BEACON_DEFINITION = 6
        private const val VARID_BEACON_NAME = 7
        private const val VARID_BEACON_TEXTURE_NAME = 8
        private const val VARID_BEACON_COST = 9
        private const val VARID_SUPPLY_NAME = 10
        private const val VARID_SUPPLY_TEXTURE_NAME = 11

        fun load(objDataChunk: ChunkReader): TeamPurchaseSettingsDefClass? {
            // Parse base DefinitionClass fields
            val parentChunk = objDataChunk.findChunk(CHUNKID_PARENT) ?: return null
            val baseVarsChunk = parentChunk.findChunkRecursive(BASE_CHUNKID_VARIABLES) ?: return null

            val idBytes = baseVarsChunk.findMicroChunk(BASE_VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = baseVarsChunk.findMicroChunk(BASE_VARID_NAME) ?: return null
            val name = decodeString(nameBytes)

            // Parse variables
            val vars = objDataChunk.findChunk(CHUNKID_VARIABLES)
                ?: return TeamPurchaseSettingsDefClass(id = id, name = name)

            var team = TEAM_GDI
            var beaconDefinitionId = 0
            var beaconNameId = 0
            var beaconCost = 0
            var beaconTextureName = ""
            var supplyNameId = 0
            var supplyTextureName = ""

            val definitions = IntArray(MAX_ENTRIES)
            val names = IntArray(MAX_ENTRIES)
            val textures = Array(MAX_ENTRIES) { "" }

            var entryIndex = 0

            vars.forEachMicroChunk { varId, data ->
                when (varId) {
                    VARID_TEAM -> if (data.size >= 4) team = readInt(data)
                    VARID_BEACON_DEFINITION -> if (data.size >= 4) beaconDefinitionId = readInt(data)
                    VARID_BEACON_NAME -> if (data.size >= 4) beaconNameId = readInt(data)
                    VARID_BEACON_COST -> if (data.size >= 4) beaconCost = readInt(data)
                    VARID_BEACON_TEXTURE_NAME -> beaconTextureName = decodeString(data)
                    VARID_SUPPLY_NAME -> if (data.size >= 4) supplyNameId = readInt(data)
                    VARID_SUPPLY_TEXTURE_NAME -> supplyTextureName = decodeString(data)
                    VARID_INDEX -> if (data.size >= 4) entryIndex = readInt(data)
                    VARID_DEFINITION -> if (data.size >= 4 && entryIndex in 0 until MAX_ENTRIES)
                        definitions[entryIndex] = readInt(data)
                    VARID_NAME -> if (data.size >= 4 && entryIndex in 0 until MAX_ENTRIES)
                        names[entryIndex] = readInt(data)
                    VARID_TEXTURE_NAME -> if (entryIndex in 0 until MAX_ENTRIES)
                        textures[entryIndex] = decodeString(data)
                }
            }

            val entries = (0 until MAX_ENTRIES).map { i ->
                TeamPurchaseEntry(
                    definitionId = definitions[i],
                    nameId = names[i],
                    texture = textures[i],
                )
            }

            return TeamPurchaseSettingsDefClass(
                id = id,
                name = name,
                team = team,
                beaconDefinitionId = beaconDefinitionId,
                beaconNameId = beaconNameId,
                beaconCost = beaconCost,
                beaconTextureName = beaconTextureName,
                supplyNameId = supplyNameId,
                supplyTextureName = supplyTextureName,
                entries = entries,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

private fun readInt(data: ByteArray): Int =
    ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int

private fun decodeString(data: ByteArray): String {
    val nullIdx = data.indexOfFirst { it == 0.toByte() }
    val len = if (nullIdx < 0) data.size else nullIdx
    return String(data, 0, len, Charsets.ISO_8859_1)
}
