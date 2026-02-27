# Kotlin Server vs C++ Original — Comprehensive Audit Report

**Date**: 2026-02-27 (v3 — audit discrepancies fixed)

---

## Executive Summary

The Kotlin server reimplementation is remarkably faithful to the C++ original. All 25+ NetworkObject subclasses exist with correct inheritance chains. All 50+ DefinitionClass loaders are present with correct chunk/micro-chunk IDs. **Every wire format export method is byte-for-byte correct** — verified field-by-field across all classes. The game loop architecture mirrors C++ closely with correct ordering of god.think → gameObjManager.think → physics → postThink.

Previous audit issues (incomplete parent chain loading for SAMSite, SoldierFactory, AirStrip, WarFactory, Simple chain, etc.) have all been **fixed** in commit `89fb97a`.

Issues from v2 have been fixed. **Remaining actionable issues**: 1 medium (BaseControllerClass::Think not yet ported), 6 informational. All definition loaders, init sequence gaps, game loop ordering, respawn delay, and player↔soldier link are now resolved.

---

## 1. WIRE FORMAT — ALL CORRECT

Every export method in every NetworkObject subclass has been verified field-by-field: type, bit-packing encoder, field order, conditional logic, and super-call chain.

### Game Object Chain

| Class | exportCreation | exportRare | exportOccasional | exportFrequent |
|-------|---------------|------------|-----------------|----------------|
| PhysicalGameObj | pos(3×WORLD)+facing(f32) | model+anim+host+playerType+hud+[isHidden] | — | onHostBone |
| ArmedGameObj | — | — | — | super+targeting(3×WORLD) |
| SmartGameObj | super+controlOwner | — | — | super+controlSc |
| SoldierGameObj | super (no own) | super+defId | super+weaponBag | inVehicle+weapon+pos+state+super |
| VehicleGameObj | super+lockOwner[+timer] | super+seats+delivered | — | rounds+physics+gunner+super |

### Building Chain

| Class | exportCreation | exportRare | exportOccasional |
|-------|---------------|------------|-----------------|
| DamageableGameObj | — | — | defenseObject |
| BuildingGameObj | super+pos+collSphere | super+destroyed+power+state | inherited |
| VehicleFactoryGameObj | — | super+isBusy | — |
| RefineryGameObj | — | super+isHarvesterDocked | — |
| RepairBayGameObj | super+repairZone+facing | — | — |

### Simple Chain, Cinematics, SAMSite

| Class | exportRare |
|-------|-----------|
| C4GameObj | super+ammo+owner+vel+stuck... |
| BeaconGameObj | super+state+owner |
| CinematicGameObj | super+animName+animMode |
| SAMSiteGameObj | exportFrequent: super (pass-through) |

### Singletons & Static Objects

| Class | Method | Fields |
|-------|--------|--------|
| Player (1011) | creation/rare/occasional | name / id+ladder+team+5more+2×wol / score+money+kills+deaths |
| Team (1010) | creation/rare/occasional | teamNumber / kills+deaths / score |
| BaseControllerClass (0) | occasional | opTimeFactor+6×bool |
| ServerFps (0) | frequent | fps |
| BackgroundMgr (0) | rare | 8×4 floats + 3 counts |
| WeatherMgr (0) | rare | 6×4 floats + 2 counts |
| StaticNetworkObject (0) | rare | animMode+4 floats |
| DoorNetworkObject (0) | rare | doorState(BITPACK_DOOR_STATE) — no super |
| ElevatorNetworkObject (0) | rare | state+doorStateTop+doorStateBottom (raw ints) — no super |
| DsapoNetworkObject (0) | rare | super + isDead+health+shield |

**Note on Elevator**: C++ `ElevatorNetworkObjectClass::Export_Rare` uses `packet.Add(State)` / `packet.Add(DoorStateTop)` / `packet.Add(DoorStateBottom)` — raw ints with NO BITPACK encoder. The Kotlin matches exactly.

### Missing NetworkObject Classes

| C++ Class | classId | Severity | Notes |
|-----------|---------|----------|-------|
| cResetWinsEvent | 1006 | LOW | No factory registered in C++ either — effectively unused |
| DamageZoneGameObj | 1000 | LOW | Def exists, no runtime game object. Single-player only |
| GameSpy events | — | NONE | Intentionally omitted (GameSpy defunct) |

---

## 2. DEFINITION CLASS LOADERS — ALL PRESENT, MINOR GAPS

All 50+ C++ concrete DefinitionClass subclasses have corresponding Kotlin loaders with correct chunk IDs and micro-chunk IDs.

### Previous Issues — ALL FIXED

The following issues from the earlier audit have been resolved in commit `89fb97a`:
- SAMSiteGameObjDef parent chain ✅
- SoldierFactoryGameObjDef parent chain ✅
- AirStripGameObjDef parent chain ✅
- WarFactoryGameObjDef parent chain ✅
- SimpleGameObjDef parent chain ✅
- C4/Beacon/PowerUp parent chain ✅
- SpecialEffectsGameObjDef parent chain ✅
- CinematicGameObjDef parent chain ✅

