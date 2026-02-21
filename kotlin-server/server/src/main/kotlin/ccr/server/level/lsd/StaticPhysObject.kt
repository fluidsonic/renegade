package ccr.server.level.lsd

import ccr.server.level.Matrix3D

data class StaticPhysObject(
    val definitionId: Int,
    val transform: Matrix3D,
    val modelName: String,
)
