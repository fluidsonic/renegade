package ccr.server.core

/**
 * C++: CRC_Stringi in wwlib/realcrc.cpp — case-insensitive CRC32 over ASCII string bytes.
 * Equivalent to java.util.zip.CRC32 over the uppercase bytes of the string.
 * Known value: crcStringi("C&C_Under.mix") == 721292856 (0x2B007C38).
 */
fun crcStringi(s: String): Int {
	val crc = java.util.zip.CRC32()
	for (c in s) crc.update(c.uppercaseChar().code and 0xFF)
	return crc.value.toInt()
}
