# Plan: Fix All C++ vs Kotlin Wire Format Discrepancies

## Context

Client still freezes. All prior fixes (physics definitions, decoder, modelName empty, rounds, creationDirtyBit) are done and committed. The latest proxy log comparison with the working decoder reveals the remaining discrepancies between C++ and Kotlin wire format. The most critical is vehicle `modelName`: Kotlin sends the full PhysDefClass path (e.g. `"vehicles\nod turret\v_nod_turret.w3d"`) while C++ sends the W3D HLod base name (`"V_NOD_TURRET"`) via `Peek_Physical_Object()->Peek_Model()->Get_Name()`. The client may fail to find models by full path, or may behave incorrectly when comparing model names case-insensitively.

## Confirmed Discrepancies

### 1. Vehicle modelName — wrong format
**File:** `kotlin-server/server/src/main/kotlin/ccr/server/God.kt`
**Function:** `resolveModelName(def: VehicleGameObjDef?)`

C++ writes: `Peek_Physical_Object()->Peek_Model()->Get_Name()` → short W3D HLod chunk name, e.g. `"V_NOD_TURRET"`
Kotlin writes: `physDef.modelName` directly → full path from DDB, e.g. `"vehicles\nod turret\v_nod_turret.w3d"`

**Fix:** After retrieving `physDef.modelName`, extract just the base name:
```kotlin
val raw = physDef.modelName  // e.g. "vehicles\nod turret\v_nod_turret.w3d"
raw.substringAfterLast('\\').substringAfterLast('/').substringBeforeLast('.').uppercase()
// → "V_NOD_TURRET"
```

Soldiers are unaffected — `resolveSoldierModelName` already returns short names like `"c_ag_nod_mg"`.

### 2. Check animMode default in PhysicalGameObj
**File:** `kotlin-server/server/src/main/kotlin/ccr/server/net/PhysicalGameObj.kt`

Decoded Kotlin turret shows `mode=3`. C++ with no AnimControl writes `anim_mode=0`. Check the Kotlin `animMode` field default — if it's not 0, set it to 0.

### 3. Check all other Export_Rare field defaults
For a level vehicle with no AnimControl, C++ Export_Rare writes:
- `model_name` = W3D base name (fix above)
- `anim_name` = "" (empty — correct in Kotlin)
- `curr_frame` = 0
- `target_frame` = 0
- `anim_mode` = 0
- `host_id` = 0, `host_bone` = 0
- `player_type` = team (from definition)
- `hud_pokable` = false
- `hidden` = false (for vehicles)
- seats × occupant = -1 each
- `vehicle_delivered` = false

Verify each field matches what Kotlin sends. Fix any that differ.

### 4. Check Export_Occasional (DamageableGameObj) fields
Decoded Kotlin turret shows `hp=300 shield=0 type=15`. Verify these match what C++ sends for Nod_Turret_MP.

### 5. Verify Export_Frequent for TURRET type
Kotlin turret uses VEHICLE_TYPE_TURRET — no physics block (no position/quaternion). Export_Frequent writes:
- `totalRounds` = -1 (unlimited, fixed)
- `driverIsGunner` = true/false (check definition value)
- SmartGameObj::exportFrequent (targeting + controls)

Verify the full frequent payload size matches C++ for TURRET type.

## Files to Modify

- `kotlin-server/server/src/main/kotlin/ccr/server/God.kt` — `resolveModelName()` fix (primary)
- `kotlin-server/server/src/main/kotlin/ccr/server/net/PhysicalGameObj.kt` — check animMode default
- Possibly `VehicleGameObj.kt` — check driverIsGunner default

## Implementation Order

1. Fix `resolveModelName` in `God.kt` — extract base name from PhysDefClass path
2. Check `animMode` default in `PhysicalGameObj.kt` — set to 0 if wrong
3. Verify all other Export_Rare defaults against C++ (check each field)
4. Run all tests
5. Capture new proxy log to verify model name is now short
6. Commit

## Verification

1. `kotlin-server/gradlew -p kotlin-server :server:test` — all pass
2. `ProxyComparisonTest.dump kotlin server traffic` — turret model name shows `"V_NOD_TURRET"` not the full path
3. Connect client — verify freeze is resolved
