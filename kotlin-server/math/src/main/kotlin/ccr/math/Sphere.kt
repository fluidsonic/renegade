package ccr.math

// C++: SphereClass (wwmath/sphere.h)
data class Sphere(
    val center: Vector3,
    val radius: Float,
) {
    companion object {
        val EMPTY = Sphere(Vector3.ZERO, 0f)
    }
}
