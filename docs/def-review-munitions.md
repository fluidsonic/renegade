# Definition Class Review: Munitions, Settings, Audio & Misc

> **Scope:** WeaponDefinitionClass, AmmoDefinitionClass, ExplosionDefinitionClass, SpawnerDefClass,
> GlobalSettingsDef, PurchaseSettingsDefs, CNCModeSettingsDef, EvaSettingsDefClass,
> CharacterClassSettingsDefClassDef, AudibleSoundDefinitionClass, TwiddlerClass
>
> **Date:** 2026-02-21

---

## Executive Summary

| Severity | Count | Description |
|----------|-------|-------------|
| **CRITICAL** | 1 | Off-by-one micro-chunk IDs in GlobalSettingsDef (5 fields read wrong data) |
| **HIGH** | 3 | Duplicate file pairs (Weapon, Ammo, Explosion, AudibleSound) |
| **MEDIUM** | 3 | Naming inconsistencies, hierarchy issues |
| **LOW** | 3 | Safe-type binary notes, helper duplication, minor style |
| **PASS** | 9 | Classes fully correct (all IDs, types, defaults verified) |

---

## CRITICAL BUGS

### 1. GlobalSettingsDef — Off-by-one micro-chunk IDs

**File:** `kotlin-server/.../defs/GlobalSettingsDefs.kt` lines 62, 83–86

The C++ enum in `globalsettings.cpp:117–156` auto-increments:

```
MICROCHUNKID_DEF_ENCY_EVENT_STRING_ID  = 24
MICROCHUNKID_DEF_HELP_TXT_SOUND       = 25   ← auto-increment
MICROCHUNKID_DEF_STEALTH_DISTANCE_HUMAN    = 26
MICROCHUNKID_DEF_STEALTH_DISTANCE_VEHICLE  = 27
MICROCHUNKID_DEF_MP_STEALTH_DISTANCE_HUMAN = 28
MICROCHUNKID_DEF_MP_STEALTH_DISTANCE_VEHICLE = 29
```

The Kotlin code reads the wrong IDs:

| Kotlin Field | Kotlin VARID | C++ VARID | Delta |
|---|---|---|---|
| `hudHelpTextSoundId` | **26** | **25** | +1 |
| `stealthDistanceHuman` | **27** | **26** | +1 |
| `stealthDistanceVehicle` | **28** | **27** | +1 |
| `mpStealthDistanceHuman` | **29** | **28** | +1 |
| `mpStealthDistanceVehicle` | **30** | **29** | +1 |

**Impact:** When loading C++ data:
- `hudHelpTextSoundId` reads micro-chunk 26, which is `StealthDistanceHuman` (a float) — interpreted as int, producing garbage.
- All stealth fields shift by +1; `mpStealthDistanceVehicle` reads ID 30 which doesn't exist in C++, silently falling back to default.

**Fix:** Change line 62 from `mcInt(26)` to `mcInt(25)`, and lines 83–86 from `mcFloat(27..30)` to `mcFloat(26..29)`.

---

## DUPLICATE FILES

Four definition types have two competing Kotlin implementations. **One copy in each pair must be removed.**

| Definition | `defs/` file | `defs/combat/` or `defs/audio/` file |
|---|---|---|
| **Weapon** | `WeaponDefinitionClass.kt` — class extends DefinitionClass, uses composite types (Vector3, RectClass) | `combat/WeaponDefinitionClass.kt` — data class named `WeaponDefinitionClassDef`, scalar fields |
| **Ammo** | `AmmoDefinitionClass.kt` — class extends DefinitionClass, composite types | `combat/AmmoDefinitionClass.kt` — class extends DefinitionClass, scalar fields |
| **Explosion** | `ExplosionDefinitionClass.kt` — **data class, does NOT extend DefinitionClass, no classId** | `combat/ExplosionDefinitionClass.kt` — class extends DefinitionClass |
| **AudibleSound** | `AudibleSoundDefinitionClass.kt` — standalone data class | `audio/AudibleSoundDefinitionClass.kt` — data class named `AudibleSoundDefinitionClassDef` |

