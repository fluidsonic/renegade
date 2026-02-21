package ccr.physics.static

import ccr.math.Vector3

abstract class AccessiblePhysClass : StaticAnimPhysClass() {
    var isOpen: Boolean = false
    var accessZoneRadius: Float = 1.0f

    fun isInAccessZone(point: Vector3): Boolean {
        val pos = transform.translation
        val dx = point.x - pos.x; val dy = point.y - pos.y; val dz = point.z - pos.z
        val distSq = dx * dx + dy * dy + dz * dz
        return distSq <= accessZoneRadius * accessZoneRadius
    }

    abstract fun open()
    abstract fun close()
}
