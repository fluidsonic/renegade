# Phase 6: SpawnManager Timers + PowerUp Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add powerup-spawner timer logic to `SpawnManager`, implement `PowerUpGameObj` pickup mechanics with health/shield/weapon grants, add God respawn cooldown, and wire everything into the `GameServer` tick loop.

**Architecture:** `SpawnManager` grows a `think(deltaSeconds, server)` method that accumulates time, checks each `isMultiplayWeaponSpawner` spawner's per-instance countdown, and calls `server.createPowerUp()` when the timer fires. `PowerUpGameObj` overrides `think(deltaSeconds)` to do a simple 3D distance scan against all live soldiers; when a soldier is within 3 m it calls `grant(soldier)` which applies health/shield/weapon grants and marks the object delete-pending. `God.deleteSoldier()` now starts a 3-second respawn countdown per player, and `God.think()` receives `deltaSeconds` to decrement those timers before checking who needs a soldier.

**Simplifications:**
- Pickup collision is a simple 3D distance check `<= 3.0f` (no AABB/physics — deferred to Phase 11)
- Powerup spawn delay uses `SpawnerDefClass.spawnDelay` (already parsed from map data; fallback constant `10.0f`)
- `persistent` powerups are NOT implemented — always delete after one grant
- `SpawnManager.AUTO_SPAWN_CHECK_DELAY = 2.0f` matches C++ `SpawnManager::Update()` 2-second accumulator

**Tech Stack:** Kotlin, kotlin.test, existing GameServer/God/SpawnManager infrastructure.

**C++ Reference:**
- `original-untouched/Code/Combat/spawn.cpp` — `SpawnManager::Update()`, `SpawnerClass::Check_Auto_Spawn()`
- `original-untouched/Code/Combat/powerup.cpp` — `PowerUpGameObj::Think()`, `Grant()`

---

## Task 1: Fix PowerUpGameObjDef parsing in FullDefinitionLoader

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt`
- Modify: `kotlin-server/server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt`

### Context

`parsePowerUpGameObjDef` already exists in `PowerUpGameObjDef.kt` with signature:

```kotlin
fun parsePowerUpGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): PowerUpGameObjDef?
```

In `FullDefinitionLoader.dispatch()`, line 93 currently reads:

```kotlin
// FIXME: PowerUpGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
PowerUpGameObjDef.CHUNK_ID -> fallback
```

The fix is to call the parser. Note that `PowerUpGameObjDef` is already a `data class` that does NOT extend `DefinitionClass`. The parser returns `PowerUpGameObjDef?` which ALSO does not extend `DefinitionClass`. However, `dispatch()` must return `DefinitionClass`. Therefore the plan calls for converting `PowerUpGameObjDef` to extend `DefinitionClass` (like `SpawnerDefClass` does) — OR, since we don't need to look it up from the registry for the spawner path (we look up by `spawnDefinitionIdList` ID), we can keep it as a `data class` and store a companion map, OR simply store the parsed object as a plain `DefinitionClass` fallback and access it via a separate registry.

**Decision:** Convert `PowerUpGameObjDef` to extend `DefinitionClass` (same pattern as `SpawnerDefClass`), update `parsePowerUpGameObjDef` accordingly, then wire it into `FullDefinitionLoader.dispatch()`. This lets `registry.findById(id) as? PowerUpGameObjDef` work naturally.

### Step 1: Write the failing test first

Add to `FullDefinitionLoaderTest.kt`:

```kotlin
@Test
fun `PowerUpGameObjDef dispatches to typed subclass`() {
    // 0x00040107 = CHUNKID_GAME_OBJECT_DEF_POWERUP
    // parsePowerUpGameObjDef needs CHUNKID_DEF_VARIABLES=909991657 inside OBJDATA
    val powerUpVars = buildChunk(909991657u, byteArrayOf(), isContainer = false)
    val ddb = buildDdbWithExtra(Triple(0x00040107u, 999u, "PowerUp_Health") to powerUpVars)
    val registry = FullDefinitionLoader.load(ddb)

    assertEquals(1, registry.size)
    val def = registry.findById(999u)
    assertNotNull(def)
    assertEquals("PowerUp_Health", def.name)
    assertTrue(def is ccr.server.defs.combat.PowerUpGameObjDef)
}
```

### Step 2: Run test — Expected FAIL

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.FullDefinitionLoaderTest*" 2>&1 | tail -10
```

Expected: test fails because `PowerUpGameObjDef.CHUNK_ID -> fallback` returns a plain `DefinitionClass`.

### Step 3: Implement

**3a. Rewrite `PowerUpGameObjDef.kt`** — change from `data class` to `class` extending `DefinitionClass`:

