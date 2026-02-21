package ccr.physics.moveable

import ccr.math.Vector3

data class GroundState(
    val onGround: Boolean,
    val surfaceType: Int,
    val height: Float,
    val normal: Vector3,
    val downSlope: Boolean,
) {
    companion object {
        val NOT_ON_GROUND = GroundState(false, 0, 0f, Vector3(0f, 0f, 1f), false)
    }
}
