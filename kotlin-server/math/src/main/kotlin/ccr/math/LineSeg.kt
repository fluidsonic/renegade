package ccr.math

data class LineSeg(val p0: Vector3, val p1: Vector3) {
    val dp: Vector3 = p1 - p0
    val length: Float = dp.length()
    val dir: Vector3 = if (length > 0f) dp / length else Vector3.ZERO

    fun pointAt(t: Float) = p0 + dp * t
    fun transform(m: Matrix3D) = LineSeg(m.transformPoint(p0), m.transformPoint(p1))
}
