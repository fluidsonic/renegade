# Full C++ parity for LDD game object loading

**Status:** Implemented on branch `feature/ldd-full-cpp-parity`

## Context

`GameObjectFactory` only extracted `definitionId` and `networkId` from `BaseGameObj::Save`. All other instance fields (position, health, team, turret angle, AI state, timers, etc.) were hardcoded defaults. This plan parses ALL fields from ALL game object types exactly as C++ does.

## Changes Made

### Step 1: Micro-chunk reading helpers
**File:** `server/src/main/kotlin/ccr/server/defs/MicroChunkReading.kt`

Added:
- `readMicroVector3(id)` — 12 bytes → 3x LE float → Vector3
- `readMicroMatrix3D(id)` — 48 bytes → 12x LE float → Matrix3D
- `readMicroOBBox(id)` — 60 bytes → 9+3+3 floats → OBBox
- `readMicroSphere(id)` — 16 bytes → 3+1 floats → Sphere
- `readMicroWideString(id)` — UTF-16LE bytes, trim null terminator

### Step 2: Geometry data classes
**File:** `server/src/main/kotlin/ccr/server/level/Geometry.kt` (new)

- `OBBox(basis: FloatArray, center: Vector3, extent: Vector3)` — oriented bounding box
- `Sphere(center: Vector3, radius: Float)` — sphere with center + radius

### Step 3: Missing chunk IDs
**File:** `server/src/main/kotlin/ccr/server/level/ChunkIds.kt`

Added:
- `GAMEOBJ_SAMSITE = 0x00040108u`
- `GAMEOBJ_SAKURA_BOSS = 0x00040131u`
- `GAMEOBJ_MENDOZA_BOSS = 0x00040149u`
- `GAMEOBJ_RAVESHAW_BOSS = 0x0004014Bu`
- `CHUNKID_GAMEOBJ_VARIABLES = 916991654u` (same value as CHUNKID_GAMEOBJMANAGER — C++ convention)

### Step 4: LoadedGameObject hierarchy restructure
**File:** `server/src/main/kotlin/ccr/server/level/ldd/LoadedGameObject.kt`

Added shared data classes:
- `LoadedDefenseObject` — health, healthMax, skin, shield fields from DefenseObjectClass
- `LoadedWeaponBagEntry` — definitionId, rounds, inventoryRounds

Updated/added per-type classes with full C++ field sets:
- `LoadedBuildingGameObj` — `playerType`, `defense`, `isPowerOn`, `collectionSphere`, `factoryChunkId` (removed old `teamId`, `sphereCenter`, `sphereRadius`)
- `LoadedSoldierGameObj` — all 24 soldier-specific micros + humanState + weaponBag + targetingPosition + controlEnabled
- `LoadedVehicleGameObj` — turretTurn, barrelTilt, transitionsEnabled, seats + weaponBag + targetingPosition
- `LoadedSimpleGameObj` — playerType, defense
- `LoadedScriptZoneGameObj` — boundingBox (OBBox), playerType with legacy remapping
- `LoadedC4GameObj` (new) — all 12 C4 micro-chunks
- `LoadedBeaconGameObj` (new) — 5 beacon micros
- `LoadedPowerUpGameObj` (new) — state, stateEndTimer
- `LoadedTransitionGameObj` (new) — transform (Matrix3D), ladderIndex
- `LoadedCinematicGameObj` (new) — DamageableGameObj fields
- `LoadedDamageZoneGameObj` (new) — boundingBox, damageTimer
- `LoadedSpecialEffectsGameObj` (new) — lifeRemaining, isInitialized
- `LoadedSAMSiteGameObj` (new) — state, timer
- `LoadedSakuraBossGameObj` (new) — DamageableGameObj + weaponBag + boss state micros
- `LoadedMendozaBossGameObj` (new) — DamageableGameObj + weaponBag
- `LoadedRaveshawBossGameObj` (new) — DamageableGameObj + weaponBag

### Step 5: GameObjectFactory per-type parsers
**File:** `server/src/main/kotlin/ccr/server/level/ldd/GameObjectFactory.kt`

