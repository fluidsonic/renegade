package ccr.physics.moveable

import ccr.math.Triangle
import ccr.math.Vector3
import ccr.physics.scene.PhysicsScene
import ccr.physics.static.StaticPhysClass
import kotlin.math.abs
import kotlin.test.*

class FloorObject : StaticPhysClass() {
    init {
        triangles = listOf(
            Triangle(Vector3(-100f, -100f, 0f), Vector3(100f, -100f, 0f), Vector3(100f, 100f, 0f)),
            Triangle(Vector3(-100f, -100f, 0f), Vector3(100f, 100f, 0f), Vector3(-100f, 100f, 0f)),
        )
    }
}

class ConcretePhys3 : Phys3Class()

class Phys3ClassTest {
    private fun makeScene(): PhysicsScene {
        val scene = PhysicsScene()
        scene.addStaticObject(FloorObject())
        return scene
    }

    @Test
    fun gravityApplied() {
        val scene = makeScene()
        val obj = ConcretePhys3()
        obj.position = Vector3(0f, 0f, 10f)
        scene.addDynamicObject(obj)
        obj.currentMode = Phys3Class.MovementMode.BALLISTIC
        repeat(100) { obj.timestep(0.01f) }
        assertTrue(obj.velocity.z < -5f, "velocity.z=${obj.velocity.z}, expected < -5 after 1s freefall")
    }

    @Test
    fun groundDetection() {
        val scene = makeScene()
        val obj = ConcretePhys3()
        obj.position = Vector3(0f, 0f, 0.9f)
        scene.addDynamicObject(obj)
        obj.checkGround(scene)
        assertTrue(obj.groundState.onGround, "Should detect ground near z=0")
    }

    @Test
    fun projectileFreeFall() {
        val proj = ProjectileClass()
        proj.position = Vector3(0f, 0f, 0f)
        proj.velocity = Vector3(0f, 0f, 0f)
        proj.gravity = 1.0f
        repeat(100) { proj.timestep(0.01f) }
        assertTrue(abs(proj.position.z - (-4.9f)) < 0.5f, "proj.z=${proj.position.z}, expected ~-4.9")
    }
}
