package ccr.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapRotationTest {

    // ---- single-map ----

    @Test fun `single map - currentName returns that map`() {
        val r = MapRotation(listOf("MapA"), loops = false)
        assertEquals("MapA", r.currentName)
    }

    @Test fun `single map no-loop - nextName is null`() {
        assertNull(MapRotation(listOf("MapA"), loops = false).nextName())
    }

    @Test fun `single map looping - nextName is still null (no rotation)`() {
        assertNull(MapRotation(listOf("MapA"), loops = true).nextName())
    }

    @Test fun `single map - isMapCycleOver is false`() {
        assertFalse(MapRotation(listOf("MapA"), loops = false).isMapCycleOver)
    }

    @Test fun `single map - advance is no-op`() {
        val r = MapRotation(listOf("MapA"), loops = false)
        assertEquals(r, r.advance())
    }

    // ---- multi-map no-loop ----

    @Test fun `multi no-loop - nextName returns second map from first`() {
        val r = MapRotation(listOf("A", "B", "C"), loops = false, currentIndex = 0)
        assertEquals("B", r.nextName())
    }

    @Test fun `multi no-loop - nextName returns null at last map`() {
        val r = MapRotation(listOf("A", "B", "C"), loops = false, currentIndex = 2)
        assertNull(r.nextName())
    }

    @Test fun `multi no-loop - isMapCycleOver false at first map`() {
        assertFalse(MapRotation(listOf("A", "B"), loops = false, currentIndex = 0).isMapCycleOver)
    }

    @Test fun `multi no-loop - isMapCycleOver true at last map`() {
        assertTrue(MapRotation(listOf("A", "B"), loops = false, currentIndex = 1).isMapCycleOver)
    }

    @Test fun `multi no-loop - advance moves to next index`() {
        val r = MapRotation(listOf("A", "B", "C"), loops = false, currentIndex = 0)
        assertEquals(1, r.advance().currentIndex)
    }

    @Test fun `multi no-loop - advance at last index wraps to 0`() {
        val r = MapRotation(listOf("A", "B", "C"), loops = false, currentIndex = 2)
        assertEquals(0, r.advance().currentIndex)
    }

    // ---- multi-map looping ----

    @Test fun `multi loop - nextName returns second map from first`() {
        val r = MapRotation(listOf("A", "B", "C"), loops = true, currentIndex = 0)
        assertEquals("B", r.nextName())
    }

    @Test fun `multi loop - nextName wraps to first map from last`() {
        val r = MapRotation(listOf("A", "B", "C"), loops = true, currentIndex = 2)
        assertEquals("A", r.nextName())
    }

    @Test fun `multi loop - isMapCycleOver false at first map`() {
        assertFalse(MapRotation(listOf("A", "B"), loops = true, currentIndex = 0).isMapCycleOver)
    }

    @Test fun `multi loop - isMapCycleOver false at last map`() {
        assertFalse(MapRotation(listOf("A", "B"), loops = true, currentIndex = 1).isMapCycleOver)
    }

    @Test fun `multi loop - advance wraps from last to first`() {
        val r = MapRotation(listOf("A", "B"), loops = true, currentIndex = 1)
        assertEquals(0, r.advance().currentIndex)
    }
}
