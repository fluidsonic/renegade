# Audit: Kotlin Server vs C++ — NetworkObject & DefinitionClass Hierarchies

**Date**: 2026-02-27
**Scope**: Full file-by-file comparison of every NetworkObject subclass and DefinitionClass subclass in the Kotlin server against the original C++ source.

---

## Critical Issues (wire format / correctness bugs)

### 1. CsTextObj (classId=1018) — Extra field corrupts C→S packet parsing
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/CsTextObj.kt`
- **C++ reference**: `original/Code/Commando/cstextobj.h/.cpp`
- **Bug**: Kotlin `importCreation` reads 5 fields: `senderId(int), type(BYTE), text(wideString), recipientId(int), isHostAdminMessage(bool)`. The C++ client only sends 4 fields — `isHostAdminMessage` exists in the S→C `cScTextObj` (1001) but NOT in C→S `cCsTextObj` (1018). The server reads one extra bool from the stream, corrupting subsequent data.
- **Fix**: Remove `isHostAdminMessage` from `CsTextObj.importCreation()`.

### 2. MendozaBossGameObjDefClass — Wrong inheritance (stub, missing all soldier fields)
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/MendozaBossGameObjDefClass.kt`
- **C++ reference**: `original/Code/Combat/mendozabossgameobj.h` — extends `SoldierGameObjDef`
- **Bug**: Kotlin class extends `DefinitionClass` directly with no fields. Should extend `SoldierGameObjDefWrapper` (like how `SakuraBossGameObjDef` extends `VehicleGameObjDefWrapper`). Missing all soldier/smart/armed/physical/damageable def fields.
- **Fix**: Refactor to extend `SoldierGameObjDefWrapper`, loading a full `SoldierGameObjDef` first.

### 3. RaveshawBossGameObjDefClass — Same wrong inheritance as Mendoza
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/RaveshawBossGameObjDefClass.kt`
- **C++ reference**: `original/Code/Combat/raveshawbossgameobj.h` — extends `SoldierGameObjDef`
- **Bug**: Same as Mendoza — extends `DefinitionClass` directly, missing all soldier fields.
- **Fix**: Same pattern — extend `SoldierGameObjDefWrapper`.

---

## Moderate Issues (wrong inheritance pattern but fields present)

### 4. AccessiblePhysDefClass — Extends DefinitionClass instead of StaticAnimPhysDefClass
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/AccessiblePhysDefClass.kt`
- **C++ reference**: `original/Code/wwphys/accessiblephys.h` — extends `StaticAnimPhysDefClass`
- **Issue**: All parent fields are present as flat constructor params, but the class extends `DefinitionClass` directly instead of `StaticAnimPhysDefClass`. Polymorphic `is` checks won't work.
- **Impact**: Low — server doesn't currently need `is StaticAnimPhysDefClass` checks.

### 5. DamageableStaticPhysDefClass — Extends DefinitionClass instead of StaticAnimPhysDefClass
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/DamageableStaticPhysDefClass.kt`
- **C++ reference**: `original/Code/Combat/damageablestaticphys.h` — extends `StaticAnimPhysDefClass`
- **Issue**: Same pattern as AccessiblePhysDefClass — all fields present but flat hierarchy.

### 6. DoorPhysDefClass — Extends DefinitionClass instead of AccessiblePhysDefClass
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/DoorPhysDefClass.kt`
- **C++ reference**: `original/Code/Combat/doors.h` — extends `AccessiblePhysDefClass`
- **Issue**: Same flat-hierarchy pattern.

### 7. ElevatorPhysDefClass — Extends DefinitionClass instead of AccessiblePhysDefClass
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/ElevatorPhysDefClass.kt`
- **C++ reference**: `original/Code/Combat/elevator.h` — extends `AccessiblePhysDefClass`
- **Issue**: Same flat-hierarchy pattern.

### 8. ShakeableStaticPhysDefClass — Standalone data class, not extending DefinitionClass
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/ShakeableStaticPhysDefClass.kt`
- **C++ reference**: `original/Code/wwphys/shakeablestaticphys.h` — extends `StaticAnimPhysDefClass`
- **Issue**: Defined as `data class` not extending any Def base. All fields present but no inheritance.

