package ccr.server

import ccr.server.mix.MixReader
import ccr.server.mix.extractLevelExtents
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Computes the Renegade 1.037 ExeKey and related CRC values from game data files.
 *
 * Run via: ./gradlew :server:run -PmainClass=ccr.server.ComputeExeKeyKt
 *
 * ExeKey = CRC32("RENEGADE {build} strings.tdb {stringsVersion} ") XOR data_file_crc
 * ExeCRC = CRC32("RENEGADE {build}")                                (no trailing space)
 * StringsCRC = CRC32("strings.tdb {stringsVersion}")                (no trailing space)
 *
 * In Export_Tier_1_Data:
 *   packet.Add(VersionNumber) = ExeKey       (Set_Version_Number(cNetwork::Get_Exe_Key()))
 *   packet.Add(Get_Exe_CRC()) = ExeCRC
 *   packet.Add(Get_Strings_CRC()) = StringsCRC
 */

// Build number stamped into Game.exe via VerStamp.asm ("Insert1Build2Number3Here4   ")
private const val BUILD = 838u

// Data files whose contents are CRC'd to form data_file_crc (from cnetwork.cpp Get_Data_Files_CRC).
// These are the decoded names (original source obfuscates with XOR 0x5).
private val DATA_FILES = listOf(
    "objects.ddb", "armor.ini", "bones.ini", "surfaceeffects.ini", "cameras.ini",
    "c_nod_mg_l0.w3d", "c_nod_rk_l0.w3d", "c_nod_fl_l0.w3d", "c_nod_en_l0.w3d",
    "c_nod_mgo_l0.w3d", "c_nod_rko_l0.w3d", "c_nod_chemt_l0.w3d", "c_nod_sniper_l0.w3d",
    "c_nod_rsold_l0.w3d", "c_nod_stlth_l0.w3d", "c_nod_saku_l0.w3d", "c_nod_saku2_l0.w3d",
    "c_nod_rav_l0.w3d", "c_nod_mrav_l0.w3d", "c_nod_mdz_l0.w3d", "c_nod_mdz2_l0.w3d",
    "c_nod_tc_l0.w3d", "c_nod_mutant_l0.w3d", "c_nod_msld_l0.w3d", "c_nod_ssold_l0.w3d",
    "c_nod_petm_l0.w3d", "c_nod_kane_l0.w3d",
    "c_gdi_mg_l0.w3d", "c_gdi_rk_l0.w3d", "c_gdi_gr_l0.w3d", "c_gdi_en_l0.w3d",
    "c_gdi_mgo_l0.w3d", "c_gdi_rko_l0.w3d", "c_gdi_syd_l0.w3d", "c_gdi_dead_l0.w3d",
    "c_gdi_gun_l0.w3d", "c_gdi_ptch_l0.w3d",
    "c_havoc_l0.w3d", "c_havocn_l0.w3d", "c_havocw_l0.w3d", "c_havocd_l0.w3d",
    "c_gdi_syd_l0.w3d",   // appears twice in original source
    "c_gdi_syd2_l0.w3d", "c_gdi_mobi_l0.w3d", "c_gdi_hotw_l0.w3d", "c_gdi_lt_l0.w3d",
    "c_nod_petr_l0.w3d", "c_logan_l0.w3d", "c_gdi_locke_l0.w3d",
    "v_nod_buggy.w3d", "v_nod_apc_m.w3d", "v_nod_artlry.w3d", "v_nod_flame.w3d",
    "v_nod_ltank.w3d", "v_nod_stlth.w3d", "v_nod_trnspt_m.w3d", "v_nod_apache_m.w3d",
    "v_chameleon.w3d",
    "v_gdi_humvee.w3d", "v_gdi_apc_m.w3d", "v_gdi_mrls.w3d", "v_gdi_medtnk.w3d",
    "v_gdi_mammth.w3d", "v_pickup01.w3d", "v_sedan01.w3d", "v_gdi_orca_m.w3d",
    "v_gdi_trnspt_m.w3d",
)

/** Seeded CRC32 matching Westwood's CRC_Memory(buf, size, seed). */
private fun crcMemory(data: ByteArray, seed: Int = 0): Int {
    // Standard CRC32: init=0xFFFFFFFF, poly=0xEDB88320, finalXor=0xFFFFFFFF.
    // Seeded: undo finalXor to restore internal state from previous output, then continue.
    var state = (seed.toLong() and 0xFFFFFFFFL) xor 0xFFFFFFFFL
    for (b in data) {
        var v = state xor (b.toLong() and 0xFF)
        repeat(8) { v = if (v and 1L != 0L) (v ushr 1) xor 0xEDB88320L else v ushr 1 }
        state = v
    }
    return (state xor 0xFFFFFFFFL).toInt()
}

