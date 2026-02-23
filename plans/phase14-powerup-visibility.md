# Phase 14: PowerUp Crate Visibility

## Goal

Make powerup crates (health, ammo, weapons) visible to clients. Currently `GameServer.createPowerUp()` passes `modelName = ""` because `PowerUpGameObjDef` doesn't carry the `physDefId` needed to look up the W3D model name.

## Context

Every `PhysicalGameObjDef`-derived def stores a `physDefId` (int) pointing to a `PhysDefClass` entry (in the same definition registry). `PhysDefClass.modelName` is the W3D model string the client uses to render the object. This field is saved by `PhysicalGameObjDef::Save()` as micro-chunk 18 inside its own `CHUNKID_DEF_VARIABLES` block.

`PowerUpGameObjDef` extends `SimpleGameObjDef` extends `PhysicalGameObjDef`. In the save file the nesting is:

```
objDataReader (top-level, inside OBJDATA = 0x00100101)
  ├─ CHUNK 909991656  PowerUpGameObjDef.CHUNKID_DEF_PARENT  ← opens first, writes parent class
  │    └─ CHUNK 930991656  SimpleGameObjDef.CHUNKID_DEF_PARENT  ← SimpleGameObjDef writes parent
  │         ├─ CHUNK 909991661  PhysicalGameObjDef.CHUNKID_DEF_PARENT  ← DamageableGameObjDef data
  │         └─ CHUNK 909991657  PhysicalGameObjDef.CHUNKID_DEF_VARIABLES
  │                └─ micro[18] = physDefId  ← what we need
  │         ... (SimpleGameObjDef.CHUNKID_DEF_VARIABLES = 930991657 also here)
  └─ CHUNK 909991657  PowerUpGameObjDef.CHUNKID_DEF_VARIABLES  ← grant fields (micro 2–21)
```

`parsePowerUpGameObjDef()` already reads `CHUNKID_DEF_VARIABLES (909991657u)` at the top level for the grant fields. The `physDefId` lives in a *different* 909991657 chunk three levels deep. Navigate there with three chained `findChunk()` calls.

Existing pattern from `God.kt` lines 381–384:
```kotlin
val physDefId = wrapper.vehicleDef.physical.physDefId
if (physDefId == 0) return ""
val physDef = server.loadedLevel?.definitions?.findById(physDefId.toUInt()) as? PhysDefClass
return physDef?.modelName ?: ""
```

We replicate this in `GameServer.createPowerUp()` using `def.physDefId`.

## Files to Modify

| File | Change |
|------|--------|
| `server/src/main/kotlin/ccr/server/defs/combat/PowerUpGameObjDef.kt` | Add `physDefId: Int = 0`, add chunk-ID constants, parse it |
| `server/src/main/kotlin/ccr/server/GameServer.kt` | Replace `modelName = ""` stub with phys-def lookup |
| `server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt` | Add test for physDefId parsing |

## Task 1 — Add `physDefId` to PowerUpGameObjDef

**File:** `server/src/main/kotlin/ccr/server/defs/combat/PowerUpGameObjDef.kt`

### Step 1 — Add field to data class

Add `val physDefId: Int = 0` to `PowerUpGameObjDef` constructor, after `alwaysAllowGrant`:

```kotlin
class PowerUpGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val grantShieldType: Int = 0,
    ...
    val alwaysAllowGrant: Boolean = false,
    val physDefId: Int = 0,           // ← add this
) : DefinitionClass(name, id, chunkId) {
```

### Step 2 — Add chunk-ID constants

After the existing `private const val CHUNKID_DEF_VARIABLES = 909991657u` line, add:

```kotlin
private const val CHUNKID_DEF_PARENT          = 909991656u  // PowerUpGameObjDef parent wrapper
private const val CHUNKID_SIMPLE_DEF_PARENT   = 930991656u  // SimpleGameObjDef parent wrapper
private const val CHUNKID_PHYSICAL_DEF_VARS   = 909991657u  // PhysicalGameObjDef CHUNKID_DEF_VARIABLES
private const val MICROCHUNKID_PHYS_ID        = 18          // same as PhysicalGameObjDefData.MICROCHUNKID_DEF_PHYS_ID
```

### Step 3 — Parse physDefId in the function

In `parsePowerUpGameObjDef()`, before the `findChunk(CHUNKID_DEF_VARIABLES)` call, add:

