package ccr.math

data class Plane(val normal: Vector3, val dist: Float) {
    fun distanceTo(point: Vector3): Float = normal.dot(point) - dist

    fun intersect(p0: Vector3, p1: Vector3): Float? {
        val d0 = distanceTo(p0)
        val d1 = distanceTo(p1)
        val denom = d0 - d1
        return if (kotlin.math.abs(denom) < 1e-10f) null else d0 / denom
    }

    companion object {
        fun fromNormalAndPoint(normal: Vector3, point: Vector3) = Plane(normal, normal.dot(point))
    }
}
