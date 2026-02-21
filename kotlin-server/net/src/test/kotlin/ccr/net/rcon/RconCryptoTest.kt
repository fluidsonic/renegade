package ccr.net.rcon

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RconCryptoTest {

    // ---- Key derivation ----

    @Test
    fun `key derivation from short password is zero-padded`() {
        val key = deriveKey("abc")
        assertEquals(8, key.size)
        assertEquals('a'.code.toByte(), key[0])
        assertEquals('b'.code.toByte(), key[1])
        assertEquals('c'.code.toByte(), key[2])
        for (i in 3..7) assertEquals(0, key[i].toInt())
    }

    @Test
    fun `key derivation from exactly 8 char password uses all chars`() {
        val key = deriveKey("12345678")
        assertContentEquals("12345678".toByteArray(Charsets.US_ASCII), key)
    }

    @Test
    fun `key derivation from longer password uses first 8 bytes`() {
        val key = deriveKey("abcdefghijklmnop")
        assertContentEquals("abcdefgh".toByteArray(Charsets.US_ASCII), key)
    }

    @Test
    fun `key derivation from empty password is all zeros`() {
        val key = deriveKey("")
        assertContentEquals(ByteArray(8), key)
    }

    // ---- Encrypt/decrypt round-trips ----

    @Test
    fun `encrypt then decrypt round-trips single byte`() {
        val plaintext = byteArrayOf(42)
        val key = deriveKey("password")
        val ciphertext = encrypt(plaintext, key)
        val recovered = decrypt(ciphertext, deriveKey("password"))
        assertContentEquals(plaintext, recovered)
    }

    @Test
    fun `encrypt then decrypt round-trips multi-byte message`() {
        val plaintext = "Hello, RCON!".toByteArray(Charsets.US_ASCII)
        val key = deriveKey("secret")
        val ciphertext = encrypt(plaintext, key)
        val recovered = decrypt(ciphertext, deriveKey("secret"))
        assertContentEquals(plaintext, recovered)
    }

    @Test
    fun `encrypt then decrypt round-trips 500 byte message`() {
        val plaintext = ByteArray(500) { i -> (i % 256).toByte() }
        val key = deriveKey("longerpassword")
        val ciphertext = encrypt(plaintext, key)
        val recovered = decrypt(ciphertext, deriveKey("longerpassword"))
        assertContentEquals(plaintext, recovered)
    }

    @Test
    fun `encrypt then decrypt round-trips empty message`() {
        val plaintext = ByteArray(0)
        val ciphertext = encrypt(plaintext, deriveKey("key"))
        val recovered = decrypt(ciphertext, deriveKey("key"))
        assertContentEquals(plaintext, recovered)
    }

    // ---- Ciphertext feedback: same plaintext byte produces different output ----

    @Test
    fun `repeated plaintext bytes produce different ciphertext bytes`() {
        val plaintext = ByteArray(16) { 0x41 } // all 'A'
        val ciphertext = encrypt(plaintext, deriveKey("testkey"))
        // Due to ciphertext feedback + offset, each output byte should differ
        // At a minimum, not all bytes should be the same
        val allSame = ciphertext.all { it == ciphertext[0] }
        assert(!allSame) { "Ciphertext feedback not working: all bytes identical" }
    }

    // ---- CRC ----

    @Test
    fun `crc of empty data is zero`() {
        assertEquals(0, computeCrc(ByteArray(0)))
    }

    @Test
    fun `crc of 4 bytes is deterministic`() {
        val data = byteArrayOf(0x01, 0x00, 0x00, 0x00)
        val crc = computeCrc(data)
        // CRC of LE uint 1: hibit=0, crc = (0 << 1) + 1 + 0 = 1 → expect 2 after shift? Let's verify by formula:
        // chunk=1, hibit=0, crc = (0 shl 1) + 1 + 0 = 1
        assertEquals(1, crc)
    }

    @Test
    fun `crc handles partial trailing bytes`() {
        // 5 bytes: first 4 as full chunk, then 1 byte remainder
        val data = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x02)
        val crc = computeCrc(data)
        // Manually:
        // step1: chunk=1, hibit=0, crc = 1
        // step2: chunk=2 (1 byte), hibit=0, crc = (1 shl 1) + 2 + 0 = 4
        assertEquals(4, crc)
    }

    @Test
    fun `crc handles 2-byte partial trailer`() {
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x01, 0x00)
        val crc = computeCrc(data)
        // step1: chunk=0, hibit=0, crc=0
        // step2: chunk=1 (2-byte LE: 0x00, 0x01 → 0x0100 = 256? No: LE means first byte is low)
        //   bytes[4]=0x01, bytes[5]=0x00 → value = 0x01 | (0x00 << 8) = 1
        // crc = (0 << 1) + 1 + 0 = 1
        assertEquals(1, crc)
    }

    @Test
    fun `crc handles 3-byte partial trailer`() {
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00)
        val crc = computeCrc(data)
        // step1: chunk=0, crc=0
        // step2: 3 bytes [0x01, 0x00, 0x00] → LE = 1
        // crc = 1
        assertEquals(1, crc)
    }

    @Test
    fun `crc of 8 bytes uses two rounds`() {
        val data = ByteArray(8) { 0 }
        data[0] = 0x01 // first chunk = 1
        // step1: chunk=1, hibit=0, crc=1
        // step2: chunk=0, hibit=0, crc = (1 shl 1) + 0 + 0 = 2
        assertEquals(2, computeCrc(data))
    }

    @Test
    fun `encrypt does not modify original plaintext`() {
        val plaintext = "test".toByteArray(Charsets.US_ASCII)
        val copy = plaintext.copyOf()
        encrypt(plaintext, deriveKey("k"))
        assertContentEquals(copy, plaintext)
    }
}
