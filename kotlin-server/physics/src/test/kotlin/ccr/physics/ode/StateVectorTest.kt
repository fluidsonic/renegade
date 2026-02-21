package ccr.physics.ode

import kotlin.test.*

class StateVectorTest {
    @Test fun addAndGet() {
        val sv = StateVector()
        sv.add(1f); sv.add(2f); sv.add(3f)
        assertEquals(3, sv.count)
        assertEquals(1f, sv[0]); assertEquals(2f, sv[1]); assertEquals(3f, sv[2])
    }

    @Test fun set() {
        val sv = StateVector()
        sv.add(0f); sv[0] = 5f; assertEquals(5f, sv[0])
    }

    @Test fun reset() {
        val sv = StateVector()
        sv.add(1f); sv.add(2f); sv.reset(); assertEquals(0, sv.count)
    }

    @Test fun ensureCapacity() {
        val sv = StateVector()
        sv.ensureCapacity(200)
        for (i in 0 until 200) sv.add(i.toFloat())
        assertEquals(200, sv.count)
        assertEquals(199f, sv[199])
    }
}
