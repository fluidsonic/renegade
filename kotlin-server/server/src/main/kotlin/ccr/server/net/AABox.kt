package ccr.server.net

import ccr.math.Vector3

// C++: AABox — axis-aligned bounding box (combat-side, wraps center+extent like C++ AABox)
// Distinct from ccr.math.AABox which uses min/max representation.
class AABox(val center: Vector3 = Vector3(), val extent: Vector3 = Vector3()) {
    // C++: convenience: identity conversion (used in code that unifies AABox and ccr.server.defs.OBBoxClass)
    fun toAABox(): AABox = this
    // Convert to physics-module ccr.math.AABox for PhysicsScene queries
    fun toMathAABox(): ccr.math.AABox = ccr.math.AABox(center, extent)
}
