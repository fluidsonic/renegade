package ccr.physics

import ccr.math.Vector3

class PhysController {
    var moveForward: Float = 0f
    var moveLeft: Float = 0f
    var moveUp: Float = 0f
    var turnLeft: Float = 0f

    val moveVector: Vector3 get() = Vector3(moveLeft, moveForward, moveUp)
    val isInactive: Boolean get() = moveForward == 0f && moveLeft == 0f && moveUp == 0f && turnLeft == 0f

    fun reset() {
        moveForward = 0f
        moveLeft = 0f
        moveUp = 0f
        turnLeft = 0f
    }
}
