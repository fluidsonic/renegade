package ccr.server.level

import ccr.server.level.ldd.LoadedGameObject
import ccr.server.level.ldd.LoadedSpawner

data class LevelDynamicData(
    val mapFilename: String = "",
    val missionDescriptionId: Int = 0,
    val description: String = "",
    val gameObjects: List<LoadedGameObject> = emptyList(),
    val spawners: List<LoadedSpawner> = emptyList(),
    val levelScripts: List<String> = emptyList(),
)
