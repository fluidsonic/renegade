# Audit: NetworkObject & DefinitionClass Subclasses — C++ vs Kotlin

**Date**: 2026-02-27
**Branch**: `feature/networkobj-subclasses`
**Build status**: 0 compile errors, 683 tests passing

---

## 1. Coverage Summary

### NetworkObject Subclasses (37 total in Kotlin)

All C++ `NetworkObjectClass` subclasses that are relevant to the multiplayer server have corresponding Kotlin implementations:

| Category | Classes | Status |
|----------|---------|--------|
| **Abstract hierarchy** | NetworkObjectClass, BaseGameObj, ScriptableGameObj, DamageableGameObj, PhysicalGameObj, ArmedGameObj, SmartGameObj | All exist |
| **Soldiers** | SoldierGameObj | Exists |
| **Vehicles** | VehicleGameObj | Exists |
| **Buildings** | BuildingGameObj, PowerPlantGameObj, RefineryGameObj, RepairBayGameObj, ComCenterGameObj, AirStripGameObj, WarFactoryGameObj, VehicleFactoryGameObj | All exist |
| **Simple objects** | SimpleGameObj, PowerUpGameObj, C4GameObj, BeaconGameObj | All exist |
| **Zones/effects** | ScriptZoneGameObj, DamageZoneGameObj, SpecialEffectsGameObj, CinematicGameObj | All exist |
| **Bosses** | MendozaBossGameObj, RaveshawBossGameObj, SakuraBossGameObj | All exist (stubs) |
| **Special** | SAMSiteGameObj, TransitionGameObj | All exist |
| **Net events** | Various (cScTextObj, EVA, WallOfText, etc.) | All exist |
| **Static network objects** | BackgroundMgr, WeatherMgr | All exist |
| **Singletons** | BaseControllerClass, ServerFps, Player | All exist |

**Missing from Kotlin (intentionally excluded — singleplayer/client-only)**:
- `Intelli`/`IntelliFaction` (AI-only)
- `BuildingAggregateClass` (client rendering)

### DefinitionClass Subclasses (68 total in Kotlin)

All gameplay-relevant definition classes have Kotlin counterparts. Hierarchy:

```
DefinitionClass
├── BaseGameObjDef (Kotlin-specific base)
│   ├── ScriptableGameObjDef
│   │   ├── DamageableGameObjDef
│   │   │   ├── BuildingGameObjDef (+ subtypes for each building)
│   │   │   ├── VehicleGameObjDef (via wrapper)
│   │   │   ├── SoldierGameObjDef (via wrapper)
│   │   │   └── PhysicalGameObjDef
│   │   │       ├── SimpleGameObjDef
│   │   │       │   ├── PowerUpGameObjDef
│   │   │       │   └── C4GameObjDef
│   │   │       ├── BeaconGameObjDef
│   │   │       ├── CinematicGameObjDef
│   │   │       ├── SAMSiteGameObjDef
│   │   │       ├── DamageZoneGameObjDef
│   │   │       └── SpecialEffectsGameObjDef
│   │   └── ScriptZoneGameObjDef ← BUG: should be here but currently extends BaseGameObjDef
│   └── TransitionGameObjDef
├── AmmoDefinitionClass
├── WeaponDefinitionClass
├── ExplosionDefinitionClass
├── SpawnerDefClass
├── CharacterClassSettingsDefClass
└── (phys defs, boss defs, etc.)
```

---

## 2. Discrepancies by Severity

### CRITICAL — Wire Format / Runtime Behavior

These directly affect client compatibility or core gameplay logic.

#### 2.1 SmartGameObj.controlOwner default value
- **C++**: `SmartGameObj::SmartGameObj()` sets `ControlOwner = SERVER_CONTROL_OWNER` (value `-99999`)
- **Kotlin**: `controlOwner = 0`
- **Impact**: Server-controlled objects (buildings, AI) may be misidentified. `controlOwner == 0` means "no owner" but C++ interprets 0 as a valid host ID. Wire format sends this value in `exportFrequent`.
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/SmartGameObj.kt`
- **Fix**: Change default to `SERVER_CONTROL_OWNER` (-99999)

#### 2.2 ScriptZoneGameObjDef hierarchy
- **C++**: `ScriptZoneGameObjDef` extends `ScriptableGameObjDef` → inherits script name/parameter lists
- **Kotlin**: Extends `BaseGameObjDef` directly — missing `ScriptableGameObjDef` in chain
- **Impact**: Script zones cannot have scripts attached — breaks zone trigger scripting
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/ScriptZoneGameObjDef.kt`
- **Fix**: Change to extend `ScriptableGameObjDef`

