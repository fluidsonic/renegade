# Definition Class Review: Physics / Vehicle / Human / Combat

Code review of Kotlin definition classes against C++ originals for the physics hierarchy.

**Scope**: VehiclePhysDefClass, MotorVehicleDefClass, WheeledVehicleDefClass, TrackedVehicleDefClass, VTOLVehicleDefClass, MotorcycleDefClass, HumanPhysDefClass, Phys3DefClass, ProjectileDefClass, AccessiblePhysDefClass, DamageableStaticPhysDefClass, DoorPhysDefClass, ElevatorPhysDefClass, ScriptZoneGameObjDef, TransitionGameObjDef

---

## 1. Duplicate Files

Three definition classes exist in two locations. The `defs/` versions and the `defs/phys/` or `defs/combat/` versions differ in design (flat data class vs proper inheritance). Both cannot co-exist at runtime.

| Class | Location 1 (older/flat) | Location 2 (newer/hierarchical) |
|---|---|---|
| VehiclePhysDefClass | `defs/VehiclePhysDefClass.kt` | `defs/phys/VehiclePhysDefClass.kt` |
| WheeledVehicleDefClass | `defs/WheeledVehicleDefClass.kt` | `defs/phys/WheeledVehicleDefClass.kt` |
| TransitionGameObjDef | `defs/TransitionGameObjDef.kt` | `defs/combat/TransitionGameObjDefDef.kt` |

**Details:**
- `defs/VehiclePhysDefClass.kt` has `parseFields()` and `load()` functions (referenced by other classes). `defs/phys/VehiclePhysDefClass.kt` is a thin class with companion constants only — no parsing logic.
- `defs/WheeledVehicleDefClass.kt` is a self-contained `data class` that re-declares every ancestor chunk ID and micro-chunk ID locally. `defs/phys/WheeledVehicleDefClass.kt` properly extends `MotorVehicleDefClass` and delegates parent parsing.
- `defs/TransitionGameObjDef.kt` extends `DefinitionClass`, uses proper `OBBoxClass`/`Matrix3D` types, but lacks legacy type remapping. `defs/combat/TransitionGameObjDefDef.kt` is a flat data class using raw `ByteArray` for zone/endingTM but includes the C++ `On_Post_Load` legacy type remapping (style types 4-7 → 8-9).

**Recommendation:** Consolidate each pair. The `defs/phys/` hierarchical versions are architecturally better, but the `defs/` versions currently have the parsing logic that other files depend on. For TransitionGameObjDef, merge the legacy remapping from the combat/ version into the defs/ version.

---

## 2. Per-Class Review

### 2.1 VehiclePhysDefClass

**C++ file:** `wwphys/vehiclephys.h/.cpp`
**Kotlin files:** `defs/VehiclePhysDefClass.kt`, `defs/phys/VehiclePhysDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `RigidBodyDefClass` |
| Chunk IDs | OK | `VEHICLEPHYSDEF_CHUNK_RIGIDBODYDEF=405001519`, `CHUNK_VARIABLES=405001520` |
| Micro-chunk IDs | OK | 0x00–0x07 match C++ `Save_Variables` order |
| Fields | OK | All 8 fields present (SpringConstant through IsFake) |
| Defaults | OK | 3.0f, 0.75f, 1.0f, 2.0f, 0.0f, 0.0f, 1.0f, false — all match C++ |
| Binary fidelity | OK | Standard micro-chunk float/bool reads |

**Issues:** None (other than the duplicate file mentioned above).

---

### 2.2 MotorVehicleDefClass

**C++ file:** `wwphys/motorvehicle.h/.cpp`
**Kotlin file:** `defs/phys/MotorVehicleDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `VehiclePhysDefClass` |
| Chunk IDs | OK | Old parent 0x00516000, Variables 0x00516001, Current parent 0x00516002 |
| Micro-chunk IDs | OK | 0x00–0x09, 0x0D–0x0F (correctly skips obsolete 0x0A–0x0C) |
| Fields | OK | All 12 non-obsolete fields present |
| Defaults | OK | MaxEngineTorque=5.0f, GearCount=4, GearRatios={12.01,7.82,5.16,3.81,2.79,1.0}, etc. |
| Old parent handling | OK | Load tries `CHUNK_VEHICLEPHYSDEF` then falls back to `CHUNK_RIGIDBODYDEF` |
| Binary fidelity | WARN | EngineTorqueCurveFilename uses `WRITE_MICRO_CHUNK_WWSTRING` in C++. Kotlin reads with `microChunkString` (null-terminated). See note below. |

