package ccr.server.combat

import ccr.server.crcStringi
import ccr.server.core.MixReader
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArmorWarheadManagerTest {

    private val testIni = """
[Armor_Types]
Armor00=Flesh
Armor01=Metal
Armor02=Light_Armor

[Warhead_Types]
Warhead00=Bullet
Warhead01=Explosive
Warhead02=Flame

[Armor_Save_IDs]
Flesh=10
Metal=20
Light_Armor=30

[Warhead_Save_IDs]
Bullet=100
Explosive=200
Flame=300

[Soft_Armor]
SoftEntry00=Flesh

[Scale_Flesh]
Bullet=1.5
Explosive=2.0
Flame=0.5

[Scale_Metal]
Bullet=0.5
Explosive=1.0
Flame=0.25

[Scale_Light_Armor]
Bullet=0.75
Explosive=1.5
Flame=0.3

[Shield_Flesh]
Bullet=0.1
Explosive=0.0
Flame=0.2

[Shield_Metal]
Bullet=0.0
Explosive=0.5
Flame=0.0

[Shield_Light_Armor]
Bullet=0.0
Explosive=0.0
Flame=0.0

[Special_Damage_Type]
Bullet=0
Explosive=0
Flame=FIRE

[Special_Damage_Probability]
Bullet=0.0
Explosive=0.0
Flame=0.8

[Visceroid_Probability]
Bullet=0.0
Explosive=0.0
Flame=0.0

[Special_Damage_Fire]
Warhead=Flame
Duration=3.0
Scale=1.5
Explosion=fireExplosion

[Special_Damage_Chem]
Warhead=Bullet
Duration=0.0
Scale=1.0
Explosion=

[Special_Damage_Electric]
Warhead=Bullet
Duration=0.0
Scale=1.0
Explosion=

[Special_Damage_Cnc_Fire]
Warhead=Bullet
Duration=0.0
Scale=1.0
Explosion=

[Special_Damage_Cnc_Chem]
Warhead=Bullet
Duration=0.0
Scale=1.0
Explosion=

[Impervious_Fire]
Impervious00=Metal
""".trimIndent()

    // Helpers for building a minimal MIX1 in memory (copied from MixReaderTest)
    private fun leInt(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )

    private fun leUInt(v: Long): ByteArray = leInt(v.toInt())

    private fun buildMix(vararg files: Pair<String, ByteArray>): ByteArray {
        val sorted = files.map { (name, content) ->
            Triple(name, content, crcStringi(name).toLong() and 0xFFFFFFFFL)
        }.sortedBy { it.third }

        val parts = mutableListOf<ByteArray>()
        parts += "MIX1".toByteArray(Charsets.US_ASCII)
        parts += leInt(0)
        parts += leInt(0)
        parts += leInt(0)

        data class EntryInfo(val crc: Long, val offset: Int, val size: Int)
        val entries = mutableListOf<EntryInfo>()
        var cursor = 16
        for ((name, content, crc) in sorted) {
            entries += EntryInfo(crc, cursor, content.size)
            parts += content
            cursor += content.size
        }

        val headerOffset = cursor
        parts += leInt(sorted.size)
        for (e in entries) {
            parts += leUInt(e.crc)
            parts += leInt(e.offset)
            parts += leInt(e.size)
        }

        val namesOffset = headerOffset + 4 + sorted.size * 12
        val raw = parts.fold(ByteArray(0)) { acc, b -> acc + b }
        val result = raw.copyOf()
        leInt(headerOffset).copyInto(result, 4)
        leInt(namesOffset).copyInto(result, 8)
        return result
    }

    @BeforeTest
    fun setup() {
        val mix = buildMix("armor.ini" to testIni.toByteArray(Charsets.UTF_8))
        ArmorWarheadManager.init(listOf(MixReader(mix)))
    }

    @AfterTest
    fun teardown() {
        ArmorWarheadManager.shutdown()
    }

    // Basic counts
    @Test fun `numArmorTypes is 3`() = assertEquals(3, ArmorWarheadManager.numArmorTypes)
    @Test fun `numWarheadTypes is 3`() = assertEquals(3, ArmorWarheadManager.numWarheadTypes)

    // Name lookup
    @Test fun `getArmorType returns correct index`() {
        assertEquals(0, ArmorWarheadManager.getArmorType("Flesh"))
        assertEquals(1, ArmorWarheadManager.getArmorType("Metal"))
        assertEquals(2, ArmorWarheadManager.getArmorType("Light_Armor"))
    }

    @Test fun `getArmorType case insensitive`() =
        assertEquals(0, ArmorWarheadManager.getArmorType("flesh"))

    @Test fun `getArmorType unknown returns 0`() =
        assertEquals(0, ArmorWarheadManager.getArmorType("Unknown"))

    @Test fun `getWarheadType returns correct index`() {
        assertEquals(0, ArmorWarheadManager.getWarheadType("Bullet"))
        assertEquals(1, ArmorWarheadManager.getWarheadType("Explosive"))
        assertEquals(2, ArmorWarheadManager.getWarheadType("Flame"))
    }

    @Test fun `getArmorName returns correct name`() {
        assertEquals("Flesh", ArmorWarheadManager.getArmorName(0))
        assertEquals("Metal", ArmorWarheadManager.getArmorName(1))
        assertEquals("Light_Armor", ArmorWarheadManager.getArmorName(2))
    }

    @Test fun `getWarheadName returns correct name`() {
        assertEquals("Bullet", ArmorWarheadManager.getWarheadName(0))
        assertEquals("Explosive", ArmorWarheadManager.getWarheadName(1))
        assertEquals("Flame", ArmorWarheadManager.getWarheadName(2))
    }

    // SaveID
    @Test fun `getArmorSaveID returns correct id`() {
        assertEquals(10, ArmorWarheadManager.getArmorSaveID(0))
        assertEquals(20, ArmorWarheadManager.getArmorSaveID(1))
        assertEquals(30, ArmorWarheadManager.getArmorSaveID(2))
    }

    @Test fun `getWarheadSaveID returns correct id`() {
        assertEquals(100, ArmorWarheadManager.getWarheadSaveID(0))
        assertEquals(200, ArmorWarheadManager.getWarheadSaveID(1))
        assertEquals(300, ArmorWarheadManager.getWarheadSaveID(2))
    }

    @Test fun `findArmorSaveID returns correct type`() {
        assertEquals(0, ArmorWarheadManager.findArmorSaveID(10))
        assertEquals(1, ArmorWarheadManager.findArmorSaveID(20))
        assertEquals(2, ArmorWarheadManager.findArmorSaveID(30))
        assertEquals(0, ArmorWarheadManager.findArmorSaveID(99)) // not found -> 0
    }

    @Test fun `findWarheadSaveID returns correct type`() {
        assertEquals(0, ArmorWarheadManager.findWarheadSaveID(100))
        assertEquals(1, ArmorWarheadManager.findWarheadSaveID(200))
        assertEquals(2, ArmorWarheadManager.findWarheadSaveID(300))
        assertEquals(0, ArmorWarheadManager.findWarheadSaveID(99)) // not found -> 0
    }

    // Damage multipliers (by type index)
    @Test fun `getDamageMultiplier Flesh vs Bullet`() =
        assertEquals(1.5f, ArmorWarheadManager.getDamageMultiplier(0, 0), 0.001f)

    @Test fun `getDamageMultiplier Metal vs Bullet`() =
        assertEquals(0.5f, ArmorWarheadManager.getDamageMultiplier(1, 0), 0.001f)

    @Test fun `getDamageMultiplier Flesh vs Flame`() =
        assertEquals(0.5f, ArmorWarheadManager.getDamageMultiplier(0, 2), 0.001f)

    // Damage multipliers by saveID (backward compat)
    @Test fun `scaleDamage scales by multiplier`() =
        assertEquals(150.0f, ArmorWarheadManager.scaleDamage(100.0f, 100, 10), 0.001f) // Bullet/Flesh: 100 * 1.5

    @Test fun `getDamageMultiplierBySaveId`() =
        assertEquals(1.5f, ArmorWarheadManager.getDamageMultiplierBySaveId(100, 10), 0.001f)

    // Absorption (shield)
    @Test fun `getShieldAbsorption Flesh vs Bullet`() =
        assertEquals(0.1f, ArmorWarheadManager.getShieldAbsorption(0, 0), 0.001f)

    @Test fun `getShieldAbsorption Metal vs Explosive`() =
        assertEquals(0.5f, ArmorWarheadManager.getShieldAbsorption(1, 1), 0.001f)

    // Soft armor
    @Test fun `isArmorSoft Flesh is true`() =
        assertTrue(ArmorWarheadManager.isArmorSoft(0))

    @Test fun `isArmorSoft Metal is false`() =
        assertFalse(ArmorWarheadManager.isArmorSoft(1))

    // Special damage
    @Test fun `getSpecialDamageType Flame warhead is FIRE`() =
        assertEquals(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE, ArmorWarheadManager.getSpecialDamageType(2))

    @Test fun `getSpecialDamageType Bullet warhead is NONE`() =
        assertEquals(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_NONE, ArmorWarheadManager.getSpecialDamageType(0))

    @Test fun `getSpecialDamageProbability Flame`() =
        assertEquals(0.8f, ArmorWarheadManager.getSpecialDamageProbability(2), 0.001f)

    @Test fun `getSpecialDamageWarhead for FIRE`() =
        assertEquals(2, ArmorWarheadManager.getSpecialDamageWarhead(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE))

    @Test fun `getSpecialDamageDuration for FIRE`() =
        assertEquals(3.0f, ArmorWarheadManager.getSpecialDamageDuration(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE), 0.001f)

    @Test fun `getSpecialDamageScale for FIRE`() =
        assertEquals(1.5f, ArmorWarheadManager.getSpecialDamageScale(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE), 0.001f)

    @Test fun `getSpecialDamageExplosion for FIRE`() =
        assertEquals("fireExplosion", ArmorWarheadManager.getSpecialDamageExplosion(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE))

    @Test fun `getVisceroidProbability Flame`() =
        assertEquals(0.0f, ArmorWarheadManager.getVisceroidProbability(2), 0.001f)

    // Impervious
    @Test fun `isSkinImpervious Metal is impervious to FIRE`() =
        assertTrue(ArmorWarheadManager.isSkinImpervious(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE, 1))

    @Test fun `isSkinImpervious Flesh is not impervious to FIRE`() =
        assertFalse(ArmorWarheadManager.isSkinImpervious(ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE, 0))

    // SUPER_FIRE copies from FIRE
    @Test fun `SUPER_FIRE copies FIRE values`() {
        val fire = ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_FIRE
        val superFire = ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_SUPER_FIRE
        assertEquals(ArmorWarheadManager.getSpecialDamageWarhead(fire), ArmorWarheadManager.getSpecialDamageWarhead(superFire))
        assertEquals(ArmorWarheadManager.getSpecialDamageDuration(fire), ArmorWarheadManager.getSpecialDamageDuration(superFire), 0.001f)
        assertEquals(ArmorWarheadManager.getSpecialDamageScale(fire), ArmorWarheadManager.getSpecialDamageScale(superFire), 0.001f)
        assertEquals(ArmorWarheadManager.getSpecialDamageExplosion(fire), ArmorWarheadManager.getSpecialDamageExplosion(superFire))
    }

    // Shutdown
    @Test fun `after shutdown numArmorTypes is 0`() {
        ArmorWarheadManager.shutdown()
        assertEquals(0, ArmorWarheadManager.numArmorTypes)
    }
}
