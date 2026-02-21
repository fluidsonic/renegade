package ccr.server.level.w3d

import ccr.server.level.Vector3

data class W3dPivot(
    val name: String,
    val parentId: Int,
    val translation: Vector3,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val rotZ: Float = 0f,
    val rotW: Float = 1f,
)

data class W3dHierarchy(
    val name: String,
    val centerPos: Vector3,
    val pivots: List<W3dPivot>,
)