**WWSTRING Note:** C++ uses `WRITE_MICRO_CHUNK_WWSTRING` / `READ_MICRO_CHUNK_WWSTRING` for `EngineTorqueCurveFilename`. If the WWSTRING format differs from a plain null-terminated byte string (e.g., length-prefixed or wide chars), the Kotlin reader would fail. In practice, Renegade-era `WideStringClass` likely stores null-terminated ASCII, so this probably works. Verify with actual binary data.

---

### 2.3 WheeledVehicleDefClass

**C++ file:** `wwphys/wheelvehicle.h/.cpp`
**Kotlin files:** `defs/WheeledVehicleDefClass.kt` (flat), `defs/phys/WheeledVehicleDefClass.kt` (hierarchical)

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK (phys/) | `defs/phys/` version extends `MotorVehicleDefClass`. `defs/` version is flat `data class`. |
| Chunk IDs | OK | `WHEELEDVEHICLEDEF_CHUNK_MOTORVEHICLEDEF=0x00990066`, `CHUNK_VARIABLES=0x00990067` |
| Micro-chunk IDs | OK | `VARIABLE_MAXSTEERINGANGLE=0x00` |
| Fields | OK | `maxSteeringAngle` present |
| Defaults | OK | `maxSteeringAngle=0.7853982f` (PI/4) matches C++ |
| Binary fidelity | WARN | See below |

**Issues:**

1. **BUG — Missing obsolete variable handling.** C++ `WheeledVehicleDefClass::Load` reads obsolete micro-chunks from its own VARIABLES chunk and stores them into parent `VehiclePhysDefClass` fields:
   - `WHEELEDVEHICLEDEF_VARIABLE_SPRINGCONSTANT` → `SpringConstant`
   - `WHEELEDVEHICLEDEF_VARIABLE_DAMPINGCONSTANT` → `DampingConstant`
   - `WHEELEDVEHICLEDEF_VARIABLE_SPRINGLENGTH` → `SpringLength`
   - `WHEELEDVEHICLEDEF_VARIABLE_TRACTIONMULTIPLIER` → `TractionMultiplier`
   - `WHEELEDVEHICLEDEF_VARIABLE_LATERALMOMENTARM` → `LateralMomentArm`
   - `WHEELEDVEHICLEDEF_VARIABLE_TRACTIVEMOMENTARM` → `TractiveMomentArm`

   Neither Kotlin version reads these. Old-format data files where these values were stored in the WheeledVehicle variables chunk (instead of VehiclePhysDef) will use defaults for those fields.

2. **Flat version re-declares all ancestor IDs.** `defs/WheeledVehicleDefClass.kt` duplicates every chunk/micro-chunk ID from PhysDefClass through WheeledVehicleDefClass. This is fragile and error-prone if any ancestor constant changes.

---

### 2.4 TrackedVehicleDefClass

