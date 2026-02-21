package ccr.math

data class OBBox(val center: Vector3, val extent: Vector3, val basis: Matrix3D = Matrix3D.IDENTITY) {
    fun computeAxisAlignedExtent(): Vector3 {
        val ax = Vector3(kotlin.math.abs(basis.m00), kotlin.math.abs(basis.m10), kotlin.math.abs(basis.m20))
        val ay = Vector3(kotlin.math.abs(basis.m01), kotlin.math.abs(basis.m11), kotlin.math.abs(basis.m21))
        val az = Vector3(kotlin.math.abs(basis.m02), kotlin.math.abs(basis.m12), kotlin.math.abs(basis.m22))
        return Vector3(
            ax.x * extent.x + ay.x * extent.y + az.x * extent.z,
            ax.y * extent.x + ay.y * extent.y + az.y * extent.z,
            ax.z * extent.x + ay.z * extent.y + az.z * extent.z,
        )
    }

    fun projectToAxis(axis: Vector3): Float {
        val r0 = Vector3(basis.m00, basis.m10, basis.m20)
        val r1 = Vector3(basis.m01, basis.m11, basis.m21)
        val r2 = Vector3(basis.m02, basis.m12, basis.m22)
        return kotlin.math.abs(r0.dot(axis)) * extent.x +
            kotlin.math.abs(r1.dot(axis)) * extent.y +
            kotlin.math.abs(r2.dot(axis)) * extent.z
    }
}