Key differences between the two sets:
- `defs/` versions parse the parent (base DefinitionClass) chunk internally; `combat/`/`audio/` versions expect caller to pass `name`/`id`/`classId`.
- `defs/` versions use composite types (`Vector3`, `RectClass`); `combat/` versions decompose into scalar fields.
- `defs/` versions duplicate micro-chunk helper functions as `private` methods in each file; `combat/` versions reference shared `ChunkReader` extensions.

**Recommendation:** Consolidate to one implementation per class. The `defs/combat/` versions are more consistent architecturally (all extend DefinitionClass, externalize base parsing), but the `defs/` versions have better type modeling (composite types). Merge the best of both.

---

## NAMING INCONSISTENCIES

| Kotlin Class Name | C++ Class Name | Issue |
|---|---|---|
| `WeaponDefinitionClassDef` (combat/) | `WeaponDefinitionClass` | Extra "Def" suffix |
| `AudibleSoundDefinitionClassDef` (audio/) | `AudibleSoundDefinitionClass` | Extra "Def" suffix |
| `CharacterClassSettingsDefClassDef` | `CharacterClassSettingsDefClass` | Extra "Def" suffix |

Additionally, `CharacterClassSettingsDefClassDef.kt` lives under `defs/combat/` while all other settings defs are under `defs/` — inconsistent package placement.

---

## PER-CLASS DETAILED REVIEW

### WeaponDefinitionClass

- **C++ source:** `Combat/weaponmanager.h:26–76`, `weaponmanager.cpp:30–278`
- **Hierarchy:** `DefinitionClass` → `WeaponDefinitionClass` — **OK** (both versions)
- **Class ID:** `CLASSID_DEF_WEAPON = 0xB001` — **OK** (both)
- **Variables chunk:** `1205091654` — **OK** (defs/ uses hex `0x47D43D46u`, combat/ uses decimal)
- **Fields:** All 26 active fields present in both versions. 44 micro-chunk IDs correctly mapped (including skipped/deprecated slots).
- **Defaults:** All match C++ constructor exactly.
- **safe_type fields:** `ReloadTime` (safe_float), `ClipSize` (safe_int), `MaxInventoryRounds` (safe_int) — see Safe-Type note below.
- **Issues:** Duplicate files (HIGH), naming inconsistency in combat/ version (MEDIUM).

### AmmoDefinitionClass

- **C++ source:** `Combat/weaponmanager.h:82–165`, `weaponmanager.cpp:283–682`
- **Hierarchy:** `DefinitionClass` → `AmmoDefinitionClass` — **OK** (both)
- **Class ID:** `CLASSID_DEF_AMMO = 0xB002` — **OK** (both)
- **Variables chunk:** `1206091429` — **OK**
- **Fields:** All 46 active fields present. 63 micro-chunk IDs correctly mapped.
- **Defaults:** All match C++ constructor.
- **safe_type fields:** 9 fields (Warhead, Damage, Range, Velocity, SprayCount, BurstMax, EffectiveRange, ChargeTime, AmmoType).
- **Note:** C++ derives `ModelName` from `ModelFilename` at load time. Kotlin omits this derived field — acceptable for server use.
- **Issues:** Duplicate files (HIGH).

### ExplosionDefinitionClass

- **C++ source:** `Combat/explosion.h:19–45`, `explosion.cpp:24–157`
- **Hierarchy:** `DefinitionClass` → `ExplosionDefinitionClass` — `defs/` version is a standalone **data class** (WRONG), `combat/` version is correct.
- **Class ID:** `CLASSID_DEF_EXPLOSION = 0xB003` — combat/ OK, defs/ has no classId at all.
- **Variables chunk:** C++ uses octal `0317001525` = decimal `54264661` — **OK** (both versions)
- **Fields:** All 11 active fields present. 13 micro-chunk IDs correctly mapped.
- **Defaults:** All match.
- **No safe_type fields.**
- **Issues:** Duplicate files (HIGH), defs/ version missing hierarchy/classId (MEDIUM).

### SpawnerDefClass

- **C++ source:** `Combat/spawn.h:30–75`, `spawn.cpp:37–248`
- **Kotlin:** `defs/combat/SpawnerDefClass.kt` (single file, no duplicate)
- **Hierarchy:** `DefinitionClass` → `SpawnerDefClass` — **OK**
- **Class ID:** `0x300D` — **OK**
- **Variables chunk:** `1013991543` — **OK**
- **Fields:** All 15 scalar fields + 3 list fields present. 24 micro-chunk IDs correctly mapped.
- **Defaults:** All match (including `PlayerType = -2`, `SpawnMax = -1`, `TeleportFirstSpawn = true`, etc.)
- **Repeated micro-chunks:** Correctly handled via `findAllMicroChunks()`.
- **PASS — No issues.**