#### 2.3 CinematicGameObj missing appPacketType
- **C++**: Constructor sets `Set_App_Packet_Type(APPPACKETTYPE_CINEMATIC)` which affects how the object is networked
- **Kotlin**: No `appPacketType` assignment in constructor
- **Impact**: Cinematic objects may use wrong packet type for replication
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/CinematicGameObj.kt`
- **Fix**: Add `appPacketType = APPPACKETTYPE_CINEMATIC` in init

#### 2.4 CinematicGameObj missing importRare
- **C++**: `CinematicGameObj::Import_Rare()` reads `AnimationName` string from bitstream, calls `Set_Animation(name, loop, blended)` — client needs this to play cutscene animations
- **Kotlin**: No `importRare()` override — client would never get animation data
- **Impact**: Cinematic animations won't play for clients
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/CinematicGameObj.kt`
- **Fix**: Add `importRare()`/`exportRare()` pair matching C++ wire format

#### 2.5 SoldierGameObj.exportFrequent has_weapon check
- **C++**: `has_weapon = (WeaponBag && WeaponBag->Get_Index())` — checks if weapon bag exists and has a valid index
- **Kotlin**: `has_weapon = (currentWeapon != null && currentWeapon.definitionId != 0u)` — extra `definitionId != 0` guard
- **Impact**: Fists (definitionId=0) would be sent as `has_weapon=false`, skipping weapon state. C++ sends fists as a weapon with `defId=0`.
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/SoldierGameObj.kt`
- **Fix**: Remove `definitionId != 0u` guard — match C++ logic exactly

---

### MODERATE — Gameplay Logic Differences

These affect server behavior but not wire format directly.

#### 2.6 C4GameObjDef hierarchy
- **C++**: `C4GameObjDef` extends `SimpleGameObjDef` → `PhysicalGameObjDef` → `DamageableGameObjDef`
- **Kotlin**: `C4GameObjDef` extends `PhysicalGameObjDef` directly, skipping `SimpleGameObjDef`
- **Impact**: Missing `SimpleGameObjDef` fields (e.g., `isHidden` behavior, model/anim defaults)
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/C4GameObjDef.kt`
- **Fix**: Change to extend `SimpleGameObjDef`

#### 2.7 DamageableGameObjDef missing encyclopedia fields
- **C++**: `DamageableGameObjDef` has `EncyclopediaType` (int) and `EncyclopediaID` (int) fields, loaded from definition chunks
- **Kotlin**: These fields are missing
- **Impact**: Encyclopedia/info display won't work for damage objects
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/DamageableGameObjDef.kt`
- **Fix**: Add `encyclopediaType: Int = 0` and `encyclopediaId: Int = 0` constructor params

#### 2.8 CinematicGameObj missing think/postThink overrides
- **C++**: `CinematicGameObj::Think()` handles animation timing, triggers, sound sync; `Post_Think()` does cleanup; `Completely_Damaged()` handles destruction; `Takes_Explosion_Damage()` returns false
- **Kotlin**: None of these overrides exist — object is inert after creation
- **Impact**: Cinematics won't animate or respond to damage on server side
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/CinematicGameObj.kt`
- **Fix**: Port Think/PostThink/Completely_Damaged/Takes_Explosion_Damage from C++

#### 2.9 SmartGameObj missing GameObjManager.addSmart() registration
- **C++**: `SmartGameObj` constructor calls `GameObjManager::Add_Smart(this)` to register in a separate smart-object list used for iteration
- **Kotlin**: No `addSmart()` call — smart objects only in main gameObjList
- **Impact**: Any code iterating smart objects specifically would miss these
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/SmartGameObj.kt`
- **Fix**: Add `GameObjManager.addSmart(this)` or equivalent registration

#### 2.10 BaseGameObj.delete() is empty
- **C++**: `BaseGameObj::~BaseGameObj()` calls cleanup; `delete` triggers destructor chain
- **Kotlin**: `fun delete()` is empty — should call `destruct()` or equivalent cleanup
- **Impact**: Object cleanup (observer notifications, manager removal) may not fire
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/BaseGameObj.kt`
- **Fix**: Have `delete()` call the destruct/cleanup chain

