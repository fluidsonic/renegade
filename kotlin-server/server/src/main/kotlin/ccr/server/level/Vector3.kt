package ccr.server.level

data class Vector3(val x: Float, val y: Float, val z: Float) {
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
    }
}
