package ccr.server.level.ldd

import ccr.server.defs.readMicroInt
import ccr.server.level.ChunkIds
import ccr.server.level.DefinitionRegistry
import ccr.server.level.LevelDynamicData
import ccr.server.level.Matrix3D
import ccr.server.mix.ChunkReader
import ccr.server.net.BaseGameObj
import java.nio.ByteBuffer
import java.nio.ByteOrder

object LddParser {

    // ScriptManager chunk IDs (scripts.cpp)
    private const val CHUNKID_SCRIPT_ENTRY = 131001134u
    private const val CHUNKID_SCRIPT_HEADER = 131001135u  // CHUNKID_SCRIPT_ENTRY + 1
    private const val MICRO_SCRIPT_NAME = 1               // MICROCHUNKID_NAME
    private const val MICRO_SCRIPT_PARAM = 2              // MICROCHUNKID_PARAM

    // Micro-chunk IDs within PHYS_CHUNK_VARIABLES (phys.cpp)
    private const val PHYS_VARIABLE_CULLABLE_PTR = 0x00  // the correlation key

    // Micro-chunk ID within RENDOBJFACTORY_CHUNKID_VARIABLES (renderobj.cpp)
    private const val RENDOBJFACTORY_VARIABLE_TRANSFORM = 0x02

