# Phase 11 Implementation Plan: Physics Integration

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Wire up the two remaining physics-related stubs: (1) make `physicsTickLoop` actually tick the `PhysicsScene`, and (2) instantiate pre-placed vehicles from the LDD at level load so harvesters and other map vehicles are visible to clients from game start.

**Architecture:** Both tasks follow existing patterns. The physics loop is wired the same way as `networkTickLoop` (one new function call). Vehicle instantiation mirrors the building instantiation loop in `initializeLevel()`, reusing `God`'s existing `resolveModelName()` + `VehicleGameObjDefWrapper` lookup.

**Tech Stack:** Kotlin, `ccr.physics.scene.PhysicsScene`, `ccr.server.GameServer`, `ccr.server.God`, `ccr.server.level.ldd.LoadedGameObject`

---

## Context

### What's already done
- `PhysicsScene` is fully implemented with `update(dt)` that sub-steps at 1/15 s
- `PhysicsSceneBuilder.build()` populates the scene with static collision triangles during `loadLevel()`
- `GameServer.physicsScene` holds the scene
- `physicsTickLoop()` exists as a separate coroutine on `gameThread` but only calls `delay()` — never calls `physicsScene?.update()`
- `LoadedVehicleGameObj` is parsed from the LDD by `GameObjectFactory.extractVehicleGameObj()`
- `God.createVehicle()` and `God.createHarvester()` already create `VehicleGameObj` instances from a `defId + position`
- Building instantiation in `initializeLevel()` shows the exact pattern to follow

### What's missing
1. `physicsScene?.update(dt)` is never called anywhere — the static collision mesh is loaded but never ticked
2. `LoadedVehicleGameObj` entries in `level.dynamicData.gameObjects` are silently ignored — pre-placed vehicles (harvesters, etc.) never appear in game

## Key Files

| File | Purpose |
|------|---------|
| `server/.../GameServer.kt` | `physicsTickLoop()`, `initializeLevel()`, building instantiation pattern |
| `server/.../God.kt` | `createVehicle()`, `createHarvester()`, `vehiclesByNetId`, `resolveModelName()` |
| `server/.../level/ldd/LoadedGameObject.kt` | `LoadedVehicleGameObj` data class |
| `server/.../defs/VehicleGameObjDef.kt` | `VehicleGameObjDefWrapper` (has `vehicleDef.type.value`, `numSeats`) |
| `physics/.../scene/PhysicsScene.kt` | `fun update(dt: Float)` — sub-steps at MAX_TIMESTEP=1/15f |

---

## Task 1: Wire physicsTickLoop to call physicsScene.update()

**Files to modify:**
- `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

**No new test file needed** — `PhysicsScene.update()` is already covered by `PhysicsSceneTest`. The wiring is a one-line change.

### Step 1: Find physicsTickLoop

In `GameServer.kt`, find the current `physicsTickLoop()` body:

```kotlin
private suspend fun physicsTickLoop() {
    val intervalMs = 1000L / 120
    while (true) {
        delay(intervalMs)
    }
}
```

### Step 2: Replace the stub body

Change it to run at 30 Hz and call `physicsScene?.update()`:

```kotlin
// C++: wwphys PhysicsSceneClass runs at ~30 Hz
private suspend fun physicsTickLoop() {
    val intervalMs = 1000L / 30
    var lastTickMs = System.currentTimeMillis()
    while (true) {
        delay(intervalMs)
        val nowMs = System.currentTimeMillis()
        val dt = ((nowMs - lastTickMs) / 1000f).coerceAtMost(0.1f)
        lastTickMs = nowMs
        physicsScene?.update(dt)
    }
}
```

Key changes:
- Rate: 120 Hz → 30 Hz (matching C++ physics rate)
- Tracks `lastTickMs` so `dt` reflects real elapsed time, not assumed interval
- Clamps `dt` to 100 ms to prevent instability after pauses
- Calls `physicsScene?.update(dt)` — the null-safe call is a no-op until level is loaded

### Step 3: Build and run tests

```
kotlin-server/gradlew -p kotlin-server test
```

Expected: all existing tests pass (this is a one-line wiring change with no new logic).

### Step 4: Commit

```bash
cd /Users/marc/Documents/ccr/.worktrees/phase11-physics
git add kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git commit -m "feat: wire physicsTickLoop to call physicsScene.update() at 30 Hz"
```

---

## Task 2: Instantiate pre-placed vehicles from LDD at level load

**Files to modify:**
- `kotlin-server/server/src/main/kotlin/ccr/server/God.kt`
- `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

