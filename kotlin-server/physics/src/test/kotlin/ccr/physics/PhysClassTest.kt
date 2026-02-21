package ccr.physics

import ccr.math.Matrix3D
import kotlin.test.*

class ConcretePhysClass : PhysClass() {
    override var transform: Matrix3D = Matrix3D.IDENTITY
}

class PhysClassTest {
    @Test
    fun collisionGroupBits() {
        val obj = ConcretePhysClass()
        obj.collisionGroup = 6
        assertEquals(6, obj.collisionGroup)
        obj.collisionGroup = 15
        assertEquals(15, obj.collisionGroup)
    }

    @Test
    fun isAsleepFlag() {
        val obj = ConcretePhysClass()
        assertFalse(obj.isAsleep)
        obj.isAsleep = true
        assertTrue(obj.isAsleep)
        obj.isAsleep = false
        assertFalse(obj.isAsleep)
    }

    @Test
    fun ignoreCount() {
        val obj = ConcretePhysClass()
        obj.ignoreCount = 3
        assertEquals(3, obj.ignoreCount)
        obj.ignoreCount = 0
        assertEquals(0, obj.ignoreCount)
    }

    @Test
    fun multipleFlags() {
        val obj = ConcretePhysClass()
        obj.collisionGroup = 5
        obj.isImmovable = true
        obj.isAsleep = true
        assertEquals(5, obj.collisionGroup)
        assertTrue(obj.isImmovable)
        assertTrue(obj.isAsleep)
    }

    @Test
    fun disabledFlags() {
        val obj = ConcretePhysClass()
        assertFalse(obj.isDisabled)
        obj.isDisabled = true
        assertTrue(obj.isDisabled)

        assertFalse(obj.isFrictionDisabled)
        obj.isFrictionDisabled = true
        assertTrue(obj.isFrictionDisabled)

        assertFalse(obj.isSimulationDisabled)
        obj.isSimulationDisabled = true
        assertTrue(obj.isSimulationDisabled)
    }

    @Test
    fun flagsDoNotInterfere() {
        val obj = ConcretePhysClass()
        obj.collisionGroup = 0xF
        obj.isImmovable = true
        obj.isAsleep = true
        obj.isDisabled = true
        obj.isFrictionDisabled = true
        obj.isSimulationDisabled = true
        obj.ignoreCount = 0xF

        assertEquals(0xF, obj.collisionGroup)
        assertTrue(obj.isImmovable)
        assertTrue(obj.isAsleep)
        assertTrue(obj.isDisabled)
        assertTrue(obj.isFrictionDisabled)
        assertTrue(obj.isSimulationDisabled)
        assertEquals(0xF, obj.ignoreCount)
    }

    @Test
    fun defaultTimestepBehavior() {
        val obj = ConcretePhysClass()
        assertFalse(obj.needsTimestep())
        // should not throw
        obj.timestep(0.1f)
        obj.postTimestepProcess()
    }
}
