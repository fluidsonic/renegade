package ccr.server.level

import ccr.server.level.lsd.*
import ccr.server.level.pathfind.PathfindData
import ccr.server.mix.WorldExtents

data class LevelStaticData(
    val worldExtents: WorldExtents? = null,
    val sunlightColor: Vector3? = null,
    val ambientLightColor: Vector3? = null,
    val staticObjects: List<StaticPhysObject> = emptyList(),
    val staticLights: List<StaticLight> = emptyList(),
    val pathfinding: PathfindData? = null,
    val background: BackgroundData? = null,
    val weather: WeatherData? = null,
    val staticSounds: List<StaticSoundEmitter> = emptyList(),
)
