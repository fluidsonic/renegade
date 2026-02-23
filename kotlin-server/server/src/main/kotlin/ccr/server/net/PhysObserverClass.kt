package ccr.server.net

// C++: PhysObserverClass (wwphys/physobserver.h)
// Observer interface for a physics object — notified on collision, expiration, removal, shatter.
interface PhysObserverClass {

    // C++: virtual CollisionReactionType Collision_Occurred(...) { return COLLISION_REACTION_DEFAULT; }
    fun collisionOccurred(event: CollisionEventClass): CollisionReactionType = CollisionReactionType.DEFAULT

    // C++: virtual ExpirationReactionType Object_Expired(...) { return EXPIRATION_APPROVED; }
    fun objectExpired(observedObj: PhysClass): ExpirationReactionType = ExpirationReactionType.APPROVED

    // C++: virtual void Object_Removed_From_Scene(...) {}
    fun objectRemovedFromScene(observedObj: PhysClass) {}

    // C++: virtual void Object_Shattered_Something(...) {}
    fun objectShatteredSomething(observedObj: PhysClass, shatteredObj: PhysClass, surfaceType: Int) {}
}

// C++: enum _CollisionReactionType
enum class CollisionReactionType {
    DEFAULT,         // COLLISION_REACTION_DEFAULT
    STOP_MOTION,     // COLLISION_REACTION_STOP_MOTION
    NO_BOUNCE,       // COLLISION_REACTION_NO_BOUNCE
}

// C++: enum _ExpirationReactionType
enum class ExpirationReactionType {
    DENIED,          // EXPIRATION_DENIED
    APPROVED,        // EXPIRATION_APPROVED
}

// C++: CombatPhysObserverClass : public PhysObserverClass (combatphysobserver.h)
// As_* methods omitted — use Kotlin 'as' operator instead.
interface CombatPhysObserverClass : PhysObserverClass
