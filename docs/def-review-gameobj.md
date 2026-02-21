# Definition Review: Game Object Definitions (Soldier/Vehicle/Smart chain)

Review of Kotlin definition classes against their C++ originals.
Date: 2026-02-21

---

## Summary of Critical Issues

| Severity | Count | Description |
|----------|-------|-------------|
| HIGH | 5 | Wrong default values that affect binary fidelity when micro-chunks are absent |
| MEDIUM | 4 | Naming inconsistencies (`DefDef` suffix) |
| MEDIUM | 2 | Duplicate files (same class in `defs/` and `defs/combat/`) |
| LOW | 2 | Inconsistent naming between duplicate files (`soundDefID` vs `soundDefId`) |
| LOW | 1 | `findChunkRecursive` approach is fragile (works but risky) |

---

## File Reviews

### SoldierGameObjDef.kt (`defs/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/SoldierGameObjDef.kt`

- **HIERARCHY**: OK. Correctly models `SoldierGameObjDef : SmartGameObjDef : ArmedGameObjDef : PhysicalGameObjDef : DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass` via composition (data classes for each layer).
- **FIELDS**: OK. All 12 soldier-own fields present + dialogList. Parent chain fields all present in their respective data classes.
- **CHUNK_IDS**: OK.
  - `CHUNKID_DEF_PARENT = 909991656u` matches C++ `909991656`
  - `CHUNKID_DEF_VARIABLES = 909991657u` matches C++ `909991657`
  - `CHUNKID_DEF_DIALOG_ENTRY = 909991658u` matches C++ `909991658`
- **MICRO_CHUNKS**: OK. All IDs 1-7, 10, 13-17 match C++. Micro 14 (ORATOR_TYPE) correctly marked as legacy/ignored at soldier level.
- **DEFAULTS**: **WRONG** (5 fields). The `load()` local variable defaults and data class defaults do not match C++ constructor:

  | Field | Kotlin default | C++ default | Impact |
  |-------|---------------|-------------|--------|
  | `turnRate` | `0f` | `DEG_TO_RADF(360) = ~6.2832f` | Wrong value if micro-chunk absent |
  | `jumpVelocity` | `0f` | `2.0f` | Wrong value if micro-chunk absent |
  | `useInnateBehavior` | `false` | `true` | Wrong value if micro-chunk absent |
  | `innateAggressiveness` | `0f` | `0.5f` | Wrong value if micro-chunk absent |
  | `innateTakeCoverProbability` | `0f` | `0.5f` | Wrong value if micro-chunk absent |

- **BINARY_FIDELITY**: ISSUE. If any soldier micro-chunk is absent (older file format), loaded values will differ from C++ behavior. In practice, all retail .mix files contain all micro-chunks, so this only affects edge cases. But for correctness, defaults should match.
- **DUPLICATES**: None (no `combat/` version of SoldierGameObjDef).
- **NAMING**: OK.

**Shared parent data class issues (affect Soldier AND Vehicle):**

`DamageableGameObjDefData` (line 172):
- `defaultPlayerType: Int = 0` should be `-2` (PLAYERTYPE_NEUTRAL)

`PhysicalGameObjDefData` (line 230):
- `oratorType: Int = -1` should be `999` (ORATOR_TYPE_START - 1 = 1000 - 1)

---

### VehicleGameObjDef.kt (`defs/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/VehicleGameObjDef.kt`

- **HIERARCHY**: OK. Same parent chain as Soldier via SmartGameObjDef composition.
- **FIELDS**: OK. All 21 vehicle-own fields present + transitions list. `VehicleType` enum correctly matches C++ values (0-4).
- **CHUNK_IDS**: OK.
  - `CHUNKID_DEF_PARENT = 930991656u` matches C++ `930991656`
  - `CHUNKID_DEF_VARIABLES = 930991657u` matches C++ `930991657`
  - `CHUNKID_DEF_TRANSITION = 930991658u` matches C++ `930991658`
  - Transition sub-chunk `0x11051106u` matches C++ TransitionDataClass.
- **MICRO_CHUNKS**: OK. All IDs 1-5, 18-36 match C++. Micro 18 (PHYS_ID) correctly handled as backward compat (ignored, physDefId lives in PhysicalGameObjDef).
- **DEFAULTS**: OK for vehicle-specific fields. All match C++ constructor:
  - `turnRadius = 10.0f`, `occupantsVisible = true`, `aim2D = true`, `engineSoundMaxPitchFactor = 2.0f`, `squishVelocity = 1.5f`, `numSeats = 2`, engine sounds = 0, report IDs = 0.
