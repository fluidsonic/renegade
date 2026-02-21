package ccr.server.level.w3d

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object W3dAnimationParser {

    fun parse(animChunk: ChunkReader): W3dAnimation? {
        var name = ""
        var hierarchyName = ""
        var numFrames = 0
        var frameRate = 0
        val channels = mutableListOf<W3dAnimationChannel>()

        animChunk.forEachChunk { chunkId, _, reader ->
            when (chunkId) {
                W3dChunkIds.ANIMATION_HEADER -> {
                    // W3dAnimHeaderStruct: version(4) + name(16) + hierarchyName(16) + numFrames(4) + frameRate(4) = 44 bytes
                    val bytes = reader.readBytes()
                    if (bytes.size >= 44) {
                        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        name = readNullTerminated(bytes, 4, 16)
                        hierarchyName = readNullTerminated(bytes, 20, 16)
                        numFrames = bb.getInt(36)
                        frameRate = bb.getInt(40)
                    }
                }
                W3dChunkIds.COMPRESSED_ANIMATION_HEADER -> {
                    // W3dCompressedAnimHeaderStruct: version(4) + name(16) + hierarchyName(16) + numFrames(4) + frameRate(uint16) + flavor(uint16) = 44 bytes
                    val bytes = reader.readBytes()
                    if (bytes.size >= 44) {
                        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        name = readNullTerminated(bytes, 4, 16)
                        hierarchyName = readNullTerminated(bytes, 20, 16)
                        numFrames = bb.getInt(36)
                        frameRate = bb.getShort(40).toInt() and 0xFFFF
                    }
                }
                W3dChunkIds.ANIMATION_CHANNEL -> {
                    // W3dAnimChannelStruct: firstFrame(u16) + lastFrame(u16) + vectorLen(u16) + flags(u16) + pivot(u16) + pad(u16) + data[...] = 12 + N*4
                    val bytes = reader.readBytes()
                    if (bytes.size >= 12) {
                        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        val firstFrame = bb.getShort(0).toInt() and 0xFFFF
                        val lastFrame = bb.getShort(2).toInt() and 0xFFFF
                        val vectorLen = bb.getShort(4).toInt() and 0xFFFF
                        val flags = bb.getShort(6).toInt() and 0xFFFF
                        val pivotIdx = bb.getShort(8).toInt() and 0xFFFF
                        val numFloats = (lastFrame - firstFrame + 1) * vectorLen
                        val data = FloatArray(numFloats)
                        for (i in 0 until numFloats) {
                            val off = 12 + i * 4
                            if (off + 4 <= bytes.size) {
                                data[i] = bb.getFloat(off)
                            }
                        }
                        channels.add(W3dAnimationChannel(firstFrame, lastFrame, vectorLen, flags, pivotIdx, data))
                    }
                }
            }
        }

        if (name.isEmpty()) return null
        return W3dAnimation(name, hierarchyName, numFrames, frameRate, channels)
    }

    private fun readNullTerminated(bytes: ByteArray, offset: Int, maxLen: Int): String {
        val end = minOf(offset + maxLen, bytes.size)
        val nullIdx = (offset until end).firstOrNull { bytes[it] == 0.toByte() } ?: end
        return String(bytes, offset, nullIdx - offset, Charsets.ISO_8859_1)
    }
}
