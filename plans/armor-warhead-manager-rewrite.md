# Plan: Rewrite ArmorWarheadManager to Match C++ Exactly

## Context

`Network.initEncoders` sets `BITPACK_SHIELD_TYPE` precision from `ArmorWarheadManager.numArmorTypes`, but the current Kotlin `ArmorWarheadManager.init()` is fundamentally broken: it reads INI **key names** (`"Armor00"`) instead of **values** (`"None"`), so `Scale_<ArmorName>` section lookups fail, the multiplier table is all 1.0, and `numArmorTypes` may be wrong. The expected value after fix is **31 armor types**.

Additionally, ArmorWarheadManager is missing most of its C++ functionality (absorption table, soft armor, special damage system), and a duplicate stub object in CombatTypeStubs.kt creates confusion.

## Approach

Worktree: `.worktrees/armor-warhead` (branch: `feature/armor-warhead`)

---

## Task 1: Rewrite `ArmorWarheadManager.kt` — Full C++ Port

**File:** `kotlin-server/server/src/main/kotlin/ccr/server/combat/ArmorWarheadManager.kt`
**C++ ref:** `original/Code/Combat/damage.cpp` + `damage.h`

### Critical bug fix
Change name loading from `ini.getEntry(section, i)` (returns key) to `ini.getString(section, ini.getEntry(section, i), "")` (returns value). This mirrors the C++ pattern: `Get_String(section, Get_Entry(section, i), "", buf, sizeof(buf))`.

### Constants to add (move from CombatTypeStubs.kt)
```
SPECIAL_DAMAGE_TYPE_NONE = 0, FIRE = 1, CHEM = 2, ELECTRIC = 3, CNC_FIRE = 4, CNC_CHEM = 5, SUPER_FIRE = 6
NUM_SPECIAL_DAMAGE_TYPES = 7
```

### Internal state to add
- `armorNames: List<String>`, `warheadNames: List<String>` — names from values
- `armorSaveIDs: IntArray`, `warheadSaveIDs: IntArray` — parallel arrays, index=type, value=saveID
- `softArmorTable: BooleanArray` — from `[Soft_Armor]` section
- `absorption: FloatArray` — from `[Shield_<ArmorName>]` sections (armor×warhead flat array)
- `specialDamageTypes: IntArray`, `specialDamageProbability: FloatArray`, `visceroidProbability: FloatArray` — per warhead
- `specialDamageWarhead/Duration/Scale: IntArray/FloatArray/FloatArray`, `specialDamageExplosion: Array<String>` — per special damage type
- `imperviousSkins: Array<MutableList<Int>>` — per special damage type

### `init(bytes)` parsing sequence (mirrors `damage.cpp:68-254`)
1. `shutdown()` to clear
2. Load armor names from `[Armor_Types]` values; init `armorSaveIDs[i] = i`, `softArmorTable[i] = false`
3. Load warhead names from `[Warhead_Types]` values; init `warheadSaveIDs[i] = i`
4. Load `[Armor_Save_IDs]` — `armorSaveIDs[i] = ini.getInt("Armor_Save_IDs", armorNames[i], i)`
5. Load `[Warhead_Save_IDs]` — same pattern
6. Load `[Soft_Armor]` — for each entry, read value, `getArmorType(value)`, set `softArmorTable[type] = true`
7. Load `[Scale_<armorName>]` — `multipliers[ai * nw + wi] = ini.getFloat("Scale_$name", warheadName, 1.0f)`
8. Load `[Shield_<armorName>]` — `absorption[ai * nw + wi] = ini.getFloat("Shield_$name", warheadName, 0.0f)`
9. Load per-warhead special damage: `[Special_Damage_Type]`, `[Special_Damage_Probability]`, `[Visceroid_Probability]` sections
10. Load per-type special damage: `[Special_Damage_Fire]`, `[Special_Damage_Chem]`, etc. — keys: `Warhead`, `Duration`, `Scale`, `Explosion`
11. Copy FIRE→SUPER_FIRE
12. Load impervious skins: `[Impervious_Fire]`, etc.