/** CRC32 over ISO-8859-1 bytes of a string, seed=0. */
private fun crcString(s: String): Int =
    crcMemory(s.toByteArray(Charsets.ISO_8859_1))

/** Extracts strings.tdb version number from the TranslateDB chunk structure. */
private fun readStringsVersion(tdbData: ByteArray): UInt {
    // CHUNKID_TRANSLATE_DB = 0x00090000 (= CHUNKID_WWTRANSLATEDB_BEGIN)
    // CHUNKID_VARIABLES    = 0x07141200
    // VARID_VERSION_NUMBER = 0x01 (micro-chunk, 4-byte uint32)
    val buf = ByteBuffer.wrap(tdbData).order(ByteOrder.LITTLE_ENDIAN)
    while (buf.remaining() >= 8) {
        val type = buf.int.toLong() and 0xFFFFFFFFL
        val sizeRaw = buf.int.toLong() and 0xFFFFFFFFL
        val size = (sizeRaw and 0x7FFFFFFFL).toInt()
        if (type != 0x00090000L) { buf.position(buf.position() + size); continue }
        val outerEnd = buf.position() + size
        while (buf.position() + 8 <= outerEnd) {
            val innerType = buf.int.toLong() and 0xFFFFFFFFL
            val innerSizeRaw = buf.int.toLong() and 0xFFFFFFFFL
            val innerSize = (innerSizeRaw and 0x7FFFFFFFL).toInt()
            if (innerType != 0x07141200L) { buf.position(buf.position() + innerSize); continue }
            val varEnd = buf.position() + innerSize
            while (buf.position() + 2 <= varEnd) {
                val mType = buf.get().toInt() and 0xFF
                val mSize = buf.get().toInt() and 0xFF
                if (mType == 0x01 && mSize == 4) return buf.int.toUInt()
                buf.position(buf.position() + mSize)
            }
            break
        }
        break
    }
    return 0u
}

fun main() {
    // Locate game data — adjust path as needed
    val renegadeData = File(
        System.getProperty("renegadeDataPath") ?:
        "/Users/marc/Library/Application Support/CrossOver/Bottles/Renegade Original/drive_c/Program Files/Renegade/Data"
    )
    println("Game data: $renegadeData")

    // Load MIX files
    val alwaysDat  = MixReader(File(renegadeData, "always.dat").readBytes())
    val always2Dat = MixReader(File(renegadeData, "Always2.dat").readBytes())
    val alwaysDbs  = MixReader(File(renegadeData, "always.dbs").readBytes())

    // ── strings.tdb version ─────────────────────────────────────────────────
    val tdbData = alwaysDbs.readFile("strings.tdb")
        ?: error("strings.tdb not found in always.dbs")
    println("strings.tdb: ${tdbData.size} bytes")

    // Extract to project data/ for reuse
    val outTdb = File("data/strings.tdb")
    outTdb.parentFile?.mkdirs()
    outTdb.writeBytes(tdbData)
    println("Extracted to: $outTdb")

    val stringsVersion = readStringsVersion(tdbData)
    println("strings.tdb version: $stringsVersion")

    // ── data_file_crc ───────────────────────────────────────────────────────
    var dataFileCrc = 0
    var found = 0; var missing = 0
    for (name in DATA_FILES) {
        val data = alwaysDat.readFile(name) ?: always2Dat.readFile(name)
        if (data != null) {
            dataFileCrc = crcMemory(data, dataFileCrc)
            found++
        } else {
            println("  MISSING: $name")
            missing++
        }
    }
    println("Data files: $found found, $missing missing")
    println("data_file_crc = 0x${Integer.toUnsignedString(dataFileCrc, 16)} ($dataFileCrc)")

    // ── ExeKey ──────────────────────────────────────────────────────────────
    val buildStr      = "RENEGADE $BUILD"
    val stringsStr    = "strings.tdb $stringsVersion"
    val keyString     = "$buildStr $stringsStr "  // trailing space per source

    val exeCrc     = crcString(buildStr)
    val stringsCrc = crcString(stringsStr)
    val exeKey     = crcString(keyString) xor dataFileCrc

    println()
    println("buildStr     = \"$buildStr\"")
    println("stringsStr   = \"$stringsStr\"")
    println("keyString    = \"$keyString\"")
    println()
    println("ExeCRC     = 0x${Integer.toUnsignedString(exeCrc, 16)} ($exeCrc)")
    println("StringsCRC = 0x${Integer.toUnsignedString(stringsCrc, 16)} ($stringsCrc)")
    println("ExeKey     = 0x${Integer.toUnsignedString(exeKey, 16)} ($exeKey)")
    println()
    println("In server.ini [Settings]:")
    println("  VersionNumber=${exeKey}")
    println("  ExeCRC=${exeCrc}")
    println("  StringsCRC=${stringsCrc}")
}
