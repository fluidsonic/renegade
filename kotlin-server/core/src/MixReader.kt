package ccr.server.mix

import ccr.server.crcStringi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads files out of a MIX1 archive (wwlib/mixfile.cpp).
 *
 * Wire layout:
 *   [0..3]   "MIX1" signature
 *   [4..7]   header_offset: int32 LE  — offset of the entry table
 *   [8..11]  names_offset:  int32 LE  — offset of the names section (not used for lookup)
 *   [12..15] unused: int32 LE = 0
 *   [16+]    embedded file data (each file at its recorded offset)
 *
 * At header_offset:
 *   file_count: int32 LE
 *   entries[file_count], sorted ascending by CRC (unsigned):
 *     crc:    uint32 LE  — CRC_Stringi(filename)
 *     offset: uint32 LE  — absolute from file start
 *     size:   uint32 LE
 */
class MixReader(private val data: ByteArray) {

    private data class Entry(val crc: Long, val offset: Int, val size: Int)

    private val entries: List<Entry>

    init {
        require(data.size >= 16) { "MIX data too short: ${data.size}" }
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val sig = ByteArray(4).also { buf.get(it) }
        require(sig.contentEquals("MIX1".toByteArray(Charsets.US_ASCII))) {
            "Not a MIX1 file (signature: ${sig.decodeToString()})"
        }

        val headerOffset = buf.getInt()   // [4..7]
        // names_offset and unused are not needed for lookup
        buf.position(headerOffset)

        val fileCount = buf.getInt()
        entries = (0 until fileCount).map {
            Entry(
                crc    = buf.getInt().toLong() and 0xFFFFFFFFL,
                offset = buf.getInt(),
                size   = buf.getInt(),
            )
        }
        // entries are already sorted by CRC in the file (MixFileCreator sorts them)
    }

    fun fileCount(): Int = entries.size

    /**
     * Returns the raw bytes of the embedded file with the given name, or null if not found.
     * Uses the same binary search as MixFileFactoryClass::Get_File (mixfile.cpp:225-238).
     */
    fun readFile(name: String): ByteArray? {
        val targetCrc = crcStringi(name).toLong() and 0xFFFFFFFFL
        val entry = binarySearch(targetCrc) ?: return null
        return data.copyOfRange(entry.offset, entry.offset + entry.size)
    }

    /** Binary search matching the C++ algorithm in mixfile.cpp:225-238. */
    private fun binarySearch(targetCrc: Long): Entry? {
        var base = 0
        var stride = entries.size
        while (stride > 0) {
            val pivot = stride / 2
            val tryEntry = entries[base + pivot]
            when {
                targetCrc < tryEntry.crc -> stride = pivot
                targetCrc == tryEntry.crc -> return tryEntry
                else -> { base = base + pivot + 1; stride -= pivot + 1 }
            }
        }
        return null
    }
}