### Public API to add (matching C++ names)
- `numWarheadTypes: Int`
- `getArmorType(name: String): Int` — case-insensitive search, 0 if not found
- `getWarheadType(name: String): Int`
- `getArmorName(type: Int): String`
- `getWarheadName(type: Int): String`
- `getArmorSaveID(type: Int): Int`
- `findArmorSaveID(id: Int): Int` — linear search, 0 if not found
- `getWarheadSaveID(type: Int): Int`
- `findWarheadSaveID(id: Int): Int`
- `getDamageMultiplier(armor: Int, warhead: Int): Float` — takes TYPE indices (C++ order: armor first)
- `getShieldAbsorption(armor: Int, warhead: Int): Float`
- `isArmorSoft(armor: Int): Boolean`
- `getSpecialDamageType(warhead: Int): Int`
- `getSpecialDamageProbability(warhead: Int): Float`
- `getSpecialDamageWarhead(type: Int): Int`
- `getSpecialDamageDuration(type: Int): Float`
- `getSpecialDamageScale(type: Int): Float`
- `getSpecialDamageExplosion(type: Int): String`
- `getVisceroidProbability(warhead: Int): Float`
- `isSkinImpervious(type: Int, skin: Int): Boolean`
- `shutdown()`

### Keep backward-compatible save-ID convenience methods
- `scaleDamage(rawDamage, warheadSaveId, armorSaveId)` — converts via `findWarheadSaveID`/`findArmorSaveID` then calls `getDamageMultiplier`
- `getDamageMultiplierBySaveId(warheadSaveId, armorSaveId)` — same conversion

---

## Task 2: Remove Stub from CombatTypeStubs.kt + Fix Imports

**File:** `kotlin-server/server/src/main/kotlin/ccr/server/net/CombatTypeStubs.kt`

### Remove
- `object ArmorWarheadManager { ... }` stub block
- Extension funs `getSpecialDamageExplosion`, `getSpecialDamageScale` on ArmorWarheadManager
- `val ArmorWarheadManager.SPECIAL_DAMAGE_TYPE_SUPER_FIRE`
- Extension funs `getSpecialDamageType`, `getSpecialDamageProbability`, `isSkinImpervious` on ArmorWarheadManager
- `fun DefenseObjectClass.getSkin(): Int = 0`
- `fun OffenseObjectClass.getWarhead(): Int = 0`

### Fix imports — add `import ccr.server.combat.ArmorWarheadManager` to:
- `SoldierGameObj.kt`
- `MendozaBossGameObj.kt`

### Fix stub accessor calls in SoldierGameObj.kt
- `damager.getWarhead()` → `damager.warhead`
- `defenseObject.getSkin()` → `defenseObject.skin`

---

## Task 3: Minor DefenseObjectClass Fix

**File:** `kotlin-server/server/src/main/kotlin/ccr/server/net/DefenseObjectClass.kt`

- `isSoft()` — change from `return false` to use `ArmorWarheadManager.isArmorSoft(skin)` + shield check
- Remove diagnostic `System.err.println` lines in `export()`

---

## Task 4: Update Tests

**File:** `kotlin-server/server/src/test/kotlin/ccr/server/combat/ArmorWarheadManagerTest.kt`

- Keep existing test INI fixture but add `[Soft_Armor]`, `[Shield_Flesh]`, `[Shield_Metal]`, special damage sections
- Update method calls to new API
- Add tests for all new methods

---

## Task 5: Build + Run Tests

```bash
kotlin-server/gradlew -p kotlin-server test
```

Verify `numArmorTypes == 31` from real `armor.ini`.

---

## Task Dependencies

```
Task 1 (Rewrite ArmorWarheadManager) ──┐
Task 2 (Remove stubs, fix imports)   ──┼── Task 4 (Tests) ── Task 5 (Build)
Task 3 (DefenseObjectClass.isSoft)   ──┘
```

## Worktree

`.worktrees/armor-warhead` (branch: `feature/armor-warhead`)
