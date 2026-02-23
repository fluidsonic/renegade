# Phase 16: DefinitionClass Promotion

## Goal

Convert 11 data class definitions to proper `DefinitionClass` subclasses so that
`registry.findById(id) as? FooClass` works for typed lookups. Removes the three
FIXME lines in `FullDefinitionLoader.kt` and the global-settings fallback block.

## Background

`DefinitionClass` is the base class for all loaded game definitions:
```kotlin
open class DefinitionClass(val name: String, val id: UInt, val chunkId: UInt)
```

Currently 11 defs are `data class` with no inheritance from `DefinitionClass`.
`FullDefinitionLoader.dispatch()` returns `fallback` (a plain `DefinitionClass`)
for them, so typed lookups (`as? C4GameObjDef`) always fail.

## Conversion Pattern

For every def:
1. Remove `data` keyword
2. Remove `val` from `name`, `id`, `chunkId` constructor params
3. Add `: DefinitionClass(name, id, chunkId)` after the closing `)`
4. Add `import ccr.server.defs.DefinitionClass` if not present
5. Parser return types don't change — they already return the correct type

Example (`C4GameObjDef`):
```kotlin
// Before:
data class C4GameObjDef(
    val name: String,
    val id: UInt,
    val chunkId: UInt,
    val throwVelocity: Float = 5f,
) { ... }

// After:
class C4GameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val throwVelocity: Float = 5f,
) : DefinitionClass(name, id, chunkId) { ... }
```

## Files to Modify

### Task 1 — Three game object defs

**`server/src/main/kotlin/ccr/server/defs/combat/C4GameObjDef.kt`**
- Convert `data class C4GameObjDef` → `class C4GameObjDef : DefinitionClass`
- Add `import ccr.server.defs.DefinitionClass`
- `parseC4GameObjDef()` signature/return type unchanged

**`server/src/main/kotlin/ccr/server/defs/combat/SimpleGameObjDef.kt`**
- Convert `data class SimpleGameObjDef` → `class SimpleGameObjDef : DefinitionClass`
- Add `import ccr.server.defs.DefinitionClass`
- `parseSimpleGameObjDef()` unchanged

**`server/src/main/kotlin/ccr/server/defs/combat/CinematicGameObjDef.kt`**
- Convert `data class CinematicGameObjDef` → `class CinematicGameObjDef : DefinitionClass`
- Add `import ccr.server.defs.DefinitionClass`
- `parseCinematicGameObjDef()` unchanged

**`server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt`** (update lines 83–109)
- Replace each `CHUNK_ID -> fallback` FIXME with a real dispatch:
  ```kotlin
  C4GameObjDef.CHUNK_ID ->
      parseC4GameObjDef(objDataChunk, name, id, chunkId)

  SimpleGameObjDef.CHUNK_ID ->
      parseSimpleGameObjDef(objDataChunk, name, id, chunkId)

  CinematicGameObjDef.CHUNK_ID ->
      parseCinematicGameObjDef(objDataChunk, name, id, chunkId)
  ```
- Remove the `// FIXME: C4GameObjDef is a data class` etc. comments

