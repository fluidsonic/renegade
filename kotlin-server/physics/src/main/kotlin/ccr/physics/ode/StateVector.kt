package ccr.physics.ode

class StateVector {
    private var data = FloatArray(64)
    var count: Int = 0
        internal set

    fun add(f: Float) {
        ensureCapacity(count + 1)
        data[count++] = f
    }

    operator fun get(i: Int): Float = data[i]
    operator fun set(i: Int, v: Float) { data[i] = v }

    fun reset() { count = 0 }

    fun ensureCapacity(n: Int) {
        if (n > data.size) {
            data = data.copyOf(maxOf(n, data.size * 2))
        }
    }
}
