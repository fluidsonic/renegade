# Phase 17: Spawner Live-Object Tracking + Purchased Soldier Models

## Goal

Fix two gameplay-correctness bugs:

1. **Powerup pile-up** — `SpawnManager` never checks whether a powerup it previously
   created is still alive, so crates stack infinitely every `spawnDelay` seconds.
2. **Wrong soldier model on purchase** — buying Havoc, Sakura, etc. spawns the default
   soldier model (`c_ag_nod_mg` / `c_ag_gdi_mg`) instead of the character-specific
   W3D model from the def's `physDefId`.

## C++ Reference Behavior

### SpawnerClass::Check_Auto_Spawn (spawn.cpp)
```
if (AutoSpawnTimer > 0) return
if (LastAutoSpawnNetworkObjectID != 0) {
    if (Find_Object(LastAutoSpawnNetworkObjectID)) return  // still alive — don't spawn
    LastAutoSpawnNetworkObjectID = 0
}
Spawn()
AutoSpawnTimer = SpawnDelay  // only reset AFTER spawning
```
Key: timer resets only when actually spawning. When timer fires with a live object present,
the timer stays ≤ 0 so the spawner is "ready" — next tick after the object dies will spawn
immediately.

### Purchased character model (god.cpp)
`cGod::Create_Commando_With_Def()` resolves the model name from the def's phys def:
```cpp
PhysDefClass* physDef = DefinitionMgrClass::Find_Typed_Definition(soldierDef->PhysDefID, …)
modelName = physDef ? physDef->Get_Name() : ""
```
Existing vehicle analog in Kotlin: `God.resolveModelName(VehicleGameObjDefWrapper?)`.

## Key Files

| File | Change |
|------|--------|
| `server/src/main/kotlin/ccr/server/SpawnManager.kt` | Add `liveObject` field; fix `think()`; change callback return type |
| `server/src/main/kotlin/ccr/server/GameServer.kt` | Update `onCreatePowerUp` lambda to return `PowerUpGameObj?`; fix purchased soldier model name |
| `server/src/main/kotlin/ccr/server/God.kt` | Add `resolveSoldierModelName(defId)`, use it in `createCommando()` + `createCommandoWithDef()` |
| `server/src/test/kotlin/ccr/server/SpawnManagerTest.kt` | New file: unit tests for live-object tracking |

## Task 1 — SpawnManager live-object tracking

**File:** `server/src/main/kotlin/ccr/server/SpawnManager.kt`

### 1a. Change `onCreatePowerUp` callback return type

```kotlin
// Before:
var onCreatePowerUp: ((position: Vector3, def: PowerUpGameObjDef) -> Unit)? = null

// After:
var onCreatePowerUp: ((position: Vector3, def: PowerUpGameObjDef) -> PowerUpGameObj?)? = null
```

### 1b. Add `liveObject` to `PowerUpSpawnerState`

```kotlin
private data class PowerUpSpawnerState(
    val spawner: LoadedSpawner,
    val def: SpawnerDefClass,
    val powerUpDef: PowerUpGameObjDef,
    val spawnDelay: Float,
    var timer: Float,
    var liveObject: PowerUpGameObj? = null,   // ← add this
)
```

Note: `PowerUpGameObj` is in `ccr.server.net` — add the import.

### 1c. Fix `think()` to check live object before spawning

```kotlin
fun think(deltaSeconds: Float) {
    if (powerUpSpawners.isEmpty()) return

    for (state in powerUpSpawners) {
        state.timer -= deltaSeconds
        if (state.timer > 0f) continue

        // C++: Check_Auto_Spawn — only spawn when no live object exists
        val live = state.liveObject
        if (live != null && !live.isDeletePending) continue  // still alive → wait

        state.liveObject = null
        state.timer = state.spawnDelay  // reset only when actually spawning
        val pos = state.spawner.transform.position
        println("[SPAWN] powerup spawner fired: '${state.powerUpDef.name}' at (${pos.x}, ${pos.y}, ${pos.z})")
        state.liveObject = onCreatePowerUp?.invoke(Vector3(pos.x, pos.y, pos.z), state.powerUpDef)
    }
}
```

## Task 2 — Update GameServer.createPowerUp callback

**File:** `server/src/main/kotlin/ccr/server/GameServer.kt`

Search for the `onCreatePowerUp` lambda assignment (set during `initializeLevel()`).
It currently looks like:
```kotlin
spawnManager.onCreatePowerUp = { position, def ->
    createPowerUp(position, def)
}
```

It must now return the created `PowerUpGameObj?`:
```kotlin
spawnManager.onCreatePowerUp = { position, def ->
    createPowerUp(position, def)
}
```

Check if `createPowerUp` already returns `PowerUpGameObj?`. If not, update its return type
to do so and return the created object. The method creates a `PowerUpGameObj`, registers it,
and adds it to `gameObjManager` — just return it at the end.