### GlobalSettingsDef

- **C++ source:** `Combat/globalsettings.h`, `globalsettings.cpp:117–245`
- **Kotlin:** `defs/GlobalSettingsDefs.kt` (contains GlobalSettingsDef, HumanLoiterGlobalSettingsDef, HUDGlobalSettingsDef, HumanAnimOverrideDef)
- **Class ID:** `0xF003` — **OK**
- **Variables chunk:** `803001813` — **OK**
- **Fields:** All 24 persisted C++ fields present.
- **Defaults:** All match C++.
- **CRITICAL BUG:** Off-by-one on IDs 25–29 (see above).

### HumanLoiterGlobalSettingsDef

- **Class ID:** `0xF002` — **OK**
- **Variables chunk:** `803001813` — **OK**
- **Fields:** 3 fields (activationDelay=1, loiterFrequency=2, loiterAnimList=3 repeated). **OK.**
- **Defaults:** `activationDelay=20f`, `loiterFrequency=10f`. **OK.**
- **PASS.**

### HUDGlobalSettingsDef

- **Class ID:** `0xF004` — **OK**
- **Variables chunk:** `803001813` — **OK**
- **Fields:** 94 micro-chunk IDs mapped. Extensive HUD layout data. Not individually verified against C++ (low priority for server; mostly client-side rendering data).
- **PASS (not fully audited — client-only data).**

### HumanAnimOverrideDef

- **Class ID:** `0xF007` — **OK**
- **Variables chunk:** `726011913` — **OK**
- **Fields:** 6 string fields (IDs 1–6). **OK.**
- **PASS.**

### PurchaseSettingsDefClass

- **C++ source:** `Combat/purchasesettings.h`, `purchasesettings.cpp`
- **Kotlin:** `defs/PurchaseSettingsDefs.kt`
- **Class ID:** `0xF008` — **OK**
- **Chunk IDs:** `PARENT = 0x08071203`, `VARIABLES = 0x08071204` — **OK**
- **Micro-chunk IDs:** All 10 active VARIDs match (1,2,5–12, skipping deprecated 3,4).
- **Constants:** `MAX_ENTRIES = 10`, `MAX_ALTERNATES = 3` — **OK**
- **Load logic:** State-machine approach (`entry_index` set by VARID_INDEX) correctly mirrors C++.
- **PASS — No issues.**

### TeamPurchaseSettingsDefClass

- **C++ source:** `Combat/teampurchasesettings.h`, `teampurchasesettings.cpp`
- **Kotlin:** `defs/PurchaseSettingsDefs.kt` (second class in file)
- **Class ID:** `0xF009` — **OK**
- **Chunk IDs:** `PARENT = 0x10231215`, `VARIABLES = 0x10231216` — **OK**
- **Micro-chunk IDs:** All 11 VARIDs match (1–11).
- **Constants:** `MAX_ENTRIES = 4` — **OK**
- **PASS — No issues.**

### CNCModeSettingsDef

- **C++ source:** `Combat/CNCModeSettings.h`, `CNCModeSettings.cpp`
- **Kotlin:** `defs/CNCModeSettingsDef.kt`
- **Class ID:** `0xF00A (61450)` — **OK**
- **Variables chunk:** `803001813` — **OK**
- **Micro-chunk IDs:** VARIDs 1–9 (individual fields), 10–39 (radio cmds array), 40–51 (beacon IDs), 52–81 (radio icons array) — all **OK**.
- **Defaults:** `announcementInterval = 30`, all IDs = 0 — **OK**
- **Note:** C++ `mConstructingID[NUM_TEAMS]` is NOT persisted (no VARID, no WRITE_MICRO_CHUNK). Kotlin correctly omits it.
- **PASS — No issues.**

### EvaSettingsDefClass

