package ccr.physics.scene

import ccr.math.AABox
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.CollisionGroup
import ccr.physics.collision.CollisionGroupMatrix
import ccr.physics.collision.RayCollisionTest
import ccr.physics.spatial.PhysGrid
import ccr.physics.spatial.StaticAABTree

class PhysicsScene {
    val staticTree = StaticAABTree()
    val dynamicGrid = PhysGrid()
    val collisionMatrix = CollisionGroupMatrix()
    val staticObjects = mutableListOf<PhysClass>()
    val dynamicObjects = mutableListOf<PhysClass>()

    companion object {
        const val MAX_TIMESTEP = 1f / 15f
    }

    fun addStaticObject(obj: PhysClass) {
        staticObjects.add(obj)
        staticTree.build(staticObjects)
    }

    fun addDynamicObject(obj: PhysClass) {
        obj.scene = this
        dynamicObjects.add(obj)
        dynamicGrid.addObject(obj)
    }

    fun removeObject(obj: PhysClass) {
        if (staticObjects.remove(obj)) {
            staticTree.build(staticObjects)
        } else if (dynamicObjects.remove(obj)) {
            dynamicGrid.removeObject(obj)
        }
        obj.observer?.objectRemovedFromScene()
    }

    fun update(dt: Float) {
        var remaining = dt
        while (remaining > 0f) {
            val step = minOf(remaining, MAX_TIMESTEP)
            for (obj in staticObjects) if (obj.needsTimestep()) obj.timestep(step)
            for (obj in dynamicObjects) if (obj.needsTimestep()) obj.timestep(step)
            for (obj in staticObjects) obj.postTimestepProcess()
            for (obj in dynamicObjects) obj.postTimestepProcess()
            remaining -= step
        }
    }

    fun castRay(test: RayCollisionTest): Boolean {
        var hit = staticTree.castRay(test)
        if (dynamicGrid.castRay(test)) hit = true
        return hit
    }

    fun castAABox(test: AABoxCollisionTest): Boolean {
        var hit = staticTree.castAABox(test)
        if (dynamicGrid.castAABox(test)) hit = true
        return hit
    }

    fun collectObjects(box: AABox): List<PhysClass> {
        val result = mutableListOf<PhysClass>()
        result.addAll(staticTree.collectObjects(box))
        result.addAll(dynamicGrid.collectObjects(box))
        return result
    }

    fun initDefaultCollisionGroups() {
        collisionMatrix.enableAll()
        // UNCOLLIDEABLE doesn't collide with anything
        for (i in 0..15) {
            collisionMatrix.disableCollision(CollisionGroup.UNCOLLIDEABLE, i)
        }
        // SOLDIER_GHOST doesn't collide with SOLDIER or BULLET
        collisionMatrix.disableCollision(CollisionGroup.SOLDIER_GHOST, CollisionGroup.SOLDIER)
        collisionMatrix.disableCollision(CollisionGroup.SOLDIER_GHOST, CollisionGroup.BULLET)
    }
}