**C++ file:** `wwphys/trackedvehicle.h/.cpp`
**Kotlin file:** `defs/TrackedVehicleDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `VehiclePhysDefClass` directly (not MotorVehicleDefClass) — matches C++ |
| Chunk IDs | OK | C++ uses octal literal `0406001454` = `0x0418032C`. Kotlin correctly converts to `0x0418032Cu`. |
| Micro-chunk IDs | OK | 0x00–0x03 |
| Fields | OK | MaxEngineTorque, TrackUScaleFactor, TrackVScaleFactor, TurnTorqueScaleFactor |
| Defaults | OK | 0.0f, 25.0f, 0.0f, 1.0f |
| Binary fidelity | OK | Standard float reads |

**Issues:** None.

---

### 2.5 VTOLVehicleDefClass

**C++ file:** `wwphys/vtolvehicle.h/.cpp`
**Kotlin file:** `defs/VTOLVehicleDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `VehiclePhysDefClass` |
| Chunk IDs | OK | `VTOLVEHICLEDEF_CHUNK_VEHICLEPHYSDEF=408000936`, `CHUNK_VARIABLES=408000937` |
| Micro-chunk IDs | OK | 0x00–0x0D (14 IDs, sequential) |
| Fields | OK | All 14 fields present |
| Defaults | OK | Verified against C++ constructor. All DEG_TO_RADF conversions match. |
| Binary fidelity | OK | Uses `forEachMicroChunk` with `ByteBuffer.wrap` — correct approach |

**Issues:** None. Well-implemented class.

---

### 2.6 MotorcycleDefClass

**C++ file:** `wwphys/motorcycle.h/.cpp`
**Kotlin file:** `defs/phys/MotorcycleDefClassDef.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `WheeledVehicleDefClass` |
| Chunk IDs | OK | `CHUNK_WHEELEDVEHICLEDEF=0x00516000`, `CHUNK_VARIABLES=0x00516001` |
| Micro-chunk IDs | OK | `VARIABLE_LEANK0=0x00`, `VARIABLE_LEANK1=0x01` |
| Fields | OK | LeanK0, LeanK1 |
| Defaults | OK | 18.0f, 5.0f |
| Binary fidelity | FAIL | Parent fields not parsed. See below. |

**Issues:**

1. **CRITICAL — `findChunkRecursive` may match wrong chunk.** The `parseMotorcycleDefClass` function uses `findChunkRecursive(CHUNK_VARIABLES)` where `CHUNK_VARIABLES=0x00516001`. However, `MotorVehicleDefClass` also uses `CHUNK_VARIABLES=0x00516001`. Depending on `findChunkRecursive`'s traversal order, this could find the MotorVehicle variables chunk (nested inside the parent chain) instead of the Motorcycle's own variables chunk.

   Binary layout:
   ```
   [0x00516000 = MOTORCYCLEDEF_CHUNK_WHEELEDVEHICLEDEF]
     [0x00990066 = WHEELEDVEHICLEDEF_CHUNK_MOTORVEHICLEDEF]
       [0x00516001 = MOTORVEHICLEDEF_CHUNK_VARIABLES]  ← WRONG match if depth-first
       ...
     [0x00990067 = WHEELEDVEHICLEDEF_CHUNK_VARIABLES]
   [0x00516001 = MOTORCYCLEDEF_CHUNK_VARIABLES]  ← CORRECT match
   ```

   **Fix:** Use `objDataReader.findChunk(CHUNK_VARIABLES)` (non-recursive, top-level only) instead of `findChunkRecursive`.

2. **CRITICAL — Parent fields use defaults.** The `parseMotorcycleDefClass` function only extracts `leanK0` and `leanK1`. All parent fields (vehicle physics, motor vehicle, wheeled vehicle) use constructor defaults. This means loading a Motorcycle definition discards all parent-chain data (mass, gear ratios, steering angle, spring constants, etc.).

3. **Naming:** File is `MotorcycleDefClassDef.kt` — the `Def` suffix on the filename is redundant.

---

### 2.7 HumanPhysDefClass

**C++ file:** `wwphys/humanphys.h/.cpp`
**Kotlin file:** `defs/HumanPhysDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `Phys3DefClass` |
| Chunk IDs | OK | `HUMANPHYSDEF_CHUNK_PHYS3DEF=0x00516000` |
| Micro-chunk IDs | N/A | No own variables |
| Fields | N/A | No own fields — all from parent |
| Defaults | OK | Delegates to `Phys3DefClass.parseFields` |
| Binary fidelity | OK | Correct parent delegation |

