package ccr.physics.collision

import ccr.math.*
import kotlin.math.abs
import kotlin.test.*

class CollisionMathTest {

    // ======================== RAY-TRIANGLE ========================

    @Test
    fun rayHitsTriangle() {
        val tri = Triangle(Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f))
        val seg = LineSeg(Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
        val res = CastResult()
        assertTrue(CollisionMath.collide(seg, tri, res))
        assertTrue(abs(res.fraction - 0.5f) < 0.01f, "fraction=${res.fraction}")
    }

    @Test
    fun rayMissesTriangleToSide() {
        val tri = Triangle(Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f))
        val seg = LineSeg(Vector3(5f, 5f, 1f), Vector3(5f, 5f, -1f))
        val res = CastResult()
        assertFalse(CollisionMath.collide(seg, tri, res))
    }

    @Test
    fun rayParallelToTriangle() {
        val tri = Triangle(Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f))
        val seg = LineSeg(Vector3(0f, 0f, 1f), Vector3(1f, 0f, 1f)) // parallel, above
        val res = CastResult()
        assertFalse(CollisionMath.collide(seg, tri, res))
    }

    @Test
    fun rayBehindTriangle() {
        val tri = Triangle(Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f))
        val seg = LineSeg(Vector3(0f, 0f, -1f), Vector3(0f, 0f, -2f)) // both behind
        val res = CastResult()
        assertFalse(CollisionMath.collide(seg, tri, res))
    }

    // ======================== RAY-AABB ========================

    @Test
    fun rayHitsAABox() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val seg = LineSeg(Vector3(0f, 0f, 3f), Vector3(0f, 0f, -3f))
        val res = CastResult()
        assertTrue(CollisionMath.collide(seg, box, res))
        assertTrue(res.fraction < 1f, "fraction=${res.fraction}")
        // Entry at z=1 out of total distance 6, so fraction ~ 2/6 = 0.333
        assertTrue(abs(res.fraction - 1f / 3f) < 0.01f, "fraction=${res.fraction}")
    }

    @Test
    fun rayMissesAABox() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val seg = LineSeg(Vector3(5f, 5f, 3f), Vector3(5f, 5f, -3f))
        val res = CastResult()
        assertFalse(CollisionMath.collide(seg, box, res))
    }

    @Test
    fun rayHitsAABoxNormal() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val seg = LineSeg(Vector3(0f, 0f, 3f), Vector3(0f, 0f, -3f))
        val res = CastResult()
        CollisionMath.collide(seg, box, res)
        // Entering through +Z face, normal should point toward ray origin = (0,0,1)
        assertEquals(Vector3(0f, 0f, 1f), res.normal)
    }

    // ======================== AABB OVERLAP/INTERSECTION ========================

    @Test
    fun aabbOverlap() {
        val a = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val b = AABox(Vector3(1.5f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertTrue(CollisionMath.intersectionTest(a, b))
    }

    @Test
    fun aabbNoOverlap() {
        val a = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val c = AABox(Vector3(5f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertFalse(CollisionMath.intersectionTest(a, c))
    }

    // ======================== AABB-TRIANGLE ========================

    @Test
    fun aabbTriangleIntersection() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val tri = Triangle(Vector3(-2f, 0f, 0f), Vector3(2f, 0f, 0f), Vector3(0f, 2f, 0f))
        assertTrue(CollisionMath.intersectionTest(box, tri))
    }

    @Test
    fun aabbTriangleNoIntersection() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val tri = Triangle(Vector3(5f, 5f, 5f), Vector3(6f, 5f, 5f), Vector3(5f, 6f, 5f))
        assertFalse(CollisionMath.intersectionTest(box, tri))
    }

    // ======================== SWEPT AABB vs TRIANGLE ========================

    @Test
    fun sweptAABoxHitsTriangle() {
        val box = AABox(Vector3(0f, 0f, 1f), Vector3(0.5f, 0.5f, 0.5f))
        val move = Vector3(0f, 0f, -2f)
        val tri = Triangle(Vector3(-2f, -2f, 0f), Vector3(2f, -2f, 0f), Vector3(0f, 2f, 0f))
        val res = CastResult()
        assertTrue(CollisionMath.collide(box, move, tri, res), "Expected swept box to hit triangle")
        assertTrue(res.fraction in 0f..1f, "fraction=${res.fraction}")
    }

    @Test
    fun sweptAABoxMissesTriangle() {
        val box = AABox(Vector3(10f, 10f, 1f), Vector3(0.5f, 0.5f, 0.5f))
        val move = Vector3(0f, 0f, -2f) // moving down but far from triangle
        val tri = Triangle(Vector3(-2f, -2f, 0f), Vector3(2f, -2f, 0f), Vector3(0f, 2f, 0f))
        val res = CastResult()
        assertFalse(CollisionMath.collide(box, move, tri, res))
    }

    // ======================== OVERLAP TESTS ========================

    @Test
    fun planePointOverlap() {
        val plane = Plane.fromNormalAndPoint(Vector3(0f, 0f, 1f), Vector3(0f, 0f, 0f))
        assertEquals(OverlapType.INSIDE, CollisionMath.overlapTest(plane, Vector3(0f, 0f, 1f)))
        assertEquals(OverlapType.OUTSIDE, CollisionMath.overlapTest(plane, Vector3(0f, 0f, -1f)))
        assertEquals(OverlapType.ON, CollisionMath.overlapTest(plane, Vector3(0f, 0f, 0f)))
    }

    @Test
    fun planeBoxOverlap() {
        val plane = Plane.fromNormalAndPoint(Vector3(0f, 0f, 1f), Vector3(0f, 0f, 0f))
        val boxInFront = AABox(Vector3(0f, 0f, 5f), Vector3(1f, 1f, 1f))
        assertEquals(OverlapType.INSIDE, CollisionMath.overlapTest(plane, boxInFront))
        val boxBehind = AABox(Vector3(0f, 0f, -5f), Vector3(1f, 1f, 1f))
        assertEquals(OverlapType.OUTSIDE, CollisionMath.overlapTest(plane, boxBehind))
        val boxStraddling = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertEquals(OverlapType.OVERLAPPED, CollisionMath.overlapTest(plane, boxStraddling))
    }

    @Test
    fun lineSegBoxOverlap() {
        val box = AABox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val segThrough = LineSeg(Vector3(0f, 0f, 2f), Vector3(0f, 0f, -2f))
        assertTrue(CollisionMath.overlapTest(box, segThrough))
        val segOutside = LineSeg(Vector3(5f, 5f, 5f), Vector3(6f, 6f, 6f))
        assertFalse(CollisionMath.overlapTest(box, segOutside))
    }

    // ======================== OBBox TESTS ========================

    @Test
    fun obboxIntersection() {
        // Two axis-aligned OBBs that overlap
        val a = OBBox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val b = OBBox(Vector3(1.5f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertTrue(CollisionMath.intersectionTest(a, b))
    }

    @Test
    fun obboxNoIntersection() {
        val a = OBBox(Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f))
        val b = OBBox(Vector3(5f, 0f, 0f), Vector3(1f, 1f, 1f))
        assertFalse(CollisionMath.intersectionTest(a, b))
    }
}
