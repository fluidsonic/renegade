package ccr.server.level.ldd

import ccr.server.level.Matrix3D

data class ScriptAttachment(val name: String, val params: String)

data class LoadedSpawner(
    val id: Int,
    val transform: Matrix3D,
    val spawnTransform: Matrix3D?,
    val definitionId: Int,
    val spawnCount: Int,
    val enabled: Boolean,
    val spawnPoints: List<Matrix3D>,
    val scripts: List<ScriptAttachment>,
    val spawnDelayTimer: Float = 0f,
)