```kotlin
package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of PowerUpGameObjDef (Combat/powerup.h).
 *
 * C++ defaults (powerup.cpp constructor):
 *   GrantShieldType(0), GrantShieldStrength(0), GrantShieldStrengthMax(0),
 *   GrantHealth(0), GrantHealthMax(0), GrantWeaponID(0), GrantWeapon(true),
 *   GrantWeaponClips(false), GrantWeaponRounds(0), Persistent(false),
 *   GrantKey(0), GrantSoundID(0), IdleSoundID(0), AlwaysAllowGrant(false)
 */
class PowerUpGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val grantShieldType: Int = 0,
    val grantShieldStrength: Float = 0f,
    val grantShieldStrengthMax: Float = 0f,
    val grantHealth: Float = 0f,
    val grantHealthMax: Float = 0f,
    val grantWeaponId: Int = 0,
    val grantWeapon: Boolean = true,
    val grantWeaponClips: Boolean = false,
    val grantWeaponRounds: Int = 0,
    val persistent: Boolean = false,
    val grantKey: Int = 0,
    val grantSoundId: Int = 0,
    val idleSoundId: Int = 0,
    val grantAnimationName: String = "",
    val idleAnimationName: String = "",
    val alwaysAllowGrant: Boolean = false,
) : DefinitionClass(name, id, chunkId) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040107u  // CHUNKID_GAME_OBJECT_DEF_POWERUP
    }
}

// Chunk IDs from powerup.cpp enum (line 107)
private const val CHUNKID_DEF_VARIABLES = 909991657u // CHUNKID_DEF_PARENT + 1

// Micro-chunk IDs from powerup.cpp enum (line 111-134)
private const val MICROCHUNKID_DEF_PERSISTENT = 2
private const val MICROCHUNKID_DEF_GRANT_SHIELD_TYPE = 3
private const val MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH = 4
private const val MICROCHUNKID_DEF_GRANT_HEALTH = 6
private const val MICROCHUNKID_DEF_GRANT_WEAPON_ID = 8
private const val MICROCHUNKID_DEF_GRANT_WEAPON = 9
private const val MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS = 10
private const val MICROCHUNKID_DEF_GRANT_ANIMATION_NAME = 13
private const val MICROCHUNKID_DEF_GRANT_SOUNDID = 14
private const val MICROCHUNKID_DEF_IDLE_ANIMATION_NAME = 15
private const val MICROCHUNKID_DEF_IDLE_SOUNDID = 16
private const val MICROCHUNKID_DEF_GRANT_KEY = 17
private const val MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT = 18
private const val MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS = 19
private const val MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX = 20
private const val MICROCHUNKID_DEF_GRANT_HEALTH_MAX = 21

/**
 * Parses a PowerUpGameObjDef from the OBJDATA chunk.
 * [name], [id], and [chunkId] are already extracted by the definition DB reader.
 */
fun parsePowerUpGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): PowerUpGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return PowerUpGameObjDef(name = name, id = id, chunkId = chunkId)

    return PowerUpGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        grantShieldType = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SHIELD_TYPE) ?: 0,
        grantShieldStrength = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH) ?: 0f,
        grantShieldStrengthMax = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX) ?: 0f,
        grantHealth = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_HEALTH) ?: 0f,
        grantHealthMax = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_HEALTH_MAX) ?: 0f,
        grantWeaponId = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_WEAPON_ID) ?: 0,
        grantWeapon = vars.readMicroBool(MICROCHUNKID_DEF_GRANT_WEAPON) ?: true,
        grantWeaponClips = vars.readMicroBool(MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS) ?: false,
        grantWeaponRounds = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS) ?: 0,
        persistent = vars.readMicroBool(MICROCHUNKID_DEF_PERSISTENT) ?: false,
        grantKey = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_KEY) ?: 0,
        grantSoundId = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SOUNDID) ?: 0,
        idleSoundId = vars.readMicroInt(MICROCHUNKID_DEF_IDLE_SOUNDID) ?: 0,
        grantAnimationName = vars.readMicroString(MICROCHUNKID_DEF_GRANT_ANIMATION_NAME) ?: "",
        idleAnimationName = vars.readMicroString(MICROCHUNKID_DEF_IDLE_ANIMATION_NAME) ?: "",
        alwaysAllowGrant = vars.readMicroBool(MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT) ?: false,
    )
}
```

**3b. Update `FullDefinitionLoader.kt`** — replace the FIXME line:

Replace:
```kotlin
// FIXME: PowerUpGameObjDef is a data class — convert to DefinitionClass subclass and parse properly
PowerUpGameObjDef.CHUNK_ID -> fallback
```

With:
```kotlin
PowerUpGameObjDef.CHUNK_ID ->
    parsePowerUpGameObjDef(objDataChunk, name, id, chunkId)
```

Also add the import at the top of `FullDefinitionLoader.kt` (if not already present via wildcard):
```kotlin
import ccr.server.defs.combat.parsePowerUpGameObjDef
```

Note: The existing wildcard `import ccr.server.defs.combat.*` already covers this if `parsePowerUpGameObjDef` is a top-level function in that package.

### Step 4: Run test — Expected PASS

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.FullDefinitionLoaderTest*" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

### Step 5: Commit

```bash
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup add \
    kotlin-server/server/src/main/kotlin/ccr/server/defs/combat/PowerUpGameObjDef.kt \
    kotlin-server/server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt \
    kotlin-server/server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup commit -m "$(cat <<'EOF'
feat: wire PowerUpGameObjDef parser into FullDefinitionLoader

PowerUpGameObjDef converted from data class to DefinitionClass subclass
so registry.findById() returns typed PowerUpGameObjDef. parsePowerUpGameObjDef
is now called from dispatch() instead of falling through to the base fallback.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: God respawn cooldown

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/God.kt`
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`
- Create: `kotlin-server/server/src/test/kotlin/ccr/server/GodRespawnCooldownTest.kt`

### Context

`God.think()` currently takes **no parameters** (line 57 of `God.kt`). `GameServer.networkTickLoop()` calls `god.think()` at line 424. The think signature must be changed to `fun think(deltaSeconds: Float)` and the call site updated.

`deleteSoldier(rhostId)` currently has no timer logic — after adding the cooldown, it must set `respawnTimers[rhostId] = RESPAWN_DELAY_SECONDS`.

`think(deltaSeconds)` must decrement timers and skip the `createCommando` call if the timer for that player is still > 0.

`RESPAWN_DELAY_SECONDS = 3.0f` — matches typical Renegade respawn time.

### Step 1: Write the failing test

Create `kotlin-server/server/src/test/kotlin/ccr/server/GodRespawnCooldownTest.kt`:

```kotlin
package ccr.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that God enforces a respawn cooldown after deleteSoldier.
 *
 * Uses a minimal fake: playerInGame contains a rhostId, but soldiersByHost does NOT
 * (soldier was just deleted). god.think() should NOT call createCommando until the
 * cooldown timer has expired.
 *
 * Since we can't easily construct a real GameServer, we test the timer state directly
 * via public observable behavior: soldiersByHost grows only after enough ticks.
 */
class GodRespawnCooldownTest {

    /**
     * Minimal God subclass that overrides createCommando to track whether it was called.
     * We bypass the real GameServer by passing a null-like reference via a minimal server stub.
     */
    private class TrackingGod(server: GameServer) : God(server) {
        var createCommandoCalled = false
        override fun createCommando(rhostId: Int, playerType: Int) = null.also {
            createCommandoCalled = true
        }
    }

