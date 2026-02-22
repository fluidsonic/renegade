package ccr.server

import ccr.math.Vector3
import ccr.server.defs.DefinitionClass
import ccr.server.defs.combat.PowerUpGameObjDef
import ccr.server.defs.combat.SpawnerDefClass
import ccr.server.level.DefinitionRegistry
import ccr.server.level.LevelDynamicData
import ccr.server.level.LevelStaticData
import ccr.server.level.LoadedLevel
import ccr.server.level.Matrix3D
import ccr.server.level.ldd.LoadedSpawner
import kotlin.test.Test
import kotlin.test.assertEquals

class SpawnManagerPowerUpTimerTest {

    /** Build a LoadedSpawner with identity rotation and the given translation. */
    private fun makeLoadedSpawner(
        id: Int,
        definitionId: Int,
        posX: Float,
        posY: Float,
        posZ: Float,
        enabled: Boolean,
    ): LoadedSpawner {
        // Matrix3D row-major 12-float layout: elements[3]=tx, elements[7]=ty, elements[11]=tz
        val transform = Matrix3D(floatArrayOf(
            1f, 0f, 0f, posX,
            0f, 1f, 0f, posY,
            0f, 0f, 1f, posZ,
        ))
        return LoadedSpawner(
            id = id,
            transform = transform,
            spawnTransform = null,
            definitionId = definitionId,
            spawnCount = 0,
            enabled = enabled,
            spawnPoints = emptyList(),
            scripts = emptyList(),
        )
    }

    private fun makeLoadedLevel(
        spawners: List<LoadedSpawner>,
        extraDefs: List<DefinitionClass> = emptyList(),
    ): LoadedLevel {
        val registry = DefinitionRegistry()
        for (s in spawners) {
            val spawnerDef = SpawnerDefClass(
                name = "Spawner_${s.id}",
                id = s.definitionId.toUInt(),
                chunkId = SpawnerDefClass.CHUNK_ID,
                isMultiplayWeaponSpawner = true,
                spawnDefinitionIdList = listOf(s.definitionId + 1000),
                spawnDelay = 10f,
            )
            registry.register(spawnerDef)
        }
        for (d in extraDefs) registry.register(d)
        return LoadedLevel(
            mapFilename = "test",
            missionDescriptionId = 0,
            description = "",
            definitions = registry,
            worldExtents = null,
            staticData = LevelStaticData(),
            dynamicData = LevelDynamicData(spawners = spawners),
        )
    }

    @Test
    fun `think() accumulates time and fires createPowerUp after spawnDelay`() {
        val spawnerDefId = 42
        val powerUpDefId = spawnerDefId + 1000

        val powerUpDef = PowerUpGameObjDef(
            name = "PowerUp_Health",
            id = powerUpDefId.toUInt(),
            chunkId = PowerUpGameObjDef.CHUNK_ID,
            grantHealth = 50f,
        )

        val spawner = makeLoadedSpawner(
            id = 1,
            definitionId = spawnerDefId,
            posX = 10f, posY = 20f, posZ = 5f,
            enabled = true,
        )

        val level = makeLoadedLevel(listOf(spawner), extraDefs = listOf(powerUpDef))
        val createdPowerUps = mutableListOf<Pair<Vector3, PowerUpGameObjDef>>()

        val sm = SpawnManager(level)
        sm.onCreatePowerUp = { pos, def -> createdPowerUps.add(Pair(pos, def)) }

        sm.think(9.0f)
        assertEquals(0, createdPowerUps.size, "should not fire before 10 seconds")

        sm.think(2.0f)
        assertEquals(1, createdPowerUps.size, "should fire after 10+ seconds")
        assertEquals(powerUpDef, createdPowerUps[0].second)

        sm.think(9.0f)
        assertEquals(1, createdPowerUps.size, "timer should reset, not fire again until next delay")
    }

    @Test
    fun `disabled spawner is not ticked`() {
        val spawner = makeLoadedSpawner(id = 2, definitionId = 99, posX = 0f, posY = 0f, posZ = 0f, enabled = false)
        val level = makeLoadedLevel(listOf(spawner))
        val sm = SpawnManager(level)
        val fired = mutableListOf<Unit>()
        sm.onCreatePowerUp = { _, _ -> fired.add(Unit) }
        sm.think(100f)
        assertEquals(0, fired.size, "disabled spawner must not fire")
    }

    @Test
    fun `soldier-startup spawner is not ticked as powerup`() {
        val registry = DefinitionRegistry()
        val soldierSpawnerDef = SpawnerDefClass(
            name = "Soldier_Spawn",
            id = 77u,
            chunkId = SpawnerDefClass.CHUNK_ID,
            isMultiplayWeaponSpawner = false,
            isSoldierStartup = true,
        )
        registry.register(soldierSpawnerDef)

        val spawner = makeLoadedSpawner(id = 3, definitionId = 77, posX = 0f, posY = 0f, posZ = 0f, enabled = true)

        val level = LoadedLevel(
            mapFilename = "test",
            missionDescriptionId = 0,
            description = "",
            definitions = registry,
            worldExtents = null,
            staticData = LevelStaticData(),
            dynamicData = LevelDynamicData(spawners = listOf(spawner)),
        )

        val sm = SpawnManager(level)
        val fired = mutableListOf<Unit>()
        sm.onCreatePowerUp = { _, _ -> fired.add(Unit) }
        sm.think(100f)
        assertEquals(0, fired.size, "soldier spawner must not be ticked as powerup")
    }
}
