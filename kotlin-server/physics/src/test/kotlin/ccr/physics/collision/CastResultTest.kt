package ccr.physics.collision

import kotlin.test.*

class CastResultTest {
    @Test
    fun initialValues() {
        val r = CastResult()
        assertEquals(1.0f, r.fraction)
        assertFalse(r.startBad)
        assertEquals(0, r.surfaceType)
        assertFalse(r.computeContactPoint)
    }

    @Test
    fun reset() {
        val r = CastResult()
        r.fraction = 0.5f
        r.startBad = true
        r.surfaceType = 3
        r.reset()
        assertEquals(1.0f, r.fraction)
        assertFalse(r.startBad)
        assertEquals(0, r.surfaceType)
    }
}
