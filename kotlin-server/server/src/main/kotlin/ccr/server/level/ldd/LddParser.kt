package ccr.server.level.ldd

import ccr.server.defs.readMicroInt
import ccr.server.level.ChunkIds
import ccr.server.level.LevelDynamicData
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object LddParser {

    // ScriptManager chunk IDs (scripts.cpp)
    private const val CHUNKID_SCRIPT_ENTRY = 131001134u
    private const val CHUNKID_SCRIPT_HEADER = 131001135u  // CHUNKID_SCRIPT_ENTRY + 1
    private const val MICRO_SCRIPT_NAME = 1               // MICROCHUNKID_NAME
    private const val MICRO_SCRIPT_PARAM = 2              // MICROCHUNKID_PARAM

    fun parse(lddData: ByteArray): LevelDynamicData {
        if (lddData.isEmpty()) return LevelDynamicData()

        val reader = ChunkReader(lddData)

        var levelInfo: LevelInfo? = null
        val gameObjects = mutableListOf<LoadedGameObject>()
        val spawners = mutableListOf<LoadedSpawner>()
        val scripts = mutableListOf<ScriptAttachment>()
        var nextDynamicId = 0

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
                                    ChunkIds.CHUNKID_GAMEOBJMANAGER -> {
                                        parseGameObjects(combatReader, gameObjects)
                                        // CHUNKID_GAMEOBJ_VARIABLES has the same numeric value as
                                        // CHUNKID_GAMEOBJMANAGER — it is a sub-chunk inside the manager
                                        // container that stores nextDynamicId (micro 1).
                                        val varsChunk = combatReader.findChunk(ChunkIds.CHUNKID_GAMEOBJ_VARIABLES)
                                        if (varsChunk != null) {
                                            nextDynamicId = varsChunk.readMicroInt(1) ?: 0
                                        }
                                    }
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
            nextDynamicNetworkId = nextDynamicId,
        )
    }

    private fun parseGameObjects(gameobjReader: ChunkReader, out: MutableList<LoadedGameObject>) {
        // GameObjManager::Save nests the object list inside CHUNKID_GAMEOBJ_OBJECTS (gameobjmanager.cpp)
        val objectsChunk = gameobjReader.findChunk(ChunkIds.CHUNKID_GAMEOBJ_OBJECTS) ?: return
        objectsChunk.forEachChunk { factoryId, _, factoryChunkReader ->
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
    private fun parseScripts(scriptsReader: ChunkReader, out: MutableList<ScriptAttachment>) {
        scriptsReader.forEachChunk { chunkId, _, entryReader ->
            if (chunkId != CHUNKID_SCRIPT_ENTRY) return@forEachChunk
            val headerChunk = entryReader.findChunk(CHUNKID_SCRIPT_HEADER) ?: return@forEachChunk
            val nameBytes = headerChunk.findMicroChunk(MICRO_SCRIPT_NAME) ?: return@forEachChunk
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val name = String(nameBytes, 0, if (nullIdx < 0) nameBytes.size else nullIdx, Charsets.ISO_8859_1)
            if (name.isEmpty()) return@forEachChunk
            val paramBytes = headerChunk.findMicroChunk(MICRO_SCRIPT_PARAM)
            val params = if (paramBytes != null) {
                val pNull = paramBytes.indexOfFirst { it == 0.toByte() }
                String(paramBytes, 0, if (pNull < 0) paramBytes.size else pNull, Charsets.ISO_8859_1)
            } else ""
            out.add(ScriptAttachment(name, params))
        }
    }
}
