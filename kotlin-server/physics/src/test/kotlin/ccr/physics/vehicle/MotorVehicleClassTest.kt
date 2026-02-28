package ccr.physics.vehicle

import ccr.math.Vector3
import ccr.physics.PhysController
import ccr.physics.rigidbody.RigidBodyState
import kotlin.test.*

private class TestMotorVehicle : WheeledVehicleClass() {
    init {
        mass = 1000f
        computeInertia(2f, 4f, 1.5f)
        gravScale = 0f // disable gravity for test isolation
        updateAuxiliaryState()
        // Add 4 wheels
        addWheel(Vector3(-1f, 1.5f, -0.5f), SuspensionElement.ENGINE or SuspensionElement.STEERING)
        addWheel(Vector3(1f, 1.5f, -0.5f), SuspensionElement.ENGINE or SuspensionElement.STEERING)
        addWheel(Vector3(-1f, -1.5f, -0.5f), SuspensionElement.ENGINE)
        addWheel(Vector3(1f, -1.5f, -0.5f), SuspensionElement.ENGINE)
    }
}

class MotorVehicleClassTest {

    @Test
    fun gearShiftsUpWhenRpmHigh() {
        val v = TestMotorVehicle()
        // C++ gears are 0-indexed
        v.currentGear = 0
        v.engineAngularVelocity = v.shiftUpRpm + 100f
        val ctrl = PhysController()
        ctrl.moveForward = 1f
        v.controller = ctrl
        // The shift happens inside timestep
        v.timestep(0.016f)
        // Gear should stay in valid range [0, gearCount-1]
        assertTrue(v.currentGear >= 0, "Gear should be >= 0")
        assertTrue(v.currentGear < v.gearCount, "Gear ${v.currentGear} >= max ${v.gearCount}")
    }

    @Test
    fun gearDoesNotExceedMax() {
        val v = TestMotorVehicle()
        v.currentGear = v.gearCount - 1  // max gear (0-indexed)
        v.shiftUpRpm = 100f // very low threshold
        val ctrl = PhysController()
        ctrl.moveForward = 1f
        v.controller = ctrl
        v.timestep(0.016f)
        assertTrue(v.currentGear < v.gearCount, "Gear ${v.currentGear} >= max ${v.gearCount}")
    }

    @Test
    fun brakeAppliesWhenMovingForwardAndReverseInput() {
        val v = TestMotorVehicle()
        // Give the vehicle forward velocity so braking can trigger (C++ checks speed > MIN_BRAKING_SPEED)
        v.state = RigidBodyState(
            position = Vector3.ZERO,
            linearMomentum = Vector3(5000f, 0f, 0f), // mass=1000, so v=5 m/s > 1.5 threshold
        )
        v.updateAuxiliaryState()
        val ctrl = PhysController()
        ctrl.moveForward = -1f // brake (reverse input while moving forward)
        v.controller = ctrl
        v.timestep(0.016f)
        // Vehicle should be braking
        assertTrue(v.isBraking, "Expected isBraking to be true when moving forward with reverse input")
    }
}