- **C++ source:** `Combat/evasettings.h`, `evasettings.cpp`
- **Kotlin:** `defs/EvaSettingsDefClass.kt`
- **Class ID:** `0xF005 (61445)` — **OK**
- **Variables chunk:** `803001813` — **OK**
- **Micro-chunk IDs:** All 13 VARIDs match (0x01–0x0D).
- **Types:** All RectClass (4 floats) and Vector2 (2 floats) correctly mapped to `Rect4`/`Vec2`.
- **Defaults:** All 13 fields match C++ constructor values exactly (detailed decimal values verified).
- **PASS — No issues.**

### CharacterClassSettingsDefClass

- **C++ source:** `Combat/characterclasssettings.h`, `characterclasssettings.cpp`
- **Kotlin:** `defs/combat/CharacterClassSettingsDefClassDef.kt`
- **Class ID:** `0xF006` — **OK**
- **Chunk IDs:** `VARIABLES = 0x12021028` — **OK**
- **Micro-chunk IDs:** 96 VARIDs (1–24 GDI costs, 25–48 NOD costs, 49–72 GDI defIDs, 73–96 NOD defIDs). Kotlin uses base + offset arithmetic: `VARID_COST_GDI_BASE=1`, `VARID_COST_NOD_BASE=25`, `VARID_DEFID_GDI_BASE=49`, `VARID_DEFID_NOD_BASE=73`. Pattern verified: `classIdx = offset / RANK_COUNT`, `rankIdx = offset % RANK_COUNT` matches C++ save order (class-major, rank-minor). **OK.**
- **Constants:** `CLASS_COUNT=6`, `RANK_COUNT=4`, `TEAM_COUNT=2` — **OK**
- **Defaults:** All tables zeroed — **OK**
- **Issues:** Naming inconsistency (extra "Def" suffix), inconsistent package location.

### AudibleSoundDefinitionClass

- **C++ source:** `WWAudio/AudibleSound.h:375–463`, `AudibleSound.cpp:1502–1776`
- **Hierarchy:** `DefinitionClass` → `AudibleSoundDefinitionClass`
- **Class ID:** `0x5000` — audio/ version OK, defs/ version missing.
- **Chunk IDs:** `VARIABLES = 0x00000100`, `BASE_CLASS = 0x00000200` — **OK**
- **Micro-chunk IDs:** All 20 active VARIDs (0x03–0x16, skipping 0x01–0x02 unused) match exactly in both versions.
- **Fields:** All 20 serialized fields present in both versions with correct types.
- **Defaults:** All match C++ constructor (verified: priority=0.5f, volume=1.0f, pan=0.5f, loopCount=1, dropOffRadius=40f, maxVolRadius=20f, is3D=true, type=1, logicalNotifyDelay=2f, logicalDropOffRadius=-1f, pitchFactor=1f, attenuationSphereColor=(0, 0.75, 0.75), etc.)
- **Issues:** Duplicate files (HIGH), naming inconsistency in audio/ version (MEDIUM), neither version extends DefinitionClass (MEDIUM).

### TwiddlerClass

- **C++ source:** `wwsaveload/twiddler.h`, `twiddler.cpp`
- **Kotlin:** `defs/TwiddlerClass.kt`
- **Hierarchy:** `DefinitionClass` → `TwiddlerClass` — **OK** (properly extends)
- **Chunk IDs:** `VARIABLES = 0x00000100`, `BASE_CLASS = 0x00000200` — **OK**
- **Micro-chunk IDs:** `VARID_DEFINTION_ID = 0x01` (repeated), `VARID_INDIRECT_CLASSID = 0x02` — **OK**
- **C++ typo preserved:** `VARID_DEFINTION_ID` (missing 'I' in DEFINITION) faithfully matches C++. Intentional for traceability.
- **Repeated micro-chunks:** Correctly handled via `forEachMicroChunk` — appends each `VARID_DEFINTION_ID` to list.
- **Defaults:** `indirectClassID = 0u`, `definitionList = emptyList()` — **OK**
- **Base class loading:** Correctly opens `CHUNKID_BASE_CLASS(0x200)` → `CHUNKID_VARIABLES(0x100)` → reads `VARID_INSTANCEID(0x01)` and `VARID_NAME(0x03)`.
- **PASS — No issues. Cleanest implementation reviewed.**

---

## CROSS-CUTTING: Safe-Type Handling