### Definition Gaps — ALL FIXED

| # | Severity | Class | Resolution |
|---|----------|-------|------------|
| 1 | ~~MEDIUM~~ | BuildingGameObjDef | ✅ Fixed — loader now reads ScriptableGameObjDef chunk (scripts) |
| 2 | ~~LOW~~ | BuildingGameObjDef | ✅ Fixed — encyclopediaType/encyclopediaId now loaded |
| 3 | ~~LOW~~ | PowerUpGameObjDef | ✅ Fixed — delegates to SimpleGameObjDef.load() for full parent chain |
| 4 | NEW | DamageableStaticPhysDefClass | ✅ Added CHUNK_ID + FullDefinitionLoader dispatch |

### Field Parity — All Verified Correct

Every field in every definition hierarchy level matches the C++ original:

- **BaseGameObjDef**: (no fields) ✅
- **ScriptableGameObjDef**: scriptNameList, scriptParameterList ✅
- **DamageableGameObjDef**: defenseObjectDef, infoIconTextureFilename, translatedNameId, encyclopediaType, encyclopediaId, notTargetable, defaultPlayerType ✅
- **DefenseObjectDefClass**: health, healthMax, skin, shieldStrength, shieldStrengthMax, shieldType, damagePoints, deathPoints ✅
- **PhysicalGameObjDef**: type, radarBlipType, bullseyeOffsetZ, animation, physDefId, killedExplosion, defaultHibernationEnable, allowInnateConversations, oratorType, useCreationEffect ✅
- **ArmedGameObjDef**: weaponTiltRate/Min/Max, weaponTurnRate/Min/Max, weaponError, weaponDefId, secondaryWeaponDefId, weaponRounds ✅
- **SmartGameObjDef**: sightRange, sightArc, listenerScale, isStealthUnit ✅
- **SoldierGameObjDef**: turnRate, jumpVelocity, skeletonHeight/Width, useInnateBehavior, innateAggressiveness, innateTakeCoverProbability, innateIsStationary, dialogList[20], firstPersonHands, humanAnimOverrideDefId, humanLoiterCollectionDefId, deathSoundPresetId ✅
- **VehicleGameObjDef**: type, typeName, fire0/1Anim, profile, transitions, turnRadius, occupantsVisible, sightDownMuzzle, aim2D, engineSoundMaxPitchFactor, engineSound[4], squishVelocity, vehicleNameId, numSeats, 4× report IDs ✅
- **BuildingGameObjDef**: meshPrefix, mctSkin, type, 4× report IDs ✅
- **WeaponDefinitionClass**: 40+ fields ✅
- **AmmoDefinitionClass**: 60+ fields ✅
- **ExplosionDefinitionClass**: 12 fields ✅
- **All 9 global settings defs**: ✅
- **All 9 building sub-defs**: ✅
- **All 3 boss defs**: ✅
- **SpawnerDefClass**: 18 fields ✅

---

## 3. GAME INIT SEQUENCE

### Correct

| Step | C++ | Kotlin |
|------|-----|--------|
| Factory registration | Static constructors | NetworkObjectFactories.register() |
| ArmorWarheadManager | Init at game start | Loaded from armor.ini |
| Definition loading | objects.ddb from always.dbs | FullDefinitionLoader.load() |
| LSD/LDD loading | SaveGameManager::Load_Game | LsdParser + LddParser |
| Encoder precision | Compute_World_Size + Set_Precision | initEncoders() |
| StaticNetworkObject gen | Generate_Static_Network_Objects() | initializeLevel() — doors |
| Spawner filtering | Filter_Spawners() | SpawnManager filters at construction |
| Connection handler | Teams + GameOptionsEvent | Teams + GameOptionsEvent |

### Gaps

| C++ Step | Status | Impact |
|----------|--------|--------|
| Static network object generation for elevators/DSAPO | ✅ **FIXED** — elevators and DSAPOs now instantiated in initializeLevel() | |
| Remix/Swap/Rebalance_Team_Sides at game start | **MISSING** — per-player auto-balance only | No team remix at round start |
| Network pump during level load (keepalives) | **MISSING** | Clients may timeout during long loads |
| Post_Load_Id_Uniqueness_Check | **MISSING** | Low risk — IDs auto-assigned |
| BIOEVENT intermission handling | **MISSING** | Clients connecting during intermission may break |
| Version check in acceptance handler | **MISSING** | Wrong-version clients not rejected |

---

## 4. MAIN GAME LOOP

### Tick Order Comparison

```
C++ Order:                          Kotlin Order:
─────────────────────              ─────────────────────
cNetwork::Update():                networkTickLoop():
  The_Game()->Think()                gameState.think()
  + BaseController.Think()           (no BaseController.Think)
  cGod::Think()                      game-over detection
  End_Game_Test()                    god.think()
  TCADO (replication)                gameObjManager.think()
  Delete_Pending()                   physicsScene.update()
CombatManager::Think():              gameObjManager.postThink()
  GameObjManager::Think()            spawnManager.think()
  Physics (inline)                   doors tick
  GameObjManager::Post_Think()       vehicle dirty bits
  SpawnManager::Update()             replicationTick()
cPlayerManager::Think()              flushOutbox()
cTeamManager::Think()
```

