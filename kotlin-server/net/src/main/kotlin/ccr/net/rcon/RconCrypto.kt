package ccr.net.rcon

// C++: servercontrolsocket.cpp — stream cipher + CRC for RCON protocol.

/**
 * Derives an 8-byte key from a password string.
 * First 8 bytes of the password, zero-padded if shorter.
 */
fun deriveKey(password: String): ByteArray {
    val key = ByteArray(8)
    val bytes = password.toByteArray(Charsets.US_ASCII)
    for (i in 0 until minOf(8, bytes.size)) {
        key[i] = bytes[i]
    }
    return key
}

/**
 * Encrypts [plaintext] in-place using the RCON stream cipher.
 * [key] must be exactly 8 bytes; it is mutated during encryption (ciphertext feedback).
 * C++: servercontrolsocket.cpp lines 987-1013.
 */
fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
    require(key.size == 8) { "Key must be 8 bytes" }
    val k = key.copyOf()
    val ciphertext = ByteArray(plaintext.size)
    for (i in plaintext.indices) {
        var a = (plaintext[i] + (i.toByte() - 50)).toByte()
        a = (a.toInt() xor k[i % 8].toInt()).toByte()
        ciphertext[i] = a
        k[i % 8] = (k[i % 8].toInt() xor a.toInt()).toByte()
    }
    return ciphertext
}

/**
 * Decrypts [ciphertext] using the RCON stream cipher.
 * [key] must be exactly 8 bytes; it is mutated during decryption (ciphertext feedback).
 * C++: exact inverse of the encrypt operation.
 */
fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray {
    require(key.size == 8) { "Key must be 8 bytes" }
    val k = key.copyOf()
    val plaintext = ByteArray(ciphertext.size)
    for (i in ciphertext.indices) {
        val c = ciphertext[i]
        val a = (c.toInt() xor k[i % 8].toInt()).toByte()
        plaintext[i] = (a - (i.toByte() - 50)).toByte()
        k[i % 8] = (k[i % 8].toInt() xor c.toInt()).toByte()
    }
    return plaintext
}

/**
 * Computes the RCON CRC over [data].
 * C++: Add_CRC (servercontrolsocket.cpp lines 935-948).
 * Processes data as 4-byte LE uint chunks; trailing 1-3 bytes are treated as partial LE uint.
 */
fun computeCrc(data: ByteArray): Int {
    var crc = 0L
    var offset = 0
    while (offset + 4 <= data.size) {
        val chunk = readLeUint(data, offset, 4)
        val hibit = (crc ushr 31) and 1L
        crc = ((crc shl 1) + chunk + hibit) and 0xFFFFFFFFL
        offset += 4
    }
    val remaining = data.size - offset
    if (remaining > 0) {
        val chunk = readLeUint(data, offset, remaining)
        val hibit = (crc ushr 31) and 1L
        crc = ((crc shl 1) + chunk + hibit) and 0xFFFFFFFFL
    }
    return crc.toInt()
}

private fun readLeUint(data: ByteArray, offset: Int, count: Int): Long {
    var value = 0L
    for (i in 0 until count) {
        value = value or ((data[offset + i].toLong() and 0xFF) shl (i * 8))
    }
    return value
}
