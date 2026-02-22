package ccr.physics.static

import ccr.math.Matrix3D
import ccr.math.Triangle
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.CollisionMath
import ccr.physics.collision.OBBoxCollisionTest
import ccr.physics.collision.RayCollisionTest

open class StaticPhysClass : PhysClass() {
    var triangles: List<Triangle> = emptyList()
    private var _transform: Matrix3D = Matrix3D.IDENTITY
    override var transform: Matrix3D
        get() = _transform
        set(v) { _transform = v }

    override fun needsTimestep(): Boolean = false
    override fun timestep(dt: Float) {} // no-op

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
}
