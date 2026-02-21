package ccr.physics.spatial

import ccr.math.AABox

class AABTreeNode(
    var bounds: AABox,
    var splitAxis: Int = -1,
    var splitDist: Float = 0f,
    var front: AABTreeNode? = null,
    var back: AABTreeNode? = null,
    var objectIndices: IntArray? = null,
) {
    val isLeaf: Boolean get() = objectIndices != null
}
