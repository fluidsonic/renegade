package ccr.server.level.w3d

import ccr.server.level.Vector3

data class W3dTriangle(
    val i0: Int,
    val i1: Int,
    val i2: Int,
    val surfaceType: Int,
    val normal: Vector3,
    val dist: Float,
)

data class W3dMesh(
    val name: String,
    val containerName: String,
    val vertexCount: Int,
    val faceCount: Int,
    val vertices: List<Vector3>,
    val normals: List<Vector3>,
    val triangles: List<W3dTriangle>,
    val textureNames: List<String>,
)
