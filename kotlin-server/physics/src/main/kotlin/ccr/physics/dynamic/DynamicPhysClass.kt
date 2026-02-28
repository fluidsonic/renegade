package ccr.physics.dynamic

import ccr.math.Matrix3D
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.OBBoxCollisionTest
import ccr.physics.collision.RayCollisionTest

abstract class DynamicPhysClass : PhysClass() {

    // Matches C++ DynamicPhysClass protected fields (dynamicphys.h)
    var dirtyVisObjectId: Boolean = true
    var visNodeId: Int = 0
    var visStatusLastUpdated: UInt = 0u

    private var _transform: Matrix3D = Matrix3D.IDENTITY
    override var transform: Matrix3D
        get() = _transform
        set(v) { _transform = v }

    override fun needsTimestep(): Boolean = false

    override fun castRay(test: RayCollisionTest): Boolean = false
    override fun castAABox(test: AABoxCollisionTest): Boolean = false
    override fun castOBBox(test: OBBoxCollisionTest): Boolean = false
    override fun timestep(dt: Float) {}

    override fun asDynamicPhysClass(): DynamicPhysClass? = this

    companion object {
        // Chunk IDs sourced from dynamicphys.cpp Save()
        const val DYNAMICPHYS_CHUNK_PHYS: Int = 0x3075698C // 813001100 decimal, wraps parent PhysClass
    }
}
