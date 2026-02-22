package ccr.server.level

import ccr.server.level.ldd.LddParser
import ccr.server.level.lsd.LsdParser
import ccr.server.mix.MixReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class LevelLoader(
    private val alwaysMix: MixReader?,
    private val mapMix: MixReader,
    private val baseName: String,   // e.g. "C&C_Field" (no extension)
) {
    suspend fun load(): LoadedLevel = withContext(Dispatchers.Default) {
        val context = LoadingContext()

        // Phase 1: Load definitions (sequential — registry is not thread-safe to mutate)
        val registry = DefinitionRegistry()

        // MixReader.readFile uses crcStringi (case-insensitive), so no case fallback needed.
        val alwaysDdbData = alwaysMix?.readFile("objects.ddb")
        if (alwaysDdbData != null) {
            val alwaysRegistry = FullDefinitionLoader.load(alwaysDdbData)
            for (def in alwaysRegistry.all()) registry.register(def)
            context.info("Loaded ${alwaysRegistry.size} definitions from always.dbs")
        } else {
            context.warn("Objects.ddb not found in always mix")
        }

        val mapDdbData = mapMix.readFile("$baseName.ddb")
        if (mapDdbData != null) {
            val before = registry.size
            val mapRegistry = FullDefinitionLoader.load(mapDdbData)
            for (def in mapRegistry.all()) registry.register(def)
            context.warn("Loaded ${registry.size - before} additional definitions from map DDB")
        }

        // Phase 2+3: Parse LSD and LDD in parallel (each produces immutable results)
        val lsdData = mapMix.readFile("$baseName.lsd")
        val lddData = mapMix.readFile("$baseName.ldd")

        if (lsdData == null) context.warn("$baseName.lsd not found in map mix")
        if (lddData == null) context.warn("$baseName.ldd not found in map mix")

        val staticDeferred  = async { if (lsdData != null) LsdParser.parse(lsdData)  else LevelStaticData() }
        val dynamicDeferred = async { if (lddData != null) LddParser.parse(lddData) else LevelDynamicData() }

        val staticData  = staticDeferred.await()
        val dynamicData = dynamicDeferred.await()

        println("[LEVEL] ${context.summary()} loading $baseName")

        // Phase 4: Assemble
        LoadedLevel(
            mapFilename          = dynamicData.mapFilename.ifEmpty { baseName },
            missionDescriptionId = dynamicData.missionDescriptionId,
            description          = dynamicData.description,
            definitions          = registry,
            worldExtents         = staticData.worldExtents,
            staticData           = staticData,
            dynamicData          = dynamicData,
        )
    }
}
