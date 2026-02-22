package ccr.server.mix

import kotlin.test.Test
import kotlin.test.assertEquals

class IniFileTest {
    private val sample = """
        [Armor_Types]
        Flesh=Flesh
        Metal=Metal

        [Armor_Save_IDs]
        Flesh=0
        Metal=1

        [Scale_Flesh]
        Bullet=1.0
        C4=1.5

        [Scale_Metal]
        Bullet=0.5
        C4=2.0
    """.trimIndent().toByteArray()

    @Test fun `getInt returns parsed value`() {
        val ini = IniFile(sample)
        assertEquals(0, ini.getInt("Armor_Save_IDs", "Flesh", -1))
        assertEquals(1, ini.getInt("Armor_Save_IDs", "Metal", -1))
    }

    @Test fun `getFloat returns parsed value`() {
        val ini = IniFile(sample)
        assertEquals(1.5f, ini.getFloat("Scale_Flesh", "C4", 1.0f))
        assertEquals(2.0f, ini.getFloat("Scale_Metal", "C4", 1.0f))
    }

    @Test fun `getString returns parsed value`() {
        val ini = IniFile(sample)
        assertEquals("Flesh", ini.getString("Armor_Types", "Flesh", ""))
    }

    @Test fun `missing key returns default`() {
        val ini = IniFile(sample)
        assertEquals(-1, ini.getInt("Armor_Save_IDs", "Missing", -1))
        assertEquals(99.0f, ini.getFloat("Scale_Flesh", "Missing", 99.0f))
    }

    @Test fun `entryCount returns number of keys in section`() {
        val ini = IniFile(sample)
        assertEquals(2, ini.entryCount("Armor_Types"))
    }

    @Test fun `getEntry returns key name at index`() {
        val ini = IniFile(sample)
        val keys = (0 until ini.entryCount("Armor_Types")).map { ini.getEntry("Armor_Types", it) }
        assertEquals(setOf("Flesh", "Metal"), keys.toSet())
    }

    @Test fun `lookups are case-insensitive`() {
        val ini = IniFile(sample)
        assertEquals(0, ini.getInt("armor_save_ids", "flesh", -1))
        assertEquals(1.5f, ini.getFloat("SCALE_FLESH", "c4", 1.0f))
    }
}