### 9. TimedDecorationPhysDefClass — Standalone data class
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/TimedDecorationPhysDefClass.kt`
- **C++ reference**: `original/Code/wwphys/timeddecophys.h` — extends `DecorationPhysDefClass`
- **Issue**: Standalone `data class`, not extending `DecorationPhysDefClass`. Missing `chunkId`.

### 10. BuildingAggregateDefClass — Standalone data class
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/BuildingAggregateDefClass.kt`
- **C++ reference**: `original/Code/Combat/buildingaggregate.h` — extends `StaticAnimPhysDefClass`
- **Issue**: Standalone `data class`. Intentional simplification — server only needs animation states and MCT flag.

---

## Low-Priority Issues

### 11. DamageZoneGameObj — Missing (game object class, not just def)
- **C++ reference**: `original/Code/Combat/damagezone.h` — `DamageZoneGameObj : BaseGameObj`
- **Kotlin**: Only `DamageZoneGameObjDef` exists, no `DamageZoneGameObj` class.
- **Impact**: Low — no network export methods in C++ (server-side only, campaign feature).

### 12. Player.wolRank — Wrong default value (wire mismatch)
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/net/Player.kt:45`
- **C++ reference**: `original/Code/Commando/player.cpp:767-768` — hardcodes `(int)0` for both removed WOL fields
- **Issue**: Kotlin has `var wolRank: Int = -1` and writes it via `exportRare`. C++ always writes `0`. Wire bytes differ (`0xFFFFFFFF` vs `0x00000000`).
- **Fix**: Change default to `0`.
- **Impact**: Low — WOL (Westwood Online) is defunct, client likely ignores these values, but wire format should still match.

### 13. SoldierFactoryGameObjDef — Missing C++ parent fields
- **File**: `kotlin-server/server/src/main/kotlin/ccr/server/defs/SoldierFactoryGameObjDef.kt`
- **C++ reference**: Has `UnloadTime`, `FundsGathered`, `HarvesterDefID` fields.
- **Issue**: These fields are shared with PowerPlant/ComCenter defs in C++ but not present in Kotlin SoldierFactory.
- **Impact**: Low — these fields are only used for harvester/tiberium logic not yet implemented.

---

## Informational (intentional differences, no action needed)

- **CsDamageEvent** extends `NetEvent` in Kotlin but `NetworkObjectClass` directly in C++. No wire format impact.
- **cResetWinsEvent (classId=1006)**: No implementation in C++ — correctly omitted from Kotlin.
- **SoldierGameObjDef/VehicleGameObjDef** use wrapper pattern (data class + wrapper) in Kotlin vs direct inheritance in C++. All fields correct, just different architecture.
- **PhysicalGameObj.exportCreation** writes `definitionId` directly in Kotlin vs factory `Prep_Packet()` in C++. Wire format is identical.

---

## Summary

| Category | Count | Status |
|----------|-------|--------|
| NetworkObject game obj classes | 29/30 | 29 correct, 1 missing (DamageZoneGameObj) |
| Non-game NetworkObject classes | 9/9 | All correct |
| NetEvent subclasses (S→C) | 13/13 | All correct |
| NetEvent subclasses (C→S) | 19/20 | 1 critical bug (CsTextObj extra field) |
| Game object defs | 19/19 | All correct |
| Building defs | 9/9 | All correct (1 low-priority note) |
| Boss defs | 1/3 | 2 critical (Mendoza/Raveshaw wrong inheritance) |
| Equipment/combat defs | 5/5 | All correct |
| Global settings defs | 10/10 | All correct |
| Physics defs | 18/24 | 6 moderate (flat hierarchy instead of inheritance) |
| **Total** | **132/142** | **3 critical, 7 moderate, 3 low** |
