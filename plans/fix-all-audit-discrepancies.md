# Plan: Fix All Audit Discrepancies

## Context

The comprehensive audit (`/docs/audit-kotlin-vs-cpp.md`) found discrepancies between the Kotlin server and C++ original. Fix all actionable issues. Note: BaseControllerClass::Think() was verified to be a no-op in C++ (`return;`), so MEDIUM-3 is dropped.

## Step 0: Save this plan
Write this plan to `/plans/fix-all-audit-discrepancies.md`.

## Step 1: BuildingGameObjDef — load scripts + encyclopedia fields (MEDIUM-1 + LOW-2)

**File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/BuildingGameObjDef.kt`

Add script and encyclopedia loading to `BuildingGameObjDef.load()`:
- Use `findChunkRecursive(627001057)` to find the ScriptableGameObjDef variables chunk
- Read micro-chunk 2 (script name, repeated) and 3 (script params, repeated) into lists
- Pass them through to the `ScriptableGameObjDef` parent fields
- Also add micro-chunk 3 (encyclopediaType) and 4 (encyclopediaId) when reading DamageableGameObjDef variables

**Reference**: `SoldierGameObjDef.kt` has `ScriptableGameObjDefData.load(chunk)` that reads chunk 627001057.

## Step 2: PowerUpGameObjDef — fix parent chain loading (LOW-1)

**File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/PowerUpGameObjDef.kt`

Replace the minimal parent traversal (lines 268-306) with delegation to `SimpleGameObjDef.load()`, matching how C4GameObjDef and BeaconGameObjDef do it:
- Open the PowerUp DEF_PARENT chunk (909991656)
- Call `SimpleGameObjDef.load(parentChunk, name, id, chunkId)` to get full parent chain
- Forward all inherited fields (defenseObjectDef, scripts, physDefId, etc.) to the PowerUpGameObjDef constructor

## Step 3: Game-over check ordering (LOW-3)

**File**: `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt`

Move the game-over detection block (current lines ~413-435) to AFTER `gameObjManager.postThink()` (after line ~448). This matches C++ where `End_Game_Test()` runs after all think loops. The core restart check should move with it.

**Before**: gameState.think → game-over → core restart → god.think → gameObjManager.think → postThink
**After**: gameState.think → god.think → gameObjManager.think → postThink → game-over → core restart

## Step 4: Remove respawn delay (LOW-4)

**File**: `kotlin-server/server/src/main/kotlin/ccr/server/God.kt`

Remove `RESPAWN_DELAY_SECONDS` (3.0f) and the `respawnTimers` map. C++ `cGod::Think()` spawns immediately — the respawn delay screen is handled client-side.

## Step 5: Player↔Soldier data link (LOW-5)

**Files**: `kotlin-server/server/src/main/kotlin/ccr/server/God.kt`, `kotlin-server/server/src/main/kotlin/ccr/server/net/Player.kt`

Make `Player` implement `PlayerDataClass` (or wrap it). Currently `SmartGameObj.setPlayerData()` takes `PlayerDataClass?`, but `Player` extends `NetworkObject` only.

Then uncomment `soldier.setPlayerData(playersByHost[rhostId])` in `createCommando()` (line 254) and `createCommandoWithDef()` (line 309).

## Step 6: Elevator/DSAPO StaticNetworkObject instantiation (MEDIUM-2)

**Files**:
- `kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt` (initializeLevel)
- `kotlin-server/server/src/main/kotlin/ccr/server/level/lsd/StaticObjectLoader.kt`
- `kotlin-server/server/src/main/kotlin/ccr/server/level/ChunkIds.kt`

Elevators are already in `KNOWN_FACTORY_IDS` in `StaticObjectLoader`, but no `ElevatorNetworkObject` instances are created.

Fix:
1. In `initializeLevel()`, after the door registration loop, add a loop for elevators: filter `staticObjects` by `factoryChunkId == PHYSICS_CHUNKID_ELEVATORPHYS`, create `ElevatorNetworkObject` per elevator, register with `NetworkObjectManager` using static network IDs
2. For DSAPO: add `PHYSICS_CHUNKID_DAMAGEABLESTATICPHYS` to `ChunkIds.kt` and to `KNOWN_FACTORY_IDS`, then create `DsapoNetworkObject` instances with health/shield from their definition's DefenseObjectDefClass
3. Network IDs for static objects follow a well-known pattern — check C++ `Generate_Static_Network_Objects()` for the ID assignment scheme

## Step 7: Update audit report

Update `/docs/audit-kotlin-vs-cpp.md` to mark all fixed issues.

## Step 8: Run tests

```
kotlin-server/gradlew -p kotlin-server test
```

## Verification

- All Kotlin tests pass
- Audit report updated with resolutions
