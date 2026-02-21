package ccr.physics.ode

interface OdeSystem {
    fun getState(out: StateVector)
    fun setState(state: StateVector, startIndex: Int): Int
    fun computeDerivatives(t: Float, testState: StateVector?, dydt: StateVector, startIndex: Int): Int
}
