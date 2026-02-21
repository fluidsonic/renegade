package ccr.server.level.w3d

import ccr.server.level.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object W3dHierarchyParser {

    fun parse(hierarchyChunk: ChunkReader): W3dHierarchy? {
        var name = ""
        var centerPos = Vector3.ZERO
        var numPivots = 0
        val pivots = mutableListOf<W3dPivot>()

        hierarchyChunk.forEachChunk { chunkId, _, reader ->
            when (chunkId) {
                W3dChunkIds.HIERARCHY_HEADER -> {
                    // W3dHierarchyStruct: version(4) + name(16) + numPivots(4) + center(12) = 36 bytes
                    val bytes = reader.readBytes()
                    if (bytes.size >= 36) {
                        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        name = readNullTerminated(bytes, 4, 16)
                        numPivots = bb.getInt(20)
                        centerPos = Vector3(bb.getFloat(24), bb.getFloat(28), bb.getFloat(32))
                    }
                }
                W3dChunkIds.PIVOTS -> {
                    // W3dPivotStruct: name(16) + parentIdx(4) + translation(12) + eulerAngles(12) + rotation(16) = 60 bytes each
                    val bytes = reader.readBytes()
                    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                    var offset = 0
                    while (offset + 60 <= bytes.size) {
                        val pivotName = readNullTerminated(bytes, offset, 16)
                        val parentIdx = bb.getInt(offset + 16)
                        val tx = bb.getFloat(offset + 20)
                        val ty = bb.getFloat(offset + 24)
                        val tz = bb.getFloat(offset + 28)
                        // eulerAngles at offset+32 (12 bytes) — skip, use quaternion instead
                        val qx = bb.getFloat(offset + 44)
                        val qy = bb.getFloat(offset + 48)
                        val qz = bb.getFloat(offset + 52)
                        val qw = bb.getFloat(offset + 56)
                        pivots.add(W3dPivot(pivotName, parentIdx, Vector3(tx, ty, tz), qx, qy, qz, qw))
                        offset += 60
                    }
                }
            }
        }

        if (name.isEmpty()) return null
        return W3dHierarchy(name, centerPos, pivots)
    }

    private fun readNullTerminated(bytes: ByteArray, offset: Int, maxLen: Int): String {
        val end = minOf(offset + maxLen, bytes.size)
        val nullIdx = (offset until end).firstOrNull { bytes[it] == 0.toByte() } ?: end
        return String(bytes, offset, nullIdx - offset, Charsets.ISO_8859_1)
    }
}
