# Audit: NetworkObject & DefinitionClass Subclasses — C++ vs Kotlin

**Date**: 2026-02-27
**Scope**: Every subclass of `NetworkObjectClass` and `DefinitionClass` in the C++ original compared field-by-field against the Kotlin server reimplementation.

---

## Summary

| Category | Checked | Issues Found |
|----------|---------|-------------|
| Game object hierarchy (export/import) | 18 classes | 0 wire-format bugs |
| Standalone NetworkObjects | 12 classes | 0 wire-format bugs |
| S→C NetEvent classes | 14 classes | 0 missing (ResetWins was never implemented in C++ either) |
| C→S NetEvent handlers | 21 classes | 7 unhandled (4 debug, 3 medium) |
| Definition classes (field loading) | 47+ classes | 2 real issues, 2 minor |
| **Total** | **~110 classes** | **2 real bugs, 7 unhandled events, 2 minor notes** |

---

## Real Issues

### 1. SakuraBossGameObjDef — `rocketsDefense` not loaded from chunk

- **Kotlin file**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/SakuraBossGameObjDef.kt:66`
- **C++ file**: `original-untouched/Code/Combat/sakurabossgameobj.cpp`
- **Problem**: C++ loads a full `DefenseObjectDefClass` from `CHUNKID_DEF_ROCKET_DEFENSEOBJ_DEF` (0x0907045A). Kotlin always creates an empty `DefenseObjectDefClass()` instead of parsing the chunk data.
- **Impact**: Sakura boss rocket defense stats (health, armor type, shield) will use defaults instead of level-defined values.

### 2. SakuraBossGameObjDef — wrong parent class

- **Kotlin file**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/SakuraBossGameObjDef.kt:28`
- **C++ hierarchy**: `SakuraBossGameObjDef` → `VehicleGameObjDef` → `SmartGameObjDef` → `ArmedGameObjDef` → `PhysicalGameObjDef` → `DamageableGameObjDef` → `ScriptableGameObjDef` → `BaseGameObjDef` → `DefinitionClass`
- **Kotlin hierarchy**: `SakuraBossGameObjDef` → `BaseGameObjDef` → `DefinitionClass`
- **Impact**: All intermediate hierarchy fields (defense object, physics def ID, weapon def IDs, sight range, vehicle type, etc.) are lost. The definition data class does load these fields separately via its own `load()`, but the `DefinitionRegistry` wrapper only exposes `BaseGameObjDef`.

---

## Medium Priority — Unhandled C→S Events

These C→S events have Kotlin class definitions but **no handler** in `GameServer.kt`'s `when (networkClassId)` dispatch. They produce an `"[GAME] unhandled networkClassId=..."` log line.

| Wire ID | C++ Name | Kotlin Class | C++ `Act()` Behavior | Priority |
|---------|----------|-------------|---------------------|----------|
| 1022 | `cMoneyEvent` | `MoneyEvent.kt` | Sets player money (requires god mode flag) | LOW — debug |
| 1028 | `cGodModeEvent` | `GodModeEvent.kt` | Toggles invulnerability (`#ifdef WWDEBUG` + password CRC) | LOW — debug |
| 1029 | `cVipModeEvent` | `VipModeEvent.kt` | Toggles damage scale (`#ifdef WWDEBUG` + password CRC) | LOW — debug |
| 1030 | `cScoreEvent` | `ScoreEvent.kt` | Increments player score (requires god mode) | LOW — debug |
| 1031 | `cClientBboEvent` | `ClientBboEvent.kt` | Sets client bandwidth cap (`rhost->Set_Maximum_Bps`) | **MEDIUM** — bandwidth |
| 1036 | `cCsConsoleCommandEvent` | `CsConsoleCommandEvent.kt` | Parses console command string via `ConsoleFunctionManager` | **MEDIUM** — remote console |
| 1037 | `cCsHint` | `CsHint.kt` | Increments `ClientHintCount` on a NetworkObject | LOW — hint tracking |

---

## Minor Notes

### 3. MendozaBossGameObjDefClass — flattened hierarchy

