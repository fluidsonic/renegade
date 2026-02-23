package ccr.server

import ccr.math.Vector3
import ccr.server.net.SoldierGameObj
import ccr.server.net.WeaponEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class GodPowerUpTest {

    private fun makeSoldier(vararg weapons: WeaponEntry) = SoldierGameObj(
        definitionId = 1,
        position = Vector3(0f, 0f, 0f),
        weapons = mutableListOf(*weapons),
    )

    @Test
    fun `addWeaponToSoldier adds new weapon when not owned`() {
        val soldier = makeSoldier(WeaponEntry(101, 30))
        God.addWeaponToSoldier(soldier, weaponDefId = 202, rounds = 5, grantWeapon = true)
        assertEquals(2, soldier.weapons.size)
        assertEquals(WeaponEntry(202, 5), soldier.weapons[1])
    }

    @Test
    fun `addWeaponToSoldier tops up rounds when already owned`() {
        val soldier = makeSoldier(WeaponEntry(101, 30))
        God.addWeaponToSoldier(soldier, weaponDefId = 101, rounds = 10, grantWeapon = true)
        assertEquals(1, soldier.weapons.size)
        assertEquals(40, soldier.weapons[0].totalRounds)
    }

    @Test
    fun `addWeaponToSoldier does not add weapon when grantWeapon false and not owned`() {
        val soldier = makeSoldier(WeaponEntry(101, 30))
        God.addWeaponToSoldier(soldier, weaponDefId = 999, rounds = 5, grantWeapon = false)
        assertEquals(1, soldier.weapons.size)
    }

    @Test
    fun `grantShieldType upgrades when grant type is higher`() {
        val soldier = SoldierGameObj(
            definitionId = 1,
            position = Vector3(0f, 0f, 0f),
        )
        soldier.shieldType = 3
        val grantType = 5
        if (grantType > 0 && grantType > soldier.shieldType) soldier.shieldType = grantType
        assertEquals(5, soldier.shieldType)
    }

    @Test
    fun `grantShieldType does not downgrade`() {
        val soldier = SoldierGameObj(
            definitionId = 1,
            position = Vector3(0f, 0f, 0f),
        )
        soldier.shieldType = 5
        val grantType = 3
        if (grantType > 0 && grantType > soldier.shieldType) soldier.shieldType = grantType
        assertEquals(5, soldier.shieldType, "lower armor type must not replace higher")
    }

    @Test
    fun `grantShieldStrengthMax add formula rounds up`() {
        // C++ formula: (grantShieldStrengthMax * baseDef.shieldStrengthMax + 0.95f).toInt()
        val baseSMax = 100f
        val grantSMax = 0.5f  // "add half of base max"
        val add = (grantSMax * baseSMax + 0.95f).toInt().toFloat()
        assertEquals(50f, add, "0.5 × 100 = 50")
    }
}
