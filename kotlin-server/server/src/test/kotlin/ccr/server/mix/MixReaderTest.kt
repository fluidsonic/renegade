package ccr.server.mix

import ccr.server.crcStringi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for MixReader — parses MIX1 format archives.
 *
 * MIX1 layout:
 *   [0..3]   "MIX1"
 *   [4..7]   header_offset (LE int32)
 *   [8..11]  names_offset  (LE int32)
 *   [12..15] unused = 0
 *   [16+]    file data
 *   [header_offset]  file_count (LE int32)
 *   [header_offset+4]  entries: (crc uint32, offset uint32, size uint32) * file_count  (sorted by CRC)
 */
class MixReaderTest {

    // Helpers for building a minimal MIX1 in memory

    private fun leInt(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )

    private fun leUInt(v: Long): ByteArray = leInt(v.toInt())

    /**
     * Builds a MIX1 file in memory containing the given (filename → content) pairs.
     * Files are placed starting at offset 16 in order, then the header table follows.
     */
    private fun buildMix(vararg files: Pair<String, ByteArray>): ByteArray {
        // Sort by CRC ascending (unsigned) as MixFileCreator does
        val sorted = files.map { (name, content) ->
            Triple(name, content, crcStringi(name).toLong() and 0xFFFFFFFFL)
        }.sortedBy { it.third }

        val parts = mutableListOf<ByteArray>()

        // Header placeholder (16 bytes)
        parts += "MIX1".toByteArray(Charsets.US_ASCII)
        parts += leInt(0) // header_offset placeholder
        parts += leInt(0) // names_offset placeholder
        parts += leInt(0) // unused

        // File data (each at its recorded offset)
        data class EntryInfo(val crc: Long, val offset: Int, val size: Int)
        val entries = mutableListOf<EntryInfo>()
        var cursor = 16
        for ((name, content, crc) in sorted) {
            entries += EntryInfo(crc, cursor, content.size)
            parts += content
            cursor += content.size
        }

        // Header table
        val headerOffset = cursor
        parts += leInt(sorted.size)
        for (e in entries) {
            parts += leUInt(e.crc)
            parts += leInt(e.offset)
            parts += leInt(e.size)
        }

        val namesOffset = headerOffset + 4 + sorted.size * 12

        // Assemble
        val raw = parts.fold(ByteArray(0)) { acc, b -> acc + b }

        // Patch header_offset and names_offset
        val result = raw.copyOf()
        leInt(headerOffset).copyInto(result, 4)
        leInt(namesOffset).copyInto(result, 8)
        return result
    }

    @Test
    fun `reads a single embedded file by name`() {
        val content = "Hello, MIX!".toByteArray()
        val mix = buildMix("test.txt" to content)
        val reader = MixReader(mix)
        val result = reader.readFile("test.txt")
        assertNotNull(result)
        assertEquals(content.toList(), result.toList())
    }

    @Test
    fun `lookup is case-insensitive via crcStringi`() {
        val content = byteArrayOf(1, 2, 3)
        // Store as uppercase, look up with mixed case
        val mix = buildMix("FILE.DAT" to content)
        val reader = MixReader(mix)
        // crcStringi uppercases both sides, so these should all match
        assertNotNull(reader.readFile("FILE.DAT"))
        assertNotNull(reader.readFile("file.dat"))
        assertNotNull(reader.readFile("File.Dat"))
    }

    @Test
    fun `returns null for a file not in the archive`() {
        val mix = buildMix("present.dat" to byteArrayOf(0x42))
        val reader = MixReader(mix)
        assertNull(reader.readFile("missing.dat"))
    }

    @Test
    fun `binary search finds correct entry among multiple files`() {
        val fileA = "alpha.dat" to byteArrayOf(0xAA.toByte())
        val fileB = "beta.dat"  to byteArrayOf(0xBB.toByte())
        val fileC = "gamma.dat" to byteArrayOf(0xCC.toByte())
        val mix = buildMix(fileA, fileB, fileC)
        val reader = MixReader(mix)

        assertEquals(listOf(0xAA.toByte()), reader.readFile("alpha.dat")?.toList())
        assertEquals(listOf(0xBB.toByte()), reader.readFile("beta.dat")?.toList())
        assertEquals(listOf(0xCC.toByte()), reader.readFile("gamma.dat")?.toList())
        assertNull(reader.readFile("delta.dat"))
    }

    @Test
    fun `fileCount returns correct number of entries`() {
        val mix = buildMix(
            "a.dat" to byteArrayOf(1),
            "b.dat" to byteArrayOf(2),
            "c.dat" to byteArrayOf(3),
        )
        assertEquals(3, MixReader(mix).fileCount())
    }

    @Test
    fun `rejects non-MIX1 data`() {
        val bad = byteArrayOf(*"BAD!".toByteArray(), *ByteArray(12))
        assertThrows<Exception> { MixReader(bad) }
    }

    @Test
    fun `reads multi-byte file content correctly`() {
        val content = ByteArray(256) { it.toByte() }
        val mix = buildMix("big.bin" to content)
        val result = MixReader(mix).readFile("big.bin")
        assertEquals(content.toList(), result?.toList())
    }
}