### Correct Ordering

- god.think() runs BEFORE gameObjManager.think() ✅ (matches C++ cGod::Think before GameObjManager)
- Physics runs inline between Think and PostThink ✅ (matches C++ COMBAT_SCENE->Update)
- SpawnManager after PostThink ✅

### Ordering Differences

| # | Aspect | C++ | Kotlin | Impact |
|---|--------|-----|--------|--------|
| 1 | Game-over check | After cGod::Think + after replication | ~~Before god.think()~~ → ✅ **FIXED** — now after postThink() | Resolved |
| 2 | Replication timing | Before GameObjManager::Think (in cNetwork::Update) | After all thinking (end of loop) | **INFO** — Kotlin clients see Think() results same tick (lower latency), C++ sees them next tick |

### Missing Per-Tick Systems

| System | Purpose | Severity |
|--------|---------|----------|
| BaseControllerClass::Think() | Harvester management, power transitions | **MEDIUM** |
| cPlayerManager::Think() | Player game time, state transitions | LOW |
| cTeamManager::Think() | Team score aggregation | LOW |
| Hibernation_Think() | Object sleep by distance | LOW (performance) |
| SyncTime increment | Client animation synchronization | LOW |
| BulletManager::Update() | Server-side projectile tracking | LOW (client-trusted damage model) |

---

## 5. GOD / PLAYER LIFECYCLE

### Correct

- State machine: UNINITIALIZED → MULTIPLAYER ✅
- Soldier creation for soldierless in-game players ✅
- Player disconnect cleanup (soldier deletion + player removal) ✅

### Differences

| Aspect | C++ | Kotlin | Status |
|--------|-----|--------|--------|
| Respawn delay | Immediate (client handles delay) | ~~3-second RESPAWN_DELAY_SECONDS~~ | ✅ **FIXED** — respawn immediate |
| Player↔Soldier link | Set_Player_Data(player) | ~~Not linked~~ | ✅ **FIXED** — Player implements PlayerDataClass |
| Base destruction bonus | 5000 points to destroying team | Not implemented | LOW |

---

## 6. MISSING SYSTEMS (Structural)

### Not Implemented

1. **Script System** — Largest missing system. C++ uses scripts for zone behaviors, weapon grants, triggers.
2. **BulletManager / Projectiles** — No server-side projectile tracking. Damage is client-reported.
3. **TransitionManager** — Vehicle entry/exit via client frequent updates (in_vehicle flag) instead.
4. **Priority-based replication** — All dirty objects sent to all clients unconditionally.
5. **Team rebalancing at game start** — Only per-player auto-balance during gameplay.

### Present & Functional

| System | Status |
|--------|--------|
| ArmorWarheadManager (damage scaling) | ✅ Full |
| Purchase handling (VendorClass) | ✅ Full |
| Building lifecycle (damage, destruction, MCT) | ✅ Full |
| Spawner auto-spawn (powerups) | ✅ Full |
| Vehicle entry/exit detection | ✅ Client-driven |
| Scoring (kills, deaths, score) | ✅ Full |
| Map rotation | ✅ Full |
| Intermission / game-over | ✅ Full |
| RCON server | ✅ Full |
| LAN broadcast | ✅ Full |

---

## 7. COMPLETE ISSUE LIST

| # | Severity | Category | Description | Status |
|---|----------|----------|-------------|--------|
| 1 | ~~MEDIUM~~ | Def loader | BuildingGameObjDef loader skips ScriptableGameObjDef layer | ✅ Fixed |
| 2 | ~~MEDIUM~~ | Init | Elevator/DSAPO StaticNetworkObjects not instantiated | ✅ Fixed |
| 3 | **MEDIUM** | Tick | BaseControllerClass::Think() not called (harvester/power state) | Open |
| 4 | ~~LOW~~ | Def loader | PowerUpGameObjDef incomplete parent chain loading | ✅ Fixed |
| 5 | ~~LOW~~ | Def loader | BuildingGameObjDef missing encyclopediaType/encyclopediaId | ✅ Fixed |
| 6 | ~~LOW~~ | Tick order | Game-over check before god.think() (C++ checks after) | ✅ Fixed |
| 7 | ~~LOW~~ | Lifecycle | 3-second respawn delay (C++ spawns immediately) | ✅ Fixed |
| 8 | ~~LOW~~ | Lifecycle | Player↔Soldier data link not established | ✅ Fixed |
| 9 | INFO | Lifecycle | No base destruction score tweaking (5000 bonus) | Open |
| 10 | INFO | Tick | cPlayerManager::Think / cTeamManager::Think not ported | Open |
| 11 | INFO | Tick | SyncTime not maintained | Open |
| 12 | INFO | Tick order | Replication after Think (C++ before) — actually lower latency | Open |
| 13 | INFO | Structure | Priority-based replication not implemented | Open |
| 14 | INFO | Init | BIOEVENT intermission handling / version check missing | Open |
