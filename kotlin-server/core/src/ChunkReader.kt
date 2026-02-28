package ccr.server.mix

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads W3D/LSD chunk-based data (wwlib/chunkio.h).
 *
 * Chunk header (8 bytes):
 *   chunkType: uint32 LE
 *   chunkSize: uint32 LE  — bit 31 = has_sub_chunks; lower 31 bits = data size in bytes
 *
 * MicroChunk header (2 bytes):
 *   type: uint8
 *   size: uint8
 */
class ChunkReader(
    private val data: ByteArray,
    private val offset: Int = 0,
    private val length: Int = data.size - offset,
) {

    private val buf: ByteBuffer
        get() = ByteBuffer.wrap(data, offset, length).order(ByteOrder.LITTLE_ENDIAN)

    /**
     * Iterates over all top-level chunks in this reader's slice.
     * [handler] receives the chunk ID, whether the chunk is a container (bit 31 set), and a
     * ChunkReader scoped to the chunk's data bytes.
     */
    fun forEachChunk(handler: (id: UInt, isContainer: Boolean, reader: ChunkReader) -> Unit) {
        val b = buf
        var pos = 0
        while (pos + 8 <= length) {
            val chunkType = b.getInt(offset + pos).toLong() and 0xFFFFFFFFL
            val chunkSizeRaw = b.getInt(offset + pos + 4).toLong() and 0xFFFFFFFFL
            val isContainer = (chunkSizeRaw and 0x80000000L) != 0L
            val dataSize = (chunkSizeRaw and 0x7FFFFFFFL).toInt()
            pos += 8
            if (pos + dataSize > length) break
            val id = chunkType.toUInt()
            val child = ChunkReader(data, offset + pos, dataSize)
            handler(id, isContainer, child)
            pos += dataSize
        }
    }

    /**
     * Finds the first chunk with the given ID in this reader's slice (direct children only), or null.
     */
    fun findChunk(id: UInt): ChunkReader? {
        var result: ChunkReader? = null
        forEachChunk { cid, _, reader ->
            if (result == null && cid == id) result = reader
        }
        return result
    }

    /**
     * Recursively searches this reader and all nested container chunks for the first chunk
     * with the given ID. Breadth-first among siblings, depth-first into containers.
     */
    fun findChunkRecursive(id: UInt): ChunkReader? {
        var result: ChunkReader? = null
        forEachChunk { cid, isContainer, reader ->
            if (result != null) return@forEachChunk
            if (cid == id) {
                result = reader
            } else if (isContainer) {
                result = reader.findChunkRecursive(id)
            }
        }
        return result
    }

    /**
     * Reads micro-chunks (type: uint8, size: uint8) from this reader's data.
     * Returns the data bytes of the first micro-chunk matching [id], or null.
     */
    fun findMicroChunk(id: Int): ByteArray? {
        val b = buf
        var pos = 0
        while (pos + 2 <= length) {
            val mType = b.get(offset + pos).toInt() and 0xFF
            val mSize = b.get(offset + pos + 1).toInt() and 0xFF
            pos += 2
            if (mType == id && pos + mSize <= length) {
                return data.copyOfRange(offset + pos, offset + pos + mSize)
            }
            pos += mSize
        }
        return null
    }

    /**
     * Returns the data bytes of ALL micro-chunks matching [id], in order.
     * Useful for repeated micro-chunks (e.g. lists serialized as multiple entries with the same ID).
     */
    fun findAllMicroChunks(id: Int): List<ByteArray> {
        val results = mutableListOf<ByteArray>()
        val b = buf
        var pos = 0
        while (pos + 2 <= length) {
            val mType = b.get(offset + pos).toInt() and 0xFF
            val mSize = b.get(offset + pos + 1).toInt() and 0xFF
            pos += 2
            if (mType == id && pos + mSize <= length) {
                results += data.copyOfRange(offset + pos, offset + pos + mSize)
            }
            pos += mSize
        }
        return results
    }

    /**
     * Iterates over all micro-chunks in this reader's data.
     * [handler] receives the micro-chunk type ID and a ByteArray of its data bytes.
     */
    fun forEachMicroChunk(handler: (id: Int, data: ByteArray) -> Unit) {
        val b = buf
        var pos = 0
        while (pos + 2 <= length) {
            val mType = b.get(offset + pos).toInt() and 0xFF
            val mSize = b.get(offset + pos + 1).toInt() and 0xFF
            pos += 2
            if (pos + mSize <= length) {
                handler(mType, data.copyOfRange(offset + pos, offset + pos + mSize))
            }
            pos += mSize
        }
    }

    /** Reads a little-endian int32 from the start of this chunk's data. */
    fun readInt(): Int = buf.getInt(offset)

    /** Reads a little-endian float32 from the start of this chunk's data. */
    fun readFloat(): Float = buf.getFloat(offset)

    /** Reads a little-endian float32 at [byteOffset] bytes into this chunk's data. */
    fun readFloat(byteOffset: Int): Float = buf.getFloat(offset + byteOffset)

    /** Returns the raw bytes of this chunk's data. */
    fun readBytes(count: Int = length): ByteArray = data.copyOfRange(offset, offset + count.coerceAtMost(length))
}
