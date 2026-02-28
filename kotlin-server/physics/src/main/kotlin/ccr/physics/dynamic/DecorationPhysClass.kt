package ccr.physics.dynamic

import ccr.math.OBBox
import ccr.physics.collision.AABoxIntersectionTest
import ccr.physics.collision.CollisionMath
import ccr.physics.collision.OBBoxIntersectionTest

open class DecorationPhysClass : DynamicPhysClass() {
    override fun timestep(dt: Float) {} // inert, no-op — matches C++ empty Timestep

    override fun asDecorationPhysClass(): DecorationPhysClass? = this

    /**
     * C++: DecorationPhysClass::Intersection_Test(PhysAABoxIntersectionTestClass&)
     *
     * C++ calls Model->Intersect_AABox(test).  In the Kotlin server there is no render model;
     * we use the world bounding box of this object as a conservative approximation.
     */
    override fun intersectionTest(test: AABoxIntersectionTest): Boolean {
        if (CollisionMath.intersectionTest(worldBoundingBox(), test.box)) {
            test.addIntersectedObject(this)
            return true
        }
        return false
    }

    /**
     * C++: DecorationPhysClass::Intersection_Test(PhysOBBoxIntersectionTestClass&)
     *
     * C++ calls Model->Intersect_OBBox(test).  We use the world bounding box wrapped as an
     * axis-aligned OBBox (identity orientation) as a conservative approximation.
     */
    override fun intersectionTest(test: OBBoxIntersectionTest): Boolean {
        val wb = worldBoundingBox()
        if (CollisionMath.intersectionTest(OBBox(wb.center, wb.extent), test.box)) {
            test.addIntersectedObject(this)
            return true
        }
        return false
    }

    companion object {
        // Chunk IDs sourced from decophys.cpp Save()
        const val DECOPHYS_CHUNK_PHYS: Int = 0x05060000        // old parent class data (legacy)
        const val DECOPHYS_CHUNK_DYNAMICPHYS: Int = 0x05060001 // current parent class data
    }
}
