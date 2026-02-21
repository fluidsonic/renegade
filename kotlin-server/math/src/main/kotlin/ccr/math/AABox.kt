package ccr.math

data class AABox(val center: Vector3, val extent: Vector3) {
    val min: Vector3 get() = center - extent
    val max: Vector3 get() = center + extent

    fun contains(point: Vector3): Boolean =
        point.x >= min.x && point.x <= max.x &&
            point.y >= min.y && point.y <= max.y &&
            point.z >= min.z && point.z <= max.z

    fun contains(box: AABox): Boolean = contains(box.min) && contains(box.max)

    fun overlaps(box: AABox): Boolean {
        val mn = min; val mx = max; val bmn = box.min; val bmx = box.max
        return mx.x >= bmn.x && mn.x <= bmx.x &&
            mx.y >= bmn.y && mn.y <= bmx.y &&
            mx.z >= bmn.z && mn.z <= bmx.z
    }

    fun addBox(box: AABox): AABox = fromMinMax(
        Vector3(minOf(min.x, box.min.x), minOf(min.y, box.min.y), minOf(min.z, box.min.z)),
        Vector3(maxOf(max.x, box.max.x), maxOf(max.y, box.max.y), maxOf(max.z, box.max.z)),
    )

    fun addPoint(v: Vector3): AABox = fromMinMax(
        Vector3(minOf(min.x, v.x), minOf(min.y, v.y), minOf(min.z, v.z)),
        Vector3(maxOf(max.x, v.x), maxOf(max.y, v.y), maxOf(max.z, v.z)),
    )

    fun translate(v: Vector3) = AABox(center + v, extent)

    companion object {
        fun fromMinMax(mn: Vector3, mx: Vector3) = AABox((mn + mx) * 0.5f, (mx - mn) * 0.5f)

        fun fromLineSeg(ls: LineSeg) = fromMinMax(
            Vector3(minOf(ls.p0.x, ls.p1.x), minOf(ls.p0.y, ls.p1.y), minOf(ls.p0.z, ls.p1.z)),
            Vector3(maxOf(ls.p0.x, ls.p1.x), maxOf(ls.p0.y, ls.p1.y), maxOf(ls.p0.z, ls.p1.z)),
        )
    }
}
