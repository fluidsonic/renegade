package ccr.server

import ccr.server.level.DefinitionRegistry
import ccr.server.level.FullDefinitionLoader
import ccr.server.mix.MixReader
import java.io.File

/**
 * Loads all definitions from always.dbs / always2.dat / always.dat and prints them.
 *
 * Output format per definition:
 *   id=0xXXXXXXXX  type=ClassName  name=DefinitionName
 *
 * Data path: resolved from system property "renegadeDataPath" (default: "data").
 *
 * Run via: ./gradlew :server:dumpDefinitions
 *   or:    ./gradlew :server:dumpDefinitions -PdataPath=/path/to/data
 */
fun main() {
    val dataPath = System.getProperty("renegadeDataPath") ?: "data"
    val dataDir = File(dataPath)

    println("Data path: ${dataDir.absolutePath}")

    // Mirror the same search order used by Network.loadLevel() / loadDefinitions()
    val alwaysMix = listOf("always.dbs", "always2.dat", "always.dat").firstNotNullOfOrNull { fileName ->
        val file = File(dataDir, fileName)
        if (!file.exists()) return@firstNotNullOfOrNull null
        try {
            MixReader(file.readBytes()).also {
                println("Opened $fileName (${it.fileCount()} files in archive)")
            }
        } catch (e: Exception) {
            System.err.println("$fileName: ${e.message}")
            null
        }
    }

    if (alwaysMix == null) {
        System.err.println("No always MIX found under ${dataDir.absolutePath}")
        System.exit(1)
        return
    }

    val ddbData = alwaysMix.readFile("objects.ddb")
    if (ddbData == null) {
        System.err.println("objects.ddb not found inside the always MIX")
        System.exit(1)
        return
    }

    val registry: DefinitionRegistry = FullDefinitionLoader.load(ddbData)

    println("Loaded ${registry.size} definitions\n")

    registry.all()
        .sortedWith(compareBy({ it.id }, { it.name }))
        .forEach { def ->
            val idHex  = def.id.toString(16).padStart(8, '0')
            val type   = def::class.simpleName ?: "DefinitionClass"
            println("id=0x$idHex  type=$type  name=${def.name}")
        }
}
