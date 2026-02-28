package ccr.physics.collision

import ccr.math.*
import kotlin.math.abs

object CollisionMath {

    private const val EPSILON = 1e-6f

    // ======================== OVERLAP TESTS ========================

    fun overlapTest(plane: Plane, point: Vector3): OverlapType {
        val d = plane.distanceTo(point)
        return when {
            d > EPSILON -> OverlapType.INSIDE
            d < -EPSILON -> OverlapType.OUTSIDE
            else -> OverlapType.ON
        }
    }

    fun overlapTest(box: AABox, seg: LineSeg): Boolean {
        // Separating axis test: translate to box-centered coordinates
        val c = box.center
        val e = box.extent
        val m = (seg.p0 + seg.p1) * 0.5f - c // segment midpoint in box space
        val d = seg.dp * 0.5f // half-direction
        val ad = Vector3(abs(d.x), abs(d.y), abs(d.z))
        // Box face axes
        if (abs(m.x) > e.x + ad.x) return false
        if (abs(m.y) > e.y + ad.y) return false
        if (abs(m.z) > e.z + ad.z) return false
        // Cross product axes
        if (abs(m.y * d.z - m.z * d.y) > e.y * ad.z + e.z * ad.y) return false
        if (abs(m.z * d.x - m.x * d.z) > e.x * ad.z + e.z * ad.x) return false
        if (abs(m.x * d.y - m.y * d.x) > e.x * ad.y + e.y * ad.x) return false
        return true
    }

    fun overlapTest(a: AABox, b: AABox): Boolean = a.overlaps(b)

    fun overlapTest(plane: Plane, box: AABox): OverlapType {
        val n = plane.normal
        val r = box.extent.x * abs(n.x) + box.extent.y * abs(n.y) + box.extent.z * abs(n.z)
        val d = plane.distanceTo(box.center)
        return when {
            d > r -> OverlapType.INSIDE
            d < -r -> OverlapType.OUTSIDE
            else -> OverlapType.OVERLAPPED
        }
    }

    // ======================== INTERSECTION TESTS ========================

    fun intersectionTest(a: AABox, b: AABox): Boolean = a.overlaps(b)