- **DEFAULTS (parent chain)**: **WRONG** — same `DamageableGameObjDefData` and `PhysicalGameObjDefData` issues as SoldierGameObjDef (see above).
- **BINARY_FIDELITY**: Same parent-chain default issues as SoldierGameObjDef. Vehicle-specific fields are fine.
- **DUPLICATES**: None.
- **NAMING**: Minor — uses `BASEGAMEOBJ_CHUNKID_DEF_PARENT_V` (trailing `_V`) vs `BASEGAMEOBJ_CHUNKID_DEF_PARENT` in SoldierGameObjDef.kt. Same value (1111991123), just inconsistent naming.

---

### C4GameObjDef.kt (`defs/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/C4GameObjDef.kt`

- **HIERARCHY**: OK. Correctly documents that C4 skips SimpleGameObjDef (C4GameObjDef::Save calls PhysicalGameObjDef::Save directly). No Simple fields are parsed.
- **FIELDS**: OK. All fields from DefinitionClass through C4GameObjDef present. Flattened structure instead of composition.
- **CHUNK_IDS**: OK.
  - `CHUNKID_C4_DEF_VARIABLES = 930991701u` matches C++ `930991701` (930991700 + 1)
  - All parent chunk IDs correct.
- **MICRO_CHUNKS**: OK.
  - C4 own: `MCID_C4_THROW_VELOCITY = 1` matches C++
  - Physical: IDs 1, 2, 4, 17-25 all correct
  - Damageable: IDs 1-6 all correct
  - Scriptable: IDs 2-3 correct
  - DefinitionClass: 0x01, 0x03 correct
- **DEFAULTS**: OK. `throwVelocity = 5f`, `oratorType = 999`, `defaultPlayerType = -2`, defense health/max = 100f. All match C++.
- **BINARY_FIDELITY**: ISSUE (minor). Uses `findChunkRecursive` to locate chunks by ID anywhere in the hierarchy. This works because C4's parent chain (Physical->Damageable->Scriptable->Base->Definition) has no chunk ID collisions. However, this approach would break if applied to a class with Smart/Armed in the chain (where `909991657` is reused for SmartGameObjDef's variables).
- **DUPLICATES**: Yes — duplicate of `defs/combat/C4GameObjDef.kt` (different approach: full vs simple).
- **NAMING**: OK.

---

### C4GameObjDef.kt (`combat/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/combat/C4GameObjDef.kt`

- **HIERARCHY**: N/A (standalone data class, no inheritance). Name/id/classId passed in externally.
- **FIELDS**: Only `throwVelocity`. No parent chain fields (by design — simple version).
- **CHUNK_IDS**: OK. `CHUNKID_DEF_VARIABLES = 930991701u` correct.
- **MICRO_CHUNKS**: OK. `MICROCHUNKID_DEF_THROW_VELOCITY = 1` correct.
- **DEFAULTS**: OK. `throwVelocity = 5f` matches C++ `ThrowVelocity(5)`.
- **BINARY_FIDELITY**: OK for own fields. Parent chain not parsed.
- **DUPLICATES**: Yes — duplicate of `defs/C4GameObjDef.kt`.
- **NAMING**: OK (no DefDef suffix here, just `C4GameObjDef`).

---

### PowerUpGameObjDef.kt (`defs/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/PowerUpGameObjDef.kt`

- **HIERARCHY**: OK. Correctly traverses PowerUp -> Simple -> Physical -> Damageable -> Scriptable -> Base -> Definition.
- **FIELDS**: OK. All fields from every level present and correctly typed.
- **CHUNK_IDS**: OK.
  - `POWERUP_PARENT = 909991656u`, `POWERUP_VARIABLES = 909991657u` match C++
  - `SIMPLE_PARENT = 930991656u`, `SIMPLE_VARIABLES = 930991657u` match C++
  - `PHYSICAL_VARIABLES = 909991657u`, `PHYSICAL_PARENT = 909991661u` match C++
  - All other parent chain IDs correct.
- **MICRO_CHUNKS**: OK. All PowerUp IDs (2-4, 6, 8-10, 13-21) match C++. Simple IDs (1-3) correct. Physical IDs correct. Damageable IDs correct.
- **DEFAULTS**: **WRONG** (multiple fallback values):

  | Field | Kotlin fallback | C++ default | Source |
  |-------|----------------|-------------|--------|
  | `defaultPlayerType` | `0` (line 162) | `-2` (PLAYERTYPE_NEUTRAL) | DamageableGameObjDef |
  | `playerTerminalType` | `0` (line 190) | `-1` (TYPE_NONE) | SimpleGameObjDef |
  | `oratorType` | `-1` (line 184) | `999` (ORATOR_TYPE_START-1) | PhysicalGameObjDef |
  | `defenseHealth` | `0f` (line 166) | `100f` | DefenseObjectDefClass |
  | `defenseHealthMax` | `0f` (line 167) | `100f` | DefenseObjectDefClass |