- **Kotlin file**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/MendozaBossGameObjDef.kt`
- **C++ parent**: `SoldierGameObjDef`; **Kotlin parent**: `DefinitionClass`
- **Impact**: None — C++ `MendozaBossGameObjDefClass::Save_Variables` is empty (no own persisted fields). The `SoldierGameObjDefWrapper` in the registry loads all soldier fields separately.

### 4. RaveshawBossGameObjDefClass — flattened hierarchy

- Same situation as Mendoza. No own persisted fields, so the hierarchy flattening has no field loss.

---

## Missing Game Objects (not yet needed)

### DamageZoneGameObj

- **C++ file**: `original-untouched/Code/Combat/damagezone.h`
- **Inherits**: `DamageableGameObj` (via `ScriptableGameObj`)
- **Export overrides**: None (pure inheritance)
- **Status**: No Kotlin class exists. This is a level-local environmental damage zone. The `DamageZoneGameObjDef` *does* exist in Kotlin for loading. Only the runtime game object is missing.
- **Impact**: LOW — environmental damage zones won't tick damage on players standing in them.

---

## Functional Simplifications (intentional)

### ClientControl.importFrequent — flush instead of parsing

- **C++ file**: `original-untouched/Code/Combat/clientcontrol.cpp` — parses ~20 fields (position, humanState, velocity, targeting, analog controls, etc.) via `Import_Control_Cs` + `Import_State_Cs`
- **Kotlin file**: `kotlin-server/server/src/main/kotlin/ccr/server/net/ClientControl.kt` — reads `smartObjId`, then calls `packet.flush()` to discard remaining data
- **Reason**: The Kotlin server handles client state through the unreliable frequent update path parsed directly in `GameServer.kt`, not through `CClientControl`'s reliable channel.
- **Impact**: None currently. If the server ever needs the reliable control data (e.g., anti-cheat validation), these fields would need to be parsed.

---

## Verified Correct (no issues)

### NetworkObject Hierarchy — Wire Formats Match Perfectly

All export/import methods verified field-by-field (types, order, bit-packing):

| Class | Creation | Rare | Occasional | Frequent | Status |
|-------|----------|------|-----------|----------|--------|
| BaseGameObj | — | — | — | — | OK |
| ScriptableGameObj | observers | — | — | — | OK |
| DamageableGameObj | — | — | defense+playerType | — | OK |
| PhysicalGameObj | defId+pos+facing | model+anim+radar | — | onHostBone | OK |
| ArmedGameObj | — | — | — | targeting | OK |
| SmartGameObj | controlOwner | — | — | controlState | OK |
| SoldierGameObj | all | defId | weaponList | state+pos+anim | OK |
| VehicleGameObj | lockOwner | seats+delivered | — | rounds+phys+driver | OK |
| BuildingGameObj | defId+pos+sphere | destroyed+power+state | — | — | OK |
| PowerPlantGameObj | — | — | — | — | OK |
| RefineryGameObj | — | harvesterDocked | — | — | OK |
| ComCenterGameObj | — | — | — | — | OK |
| SoldierFactoryGameObj | — | — | — | — | OK |
| VehicleFactoryGameObj | — | isBusy | — | — | OK |
| WarFactoryGameObj | — | — | — | — | OK |
| AirStripGameObj | — | — | — | — | OK |
| RepairBayGameObj | repairZone | — | — | — | OK |
| SimpleGameObj | — | — | — | — | OK |
| C4GameObj | — | timer+owner+mode | — | — | OK |
| BeaconGameObj | — | owner+state+timer | — | — | OK |
| PowerUpGameObj | — | — | — | — | OK |
| SpecialEffectsGameObj | — | — | — | — | OK |
| CinematicGameObj | — | — | — | — | OK |
| SAMSiteGameObj | — | — | — | — | OK |
| MendozaBossGameObj | — | — | — | — | OK |
| RaveshawBossGameObj | — | — | — | — | OK |
| SakuraBossGameObj | — | — | — | — | OK |
| Team | number | kills+deaths | score | — | OK |
| Player | name | id+ladder+team+flags | score+money+k/d | — | OK |
| ServerFps | — | — | — | fps | OK |
| BaseControllerClass | — | — | all flags | — | OK |
| ClientControl | clientId | — | — | smartObj+flush | OK |
| ClientFps | clientId | — | — | fps | OK |
| StaticNetworkObject | — | anim fields | — | — | OK |
| DoorNetworkObject | — | doorState | — | — | OK |
| ElevatorNetworkObject | — | state+doors | — | — | OK |
| DsapoNetworkObject | — | health+shield | — | — | OK |
| BackgroundMgr | — | 8 params + overrides | — | — | OK |
| WeatherMgr | — | 6 params + overrides | — | — | OK |

### S→C NetEvent Classes — All Present

| ID | Event | Status |
|----|-------|--------|
| 1001 | ScTextObj | OK |
| 1002 | PlayerKill | OK |
| 1003 | WinEvent | OK |
| 1004 | PurchaseResponseEvent | OK |
| 1005 | ConsoleCommandEvent | OK |
| 1006 | ResetWinsEvent | N/A (unimplemented in C++ too) |
| 1007 | SvrGoodbyeEvent | OK |
| 1008 | GameOptionsEvent | OK |
| 1009 | EvictionEvent | OK |
| 1012 | GameDataUpdateEvent | OK |
| 1013 | ScPingResponseEvent | OK |
| 1014 | ScExplosionEvent | OK |
| 1015 | ScObeliskEvent | OK |
| 1016 | SCAnnouncement | OK |

### C→S NetEvent Handlers — Implemented

| ID | Event | Status |
|----|-------|--------|
| 1018 | CsTextObj | Handled |
| 1019 | SuicideEvent | Handled |
| 1020 | ChangeTeamEvent | Handled |
| 1023 | WarpEvent | Handled |
| 1024 | PurchaseRequestEvent | Handled |
| 1025 | ClientGoodbyeEvent | Handled |
| 1026 | BioEvent | Handled |
| 1027 | LoadingEvent | Handled |
| 1033 | CsPingRequestEvent | Handled |
| 1034 | CsDamageEvent | Handled |
| 1035 | RequestKillEvent | Handled |
| 1038 | CSAnnouncement | Handled |
| 1039 | DonateEvent | Handled |

### Definition Classes — All Verified Correct

All 47+ definition classes verified with matching fields, types, and micro-chunk IDs:

- **Abstract hierarchy**: BaseGameObjDef, ScriptableGameObjDef, DamageableGameObjDef, PhysicalGameObjDef, ArmedGameObjDef, SmartGameObjDef — all field-complete
- **Game object defs**: SoldierGameObjDef, VehicleGameObjDef, SimpleGameObjDef, C4GameObjDef, BeaconGameObjDef, PowerUpGameObjDef, CinematicGameObjDef, SpecialEffectsGameObjDef, TransitionGameObjDef, ScriptZoneGameObjDef, DamageZoneGameObjDef, SAMSiteGameObjDef — all correct
- **Building defs**: BuildingGameObjDef, PowerPlantGameObjDef, SoldierFactoryGameObjDef, VehicleFactoryGameObjDef, AirStripGameObjDef, WarFactoryGameObjDef, RefineryGameObjDef, ComCenterGameObjDef, RepairBayGameObjDef — all correct
- **Munitions defs**: WeaponDefinitionClass, AmmoDefinitionClass, ExplosionDefinitionClass — all correct (50+ fields each verified)
- **Global settings**: GlobalSettingsGeneralDef, HumanLoiterGlobalSettingsDef, HUDGlobalSettingsDef, HumanAnimOverrideDef, EvaSettingsDefClass, CharacterClassSettingsDefClass, PurchaseSettingsDefClass, TeamPurchaseSettingsDefClass, CNCModeSettingsDef — all correct
- **Other**: SpawnerDefClass, AudibleSoundDefinitionClass — all correct
- **Physics defs**: Full hierarchy (StaticPhys, DynamicPhys, Moveable, Phys3, HumanPhys, RigidBody, VehiclePhys, Projectile, DecorationPhys, etc.) — all correct
