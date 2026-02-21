# Physics Hierarchy Definition Review: Static / Decoration / Base

Reviewed Kotlin definition classes against their C++ originals in `original/Code/wwphys/`.

## C++ Inheritance Chain (Def Classes)

```
DefinitionClass
  PhysDefClass
    StaticPhysDefClass
      StaticAnimPhysDefClass
        ShakeableStaticPhysDefClass
    DynamicPhysDefClass
      MoveablePhysDefClass
        RigidBodyDefClass
      DecorationPhysDefClass
        DynamicAnimPhysDefClass
        TimedDecorationPhysDefClass
```

---

## Critical Bugs

### BUG-1: AnimCollisionManagerDef default `CollisionMode` is wrong (3 instead of 2)

**C++ source** (`animcollisionmanager.cpp:1414`):
```cpp
AnimCollisionManagerDefClass::AnimCollisionManagerDefClass(void) :
    CollisionMode(AnimCollisionManagerClass::COLLIDE_PUSH),  // COLLIDE_PUSH = 2
    AnimationMode(AnimCollisionManagerClass::ANIMATE_LOOP)
```

Enum values (`animcollisionmanager.h:76`):
```cpp
COLLIDE_NONE = 0, COLLIDE_STOP = 1, COLLIDE_PUSH = 2, COLLIDE_KILL = 3
```

**Affected Kotlin files (both use `3` instead of `2`):**
- `defs/phys/AnimCollisionManagerDefData.kt:13` &mdash; `val collisionMode: Int = 3` (comment says "COLLIDE_PUSH" but value is COLLIDE_KILL)
- `defs/DynamicAnimPhysDefClass.kt:22` &mdash; `val animCollisionMode: Int = 3` (same error)

**Correct version** exists in `defs/phys/StaticAnimPhysDefClassDef.kt:13`:
```kotlin
data class AnimCollisionManagerDef(val collisionMode: Int = 2, ...)  // correct
```

**Impact:** Any definition loaded that lacks an explicit CollisionMode micro-chunk will silently get COLLIDE_KILL (3) instead of COLLIDE_PUSH (2), causing different collision behavior at runtime.

### BUG-2: `DecorationPhysDefClassDef.kt` parses PhysDef at wrong chunk level

**File:** `defs/phys/DecorationPhysDefClassDef.kt:42`
```kotlin
val (modelName, isPreLit) = ccr.server.defs.PhysDefClass.parseFields(objDataReader)
```

This calls `parseFields` on the top-level `objDataReader`, but `PhysDefClass.parseFields` looks for `PHYSDEF_CHUNK_VARIABLES (0x055FFE08)` which is nested **two levels deep**:

```
objDataReader
  DECORATIONPHYSDEF_CHUNK_DYNAMICPHYSDEF (0x01070004)
    DYNAMICPHYSDEF_CHUNK_PHYSDEF (813001104)
      PHYSDEF_CHUNK_DEFINITION (0x055FFE07)   <-- DefinitionClass
      PHYSDEF_CHUNK_VARIABLES  (0x055FFE08)   <-- modelName, isPreLit
```

`parseFields` won't find the variables chunk at the top level. The `defs/DecorationPhysDefClass.kt` version handles this correctly by navigating the nesting first.

### BUG-3: `StaticAnimPhysDefClass.kt` (defs/) uses wrong defaults

**File:** `defs/StaticAnimPhysDefClass.kt`

| Field | C++ Default | Kotlin (defs/) | Kotlin (defs/phys/) |
|---|---|---|---|
| `modelName` | `"NULL"` | `""` (line 17) | `"NULL"` (correct) |
| `isNonOccluder` | `true` | `false` (line 19) | `true` (correct) |

The `defs/phys/StaticAnimPhysDefClassDef.kt` version has correct defaults.

---

## Structural Issues

### STRUCT-1: Duplicate Files (Two Implementations per Class)

The following C++ classes have **two separate Kotlin implementations** in different packages:

