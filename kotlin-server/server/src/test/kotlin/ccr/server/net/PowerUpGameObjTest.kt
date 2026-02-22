package ccr.server.net

import ccr.math.Vector3
import ccr.server.defs.combat.PowerUpGameObjDef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PowerUpGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = PowerUpGameObj(definitionId = 100, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = PowerUpGameObj(
            definitionId   = 200,
            position       = Vector3(1f, 2f, 3f),
            modelName      = "somemodel",
            health         = 50f,
        )
        assertEquals(200, obj.definitionId)
    }

    @Test fun `grant increases soldier health capped at healthMax`() {
        val soldier = SoldierGameObj(
            definitionId  = 1,
            controlOwner  = 0,
            team          = 0,
            modelName     = "c_ag_nod_mg",
            position      = Vector3(0f, 0f, 0f),
            health        = 60f,
        )
        soldier.healthMax = 100f

        val def = PowerUpGameObjDef(
            name = "Health",
            id = 1u,
            chunkId = PowerUpGameObjDef.CHUNK_ID,
            grantHealth = 50f,   // would overshoot without cap
        )

        val powerUp = PowerUpGameObj(definitionId = def.id.toInt(), position = Vector3(0f, 0f, 0f))
        powerUp.powerUpDef = def
        powerUp.grant(soldier, emptySet())

        assertEquals(100f, soldier.health, "health must be capped at healthMax")
        assertTrue(powerUp.isDeletePending, "powerup must be delete-pending after grant")
    }

    @Test fun `grant adds weapon to soldier weapons list`() {
        val soldier = SoldierGameObj(
            definitionId = 1,
            controlOwner = 0,
            team         = 0,
            modelName    = "c_ag_nod_mg",
            position     = Vector3(0f, 0f, 0f),
        )
        assertEquals(0, soldier.weapons.size)

        val def = PowerUpGameObjDef(
            name             = "Weapon",
            id               = 2u,
            chunkId          = PowerUpGameObjDef.CHUNK_ID,
            grantWeapon      = true,
            grantWeaponId    = 999,
            grantWeaponRounds = 30,
        )

        val powerUp = PowerUpGameObj(definitionId = def.id.toInt(), position = Vector3(0f, 0f, 0f))
        powerUp.powerUpDef = def
        powerUp.grant(soldier, emptySet())

        assertEquals(1, soldier.weapons.size)
        assertEquals(999, soldier.weapons[0].definitionId)
        assertEquals(30, soldier.weapons[0].totalRounds)
    }

    @Test fun `think() picks up soldier within 3m`() {
        val soldier = SoldierGameObj(
            definitionId = 1,
            controlOwner = 42,
            team         = 0,
            modelName    = "c_ag_nod_mg",
            position     = Vector3(1f, 0f, 0f),  // 1m from powerup
            health       = 50f,
        )
        soldier.healthMax = 100f

        val def = PowerUpGameObjDef(
            name = "Health",
            id = 3u,
            chunkId = PowerUpGameObjDef.CHUNK_ID,
            grantHealth = 30f,
        )

        val powerUp = PowerUpGameObj(definitionId = def.id.toInt(), position = Vector3(0f, 0f, 0f))
        powerUp.powerUpDef = def

        // think() needs a list of soldiers to scan — inject directly
        powerUp.thinkWithSoldiers(listOf(soldier), inGameClientIds = emptySet(), deltaSeconds = 0.016f)

        assertEquals(80f, soldier.health)
        assertTrue(powerUp.isDeletePending)
    }

    @Test fun `think() does NOT pick up soldier beyond 3m`() {
        val soldier = SoldierGameObj(
            definitionId = 1,
            controlOwner = 42,
            team         = 0,
            modelName    = "c_ag_nod_mg",
            position     = Vector3(5f, 0f, 0f),  // 5m away — outside 3m radius
            health       = 50f,
        )

        val def = PowerUpGameObjDef(
            name = "Health",
            id = 4u,
            chunkId = PowerUpGameObjDef.CHUNK_ID,
            grantHealth = 30f,
        )

        val powerUp = PowerUpGameObj(definitionId = def.id.toInt(), position = Vector3(0f, 0f, 0f))
        powerUp.powerUpDef = def
        powerUp.thinkWithSoldiers(listOf(soldier), inGameClientIds = emptySet(), deltaSeconds = 0.016f)

        assertEquals(50f, soldier.health, "health must be unchanged when out of range")
        assertFalse(powerUp.isDeletePending)
    }
}