**Issues:** None.

---

### 2.8 Phys3DefClass

**C++ file:** `wwphys/phys3.h/.cpp`
**Kotlin file:** `defs/Phys3DefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `MoveablePhysDefClass` |
| Chunk IDs | OK | `PHYS3DEF_CHUNK_MOVEABLEPHYSDEF=0x04486000`, `CHUNK_VARIABLES=0x04486001` |
| Micro-chunk IDs | OK | 0x00, 0x01, 0x02 |
| Fields | OK | NormSpeed, SlideAngle, StepHeight |
| Defaults | OK | 10.0f, DEG_TO_RADF(45)=0.7853982f, 0.25f — verified against C++ `DEFAULT_*` constants |
| Binary fidelity | OK | Standard float reads |

**Issues:** None.

---

### 2.9 ProjectileDefClass

**C++ file:** `wwphys/projectile.h/.cpp`
**Kotlin file:** `defs/ProjectileDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK | Extends `MoveablePhysDefClass` |
| Chunk IDs | OK | `PROJECTILEDEF_CHUNK_MOVEABLEPHYSDEF=0x01210011`, `CHUNK_VARIABLES=0x01210012` |
| Micro-chunk IDs | OK | 0x00–0x05 |
| Fields | OK | CollidesOnMove, OrientationMode, TumbleAxis (Vector3), TumbleRate, Lifetime, BounceCount |
| Defaults | OK | true, 0, (1,2,1), DEG_TO_RADF(10)=0.17453292f, 2.0f, 0 |
| Binary fidelity | OK | TumbleAxis correctly reads 12 bytes (3 floats) |

**Issues:** None. Well-implemented class with proper Vector3 handling for TumbleAxis.

---

### 2.10 AccessiblePhysDefClass

**C++ file:** `wwphys/accessiblephys.h/.cpp`
**Kotlin file:** `defs/AccessiblePhysDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | WARN | C++ extends `StaticAnimPhysDefClass`. Kotlin is a flat `data class`. |
| Chunk IDs | OK | `CHUNKID_DEF_VARIABLES=0x1031124A` matches C++ |
| Micro-chunk IDs | OK | `VARID_DEF_LOCKCODE=1` (correctly starts at 1, not 0) |
| Fields | WARN | Only `lockCode` extracted. Missing `modelName`, `isPreLit`, and all static phys fields. |
| Defaults | OK | lockCode=0 |
| Binary fidelity | WARN | Parent chain fields not parsed |

**Issues:**

1. **Missing parent chunk ID.** C++ defines `CHUNKID_DEF_PARENT=0x10311249` for the parent chunk. Kotlin doesn't define or navigate it — uses `findChunkRecursive` for `CHUNKID_BASE_VARIABLES` to get name/id.

2. **Missing static phys fields.** AccessiblePhysDefClass inherits from StaticAnimPhysDefClass which has animation-related fields. These are not extracted.

---

### 2.11 DamageableStaticPhysDefClass

**C++ file:** `Combat/damageablestaticphys.h/.cpp`
**Kotlin file:** `defs/DamageableStaticPhysDefClass.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | WARN | C++ extends `StaticAnimPhysDefClass`. Kotlin is flat `data class`. |
| Chunk IDs | OK | Parent=7311734, Variables=7311735, DefenseObjectDef=7311736 |
| Micro-chunk IDs | OK | 0x00–0x0C. Correctly notes 0x01 (ResetAfterAnim) as unused. |
| Fields | OK | All 12 non-obsolete fields present + DefenseObjectDef |
| Defaults | OK | All ints=0, playTwitchesToCompletion=false |
| Binary fidelity | OK | Uses `forEachMicroChunk` — handles all IDs correctly |

