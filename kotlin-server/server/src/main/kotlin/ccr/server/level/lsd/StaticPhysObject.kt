package ccr.server.level.lsd

import ccr.server.level.Matrix3D

data class StaticPhysObject(
    val factoryChunkId: UInt,
    val instanceId: Int,
    val definitionId: Int,
    val transform: Matrix3D,
    val modelName: String,
)
