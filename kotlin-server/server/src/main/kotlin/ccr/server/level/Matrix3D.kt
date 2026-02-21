package ccr.server.level

/**
 * 3x4 rotation+translation matrix, row-major, 12 floats = 48 bytes.
 * Layout: [row0:4floats, row1:4floats, row2:4floats]
 * Translation (position) is at column index 3: elements[3], elements[7], elements[11]
 * Matches C++ Matrix3D (wwmath/matrix3d.h).
 */
data class Matrix3D(val elements: FloatArray) {
    init { require(elements.size == 12) { "Matrix3D requires exactly 12 floats, got ${elements.size}" } }

    val position: Vector3 get() = Vector3(elements[3], elements[7], elements[11])

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix3D) return false
        return elements.contentEquals(other.elements)
    }
    override fun hashCode(): Int = elements.contentHashCode()

    companion object {
        val IDENTITY = Matrix3D(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f
        ))
    }
}
