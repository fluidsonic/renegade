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

    companion object {
        /** Parse from DoorPhysDefClass triggerZone wire format: basis(9f row-major) + center(3f) + extent(3f). */
        fun fromFloatArray(f: FloatArray): OBBox {
            require(f.size >= 15)
            return OBBox(
                center = Vector3(f[9], f[10], f[11]),
                extent = Vector3(f[12], f[13], f[14]),
                basis = Matrix3D(
                    m00 = f[0], m01 = f[1], m02 = f[2],
                    m10 = f[3], m11 = f[4], m12 = f[5],
                    m20 = f[6], m21 = f[7], m22 = f[8],
                ),
            )
        }
    }
}

/** Transform an OBBox by a Matrix3D: rotate basis, transform center, keep extent. */
fun transformOBBox(box: OBBox, tm: Matrix3D): OBBox {
    val r0 = tm.rotateVector(Vector3(box.basis.m00, box.basis.m01, box.basis.m02))
    val r1 = tm.rotateVector(Vector3(box.basis.m10, box.basis.m11, box.basis.m12))
    val r2 = tm.rotateVector(Vector3(box.basis.m20, box.basis.m21, box.basis.m22))
    return OBBox(
        center = tm.transformPoint(box.center),
        extent = box.extent,
        basis = Matrix3D(
            m00 = r0.x, m01 = r0.y, m02 = r0.z,
            m10 = r1.x, m11 = r1.y, m12 = r1.z,
            m20 = r2.x, m21 = r2.y, m22 = r2.z,
        ),
    )
}
