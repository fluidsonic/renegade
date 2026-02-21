package ccr.physics.spatial

import ccr.math.AABox
import ccr.math.LineSeg
import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.collision.RayCollisionTest
import kotlin.test.*

private class SimplePhysObj(pos: Vector3) : PhysClass() {
    override var transform: Matrix3D = Matrix3D.IDENTITY.withTranslation(pos)
    var rayCastCalled = false
    override fun castRay(test: RayCollisionTest): Boolean {
        rayCastCalled = true
        return false
    }
}

class PhysGridTest {
    @Test
    fun addAndCollect() {
        val grid = PhysGrid()
        grid.rePartition(Vector3(-50f, -50f, -50f), Vector3(50f, 50f, 50f), 10f)
        val obj = SimplePhysObj(Vector3(0f, 0f, 0f))
        grid.addObject(obj)
        val collected = grid.collectObjects(AABox(Vector3(0f, 0f, 0f), Vector3(5f, 5f, 5f)))
        assertTrue(obj in collected)
    }

    @Test
    fun removeObject() {
        val grid = PhysGrid()
        grid.rePartition(Vector3(-50f, -50f, -50f), Vector3(50f, 50f, 50f), 10f)
        val obj = SimplePhysObj(Vector3(0f, 0f, 0f))
        grid.addObject(obj)
        grid.removeObject(obj)
        val collected = grid.collectObjects(AABox(Vector3(0f, 0f, 0f), Vector3(5f, 5f, 5f)))
        assertFalse(obj in collected)
    }

    @Test
    fun updateObjectMovesToNewCell() {
        val grid = PhysGrid()
        grid.rePartition(Vector3(-50f, -50f, -50f), Vector3(50f, 50f, 50f), 10f)
        val obj = SimplePhysObj(Vector3(0f, 0f, 0f))
        grid.addObject(obj)

        // Move object to a far-away cell
        obj.transform = Matrix3D.IDENTITY.withTranslation(Vector3(40f, 40f, 40f))
        grid.updateObject(obj)

        // Should not be found at old location
        val oldArea = grid.collectObjects(AABox(Vector3(0f, 0f, 0f), Vector3(2f, 2f, 2f)))
        assertFalse(obj in oldArea)

        // Should be found at new location
        val newArea = grid.collectObjects(AABox(Vector3(40f, 40f, 40f), Vector3(5f, 5f, 5f)))
        assertTrue(obj in newArea)
    }

    @Test
    fun castRayCallsObjectCastRay() {
        val grid = PhysGrid()
        grid.rePartition(Vector3(-50f, -50f, -50f), Vector3(50f, 50f, 50f), 10f)
        val obj = SimplePhysObj(Vector3(5f, 0f, 0f))
        grid.addObject(obj)

        val test = RayCollisionTest(LineSeg(Vector3(0f, 0f, 0f), Vector3(10f, 0f, 0f)))
        grid.castRay(test)
        assertTrue(obj.rayCastCalled)
    }
}