## Task 3 — Resolve purchased soldier model name

**File:** `server/src/main/kotlin/ccr/server/God.kt`

### 3a. Add `resolveSoldierModelName` helper

Add a private helper after the existing `resolveModelName` for vehicles (around line 379):

```kotlin
/**
 * Resolves the W3D model name for a soldier definition.
 * Mirrors C++ cGod::Create_Commando: looks up SoldierGameObjDef.physDefId → PhysDefClass.modelName.
 * Returns empty string if the chain is broken.
 */
private fun resolveSoldierModelName(defId: Int): String {
    val wrapper = server.loadedLevel?.definitions?.findById(defId.toUInt())
        as? SoldierGameObjDefWrapper ?: return ""
    val physDefId = wrapper.soldierDef.physDefId
    if (physDefId == 0) return ""
    return (server.loadedLevel?.definitions?.findById(physDefId.toUInt())
        as? PhysDefClass)?.modelName ?: ""
}
```

Imports needed: `SoldierGameObjDefWrapper` is already imported; add `PhysDefClass` if not present.

### 3b. Use `resolveSoldierModelName` in `createCommando()`

Currently (around line 234):
```kotlin
val modelName = if (playerType == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
```

Replace with:
```kotlin
val fallbackModel = if (playerType == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
val modelName = resolveSoldierModelName(defId).ifEmpty { fallbackModel }
```

### 3c. Update `GameServer.kt` purchased character spawn

In `GameServer.kt` around line 840 (PURCHASEREQUESTEVENT handler, character purchase branch):
```kotlin
// Before:
val purchasedModelName = if (playerTeam == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
god.createCommandoWithDef(rhostId, playerTeam, result.purchasedDefId, purchasedModelName)

// After — God resolves the model from the def:
god.createCommandoWithDef(rhostId, playerTeam, result.purchasedDefId)
```

Then update `createCommandoWithDef` in `God.kt` to resolve its own model name:

```kotlin
fun createCommandoWithDef(rhostId: Int, playerType: Int, defId: Int): SoldierGameObj? {
    ...
    val fallbackModel = if (playerType == 0) "c_ag_nod_mg" else "c_ag_gdi_mg"
    val modelName = resolveSoldierModelName(defId).ifEmpty { fallbackModel }
    ...
}
```

Remove the `modelName` parameter entirely from `createCommandoWithDef` — the caller no longer
needs to supply it.

## Task 4 — Tests

**File:** `server/src/test/kotlin/ccr/server/SpawnManagerTest.kt` (new file)

Two tests:

### Test 1: `think does not spawn while live object is alive`

```kotlin
@Test
fun `think does not spawn while live object is alive`() {
    var spawnCount = 0
    val aliveObject = makePowerUp(deleted = false)

    val mgr = buildManager(spawnDelay = 5f)
    mgr.onCreatePowerUp = { _, _ ->
        spawnCount++
        aliveObject
    }

    // First tick at t=6s → fires, spawnCount=1, liveObject=aliveObject
    mgr.think(6f)
    assertEquals(1, spawnCount)

    // Second tick at t=6s → timer fires again (5s) but liveObject is still alive → no spawn
    mgr.think(6f)
    assertEquals(1, spawnCount)  // still 1
}
```

### Test 2: `think spawns again after live object is deleted`

```kotlin
@Test
fun `think spawns again after live object is deleted`() {
    var spawnCount = 0
    var liveObject = makePowerUp(deleted = false)

    val mgr = buildManager(spawnDelay = 5f)
    mgr.onCreatePowerUp = { _, _ ->
        spawnCount++
        liveObject
    }

    mgr.think(6f)
    assertEquals(1, spawnCount)

    // Object gets deleted
    liveObject = makePowerUp(deleted = true)
    // Since timer is already ≤ 0, next think spawns immediately
    mgr.think(0.1f)
    assertEquals(2, spawnCount)
}
```

Helper builders for the test:
- `makePowerUp(deleted: Boolean)` — creates a minimal `PowerUpGameObj` stub with `isDeletePending = deleted`
- `buildManager(spawnDelay: Float)` — builds a `SpawnManager` with a fake level containing
  one powerup spawner with the given delay; requires `SpawnManager` to be constructible with
  test data (or mock the level/registry)

**Note**: If `SpawnManager` is hard to construct in tests (due to `LoadedLevel` dependencies),
expose the live-object tracking logic through a package-internal method or test the behavior
indirectly via the `onCreatePowerUp` callback count (as shown above). Use whatever minimal
scaffolding works; don't create elaborate mocks.

## Verification

```
kotlin-server/gradlew -p kotlin-server test
```

All 679 existing tests pass + 2 new `SpawnManagerTest` tests = 681 total.
