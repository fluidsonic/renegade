package ccr.server

import ccr.server.defs.readDefinitions
import ccr.server.mix.ChunkReader
import ccr.server.mix.MixReader
import java.io.File

/**
 * Reads Objects.DDB from always.dat and prints the actual IDs of soldier definitions,
 * verifying them against the hardcoded defaults in ServerConfig.kt.
 *
 * Run via: ./gradlew :server:verifyDefIds
 */

// Hardcoded defaults from ServerConfig.kt
private const val NOD_SOLDIER_DEF_ID_DEFAULT = 81930257u  // 0x04e22811
private const val GDI_SOLDIER_DEF_ID_DEFAULT = 81930243u  // 0x04e22803

fun main() {
    val renegadeData = File(
        System.getProperty("renegadeDataPath") ?: "data"
    )
    println("Game data: $renegadeData")

    val alwaysDat = MixReader(File(renegadeData, "always.dat").readBytes())
    println("always.dat: ${alwaysDat.fileCount()} files in MIX")

    // Also check always.dbs which may contain game definition data
    val alwaysDbs = File(renegadeData, "always.dbs").takeIf { it.exists() }
        ?.let { MixReader(it.readBytes()) }
    if (alwaysDbs != null) println("always.dbs: ${alwaysDbs.fileCount()} files in MIX")

    val ddbData = alwaysDat.readFile("Objects.DDB")
        ?: alwaysDat.readFile("objects.ddb")
        ?: alwaysDbs?.readFile("Objects.DDB")
        ?: alwaysDbs?.readFile("objects.ddb")
        ?: error("Objects.DDB not found in always.dat or always.dbs")
    println("Objects.DDB: ${ddbData.size} bytes")



    val definitions = readDefinitions(ddbData)
    println("Total definitions found: ${definitions.size}")
    println()

    // Look up specific soldier definitions
    val targets = listOf(
        "CnC_Nod_Minigunner_0" to NOD_SOLDIER_DEF_ID_DEFAULT,
        "CnC_GDI_MiniGunner_0" to GDI_SOLDIER_DEF_ID_DEFAULT,
    )

    println("=== Soldier definition lookup ===")
    for ((name, defaultId) in targets) {
        val entry = definitions.find { it.name == name }
        if (entry == null) {
            println("NOT FOUND: $name")
        } else {
            val idHex = entry.id.toString(16).padStart(8, '0')
            val classHex = entry.classId.toString(16).padStart(8, '0')
            println("${entry.name} = ${entry.id} (hex: 0x$idHex) classId=0x$classHex")
            val matches = entry.id == defaultId
            val defaultHex = defaultId.toString(16).padStart(8, '0')
            println("  Matches ServerConfig default 0x$defaultHex (${defaultId}): $matches")
        }
    }

    // Discovery: all definitions whose name contains "Minigunner" (case-insensitive)
    println()
    println("=== All definitions containing \"Minigunner\" ===")
    val minigunners = definitions.filter { it.name.contains("Minigunner", ignoreCase = true) }
    if (minigunners.isEmpty()) {
        println("(none found)")
    } else {
        for (entry in minigunners) {
            val idHex = entry.id.toString(16).padStart(8, '0')
            val classHex = entry.classId.toString(16).padStart(8, '0')
            println("  ${entry.name} = ${entry.id} (hex: 0x$idHex) classId=0x$classHex")
        }
    }
}
