package ccr.physics.scene

import ccr.math.Matrix3D
import ccr.physics.PhysClass
import ccr.physics.collision.CollisionGroup
import kotlin.test.*

private class TimestepCountObj : PhysClass() {
    override var transform: Matrix3D = Matrix3D.IDENTITY
    var timestepCount = 0
    override fun needsTimestep() = true
    override fun timestep(dt: Float) {
        timestepCount++
    }
}

class PhysicsSceneTest {
    @Test
    fun addAndRemoveStatic() {
        val scene = PhysicsScene()
        val obj = TimestepCountObj()
        scene.addStaticObject(obj)
        assertTrue(obj in scene.staticObjects)
        scene.removeObject(obj)
        assertFalse(obj in scene.staticObjects)
    }

    @Test
    fun addAndRemoveDynamic() {
        val scene = PhysicsScene()
        val obj = TimestepCountObj()
        scene.addDynamicObject(obj)
        assertTrue(obj in scene.dynamicObjects)
        scene.removeObject(obj)
        assertFalse(obj in scene.dynamicObjects)
    }

    @Test
    fun updateCallsTimestepCorrectly() {
        val scene = PhysicsScene()
        val obj = TimestepCountObj()
        scene.addDynamicObject(obj)
        // 1/30 second = 1 sub-step (< MAX_TIMESTEP=1/15)
        scene.update(1f / 30f)
        assertEquals(1, obj.timestepCount)
    }

    @Test
    fun updateSubstepsLargeDt() {
        val scene = PhysicsScene()
        val obj = TimestepCountObj()
        scene.addDynamicObject(obj)
        // 1 second should require many sub-steps (at least 15)
        scene.update(1f)
        assertTrue(obj.timestepCount >= 15, "Expected at least 15 sub-steps, got ${obj.timestepCount}")
    }

    @Test
    fun removeNotifiesObserver() {
        val scene = PhysicsScene()
        val obj = TimestepCountObj()
        var notified = false
        obj.observer = object : ccr.physics.PhysObserver {
            override fun objectRemovedFromScene() {
                notified = true
            }
        }
        scene.addStaticObject(obj)
        scene.removeObject(obj)
        assertTrue(notified)
    }

    @Test
    fun initDefaultCollisionGroups() {
        val scene = PhysicsScene()
        scene.initDefaultCollisionGroups()
        // UNCOLLIDEABLE should not collide with anything
        assertFalse(scene.collisionMatrix.doGroupsCollide(CollisionGroup.UNCOLLIDEABLE, CollisionGroup.DEFAULT))
        assertFalse(scene.collisionMatrix.doGroupsCollide(CollisionGroup.UNCOLLIDEABLE, CollisionGroup.SOLDIER))
        // DEFAULT should collide with DEFAULT
        assertTrue(scene.collisionMatrix.doGroupsCollide(CollisionGroup.DEFAULT, CollisionGroup.DEFAULT))
        // SOLDIER_GHOST should not collide with SOLDIER or BULLET
        assertFalse(scene.collisionMatrix.doGroupsCollide(CollisionGroup.SOLDIER_GHOST, CollisionGroup.SOLDIER))
        assertFalse(scene.collisionMatrix.doGroupsCollide(CollisionGroup.SOLDIER_GHOST, CollisionGroup.BULLET))
    }
}