| C++ Class | File in `defs/` | File in `defs/phys/` |
|---|---|---|
| `PhysDefClass` | `PhysDefClass.kt` (67 lines, with parse helpers) | `PhysDefClass.kt` (33 lines, constants only) |
| `DecorationPhysDefClass` | `DecorationPhysDefClass.kt` (63 lines, correct parsing) | `DecorationPhysDefClassDef.kt` (51 lines, **buggy parsing**) |
| `StaticAnimPhysDefClass` | `StaticAnimPhysDefClass.kt` (data class `StaticAnimPhysDefClassDef`, **wrong defaults**) | `StaticAnimPhysDefClassDef.kt` (class `StaticAnimPhysDefClass`, correct) |
| `AnimCollisionManagerDef` | embedded in `StaticAnimPhysDefClassDef.kt` (correct default 2) | `AnimCollisionManagerDefData.kt` (**wrong default 3**) |

**Recommendation:** Consolidate into a single canonical location. The `defs/phys/` versions generally use better structure (proper class hierarchy), but the `defs/` versions sometimes have more correct parsing logic. Need to pick the best of each and remove duplicates.

### STRUCT-2: Incomplete Standalone Classes

Two Kotlin classes are standalone `data class`es that don't participate in the Kotlin inheritance hierarchy and **miss all parent fields**:

**`ShakeableStaticPhysDefClass`** (`defs/ShakeableStaticPhysDefClass.kt`):
- Only has: `id`, `name`
- Missing: `modelName`, `isPreLit`, `isNonOccluder`, all shadow fields, anim manager fields, projector manager fields
- C++ parent: `StaticAnimPhysDefClass` (which has ~20+ fields)

**`TimedDecorationPhysDefClass`** (`defs/TimedDecorationPhysDefClass.kt`):
- Only has: `id`, `name`, `lifetime`
- Missing: `modelName`, `isPreLit`
- C++ parent: `DecorationPhysDefClass` (which has modelName, isPreLit)

### STRUCT-3: Duplicated Micro-Chunk Helpers

`MicroChunkReading.kt` defines `readMicroInt`, `readMicroFloat`, `readMicroBool`, `readMicroString` as extension functions on `ChunkReader`.

The same functions also exist as:
- `PhysDefClass.microChunkInt/Float/Bool/String` (companion object static methods in `defs/PhysDefClass.kt`)
- Private extension functions in `defs/phys/StaticPhysDefClass.kt`
- Private extension functions in `defs/phys/StaticAnimPhysDefClassDef.kt`

**Recommendation:** Use one canonical set (either the extension functions in `MicroChunkReading.kt` or the companion object methods), and remove the rest.

---

## Per-Class Detailed Review

### DefinitionClass (`defs/DefinitionClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Base class (mirrors EditableClass in C++) |
| Fields | OK | `name`, `id`, `classId` &mdash; classId is metadata, not persisted by DefinitionClass itself |
| Chunk IDs | N/A | DefinitionClass uses `CHUNKID_VARIABLES=0x100`, `VARID_INSTANCEID=0x01`, `VARID_NAME=0x03` &mdash; parsed externally |
| Defaults | OK | m_ID defaults to 0 in C++ |

### PhysDefClass (`defs/PhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends DefinitionClass |
| Fields | OK | `modelName`, `isPreLit` |
| Chunk IDs | OK | `0x055FFE07`, `0x055FFE08` match C++ |
| Micro-chunk IDs | OK | FLAGS=0x00 (obsolete, correctly skipped), MODELNAME=0x01, ISPRELIT=0x02 |
| Defaults | OK | `modelName="NULL"`, `isPreLit=false` match C++ constructor |
| Parsing | OK | `parseFields` handles nested chunk navigation correctly |

### PhysDefClass (`defs/phys/PhysDefClass.kt`) &mdash; DUPLICATE

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends DefinitionClass |
| Fields | OK | Same as above |
| Chunk IDs | OK | Same values, different constant naming (CHUNK_DEFINITION, CHUNK_VARIABLES) |
| Micro-chunk IDs | OK | VARIABLE_MODELNAME=0x01, VARIABLE_ISPRELIT=0x02 |
| Parsing | NONE | No parsing logic, just constants used by other files |

### DynamicPhysDefClass (`defs/DynamicPhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends PhysDefClass |
| Fields | OK | No own fields (matches C++) |
| Chunk IDs | OK | `DYNAMICPHYSDEF_CHUNK_PHYSDEF = 813001104u` matches C++ |
| Defaults | OK | No own defaults needed |