**`server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt`**
- Remove `C4GameObjDef`, `SimpleGameObjDef`, `CinematicGameObjDef` from the existing
  `data class definitions fall through` test (it currently uses chunkId 0x3001, so
  it's unaffected — no change needed)
- Add a new test: `C4GameObjDef and SimpleGameObjDef dispatch to typed subclasses`
  verifying both `as? C4GameObjDef` and `as? SimpleGameObjDef` return non-null

### Task 2 — Four defs in GlobalSettingsDefs.kt

**`server/src/main/kotlin/ccr/server/defs/GlobalSettingsDefs.kt`**

Convert these four data classes (same pattern as above):
- `GlobalSettingsGeneralDef` (add `import ccr.server.defs.DefinitionClass`)
- `HumanLoiterGlobalSettingsDef`
- `HUDGlobalSettingsDef`
- `HumanAnimOverrideDef`

Note: `HUDGlobalSettingsDef` is a large class but the change is mechanical —
only the class declaration line changes, not any of the fields.

Parser functions (`parseGlobalSettingsGeneralDef`, `parseHumanLoiterGlobalSettingsDef`,
`parseHUDGlobalSettingsDef`, `parseHumanAnimOverrideDef`) already return the correct
type; no changes needed to them.

### Task 3 — Four remaining defs

**`server/src/main/kotlin/ccr/server/defs/EvaSettingsDefClass.kt`**
- Convert `data class EvaSettingsDefClass` → `class EvaSettingsDefClass : DefinitionClass`
- Add `import ccr.server.defs.DefinitionClass`
- `parseEvaSettingsDefClass()` unchanged

**`server/src/main/kotlin/ccr/server/defs/combat/CharacterClassSettingsDefClass.kt`**
- Convert `data class CharacterClassSettingsDefClass` → `class CharacterClassSettingsDefClass : DefinitionClass`
- Add `import ccr.server.defs.DefinitionClass`
- `parseCharacterClassSettingsDefClass()` unchanged

**`server/src/main/kotlin/ccr/server/defs/PurchaseSettingsDefs.kt`**
- Convert `data class TeamPurchaseSettingsDefClass` → `class TeamPurchaseSettingsDefClass : DefinitionClass`
- The class has `id` before `name` in the constructor — keep that order;
  just change to `id: UInt, name: String` (no `val`) and add `: DefinitionClass(name, id, CHUNK_ID)`
  (note: uses `CHUNK_ID` not `chunkId` as parameter — same as `PurchaseSettingsDefClass`)
- No change needed to the `load()` companion function body

**`server/src/main/kotlin/ccr/server/defs/CNCModeSettingsDef.kt`**
- Convert `data class CNCModeSettingsDef` → `class CNCModeSettingsDef : DefinitionClass`
- Add `import ccr.server.defs.DefinitionClass`
- `parseCNCModeSettingsDef()` unchanged

### Task 4 — Wire up FullDefinitionLoader + tests

**`server/src/main/kotlin/ccr/server/level/FullDefinitionLoader.kt`**

The current multi-value global-settings fallback block (lines 237–246):
```kotlin
// FIXME: remaining are data classes — convert to DefinitionClass subclasses and parse properly
0x00040600u,                                    // CHUNKID_GLOBAL_SETTINGS_DEF (wrapper)
HumanLoiterGlobalSettingsDef.CHUNK_ID,          // 0x40601
GlobalSettingsGeneralDef.CHUNK_ID,              // 0x40602
HUDGlobalSettingsDef.CHUNK_ID,                  // 0x40603
EvaSettingsDefClass.CHUNK_ID,                   // 0x40604
CharacterClassSettingsDefClass.CHUNK_ID,        // 0x40605
HumanAnimOverrideDef.CHUNK_ID,                  // 0x40606
TeamPurchaseSettingsDefClass.CHUNK_ID,          // 0x40608
CNCModeSettingsDef.CHUNK_ID,                    // 0x40609
-> fallback
```

Replace with individual dispatch cases:
```kotlin
0x00040600u -> fallback                         // CHUNKID_GLOBAL_SETTINGS_DEF (wrapper only)

HumanLoiterGlobalSettingsDef.CHUNK_ID ->
    parseHumanLoiterGlobalSettingsDef(objDataChunk, name, id, chunkId) ?: fallback

GlobalSettingsGeneralDef.CHUNK_ID ->
    parseGlobalSettingsGeneralDef(objDataChunk, name, id, chunkId) ?: fallback

HUDGlobalSettingsDef.CHUNK_ID ->
    parseHUDGlobalSettingsDef(objDataChunk, name, id, chunkId) ?: fallback

EvaSettingsDefClass.CHUNK_ID ->
    parseEvaSettingsDefClass(objDataChunk, name, id, chunkId) ?: fallback

CharacterClassSettingsDefClass.CHUNK_ID ->
    parseCharacterClassSettingsDefClass(objDataChunk, name, id, chunkId) ?: fallback

HumanAnimOverrideDef.CHUNK_ID ->
    parseHumanAnimOverrideDef(objDataChunk, name, id, chunkId) ?: fallback

TeamPurchaseSettingsDefClass.CHUNK_ID ->
    TeamPurchaseSettingsDefClass.load(objDataChunk) ?: fallback

CNCModeSettingsDef.CHUNK_ID ->
    parseCNCModeSettingsDef(objDataChunk, name, id, chunkId) ?: fallback
```

Also add the needed imports at the top of `FullDefinitionLoader.kt`:
- `import ccr.server.defs.combat.C4GameObjDef` (and its `parseC4GameObjDef`)
- `import ccr.server.defs.combat.SimpleGameObjDef` (and its `parseSimpleGameObjDef`)
- `import ccr.server.defs.combat.CinematicGameObjDef` (and its `parseCinematicGameObjDef`)
- `import ccr.server.defs.combat.CharacterClassSettingsDefClass` (and its parser)
- `import ccr.server.defs.EvaSettingsDefClass` (and its `parseEvaSettingsDefClass`)
- `import ccr.server.defs.CNCModeSettingsDef` (and its `parseCNCModeSettingsDef`)
- Other defs already imported via `import ccr.server.defs.*`

**`server/src/test/kotlin/ccr/server/level/FullDefinitionLoaderTest.kt`**

Add two new tests:

1. `global settings defs dispatch to typed subclasses` — builds a DDB with
   `GlobalSettingsGeneralDef.CHUNK_ID` (0x00040602u) and verifies:
   ```kotlin
   assertTrue(def is GlobalSettingsGeneralDef)
   ```

2. `C4GameObjDef dispatches to typed subclass` — builds a DDB with
   `C4GameObjDef.CHUNK_ID` (0x00040103u) and verifies:
   ```kotlin
   assertTrue(def is C4GameObjDef)
   ```

## Verification

```
kotlin-server/gradlew -p kotlin-server test
```

All tests pass (17 existing + 2 new = 19).

## Notes

- `0x00040600u` (the wrapper chunk) has no def class — stays as `fallback`
- `TeamPurchaseSettingsDefClass` keeps using its own `load()` which re-reads name/id
  from the inner PARENT chunk (same pattern as `PurchaseSettingsDefClass`)
- No `data` class features (copy, componentN, structural equality) are used anywhere
  outside these files, so removing `data` is safe
