package ccr.physics.ode

object Integrator {
    // Pre-allocated scratch vectors to avoid allocation per frame
    private val scratch1 = StateVector()
    private val scratch2 = StateVector()
    private val scratch3 = StateVector()
    private val scratch4 = StateVector()
    private val scratch5 = StateVector()
    private val scratch6 = StateVector()

    fun eulerIntegrate(sys: OdeSystem, t: Float, dt: Float) {
        val y = scratch1; y.reset()
        val dydt = scratch2; dydt.reset()
        sys.getState(y)
        val n = y.count
        dydt.ensureCapacity(n); dydt.count = n
        sys.computeDerivatives(t, null, dydt, 0)
        val out = scratch3; out.ensureCapacity(n); out.count = n
        for (i in 0 until n) out[i] = y[i] + dt * dydt[i]
        sys.setState(out, 0)
    }

    fun midpointIntegrate(sys: OdeSystem, t: Float, dt: Float) {
        val y = scratch1; y.reset()
        sys.getState(y)
        val n = y.count
        val k1 = scratch2; k1.ensureCapacity(n); k1.count = n
        sys.computeDerivatives(t, null, k1, 0)
        val yMid = scratch3; yMid.ensureCapacity(n); yMid.count = n
        for (i in 0 until n) yMid[i] = y[i] + dt * 0.5f * k1[i]
        val k2 = scratch4; k2.ensureCapacity(n); k2.count = n
        sys.computeDerivatives(t + dt * 0.5f, yMid, k2, 0)
        val out = scratch5; out.ensureCapacity(n); out.count = n
        for (i in 0 until n) out[i] = y[i] + dt * k2[i]
        sys.setState(out, 0)
    }

    fun rungeKuttaIntegrate(sys: OdeSystem, t: Float, dt: Float) {
        val y = scratch1; y.reset()
        sys.getState(y)
        val n = y.count
        val k1 = scratch2; k1.ensureCapacity(n); k1.count = n
        sys.computeDerivatives(t, null, k1, 0)
        val tmp = scratch3; tmp.ensureCapacity(n); tmp.count = n
        for (i in 0 until n) tmp[i] = y[i] + dt * 0.5f * k1[i]
        val k2 = scratch4; k2.ensureCapacity(n); k2.count = n
        sys.computeDerivatives(t + dt * 0.5f, tmp, k2, 0)
        for (i in 0 until n) tmp[i] = y[i] + dt * 0.5f * k2[i]
        val k3 = scratch5; k3.ensureCapacity(n); k3.count = n
        sys.computeDerivatives(t + dt * 0.5f, tmp, k3, 0)
        for (i in 0 until n) tmp[i] = y[i] + dt * k3[i]
        val k4 = scratch6; k4.ensureCapacity(n); k4.count = n
        sys.computeDerivatives(t + dt, tmp, k4, 0)
        // Reuse tmp for output
        for (i in 0 until n) tmp[i] = y[i] + (dt / 6f) * (k1[i] + 2f * k2[i] + 2f * k3[i] + k4[i])
        sys.setState(tmp, 0)
    }

    fun rungeKutta5Integrate(sys: OdeSystem, t: Float, dt: Float) {
        // Cash-Karp 5th order — needs more scratch than pre-allocated, so allocate locally
        val y = StateVector(); y.reset()
        sys.getState(y)
        val n = y.count
        val tmp = StateVector(); tmp.ensureCapacity(n); tmp.count = n

        // k1
        val k1 = StateVector(); k1.ensureCapacity(n); k1.count = n
        sys.computeDerivatives(t, null, k1, 0)
        // k2
        for (i in 0 until n) tmp[i] = y[i] + dt * (1f / 5f) * k1[i]
        val k2 = StateVector(); k2.ensureCapacity(n); k2.count = n
        sys.computeDerivatives(t + dt / 5f, tmp, k2, 0)
        // k3
        for (i in 0 until n) tmp[i] = y[i] + dt * ((3f / 40f) * k1[i] + (9f / 40f) * k2[i])
        val k3 = StateVector(); k3.ensureCapacity(n); k3.count = n
        sys.computeDerivatives(t + 3f * dt / 10f, tmp, k3, 0)
        // k4
        for (i in 0 until n) tmp[i] = y[i] + dt * ((3f / 10f) * k1[i] - (9f / 10f) * k2[i] + (6f / 5f) * k3[i])
        val k4 = StateVector(); k4.ensureCapacity(n); k4.count = n
        sys.computeDerivatives(t + 3f * dt / 5f, tmp, k4, 0)
        // k5
        for (i in 0 until n) tmp[i] = y[i] + dt * ((-11f / 54f) * k1[i] + (5f / 2f) * k2[i] - (70f / 27f) * k3[i] + (35f / 27f) * k4[i])
        val k5 = StateVector(); k5.ensureCapacity(n); k5.count = n
        sys.computeDerivatives(t + dt, tmp, k5, 0)
        // k6
        for (i in 0 until n) tmp[i] = y[i] + dt * ((1631f / 55296f) * k1[i] + (175f / 512f) * k2[i] + (575f / 13824f) * k3[i] + (44275f / 110592f) * k4[i] + (253f / 4096f) * k5[i])
        val k6 = StateVector(); k6.ensureCapacity(n); k6.count = n
        sys.computeDerivatives(t + 7f * dt / 8f, tmp, k6, 0)
        // 5th order result
        val out = StateVector(); out.ensureCapacity(n); out.count = n
        for (i in 0 until n) out[i] = y[i] + dt * ((37f / 378f) * k1[i] + (250f / 621f) * k3[i] + (125f / 594f) * k4[i] + (512f / 1771f) * k6[i])
        sys.setState(out, 0)
    }
}
