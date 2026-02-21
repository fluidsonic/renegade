package ccr.physics

import ccr.math.Vector3

object PhysicsConstants {
    val gravityAcceleration = Vector3(0f, 0f, -9.8f)
    const val linearDamping = 0.01f
    const val angularDamping = 0.05f
    const val restingContactVelocity = 0.1f
    const val minFrictionVelocity = 0.05f
    const val defaultContactFriction = 0.5f
    const val defaultContactDrag = 0.0f

    // [DynamicObjType][SurfaceType] friction table
    val frictionTable = Array(DynamicObjType.COUNT) { FloatArray(SurfaceType.COUNT) { defaultContactFriction } }
    val dragTable = Array(DynamicObjType.COUNT) { FloatArray(SurfaceType.COUNT) { defaultContactDrag } }
}
