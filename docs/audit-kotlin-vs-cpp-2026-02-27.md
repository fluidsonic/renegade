# Kotlin Server Audit vs C++ — 2026-02-27

## Summary

Audit of the Kotlin server against the C++ original revealed that the NetworkObject creation pipeline was missing the factory layer. Additionally, WeaponBagClass had a structural mismatch in how it handled the null weapon sentinel.

---

## Finding 1: Missing Factory Layer (FIXED)

### C++ Architecture

In C++, `NetworkObjectFactoryClass` sits between the packet header and `Export_Creation`/`Import_Creation`. For game objects (classId=1000), `NetworkGameObjectFactoryClass::Prep_Packet` writes `definitionId` before `Export_Creation` is called. On the client side, `NetworkGameObjectFactoryClass::Create` reads `definitionId` to instantiate the correct object type.

### Kotlin Before

The Kotlin code wrote `definitionId` inside `PhysicalGameObj.exportCreation()` and `BuildingGameObj.exportCreation()`, bypassing the factory layer entirely. The `NetworkObjectPacketWriter.writeCreation` method had no factory call.

### Fix Applied

- Added `NetworkGameObjectFactory` (classId=1000): `prepPacket` writes `definitionId`; `create` reads it (server-only, returns null)
- Added `SimpleNetworkObjectFactory` for event classIds 1001–1038 (no-op prepPacket)
- Added `SimpleNetworkObjectFactory(0)` for classId=0 singletons (BaseControllerClass, ServerFps, StaticNetworkObjects)
- Updated `NetworkObjectPacketWriter.writeCreation` to call `factory.prepPacket()` after writing classId
- Removed `packet.addInt(definitionId)` from `PhysicalGameObj.exportCreation()` and `BuildingGameObj.exportCreation()`
- Added `NetworkObjectFactories` singleton to register all factories at server startup
- Created `NetworkFactoriesExtension` JUnit5 auto-discovery extension so tests auto-register factories

### Wire Format (Corrected)

```
[networkId:32][dirtyBits:8][isDeletePending:1][classId:32][factory_data][exportCreation][exportRare][exportOccasional][exportFrequent]
```

For classId=1000: `factory_data = [definitionId:32]`

For classId=1001–1038 and classId=0: `factory_data = (empty)`

### C++ Reference

- `basegameobj.cpp:54–106` — `NetworkGameObjectFactoryClass`
- `messages.cpp:987–999` — packet write sequence

---

## Finding 2: WeaponBagClass Null Sentinel Mismatch (FIXED)

### C++ Architecture

`WeaponBagClass` constructor does `WeaponList.Add(NULL)` — index 0 is always a null pointer, acting as the "no weapon" sentinel.

### Kotlin Before

Created a real `WeaponClass(defId=0)` at index 0, then compensated with `weaponBag.getIndex() != 0` checks in `SoldierGameObj.exportFrequent`. The `SoldierGameObj` secondary constructor also explicitly called `weaponBag.addWeapon(0, 0, giveWeapon = false)` to create the fists placeholder.

### Fix Applied

- Changed `weaponList: MutableList<WeaponClass?>` — nullable elements, initialized with `null` at index 0
- Removed fists placeholder from `SoldierGameObj` secondary constructor
- Updated `SoldierGameObj.hasWeapon`: simplified from `(p_weapon != null && weaponBag.getIndex() != 0)` to `(p_weapon != null)`
- Updated `setupInnateWeapons` check from `getCount() > 0` to `getCount() > 1` (index 0 is null sentinel)
- Fixed `God.kt` and `PowerUpGameObjDef.kt` loops to start from index 1 (skipping null sentinel)

---

## Remaining Structural Differences (Tracked, Not Fixed)

These are known mismatches that require larger refactors and are not addressed in this step.

### 1. SoldierGameObjDef / VehicleGameObjDef

Uses data class + wrapper instead of C++ inheritance chain. Kotlin wraps a flat data class in a `BaseGameObjDef` wrapper.

### 2. DoorPhysDefClass / ElevatorPhysDefClass

Flattened fields instead of the C++ hierarchy `AccessiblePhysDefClass : StaticAnimPhysDefClass : StaticPhysDefClass : PhysDefClass`.

### 3. Missing FullDefinitionLoader Dispatches

The following definition types have no loader dispatch yet:

- `TwiddlerClass`
- `ShakeableStaticPhysDefClass`
- `BuildingAggregateDefClass`
- `TimedDecorationPhysDefClass`

### 4. Missing Runtime Game Objects

The following game object types are not yet implemented:

- `PowerUpGameObj`
- `ScriptZoneGameObj`
- `DamageZoneGameObj`
- `WarFactoryGameObj` (specialized subtype)
- `AirStripGameObj` (specialized subtype)

### 5. Missing NetEvent

- `ResetWinsEvent` (classId=1006)

---

## Files Changed

| File | Change |
|---|---|
| `net/.../replication/NetworkObjectFactory.kt` | Already existed — no changes needed |
| `server/.../net/NetworkGameObjectFactory.kt` | NEW — game object factory + simple factory + init object |
| `server/.../net/NetworkObjectPacketWriter.kt` | Call `factory.prepPacket` before `exportCreation` |
| `server/.../net/PhysicalGameObj.kt` | Remove `definitionId` write from `exportCreation` |
| `server/.../net/BuildingGameObj.kt` | Remove `definitionId` write from `exportCreation` |
| `server/.../GameServer.kt` | Register all factories at startup |
| `server/.../net/WeaponBagClass.kt` | Nullable slot 0, match C++ `WeaponList.Add(NULL)` |
| `server/.../net/SoldierGameObj.kt` | Remove fists placeholder, simplify `hasWeapon` check |
| `server/.../God.kt` | Fix `peekWeapon` loops for nullable slot 0 |
| `server/.../defs/PowerUpGameObjDef.kt` | Fix `peekWeapon` loop for nullable slot 0 |
| `server/src/test/.../NetworkFactoriesExtension.kt` | NEW — JUnit5 auto-discovery extension |
| `server/src/test/resources/META-INF/services/...` | NEW — JUnit5 extension service file |
| `server/src/test/resources/junit-platform.properties` | NEW — enable JUnit5 auto-detection |
| Multiple test files | Updated bit counts, removed `definitionId` skips |
