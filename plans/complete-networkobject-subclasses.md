# Plan: Complete ALL C++ NetworkObjectClass Subclasses (Kotlin Port)

## Context

The abstract GameObj hierarchy has been faithfully ported from C++ to Kotlin (new no-constructor-params design):
**BaseGameObj → ScriptableGameObj → DamageableGameObj → PhysicalGameObj → ArmedGameObj → SmartGameObj — DONE, MUST NOT BE TOUCHED.**

The leaf classes still use an **older constructor-parameter-based design**. They need to be rewritten to match C++ faithfully, following `docs/implementing-gameobj-classes.md`.

**Scope**: ALL NetworkObjectClass subclasses INCLUDING client-only. Def classes too.
**Call sites**: Fix at the end, staying VERY close to C++ server code.
**Tests**: Successful tests are a non-goal yet.
**Execution**: Use agents ONLY — do NOT implement or explore from main conversation.

---

## Rules (from `docs/implementing-gameobj-classes.md`)

Per class:
1. Read C++ `.h` and `.cpp` fully before writing any Kotlin
2. Fields: all C++ members as `var` properties (not constructor params), match init values
3. Constructor/destructor: `init {}` + `override fun destruct()`
4. `init()` / `init(definition)` / `copySettings(definition)` / `reInit(definition)` / `getDefinition()`
5. `save()` / `load()` / `onPostLoad()`
6. `think()` / `postThink()` if overridden in C++
7. Network: all `export/import` methods for Creation, Rare, Occasional, Frequent
8. All remaining methods from C++ header, in order
9. Chunk/micro-chunk IDs verified against C++ enums (include LEGACY IDs)
10. `companion object` with all constants

**One file at a time. Assume all external classes exist. Fix cross-file errors only at the very end.**

---

## Existing Def Classes (in `kotlin-server/server/src/main/kotlin/ccr/server/defs/`)

### Already exist — need C++ verification and possible merge:
- `BaseGameObjDef.kt`, `DamageableGameObjDef.kt`, `PhysicalGameObjDef.kt`, `ArmedGameObjDef.kt`, `SmartGameObjDef.kt` — abstract hierarchy Defs (in `SoldierGameObjDef.kt` as data classes)
- `DefenseObjectDefClass.kt` — DefenseObjectDef (duplicate in BuildingGameObjDef.kt too)
- `SoldierGameObjDef.kt` — full, uses composition pattern
- `VehicleGameObjDef.kt` — full, uses composition pattern
- `BuildingGameObjDef.kt` — full, uses inheritance pattern (extends DefinitionClass)
- `SAMSiteGameObjDef.kt` — exists
- `SoldierFactoryGameObjDef.kt` — exists
- `TransitionGameObjDef.kt` — exists

### Need to be created:
- `SimpleGameObjDef.kt`
- `C4GameObjDef.kt` (BeaconGameObjDef in C++ is BeaconGameObjDef)
- `BeaconGameObjDef.kt`
- `PowerUpGameObjDef.kt`
- `SpecialEffectsGameObjDef.kt`
- `CinematicGameObjDef.kt`
- `PowerPlantGameObjDef.kt`
- `ComCenterGameObjDef.kt`
- `RefineryGameObjDef.kt`
- `RepairBayGameObjDef.kt`
- `VehicleFactoryGameObjDef.kt`
- `AirStripGameObjDef.kt`
- `WarFactoryGameObjDef.kt`
- `DamageZoneGameObjDef.kt`
- `ScriptZoneGameObjDef.kt`

---

## Execution Order

### Phase 1: Core Leaf Classes (PhysicalGameObj branch)

Each item = one agent task. Process in inheritance order.

| # | Type | Class | C++ Source | C++ Lines | Notes |
|---|------|-------|------------|-----------|-------|
| 1a | Def | SimpleGameObjDef | simplegameobj.h/cpp | ~40 | Create new |
| 1b | Obj | SimpleGameObj | simplegameobj.h/cpp | 291 | Rewrite |
| 2a | Def | C4GameObjDef | c4.h/cpp | ~60 | Create new |
| 2b | Obj | C4GameObj | c4.h/cpp | 906 | Rewrite — think, detonate, exportRare |
| 3a | Def | BeaconGameObjDef | beacongameobj.h/cpp | ~60 | Create new |
| 3b | Obj | BeaconGameObj | beacongameobj.h/cpp | 1462 | Rewrite — arming, detonation |
| 4a | Def | PowerUpGameObjDef | powerup.h/cpp | ~60 | Create new |
| 4b | Obj | PowerUpGameObj | powerup.h/cpp | 884 | Rewrite — grant, think |
| 5a | Def | SpecialEffectsGameObjDef | specialeffectsgameobj.h/cpp | ~40 | Create new |
| 5b | Obj | SpecialEffectsGameObj | specialeffectsgameobj.h/cpp | 497 | Rewrite — think, doEffect |

