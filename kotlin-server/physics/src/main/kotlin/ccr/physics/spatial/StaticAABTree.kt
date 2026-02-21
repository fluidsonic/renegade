package ccr.physics.spatial

import ccr.math.AABox
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.RayCollisionTest

class StaticAABTree {
    private var objects: List<PhysClass> = emptyList()
    private var root: AABTreeNode? = null

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
        val positions = indices.map { objects[it].transform.translation }
        val mn = Vector3(positions.minOf { it.x }, positions.minOf { it.y }, positions.minOf { it.z })
        val mx = Vector3(positions.maxOf { it.x }, positions.maxOf { it.y }, positions.maxOf { it.z })
        val pad = Vector3(1f, 1f, 1f)
        return AABox.fromMinMax(mn - pad, mx + pad)
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
            val t = objects[idx].transform.translation
            when (axis) { 0 -> t.x; 1 -> t.y; else -> t.z }
        }
        val mid = sorted.size / 2
        val splitPos = sorted[mid].let { idx ->
            val t = objects[idx].transform.translation
            when (axis) { 0 -> t.x; 1 -> t.y; else -> t.z }
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
