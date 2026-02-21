package ccr.physics

import ccr.physics.collision.CollisionEvent
import ccr.physics.collision.CollisionReactionType

interface PhysObserver {
    fun collisionOccurred(event: CollisionEvent): CollisionReactionType = CollisionReactionType.DEFAULT
    fun objectExpired() {}
    fun objectRemovedFromScene() {}
}
