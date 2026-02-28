package ccr.physics.collision

import ccr.math.AABox
import ccr.math.Triangle

/**
 * C++: PhysAABoxIntersectionTestClass (physinttest.h)
 *
 * Axis-aligned box intersection test.  Mirrors the C++ class:
 *   - [box] is the world-space AABox to test against.
 *   - [collisionGroup] limits which objects are tested.
 *   - [collisionType] is the type of collision surfaces considered.
 *   - [checkStaticObjs] / [checkDynamicObjs] control which categories are queried.
 *   - [intersectedObjects] accumulates every [ccr.physics.PhysClass] that overlaps [box].
 */
class AABoxIntersectionTest(
    val box: AABox,
    val collisionGroup: Int,
    val collisionType: Int,
    val checkStaticObjs: Boolean = true,
    val checkDynamicObjs: Boolean = true,
) {
    val intersectedObjects: MutableList<ccr.physics.PhysClass> = mutableListOf()

    /** C++: Add_Intersected_Object — appends [obj] to the result list. */
    fun addIntersectedObject(obj: ccr.physics.PhysClass) {
        intersectedObjects.add(obj)
    }

    /** C++: AABoxIntersectionTestClass::Cull(const Vector3& min, const Vector3& max) */
    fun cull(cullMin: ccr.math.Vector3, cullMax: ccr.math.Vector3): Boolean {
        val boxMin = box.min
        val boxMax = box.max
        if (boxMin.x > cullMax.x || boxMax.x < cullMin.x) return true
        if (boxMin.y > cullMax.y || boxMax.y < cullMin.y) return true
        if (boxMin.z > cullMax.z || boxMax.z < cullMin.z) return true
        return false
    }

    /** C++: AABoxIntersectionTestClass::Cull(const AABoxClass& box) */
    fun cull(cullBox: AABox): Boolean {
        val dc = cullBox.center - box.center
        val r = cullBox.extent + box.extent
        if (kotlin.math.abs(dc.x) > r.x) return true
        if (kotlin.math.abs(dc.y) > r.y) return true
        if (kotlin.math.abs(dc.z) > r.z) return true
        return false
    }

    /** C++: AABoxIntersectionTestClass::Intersect_Triangle(const TriClass& tri) */
    fun intersectTriangle(tri: Triangle): Boolean = CollisionMath.intersectionTest(box, tri)
}
