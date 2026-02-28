package ccr.physics.spatial

import ccr.math.AABox
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.RayCollisionTest

class StaticAABTree {
    private var objects: List<PhysClass> = emptyList()
    private var root: AABTreeNode? = null

    /** Returns the bounding box of the root node, or null if the tree is empty. */
    fun rootBounds(): AABox? = root?.bounds

    /** Returns the total number of nodes in the tree (for diagnostics). */
    fun nodeCount(): Int {
        val r = root ?: return 0
        return countNodes(r)
    }

    private fun countNodes(node: AABTreeNode): Int {
        var count = 1
        node.front?.let { count += countNodes(it) }
        node.back?.let { count += countNodes(it) }
        return count
    }

    fun build(objects: List<PhysClass>) {
        this.objects = objects
        if (objects.isEmpty()) {
            root = null
            return
        }
        root = buildNode(objects.indices.toMutableList())
    }

    private fun computeBounds(indices: List<Int>): AABox {
        if (indices.isEmpty()) return AABox(Vector3.ZERO, Vector3.ZERO)
        val boxes = indices.map { objects[it].worldBoundingBox() }
        val mn = Vector3(boxes.minOf { it.min.x }, boxes.minOf { it.min.y }, boxes.minOf { it.min.z })
        val mx = Vector3(boxes.maxOf { it.max.x }, boxes.maxOf { it.max.y }, boxes.maxOf { it.max.z })
        return AABox.fromMinMax(mn, mx)
    }

    private fun buildNode(indices: MutableList<Int>): AABTreeNode {
        val bounds = computeBounds(indices)
        if (indices.size <= 4) {
            return AABTreeNode(bounds, objectIndices = indices.toIntArray())
        }
        val ext = bounds.extent
        val axis = when {
            ext.x >= ext.y && ext.x >= ext.z -> 0
            ext.y >= ext.z -> 1
            else -> 2
        }
        val sorted = indices.sortedBy { idx ->
            val c = objects[idx].worldBoundingBox().center
            when (axis) { 0 -> c.x; 1 -> c.y; else -> c.z }
        }
        val mid = sorted.size / 2
        // Guard: if the partition doesn't actually split the set (one side is empty),
        // fall back to a leaf — mirrors C++ Build_Tree's FrontCount+BackCount != polycount check.
        if (mid == 0 || mid == sorted.size) {
            return AABTreeNode(bounds, objectIndices = sorted.toIntArray())
        }
        val splitPos = sorted[mid].let { idx ->
            val c = objects[idx].worldBoundingBox().center
            when (axis) { 0 -> c.x; 1 -> c.y; else -> c.z }
        }
        val node = AABTreeNode(bounds, splitAxis = axis, splitDist = splitPos)
        node.front = buildNode(sorted.subList(0, mid).toMutableList())
        node.back = buildNode(sorted.subList(mid, sorted.size).toMutableList())
        return node
    }

    fun castRay(test: RayCollisionTest): Boolean {
        val r = root ?: return false
        return castRayNode(r, test)
    }

    private fun castRayNode(node: AABTreeNode, test: RayCollisionTest): Boolean {
        if (!test.cull(node.bounds)) return false
        if (node.isLeaf) {
            var hit = false
            for (i in node.objectIndices!!) {
                if (objects[i].castRay(test)) hit = true
            }
            return hit
        }
        var hit = false
        node.front?.let { if (castRayNode(it, test)) hit = true }
        node.back?.let { if (castRayNode(it, test)) hit = true }
        return hit
    }

    fun castAABox(test: AABoxCollisionTest): Boolean {
        val r = root ?: return false
        return castAABoxNode(r, test)
    }

    private fun castAABoxNode(node: AABTreeNode, test: AABoxCollisionTest): Boolean {
        if (!test.cull(node.bounds)) return false
        if (node.isLeaf) {
            var hit = false
            for (i in node.objectIndices!!) {
                if (objects[i].castAABox(test)) hit = true
            }
            return hit
        }
        var hit = false
        node.front?.let { if (castAABoxNode(it, test)) hit = true }
        node.back?.let { if (castAABoxNode(it, test)) hit = true }
        return hit
    }

    fun collectObjects(box: AABox): List<PhysClass> {
        val r = root ?: return emptyList()
        val result = mutableListOf<PhysClass>()
        collectNode(r, box, result)
        return result
    }

    private fun collectNode(node: AABTreeNode, box: AABox, result: MutableList<PhysClass>) {
        if (!node.bounds.overlaps(box)) return
        if (node.isLeaf) {
            for (i in node.objectIndices!!) {
                result.add(objects[i])
            }
            return
        }
        node.front?.let { collectNode(it, box, result) }
        node.back?.let { collectNode(it, box, result) }
    }
}