**Issues:**

1. **Missing parent fields.** Only extracts name/id via `findChunkRecursive`. Static phys fields (modelName, isPreLit, etc.) not parsed.

2. **DefenseObjectDef dependency.** References `DefenseObjectDef.load()` — not reviewed here but should be verified separately.

---

### 2.12 DoorPhysDefClass

**C++ file:** `Combat/doors.h/.cpp`
**Kotlin file:** `defs/combat/DoorPhysDefClassDef.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | WARN | C++ extends `AccessiblePhysDefClass`. Kotlin is flat `data class`. |
| Chunk IDs | OK | `CHUNKID_DEF_VARIABLES=320001903`, `CHUNKID_DEF_PARENT=320001904` |
| Micro-chunk IDs | OK | 2–10, correctly skips 1 (obsolete TRIGGER_RADIUS) |
| Fields | OK | All 9 fields (CloseDelay through DoorOpensForVehicles) |
| Defaults | OK | CloseDelay=2f, sounds=0, DoorOpensForVehicles=false |
| TriggerZone | OK | Reads as 15-float array (OBBoxClass = center+extent+basis) |
| LockCode | OK | Reads from parent AccessiblePhysDef, falls back to old micro-chunk 4 |
| Binary fidelity | WARN | See below |

**Issues:**

1. **Naming:** Class is `DoorPhysDefClassDef` — should be `DoorPhysDefClass` to match C++.

2. **Missing old parent handling.** C++ Load handles both:
   - `CHUNKID_DEF_OLD_PARENT=320001902` → `StaticAnimPhysDefClass::Load`
   - `CHUNKID_DEF_PARENT=320001904` → `AccessiblePhysDefClass::Load`

   Kotlin only looks at `CHUNKID_DEF_PARENT`. Very old data files with `CHUNKID_DEF_OLD_PARENT` would fail to parse the parent chain.

3. **CloseDelay type:** C++ declares `float CloseDelay` — Kotlin uses `Float`. Correct.

---

### 2.13 ElevatorPhysDefClass

**C++ file:** `Combat/elevator.h/.cpp`
**Kotlin file:** `defs/combat/ElevatorPhysDefClassDef.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | WARN | C++ extends `AccessiblePhysDefClass`. Kotlin is flat `data class`. |
| Chunk IDs | OK | `CHUNKID_DEF_VARIABLES=714001419` (second enum value) |
| Micro-chunk IDs | WARN | See below |
| Fields | WARN | See below |
| Defaults | OK | CloseDelay=2, frames=-1, sounds=0 |
| Binary fidelity | FAIL | OBBox basis matrix dropped |

**Issues:**

1. **Naming:** Class is `ElevatorPhysDefClassDef` — should be `ElevatorPhysDefClass`.

2. **BUG — Missing micro-chunk ID 2 (LOCK_CODE).** C++ enum defines `MICROCHUNKID_DEF_LOCK_CODE=2`. While C++ Save comments it out (`//WRITE_MICRO_CHUNK`), the C++ Load DOES read it for backward compatibility:
   ```cpp
   READ_MICRO_CHUNK( cload, MICROCHUNKID_DEF_LOCK_CODE, LockCode );
   ```
   Kotlin jumps from ID 1 (CLOSE_DELAY) to ID 3 (UPPER_CALL_ZONE), skipping ID 2. Old data with LOCK_CODE at this level will silently lose the value.

3. **BUG — OBBox basis matrix dropped.** C++ `OBBoxClass` stores 15 floats: center(3) + extent(3) + basis(9). The Kotlin `parseOBBox` reads only the first 24 bytes (center + extent) and ignores the remaining 36 bytes (basis matrix). This means:
   - Rotated trigger zones lose their rotation data
   - If data is re-serialized, the basis will be identity instead of the original value
   - This is a **binary fidelity failure**

