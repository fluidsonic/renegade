package ccr.physics.collision

class CollisionGroupMatrix {
    private val matrix = BooleanArray(256)

    private fun idx(g0: Int, g1: Int) = (g0 and 0xF) or ((g1 and 0xF) shl 4)

    fun doGroupsCollide(g0: Int, g1: Int): Boolean = matrix[idx(g0, g1)]

    fun enableCollision(g0: Int, g1: Int) {
        matrix[idx(g0, g1)] = true
        matrix[idx(g1, g0)] = true
    }

    fun disableCollision(g0: Int, g1: Int) {
        matrix[idx(g0, g1)] = false
        matrix[idx(g1, g0)] = false
    }

    fun enableAll() {
        for (i in 0 until 256) matrix[i] = true
    }

    fun disableAll() {
        for (i in 0 until 256) matrix[i] = false
    }
}