### Phase 2: SmartGameObj Branch

| # | Type | Class | C++ Source | C++ Lines | Notes |
|---|------|-------|------------|-----------|-------|
| 6a | Def | SoldierGameObjDef | soldier.h/cpp | exists | Verify/merge existing |
| 6b | Obj | SoldierGameObj | soldier.h/cpp | 5308 | Rewrite — LARGEST CLASS (~95 methods) |
| 7a | Def | VehicleGameObjDef | vehicle.h/cpp | exists | Verify/merge existing |
| 7b | Obj | VehicleGameObj | vehicle.h/cpp | 2653 | Rewrite — occupants, turret, physics |
| 8a | Def | SAMSiteGameObjDef | samsite.h/cpp | exists | Verify existing |
| 8b | Obj | SAMSiteGameObj | samsite.h/cpp | 398 | Rewrite — turret, targeting |

### Phase 3: ArmedGameObj Branch

| # | Type | Class | C++ Source | C++ Lines | Notes |
|---|------|-------|------------|-----------|-------|
| 9a | Def | CinematicGameObjDef | cinematicgameobj.h/cpp | ~40 | Create new |
| 9b | Obj | CinematicGameObj | cinematicgameobj.h/cpp | 515 | Rewrite |

### Phase 4: Building Hierarchy (DamageableGameObj branch)

| # | Type | Class | C++ Source | C++ Lines | Notes |
|---|------|-------|------------|-----------|-------|
| 10a | Def | BuildingGameObjDef | building.h/cpp | exists | Verify/merge existing |
| 10b | Obj | BuildingGameObj | building.h/cpp | 1635 | Rewrite — damage, power, state, mesh management |
| 11a | Def | PowerPlantGameObjDef | powerplantgameobj.h/cpp | ~30 | Create new |
| 11b | Obj | PowerPlantGameObj | powerplantgameobj.h/cpp | 378 | Rewrite |
| 12a | Def | SoldierFactoryGameObjDef | soldierfactorygameobj.h/cpp | exists | Verify existing |
| 12b | Obj | SoldierFactoryGameObj | soldierfactorygameobj.h/cpp | 400 | Rewrite |
| 13a | Def | ComCenterGameObjDef | comcentergameobj.h/cpp | ~30 | Create new |
| 13b | Obj | ComCenterGameObj | comcentergameobj.h/cpp | 379 | Rewrite |
| 14a | Def | RefineryGameObjDef | refinerygameobj.h/cpp | ~40 | Create new |
| 14b | Obj | RefineryGameObj | refinerygameobj.h/cpp | 871 | Rewrite — harvester management |
| 15a | Def | RepairBayGameObjDef | repairbaygameobj.h/cpp | ~40 | Create new |
| 15b | Obj | RepairBayGameObj | repairbaygameobj.h/cpp | 1025 | Rewrite — repair logic |
| 16a | Def | VehicleFactoryGameObjDef | vehiclefactorygameobj.h/cpp | ~40 | Create new |
| 16b | Obj | VehicleFactoryGameObj | vehiclefactorygameobj.h/cpp | 811 | Rewrite — vehicle generation |
| 17a | Def | AirStripGameObjDef | airstripgameobj.h/cpp | ~30 | Create new |
| 17b | Obj | AirStripGameObj | airstripgameobj.h/cpp | 572 | Rewrite |
| 18a | Def | WarFactoryGameObjDef | warfactorygameobj.h/cpp | ~30 | Create new |
| 18b | Obj | WarFactoryGameObj | warfactorygameobj.h/cpp | 561 | Rewrite |

### Phase 5: Server-Only Classes

| # | Type | Class | C++ Source | C++ Lines | Notes |
|---|------|-------|------------|-----------|-------|
| 19a | Def | TransitionGameObjDef | transitiongameobj.h/cpp | exists | Verify existing |
| 19b | Obj | TransitionGameObj | transitiongameobj.h/cpp | 341 | Create new |
| 20a | Def | DamageZoneGameObjDef | damagezone.h/cpp | ~30 | Create new |
| 20b | Obj | DamageZoneGameObj | damagezone.h/cpp | 273 | Create new |
| 21a | Def | ScriptZoneGameObjDef | scriptzone.h/cpp | ~30 | Create new |
| 21b | Obj | ScriptZoneGameObj | scriptzone.h/cpp | 562 | Create new |

### Phase 6: Non-GameObj NetworkObjects

These follow their own C++ patterns (not the GameObj guide). Port faithfully from C++.

