package ccr.physics.vehicle

import ccr.math.Vector3
import kotlin.test.*

private class TestVehicle : VehiclePhysClass() {
    override fun allocSuspensionElement(): SuspensionElement = WVWheel()
    init {
        mass = 1000f
        computeInertia(2f, 4f, 1.5f)
        updateAuxiliaryState()
    }
}

class VehiclePhysClassTest {

    @Test
    fun fakeVehicleDoesNotMove() {
        val v = TestVehicle()
        v.isFake = true
        val posBefore = v.state.position
        v.timestep(0.1f)
        assertEquals(posBefore, v.state.position)
    }

    @Test
    fun addWheelIncreasesCount() {
        val v = TestVehicle()
        assertEquals(0, v.wheels.size)
        v.addWheel(Vector3(1f, 1f, -0.5f), SuspensionElement.ENGINE)
        assertEquals(1, v.wheels.size)
        assertTrue(v.wheels[0].hasFlag(SuspensionElement.ENGINE))
    }

    @Test
    fun wheelInheritsVehicleSpringConstants() {
        val v = TestVehicle()
        v.springConstant = 7000f
        v.dampingConstant = 700f
        v.springLength = 0.6f
        val w = v.addWheel(Vector3(0f, 0f, 0f))
        assertEquals(7000f, w.springConstant)
        assertEquals(700f, w.dampingConstant)
        assertEquals(0.6f, w.springRestLength)
    }
}