    @Test
    fun `respawn is suppressed during cooldown period`() {
        // Create a minimal server configuration with no map (avoids I/O)
        val config = ServerConfig(mapName = "", gamePort = 4848, rconPort = 4849)
        val server = GameServer(config)
        val god = TrackingGod(server)

        val rhostId = 1
        god.state = God.State.MULTIPLAYER
        god.playerInGame.add(rhostId)
        god.playerTeams[rhostId] = 0

        // Start cooldown — simulates deleteSoldier starting the timer
        god.startRespawnCooldown(rhostId)

        // Tick with 1.0s — timer is 3s, so createCommando must NOT be called
        god.think(1.0f)
        assertFalse(god.createCommandoCalled, "createCommando must not be called during cooldown (1s elapsed)")

        // Tick another 1.5s (total 2.5s) — still in cooldown
        god.think(1.5f)
        assertFalse(god.createCommandoCalled, "createCommando must not be called during cooldown (2.5s elapsed)")

        // Tick another 1.0s (total 3.5s) — cooldown expired, createCommando should be called
        god.think(1.0f)
        assertTrue(god.createCommandoCalled, "createCommando must be called after 3+ seconds")
    }

    @Test
    fun `no cooldown on first spawn (new player)`() {
        val config = ServerConfig(mapName = "", gamePort = 4848, rconPort = 4849)
        val server = GameServer(config)
        val god = TrackingGod(server)

        val rhostId = 2
        god.state = God.State.MULTIPLAYER
        god.playerInGame.add(rhostId)
        god.playerTeams[rhostId] = 1
        // No startRespawnCooldown called — first spawn has no cooldown

        god.think(0.016f)  // one frame
        assertTrue(god.createCommandoCalled, "first spawn must not have a cooldown")
    }
}
```

**Note on test architecture:** Because `God` references `GameServer` (for `server.gameState.isIntermission`, `server.spawnManager`, `server.nodSoldierDefId`, `server.gdiSoldierDefId`, etc.), a full unit test requires a real `GameServer` instance or a refactor to inject interfaces. For simplicity: the test creates a `ServerConfig` with `mapName = ""` (skips I/O in `loadLevel()`), constructs a real `GameServer(config)` but overrides `createCommando` in a subclass. **Alternatively**, if `GameServer` is hard to construct in tests, refactor to pass `createCommando` as a lambda — but prefer the subclass approach since it avoids changing the public API.

**Simpler alternative test approach** (avoids needing a live GameServer): test the timer map directly as a unit, without instantiating GameServer. Add a package-internal helper `fun respawnTimerRemaining(rhostId: Int): Float` to `God` so tests can inspect state. See step 3 for details.

### Step 2: Run test — Expected FAIL

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.GodRespawnCooldownTest*" 2>&1 | tail -10
```

