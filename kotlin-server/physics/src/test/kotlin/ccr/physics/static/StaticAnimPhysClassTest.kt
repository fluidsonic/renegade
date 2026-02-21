package ccr.physics.static

import kotlin.test.*

class ConcreteStaticAnim : StaticAnimPhysClass()

class StaticAnimPhysClassTest {
    @Test fun advancesAnimFrame() {
        val obj = ConcreteStaticAnim()
        obj.animSpeed = 1f; obj.animLength = 2f; obj.animLooping = false
        obj.timestep(0.5f)
        assertTrue(kotlin.math.abs(obj.animFrame - 0.5f) < 1e-5f)
    }

    @Test fun loopsAnimFrame() {
        val obj = ConcreteStaticAnim()
        obj.animSpeed = 1f; obj.animLength = 1f; obj.animLooping = true; obj.animFrame = 0.9f
        obj.timestep(0.5f) // 0.9 + 0.5 = 1.4 → wraps to 0.4
        assertTrue(kotlin.math.abs(obj.animFrame - 0.4f) < 1e-5f)
    }

    @Test fun needsTimestepTrue() {
        assertTrue(ConcreteStaticAnim().needsTimestep())
    }
}
