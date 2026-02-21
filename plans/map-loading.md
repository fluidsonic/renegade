# Plan: Renegade Map Loading in Kotlin Server

## Context

The Kotlin game server currently hardcodes spawn positions and loads only basic definition metadata (name+id+classId) from `always.dbs`. To progress the C++ server migration, the full map must be loaded — definitions, game objects, pathfinding, sounds, geometry, weather/sky — into structured Kotlin objects. The C++ loading pipeline is: **MIX → DDB (definitions) → LSD (static) → LDD (dynamic) → post-process**.

## What Already Exists

- **MixReader** (`ccr.server.mix`): MIX1 archive reader
- **ChunkReader** (`ccr.server.mix`): Chunk-based binary reader (8-byte headers, 2-byte micro-chunks)
- **MicroChunkReading.kt**: Extensions `readMicroInt`, `readMicroFloat`, `readMicroBool`, `readMicroString`
- **60+ definition parsers** (`ccr.server.defs/`): `SoldierGameObjDef`, `VehicleGameObjDef`, `BuildingGameObjDef`, etc. with full field parsing via chunk-walking
- **DefinitionDbReader**: Basic name+id+classId extraction from Objects.DDB
- **LevelExtents**: Extracts world bounding box from .lsd AABTree
- **GameServer.kt**: Loads defs from always.dbs + world extents; configures BitPack encoders

## Package Structure

```
ccr.server.level          -- LevelLoader orchestrator + LoadedLevel data model
ccr.server.level.lsd      -- .lsd static data parsers (physics, pathfinding, lights, background, weather, sound)
ccr.server.level.ldd      -- .ldd dynamic data parsers (game objects, spawners, scripts, objectives)
ccr.server.level.w3d      -- W3D file format parsers (mesh, hierarchy, animation)
ccr.server.level.pathfind -- Pathfinding data structures (sectors, portals, waypaths, height DB)
```

## Key Design Decisions

- **Definitions**: Use existing 60+ rich typed parsers. Dispatch by classId → produce `SoldierGameObjDef`, `VehicleGameObjDef`, etc. Unknown classIds get generic `DefinitionClass`.
- **Game objects**: Typed sealed hierarchy (`LoadedGameObject` with `LoadedSoldierGameObj`, `LoadedBuildingGameObj`, etc.) matching C++ PersistFactory pattern. Unknown factory chunk IDs logged and stored as raw data.
- **W3D geometry**: Full vertex/triangle data parsed (vertices, normals, triangles, textures, hierarchies, animations).
- **Sounds**: Full metadata (filenames, 3D position, attenuation, volume, loop settings).
- **Dispatcher**: `Dispatchers.IO` for file reads, `Dispatchers.Default` for CPU-bound chunk parsing. LSD and LDD parsed in parallel via `async`.
- **Thread safety**: Parallel parsing is fine (each parser produces its own immutable result). Registration into shared structures (DefinitionRegistry, object lists) must be sequential — collect parsed results first, then register on a single thread. No concurrent mutation of registries.

## Implementation Steps

### Step 1: Foundation — ChunkIds + Data Model + DefinitionRegistry

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/ChunkIds.kt` — All chunk ID constants
- `server/src/main/kotlin/ccr/server/level/DefinitionRegistry.kt` — Registry class with `byId`/`byName` maps
- `server/src/main/kotlin/ccr/server/level/LoadedLevel.kt` — Top-level result data class
- `server/src/main/kotlin/ccr/server/level/LevelStaticData.kt` — Static data container
- `server/src/main/kotlin/ccr/server/level/LevelDynamicData.kt` — Dynamic data container
- `server/src/main/kotlin/ccr/server/level/BinaryExtensions.kt` — ByteArray extensions for Matrix3D/Vector3
- `server/src/test/kotlin/ccr/server/level/DefinitionRegistryTest.kt`

### Step 2: Enhanced Definition Loading (FullDefinitionLoader)

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt` — Dispatches by classId to typed parsers
- `server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt`

### Step 3: LDD Level Info + Spawner Loading

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/ldd/LevelInfoLoader.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/SpawnerLoader.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedSpawner.kt`
- Tests

### Step 4: LDD Game Object Loading (PersistFactory)

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/ldd/GameObjectFactory.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedGameObject.kt` — Sealed hierarchy
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedBuildingGameObj.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedSoldierGameObj.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedVehicleGameObj.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedSimpleGameObj.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LoadedScriptZoneGameObj.kt`
- `server/src/main/kotlin/ccr/server/level/ldd/LddParser.kt`
- Tests

### Step 5: LSD Static Data Parser

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/lsd/LsdParser.kt`
- `server/src/main/kotlin/ccr/server/level/lsd/PhysicsSceneLoader.kt`
- `server/src/main/kotlin/ccr/server/level/lsd/StaticObjectLoader.kt`
- `server/src/main/kotlin/ccr/server/level/lsd/StaticPhysObject.kt`, `StaticLight.kt`
- Tests

### Step 6: Pathfinding

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/pathfind/PathfindData.kt`
- `server/src/main/kotlin/ccr/server/level/pathfind/PathfindLoader.kt`
- `server/src/main/kotlin/ccr/server/level/pathfind/PathSector.kt`, `PathPortal.kt`, `HeightDatabase.kt`, `Waypath.kt`, `Waypoint.kt`
- Tests

### Step 7: W3D Parsing

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/w3d/W3dChunkIds.kt`
- `server/src/main/kotlin/ccr/server/level/w3d/W3dMesh.kt` + `W3dMeshParser.kt`
- `server/src/main/kotlin/ccr/server/level/w3d/W3dHierarchy.kt` + `W3dHierarchyParser.kt`
- `server/src/main/kotlin/ccr/server/level/w3d/W3dAnimation.kt` + `W3dAnimationParser.kt`
- `server/src/main/kotlin/ccr/server/level/w3d/W3dFile.kt`
- Tests

### Step 8: Background + Weather

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/lsd/BackgroundData.kt` + `BackgroundLoader.kt`
- `server/src/main/kotlin/ccr/server/level/lsd/WeatherData.kt` + `WeatherLoader.kt`
- Tests

### Step 9: Sound Metadata

**Files to create:**
- `server/src/main/kotlin/ccr/server/level/lsd/SoundLoader.kt`
- `server/src/main/kotlin/ccr/server/level/lsd/StaticSoundEmitter.kt`
- Tests

### Step 10: LevelLoader Orchestrator + GameServer Integration

**Files to create/modify:**
- `server/src/main/kotlin/ccr/server/level/LevelLoader.kt`
- `server/src/main/kotlin/ccr/server/level/LoadingContext.kt`
- Modify `server/src/main/kotlin/ccr/server/GameServer.kt`

## Verification

1. **Unit tests**: Run `gradlew :server:test` — all new tests pass
2. **Server runtime**: Start server with map loaded, verify soldier spawns at real spawn point

## Worktree

Working in: `/Users/marc/Documents/ccr/.worktrees/map-loading/`
Branch: `feature/map-loading`
Build: `/Users/marc/Documents/ccr/kotlin-server/gradlew -p /Users/marc/Documents/ccr/.worktrees/map-loading/kotlin-server :server:test`
