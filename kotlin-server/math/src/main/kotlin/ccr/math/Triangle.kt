package ccr.math

data class Triangle(val v0: Vector3, val v1: Vector3, val v2: Vector3) {
    val normal: Vector3 = run {
        val n = (v1 - v0).cross(v2 - v0)
        val len = n.length()
        if (len > 0f) n / len else Vector3.ZERO
    }
}
