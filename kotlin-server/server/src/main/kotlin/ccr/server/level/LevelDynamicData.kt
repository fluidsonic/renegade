package ccr.server.level

import ccr.server.level.ldd.LoadedSpawner
import ccr.server.level.ldd.ScriptAttachment
import ccr.server.net.BaseGameObj

data class LevelDynamicData(
    val mapFilename: String = "",
    val missionDescriptionId: Int = 0,
    val description: String = "",
    val gameObjects: List<BaseGameObj> = emptyList(),
    val spawners: List<LoadedSpawner> = emptyList(),
    val levelScripts: List<ScriptAttachment> = emptyList(),
    val nextDynamicNetworkId: Int = 0,
)
