package ccr.physics.spatial

import ccr.math.AABox
import ccr.math.LineSeg
import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.collision.RayCollisionTest
import kotlin.test.*

private class StaticObj(pos: Vector3) : PhysClass() {
    override var transform: Matrix3D = Matrix3D.IDENTITY.withTranslation(pos)
}

class StaticAABTreeTest {
    @Test
    fun buildAndCollect() {
        val tree = StaticAABTree()
        val objs = (0 until 10).map { StaticObj(Vector3(it.toFloat(), 0f, 0f)) }
        tree.build(objs)
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(3f, 3f, 3f))
        val collected = tree.collectObjects(box)
        assertTrue(collected.isNotEmpty())
    }

    @Test
    fun emptyTree() {
        val tree = StaticAABTree()
        tree.build(emptyList())
        val ray = LineSeg(Vector3(0f, 0f, 0f), Vector3(1f, 0f, 0f))
        val test = RayCollisionTest(ray)
        assertFalse(tree.castRay(test))
    }

    @Test
    fun collectFiltersOutOfRange() {
        val tree = StaticAABTree()
        val objs = (0 until 10).map { StaticObj(Vector3(it.toFloat() * 10f, 0f, 0f)) }
        tree.build(objs)
        // Small box around origin - should only get nearby objects
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(2f, 2f, 2f))
        val collected = tree.collectObjects(box)
        assertTrue(collected.size < objs.size, "Should not collect all objects")
    }

    @Test
    fun largeBoxCollectsAll() {
        val tree = StaticAABTree()
        val objs = (0 until 10).map { StaticObj(Vector3(it.toFloat(), 0f, 0f)) }
        tree.build(objs)
        // Very large box should contain everything
        val box = AABox(Vector3(5f, 0f, 0f), Vector3(100f, 100f, 100f))
        val collected = tree.collectObjects(box)
        assertEquals(objs.size, collected.size)
    }
}
