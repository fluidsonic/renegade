package ccr.physics.collision

import ccr.math.AABox
import ccr.math.LineSeg

class RayCollisionTest(
    val ray: LineSeg,
    val result: CastResult = CastResult(),
    val collisionGroup: Int = CollisionGroup.DEFAULT,
    val collisionType: Int = CollisionType.PHYSICAL,
    val checkStatic: Boolean = true,
    val checkDynamic: Boolean = true,
) {
    fun cull(box: AABox): Boolean {
        val rayBox = AABox.fromLineSeg(ray)
        return rayBox.overlaps(box)
    }
}
