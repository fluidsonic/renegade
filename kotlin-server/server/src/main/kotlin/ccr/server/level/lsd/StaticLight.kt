package ccr.server.level.lsd

import ccr.server.level.Vector3

data class StaticLight(
    val position: Vector3,
    val color: Vector3,
    val innerRadius: Float,
    val outerRadius: Float,
    val multiplier: Float,
)