#### 2.11 VehicleFactoryGameObj.requestVehicle always returns false
- **C++**: `VehicleFactoryGameObj::Request_Vehicle()` sets `IsBusy=true`, starts creation timer, returns true on success
- **Kotlin**: Method body has logic but `val result = false` is never reassigned (val not var)
- **Impact**: Vehicle purchases from VehicleFactory always fail
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/VehicleFactoryGameObj.kt`
- **Fix**: Change `val result = false` to `var result = false` and set `result = true` on success path

#### 2.12 BuildingGameObj missing aggregateDefId / MCT spawn
- **C++**: `BuildingGameObj` stores `AggregateDefID`, spawns MCT (MasterControlTerminal) as child `PhysicalGameObj` in `Init()`, tracks it for damage multiplier
- **Kotlin**: No aggregate def ID field, no MCT spawn logic
- **Impact**: Buildings won't have MCT weak points — changes balance significantly
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/BuildingGameObj.kt`
- **Fix**: Add `aggregateDefId` field and MCT creation in `init()`

#### 2.13 SoldierGameObj missing innate weapon setup
- **C++**: `SoldierGameObj::Init()` calls `Setup_Innate_Weapons()` → creates weapon bag from def's `WeaponDefID`/`SecondaryWeaponDefID`/`TertiaryWeaponDefID`
- **Kotlin**: Weapon bag exists but innate weapon population from definition is not implemented
- **Impact**: Spawned soldiers have no weapons until manually assigned
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/SoldierGameObj.kt`
- **Fix**: Port `Setup_Innate_Weapons()` from C++

#### 2.14 VehicleGameObj missing driver/passenger seat management
- **C++**: Full `SeatEntry`/`TransitionEntry` arrays, `Add_Occupant()`/`Remove_Occupant()` with bone attach, `Get_Driver()`, driver-is-gunner logic
- **Kotlin**: Basic `occupants` list and `driverSeat`/`gunnerSeat` indices exist, but seat management is minimal stubs
- **Impact**: Vehicle occupancy won't work correctly — enters/exits, seat switching, driver vs passenger distinction
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/VehicleGameObj.kt`
- **Fix**: Port full seat management from C++ `vehiclegameobj.cpp`

---

### LOW — Missing Features / Stubs (Not Blocking Core Gameplay)

#### 2.15 PowerPlantGameObj missing Save/Load
- **C++**: Has `Save()`/`Load()` for persistence
- **Kotlin**: No save/load — only matters for singleplayer campaign save games
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/PowerPlantGameObj.kt`

#### 2.16 ScriptZoneGameObj non-CheckStarsOnly physics query
- **C++**: `ScriptZoneGameObj::Think()` does a `PhysAABoxIntersectionTestClass` to find all objects in zone
- **Kotlin**: Has a `// FIXME` stub — only CheckStarsOnly (player-only) path is wired
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/ScriptZoneGameObj.kt`

#### 2.17 ArmedGameObj MuzzleRecoilController
- **C++**: `ArmedGameObj` has `MuzzleRecoilController` for visual muzzle bone animation
- **Kotlin**: Not ported — client-side visual only
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/ArmedGameObj.kt`

#### 2.18 PhysicalGameObj missing radar/stealth blip
- **C++**: `PhysicalGameObj` manages `RadarBlipShape`, stealth enable/disable, handles `STEALTH_BROKEN_FRACTION`
- **Kotlin**: Stealth fields exist as stubs but radar blip management is not wired
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/PhysicalGameObj.kt`

#### 2.19 SmartGameObj missing action/conversation systems
- **C++**: Manages `ActionQueue`, `ActiveConversation`, dialogue system
- **Kotlin**: Action stubs exist but conversation/dialogue not ported
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/SmartGameObj.kt`

