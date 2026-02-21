package ccr.physics.collision

import ccr.math.Vector3

class CastResult {
    var startBad: Boolean = false
    var fraction: Float = 1.0f
    var normal: Vector3 = Vector3.ZERO
    var surfaceType: Int = 0
    var computeContactPoint: Boolean = false
    var contactPoint: Vector3 = Vector3.ZERO

    fun reset() {
        startBad = false
        fraction = 1.0f
        normal = Vector3.ZERO
        surfaceType = 0
        computeContactPoint = false
        contactPoint = Vector3.ZERO
    }
}
