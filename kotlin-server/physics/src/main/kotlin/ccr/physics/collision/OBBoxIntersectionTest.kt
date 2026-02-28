package ccr.physics.collision

import ccr.math.AABox
import ccr.math.OBBox
import ccr.math.Vector3
import kotlin.math.abs

/**
 * C++: PhysOBBoxIntersectionTestClass (physinttest.h)
 *
 * Oriented box intersection test.  Mirrors the C++ class:
 *   - [box] is the world-space OBBox to test against.
 *   - [boundingBox] is the AABB wrapping [box] (used for quick culling, mirrors C++ BoundingBox).
 *   - [collisionGroup] limits which objects are tested.
 *   - [collisionType] is the type of collision surfaces considered.
 *   - [checkStaticObjs] / [checkDynamicObjs] control which categories are queried.
 *   - [intersectedObjects] accumulates every [ccr.physics.PhysClass] that overlaps [box].
 */
class OBBoxIntersectionTest(
    val box: OBBox,
    val collisionGroup: Int,
    val collisionType: Int,
    val checkStaticObjs: Boolean = true,
    val checkDynamicObjs: Boolean = true,
) {
    /**
     * C++: OBBoxIntersectionTestClass::BoundingBox — the axis-aligned wrapper for fast culling.
     * Computed via OBBoxClass::Basis.Rotate_AABox_Extent, equivalent to [OBBox.computeAxisAlignedExtent].
     */
    val boundingBox: AABox = AABox(box.center, box.computeAxisAlignedExtent())

    val intersectedObjects: MutableList<ccr.physics.PhysClass> = mutableListOf()

    /** C++: Add_Intersected_Object — appends [obj] to the result list. */
    fun addIntersectedObject(obj: ccr.physics.PhysClass) {
        intersectedObjects.add(obj)
    }

    /** C++: OBBoxIntersectionTestClass::Cull(const Vector3& min, const Vector3& max) */
    fun cull(cullMin: Vector3, cullMax: Vector3): Boolean {
        val boxMin = boundingBox.min
        val boxMax = boundingBox.max
        if (boxMin.x > cullMax.x || boxMax.x < cullMin.x) return true
        if (boxMin.y > cullMax.y || boxMax.y < cullMin.y) return true
        if (boxMin.z > cullMax.z || boxMax.z < cullMin.z) return true
        return false
    }

    /** C++: OBBoxIntersectionTestClass::Cull(const AABoxClass& box) */
    fun cull(cullBox: AABox): Boolean {
        val dc = cullBox.center - boundingBox.center
        val r = cullBox.extent + boundingBox.extent
        if (abs(dc.x) > r.x) return true
        if (abs(dc.y) > r.y) return true
        if (abs(dc.z) > r.z) return true
        return false
    }
}