| # | Class | C++ Source | Kotlin Status | Notes |
|---|-------|------------|---------------|-------|
| 22 | Player (cPlayer) | Commando/player.h/cpp | EXISTS — verify completeness | Save/Load, export methods, ~30 methods |
| 23 | Team (cTeam) | Commando/team.h/cpp | EXISTS — verify completeness | Small class |
| 24 | BaseControllerClass | Combat/basecontroller.h/cpp | EXISTS — verify completeness | Singleton, export_occasional |
| 25 | ServerFps (cServerFps) | Commando/serverfps.h/cpp | EXISTS — verify completeness | Singleton |
| 26 | ClientControl (CClientControl) | Combat/clientcontrol.h/cpp | EXISTS — verify completeness | C→S |
| 27 | ClientFps (CClientFps) | Commando/clientfps.h/cpp | EXISTS — verify completeness | C→S |
| 28 | StaticNetworkObject | Combat/staticnetworkobject.h/cpp | EXISTS — verify completeness | Base + 3 subclasses |
| 29 | DoorNetworkObject | Combat/staticnetworkobject.h/cpp | EXISTS — verify completeness | |
| 30 | ElevatorNetworkObject | Combat/staticnetworkobject.h/cpp | EXISTS — verify completeness | |
| 31 | DsapoNetworkObject | Combat/staticnetworkobject.h/cpp | EXISTS — verify completeness | |
| 32 | BackgroundMgr | Combat/backgroundmgr.h/cpp | EXISTS — verify completeness | Singleton |
| 33 | WeatherMgr | Combat/WeatherMgr.h/cpp | EXISTS — verify completeness | Singleton |

### Phase 7: NetEvent Classes

These are fire-and-forget creation-only events. Verify each against C++ for completeness.

| # | Class | C++ Source | Notes |
|---|-------|------------|-------|
| 34 | All S→C events | Commando/*.h/cpp | ScTextObj, PlayerKill, WinEvent, PurchaseResponse, ConsoleCommand, SvrGoodbye, GameOptions, Eviction, GameDataUpdate, ScPingResponse, ScExplosion, ScObelisk, ScAnnouncement |
| 35 | All C→S events | Commando/*.h/cpp | CsTextObj, Suicide, ChangeTeam, Money, Warp, PurchaseRequest, ClientGoodbye, BioEvent, Loading, GodMode, VipMode, Score, ClientBbo, CsPingRequest, CsDamage, RequestKill, CsConsoleCommand, CsHint, CsAnnouncement, Donate |

### Phase 8: Fix Call Sites

After ALL classes ported, fix every file that constructs/uses the old API:
- `God.kt` — soldier/player creation
- `GameServer.kt` — object construction
- `BuildingManager.kt` — building construction
- `GameObjManager.kt` — object registration
- `NetworkObjectPacketWriter.kt` — if affected
- `SpawnManager` — spawning
- All other files referencing old constructor-param patterns
- Stay VERY VERY CLOSE to C++ server code (god.cpp, combat.cpp, etc.)

---

## Agent Task Template

Each agent receives (via reading files):
1. `docs/implementing-gameobj-classes.md` — the strict guide
2. The C++ `.h` and `.cpp` for the class being ported
3. The current Kotlin file (to understand existing code to rewrite)
4. One completed example (e.g., SmartGameObj.kt) as pattern reference
5. For Def classes: existing Def examples (SoldierGameObjDef.kt pattern)

Agent instruction pattern:
> "Rewrite `{ClassName}.kt` following `docs/implementing-gameobj-classes.md` strictly. Read the C++ `.h` and `.cpp` files first. Port ALL fields, methods, save/load, think, export/import from C++. One file only. Assume all external classes exist."

---

## Parallelization Strategy

- Def + Obj for the same class: sequential (Def first, then Obj)
- Independent branches: can run in parallel (e.g., Phase 1 items vs Phase 4 items)
- Within a branch: sequential by inheritance (parent before child)
- Phase 6-7 (verification of existing classes): can run many in parallel
- Phase 8 (call sites): sequential, after all classes done

---

## Key Files Reference

### C++ source directories:
- `original-untouched/Code/Combat/` — all GameObj .h/.cpp files
- `original-untouched/Code/Commando/` — Player, Team, ServerFps, ClientFps, NetEvent subclasses

### Kotlin target directories:
- `kotlin-server/server/src/main/kotlin/ccr/server/net/` — all GameObj .kt files
- `kotlin-server/server/src/main/kotlin/ccr/server/defs/` — all *Def .kt files

### Completed examples (DO NOT MODIFY):
- `SmartGameObj.kt` — most complete abstract class example
- `PhysicalGameObj.kt` — export/import pattern
- `DamageableGameObj.kt` — DefenseObject integration
- `SoldierGameObjDef.kt` — Def class loading pattern
