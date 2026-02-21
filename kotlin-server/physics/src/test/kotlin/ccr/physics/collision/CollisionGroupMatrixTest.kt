package ccr.physics.collision

import kotlin.test.*

class CollisionGroupMatrixTest {
    @Test
    fun enableAll() {
        val m = CollisionGroupMatrix()
        m.enableAll()
        assertTrue(m.doGroupsCollide(0, 0))
        assertTrue(m.doGroupsCollide(0, 15))
        assertTrue(m.doGroupsCollide(6, 7))
    }

    @Test
    fun disableAll() {
        val m = CollisionGroupMatrix()
        m.enableAll()
        m.disableAll()
        assertFalse(m.doGroupsCollide(0, 0))
        assertFalse(m.doGroupsCollide(15, 15))
    }

    @Test
    fun enableDisablePair() {
        val m = CollisionGroupMatrix()
        m.enableCollision(CollisionGroup.SOLDIER, CollisionGroup.TERRAIN)
        assertTrue(m.doGroupsCollide(CollisionGroup.SOLDIER, CollisionGroup.TERRAIN))
        assertTrue(m.doGroupsCollide(CollisionGroup.TERRAIN, CollisionGroup.SOLDIER)) // symmetric
        assertFalse(m.doGroupsCollide(CollisionGroup.DEFAULT, CollisionGroup.TERRAIN)) // unrelated
        m.disableCollision(CollisionGroup.SOLDIER, CollisionGroup.TERRAIN)
        assertFalse(m.doGroupsCollide(CollisionGroup.SOLDIER, CollisionGroup.TERRAIN))
    }
}