- **BINARY_FIDELITY**: ISSUE. If any chunk/micro-chunk is absent, fallback values differ from C++ constructor defaults. This is the same class of bug as in SoldierGameObjDef but with more affected fields.
- **DUPLICATES**: Yes — duplicate of `defs/combat/PowerUpGameObjDefDef.kt`.
- **NAMING**: OK.

---

### PowerUpGameObjDefDef.kt (`combat/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/combat/PowerUpGameObjDefDef.kt`

- **HIERARCHY**: N/A (standalone data class).
- **FIELDS**: OK. All 16 PowerUp-own fields present. No parent chain fields (by design).
- **CHUNK_IDS**: OK. `CHUNKID_DEF_VARIABLES = 909991657u` correct.
- **MICRO_CHUNKS**: OK. All 16 IDs (2-4, 6, 8-10, 13-21) match C++.
- **DEFAULTS**: OK. All match C++ constructor: `grantWeapon = true`, everything else 0/false/"".
- **BINARY_FIDELITY**: OK for own fields.
- **DUPLICATES**: Yes — duplicate of `defs/PowerUpGameObjDef.kt`.
- **NAMING**: **ISSUE** — `PowerUpGameObjDefDef` has redundant `Def` suffix. Should be `PowerUpGameObjDef`. Also, parse function is `parsePowerUpGameObjDefDef`.

---

### SimpleGameObjDefDef.kt (`combat/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/combat/SimpleGameObjDefDef.kt`

- **HIERARCHY**: N/A (standalone data class).
- **FIELDS**: OK. All 3 fields: `isEditorObject`, `isHiddenObject`, `playerTerminalType`.
- **CHUNK_IDS**: OK. `CHUNKID_DEF_VARIABLES = 930991657u` matches C++ `930991657`.
- **MICRO_CHUNKS**: OK. IDs 1, 2, 3 all correct.
- **DEFAULTS**: OK. `isEditorObject = false`, `isHiddenObject = false`, `playerTerminalType = -1` (TYPE_NONE). All match C++.
- **BINARY_FIDELITY**: OK.
- **DUPLICATES**: No full-hierarchy version exists in `defs/`.
- **NAMING**: **ISSUE** — `SimpleGameObjDefDef` has redundant `Def`. Should be `SimpleGameObjDef`. Parse function `parseSimpleGameObjDefDef` also affected.

---

### CinematicGameObjDefDef.kt (`combat/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/combat/CinematicGameObjDefDef.kt`

- **HIERARCHY**: N/A (standalone data class). C++ hierarchy is `CinematicGameObjDef : ArmedGameObjDef`.
- **FIELDS**: OK. All 6 fields present: `soundDefId`, `soundBoneName`, `animationName`, `autoFireWeapon`, `destroyAfterAnimation`, `cameraRelative`.
- **CHUNK_IDS**: OK. `CHUNKID_DEF_VARIABLES = 418001958u` matches C++ `418001958` (418001957 + 1).
- **MICRO_CHUNKS**: OK. IDs 1-6 all match C++. Correctly notes micro 3 has `XXX_` prefix in C++ but is still actively read/written.
- **DEFAULTS**: OK. `soundDefId = 0`, `autoFireWeapon = false`, `destroyAfterAnimation = true`, `cameraRelative = false`. All match C++ constructor.
- **BINARY_FIDELITY**: OK.
- **DUPLICATES**: No full-hierarchy version in `defs/`.
- **NAMING**: **ISSUE** — `CinematicGameObjDefDef` has redundant `Def`. Should be `CinematicGameObjDef`. Parse function `parseCinematicGameObjDef` does NOT have the double suffix (inconsistent with class name).

---

### BeaconGameObjDefDef.kt (`combat/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/combat/BeaconGameObjDefDef.kt`