Full rewrite with:
- Shared helper methods: `extractBaseFields`, `extractDamageableFields`, `extractDefenseObject`, `extractTargetingPosition`, `extractWeaponBag`
- Per-type extractors for all 16 game object types
- Dispatch expanded to cover all known factory chunk IDs
- C++ chunk ID constants documented with source file references
- Building post-load fixup: if collectionSphere.center is (0,0,0), copy position into it

### Step 6: Script parameters in LddParser
**File:** `server/src/main/kotlin/ccr/server/level/ldd/LddParser.kt`

- Changed output from `MutableList<String>` to `MutableList<ScriptAttachment>`
- Added parsing of micro 2 (MICROCHUNKID_PARAM) alongside micro 1 (name) in `parseScripts()`
- Also added `nextDynamicId` parsing from `CHUNKID_GAMEOBJ_VARIABLES` inside the GameObjManager

### Step 7: GameObjManager nextDynamicId
**File:** `server/src/main/kotlin/ccr/server/level/LevelDynamicData.kt`

Added `nextDynamicNetworkId: Int = 0` field.

**File:** `server/src/main/kotlin/ccr/server/GameServer.kt`

After loading level, calls `NetworkObjectManager.setNewDynamicId(nextDynId)` if non-zero.

### Step 8: LevelInfo description encoding fix
**File:** `server/src/main/kotlin/ccr/server/level/ldd/LevelInfoLoader.kt`

Changed micro 3 (description) from `readMicroString` to `readMicroWideString` — C++ writes it with `WRITE_MICRO_CHUNK_WIDESTRING` (UTF-16LE).

### Step 9: SpawnerClass SpawnDelayTimer
**File:** `server/src/main/kotlin/ccr/server/level/ldd/LoadedSpawner.kt`

Added `spawnDelayTimer: Float = 0f`.

**File:** `server/src/main/kotlin/ccr/server/level/ldd/SpawnerLoader.kt`

Added `5 -> spawnDelayTimer = data.toFloat32()` in micro-chunk handler.

### Step 10: BuildingManager wiring
**File:** `server/src/main/kotlin/ccr/server/BuildingManager.kt`

- Uses `lb.collectionSphere.center` and `lb.collectionSphere.radius` (was `lb.sphereCenter`/`lb.sphereRadius`)
- `getTeamForBuilding()` now uses a `buildingTeams` map populated from `lb.playerType` during construction
- Logs `playerType` per building

## Key C++ Chunk IDs

| Class | Chunk ID | Source file |
|-------|----------|-------------|
| BaseGameObj VARIABLES | 910991407 | basegameobj.cpp |
| DamageableGameObj VARIABLES | 207011214 | damageablegameobj.cpp |
| DefenseObjectClass VARIABLES | 914991020 | damage.cpp |
| ArmedGameObj VARIABLES | 418001842 | armedgameobj.cpp |
| ArmedGameObj WEAPONBAG | 418001843 | armedgameobj.cpp |
| SmartGameObj CONTROL | 910991115 | smartgameobj.cpp |
| BuildingGameObj VARIABLES | 207011121 | building.cpp |
| SoldierGameObj VARIABLES | 909991657 | soldier.cpp |
| SoldierGameObj HUMAN_STATE | 909991659 | soldier.cpp |
| VehicleGameObj VARIABLES | 923991633 | vehicle.cpp |
| ScriptZoneGameObj VARIABLES | 922991807 | scriptzone.cpp |
| C4GameObj VARIABLES | 922991751 | c4.cpp |
| BeaconGameObj VARIABLES | 0x00219044 | beacongameobj.cpp |
| PowerUpGameObj VARIABLES | 927991636 | powerup.cpp |
| TransitionGameObj VARIABLES | 1111991207 | transitiongameobj.cpp |
| SpecialEffectsGameObj VARIABLES | 0x09010237 | specialeffectsgameobj.cpp |
| SakuraBossGameObj VARIABLES | 0x0907045A | sakurabossgameobj.cpp |
| DamageZoneGameObj VARIABLES | 626000948 | damagezone.cpp |
| GameObjManager OBJECTS | 916991653 | gameobjmanager.cpp |
| GameObjManager VARIABLES | 916991654 | gameobjmanager.cpp |

## Verification

All 337 tests pass after implementation.
