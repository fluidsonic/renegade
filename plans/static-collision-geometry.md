# Load Static Collision Geometry into PhysicsScene

## Context

The Kotlin server receives client soldier positions and relays them to other clients, but has no world geometry loaded. The `PhysicsScene` class exists in the physics module with full collision query support (`castRay`, `castAABox`), and `StaticPhysClass` holds triangle soup for collision testing. However, nothing populates them during level load.

The LSD file's static objects subsystem contains serialized `StaticPhysClass` instances. Crucially, the LSD does **not** embed mesh vertex/face data -- it stores only the **model name** and **world-space transform**. At load time, the C++ code calls `WW3DAssetManager::Create_Render_Obj(name)` to load the actual mesh from `.w3d` files in the MIX archives.

**Goal**: During `loadLevel()`, parse static object metadata from the LSD, load the corresponding W3D meshes from MIX archives, transform their vertices to world space, and populate a `PhysicsScene` with the collision triangles.

## Implementation

### Step 1: Add `attributes` field to W3dMesh
- `server/src/main/kotlin/ccr/server/level/w3d/W3dMesh.kt` -- add `val attributes: UInt` field
- `server/src/main/kotlin/ccr/server/level/w3d/W3dMeshParser.kt` -- read `bb.getInt(4)` as attributes
- `server/src/main/kotlin/ccr/server/level/w3d/W3dChunkIds.kt` -- add collision flag constants

Collision flag constants:
- `W3D_MESH_FLAG_COLLISION_TYPE_PHYSICAL = 0x00000010u`
- `W3D_MESH_FLAG_COLLISION_TYPE_VEHICLE  = 0x00000100u`

### Step 2: Complete StaticObjectLoader

Parse nested chunk structure:
```
PSCENE_SO_CHUNK_STATIC_OBJECT (0x00770101)
  [factoryChunkId]
    SIMPLEFACTORY_CHUNKID_OBJPOINTER (0x00100100) -- skip
    SIMPLEFACTORY_CHUNKID_OBJDATA (0x00100101)
      STATICPHYS_CHUNK_PHYS (0x00DC2F94)
        PHYS_CHUNK_VARIABLES (0x00660055)
          micro 0x03 = flags, 0x04 = name, 0x06 = definitionId
        PHYS_CHUNK_MODEL (0x00660056)
          WW3D_PERSIST_CHUNKID_RENDEROBJ (0x00010000)
            RENDOBJFACTORY_CHUNKID_VARIABLES (0x00555040)
              micro 0x01 = modelName, 0x02 = transform (48 bytes)
      STATICPHYS_CHUNK_VARIABLES (0x00DC2F95)
        micro 0x00 = visObjectId, 0x01 = visSectorId
```

Add chunk ID constants to ChunkIds.kt:
- `SIMPLEFACTORY_CHUNKID_OBJPOINTER = 0x00100100u`
- `SIMPLEFACTORY_CHUNKID_OBJDATA = 0x00100101u`
- `STATICPHYS_CHUNK_PHYS = 0x00DC2F94u`
- `STATICPHYS_CHUNK_VARIABLES = 0x00DC2F95u`
- `PHYS_CHUNK_VARIABLES = 0x00660055u`
- `PHYS_CHUNK_MODEL = 0x00660056u`
- `WW3D_PERSIST_CHUNKID_RENDEROBJ = 0x00010000u`
- `RENDOBJFACTORY_CHUNKID_VARIABLES = 0x00555040u`

### Step 3: Make StaticPhysClass non-abstract
- `physics/src/main/kotlin/ccr/physics/static/StaticPhysClass.kt` -- `abstract class` → `open class`

### Step 4: Build W3D asset loading + PhysicsScene population
- New file: `server/src/main/kotlin/ccr/server/level/PhysicsSceneBuilder.kt`
- Collect unique model names from static objects
- Load/parse W3D files from MIX archives
- Filter to physical collision meshes (`attributes and 0x10u \!= 0u`)
- Transform vertices to world space, create StaticPhysClass instances
- Log: `[LEVEL] Loaded N static collision objects, M triangles`

### Step 5: Wire into GameServer
- `server/build.gradle.kts` -- add `implementation(project(":physics"))`
- `server/src/main/kotlin/ccr/server/GameServer.kt` -- store PhysicsScene, call builder after loadLevel()

## Verification
1. All Kotlin tests pass
2. Server logs show collision object/triangle counts
3. Client gameplay still works
