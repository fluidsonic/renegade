# Phase 18: Map Restart & Rotation Cleanup

## Goal

Fix three correctness gaps in the existing map restart / map rotation flow:

1. **Stale C4 / beacon tracking lists** — `God.c4Objects` and `God.beaconObjects` are never
   cleared on restart, leaving orphaned (pending-deletion) objects in the tracking lists.
2. **Respawn timer leak** — `God.respawnTimers` is never cleared on same-map restart;
   players locked in a respawn cooldown at game-end stay locked forever.
3. **Rate-limit map staleness** — `God.lastC4PlaceMs` / `God.lastBeaconPlaceMs` carry old
   timestamps into the new round, potentially blocking placements for up to 1 second.
4. **Map rotation client re-sync** — after `initializeLevel()` creates new buildings and base
   controllers, each already-connected in-game client must have `restoreDirtyBits()` called
   so the replication tick sends them the new level objects (creation packets).

## C++ Reference

### cGod teardown
`cGod::Destructor` / level unload tears down all objects via destructors — equivalent to
`gameObjManager.destroyAll()`. The tracking lists are per-session only and are rebuilt fresh
on the next level load.

### BioEvent reconnect logic (`bioevent.cpp:162-179`)
During intermission, when a client re-sends BIOEVENT with the new map name, C++ calls
`Tell_Client_About_Dynamic_Objects()` (= `restoreDirtyBits`) to send all new-level objects.
Our Kotlin server already does this for new-joining clients; we need to do it for
already-connected clients too after `initializeLevel()`.

## Key Files

| File | Change |
|------|--------|
| `server/src/main/kotlin/ccr/server/God.kt` | Add `clearRoundState()` |
| `server/src/main/kotlin/ccr/server/GameServer.kt` | Call `clearRoundState()` from `handleCoreRestart()` + `unloadLevel()`; call `restoreDirtyBits()` loop after `initializeLevel()` in `handleMapRotation()` |

## Task 1 — Add `God.clearRoundState()`

**File:** `server/src/main/kotlin/ccr/server/God.kt`

Add a new `internal` method after `deleteSoldier()`. It cleans up all round-specific
tracking that is NOT cleared by `deleteSoldier()`.

```kotlin
/**
 * Clears round-specific tracking state in God after all soldiers have been deleted.
 * Called by handleCoreRestart() and unloadLevel() to prevent stale references
 * from carrying over into the next round / next map.
 */
internal fun clearRoundState() {
    // Defuse/cancel any C4 and beacons not already cleaned up by deleteSoldier()
    // (e.g. timed C4 owned by no live player, or anything that slipped through)
    c4Objects.filter { !it.isDeletePending }.forEach { it.defuse() }
    beaconObjects.filter { !it.isDeletePending }.forEach { it.cancel() }
    c4Objects.clear()
    beaconObjects.clear()

    // Respawn timers — clear so no player is locked out at the start of a new round
    respawnTimers.clear()

    // Rate-limit maps — clear so old timestamps don't block first placements
    lastC4PlaceMs.clear()
    lastBeaconPlaceMs.clear()

    // Player vehicle references — clear stale associations
    // (vehicles themselves are cleaned up separately by unloadLevel/handleCoreRestart)
    playerVehicles.clear()
}
```

Note: `c4Objects`, `beaconObjects`, `respawnTimers`, `lastC4PlaceMs`, `lastBeaconPlaceMs`,
and `playerVehicles` are all `private` or `internal` fields in `God.kt`. Check exact visibility
and adjust if needed (make `private` ones `internal` if required — or add the method to the
`God` class directly where it has private access).

## Task 2 — Wire `clearRoundState()` into restart and unload

**File:** `server/src/main/kotlin/ccr/server/GameServer.kt`

### 2a. `handleCoreRestart()` — after soldiers are deleted

Current structure (around line 1135):
```kotlin
private fun handleCoreRestart() {
    hostedGameNumber++
    teamNod.reset()
    teamGdi.reset()
    gameState.reset()
    for (player in god.playersByHost.values) { player.resetStats() }
    for (rhostId in god.playerInGame.toList()) {
        god.deleteSoldier(rhostId)  // exits vehicles, defuses owned C4, cancels owned beacons
    }
    // ADD HERE:
    god.clearRoundState()           // clear anything deleteSoldier() left behind
    // ...rest continues...
    baseControllerNod?.reset()
    baseControllerGdi?.reset()
    for (building in gameObjManager.getBuildingList()) { building.resetToFull() }
}
```

Insert `god.clearRoundState()` after the `deleteSoldier()` loop and before the building reset block.

### 2b. `unloadLevel()` — before unregistering objects

Current structure (around line 1274):
```kotlin
private fun unloadLevel() {
    // ADD AT TOP, before any unregister/destroy calls:
    god.clearRoundState()   // clear tracking lists before objects are destroyed

    // ... existing: unregister doors, buildings, controllers, vehicles ...
    // ... existing: gameObjManager.destroyAll() ...
    // ... existing: clear level pointers ...
}
```

Insert `god.clearRoundState()` as the very first statement in `unloadLevel()`, before the
`doorObjects` loop. This ensures the lists are cleared before `gameObjManager.destroyAll()`
destroys the objects they reference.

## Task 3 — Map rotation client re-sync

**File:** `server/src/main/kotlin/ccr/server/GameServer.kt`

### In `handleMapRotation()`, after `initializeLevel()`

Current structure (around line 1320):
```kotlin
private suspend fun handleMapRotation(nextMapName: String) {
    ...
    unloadLevel()
    currentMapName = nextMapName
    gameData.mapNameCrc = ...
    loadLevel(nextMapName)
    initializeLevel()
    // ADD HERE:
    // Re-sync all still-connected in-game clients so they receive creation packets
    // for the new level's buildings, base controllers, doors, and vehicles.
    // C++: Tell_Client_About_Dynamic_Objects() called when client re-sends BIOEVENT
    // with the new map. We call it proactively for all in-game clients so they don't
    // need to reconnect.
    for (rhostId in god.playerInGame) {
        NetworkObjectManager.restoreDirtyBits(rhostId)
    }
    println("[GAME] map rotation complete — now on '$nextMapName', hostedGameNumber=$hostedGameNumber")
}
```

`NetworkObjectManager.restoreDirtyBits(rhostId)` (net module) sets each object's
`creationDirtyBit` for that client. The next `replicationTick()` call will send creation
(BIT_CREATION) or occasional (BIT_OCCASIONAL) packets to each in-game client for every
newly-registered object in the new level.

## Verification

```
kotlin-server/gradlew -p kotlin-server test
```

All 681 existing tests must continue to pass (no new tests required — changes are
small wiring calls with no branch logic worth unit-testing in isolation).

After compilation, trigger a same-map restart and a map rotation via the test server
to confirm no crash and that buildings/base controllers appear correctly on clients.
