package ccr.physics.static

import ccr.math.AABox
import ccr.math.Matrix3D
import ccr.math.Triangle
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.AABoxIntersectionTest
import ccr.physics.collision.CollisionMath
import ccr.physics.collision.OBBoxCollisionTest
import ccr.physics.collision.OBBoxIntersectionTest
import ccr.physics.collision.RayCollisionTest

open class StaticPhysClass : PhysClass() {

    companion object {
        // Chunk IDs from staticphys.cpp (see also PhysChunkIds.STATICPHYS_CHUNK_PHYS / _VARIABLES)
        // Micro-chunk IDs within STATICPHYS_CHUNK_VARIABLES:
        const val STATICPHYS_VARIABLE_VIS_OBJECT_ID: Int = 0x00  // uint32 visObjectId
        const val STATICPHYS_VARIABLE_VIS_SECTOR_ID: Int = 0x01  // int32 visSectorId
    }

    var triangles: List<Triangle> = emptyList()

    /** Vis-sector ID for this static object (-1 / 0xFFFFFFFF if not a vis sector). Mirrors C++ VisSectorID. */
    var visSectorId: Int = -1

    private var _transform: Matrix3D = Matrix3D.IDENTITY
    override var transform: Matrix3D
        get() = _transform
        set(v) { _transform = v }

    override fun asStaticPhysClass(): StaticPhysClass? = this

    override fun needsTimestep(): Boolean = false
    override fun timestep(dt: Float) {} // no-op

    /**
     * C++: virtual bool StaticPhysClass::Has_Dynamic_State() — returns false.
     * If true, the object has runtime state that can be saved/loaded separately
     * from the full object save. StaticAnimPhysClass overrides this to return true.
     */
    open fun hasDynamicState(): Boolean = false

    /**
     * C++: virtual void StaticPhysClass::Save_State(ChunkSaveClass &) — no-op.
     * Override in subclasses to write runtime state (e.g. animation frame).
     */
    open fun saveState(csave: Any) {}

    /**
     * C++: virtual void StaticPhysClass::Load_State(ChunkLoadClass &) — no-op.
     * Override in subclasses to read runtime state (e.g. animation frame).
     */
    open fun loadState(cload: Any) {}

    /** Compute world-space bounding box from actual triangle geometry. */
    override fun worldBoundingBox(): AABox {
        if (triangles.isEmpty()) return super.worldBoundingBox()
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (tri in triangles) {
            for (v in listOf(tri.v0, tri.v1, tri.v2)) {
                if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x
                if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y
                if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z
            }
        }
        return AABox.fromMinMax(Vector3(minX, minY, minZ), Vector3(maxX, maxY, maxZ))
    }

    override fun castRay(test: RayCollisionTest): Boolean {
        var hit = false
        for (tri in triangles) {
            if (CollisionMath.collide(test.ray, tri, test.result)) hit = true
        }
        return hit
    }

    override fun castAABox(test: AABoxCollisionTest): Boolean {
        var hit = false
        for (tri in triangles) {
            if (CollisionMath.collide(test.box, test.move, tri, test.result)) hit = true
        }
        return hit
    }

    override fun castOBBox(test: OBBoxCollisionTest): Boolean = false

    /**
     * C++: StaticPhysClass::Intersection_Test(PhysAABoxIntersectionTestClass&)
     *
     * C++ calls Model->Intersect_AABox(test) which tests the W3D render geometry.
     * In the Kotlin server we store triangles directly — test each one against [test].box.
     * If any triangle intersects, the object is added and true is returned.
     */
    override fun intersectionTest(test: AABoxIntersectionTest): Boolean {
        for (tri in triangles) {
            if (CollisionMath.intersectionTest(test.box, tri)) {
                test.addIntersectedObject(this)
                return true
            }
        }
        return false
    }

    /**
     * C++: StaticPhysClass::Intersection_Test(PhysOBBoxIntersectionTestClass&)
     *
     * C++ calls Model->Intersect_OBBox(test) which tests the W3D render geometry.
     * In the Kotlin server we store triangles — test each one against [test].box (OBBox).
     */
    override fun intersectionTest(test: OBBoxIntersectionTest): Boolean {
        for (tri in triangles) {
            if (CollisionMath.intersectionTest(test.box, tri)) {
                test.addIntersectedObject(this)
                return true
            }
        }
        return false
    }
}
