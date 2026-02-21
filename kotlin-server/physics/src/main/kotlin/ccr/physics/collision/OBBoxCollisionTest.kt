package ccr.physics.collision

import ccr.math.AABox
import ccr.math.OBBox
import ccr.math.Vector3

class OBBoxCollisionTest(
    val box: OBBox,
    val move: Vector3,
    val result: CastResult = CastResult(),
) {
    val sweepAABox: AABox = run {
        val aaExtent = box.computeAxisAlignedExtent()
        val aaBox = AABox(box.center, aaExtent)
        AABox.fromMinMax(
            Vector3(
                minOf(aaBox.min.x, aaBox.min.x + move.x),
                minOf(aaBox.min.y, aaBox.min.y + move.y),
                minOf(aaBox.min.z, aaBox.min.z + move.z),
            ),
            Vector3(
                maxOf(aaBox.max.x, aaBox.max.x + move.x),
                maxOf(aaBox.max.y, aaBox.max.y + move.y),
                maxOf(aaBox.max.z, aaBox.max.z + move.z),
            ),
        )
    }

    fun cull(other: AABox): Boolean = sweepAABox.overlaps(other)
}