- **HIERARCHY**: N/A (standalone data class). C++ hierarchy is `BeaconGameObjDef : SimpleGameObjDef`.
- **FIELDS**: OK. All 17 own fields present (plus name/id/classId).
- **CHUNK_IDS**: OK. `CHUNKID_DEF_VARIABLES = 35193910u` matches C++ `0x02190436` = 35193910.
- **MICRO_CHUNKS**: OK. All 17 IDs (1-17) match C++ enum exactly.
- **DEFAULTS**: OK. All match C++ constructor:
  - `broadcastToAllTime = 5f`, `armTime = 10f`, `disarmTime = 10f`, `detonateTime = 30f`, `postDetonateTime = 10f`, `isNuke = 1` (C++ uses `true` which is int 1). All int fields default to 0.
- **BINARY_FIDELITY**: OK.
- **DUPLICATES**: No full-hierarchy version in `defs/`.
- **NAMING**: **ISSUE** — `BeaconGameObjDefDef` has redundant `Def`. Should be `BeaconGameObjDef`. Parse function `parseBeaconGameObjDefDef` also affected.

---

### SpecialEffectsGameObjDef.kt (`defs/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/SpecialEffectsGameObjDef.kt`

- **HIERARCHY**: PARTIAL. Extends `DefinitionClass` directly. C++ hierarchy is `SpecialEffectsGameObjDef : PhysicalGameObjDef : DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass`. The Kotlin version navigates the parent chain to extract `id` and `name` from DefinitionClass, but does not expose any intermediate-level fields (Physical, Damageable, Scriptable).
- **FIELDS**: MISSING parent chain fields (Physical: type, physDefId, etc.; Damageable: defense, translated name, etc.; Scriptable: scripts). Only own fields `animationName` and `soundDefID` are present.
- **CHUNK_IDS**: OK. `CHUNKID_DEF_PARENT = 0x09010212u`, `CHUNKID_DEF_VARIABLES = 0x09010213u` match C++.
- **MICRO_CHUNKS**: OK. `VARID_DEF_ANIMATION_NAME = 1`, `VARID_DEF_SOUNDID = 2` match C++.
- **DEFAULTS**: OK. `animationName = ""`, `soundDefID = 0` match C++.
- **BINARY_FIDELITY**: OK for own fields. Parent chain data is not captured.
- **DUPLICATES**: Yes — duplicate of `defs/combat/SpecialEffectsGameObjDef.kt`.
- **NAMING**: Minor inconsistency — uses `soundDefID` (uppercase ID) vs `soundDefId` (lowercase Id) in the `combat/` version.

---

### SpecialEffectsGameObjDef.kt (`combat/`)

**Path:** `server/src/main/kotlin/ccr/server/defs/combat/SpecialEffectsGameObjDef.kt`

- **HIERARCHY**: Same as `defs/` version — extends `DefinitionClass` directly, C++ extends `PhysicalGameObjDef`.
- **FIELDS**: Same as `defs/` — only `animationName` and `soundDefId`. Parent chain fields missing.
- **CHUNK_IDS**: OK. `CHUNKID_DEF_VARIABLES = 0x09010213u` correct.
- **MICRO_CHUNKS**: OK. IDs 1 and 2 correct.
- **DEFAULTS**: OK. All match C++.
- **BINARY_FIDELITY**: OK for own fields.
- **DUPLICATES**: Yes — duplicate of `defs/SpecialEffectsGameObjDef.kt`. Uses `readMicroInt`/`readMicroString` imported from `ccr.server.defs` (different helper approach than `defs/` version which defines its own private helpers).
- **NAMING**: Uses `soundDefId` (lowercase Id) — differs from `defs/` version's `soundDefID` (uppercase ID).

---

## Cross-Cutting Issues

### 1. Default Value Mismatches (HIGH)

The `defs/` full-hierarchy versions (`SoldierGameObjDef.kt`, `VehicleGameObjDef.kt`, `PowerUpGameObjDef.kt`) have incorrect default/fallback values in shared parent data classes. If a micro-chunk is absent from the binary data (e.g., older format), the loaded value will differ from C++.

**Affected shared defaults:**

