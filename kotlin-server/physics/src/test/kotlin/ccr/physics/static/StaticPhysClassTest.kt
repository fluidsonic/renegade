package ccr.physics.static

import ccr.math.LineSeg
import ccr.math.Triangle
import ccr.math.Vector3
import ccr.physics.collision.RayCollisionTest
import kotlin.test.*

class ConcreteStaticPhys : StaticPhysClass()

class StaticPhysClassTest {
    @Test fun rayHitsTriangle() {
        val obj = ConcreteStaticPhys()
        obj.triangles = listOf(Triangle(Vector3(-1f, -1f, 0f), Vector3(1f, -1f, 0f), Vector3(0f, 1f, 0f)))
        val ray = LineSeg(Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
        val test = RayCollisionTest(ray)
        assertTrue(obj.castRay(test))
        assertTrue(test.result.fraction < 1f)
    }

    @Test fun rayMissesWhenNoTriangles() {
        val obj = ConcreteStaticPhys()
        val ray = LineSeg(Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
        val test = RayCollisionTest(ray)
        assertFalse(obj.castRay(test))
    }

    @Test fun closestTriangleWins() {
        val obj = ConcreteStaticPhys()
        // Two triangles at different depths
        obj.triangles = listOf(
            Triangle(Vector3(-1f, -1f, -0.5f), Vector3(1f, -1f, -0.5f), Vector3(0f, 1f, -0.5f)),
            Triangle(Vector3(-1f, -1f, 0.5f), Vector3(1f, -1f, 0.5f), Vector3(0f, 1f, 0.5f)),
        )
        val ray = LineSeg(Vector3(0f, 0f, 2f), Vector3(0f, 0f, -2f))
        val test = RayCollisionTest(ray)
        obj.castRay(test)
        // The closer triangle (at z=0.5) should have a smaller fraction
        assertTrue(test.result.fraction < 0.75f, "fraction=${test.result.fraction}")
    }

    @Test fun needsTimestepFalse() {
        assertFalse(ConcreteStaticPhys().needsTimestep())
    }
}