    fun parse(lddData: ByteArray, definitions: DefinitionRegistry = DefinitionRegistry()): LevelDynamicData {
        if (lddData.isEmpty()) return LevelDynamicData()

        val reader = ChunkReader(lddData)
        val factory = GameObjectFactory(definitions)

        var levelInfo: LevelInfo? = null
        val gameObjects = mutableListOf<BaseGameObj>()
        val spawners = mutableListOf<LoadedSpawner>()
        val scripts = mutableListOf<ScriptAttachment>()
        var nextDynamicId = 0
        // physTransformMap: maps raw pointer (4-byte key saved in PhysClass) to Matrix3D transform
        // Built from PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM; populated before game objects are
        // processed (two-pass: physics map first, then game objects use it via factory).
        var physTransformMap: Map<Int, Matrix3D> = emptyMap()

        reader.forEachChunk { chunkId, isContainer, chunkReader ->
            when (chunkId) {
                ChunkIds.CHUNKID_LEVEL_INFO -> {
                    levelInfo = LevelInfoLoader.load(chunkReader)
                }
                ChunkIds.CHUNKID_LEVEL_DATA -> {
                    // First pass: extract the physics transform map so game objects can look up
                    // their positions when they are parsed in the second pass below.
                    val physSubsystemReader = chunkReader.findChunk(ChunkIds.PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM)
                    if (physSubsystemReader != null) {
                        physTransformMap = buildPhysTransformMap(physSubsystemReader)
                        println("[LDD] physTransformMap: ${physTransformMap.size} dynamic objects loaded")
                    }

                    factory.physTransformMap = physTransformMap

                    chunkReader.forEachChunk { subId, _, subReader ->
                        // Game objects/spawners/scripts are nested inside CHUNKID_COMBAT_BEGIN
                        // (written by SaveLoadSystemClass for the combat subsystem)
                        if (subId == ChunkIds.CHUNKID_COMBAT_BEGIN) {
                            subReader.forEachChunk { combatId, _, combatReader ->
                                when (combatId) {
                                    ChunkIds.CHUNKID_GAMEOBJMANAGER -> {
                                        parseGameObjects(combatReader, gameObjects, factory)
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

    /**
     * Parses the PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM chunk and builds a map from the
     * raw 4-byte pointer (PHYS_VARIABLE_CULLABLE_PTR, the correlation key between a PhysClass
     * instance and the physObjPtr stored in PhysicalGameObj::Save) to the Matrix3D transform
     * read from the render object factory variables.
     *
     * All dynamic physics types (DecorationPhysClass, TimedDecorationPhysClass,
     * RenderObjPhysClass, etc.) ultimately call PhysClass::Save which writes
     * PHYS_CHUNK_VARIABLES and PHYS_CHUNK_MODEL at some depth in their chunk hierarchy.
     * We use findChunkRecursive to locate these regardless of the intermediate wrapping
     * chunks (e.g. DECOPHYS_CHUNK_DYNAMICPHYS, TIMEDDECOPHYS_CHUNK_DECOPHYS, etc.).
     *
     * Chunk hierarchy (C++ sources: physdynamicsavesystem.cpp, pscene_saveload.cpp,
     * phys.cpp, renderobj.cpp):
     *
     *   PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM
     *     PDSSC_CHUNKID_SCENE (0x00007001)
     *       PSCENE_DD_CHUNK_DYNAMIC_OBJECTS (0x00890100)
     *         PSCENE_DD_CHUNK_DYNAMIC_OBJECT (0x00890101)  [repeated]
     *           [any physics type chunk ID]
     *             ... (type-specific intermediate chunks) ...
     *               PHYS_CHUNK_VARIABLES (0x00660055)      <- found via findChunkRecursive
     *                 micro 0x00 = 4-byte raw ptr (cullable_ptr correlation key)
     *               PHYS_CHUNK_MODEL (0x00660056)          <- found via findChunkRecursive
     *                 WW3D_PERSIST_CHUNKID_RENDEROBJ (0x00010000)
     *                   RENDOBJFACTORY_CHUNKID_VARIABLES (0x00555040)
     *                     micro 0x02 = 48-byte Matrix3D transform (row-major)
     */
    private fun buildPhysTransformMap(physSubsystemReader: ChunkReader): Map<Int, Matrix3D> {
        val result = mutableMapOf<Int, Matrix3D>()

        val sceneReader = physSubsystemReader.findChunk(ChunkIds.PDSSC_CHUNKID_SCENE) ?: return result
        val dynObjsReader = sceneReader.findChunk(ChunkIds.PSCENE_DD_CHUNK_DYNAMIC_OBJECTS) ?: return result

        dynObjsReader.forEachChunk { chunkId, _, dynObjReader ->
            if (chunkId != ChunkIds.PSCENE_DD_CHUNK_DYNAMIC_OBJECT) return@forEachChunk

            // All dynamic physics types eventually call PhysClass::Save which writes
            // PHYS_CHUNK_VARIABLES and PHYS_CHUNK_MODEL somewhere in their hierarchy.
            // Use findChunkRecursive to find them regardless of the intermediate chunks
            // (e.g. PHYSICS_CHUNKID_DECORATIONPHYS → DECOPHYS_CHUNK_DYNAMICPHYS →
            //  DYNAMICPHYS_CHUNK_PHYS → PHYS_CHUNK_VARIABLES).

            // Read PHYS_VARIABLE_CULLABLE_PTR (micro 0x00) — 4-byte raw pointer (correlation key)
            val physVarsReader = dynObjReader.findChunkRecursive(ChunkIds.PHYS_CHUNK_VARIABLES) ?: return@forEachChunk
            val cullablePtrBytes = physVarsReader.findMicroChunk(PHYS_VARIABLE_CULLABLE_PTR) ?: return@forEachChunk
            if (cullablePtrBytes.size < 4) return@forEachChunk
            val cullablePtr = ByteBuffer.wrap(cullablePtrBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int

            // Read transform: PHYS_CHUNK_MODEL → WW3D_PERSIST_CHUNKID_RENDEROBJ →
            //                 RENDOBJFACTORY_CHUNKID_VARIABLES → micro 0x02
            val physModelReader = dynObjReader.findChunkRecursive(ChunkIds.PHYS_CHUNK_MODEL) ?: return@forEachChunk
            val rendObjReader = physModelReader.findChunk(ChunkIds.WW3D_PERSIST_CHUNKID_RENDEROBJ) ?: return@forEachChunk
            val rendVarsReader = rendObjReader.findChunk(ChunkIds.RENDOBJFACTORY_CHUNKID_VARIABLES) ?: return@forEachChunk
            val transformBytes = rendVarsReader.findMicroChunk(RENDOBJFACTORY_VARIABLE_TRANSFORM) ?: return@forEachChunk
            if (transformBytes.size < 48) return@forEachChunk

            val bb = ByteBuffer.wrap(transformBytes, 0, 48).order(ByteOrder.LITTLE_ENDIAN)
            val elems = FloatArray(12) { bb.float }
            val transform = Matrix3D(elems)

            result[cullablePtr] = transform
        }

        println("[PHYS-MAP] entries=${result.size} keys=${result.keys.take(5).map { "0x${it.toLong().and(0xFFFFFFFFL).toString(16)}" }}")
        return result
    }

    private fun parseGameObjects(gameobjReader: ChunkReader, out: MutableList<BaseGameObj>, factory: GameObjectFactory) {
        // GameObjManager::Save nests the object list inside CHUNKID_GAMEOBJ_OBJECTS (gameobjmanager.cpp)
        val objectsChunk = gameobjReader.findChunk(ChunkIds.CHUNKID_GAMEOBJ_OBJECTS) ?: return
        val countBefore = out.size
        objectsChunk.forEachChunk { factoryId, _, factoryChunkReader ->
            val objData = factoryChunkReader.findChunk(ChunkIds.SIMPLEFACTORY_CHUNKID_OBJDATA)
                ?: return@forEachChunk
            try {
                val obj = factory.load(factoryId, objData)
                if (obj != null) out.add(obj)
            } catch (e: Exception) {
                System.err.println("[WARN] Failed to parse game object (factory=0x${factoryId.toString(16)}): ${e.message}")
            }
        }
        val simpleCount = out.drop(countBefore).count { it is ccr.server.net.SimpleGameObj }
        println("[LDD] parseGameObjects complete: total loaded=${out.size - countBefore} SimpleGameObj=$simpleCount")
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