| Data Class | Field | Kotlin | C++ | Fix |
|-----------|-------|--------|-----|-----|
| `DamageableGameObjDefData` | `defaultPlayerType` | `0` | `-2` | Change to `-2` |
| `PhysicalGameObjDefData` | `oratorType` | `-1` | `999` | Change to `999` |
| `SoldierGameObjDef` (data class + load vars) | `turnRate` | `0f` | `~6.2832f` | Change to `DEG_TO_RAD(360)` |
| `SoldierGameObjDef` (data class + load vars) | `jumpVelocity` | `0f` | `2f` | Change to `2f` |
| `SoldierGameObjDef` (data class + load vars) | `useInnateBehavior` | `false` | `true` | Change to `true` |
| `SoldierGameObjDef` (data class + load vars) | `innateAggressiveness` | `0f` | `0.5f` | Change to `0.5f` |
| `SoldierGameObjDef` (data class + load vars) | `innateTakeCoverProbability` | `0f` | `0.5f` | Change to `0.5f` |
| `PowerUpGameObjDef` load fallbacks | `defaultPlayerType` | `0` | `-2` | Change to `-2` |
| `PowerUpGameObjDef` load fallbacks | `playerTerminalType` | `0` | `-1` | Change to `-1` |
| `PowerUpGameObjDef` load fallbacks | `oratorType` | `-1` | `999` | Change to `999` |
| `PowerUpGameObjDef` load fallbacks | `defenseHealth` | `0f` | `100f` | Change to `100f` |
| `PowerUpGameObjDef` load fallbacks | `defenseHealthMax` | `0f` | `100f` | Change to `100f` |

Note: `C4GameObjDef.kt` in `defs/` does NOT have these issues — its defaults are correct (`oratorType = 999`, `defaultPlayerType = -2`, defense health = 100f).

### 2. Duplicate Files (MEDIUM)

Three definitions exist in both `defs/` and `defs/combat/` with different approaches:

| `defs/` (full hierarchy) | `defs/combat/` (simple) |
|--------------------------|------------------------|
| `C4GameObjDef.kt` — flattened, all parent fields | `C4GameObjDef.kt` — own fields only |
| `PowerUpGameObjDef.kt` — explicit chain navigation | `PowerUpGameObjDefDef.kt` — own fields only |
| `SpecialEffectsGameObjDef.kt` — extends DefinitionClass | `SpecialEffectsGameObjDef.kt` — extends DefinitionClass |

The two approaches serve different use cases, but having both creates maintenance risk. Consider consolidating or clearly documenting the relationship.

### 3. DefDef Naming (MEDIUM)

Four files in `defs/combat/` use a `DefDef` suffix:
- `PowerUpGameObjDefDef` (class) / `parsePowerUpGameObjDefDef` (function)
- `SimpleGameObjDefDef` / `parseSimpleGameObjDefDef`
- `BeaconGameObjDefDef` / `parseBeaconGameObjDefDef`
- `CinematicGameObjDefDef` (class) / `parseCinematicGameObjDef` (function — inconsistently lacks double Def!)

The C++ classes use single `Def`: `SimpleGameObjDef`, `BeaconGameObjDef`, etc. The Kotlin should match.

### 4. `findChunkRecursive` vs Explicit Chain (LOW)

`C4GameObjDef.kt` in `defs/` uses `findChunkRecursive` to locate chunks by ID anywhere in the hierarchy. This works for C4 (whose chain has no chunk ID collisions) but would be incorrect for Soldier/Vehicle chains where `909991657` is used by both `PhysicalGameObjDef` and `SmartGameObjDef`. The explicit chain traversal approach used by Soldier/Vehicle/PowerUp is safer.

---

## Verification Matrix

| File | Hierarchy | Fields | ChunkIDs | MicroChunks | Defaults | BinaryFidelity | Naming |
|------|-----------|--------|----------|-------------|----------|----------------|--------|
| SoldierGameObjDef.kt (defs/) | OK | OK | OK | OK | **WRONG** | **ISSUE** | OK |
| VehicleGameObjDef.kt (defs/) | OK | OK | OK | OK | **WRONG** (parent) | **ISSUE** | OK |
| C4GameObjDef.kt (defs/) | OK | OK | OK | OK | OK | Low risk | OK |
| C4GameObjDef.kt (combat/) | N/A | OK (own) | OK | OK | OK | OK | OK |
| PowerUpGameObjDef.kt (defs/) | OK | OK | OK | OK | **WRONG** | **ISSUE** | OK |
| PowerUpGameObjDefDef.kt (combat/) | N/A | OK (own) | OK | OK | OK | OK | **DefDef** |
| SimpleGameObjDefDef.kt (combat/) | N/A | OK | OK | OK | OK | OK | **DefDef** |
| CinematicGameObjDefDef.kt (combat/) | N/A | OK | OK | OK | OK | OK | **DefDef** |
| BeaconGameObjDefDef.kt (combat/) | N/A | OK | OK | OK | OK | OK | **DefDef** |
| SpecialEffectsGameObjDef.kt (defs/) | Partial | Missing parent | OK | OK | OK | OK | Minor |
| SpecialEffectsGameObjDef.kt (combat/) | Partial | Missing parent | OK | OK | OK | OK | Minor |
