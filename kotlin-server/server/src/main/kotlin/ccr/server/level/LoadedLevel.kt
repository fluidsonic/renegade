package ccr.server.level

import ccr.server.mix.WorldExtents

data class LoadedLevel(
    val mapFilename: String,
    val missionDescriptionId: Int,
    val description: String,
    val definitions: DefinitionRegistry,
    val worldExtents: WorldExtents?,
    val staticData: LevelStaticData,
    val dynamicData: LevelDynamicData,
)
