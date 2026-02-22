package ccr.server.level.ldd

import ccr.server.level.Matrix3D

sealed class LoadedGameObject {
    abstract val definitionId: Int
    abstract val transform: Matrix3D
    abstract val networkId: Int
}

data class LoadedSimpleGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val modelName: String = "",
) : LoadedGameObject()

data class LoadedSoldierGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val playerName: String = "",
) : LoadedGameObject()

data class LoadedVehicleGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
) : LoadedGameObject()

data class LoadedBuildingGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val teamId: Int = 0,
    val factoryChunkId: UInt = 0u,
    val isPowerOn: Boolean = true,
    val sphereCenter: ccr.server.level.Vector3 = ccr.server.level.Vector3(0f, 0f, 0f),
    val sphereRadius: Float = 10f,
) : LoadedGameObject()

data class LoadedScriptZoneGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val scripts: List<ScriptAttachment> = emptyList(),
) : LoadedGameObject()

data class UnknownGameObj(
    override val definitionId: Int,
    override val transform: Matrix3D,
    override val networkId: Int,
    val factoryChunkId: UInt,
) : LoadedGameObject()
