# Plan: Port cGod to God.kt

## Context

Players were spawning at 0,0 instead of proper map spawn points. The spawning lifecycle was scattered across `GameServer.kt` inline code rather than being in a dedicated class matching the C++ architecture. The C++ `cGod` class (`original/Code/Commando/god.cpp`) owns the full player/soldier lifecycle: state machine, player creation, team assignment, soldier spawning, death handling, and inventory.

Additionally, the Kotlin `SpawnManager.getMultiplayerSpawnLocation()` was using `spawnPoints` (SpawnPointList) when available, but C++ `Get_Multiplayer_Spawn_Location()` always uses the spawner's own `TM` transform. This behavioral mismatch contributed to the spawn position issue.

This plan ported `cGod` as `God.kt`, staying close to C++ structure, and fixed `SpawnManager` to match C++ behavior.

## Status: COMPLETED

All tasks implemented and build verified (`./gradlew :server:build` — BUILD SUCCESSFUL).

## Files Changed

### Created: `server/src/main/kotlin/ccr/server/God.kt`

Full 1:1 port of `cGod`. Class structure:
- `State` enum: `UNINITIALIZED, MULTIPLAYER, EXITING, SINGLE_INIT, SINGLE_RUNNING, SINGLE_DEAD`
- State maps moved from GameServer: `playersByHost`, `soldiersByHost`, `playerTeams`, `playerInGame`, `playerNetIds`
- `think()` — respawn loop, transitions state machine (always MP for server)
- `createPlayer(rhostId, name)` — creates/registers Player, uses pre-assigned team from `playerTeams`
- `createCommando(player)` — convenience overload extracting clientId/playerType
- `createCommando(rhostId, playerType)` — spawns soldier, sends to controlling host + broadcasts
- `deleteSoldier(rhostId)` — sends deletion to peers, unregisters
- `removePlayer(rhostId)` — full disconnect cleanup
- `choosePlayerType()` — auto-balance team assignment
- SP stubs (starKilled, respawn, restart, etc.) — no-op for MP server

### Modified: `server/src/main/kotlin/ccr/server/SpawnManager.kt`

**Fixed spawn position bug**: `getMultiplayerSpawnLocation()` now always uses `selected.spawner.transform.position` (the spawner's own TM), matching C++ `Get_Multiplayer_Spawn_Location()` which uses `SpawnerList[i]->Get_TM()`. Previously it was using `spawnPoints.random().position` when available, which was incorrect.

Added diagnostic logging: `[SPAWN] selected spawner defId=... pos=(x, y, z)`.

### Modified: `server/src/main/kotlin/ccr/server/GameServer.kt`

1. **Removed state** moved to God: `playerTeams`, `playerInGame`, `playersByHost`, `soldiersByHost`, `playerNetIds`
2. **Removed methods** moved to God: `spawnSoldier()`, `deleteSoldier()`
3. **Added `god: God`** — initialized after `loadLevel()` in `run()`
4. **Exposed as `internal`**: `connectionManager`, `spawnManager`, `loadedLevel`, `nodSoldierDefId`, `gdiSoldierDefId`, `pistolWeaponDefId`, `playerNicknames`, `sendGameNetObj()`, `sendUnreliable()`
5. **Delegated to God**: disconnect handler → `god.removePlayer()`, respawn loop → `god.think()`, BIOEVENT player creation → `god.createPlayer()`, SUICIDEEVENT → `god.deleteSoldier()`, CHANGETEAMEVENT → `god.playerTeams`/`god.deleteSoldier()`, team assignment → `god.choosePlayerType()`
6. **Updated all references**: `playerInGame` → `god.playerInGame`, `soldiersByHost` → `god.soldiersByHost`, etc.

## Key C++ References

- `original/Code/Commando/god.cpp` — source ported
- `original/Code/Commando/god.h` — class declaration
- `original/Code/Combat/spawn.cpp:796-880` — `Get_Multiplayer_Spawn_Location` (uses spawner TM, not SpawnPointList)

## Root Cause of Spawn Bug

C++ `Get_Multiplayer_Spawn_Location` (spawn.cpp:796-880) uses `SpawnerList[i]->Get_TM()` to get the spawner's world transform. The `SpawnPointList` on a SpawnerDef lists alternate spawn points relative to the spawner, and is typically empty in standard multiplayer maps. The Kotlin implementation was incorrectly preferring `spawnPoints` (which are relative positions from spawn.cpp's `SpawnPointClass`) over the spawner's own world TM.