### MoveablePhysDefClass (`defs/MoveablePhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends DynamicPhysDefClass |
| Fields | OK | `mass`, `gravScale`, `elasticity`, `cinematicCollisionMode` |
| Chunk IDs | OK | `0x04486000u` (obsolete), `0x04486001u`, `0x04486002u` |
| Micro-chunk IDs | OK | MASS=0x00, GRAVSCALE=0x01, ELASTICITY=0x02, CINEMATICCOLLISIONMODE=0x03 |
| Defaults | OK | 1f, 1f, 0.1f, CINEMATIC_COLLISION_PUSH=2 match C++ |
| Enum values | OK | NONE=0, STOP=1, PUSH=2, KILL=3 |
| Parsing | OK | Handles both obsolete (CHUNK_PHYSDEF) and current (CHUNK_DYNAMICPHYSDEF) parent formats |

### RigidBodyDefClass (`defs/RigidBodyDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends MoveablePhysDefClass |
| Fields | OK | `aerodynamicDragCoefficient`, `collisionDisabled` |
| Chunk IDs | OK | `0x01106650u`, `0x01106651u` |
| Micro-chunk IDs | OK | AERODYNAMICDRAGCOEFFICIENT=0x00, COLLISIONDISABLED=0x01 |
| Defaults | OK | `0f`, `false` match C++ |
| CLASS_ID | OK | `0x9005u` |
| Parsing | OK | Navigates parent chain correctly |

### StaticPhysDefClass (`defs/phys/StaticPhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends PhysDefClass |
| Fields | OK | `isNonOccluder` |
| Chunk IDs | OK | `0x01070002u`, `0x01070003u` |
| Micro-chunk IDs | OK | ISNONOCCLUDER=0x00 |
| Defaults | OK | `isNonOccluder = true` matches C++ |
| CLASS_ID | OK | `0x9007u` |
| Parsing | OK | `parseStaticPhysDefClass` navigates parent chain correctly |

### StaticAnimPhysDefClass (`defs/phys/StaticAnimPhysDefClassDef.kt`) &mdash; PREFERRED VERSION

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends StaticPhysDefClass |
| Fields | OK | All shadow fields, doesCollideInPathfind, isCosmetic, animManagerDef, projectorManagerDef |
| Chunk IDs | OK | `0x55110100u` through `0x55110103u` |
| Micro-chunk IDs | OK | 0x00..0x09 all match C++ enum |
| Defaults | OK | All match C++ constructor |
| Legacy handling | OK | CollisionMode (0x00) and AnimationName (0x07) correctly override animManagerDef |
| Sub-parsers | OK | `parseAnimCollisionManagerDef` and `parseProjectorManagerDef` are correct |

### StaticAnimPhysDefClass (`defs/StaticAnimPhysDefClass.kt`) &mdash; DUPLICATE, HAS BUGS

| Check | Status | Notes |
|---|---|---|
| Type | ISSUE | Named `StaticAnimPhysDefClassDef` (data class), not `StaticAnimPhysDefClass` |
| Defaults | **BUG** | `modelName=""` should be `"NULL"`, `isNonOccluder=false` should be `true` |
| Chunk IDs | OK | Same values as preferred version |
| Micro-chunk IDs | OK | Same values as preferred version |
| Legacy handling | OK | Handles CollisionMode/AnimationName overrides |

### ShakeableStaticPhysDefClass (`defs/ShakeableStaticPhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | **WRONG** | Standalone data class, should extend StaticAnimPhysDefClass |
| Fields | **INCOMPLETE** | Only `id`, `name` &mdash; missing ~20 fields from parent chain |
| Chunk IDs | OK | `CHUNKID_PARENT = 7311734u` matches C++ |
| CLASS_ID | OK | `0x900Fu` |

### DecorationPhysDefClass (`defs/DecorationPhysDefClass.kt`) &mdash; PREFERRED VERSION

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends DynamicPhysDefClass |
| Fields | OK | No own fields (matches C++) |
| Chunk IDs | OK | `0x01070003u` (old), `0x01070004u` (current) |
| CLASS_ID | OK | `0x9000u` |
| Parsing | OK | `parseParentFields` correctly navigates both old and new format |

### DecorationPhysDefClass (`defs/phys/DecorationPhysDefClassDef.kt`) &mdash; DUPLICATE, HAS BUG

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends DynamicPhysDefClass |
| Chunk IDs | OK | Same values |
| Parsing | **BUG** | Calls `PhysDefClass.parseFields(objDataReader)` at wrong nesting level (see BUG-2) |

