package ccr.server.level.lsd

import ccr.server.level.Vector3

data class StaticSoundEmitter(
    val definitionId: Int = 0,
    val position: Vector3 = Vector3.ZERO,
    val innerRadius: Float = 0f,
    val outerRadius: Float = 40f,
    val volume: Float = 1.0f,
    val loop: Boolean = false,
    val filename: String = "",
)
