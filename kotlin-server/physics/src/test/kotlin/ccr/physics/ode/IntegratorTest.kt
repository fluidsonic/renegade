package ccr.physics.ode

import kotlin.test.*
import kotlin.math.abs

// Simple free-fall: state = [y, v], derivatives = [v, -9.8]
private class FreeFallSystem : OdeSystem {
    var y: Float = 0f
    var v: Float = 0f

    override fun getState(out: StateVector) {
        out.reset()
        out.add(y); out.add(v)
    }

    override fun setState(state: StateVector, startIndex: Int): Int {
        y = state[startIndex]; v = state[startIndex + 1]; return startIndex + 2
    }

    override fun computeDerivatives(t: Float, testState: StateVector?, dydt: StateVector, startIndex: Int): Int {
        val curV = testState?.get(startIndex + 1) ?: v
        dydt[startIndex] = curV
        dydt[startIndex + 1] = -9.8f
        return startIndex + 2
    }
}

class IntegratorTest {
    @Test fun eulerFreeFall() {
        val sys = FreeFallSystem()
        repeat(100) { Integrator.eulerIntegrate(sys, it * 0.01f, 0.01f) }
        // Euler has ~5% error with 100 steps
        assertTrue(abs(sys.y - (-4.9f)) < 0.5f, "Euler: y=${sys.y}, expected ~ -4.9")
        assertTrue(abs(sys.v - (-9.8f)) < 0.1f, "Euler: v=${sys.v}, expected ~ -9.8")
    }

    @Test fun midpointFreeFall() {
        val sys = FreeFallSystem()
        repeat(100) { Integrator.midpointIntegrate(sys, it * 0.01f, 0.01f) }
        assertTrue(abs(sys.y - (-4.9f)) < 0.05f, "Midpoint: y=${sys.y}, expected ~ -4.9")
    }

    @Test fun rk4FreeFall() {
        val sys = FreeFallSystem()
        repeat(100) { Integrator.rungeKuttaIntegrate(sys, it * 0.01f, 0.01f) }
        assertTrue(abs(sys.y - (-4.9f)) < 0.001f, "RK4: y=${sys.y}, expected ~ -4.9")
    }

    @Test fun rk5FreeFall() {
        val sys = FreeFallSystem()
        repeat(100) { Integrator.rungeKutta5Integrate(sys, it * 0.01f, 0.01f) }
        assertTrue(abs(sys.y - (-4.9f)) < 0.001f, "RK5: y=${sys.y}, expected ~ -4.9")
    }
}
