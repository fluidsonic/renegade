package ccr.physics.collision

import ccr.math.AABox
import ccr.math.Vector3

class AABoxCollisionTest(
    val box: AABox,
    val move: Vector3,
    val result: CastResult = CastResult(),
) {
    val sweepMin: Vector3 = Vector3(
        minOf(box.min.x, box.min.x + move.x),
        minOf(box.min.y, box.min.y + move.y),
        minOf(box.min.z, box.min.z + move.z),
    )
    val sweepMax: Vector3 = Vector3(
        maxOf(box.max.x, box.max.x + move.x),
        maxOf(box.max.y, box.max.y + move.y),
        maxOf(box.max.z, box.max.z + move.z),
    )

    fun cull(other: AABox): Boolean {
        val sweepBox = AABox.fromMinMax(sweepMin, sweepMax)
        return sweepBox.overlaps(other)
    }
}