    fun intersectionTest(box: AABox, tri: Triangle): Boolean {
        // Translate triangle to box-center space
        val v0 = tri.v0 - box.center
        val v1 = tri.v1 - box.center
        val v2 = tri.v2 - box.center
        val e = box.extent
        // SAT: 3 box face axes
        if (maxOf(v0.x, v1.x, v2.x) < -e.x || minOf(v0.x, v1.x, v2.x) > e.x) return false
        if (maxOf(v0.y, v1.y, v2.y) < -e.y || minOf(v0.y, v1.y, v2.y) > e.y) return false
        if (maxOf(v0.z, v1.z, v2.z) < -e.z || minOf(v0.z, v1.z, v2.z) > e.z) return false
        // SAT: triangle normal
        val n = tri.normal
        val r = e.x * abs(n.x) + e.y * abs(n.y) + e.z * abs(n.z)
        val d = n.dot(v0)
        if (d > r || d < -r) return false
        // SAT: 9 edge cross products (3 triangle edges x 3 box axes)
        val edges = arrayOf(v1 - v0, v2 - v1, v0 - v2)
        val axes = arrayOf(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, 0f, 1f))
        for (edge in edges) {
            for (axis in axes) {
                val a2 = axis.cross(edge)
                if (a2.lengthSquared() < EPSILON * EPSILON) continue
                val p0 = a2.dot(v0); val p1 = a2.dot(v1); val p2 = a2.dot(v2)
                val pMin = minOf(p0, p1, p2); val pMax = maxOf(p0, p1, p2)
                val rad = e.x * abs(a2.x) + e.y * abs(a2.y) + e.z * abs(a2.z)
                if (pMin > rad || pMax < -rad) return false
            }
        }
        return true
    }

    fun intersectionTest(box: OBBox, tri: Triangle): Boolean {
        // Transform triangle into OBB local space, then do AABB-triangle test
        val localBox = AABox(Vector3.ZERO, box.extent)
        val localTri = Triangle(toOBBLocal(box, tri.v0), toOBBLocal(box, tri.v1), toOBBLocal(box, tri.v2))
        return intersectionTest(localBox, localTri)
    }

    fun intersectionTest(a: OBBox, b: OBBox): Boolean {
        // SAT with 15 axes: 3 from A, 3 from B, 9 cross products
        val axesA = obbAxes(a)
        val axesB = obbAxes(b)
        val t = b.center - a.center
        for (ax in axesA) if (!satOverlap(ax, t, a, b)) return false
        for (ax in axesB) if (!satOverlap(ax, t, a, b)) return false
        for (ax in axesA) for (bx in axesB) {
            val cross = ax.cross(bx)
            if (cross.lengthSquared() >= EPSILON * EPSILON) {
                if (!satOverlap(cross, t, a, b)) return false
            }
        }
        return true
    }

    fun pointInOBBox(point: Vector3, box: OBBox): Boolean {
        val d = point - box.center
        val r0 = Vector3(box.basis.m00, box.basis.m01, box.basis.m02)
        val r1 = Vector3(box.basis.m10, box.basis.m11, box.basis.m12)
        val r2 = Vector3(box.basis.m20, box.basis.m21, box.basis.m22)
        if (abs(d.dot(r0)) > box.extent.x) return false
        if (abs(d.dot(r1)) > box.extent.y) return false
        if (abs(d.dot(r2)) > box.extent.z) return false
        return true
    }

    // ======================== COLLISION (SWEPT) ========================

    /** Moller-Trumbore ray-triangle intersection. */
    fun collide(seg: LineSeg, tri: Triangle, res: CastResult): Boolean {
        val e1 = tri.v1 - tri.v0
        val e2 = tri.v2 - tri.v0
        val d = seg.dp
        val h = d.cross(e2)
        val a = e1.dot(h)
        if (abs(a) < EPSILON) return false
        val f = 1f / a
        val s = seg.p0 - tri.v0
        val u = f * s.dot(h)
        if (u < 0f || u > 1f) return false
        val q = s.cross(e1)
        val v = f * d.dot(q)
        if (v < 0f || u + v > 1f) return false
        val t = f * e2.dot(q)
        if (t < 0f || t > res.fraction) return false
        res.fraction = t
        res.normal = if (a < 0f) -tri.normal else tri.normal
        if (res.computeContactPoint) res.contactPoint = seg.pointAt(t)
        return true
    }

    /** Slab method for ray vs AABB. */
    fun collide(seg: LineSeg, box: AABox, res: CastResult): Boolean {
        val d = seg.dp
        if (seg.length < EPSILON) return false
        // t1/t2 are parametric along dp: 0=p0, 1=p1
        var tMin = 0f
        var tMax = 1f
        var hitAxis = -1
        var hitSign = 0f
        for (axis in 0..2) {
            val origin = componentOf(seg.p0, axis)
            val dir = componentOf(d, axis)
            val bMin = componentOf(box.min, axis)
            val bMax = componentOf(box.max, axis)
            if (abs(dir) < EPSILON) {
                if (origin < bMin || origin > bMax) return false
            } else {
                var t1 = (bMin - origin) / dir
                var t2 = (bMax - origin) / dir
                var sign = -1f
                if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp; sign = 1f }
                if (t1 > tMin) { tMin = t1; hitAxis = axis; hitSign = sign }
                if (t2 < tMax) tMax = t2
                if (tMin > tMax) return false
            }
        }
        if (tMin < 0f || tMin > res.fraction) return false
        res.fraction = tMin
        res.normal = when (hitAxis) {
            0 -> Vector3(hitSign, 0f, 0f)
            1 -> Vector3(0f, hitSign, 0f)
            2 -> Vector3(0f, 0f, hitSign)
            else -> Vector3.ZERO
        }
        if (res.computeContactPoint) res.contactPoint = seg.pointAt(tMin)
        return true
    }

    /** Ray vs OBBox: transform to local space and use slab method. */
    fun collide(seg: LineSeg, box: OBBox, res: CastResult): Boolean {
        val localP0 = toOBBLocal(box, seg.p0)
        val localP1 = toOBBLocal(box, seg.p1)
        val localBox = AABox(Vector3.ZERO, box.extent)
        return collide(LineSeg(localP0, localP1), localBox, res)
    }

    /** Swept AABB vs static triangle.
     *
     * Uses the Minkowski sum approach: cast the box center as a segment against the
     * Minkowski sum of the triangle and the inverted box.  The Minkowski sum is tested
     * with SAT using:
     *   - the triangle normal (1 axis)
     *   - 3 box face normals (X, Y, Z axes) projected on the contact plane
     *   - 9 edge–edge cross product axes (3 triangle edges × 3 box axes)
     *
     * This is equivalent to how the C++ PhysAABoxCollisionTestClass works internally.
     */
    fun collide(box: AABox, move: Vector3, tri: Triangle, res: CastResult): Boolean {
        val center = box.center
        val e = box.extent
        val n = tri.normal

        // ---- Step 1: find t when box face first contacts the triangle plane ----
        val boxRadius = e.x * abs(n.x) + e.y * abs(n.y) + e.z * abs(n.z)
        val d0 = n.dot(center) - n.dot(tri.v0)
        val d1 = n.dot(center + move) - n.dot(tri.v0)
        // If both endpoints are on the same side and farther than boxRadius, no collision.
        if (d0 > boxRadius && d1 > boxRadius) return false
        if (d0 < -boxRadius && d1 < -boxRadius) return false
        val enterDist = if (d0 > 0f) boxRadius else -boxRadius
        val denom = d0 - d1
        if (abs(denom) < EPSILON) {
            // Moving parallel to the plane — only a hit if already overlapping (d0 between -r and r).
            // Treat as no collision (box is sliding along the plane, not penetrating).
            return false
        }
        val t = (d0 - enterDist) / denom
        if (t < 0f || t > res.fraction) return false

        // ---- Step 2: SAT on the contact plane to verify the box actually overlaps the triangle ----
        // At time t the box center is at contactCenter.  We test the 2D "shadow" of the box
        // against the Minkowski sum of the triangle's edges using the 9 edge-cross axes and the
        // 3 box face normals projected onto the triangle plane.
        val contactCenter = center + move * t

        // Translate triangle vertices into contact-center space
        val v0 = tri.v0 - contactCenter
        val v1 = tri.v1 - contactCenter
        val v2 = tri.v2 - contactCenter

        val boxAxes = arrayOf(Vector3(1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, 0f, 1f))
        val triEdges = arrayOf(v1 - v0, v2 - v1, v0 - v2)

        // Check 9 edge-cross axes
        for (edge in triEdges) {
            for (boxAxis in boxAxes) {
                val axis = boxAxis.cross(edge)
                if (axis.lengthSquared() < EPSILON * EPSILON) continue
                val p0 = axis.dot(v0); val p1 = axis.dot(v1); val p2 = axis.dot(v2)
                val pMin = minOf(p0, p1, p2); val pMax = maxOf(p0, p1, p2)
                // Box projected onto axis: radius = sum of |axis . boxAxis_i| * extent_i
                val rad = e.x * abs(axis.x) + e.y * abs(axis.y) + e.z * abs(axis.z)
                if (pMin > rad || pMax < -rad) return false
            }
        }

        // Check 3 box face axes (X, Y, Z)
        val extentComponents = floatArrayOf(e.x, e.y, e.z)
        for (axisIdx in 0..2) {
            val boxAxis = boxAxes[axisIdx]
            val p0 = boxAxis.dot(v0); val p1 = boxAxis.dot(v1); val p2 = boxAxis.dot(v2)
            val pMin = minOf(p0, p1, p2); val pMax = maxOf(p0, p1, p2)
            val rad = extentComponents[axisIdx]
            if (pMin > rad || pMax < -rad) return false
        }

        // All axes pass — genuine contact
        res.fraction = t
        res.normal = if (d0 > 0f) n else -n
        if (res.computeContactPoint) res.contactPoint = contactCenter
        return true
    }

    /** Swept AABB vs swept AABB using relative motion. */
    fun collide(a: AABox, aMove: Vector3, b: AABox, bMove: Vector3, res: CastResult): Boolean {
        // Treat a's center as a ray, expand b by a's extent, use relative motion
        val relMove = aMove - bMove
        val expanded = AABox(b.center, b.extent + a.extent)
        return collide(LineSeg(a.center, a.center + relMove), expanded, res)
    }

    /** Swept OBBox vs moving triangle. */
    fun collide(box: OBBox, move: Vector3, tri: Triangle, triMove: Vector3, res: CastResult): Boolean {
        val relMove = move - triMove
        val localMove = rotateToOBBLocal(box, relMove)
        val localBox = AABox(Vector3.ZERO, box.extent)
        val localTri = Triangle(toOBBLocal(box, tri.v0), toOBBLocal(box, tri.v1), toOBBLocal(box, tri.v2))
        return collide(localBox, localMove, localTri, res)
    }

    /** Swept OBBox vs moving AABox. */
    fun collide(box: OBBox, move: Vector3, aabox: AABox, aaboxMove: Vector3, res: CastResult): Boolean {
        val relMove = move - aaboxMove
        val localMove = rotateToOBBLocal(box, relMove)
        val localAA = AABox(toOBBLocal(box, aabox.center), aabox.extent)
        return collide(AABox(Vector3.ZERO, box.extent), localMove, localAA, Vector3.ZERO, res)
    }

    /** Swept OBBox vs swept OBBox. */
    fun collide(a: OBBox, aMove: Vector3, b: OBBox, bMove: Vector3, res: CastResult): Boolean {
        val relMove = aMove - bMove
        val localMove = rotateToOBBLocal(a, relMove)
        val localB = AABox(toOBBLocal(a, b.center), b.extent)
        return collide(AABox(Vector3.ZERO, a.extent), localMove, localB, Vector3.ZERO, res)
    }

    // ======================== HELPERS ========================

    private fun componentOf(v: Vector3, axis: Int): Float = when (axis) {
        0 -> v.x; 1 -> v.y; else -> v.z
    }

    /** Transform a world-space point into OBB local space. */
    private fun toOBBLocal(box: OBBox, v: Vector3): Vector3 {
        val d = v - box.center
        val r0 = Vector3(box.basis.m00, box.basis.m01, box.basis.m02)
        val r1 = Vector3(box.basis.m10, box.basis.m11, box.basis.m12)
        val r2 = Vector3(box.basis.m20, box.basis.m21, box.basis.m22)
        return Vector3(r0.dot(d), r1.dot(d), r2.dot(d))
    }

    /** Rotate a direction vector into OBB local space (no translation). */
    private fun rotateToOBBLocal(box: OBBox, v: Vector3): Vector3 {
        val r0 = Vector3(box.basis.m00, box.basis.m01, box.basis.m02)
        val r1 = Vector3(box.basis.m10, box.basis.m11, box.basis.m12)
        val r2 = Vector3(box.basis.m20, box.basis.m21, box.basis.m22)
        return Vector3(r0.dot(v), r1.dot(v), r2.dot(v))
    }

    /** Extract the 3 column axes of an OBBox basis (world-space directions). */
    private fun obbAxes(box: OBBox): Array<Vector3> = arrayOf(
        Vector3(box.basis.m00, box.basis.m10, box.basis.m20),
        Vector3(box.basis.m01, box.basis.m11, box.basis.m21),
        Vector3(box.basis.m02, box.basis.m12, box.basis.m22),
    )

    /** SAT overlap test for a single axis between two OBBs. */
    private fun satOverlap(axis: Vector3, t: Vector3, a: OBBox, b: OBBox): Boolean {
        val ra = a.projectToAxis(axis)
        val rb = b.projectToAxis(axis)
        return abs(t.dot(axis)) <= ra + rb
    }
}
