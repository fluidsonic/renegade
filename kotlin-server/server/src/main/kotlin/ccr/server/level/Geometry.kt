package ccr.server.level

/**
 * Oriented bounding box — C++ OBBoxClass (wwmath/obbox.h).
 * basis: 9 floats (3x3 rotation matrix, row-major)
 * center: box center in world space
 * extent: half-extents along each local axis
 */
data class OBBox(
    val basis: FloatArray,  // 9 floats row-major
    val center: Vector3,
    val extent: Vector3,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OBBox) return false
        return basis.contentEquals(other.basis) && center == other.center && extent == other.extent
    }
    override fun hashCode(): Int = 31 * (31 * basis.contentHashCode() + center.hashCode()) + extent.hashCode()

    companion object {
        val EMPTY = OBBox(FloatArray(9), Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f))
    }
}

/**
 * Sphere — center + radius, used for BuildingGameObj collection spheres.
 */
data class Sphere(
    val center: Vector3,
    val radius: Float,
) {
    companion object {
        val EMPTY = Sphere(Vector3(0f, 0f, 0f), 0f)
    }
}
