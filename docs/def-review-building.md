# Definition Review: Building Chain, Subtypes, Bosses & Others

Review of Kotlin definition classes against C++ originals.

**Date:** 2026-02-21
**Scope:** BuildingGameObjDef chain, building subtypes, boss defs, SAMSite, DamageZone
**Status:** Read-only review. No code changes made.

---

## Legend

| Tag | Meaning |
|-----|---------|
| OK | Matches C++ original |
| WARN | Minor discrepancy, non-blocking |
| BUG | Incorrect behavior or data loss risk |
| NOTE | Observation, not necessarily a problem |

---

## 1. BuildingGameObjDef.kt

**Kotlin:** `ccr.server.defs.BuildingGameObjDef`
**C++:** `BuildingGameObjDef` (Combat/building.h, building.cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | NOTE | C++: `BuildingGameObjDef : DamageableGameObjDef : ScriptableGameObjDef : BaseGameObjDef : DefinitionClass`. Kotlin: standalone `open class` with all parent fields inlined (does not extend `DefinitionClass`). Intentional flat design — different from all other def classes. |
| FIELDS | OK | All persisted fields from the full chain are present: DefinitionClass (name, id), ScriptableGameObjDef (scriptNames, scriptParameters), DamageableGameObjDef (translatedNameId, infoIconTexture, encyclopediaType/Id, notTargetable, defaultPlayerType), DefenseObjectDef (8 fields), BuildingGameObjDef (meshPrefix, mctSkin, buildingType, 4 report IDs). |
| CHUNK_IDS | OK | CHUNKID_DEF_BASE_VARIABLES=0x100, Scriptable=627001057, Damageable=207011206, DefenseObj=207011207, DefenseObjVars=7311607, BuildingVars=207011031. All match C++. |
| MICRO_CHUNKS | OK | All micro-chunk IDs verified: DefBase (0x01=ID, 0x03=Name), Scriptable (2=ScriptName, 3=ScriptParams), Damageable (1-6), DefenseObj (0x00-0x07), Building (1-8 including legacy team). |
| DEFAULTS | OK | BuildingType=-1 (TYPE_NONE), MCTSkin=0, report IDs=0, encyclopediaType=-1, defaultPlayerType=-2 (NEUTRAL), DefenseObj health/max=100. All match C++ constructors. |
| BINARY_FIDELITY | OK | Full recursive chunk parsing via `findChunkRecursive`. Legacy team migration implemented correctly (MCID 4 → maps to defaultPlayerType). |
| DUPLICATES | WARN | `DefenseObjectDef` is defined here as a nested class AND duplicated as `RocketsDefenseObjectDef` in SakuraBossGameObjDef.kt. Same 8-field structure with same chunk/micro IDs. |
| NAMING | OK | Class name matches C++ `BuildingGameObjDef`. |

**Additional notes:**
- `CHUNKID_GAME_OBJECT_DEF_BUILDING = 0x00040133u` — this is the persist factory chunk ID, correctly placed.
- `BuildingConstants` type enum values match C++ (TYPE_NONE=-1 through TYPE_BASE_DEFENSE=9).
- `load()` is a companion factory method rather than a standalone `parse*` function — different pattern from other def parsers.
- Does NOT use `DefinitionClass` as a base class, so `name` and `definitionId` are own properties rather than inherited.

---

## 2. BuildingGameObjDefDef.kt (combat package)

**Kotlin:** `ccr.server.defs.combat.BuildingGameObjDefDef`
**C++:** N/A

| Check | Status | Detail |
|-------|--------|--------|
| DUPLICATES | WARN | Superseded empty file. Contains only a redirect comment pointing to `ccr.server.defs.BuildingGameObjDef`. Should be deleted to avoid confusion. |

---

## 3. ComCenterGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.ComCenterGameObjDef`
**C++:** `ComCenterGameObjDef` (Combat/comcentergameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: `ComCenterGameObjDef : BuildingGameObjDef`. Kotlin: extends `DefinitionClass(name, id, classId)`. Flat hierarchy is consistent with the subtype pattern used here (parent chain parsed separately). |
| FIELDS | OK | C++ `.h` declares `UnloadTime`, `FundsGathered`, `HarvesterDefID` as members, but `Save_Variables()` is empty — these are NOT persisted. Kotlin correctly has no fields. |
| CHUNK_IDS | NOTE | C++ uses CHUNKID_DEF_PARENT=0x02211153, CHUNKID_DEF_VARIABLES=0x02211154. Kotlin parser doesn't reference these since there's nothing to parse. Acceptable for a no-field subtype. |
| MICRO_CHUNKS | OK | None (empty Save_Variables in C++). |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data to extract. |
| DUPLICATES | OK | No duplicates. |
| NAMING | OK | Class name matches C++ (minus the file's `DefDef` filename convention). |
| CLASS_ID | OK | `0xD008u` = CLASSID_BUILDINGS + 8. Matches C++. |

---

## 4. PowerPlantGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.PowerPlantGameObjDef`
**C++:** `PowerPlantGameObjDef` (Combat/powerplantgameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `BuildingGameObjDef`. Kotlin: extends `DefinitionClass`. Same flat pattern. |
| FIELDS | OK | C++ `.h` declares `UnloadTime`, `FundsGathered`, `HarvesterDefID` but `Save_Variables()` is empty. Kotlin correctly has no fields. |
| CHUNK_IDS | NOTE | C++ shares same chunk IDs as ComCenter (CHUNKID_DEF_PARENT=0x02211153). Not referenced in Kotlin. |
| MICRO_CHUNKS | OK | None. |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0xD003u` = CLASSID_BUILDINGS + 3. |

---

## 5. RefineryGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.RefineryGameObjDef`
**C++:** `RefineryGameObjDef` (Combat/refinerygameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `BuildingGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | `unloadTime(Float)`, `fundsGathered(Float)`, `fundsDistributedPerSec(Float)`, `harvesterDefId(Int)`. All match C++ persisted fields. |
| CHUNK_IDS | OK | CHUNKID_DEF_VARIABLES = `0x02200639u`. C++ CHUNKID_DEF_PARENT=0x02200638, +1 = 0x02200639. Match. |
| MICRO_CHUNKS | OK | 1=UnloadTime, 2=FundsGathered, 3=HarvesterDefID, 4=FundsPerSec. Match C++ enum. |
| DEFAULTS | OK | All `0`/`0f`. Match C++ constructor. |
| BINARY_FIDELITY | OK | Uses shared `readMicroFloat`/`readMicroInt` from `ccr.server.defs` package. |
| DUPLICATES | OK | Uses shared micro-chunk helpers (good). |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0xD002u` = CLASSID_BUILDINGS + 2. |

---

## 6. RepairBayGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.RepairBayGameObjDef`
**C++:** `RepairBayGameObjDef` (Combat/repairbaygameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `BuildingGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | `repairPerSec(Float)`, `repairingStaticAnimDefId(Int)`. Match C++. |
| CHUNK_IDS | OK | CHUNKID_DEF_VARIABLES = `0x02200639u`. Match. |
| MICRO_CHUNKS | OK | 1=RepairPerSec, 2=RepairingStaticAnimDefID. Match C++. |
| DEFAULTS | OK | `0f`, `0`. Match C++. |
| BINARY_FIDELITY | OK | Uses shared helpers. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0xD009u` = CLASSID_BUILDINGS + 9. |

---

## 7. VehicleFactoryGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.VehicleFactoryGameObjDef`
**C++:** `VehicleFactoryGameObjDef` (Combat/vehiclefactorygameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `BuildingGameObjDef`. Kotlin: `open class` extending `DefinitionClass`. Marked `open` for WarFactory/AirStrip. |
| FIELDS | OK | `padClearingWarhead(Int)`, `totalBuildingTime(Float)`. Match C++. |
| CHUNK_IDS | OK | CHUNKID_DEF_VARIABLES = `0x02200639u`. Match. |
| MICRO_CHUNKS | OK | 1=unused (skipped), 2=PadClearingWarhead, 3=TotalBuildingTime. Match C++. |
| DEFAULTS | OK | `25`, `12f`. Match C++ constructor defaults. |
| BINARY_FIDELITY | OK | Uses shared helpers. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0xD005u` = CLASSID_BUILDINGS + 5. |

---

## 8. WarFactoryGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.WarFactoryGameObjDef`
**C++:** `WarFactoryGameObjDef` (Combat/warfactorygameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | NOTE | C++: `WarFactoryGameObjDef : VehicleFactoryGameObjDef`. Kotlin: extends `DefinitionClass` (not `VehicleFactoryGameObjDef`). Since VehicleFactory is `open` in Kotlin, this could inherit from it, but the flat pattern is consistent with the rest. |
| FIELDS | OK | No additional persisted fields (C++ `Save_Variables` is empty). |
| CHUNK_IDS | NOTE | C++ CHUNKID_DEF_PARENT = 0x02200638. Not referenced in Kotlin (nothing to parse). |
| MICRO_CHUNKS | OK | None. |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0xD007u` = CLASSID_BUILDINGS + 7. |

---

## 9. SoldierFactoryGameObjDef.kt

**Kotlin:** `ccr.server.defs.SoldierFactoryGameObjDef`
**C++:** `SoldierFactoryGameObjDef` (Combat/soldierfactorygameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `BuildingGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | No additional persisted fields. C++ `.h` declares `UnloadTime`, `FundsGathered`, `HarvesterDefID` but `Save_Variables` is empty. Correctly omitted. |
| CHUNK_IDS | NOTE | C++ uses CHUNKID_DEF_PARENT=0x02211153, CHUNKID_DEF_VARIABLES=0x02211154. Correctly documented in KDoc. |
| MICRO_CHUNKS | OK | None. |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0xD004u` = CLASSID_BUILDINGS + 4. |
| PACKAGE | NOTE | In `ccr.server.defs` (not `ccr.server.defs.combat`). Inconsistent with other building subtypes which are in `combat`. |

---

## 10. AirStripGameObjDefDef.kt

**Kotlin:** `ccr.server.defs.combat.AirStripGameObjDefDef`
**C++:** `AirStripGameObjDef` (Combat/airstripgameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | WARN | C++: `AirStripGameObjDef : VehicleFactoryGameObjDef`. Kotlin: `data class` not extending any parent. Uses `data class` instead of regular `class` — inconsistent with all other defs. |
| FIELDS | OK | `cinematicDefId(Int)`, `cinematicSlotIndex(Int)`, `cinematicLengthToDropOff(Float)`, `cinematicLengthToVehicleDisplay(Float)`. Match C++. |
| CHUNK_IDS | OK | CHUNKID_DEF_VARIABLES = `0x02200639u`. Match. |
| MICRO_CHUNKS | OK | 1=CinematicDefID, 2=CinematicLengthToDropOff, 3=CinematicSlotIndex, 4=DisplayVehicleTime. Match C++. |
| DEFAULTS | OK | All `0`/`0f`. Match C++. |
| BINARY_FIDELITY | OK | Parsing is correct. |
| DUPLICATES | BUG | Defines its own private `microChunkInt()` and `microChunkFloat()` extension functions on `ChunkReader`. These duplicate the shared `readMicroInt`/`readMicroFloat` used by other files. Should use the shared versions from `ccr.server.defs`. |
| NAMING | BUG | Class name `AirStripGameObjDefDef` has double "Def". C++ is `AirStripGameObjDef`. Also affects parse function name `parseAirStripGameObjDefDef`. |
| CLASS_ID | OK | `0xD006u` = CLASSID_BUILDINGS + 6. |

---

## 11. SAMSiteGameObjDef.kt

**Kotlin:** `ccr.server.defs.SAMSiteGameObjDef`
**C++:** `SAMSiteGameObjDef` (Combat/samsite.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `SmartGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | No additional def-specific persisted fields. C++ `Save()` only wraps parent in CHUNKID_DEF_PARENT (930991800). |
| CHUNK_IDS | NOTE | CHUNKID_DEF_PARENT = 930991800 documented in KDoc. No DEF_VARIABLES chunk. |
| MICRO_CHUNKS | OK | None. |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0x3007u`. |
| PACKAGE | NOTE | In `ccr.server.defs` (not `ccr.server.defs.combat`). Same inconsistency as SoldierFactory. |

---

## 12. SakuraBossGameObjDef.kt

**Kotlin:** `ccr.server.defs.SakuraBossGameObjDef`
**C++:** `SakuraBossGameObjDef` (Combat/sakurabossgameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `VehicleGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | `gattlingGunDefId(Int)`, `rocketLauncherDefId(Int)`, `gattlingGunRevSoundDefId(Int)`, `rocketDoorOpenSoundId(Int)`, `rocketDestroyedExplosionId(Int)`, `rocketsDefense(RocketsDefenseObjectDef)`. All match C++. |
| CHUNK_IDS | OK | CHUNKID_DEF_VARIABLES = `0x09070459u`, CHUNKID_DEF_ROCKET_DEFENSEOBJ_DEF = `0x0907045Au`. Match C++. |
| MICRO_CHUNKS | OK | 1=GattlingGunDefID, 2=RocketLauncherDefID, 3=GattlingGunRevSoundDefID, 4=RocketDoorOpenSoundID, 5=RocketDestroyedExplosionID. Match C++. |
| DEFAULTS | OK | All `0` for int fields, DefenseObj defaults match (`health=100, max=100`, rest `0`). |
| BINARY_FIDELITY | OK | Correctly parses nested DefenseObjectDefClass chunk for rockets. |
| DUPLICATES | WARN | (1) `RocketsDefenseObjectDef` duplicates `BuildingGameObjDef.DefenseObjectDef` — same 8 fields, same chunk/micro IDs. Should share a single type. (2) Defines private `readMicroInt()`/`readMicroFloat()` helpers that duplicate the shared versions in `ccr.server.defs`. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0x3014u`. |
| PACKAGE | NOTE | In `ccr.server.defs` (not `ccr.server.defs.combat`). |

---

## 13. MendozaBossGameObjDefClass.kt

**Kotlin:** `ccr.server.defs.combat.MendozaBossGameObjDefClass`
**C++:** `MendozaBossGameObjDefClass` (Combat/mendozabossgameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `SoldierGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | C++ declares micro-chunk IDs in enum (gatling/rocket defs) but `Save_Variables()` is empty. Kotlin correctly has no fields. |
| CHUNK_IDS | NOTE | C++ CHUNKID_DEF_PARENT = 0x09230242. Not referenced in Kotlin. |
| MICRO_CHUNKS | OK | None persisted (C++ enum is dead code). |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++ (includes "Class" suffix as in original). |
| CLASS_ID | OK | `0x3017u` = CLASSID_GAME_OBJECTS + 23. |

---

## 14. RaveshawBossGameObjDefClass.kt

**Kotlin:** `ccr.server.defs.combat.RaveshawBossGameObjDefClass`
**C++:** `RaveshawBossGameObjDefClass` (Combat/raveshawbossgameobj.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `SoldierGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | Same as Mendoza — micro-chunk IDs declared but `Save_Variables()` empty. Correctly no fields. |
| CHUNK_IDS | NOTE | C++ shares CHUNKID_DEF_PARENT = 0x09230242 with Mendoza. |
| MICRO_CHUNKS | OK | None persisted. |
| DEFAULTS | OK | N/A. |
| BINARY_FIDELITY | OK | No def-specific data. |
| DUPLICATES | OK | None. |
| NAMING | OK | Matches C++ (includes "Class" suffix). |
| CLASS_ID | OK | `0x3018u` = CLASSID_GAME_OBJECTS + 24. |

---

## 15. DamageZoneGameObjDef.kt

**Kotlin:** `ccr.server.defs.combat.DamageZoneGameObjDef`
**C++:** `DamageZoneGameObjDef` (Combat/damagezone.h/cpp)

| Check | Status | Detail |
|-------|--------|--------|
| HIERARCHY | OK | C++: extends `BaseGameObjDef`. Kotlin: extends `DefinitionClass`. |
| FIELDS | OK | `damageRate(Float)`, `damageWarhead(Int)`, `colorR/G/B(Float)`. Match C++. Color stored as 3 separate floats (C++ is Vector3). |
| CHUNK_IDS | OK | CHUNKID_DEF_VARIABLES = `626000948u` = 626000947 + 1. Match C++. |
| MICRO_CHUNKS | OK | 1=deprecated (skipped), 2=ZoneColor(Vector3, 12 bytes), 3=DamageRate, 4=DamageWarhead. Match C++. |
| DEFAULTS | OK | `damageRate=10f`, `damageWarhead=1`, `colorR=0.7f`, `colorG=0f`, `colorB=0f`. Match C++ constructor. |
| BINARY_FIDELITY | OK | Color parsed via `readMicroFloatAt(id, offset)` at byte offsets 0/4/8 from micro-chunk 2. Correctly handles the Vector3-in-single-micro-chunk pattern. |
| DUPLICATES | OK | Uses shared helpers from `ccr.server.defs`. |
| NAMING | OK | Matches C++. |
| CLASS_ID | OK | `0x3012u` = CLASSID_GAME_OBJECTS + 18. |

---

## Cross-Cutting Issues

### 1. Package Inconsistency

Files are split across two packages without clear criteria:

| Package | Files |
|---------|-------|
| `ccr.server.defs` | BuildingGameObjDef, SoldierFactoryGameObjDef, SAMSiteGameObjDef, SakuraBossGameObjDef |
| `ccr.server.defs.combat` | ComCenter, PowerPlant, Refinery, RepairBay, VehicleFactory, WarFactory, AirStrip, MendozaBoss, RaveshawBoss, DamageZone, BuildingGameObjDefDef (superseded) |

All of these are from `Combat/` in C++. Consider consolidating to one package.

### 2. Duplicated Micro-Chunk Helpers

Three different sets of micro-chunk reader helpers exist:

| Location | Functions | Used By |
|----------|-----------|---------|
| `ccr.server.defs` package (shared) | `readMicroFloat()`, `readMicroInt()`, `readMicroFloatAt()` | Refinery, RepairBay, VehicleFactory, DamageZone |
| `AirStripGameObjDefDef.kt` (private) | `microChunkInt()`, `microChunkFloat()` | AirStrip only |
| `SakuraBossGameObjDef.kt` (private) | `readMicroInt()`, `readMicroFloat()` | SakuraBoss only |
| `BuildingGameObjDef.kt` (companion) | `mcInt()`, `mcFloat()`, `mcBool()`, `mcString()` | BuildingGameObjDef only |

**Recommendation:** Consolidate to a single set of shared helpers.

### 3. Duplicated DefenseObjectDef

Two identical structures for `DefenseObjectDefClass`:

| Location | Type Name | Chunk/Micro IDs |
|----------|-----------|-----------------|
| `BuildingGameObjDef.DefenseObjectDef` | Nested class | VARS=7311607, micros 0x00-0x07 |
| `RocketsDefenseObjectDef` | Top-level data class | Same: VARS=7311607, micros 0x00-0x07 |

**Recommendation:** Share a single `DefenseObjectDef` type.

### 4. AirStrip Double-Def Naming

`AirStripGameObjDefDef` should be `AirStripGameObjDef` to match C++. The parse function `parseAirStripGameObjDefDef` should similarly be `parseAirStripGameObjDef`.

### 5. AirStrip Uses `data class`

`AirStripGameObjDefDef` is a `data class` while all other def classes are plain `class`. This is inconsistent and generates unnecessary `equals`/`hashCode`/`copy`/`componentN` methods.

### 6. Superseded Empty File

`ccr.server.defs.combat.BuildingGameObjDefDef.kt` is an empty redirect. Should be deleted.

### 7. BuildingGameObjDef Structural Divergence

`BuildingGameObjDef` in `ccr.server.defs` uses a fundamentally different pattern from all other def classes:
- Does NOT extend `DefinitionClass` — has its own `name`/`definitionId` properties
- Uses property name `definitionId` instead of `id`
- Has a companion `load()` factory method instead of a standalone `parse*()` function
- Parses the full parent chain itself (DefinitionClass → Scriptable → Damageable → Building)
- Other subtypes rely on the definition DB reader to extract `name`/`id`/`classId` and pass them in

This is noted as intentional design (the class was written to be self-contained), but it means `BuildingGameObjDef` instances are not polymorphic with other `DefinitionClass` subtypes.

---

## Summary

| File | Verdict | Issues |
|------|---------|--------|
| BuildingGameObjDef.kt | OK | NOTE: Different structural pattern, doesn't extend DefinitionClass |
| BuildingGameObjDefDef.kt (combat) | WARN | Superseded empty file, delete |
| ComCenterGameObjDefDef.kt | OK | Clean |
| PowerPlantGameObjDefDef.kt | OK | Clean |
| RefineryGameObjDefDef.kt | OK | Clean |
| RepairBayGameObjDefDef.kt | OK | Clean |
| VehicleFactoryGameObjDefDef.kt | OK | Clean |
| WarFactoryGameObjDefDef.kt | OK | Clean |
| SoldierFactoryGameObjDef.kt | OK | NOTE: Package inconsistency |
| AirStripGameObjDefDef.kt | BUG | Double "Def" naming, `data class`, duplicated helpers |
| SAMSiteGameObjDef.kt | OK | NOTE: Package inconsistency |
| SakuraBossGameObjDef.kt | WARN | Duplicated DefenseObjectDef, duplicated helpers |
| MendozaBossGameObjDefClass.kt | OK | Clean |
| RaveshawBossGameObjDefClass.kt | OK | Clean |
| DamageZoneGameObjDef.kt | OK | Clean |

**Overall:** All chunk IDs, micro-chunk IDs, field types, and default values match the C++ originals. Binary parsing fidelity is correct across all files. The issues found are structural/naming concerns, not data correctness bugs.
