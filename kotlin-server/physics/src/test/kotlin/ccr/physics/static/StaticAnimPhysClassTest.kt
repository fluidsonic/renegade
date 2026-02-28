package ccr.physics.static

import kotlin.test.*

class ConcreteStaticAnim : StaticAnimPhysClass()

class StaticAnimPhysClassTest {
    @Test fun needsTimestepTrue() {
        assertTrue(ConcreteStaticAnim().needsTimestep())
    }

    @Test fun asStaticAnimPhysClassReturnsSelf() {
        val obj = ConcreteStaticAnim()
        assertSame(obj, obj.asStaticAnimPhysClass())
    }

    @Test fun asStaticPhysClassReturnsSelf() {
        val obj = ConcreteStaticAnim()
        assertSame(obj, obj.asStaticPhysClass())
    }
}
