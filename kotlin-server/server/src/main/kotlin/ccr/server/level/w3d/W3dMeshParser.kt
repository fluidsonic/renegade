package ccr.server.level.w3d

import ccr.server.level.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object W3dMeshParser {

    fun parse(meshChunk: ChunkReader): W3dMesh? {
        var name = ""
        var containerName = ""
        var attributes = 0u
        var numVertices = 0
        var numTris = 0
        val vertices = mutableListOf<Vector3>()
        val normals = mutableListOf<Vector3>()
        val triangles = mutableListOf<W3dTriangle>()
        val textureNames = mutableListOf<String>()

        meshChunk.forEachChunk { chunkId, _, reader ->
            when (chunkId) {
                W3dChunkIds.MESH_HEADER3 -> {
                    // W3dMeshHeader3Struct: version(4) + attributes(4) + meshName(16) + containerName(16)
                    //   + numTris(4) + numVertices(4) + ... = 120 bytes total
                    val bytes = reader.readBytes()
                    if (bytes.size >= 48) {
                        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        attributes = bb.getInt(4).toUInt()
                        name = readNullTerminated(bytes, 8, 16)
                        containerName = readNullTerminated(bytes, 24, 16)
                        numTris = bb.getInt(40)
                        numVertices = bb.getInt(44)
                    }
                }
                W3dChunkIds.VERTICES -> {
                    val bytes = reader.readBytes()
                    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    var offset = 0
                    while (offset + 12 <= bytes.size) {
                        vertices.add(Vector3(bb.getFloat(offset), bb.getFloat(offset + 4), bb.getFloat(offset + 8)))
                        offset += 12
                    }
                }
                W3dChunkIds.VERTEX_NORMALS -> {
                    val bytes = reader.readBytes()
                    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    var offset = 0
                    while (offset + 12 <= bytes.size) {
                        normals.add(Vector3(bb.getFloat(offset), bb.getFloat(offset + 4), bb.getFloat(offset + 8)))
                        offset += 12
                    }
                }
                W3dChunkIds.TRIANGLES -> {
                    // Each triangle: 3×uint32 (vertex indices) + uint32 surfaceType + 3×float32 normal + float32 dist = 32 bytes
                    val bytes = reader.readBytes()
                    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    var offset = 0
                    while (offset + 32 <= bytes.size) {
                        val i0 = bb.getInt(offset)
                        val i1 = bb.getInt(offset + 4)
                        val i2 = bb.getInt(offset + 8)
                        val surfType = bb.getInt(offset + 12)
                        val nx = bb.getFloat(offset + 16)
                        val ny = bb.getFloat(offset + 20)
                        val nz = bb.getFloat(offset + 24)
                        val dist = bb.getFloat(offset + 28)
                        triangles.add(W3dTriangle(i0, i1, i2, surfType, Vector3(nx, ny, nz), dist))
                        offset += 32
                    }
                }
                W3dChunkIds.TEXTURE_NAME -> {
                    val bytes = reader.readBytes()
                    val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
                    val len = if (nullIdx < 0) bytes.size else nullIdx
                    textureNames.add(String(bytes, 0, len, Charsets.ISO_8859_1))
                }
            }
        }

        return W3dMesh(name, containerName, attributes, numVertices, numTris, vertices, normals, triangles, textureNames)
    }

    private fun readNullTerminated(bytes: ByteArray, offset: Int, maxLen: Int): String {
        val end = minOf(offset + maxLen, bytes.size)
        val nullIdx = (offset until end).firstOrNull { bytes[it] == 0.toByte() } ?: end
        return String(bytes, offset, nullIdx - offset, Charsets.ISO_8859_1)
    }
}
