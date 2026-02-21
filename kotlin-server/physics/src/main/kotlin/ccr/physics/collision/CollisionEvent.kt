package ccr.physics.collision

import ccr.math.Vector3

data class CollisionEvent(
    val collidedObj: Any?,
    val point: Vector3,
    val normal: Vector3,
    val surfaceType: Int,
    val fraction: Float,
)