Expected: compilation error or test failure (think() signature doesn't match / no startRespawnCooldown).

### Step 3: Implement

**3a. Modify `God.kt`:**

Add after the C4 tracking fields (around line 49):

```kotlin
// Respawn cooldown — set by deleteSoldier, decremented by think()
private val respawnTimers = mutableMapOf<Int, Float>()  // rhostId → remaining seconds

companion object {
    const val RESPAWN_DELAY_SECONDS = 3.0f
}
```

Change `fun think()` signature to `fun think(deltaSeconds: Float)`:

```kotlin
fun think(deltaSeconds: Float) {
    if (playerInGame.isEmpty()) return
    if (server.gameState.isIntermission) return

    if (state == State.UNINITIALIZED) {
        state = State.MULTIPLAYER
    }

    if (state == State.SINGLE_INIT) {
        state = State.SINGLE_RUNNING
    }

    if (state == State.SINGLE_RUNNING) {
        // Fix player-soldier links after load — stub
    }

    if (state == State.MULTIPLAYER) {
        // Decrement respawn timers first
        val timerIter = respawnTimers.iterator()
        while (timerIter.hasNext()) {
            val entry = timerIter.next()
            entry.setValue(entry.value - deltaSeconds)
            if (entry.value <= 0f) timerIter.remove()
        }

        for (rhostId in playerInGame.toList()) {
            if (rhostId !in soldiersByHost) {
                // Skip if still in cooldown
                if (respawnTimers.containsKey(rhostId)) continue

                val player = playersByHost[rhostId] ?: continue
                createCommando(player)
            }
        }
    }
}
```

Add a helper for tests to inspect timer state (package-internal):

```kotlin
internal fun respawnTimerRemaining(rhostId: Int): Float = respawnTimers[rhostId] ?: 0f
```

Add `startRespawnCooldown` (also called by `deleteSoldier`):

```kotlin
internal fun startRespawnCooldown(rhostId: Int) {
    respawnTimers[rhostId] = RESPAWN_DELAY_SECONDS
}
```

In `deleteSoldier(rhostId)`, add after removing the soldier from `soldiersByHost`:

```kotlin
// Start respawn cooldown — prevents immediate re-spawn on next think() tick
startRespawnCooldown(rhostId)
```

The full updated `deleteSoldier` method (replace lines 464-476):

```kotlin
fun deleteSoldier(rhostId: Int) {
    if (rhostId in playerVehicles) {
        exitVehicle(rhostId)
    }
    c4Objects.filter { !it.isDeletePending && it.ownerRhostId == rhostId && it.ammoDefinition?.ammoType == AMMO_TYPE_C4_REMOTE }
        .forEach { it.defuse() }
    val soldier = soldiersByHost.remove(rhostId) ?: return
    server.gameObjManager.removeStar(soldier)
    soldier.setDeletePending()
    // Start respawn cooldown — prevents immediate re-spawn on next think() tick
    startRespawnCooldown(rhostId)
    println("[GOD] marked soldier delete-pending for host $rhostId netId=${soldier.networkId}")
}
```

**3b. Modify `GameServer.kt`** — update the call site at line 424:

Replace:
```kotlin
god.think()
```
With:
```kotlin
god.think(frameDeltaSeconds)
```

### Step 4: Run test — Expected PASS

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.GodRespawnCooldownTest*" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

Also verify full test suite still passes:
```
kotlin-server/gradlew -p kotlin-server :server:test 2>&1 | tail -10
```

### Step 5: Commit

```bash
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup add \
    kotlin-server/server/src/main/kotlin/ccr/server/God.kt \
    kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt \
    kotlin-server/server/src/test/kotlin/ccr/server/GodRespawnCooldownTest.kt
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup commit -m "$(cat <<'EOF'
feat: God respawn cooldown — 3-second delay after soldier deletion

God.think() now takes deltaSeconds: Float. Respawn timers per rhostId are
decremented each tick; createCommando is suppressed while timer > 0.
deleteSoldier() starts a 3s cooldown. GameServer call site updated.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: SpawnManager powerup timer + think()

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/SpawnManager.kt`
- Create: `kotlin-server/server/src/test/kotlin/ccr/server/SpawnManagerPowerUpTimerTest.kt`

### Context

`SpawnerDefClass` has:
- `isMultiplayWeaponSpawner: Boolean` — true for weapon/powerup spawners
- `spawnDefinitionIdList: List<Int>` — list of definition IDs of the objects to spawn
- `spawnDelay: Float = 10f` — already parsed from map data

`LoadedSpawner` has:
- `enabled: Boolean`
- `definitionId: Int` — the SpawnerDefClass def ID (not the object def ID)
- `transform: Matrix3D` — spawn position is `transform.position`

The object to spawn is identified by `SpawnerDefClass.spawnDefinitionIdList[0]` — looked up in the registry as a `PowerUpGameObjDef`.

`SpawnManager` currently has a private `spawners: List<ResolvedSpawner>` (each resolved to a `SpawnerDefClass`). We add a parallel private list `powerUpSpawners: List<PowerUpSpawnerState>` that only includes spawners where `def.isMultiplayWeaponSpawner && spawner.enabled`.

### Step 1: Write the failing test

Create `kotlin-server/server/src/test/kotlin/ccr/server/SpawnManagerPowerUpTimerTest.kt`:

```kotlin
package ccr.server

import ccr.math.Vector3
import ccr.server.defs.DefinitionClass
import ccr.server.defs.combat.PowerUpGameObjDef
import ccr.server.defs.combat.SpawnerDefClass
import ccr.server.level.DefinitionRegistry
import ccr.server.level.LoadedLevel
import ccr.server.level.Matrix3D
import ccr.server.level.StaticLevelData
import ccr.server.level.ldd.DynamicLevelData
import ccr.server.level.ldd.LoadedSpawner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnManagerPowerUpTimerTest {

    /** Minimal Matrix3D with a position vector. */
    private fun matrixAt(x: Float, y: Float, z: Float): Matrix3D {
        // Matrix3D stores column-major 3×4: columns 0-2 are rotation rows, column 3 is translation.
        // For a test we just need position; fill rotation with identity and set translation.
        val cols = Array(4) { Vector3(0f, 0f, 0f) }
        cols[0] = Vector3(1f, 0f, 0f)
        cols[1] = Vector3(0f, 1f, 0f)
        cols[2] = Vector3(0f, 0f, 1f)
        cols[3] = Vector3(x, y, z)
        return Matrix3D(cols)
    }

    private fun makeLoadedLevel(
        spawners: List<LoadedSpawner>,
        extraDefs: List<DefinitionClass> = emptyList(),
    ): LoadedLevel {
        val registry = DefinitionRegistry()
        // Register a SpawnerDefClass with ID matching the spawner's definitionId
        for (s in spawners) {
            val spawnerDef = SpawnerDefClass(
                name = "Spawner_${s.id}",
                id = s.definitionId.toUInt(),
                chunkId = SpawnerDefClass.CHUNK_ID,
                isMultiplayWeaponSpawner = true,
                spawnDefinitionIdList = listOf(s.definitionId + 1000),  // object def ID convention
                spawnDelay = 10f,
            )
            registry.register(spawnerDef)
        }
        for (d in extraDefs) registry.register(d)
        return LoadedLevel(
            definitions = registry,
            staticData = StaticLevelData(),
            dynamicData = DynamicLevelData(spawners = spawners),
            worldExtents = null,
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

        val spawner = LoadedSpawner(
            id = 1,
            transform = matrixAt(10f, 20f, 5f),
            spawnTransform = null,
            definitionId = spawnerDefId,
            spawnCount = 0,
            enabled = true,
            spawnPoints = emptyList(),
            scripts = emptyList(),
        )

        val level = makeLoadedLevel(listOf(spawner), extraDefs = listOf(powerUpDef))
        val createdPowerUps = mutableListOf<Pair<Vector3, PowerUpGameObjDef>>()

        val sm = SpawnManager(level)
        // Replace createPowerUp with tracking lambda
        sm.onCreatePowerUp = { pos, def -> createdPowerUps.add(Pair(pos, def)) }

        // Tick for 9 seconds — not yet fired (delay is 10s)
        sm.think(9.0f)
        assertEquals(0, createdPowerUps.size, "should not fire before 10 seconds")

        // Tick 2 more seconds (total 11s) — should fire exactly once
        sm.think(2.0f)
        assertEquals(1, createdPowerUps.size, "should fire after 10+ seconds")
        assertEquals(powerUpDef, createdPowerUps[0].second)

        // Timer reset — ticking 9 more seconds should not fire again
        sm.think(9.0f)
        assertEquals(1, createdPowerUps.size, "timer should reset, not fire again until next delay")
    }

    @Test
    fun `disabled spawner is not ticked`() {
        val spawner = LoadedSpawner(
            id = 2,
            transform = matrixAt(0f, 0f, 0f),
            spawnTransform = null,
            definitionId = 99,
            spawnCount = 0,
            enabled = false,  // disabled
            spawnPoints = emptyList(),
            scripts = emptyList(),
        )

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
            isMultiplayWeaponSpawner = false,  // NOT a weapon spawner
            isSoldierStartup = true,
        )
        registry.register(soldierSpawnerDef)

        val spawner = LoadedSpawner(
            id = 3,
            transform = matrixAt(0f, 0f, 0f),
            spawnTransform = null,
            definitionId = 77,
            spawnCount = 0,
            enabled = true,
            spawnPoints = emptyList(),
            scripts = emptyList(),
        )

        val level = LoadedLevel(
            definitions = registry,
            staticData = StaticLevelData(),
            dynamicData = DynamicLevelData(spawners = listOf(spawner)),
            worldExtents = null,
        )

        val sm = SpawnManager(level)
        val fired = mutableListOf<Unit>()
        sm.onCreatePowerUp = { _, _ -> fired.add(Unit) }

        sm.think(100f)
        assertEquals(0, fired.size, "soldier spawner must not be ticked as powerup")
    }
}
```

**Note on `onCreatePowerUp` callback:** Rather than passing a `GameServer` to `think()` and calling `server.createPowerUp()`, make `SpawnManager` expose an `onCreatePowerUp: ((Vector3, PowerUpGameObjDef) -> Unit)?` callback. This decouples the timer logic from the server and makes it easily testable. `GameServer` sets this callback during init.

### Step 2: Run test — Expected FAIL

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.SpawnManagerPowerUpTimerTest*" 2>&1 | tail -10
```

Expected: compilation error — `think()`, `onCreatePowerUp` don't exist on `SpawnManager` yet.

### Step 3: Implement

Replace `SpawnManager.kt` with the full implementation:

```kotlin
package ccr.server

import ccr.math.Vector3
import ccr.server.defs.combat.PowerUpGameObjDef
import ccr.server.defs.combat.SpawnerDefClass
import ccr.server.level.LoadedLevel
import ccr.server.level.ldd.LoadedSpawner

/**
 * Encapsulates spawn location logic for multiplayer spawning.
 * Matches C++ SpawnManager::Get_Multiplayer_Spawn_Location() (spawn.cpp:796-880).
 *
 * Also owns the powerup-spawner timer logic that matches C++ SpawnManager::Update()
 * and SpawnerClass::Check_Auto_Spawn() (spawn.cpp).
 *
 * Filters soldier spawners by: !isPrimary && isSoldierStartup && playerType == effectiveTeam.
 * No collision checks or furthest-from-enemy logic (no physics engine yet).
 */
class SpawnManager(level: LoadedLevel) {

    private data class ResolvedSpawner(
        val spawner: LoadedSpawner,
        val def: SpawnerDefClass,
    )

    private data class PowerUpSpawnerState(
        val spawner: LoadedSpawner,
        val def: SpawnerDefClass,
        val powerUpDef: PowerUpGameObjDef,
        val spawnDelay: Float,
        var timer: Float,
    )

    private val spawners: List<ResolvedSpawner>
    private val powerUpSpawners: List<PowerUpSpawnerState>

    // Callback invoked when a powerup spawner's timer fires.
    // Arguments: spawn position, PowerUpGameObjDef to create.
    // GameServer sets this after constructing SpawnManager.
    var onCreatePowerUp: ((position: Vector3, def: PowerUpGameObjDef) -> Unit)? = null

    init {
        val rawSpawners = level.dynamicData.spawners
        println("[SPAWN] ${rawSpawners.size} spawner instances in LDD")

        spawners = rawSpawners.mapNotNull { spawner ->
            val def = level.definitions.findById(spawner.definitionId.toUInt())
            when {
                def == null ->
                    null.also { println("[SPAWN]   defId=${spawner.definitionId}: NOT FOUND in registry") }
                def !is SpawnerDefClass ->
                    null.also { println("[SPAWN]   defId=${spawner.definitionId}/${def.chunkId} name='${def.name}': found but is ${def::class.simpleName} (not SpawnerDefClass)") }
                else -> {
                    println("[SPAWN]   defId=${spawner.definitionId} name='${def.name}': " +
                        "isPrimary=${def.isPrimary} isSoldierStartup=${def.isSoldierStartup} playerType=${def.playerType}")
                    ResolvedSpawner(spawner, def)
                }
            }
        }

        // Build powerup spawner list — enabled weapon spawners with a known PowerUpGameObjDef
        powerUpSpawners = spawners.mapNotNull { (spawner, def) ->
            if (!spawner.enabled) return@mapNotNull null
            if (!def.isMultiplayWeaponSpawner) return@mapNotNull null

            val objectDefId = def.spawnDefinitionIdList.firstOrNull() ?: return@mapNotNull null
            val powerUpDef = level.definitions.findById(objectDefId.toUInt()) as? PowerUpGameObjDef
                ?: return@mapNotNull null.also {
                    println("[SPAWN]   weapon spawner defId=${spawner.definitionId} has objectDefId=$objectDefId but not a PowerUpGameObjDef, skipping")
                }

            val delay = if (def.spawnDelay > 0f) def.spawnDelay else SPAWN_DELAY_DEFAULT
            println("[SPAWN]   powerup spawner: spawnerDefId=${spawner.definitionId} " +
                "powerUpName='${powerUpDef.name}' delay=${delay}s pos=" +
                "(${spawner.transform.position.x}, ${spawner.transform.position.y}, ${spawner.transform.position.z})")
            PowerUpSpawnerState(spawner, def, powerUpDef, delay, timer = delay)
        }

        val nodSoldier = spawners.count { !it.def.isPrimary && it.def.isSoldierStartup && it.def.playerType == 0 }
        val gdiSoldier = spawners.count { !it.def.isPrimary && it.def.isSoldierStartup && it.def.playerType == 1 }
        val weapon = spawners.count { it.def.isMultiplayWeaponSpawner }
        println("[SPAWN] resolved ${spawners.size} spawners: $nodSoldier NOD soldier, $gdiSoldier GDI soldier, $weapon weapon (${powerUpSpawners.size} with PowerUpGameObjDef)")
    }

    /**
     * Returns a random spawn position for the given playerType.
     * C++: SpawnManager::Get_Multiplayer_Spawn_Location(playerType) — spawn.cpp:796-880.
     *
     * @param playerType  0=NOD, 1=GDI, -1=RENEGADE (neutral/unassigned → remapped to RENEGADE)
     */
    fun getMultiplayerSpawnLocation(playerType: Int): Pair<Vector3, Float> {
        val effectiveType = if (playerType == PLAYERTYPE_NEUTRAL) PLAYERTYPE_RENEGADE else playerType

        val candidates = spawners.filter { (_, def) ->
            !def.isPrimary && def.isSoldierStartup && def.playerType == effectiveType
        }

        if (candidates.isEmpty()) {
            println("[SPAWN] WARNING: no spawners for playerType=$effectiveType, using origin fallback")
            return Pair(Vector3(0f, 0f, 5f), 0f)
        }

        val selected = candidates.random()
        val levelPos = selected.spawner.transform.position
        println("[SPAWN] selected spawner defId=${selected.spawner.definitionId} " +
            "pos=(${levelPos.x}, ${levelPos.y}, ${levelPos.z})")
        return Pair(Vector3(levelPos.x, levelPos.y, levelPos.z), 0f)
    }

    /**
     * Ticks all powerup spawner timers.
     * C++: SpawnManager::Update() (spawn.cpp) accumulates time and calls Check_Auto_Spawn().
     *
     * @param deltaSeconds  time elapsed since last tick
     */
    fun think(deltaSeconds: Float) {
        if (powerUpSpawners.isEmpty()) return

        for (state in powerUpSpawners) {
            state.timer -= deltaSeconds
            if (state.timer <= 0f) {
                // Reset timer first, then fire
                state.timer = state.spawnDelay
                val pos = state.spawner.transform.position
                println("[SPAWN] powerup spawner fired: '${state.powerUpDef.name}' at (${pos.x}, ${pos.y}, ${pos.z})")
                onCreatePowerUp?.invoke(Vector3(pos.x, pos.y, pos.z), state.powerUpDef)
            }
        }
    }

    companion object {
        private const val PLAYERTYPE_NEUTRAL  = -2
        private const val PLAYERTYPE_RENEGADE = -1

        /** Fallback spawn delay if SpawnerDefClass.spawnDelay is 0 or missing. */
        const val SPAWN_DELAY_DEFAULT = 10.0f
    }
}
```

### Step 4: Run test — Expected PASS

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.SpawnManagerPowerUpTimerTest*" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

Also run all tests:
```
kotlin-server/gradlew -p kotlin-server :server:test 2>&1 | tail -10
```

### Step 5: Commit

```bash
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup add \
    kotlin-server/server/src/main/kotlin/ccr/server/SpawnManager.kt \
    kotlin-server/server/src/test/kotlin/ccr/server/SpawnManagerPowerUpTimerTest.kt
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup commit -m "$(cat <<'EOF'
feat: SpawnManager powerup spawner timers

SpawnManager.think(deltaSeconds) ticks per-spawner countdown for all enabled
isMultiplayWeaponSpawner spawners. When the timer fires it invokes onCreatePowerUp
callback with the spawn position and PowerUpGameObjDef. Timer resets to spawnDelay
after each fire (matching C++ Check_Auto_Spawn).

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: PowerUpGameObj think() + grant()

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/net/PowerUpGameObj.kt`
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/net/SoldierGameObj.kt`
- Modify: `kotlin-server/server/src/test/kotlin/ccr/server/net/PowerUpGameObjTest.kt`

### Context

`SoldierGameObj.weapons` is currently `val weapons: List<WeaponEntry>`. To add a weapon at runtime, it must be changed to `val weapons: MutableList<WeaponEntry>` (initialized with `mutableListOf()`). Check that `exportOccasional` still works — it iterates `weapons`, which works fine with `MutableList<WeaponEntry>`.

`PowerUpGameObj` needs:
- A reference to a `GameServer` (as `serverRef`) so `think()` can iterate `server.god.playerInGame` and `server.god.soldiersByHost`
- A reference to `PowerUpGameObjDef` (as `powerUpDef`)
- A `granted: Boolean` guard to avoid double-grant

The `grant(soldier)` method:
- Adds `def.grantHealth` to `soldier.health` capped at `soldier.healthMax`
- Adds `def.grantShieldStrength` to `soldier.shieldStrength` capped at `soldier.shieldStrengthMax`
- If `def.grantWeapon && def.grantWeaponId != 0`: adds `WeaponEntry(def.grantWeaponId, def.grantWeaponRounds)` to `soldier.weapons`, then calls `soldier.setObjectDirtyBit(NetworkObject.BIT_RARE, true)` for all in-game clients
- If health or shield changed: calls `soldier.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)` for all in-game clients
- Calls `setDeletePending()` on the powerup (always; `persistent` not implemented in Phase 6)

Pickup radius: 3m → distance-squared check `<= 9.0f`.

### Step 1: Write the failing test

Extend `PowerUpGameObjTest.kt`:

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.server.defs.combat.PowerUpGameObjDef
import kotlin.test.Test
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
            definitionId = 200,
            position     = Vector3(1f, 2f, 3f),
            modelName    = "somemodel",
            health       = 50f,
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
```

**Note on testability:** `PowerUpGameObj.think(deltaSeconds)` normally accesses `server.god.soldiersByHost`. To keep tests pure (no GameServer), extract the inner logic into `internal fun thinkWithSoldiers(soldiers: List<SoldierGameObj>, inGameClientIds: Set<Int>, deltaSeconds: Float)`. The real `think(deltaSeconds)` override calls this by delegating to `serverRef?.god?.soldiersByHost?.values?.toList()` and `serverRef?.god?.playerInGame`.

### Step 2: Run test — Expected FAIL

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.PowerUpGameObjTest*" 2>&1 | tail -10
```

Expected: compilation error — `powerUpDef`, `grant()`, `thinkWithSoldiers()` don't exist.

### Step 3: Implement

**3a. Modify `SoldierGameObj.kt`** — change `val weapons: List<WeaponEntry>` to `val weapons: MutableList<WeaponEntry>`:

In the constructor:
```kotlin
val weapons: MutableList<WeaponEntry> = mutableListOf(),
```

Change the default parameter from `= emptyList()` to `= mutableListOf()`. All call sites that pass `weapons = buildList { ... }` should be changed to `weapons = mutableListOf(...) // or buildList { }.toMutableList()`. Check usages in `God.kt` (lines 175-178 and 232-235) and update:

```kotlin
val weapons = mutableListOf<WeaponEntry>().also { list ->
    if (server.pistolWeaponDefId != 0) list.add(WeaponEntry(server.pistolWeaponDefId, 100))
    if (server.timedC4WeaponDefId != 0) list.add(WeaponEntry(server.timedC4WeaponDefId, 1))
}
```

**3b. Rewrite `PowerUpGameObj.kt`**:

```kotlin
package ccr.server.net

import ccr.math.Vector3
import ccr.net.replication.NetworkObject
import ccr.server.GameServer
import ccr.server.defs.combat.PowerUpGameObjDef

// C++: PowerUpGameObj — extends SimpleGameObj.
// Is_Always_Dirty returns false — no Export_Creation/Rare/Occasional/Frequent overrides.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → PowerUpGameObj
class PowerUpGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    /** The definition that specifies what this powerup grants. Set by GameServer after creation. */
    var powerUpDef: PowerUpGameObjDef? = null

    /** Reference to the server — used by think() to iterate live soldiers. */
    var serverRef: GameServer? = null

    private var granted = false

    // C++: PowerUpGameObj::Think — checks bounding-box overlap with soldiers, calls Grant() on hit.
    override fun think(deltaSeconds: Float) {
        if (isDeletePending || granted) return
        val server = serverRef ?: return
        val soldiers = server.god.soldiersByHost.values.toList()
        val inGameClientIds = server.god.playerInGame.toSet()
        thinkWithSoldiers(soldiers, inGameClientIds, deltaSeconds)
    }

    /**
     * Testable inner logic — separated from GameServer dependency.
     * Scans [soldiers] for any within PICKUP_RADIUS_M and calls [grant] on the first found.
     */
    internal fun thinkWithSoldiers(
        soldiers: List<SoldierGameObj>,
        inGameClientIds: Set<Int>,
        deltaSeconds: Float,
    ) {
        if (isDeletePending || granted) return
        val def = powerUpDef ?: return

        for (soldier in soldiers) {
            val dx = soldier.position.x - position.x
            val dy = soldier.position.y - position.y
            val dz = soldier.position.z - position.z
            if (dx * dx + dy * dy + dz * dz <= PICKUP_RADIUS_SQ) {
                grant(soldier, inGameClientIds)
                return
            }
        }
    }

    /**
     * Grants this powerup to [soldier].
     * C++: PowerUpGameObj::Grant(SoldierGameObj*) — powerup.cpp.
     *
     * @param inGameClientIds  set of client IDs to mark dirty bits for (empty in tests)
     */
    fun grant(soldier: SoldierGameObj, inGameClientIds: Set<Int>) {
        if (granted) return
        granted = true
        val def = powerUpDef ?: run {
            setDeletePending()
            return
        }

        var healthOrShieldChanged = false

        // Health grant
        if (def.grantHealth > 0f) {
            soldier.health = minOf(soldier.health + def.grantHealth, soldier.healthMax)
            healthOrShieldChanged = true
        }

        // Shield grant
        if (def.grantShieldStrength > 0f) {
            soldier.shieldStrength = minOf(soldier.shieldStrength + def.grantShieldStrength, soldier.shieldStrengthMax)
            healthOrShieldChanged = true
        }

        if (healthOrShieldChanged) {
            for (clientId in inGameClientIds) {
                soldier.setObjectDirtyBit(clientId, NetworkObject.BIT_OCCASIONAL, true)
            }
        }

        // Weapon grant
        if (def.grantWeapon && def.grantWeaponId != 0) {
            soldier.weapons.add(WeaponEntry(def.grantWeaponId, def.grantWeaponRounds))
            for (clientId in inGameClientIds) {
                soldier.setObjectDirtyBit(clientId, NetworkObject.BIT_RARE, true)
            }
        }

        println("[POWERUP] granted '${def.name}' to soldier netId=${soldier.networkId}: " +
            "health+=${def.grantHealth} shield+=${def.grantShieldStrength} weapon=${if (def.grantWeapon && def.grantWeaponId != 0) def.grantWeaponId else 0}")

        // Phase 6: always delete after grant (persistent not implemented until Phase 11)
        setDeletePending()
    }

    companion object {
        private const val PICKUP_RADIUS_M  = 3.0f
        private const val PICKUP_RADIUS_SQ = PICKUP_RADIUS_M * PICKUP_RADIUS_M  // 9.0f
    }
}
```

### Step 4: Run test — Expected PASS

```
kotlin-server/gradlew -p kotlin-server :server:test --tests "*.PowerUpGameObjTest*" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

Also run all tests:
```
kotlin-server/gradlew -p kotlin-server :server:test 2>&1 | tail -10
```

### Step 5: Commit

```bash
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup add \
    kotlin-server/server/src/main/kotlin/ccr/server/net/PowerUpGameObj.kt \
    kotlin-server/server/src/main/kotlin/ccr/server/net/SoldierGameObj.kt \
    kotlin-server/server/src/test/kotlin/ccr/server/net/PowerUpGameObjTest.kt
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup commit -m "$(cat <<'EOF'
feat: PowerUpGameObj think() + grant() — health/shield/weapon pickup mechanics

PowerUpGameObj.think() scans soldiers within 3m and calls grant() on the first hit.
grant() applies health/shield caps, adds weapons to the MutableList, sets dirty bits,
and marks delete-pending. SoldierGameObj.weapons changed to MutableList for runtime adds.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: GameServer wiring — createPowerUp() + tick SpawnManager

**Files:**
- Modify: `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

### Context

`SpawnManager` is already constructed in `GameServer.run()`:
```kotlin
loadedLevel?.also { level ->
    if (level.dynamicData.spawners.isNotEmpty()) {
        spawnManager = SpawnManager(level)
    }
}
```

We need to:
1. After constructing `SpawnManager`, set `spawnManager?.onCreatePowerUp = ::createPowerUp`
2. Add `internal fun createPowerUp(position: Vector3, def: PowerUpGameObjDef)` that instantiates `PowerUpGameObj`, wires `powerUpDef` and `serverRef`, registers it, and adds to `gameObjManager`
3. In `networkTickLoop()`, call `spawnManager?.think(frameDeltaSeconds)` — add it alongside `god.think(frameDeltaSeconds)` (currently at line ~424)

No new test needed here — the integration is covered by the SpawnManager unit test (Task 3) and the PowerUpGameObj unit test (Task 4). An optional smoke test that verifies `createPowerUp` registers the object with `NetworkObjectManager` is noted below.

### Step 1: No failing test needed for pure wiring

The wiring in GameServer is integration glue; tests for the components are in Tasks 3 and 4. However, add a minimal smoke test in `PowerUpGameObjTest.kt` verifying `powerUpDef` and `serverRef` can be set without error (the existing test in Task 4 already does this implicitly).

### Step 2: Implement

In `GameServer.kt`:

**2a. Add imports at top:**
```kotlin
import ccr.server.defs.combat.PowerUpGameObjDef
import ccr.server.net.PowerUpGameObj
```

**2b. Wire the callback after SpawnManager construction** (around line 169 in `run()`):

```kotlin
loadedLevel?.also { level ->
    if (level.dynamicData.spawners.isNotEmpty()) {
        spawnManager = SpawnManager(level)
        spawnManager?.onCreatePowerUp = { position, def ->
            createPowerUp(position, def)
        }
    }
}
```

**2c. Add `createPowerUp` method** (place near `createBuilding` or after the C4 helper methods at the bottom of `GameServer.kt`):

```kotlin
/**
 * Creates a PowerUpGameObj at the given position and registers it with the server.
 * Called by SpawnManager when a powerup spawner's timer fires.
 * C++: SpawnerClass::Spawn_Object() creates the object and calls Add_Network_Object().
 *
 * @param position  world position from the spawner's transform
 * @param def       the PowerUpGameObjDef describing what to grant
 */
internal fun createPowerUp(position: Vector3, def: PowerUpGameObjDef) {
    val powerUp = PowerUpGameObj(
        definitionId  = def.id.toInt(),
        position      = position,
        modelName     = "",   // C++: model comes from physics def; stub for Phase 6
    )
    powerUp.powerUpDef = def
    powerUp.serverRef  = this

    val netId = NetworkObjectManager.getNewDynamicId()
    NetworkObjectManager.registerObject(powerUp, netId)
    gameObjManager.add(powerUp)

    println("[POWERUP] spawned '${def.name}' netId=$netId at (${position.x}, ${position.y}, ${position.z})")
}
```

**2d. Tick SpawnManager in `networkTickLoop()`** — add after `gameObjManager.think(frameDeltaSeconds)` (around line 418) and before `god.think(frameDeltaSeconds)`:

```kotlin
// SpawnManager.think() — ticks powerup spawner countdown timers
spawnManager?.think(frameDeltaSeconds)
```

The updated block (lines ~417-424) becomes:

```kotlin
// GameObjManager.think() — drives building Think() loops (refinery trickle, war factory timer, etc.)
gameObjManager.think(frameDeltaSeconds)

// SpawnManager.think() — ticks powerup spawner countdown timers
spawnManager?.think(frameDeltaSeconds)

// Update measured FPS and push to clients
updateFps(nowMs)

// God.think() — handles respawn loop (creates soldiers for soldierless in-game players)
god.think(frameDeltaSeconds)
```

### Step 3: Build and run full test suite

```
kotlin-server/gradlew -p kotlin-server :server:test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

Also do a full build verification:
```
kotlin-server/gradlew -p kotlin-server build 2>&1 | tail -15
```

### Step 4: Commit

```bash
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup add \
    kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git -C /Users/marc/Documents/ccr/.worktrees/phase6-spawn-powerup commit -m "$(cat <<'EOF'
feat: wire SpawnManager.think() and createPowerUp() into GameServer tick loop

SpawnManager.onCreatePowerUp callback wired to GameServer.createPowerUp().
networkTickLoop() now ticks spawnManager each frame. createPowerUp() registers
PowerUpGameObj with NetworkObjectManager and gameObjManager for full lifecycle.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Verification Checklist

- [ ] `FullDefinitionLoaderTest` — `PowerUpGameObjDef dispatches to typed subclass` passes
- [ ] `GodRespawnCooldownTest` — cooldown suppresses spawning during 3s window, then allows spawn
- [ ] `GodRespawnCooldownTest` — first spawn (no deleteSoldier) spawns immediately
- [ ] `SpawnManagerPowerUpTimerTest` — timer fires after spawnDelay seconds
- [ ] `SpawnManagerPowerUpTimerTest` — disabled spawner never fires
- [ ] `SpawnManagerPowerUpTimerTest` — soldier-startup spawner is not ticked as powerup
- [ ] `PowerUpGameObjTest` — health grant applies with cap at healthMax
- [ ] `PowerUpGameObjTest` — weapon grant adds to soldier.weapons
- [ ] `PowerUpGameObjTest` — soldier within 3m triggers pickup
- [ ] `PowerUpGameObjTest` — soldier beyond 3m does not trigger pickup
- [ ] Full test suite: `kotlin-server/gradlew -p kotlin-server test` — BUILD SUCCESSFUL
- [ ] `kotlin-server/gradlew -p kotlin-server build` — BUILD SUCCESSFUL (no compilation errors)

---

## Implementation Notes (Potential Pitfalls)

### `God.think()` signature change
`God.think()` currently takes no parameters. After the change, any other code that calls `god.think()` (e.g., tests, if any) must be updated to pass `deltaSeconds`. Check the test suite for any direct calls to `god.think()`.

### `SoldierGameObj.weapons` mutable list
Changing from `val weapons: List<WeaponEntry>` to `val weapons: MutableList<WeaponEntry>` requires updating all call sites that pass `weapons = buildList { ... }` (in `God.kt` at `createCommando` and `createCommandoWithDef`). Change these to `.toMutableList()` or use `mutableListOf()` directly.

### `PowerUpGameObjDef` no longer a `data class`
Any code using `copy()` on `PowerUpGameObjDef` (search the codebase) must be updated. Likely none, since it was a stub.

### `GameObjManager.think()` and delete-pending guard
Per CLAUDE.md: "The delete-pending loop does NOT call `gameObjManager.remove()` — objects with custom `think()` MUST guard with `if (isDeletePending) return` at the top". `PowerUpGameObj.think()` already does this.

### `PowerUpGameObj.thinkWithSoldiers` vs `think()`
In production, `think(deltaSeconds)` is called by `GameObjManager.think()` (since `gameObjManager.add(powerUp)` is called in `createPowerUp`). The `serverRef` must be set before the first tick — this is guaranteed since `createPowerUp` sets both `powerUp.serverRef = this` and `gameObjManager.add(powerUp)` in the same synchronous method.

### Registry lookup for `PowerUpGameObjDef` by spawner
`SpawnerDefClass.spawnDefinitionIdList` contains the definition IDs of objects to spawn. `level.definitions.findById(objectDefId.toUInt()) as? PowerUpGameObjDef` works only after Task 1 (the def is registered as a typed subclass). Tasks must be executed in order.

---

## Later Phases

| Phase | Name |
|-------|------|
| 7 | Beacon System |
| 8 | Explosion System (full AoE radius) |
| 9 | Map Rotation |
| 10 | Bandwidth + Polish |
| 11 | Physics Integration |
