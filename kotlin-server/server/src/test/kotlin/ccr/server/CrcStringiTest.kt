package ccr.server

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// Verifies crcStringi matches C++ realcrc.cpp CRC_Stringi behavior.
// Reference value from demosupport.h: // Crc of "C&C_Under.mix" = 721292856.
class CrcStringiTest {

    @Test
    fun `known map name matches documented CRC`() {
        // demosupport.h: Crc of "C&C_Under.mix" = 721292856
        assertEquals(721292856, crcStringi("C&C_Under.mix"))
    }

    @Test
    fun `case insensitive - uppercase and mixed give same result`() {
        assertEquals(crcStringi("c&c_under.mix"), crcStringi("C&C_UNDER.MIX"))
        assertEquals(crcStringi("C&C_Under.mix"), crcStringi("c&c_under.mix"))
    }

    @Test
    fun `empty string gives zero`() {
        // CRC32 with no bytes → value=0 (CRC32 init=0, no updates, value stays 0)
        assertEquals(0, crcStringi(""))
    }

    @Test
    fun `different strings give different CRCs`() {
        val a = crcStringi("C&C_Under.mix")
        val b = crcStringi("C&C_Field.mix")
        assert(a != b) { "Different map names must have different CRCs" }
    }
}
