package ccr.server.level.w3d

import ccr.server.mix.ChunkReader

data class W3dFile(
    val meshes: List<W3dMesh> = emptyList(),
    val hierarchies: List<W3dHierarchy> = emptyList(),
    val animations: List<W3dAnimation> = emptyList(),
)

object W3dFileParser {

    fun parse(data: ByteArray): W3dFile {
        val reader = ChunkReader(data)
        val meshes = mutableListOf<W3dMesh>()
        val hierarchies = mutableListOf<W3dHierarchy>()
        val animations = mutableListOf<W3dAnimation>()

        reader.forEachChunk { chunkId, _, chunkReader ->
            when (chunkId) {
                W3dChunkIds.MESH -> W3dMeshParser.parse(chunkReader)?.let { meshes.add(it) }
                W3dChunkIds.HIERARCHY -> W3dHierarchyParser.parse(chunkReader)?.let { hierarchies.add(it) }
                W3dChunkIds.ANIMATION, W3dChunkIds.COMPRESSED_ANIMATION -> {
                    W3dAnimationParser.parse(chunkReader)?.let { animations.add(it) }
                }
            }
        }

        return W3dFile(meshes, hierarchies, animations)
    }
}
