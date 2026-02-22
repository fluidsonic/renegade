package ccr.server.combat

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArmorWarheadManagerTest {

    // Minimal armor.ini matching Renegade section format
    private val armorIni = """
        [Armor_Types]
        Flesh=Flesh
        Metal=Metal

        [Warhead_Types]
        Bullet=Bullet
        C4=C4

        [Armor_Save_IDs]
        Flesh=0
        Metal=3

        [Warhead_Save_IDs]
        Bullet=0
        C4=5

        [Scale_Flesh]
        Bullet=1.0
        C4=1.5

        [Scale_Metal]
        Bullet=0.5
        C4=2.0
    """.trimIndent().toByteArray()

    @BeforeTest fun setup() { ArmorWarheadManager.init(armorIni) }

    @Test fun `getDamageMultiplier Flesh vs Bullet`() =
        assertEquals(1.0f, ArmorWarheadManager.getDamageMultiplier(0, 0))

    @Test fun `getDamageMultiplier Flesh vs C4`() =
        assertEquals(1.5f, ArmorWarheadManager.getDamageMultiplier(5, 0))

    @Test fun `getDamageMultiplier Metal vs Bullet`() =
        assertEquals(0.5f, ArmorWarheadManager.getDamageMultiplier(0, 3))

    @Test fun `getDamageMultiplier Metal vs C4`() =
        assertEquals(2.0f, ArmorWarheadManager.getDamageMultiplier(5, 3))

    @Test fun `scaleDamage applies multiplier`() =
        assertEquals(300.0f, ArmorWarheadManager.scaleDamage(150.0f, 5, 3))  // 150 * 2.0

    @Test fun `unknown save ID returns 1f multiplier`() =
        assertEquals(1.0f, ArmorWarheadManager.getDamageMultiplier(99, 99))
}
