package ccr.server

import ccr.math.Vector3
import ccr.server.defs.DefinitionClass
import ccr.server.defs.PowerUpGameObjDef
import ccr.server.defs.SpawnerDefClass
import ccr.server.level.DefinitionRegistry
import ccr.server.level.LevelDynamicData
import ccr.server.level.LevelStaticData
import ccr.server.level.LoadedLevel
import ccr.server.level.Matrix3D
import ccr.server.level.ldd.LoadedSpawner
import ccr.server.net.PowerUpGameObj
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
        // Return a real PowerUpGameObj so the spawn is considered successful and the timer resets.
        // Returning null would leave the timer frozen at ≤ 0 and retry immediately (C++ behavior).
        sm.onCreatePowerUp = { pos, def ->
            createdPowerUps.add(Pair(pos, def))
            PowerUpGameObj(definitionId = powerUpDefId, position = pos)
        }

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
        sm.onCreatePowerUp = { _, _ -> fired.add(Unit); null }
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
        sm.onCreatePowerUp = { _, _ -> fired.add(Unit); null }
        sm.think(100f)
        assertEquals(0, fired.size, "soldier spawner must not be ticked as powerup")
    }

    // -------------------------------------------------------------------------
    // Live-object tracking tests (Task 4 / Phase 17)
    // -------------------------------------------------------------------------

    /** Creates a PowerUpGameObj and optionally marks it delete-pending. */
    private fun makePowerUp(deleted: Boolean): PowerUpGameObj {
        val obj = PowerUpGameObj(definitionId = 1, position = Vector3(0f, 0f, 0f))
        if (deleted) obj.setDeletePending()
        return obj
    }

    /**
     * Builds a SpawnManager with a single enabled powerup spawner whose timer
     * fires after [spawnDelay] seconds.
     */
    private fun buildManager(spawnDelay: Float): SpawnManager {
        val spawnerDefId = 200
        val powerUpDefId = spawnerDefId + 1000

        val registry = DefinitionRegistry()

        val spawnerDef = SpawnerDefClass(
            name = "Spawner_Live",
            id = spawnerDefId.toUInt(),
            chunkId = SpawnerDefClass.CHUNK_ID,
            isMultiplayWeaponSpawner = true,
            spawnDefinitionIdList = listOf(powerUpDefId),
            spawnDelay = spawnDelay,
        )
        registry.register(spawnerDef)

        val powerUpDef = PowerUpGameObjDef(
            name = "PowerUp_Live",
            id = powerUpDefId.toUInt(),
            chunkId = PowerUpGameObjDef.CHUNK_ID,
            grantHealth = 25f,
        )
        registry.register(powerUpDef)

        val spawner = makeLoadedSpawner(
            id = 10,
            definitionId = spawnerDefId,
            posX = 0f, posY = 0f, posZ = 0f,
            enabled = true,
        )

        val level = LoadedLevel(
            mapFilename = "test",
            missionDescriptionId = 0,
            description = "",
            definitions = registry,
            worldExtents = null,
            staticData = LevelStaticData(),
            dynamicData = LevelDynamicData(spawners = listOf(spawner)),
        )

        return SpawnManager(level)
    }

    @Test
    fun `think does not spawn while live object is alive`() {
        var spawnCount = 0
        val aliveObject = makePowerUp(deleted = false)

        val mgr = buildManager(spawnDelay = 5f)
        mgr.onCreatePowerUp = { _, _ ->
            spawnCount++
            aliveObject
        }

        // First tick at t=6s → timer (5s) fires, spawnCount=1, liveObject=aliveObject
        mgr.think(6f)
        assertEquals(1, spawnCount, "should fire once after timer expires")

        // Second tick: aliveObject still alive → live != null && !isDeletePending → continue fires
        // before timer is decremented at all; timer is completely frozen → no spawn
        mgr.think(6f)
        assertEquals(1, spawnCount, "should not spawn again while live object exists")
    }

    @Test
    fun `think spawns again after live object is deleted`() {
        var spawnCount = 0
        val liveObject = makePowerUp(deleted = false)

        val mgr = buildManager(spawnDelay = 5f)
        mgr.onCreatePowerUp = { _, _ ->
            spawnCount++
            liveObject
        }

        // First tick: timer fires, liveObject is stored as state.liveObject
        mgr.think(6f)
        assertEquals(1, spawnCount, "should fire once after timer expires")

        // Mark the live object as deleted — state.liveObject.isDeletePending == true
        liveObject.setDeletePending()

        // Next tick: stale reference cleared, timer counts down (6f > 5f) and fires again
        mgr.think(6f)
        assertEquals(2, spawnCount, "should spawn again after live object is deleted")
    }
}