**No new test file** — the pattern exactly mirrors building instantiation which has no dedicated unit tests. Correctness verified by the build passing and the log output.

### Step 2.1: Add createLevelVehicle() to God

Open `God.kt`. After `createHarvester()` (around line 421), add:

```kotlin
/**
 * Instantiates a pre-placed vehicle from the level's LDD data.
 * Unlike [createVehicle] (purchased), uses the LDD-assigned network ID.
 * C++: cGod equivalent of loading SaveGame objects for vehicles.
 */
fun createLevelVehicle(lv: ccr.server.level.ldd.LoadedVehicleGameObj): VehicleGameObj? {
    if (lv.definitionId == 0) return null
    val wrapper = server.loadedLevel?.definitions?.findById(lv.definitionId.toUInt())
        as? VehicleGameObjDefWrapper
    val vehicleType = wrapper?.vehicleDef?.type?.value ?: VehicleGameObj.VEHICLE_TYPE_CAR
    val seatCount   = wrapper?.vehicleDef?.numSeats ?: 1
    val vehicle = VehicleGameObj(
        definitionId = lv.definitionId,
        modelName    = resolveModelName(wrapper),
        position     = lv.transform.position,
        team         = lv.playerType,
        vehicleType  = vehicleType,
        seatCount    = seatCount,
        vehicleDelivered = true,
        controlOwner = 0,
    )
    NetworkObjectManager.registerObject(vehicle, lv.networkId)
    vehiclesByNetId[lv.networkId] = vehicle
    server.gameObjManager.add(vehicle)
    println("[GOD] level vehicle: defId=${lv.definitionId} netId=${lv.networkId} " +
        "team=${if (lv.playerType == 0) "NOD" else "GDI"} " +
        "pos=(${lv.transform.position.x}, ${lv.transform.position.y}, ${lv.transform.position.z})")
    return vehicle
}
```

You will need to add this import at the top of God.kt if not already present:
```kotlin
import ccr.server.level.ldd.LoadedVehicleGameObj
```

### Step 2.2: Add vehicle instantiation loop in initializeLevel()

Open `GameServer.kt`. Find `initializeLevel()`. Locate the buildings loop — it ends with something like `println("[LEVEL] ... buildings registered")`. Immediately after that buildings block, add a vehicles block:

```kotlin
// Instantiate pre-placed vehicles from LDD (harvesters, decorative vehicles, etc.)
// C++: cGod loads all VehicleGameObj save-data entries during level init
val loadedVehicles = level.dynamicData.gameObjects.filterIsInstance<LoadedVehicleGameObj>()
for (lv in loadedVehicles) {
    god.createLevelVehicle(lv)
}
println("[LEVEL] ${loadedVehicles.size} level vehicles instantiated")
```

Add the import at the top of GameServer.kt if not already present:
```kotlin
import ccr.server.level.ldd.LoadedVehicleGameObj
```

### Step 2.3: Verify unloadLevel() handles level vehicles

Open `GameServer.kt`, find `unloadLevel()`. Confirm it already clears `god.vehiclesByNetId`:

```kotlin
for (vehicle in god.vehiclesByNetId.values.toList()) NetworkObjectManager.unregisterObject(vehicle)
god.vehiclesByNetId.clear()
```

If this loop is present, no change needed — level vehicles (stored in `vehiclesByNetId`) are already cleaned up on map rotation/restart. If missing, add it (but per the research it's already there).

### Step 2.4: Build and run tests

```
kotlin-server/gradlew -p kotlin-server test
```

Expected: all tests pass. If a compilation error appears because `LoadedVehicleGameObj` import is in a different package, adjust the import path by checking the actual package declaration at the top of `LoadedGameObject.kt`.

### Step 2.5: Commit

```bash
cd /Users/marc/Documents/ccr/.worktrees/phase11-physics
git add kotlin-server/server/src/main/kotlin/ccr/server/God.kt \
        kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git commit -m "feat: instantiate pre-placed LDD vehicles (harvesters etc.) at level load"
```

---

## Verification

After both tasks:

```
kotlin-server/gradlew -p kotlin-server test
```

All tests pass. Manual verification with a real client:
- Server logs show `[LEVEL] N level vehicles instantiated` at startup
- Connecting client sees pre-placed vehicles (harvesters) on the map from the start
- PhysicsScene ticked at 30 Hz (log shows no errors from physics update)
- Map rotation: level vehicles unloaded correctly, new map's vehicles appear

## Note

Save this plan to `/plans/phase11-physics-integration.md` immediately once approved.
