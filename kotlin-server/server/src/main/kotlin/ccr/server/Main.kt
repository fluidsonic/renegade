package ccr.server

import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
    val configPath = args.firstOrNull() ?: "server.ini"
    val configFile = File(configPath)

    val config = if (configFile.exists()) {
        println("Loading config from: ${configFile.absolutePath}")
        ServerConfig.from(IniParser.parse(configFile.readText()))
    } else {
        println("Config file not found at $configPath, using defaults")
        ServerConfig()
    }

    println("Starting ${config.serverName.ifEmpty { "CCR Server" }} on port ${config.gamePort}")
    println("Max players: ${config.maxPlayers}")
    println("Map: '${config.mapName}' CRC=0x${(config.mapNameCrc.toLong() and 0xFFFFFFFFL).toString(16).uppercase()}  Mod: '${config.modName}' CRC=0x${(config.modNameCrc.toLong() and 0xFFFFFFFFL).toString(16).uppercase()}")
    if (config.rconPassword.isNotEmpty()) println("RCON enabled on port ${config.rconPort}")

    runBlocking {
        Network(config).run()
    }
}