```kotlin
val physDefId = objDataReader
    .findChunk(CHUNKID_DEF_PARENT)
    ?.findChunk(CHUNKID_SIMPLE_DEF_PARENT)
    ?.findChunk(CHUNKID_PHYSICAL_DEF_VARS)
    ?.readMicroInt(MICROCHUNKID_PHYS_ID)
    ?: 0
```

Then pass `physDefId = physDefId` to both the early-return and the normal return:

```kotlin
fun parsePowerUpGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): PowerUpGameObjDef {
    val physDefId = objDataReader
        .findChunk(CHUNKID_DEF_PARENT)
        ?.findChunk(CHUNKID_SIMPLE_DEF_PARENT)
        ?.findChunk(CHUNKID_PHYSICAL_DEF_VARS)
        ?.readMicroInt(MICROCHUNKID_PHYS_ID)
        ?: 0

    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return PowerUpGameObjDef(name = name, id = id, chunkId = chunkId, physDefId = physDefId)

    return PowerUpGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        physDefId = physDefId,
        grantShieldType = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SHIELD_TYPE) ?: 0,
        // ... all other existing fields ...
    )
}
```

### Step 4 — Verify build

```
kotlin-server/gradlew -p kotlin-server :server:compileKotlin
```

Expected: BUILD SUCCESSFUL (no errors)

### Step 5 — Write test in FullDefinitionLoaderTest.kt

Add a new `@Test` at the end (before the closing `}`):

```kotlin
@Test
fun `PowerUpGameObjDef parses physDefId from nested parent chain`() {
    // Build: CHUNKID_DEF_PARENT(909991656) → SIMPLE_DEF_PARENT(930991656) → PHYSICAL_VARS(909991657) → micro[18]=12345
    val physIdMicro  = buildMicroChunk(18, intToLeBytes(12345))
    val physicalVars = buildChunk(909991657u, physIdMicro, isContainer = false)
    val simpleParent = buildChunk(930991656u, physicalVars, isContainer = true)
    val powerUpParent = buildChunk(909991656u, simpleParent, isContainer = true)

    val ddb = buildDdbWithExtra(Triple(0x00040107u, 888u, "Health_Crate") to powerUpParent)
    val registry = FullDefinitionLoader.load(ddb)

    val def = registry.findById(888u) as? ccr.server.defs.combat.PowerUpGameObjDef
    assertNotNull(def)
    assertEquals(12345, def.physDefId)
}
```

### Step 6 — Run tests

```
kotlin-server/gradlew -p kotlin-server :server:test
```

Expected: BUILD SUCCESSFUL, all tests pass (including the new one)

### Step 7 — Commit

```
git add kotlin-server/server/src/main/kotlin/ccr/server/defs/combat/PowerUpGameObjDef.kt \
        kotlin-server/server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt
git commit -m "feat: parse physDefId from PowerUpGameObjDef parent chain"
```

---

## Task 2 — Resolve model name in GameServer.createPowerUp()

**File:** `server/src/main/kotlin/ccr/server/GameServer.kt`

### Step 1 — Replace the stub

Find `createPowerUp()` (around line 1679). Replace:

```kotlin
val powerUp = PowerUpGameObj(
    definitionId  = def.id.toInt(),
    position      = position,
    modelName     = "",   // C++: model comes from physics def; stub for Phase 6
)
```

With:

```kotlin
val modelName = if (def.physDefId != 0)
    (loadedLevel?.definitions?.findById(def.physDefId.toUInt()) as? PhysDefClass)?.modelName ?: ""
else ""

val powerUp = PowerUpGameObj(
    definitionId  = def.id.toInt(),
    position      = position,
    modelName     = modelName,
)
```

### Step 2 — Ensure PhysDefClass is imported

Verify there is already an `import ccr.server.defs.PhysDefClass` at the top of `GameServer.kt` (it's used by vehicle model resolution via God.kt, but GameServer may not import it directly). If missing, add:

```kotlin
import ccr.server.defs.PhysDefClass
```

### Step 3 — Build and test

```
kotlin-server/gradlew -p kotlin-server test
```

Expected: BUILD SUCCESSFUL, all existing tests still pass.

### Step 4 — Commit

```
git add kotlin-server/server/src/main/kotlin/ccr/server/GameServer.kt
git commit -m "fix: resolve powerup model name from PhysDefClass"
```

---

## Verification

1. Build passes: `kotlin-server/gradlew -p kotlin-server test`
2. Manual test (if possible): spawn a map, observe powerup crates visible in client as W3D models (health/ammo/weapon boxes)
3. `println("[POWERUP] spawned ...")` log now shows non-empty model names

## Note

Save this plan to `plans/phase14-powerup-visibility.md` immediately once approved.