#### 2.20 Building subtypes missing operational logic
- **Refinery**: Missing harvester spawn/tracking, trickle income timer
- **RepairBay**: Missing repair zone detection, heal-per-tick logic
- **ComCenter**: Missing radar enable/disable toggle
- **AirStrip/WarFactory/VehicleFactory**: Vehicle creation timers exist but delivery animation/path logic is stubbed
- **Files**: All under `kotlin-server/server/src/main/kotlin/ccr/server/net/`

#### 2.21 DamageableGameObj missing damage application
- **C++**: Full `Apply_Damage()` → `Apply_Damage_Extended()` pipeline with armor lookup, shield absorption, death trigger
- **Kotlin**: `applyDamage()` exists but is a simplified stub — no armor table lookup, no shield logic
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/DamageableGameObj.kt`

#### 2.22 Boss game objects are minimal stubs
- **MendozaBossGameObj**, **RaveshawBossGameObj**, **SakuraBossGameObj**: Exist but are mostly empty classes
- **C++**: Each has unique AI behavior, multi-phase fights, special attacks
- **Impact**: None for multiplayer — boss fights are singleplayer campaign only

---

## 3. Per-File Comparison Matrix

### Abstract Hierarchy Classes

| C++ File | Kotlin File | Match Quality | Notes |
|----------|-------------|---------------|-------|
| `basegameobj.h/cpp` | `BaseGameObj.kt` | 85% | Missing: delete() cleanup, observer notification loop |
| `scriptableobj.h/cpp` | `ScriptableGameObj.kt` | 80% | Missing: script attach/detach, Custom timer, observer forwarding |
| `damageableobj.h/cpp` | `DamageableGameObj.kt` | 75% | Missing: full Apply_Damage pipeline, encyclopedia fields |
| `physicalobj.h/cpp` | `PhysicalGameObj.kt` | 80% | Missing: radar blip, collision group setup, vis tracking |
| `armedobj.h/cpp` | `ArmedGameObj.kt` | 85% | Missing: muzzle recoil controller (client-only) |
| `smartobj.h/cpp` | `SmartGameObj.kt` | 70% | Missing: controlOwner default, addSmart registration, action system |

### Concrete Game Object Classes

| C++ File | Kotlin File | Match Quality | Notes |
|----------|-------------|---------------|-------|
| `soldiergameobj.h/cpp` | `SoldierGameObj.kt` | 80% | Missing: innate weapon setup, exportFrequent has_weapon bug |
| `vehiclegameobj.h/cpp` | `VehicleGameObj.kt` | 70% | Missing: full seat management, driver/turret logic |
| `buildinggameobj.h/cpp` | `BuildingGameObj.kt` | 75% | Missing: MCT spawn, aggregate def, power dependency |
| `powerplant.h/cpp` | `PowerPlantGameObj.kt` | 85% | Missing: save/load (SP-only) |
| `refinery.h/cpp` | `RefineryGameObj.kt` | 60% | Missing: harvester tracking, trickle income |
| `repairbay.h/cpp` | `RepairBayGameObj.kt` | 60% | Missing: repair zone healing |
| `comcenter.h/cpp` | `ComCenterGameObj.kt` | 70% | Missing: radar toggle |
| `airstrip.h/cpp` | `AirStripGameObj.kt` | 65% | Think exists but delivery path stubbed |
| `warfactory.h/cpp` | `WarFactoryGameObj.kt` | 65% | Think exists but delivery animation stubbed |
| `vehfactory.h/cpp` | `VehicleFactoryGameObj.kt` | 65% | requestVehicle always returns false (val bug) |
| `simplegameobj.h/cpp` | `SimpleGameObj.kt` | 90% | Good match |
| `powerupobj.h/cpp` | `PowerUpGameObj.kt` | 85% | Good match |
| `c4gameobj.h/cpp` | `C4GameObj.kt` | 80% | Def hierarchy wrong (extends PhysicalGameObjDef not SimpleGameObjDef) |
| `beacongameobj.h/cpp` | `BeaconGameObj.kt` | 85% | Good match |
| `samsite.h/cpp` | `SAMSiteGameObj.kt` | 80% | Targeting logic present |
| `cinematicgameobj.h/cpp` | `CinematicGameObj.kt` | 40% | Missing: importRare, think, appPacketType |
| `scriptzoneobj.h/cpp` | `ScriptZoneGameObj.kt` | 70% | Missing: full physics intersection query |
| `damagezone.h/cpp` | `DamageZoneGameObj.kt` | 85% | Good match |
| `specialeffects.h/cpp` | `SpecialEffectsGameObj.kt` | 85% | Good match |
| `transitiongameobj.h/cpp` | `TransitionGameObj.kt` | 75% | Exists, basic |

### Definition Classes

| C++ File | Kotlin File | Match Quality | Notes |
|----------|-------------|---------------|-------|
| `scriptableobj.h` (def) | `ScriptableGameObjDef.kt` | 90% | Good |
| `damageableobj.h` (def) | `DamageableGameObjDef.kt` | 80% | Missing encyclopedia fields |
| `physicalobj.h` (def) | `PhysicalGameObjDef.kt` | 85% | Good |
| `simplegameobj.h` (def) | `SimpleGameObjDef.kt` | 90% | Good |
| `soldiergameobjdef.h` | `SoldierGameObjDef.kt` + wrapper | 85% | Composition pattern differs from C++ inheritance |
| `vehiclegameobjdef.h` | `VehicleGameObjDef.kt` + wrapper | 80% | Same composition pattern |
| `buildinggameobjdef.h` | `BuildingGameObjDef.kt` | 85% | Good |
| `c4gameobjdef.h` | `C4GameObjDef.kt` | 75% | Wrong parent class |
| `beacongameobjdef.h` | `BeaconGameObjDef.kt` | 90% | Good |
| `powerupobj.h` (def) | `PowerUpGameObjDef.kt` | 85% | Good |
| `samsite.h` (def) | `SAMSiteGameObjDef.kt` | 85% | Good |
| `cinematicgameobj.h` (def) | `CinematicGameObjDef.kt` | 85% | Good |
| `scriptzoneobj.h` (def) | `ScriptZoneGameObjDef.kt` | 70% | Wrong parent (should extend ScriptableGameObjDef) |
| `weapondef.h` | `WeaponDefinitionClass.kt` | 85% | Good |
| `ammodef.h` | `AmmoDefinitionClass.kt` | 85% | Good |
| `explosiondef.h` | `ExplosionDefinitionClass.kt` | 85% | Good |

### Non-GameObj Network Classes

| C++ File | Kotlin File | Match Quality | Notes |
|----------|-------------|---------------|-------|
| `basecontrollerclass.h/cpp` | `BaseControllerClass.kt` | 85% | Good, handles team scoring |
| `serverfps.h/cpp` | `ServerFps.kt` | 90% | Good |
| `player.h/cpp` | `Player.kt` | 85% | Good |
| `backgroundmgr.h/cpp` | `BackgroundMgr.kt` | 85% | Good |
| `weathermgr.h/cpp` | `WeatherMgr.kt` | 85% | Good |

---

## 4. Recommended Fix Priority

### Phase A — Critical wire-format fixes (do first)
1. `SmartGameObj.controlOwner` default → `-99999`
2. `ScriptZoneGameObjDef` → extend `ScriptableGameObjDef`
3. `CinematicGameObj` → add `appPacketType`, `exportRare`/`importRare`
4. `SoldierGameObj.exportFrequent` → remove `definitionId != 0u` guard
5. `VehicleFactoryGameObj.requestVehicle` → fix `val` to `var`

### Phase B — Moderate gameplay fixes
6. `C4GameObjDef` → extend `SimpleGameObjDef`
7. `DamageableGameObjDef` → add encyclopedia fields
8. `SmartGameObj` → add GameObjManager.addSmart() registration
9. `BaseGameObj.delete()` → call destruct chain
10. `BuildingGameObj` → add MCT spawn logic
11. `SoldierGameObj` → port innate weapon setup

### Phase C — Feature completion (future work)
12. VehicleGameObj full seat management
13. Building subtype operational logic (refinery income, repair zone, etc.)
14. Full damage pipeline with armor table
15. CinematicGameObj think/postThink
