package ccr.physics.static

import ccr.math.AABox
import ccr.math.Matrix3D
import ccr.math.Triangle
import ccr.math.Vector3
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
}