4. **Missing old parent handling.** Like DoorPhysDef, C++ handles `CHUNKID_DEF_OLD_PARENT=714001418` for backward compat. Kotlin doesn't.

5. **`findChunkRecursive` for VARIABLES.** The `parseElevatorPhysDefClassDef` function uses `findChunkRecursive(CHUNKID_DEF_VARIABLES)`. Since the Elevator VARIABLES chunk ID (714001419) is unique, this works, but direct navigation via the parent chunk would be more robust.

---

### 2.14 ScriptZoneGameObjDef

**C++ file:** `Combat/scriptzone.h/.cpp`
**Kotlin file:** `defs/combat/ScriptZoneGameObjDefDef.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | WARN | C++ extends `ScriptableGameObjDef`. Kotlin is flat `data class`. |
| Chunk IDs | OK | `CHUNKID_DEF_VARIABLES=1111991133` matches C++ |
| Micro-chunk IDs | OK | 2–5. See note about ID 1 below. |
| Fields | OK | Color (Vector3), CheckStarsOnly, ZoneType, IsEnvironmentZone |
| Defaults | OK | Color=(0,0.7,0), CheckStarsOnly=true, ZoneType=0, IsEnvironmentZone=false |
| Binary fidelity | OK | Color correctly parsed as 3 floats |

**Notes:**

1. **IsCTFZone (micro-chunk ID 1) intentionally omitted — correct.** The C++ enum defines `MICROCHUNKID_DEF_IS_CTF_ZONE=1` but the C++ Save function never writes it, and the C++ Load never reads it. The Kotlin is correct to skip it.

2. **Naming:** Class is `ScriptZoneGameObjDefDef` — should be `ScriptZoneGameObjDef`.

---

### 2.15 TransitionGameObjDef

**C++ file:** `Combat/transitiongameobj.h/.cpp`, `Combat/transition.h/.cpp`
**Kotlin files:** `defs/TransitionGameObjDef.kt`, `defs/combat/TransitionGameObjDefDef.kt`

| Check | Status | Notes |
|---|---|---|
| Hierarchy | OK (defs/) | `defs/` version extends `DefinitionClass`. C++ extends `BaseGameObjDef`. |
| Chunk IDs | OK | `CHUNKID_DEF_PARENT=1111991201`, `CHUNKID_DEF_TRANSITION=1111991202` |
| TransitionData IDs | OK | `CHUNKID_VARIABLES=0x11051106`, micro-chunks 1–4 |
| Fields | OK | type, zone (OBBoxClass), animationName, endingTM (Matrix3D) |
| Defaults | OK | type=0 in defs/ version, type=-1 in combat/ version |

**Issues:**

1. **Neither version is complete:**
   - `defs/TransitionGameObjDef.kt`: Proper `OBBoxClass` and `Matrix3D` types, correct parent navigation, but **missing legacy type remapping** (C++ `On_Post_Load` converts style types 4→8, 5→8, 6→9, 7→9).
   - `defs/combat/TransitionGameObjDefDef.kt`: Has legacy type remapping, but stores zone as raw `ByteArray` instead of structured `OBBoxClass`, and endingTM as raw `ByteArray` instead of `Matrix3D`.

2. **Default type mismatch:** `defs/` version defaults type=0 (`STYLE_LADDER_EXIT_TOP`). `combat/` version defaults type=-1 (`STYLE_DISABLED`). C++ `TransitionDataClass` constructor initializes `Type` to... likely 0 or whatever the first enum value is. The defs/ version with type=0 is more likely correct.

3. **Naming:** `defs/combat/TransitionGameObjDefDef.kt` — should be `TransitionGameObjDef`.

---

## 3. Summary of All Issues

### Critical (data loss / incorrect loading)

| # | Class | Issue |
|---|---|---|
| C1 | MotorcycleDefClass | `findChunkRecursive(0x00516001)` may find MotorVehicle variables instead of Motorcycle variables |
| C2 | MotorcycleDefClass | Parent chain not parsed — all vehicle/motor/wheeled fields use defaults |
| C3 | ElevatorPhysDefClass | OBBox basis matrix (9 floats) dropped — rotated trigger zones lose rotation |
| C4 | TransitionGameObjDef (defs/) | Missing legacy type remapping from `On_Post_Load` |

### High (backward compatibility)

| # | Class | Issue |
|---|---|---|
| H1 | WheeledVehicleDefClass (both) | Obsolete variables from old-format data not migrated to parent fields |
| H2 | DoorPhysDefClass | Old parent chunk `CHUNKID_DEF_OLD_PARENT=320001902` not handled |
| H3 | ElevatorPhysDefClass | Old parent chunk not handled; LOCK_CODE (micro-chunk 2) not read |

### Medium (missing fields / incomplete modeling)

| # | Class | Issue |
|---|---|---|
| M1 | AccessiblePhysDefClass | Flat data class, missing PhysDefClass/StaticPhys fields |
| M2 | DamageableStaticPhysDefClass | Flat data class, missing parent fields |
| M3 | DoorPhysDefClass | Flat data class, missing parent fields |
| M4 | ElevatorPhysDefClass | Flat data class, missing parent fields |
| M5 | ScriptZoneGameObjDef | Flat data class, missing parent fields |
| M6 | TransitionGameObjDefDef (combat/) | Zone/endingTM stored as raw ByteArray |

### Low (naming / style)

| # | Class | Issue |
|---|---|---|
| L1 | DoorPhysDefClassDef | Class name should be `DoorPhysDefClass` |
| L2 | ElevatorPhysDefClassDef | Class name should be `ElevatorPhysDefClass` |
| L3 | ScriptZoneGameObjDefDef | Class name should be `ScriptZoneGameObjDef` |
| L4 | TransitionGameObjDefDef | Class name should be `TransitionGameObjDef` |
| L5 | MotorcycleDefClassDef.kt | Filename should be `MotorcycleDefClass.kt` |
| L6 | WheeledVehicleDefClass (defs/) | Duplicates all ancestor constants locally |

### Duplicates

| # | Files |
|---|---|
| D1 | `defs/VehiclePhysDefClass.kt` vs `defs/phys/VehiclePhysDefClass.kt` |
| D2 | `defs/WheeledVehicleDefClass.kt` vs `defs/phys/WheeledVehicleDefClass.kt` |
| D3 | `defs/TransitionGameObjDef.kt` vs `defs/combat/TransitionGameObjDefDef.kt` |

---

## 4. WWSTRING Binary Format Note

`MotorVehicleDefClass` uses `WRITE_MICRO_CHUNK_WWSTRING` / `READ_MICRO_CHUNK_WWSTRING` for `EngineTorqueCurveFilename`. The same macro is used in `DefinitionClass` for `m_Name`. Kotlin reads these with standard null-terminated string parsing. If the WWSTRING macro writes a length prefix or wide characters, this would be a binary fidelity issue. In practice, Renegade-era `WideStringClass` likely stores null-terminated ISO-8859-1 bytes, so this probably works. Verify with actual `.mix` file data.

---

## 5. Classes With No Issues

The following classes passed all checks:

- **VehiclePhysDefClass** (`defs/`): Correct hierarchy, chunk IDs, micro-chunks, defaults, parsing.
- **TrackedVehicleDefClass**: Correct octal-to-hex chunk ID conversion, all fields and defaults match.
- **VTOLVehicleDefClass**: All 14 fields, DEG_TO_RADF defaults verified against C++ constructor.
- **HumanPhysDefClass**: Correctly delegates to Phys3DefClass with no own variables.
- **Phys3DefClass**: Verified defaults against C++ `DEFAULT_*` constants.
- **ProjectileDefClass**: Correct Vector3 handling for TumbleAxis, all defaults match.
