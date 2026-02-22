package ccr.server.level.ldd

import ccr.server.level.ChunkIds
import ccr.server.level.LevelDynamicData
import ccr.server.mix.ChunkReader

object LddParser {

    // ScriptManager chunk IDs (scripts.cpp)
    private const val CHUNKID_SCRIPT_ENTRY = 131001134u
    private const val CHUNKID_SCRIPT_HEADER = 131001135u  // CHUNKID_SCRIPT_ENTRY + 1
    private const val MICRO_SCRIPT_NAME = 1               // MICROCHUNKID_NAME

    fun parse(lddData: ByteArray): LevelDynamicData {
        if (lddData.isEmpty()) return LevelDynamicData()

        val reader = ChunkReader(lddData)

        var levelInfo: LevelInfo? = null
        val gameObjects = mutableListOf<LoadedGameObject>()
        val spawners = mutableListOf<LoadedSpawner>()
        val scripts = mutableListOf<String>()

        reader.forEachChunk { chunkId, isContainer, chunkReader ->
            when (chunkId) {
                ChunkIds.CHUNKID_LEVEL_INFO -> {
                    levelInfo = LevelInfoLoader.load(chunkReader)
                }
                ChunkIds.CHUNKID_LEVEL_DATA -> {
                    chunkReader.forEachChunk { subId, _, subReader ->
                        // Game objects/spawners/scripts are nested inside CHUNKID_COMBAT_BEGIN
                        // (written by SaveLoadSystemClass for the combat subsystem)
                        if (subId == ChunkIds.CHUNKID_COMBAT_BEGIN) {
                            subReader.forEachChunk { combatId, _, combatReader ->
                                when (combatId) {
                                    ChunkIds.CHUNKID_GAMEOBJMANAGER -> parseGameObjects(combatReader, gameObjects)
                                    ChunkIds.CHUNKID_SPAWNERS -> spawners.addAll(SpawnerLoader.load(combatReader))
                                    ChunkIds.CHUNKID_SCRIPTS -> parseScripts(combatReader, scripts)
                                }
                            }
                        }
                    }
                }
            }
        }

        return LevelDynamicData(
            mapFilename = levelInfo?.mapFilename ?: "",
            missionDescriptionId = levelInfo?.missionDescriptionId ?: 0,
            description = levelInfo?.description ?: "",
            gameObjects = gameObjects,
            spawners = spawners,
            levelScripts = scripts,
        )
    }

    private fun parseGameObjects(gameobjReader: ChunkReader, out: MutableList<LoadedGameObject>) {
        gameobjReader.forEachChunk { factoryId, _, factoryChunkReader ->
            val objData = factoryChunkReader.findChunk(ChunkIds.SIMPLEFACTORY_CHUNKID_OBJDATA)
                ?: return@forEachChunk
            try {
                out.add(GameObjectFactory.load(factoryId, objData))
            } catch (e: Exception) {
                System.err.println("[WARN] Failed to parse game object (factory=0x${factoryId.toString(16)}): ${e.message}")
            }
        }
    }

    /**
     * Parses the ScriptManager save data.
     *
     * Structure per script (scripts.cpp):
     *   [CHUNKID_SCRIPT_ENTRY]
     *     [CHUNKID_SCRIPT_HEADER] — micro 1 = name string, micro 2 = params string
     *     [CHUNKID_SCRIPT_DATA]   — optional script-specific data
     */
    private fun parseScripts(scriptsReader: ChunkReader, out: MutableList<String>) {
        scriptsReader.forEachChunk { chunkId, _, entryReader ->
            if (chunkId != CHUNKID_SCRIPT_ENTRY) return@forEachChunk
            val headerChunk = entryReader.findChunk(CHUNKID_SCRIPT_HEADER) ?: return@forEachChunk
            val nameBytes = headerChunk.findMicroChunk(MICRO_SCRIPT_NAME) ?: return@forEachChunk
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val name = String(nameBytes, 0, if (nullIdx < 0) nameBytes.size else nullIdx, Charsets.ISO_8859_1)
            if (name.isNotEmpty()) out.add(name)
        }
    }
}