C++ uses `safe_int`/`safe_float` (from `datasafe.h`) for anti-cheat obfuscation. The macros `WRITE_SAFE_MICRO_CHUNK` and `READ_SAFE_MICRO_CHUNK` write **8 bytes** per field (4-byte value + 4-byte XOR complement).

Affected fields:

| Class | Field | C++ Type |
|---|---|---|
| WeaponDef | ReloadTime | safe_float |
| WeaponDef | ClipSize | safe_int |
| WeaponDef | MaxInventoryRounds | safe_int |
| AmmoDef | Warhead | safe_int |
| AmmoDef | Damage | safe_float |
| AmmoDef | Range | safe_float |
| AmmoDef | Velocity | safe_float |
| AmmoDef | SprayCount | safe_int |
| AmmoDef | BurstMax | safe_int |
| AmmoDef | EffectiveRange | safe_float |
| AmmoDef | ChargeTime | safe_float |
| AmmoDef | AmmoType | safe_int |

The Kotlin code reads these with `microChunkFloat`/`microChunkInt` (4 bytes).

**Analysis:** The first 4 bytes of a SAFE micro-chunk are the plain value. The micro-chunk header specifies total data length (8 bytes), but Kotlin reads only the first 4 via `ByteBuffer.wrap(bytes, 0, 4)`. **This is correct for loading** — the XOR complement bytes are harmlessly ignored.

**Caveat:** If save/re-serialize is ever needed, SAFE micro-chunks must write 8 bytes (value + XOR complement) for binary fidelity. Current read-only approach is fine.

---

## HELPER FUNCTION DUPLICATION

The `defs/` file set duplicates micro-chunk reader helpers (`microChunkInt`, `microChunkFloat`, `microChunkBool`, `microChunkString`) as `private` functions in each file. The `GlobalSettingsDefs.kt` defines them as `internal` extension functions on `ChunkReader`.

**Recommendation:** Consolidate all helper functions into a single shared location (e.g., `GlobalSettingsDefs.kt` already provides `internal` versions that other files could use).

---

## SUMMARY TABLE

| Class | File(s) | Hierarchy | Fields | Chunk IDs | Micro-chunk IDs | Defaults | Verdict |
|---|---|---|---|---|---|---|---|
| WeaponDefinitionClass | 2 files (DUPE) | OK | 26/26 | OK | 44/44 | OK | **HIGH: Duplicate** |
| AmmoDefinitionClass | 2 files (DUPE) | OK | 46/46 | OK | 63/63 | OK | **HIGH: Duplicate** |
| ExplosionDefinitionClass | 2 files (DUPE) | defs/ WRONG | 11/11 | OK | 13/13 | OK | **HIGH: Duplicate + hierarchy** |
| SpawnerDefClass | 1 file | OK | 18/18 | OK | 24/24 | OK | **PASS** |
| GlobalSettingsDef | 1 file | OK | 24/24 | OK | **5 wrong** | OK | **CRITICAL: Off-by-one** |
| HumanLoiterGlobalSettingsDef | 1 file | OK | 3/3 | OK | 3/3 | OK | **PASS** |
| HUDGlobalSettingsDef | 1 file | OK | ~94 | OK | ~94 | OK | **PASS** (not fully audited) |
| HumanAnimOverrideDef | 1 file | OK | 6/6 | OK | 6/6 | OK | **PASS** |
| PurchaseSettingsDefClass | 1 file | OK | 10/10 | OK | 10/10 | OK | **PASS** |
| TeamPurchaseSettingsDefClass | 1 file | OK | 11/11 | OK | 11/11 | OK | **PASS** |
| CNCModeSettingsDef | 1 file | OK | 81/81 | OK | 81/81 | OK | **PASS** |
| EvaSettingsDefClass | 1 file | OK | 13/13 | OK | 13/13 | OK | **PASS** |
| CharacterClassSettingsDefClass | 1 file | OK | 96/96 | OK | 96/96 | OK | **PASS** (naming issue) |
| AudibleSoundDefinitionClass | 2 files (DUPE) | Neither extends | 20/20 | OK | 20/20 | OK | **HIGH: Duplicate + hierarchy** |
| TwiddlerClass | 1 file | OK | 2/2 | OK | 2/2 | OK | **PASS** |