### DynamicAnimPhysDefClass (`defs/DynamicAnimPhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends DecorationPhysDefClass (from `defs/phys/`) |
| Fields | OK | AnimCollisionManagerDef sub-fields + CastsShadows, ShadowNearZ, ShadowFarZ |
| Chunk IDs | OK | `0xAB00CE`, `0xAB00CF`, `0xAB00D0` (correctly decoded from C++ octal `052600316`) |
| Micro-chunk IDs | OK | CASTSSHADOWS=0x01, SHADOWNEARZ=0x02, SHADOWFARZ=0x03 |
| Defaults | **BUG** | `animCollisionMode = 3` should be `2` (see BUG-1) |
| Parsing | OK | Navigates DecoPhys -> DynamicPhys -> PhysDef chain correctly |

### TimedDecorationPhysDefClass (`defs/TimedDecorationPhysDefClass.kt`)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | **WRONG** | Standalone data class, should extend DecorationPhysDefClass |
| Fields | **INCOMPLETE** | Only `id`, `name`, `lifetime` &mdash; missing `modelName`, `isPreLit` |
| Chunk IDs | OK | `0x01170003u`, `0x01170004u` |
| Micro-chunk IDs | OK | LIFETIME=0x00 |
| Defaults | OK | `lifetime = 2.0f` matches C++ |
| CLASS_ID | OK | `0x900Au` |

### AnimCollisionManagerDefData (`defs/phys/AnimCollisionManagerDefData.kt`)

| Check | Status | Notes |
|---|---|---|
| Chunk IDs | OK | `0x1F4ADE72u` = 525000306 decimal |
| Micro-chunk IDs | OK | COLLISIONMODE=0x00, ANIMATIONMODE=0x01, ANIMATIONNAME=0x02 |
| Defaults | **BUG** | `collisionMode = 3` should be `2` (COLLIDE_PUSH) |

---

## Summary Table

| Class | Hierarchy | Fields | Chunk IDs | Micro-chunk IDs | Defaults | Parsing |
|---|---|---|---|---|---|---|
| DefinitionClass | OK | OK | N/A | N/A | OK | N/A |
| PhysDefClass (defs/) | OK | OK | OK | OK | OK | OK |
| PhysDefClass (defs/phys/) | OK | OK | OK | OK | OK | N/A |
| DynamicPhysDefClass | OK | OK | OK | N/A | OK | N/A |
| MoveablePhysDefClass | OK | OK | OK | OK | OK | OK |
| RigidBodyDefClass | OK | OK | OK | OK | OK | OK |
| StaticPhysDefClass | OK | OK | OK | OK | OK | OK |
| StaticAnimPhysDef (defs/phys/) | OK | OK | OK | OK | OK | OK |
| StaticAnimPhysDef (defs/) | ISSUE | OK | OK | OK | **BUG** | OK |
| ShakeableStaticPhysDef | **WRONG** | **INCOMPLETE** | OK | N/A | N/A | N/A |
| DecorationPhysDef (defs/) | OK | OK | OK | N/A | OK | OK |
| DecorationPhysDef (defs/phys/) | OK | OK | OK | N/A | OK | **BUG** |
| DynamicAnimPhysDef | OK | OK | OK | OK | **BUG** | OK |
| TimedDecorationPhysDef | **WRONG** | **INCOMPLETE** | OK | OK | OK | N/A |
| AnimCollisionManagerDefData | N/A | OK | OK | OK | **BUG** | OK |

---

## Priority Fix List

1. **Fix AnimCollisionManagerDef defaults** &mdash; Change `collisionMode` default from `3` to `2` in:
   - `defs/phys/AnimCollisionManagerDefData.kt`
   - `defs/DynamicAnimPhysDefClass.kt`

2. **Fix DecorationPhysDefClassDef.kt parsing** &mdash; Navigate through parent chunks before calling `parseFields`.

3. **Fix StaticAnimPhysDefClass.kt defaults** &mdash; `modelName` should be `"NULL"`, `isNonOccluder` should be `true`.

4. **Consolidate duplicates** &mdash; Pick one canonical location for each class and remove the other.

5. **Complete ShakeableStaticPhysDefClass** &mdash; Either extend StaticAnimPhysDefClass properly or flatten all parent fields into it.

6. **Complete TimedDecorationPhysDefClass** &mdash; Either extend DecorationPhysDefClass properly or add missing parent fields.

7. **Consolidate micro-chunk helpers** &mdash; Use one canonical set of `readMicroInt/Float/Bool/String` functions.
