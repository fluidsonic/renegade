# Phase 4 Execution Plan: C4 System

## Context

Buildings are invulnerable — C4 is the primary mechanic for destroying enemy buildings in C&C mode. Phase 3 (vehicle entry/exit) is merged. This phase adds C4 placement, timed/remote detonation, and building damage. Master plan at `/plans/imperative-kindling-ripple.md`.

**Simplifications** (no physics until Phase 11):
- No projectile flight — C4 instantly sticks to the nearest enemy building when fired
- Only building attachment (not vehicles/soldiers/terrain)
- No proximity C4 (AMMO_TYPE_C4_PROXIMITY) — timed and remote only
- Damage uses `BuildingGameObj.applyDamage()` directly with `ExplosionDefinitionClass.damageStrength`

## Key Research Findings

### Existing Infrastructure
| Component | Status | File |
|-----------|--------|------|
| `C4GameObj` + `exportRare()` | Done (all fields immutable) | `net/C4GameObj.kt` |
| `C4GameObjTest` | Done (round-trip tests) | `net/C4GameObjTest.kt` |
| `AmmoDefinitionClass` | Fully parsed (ammoType, c4TriggerTime*, explosionDefId) | `defs/AmmoDefinitionClass.kt` |
| `WeaponDefinitionClass` | Fully parsed (style=0 is C4) | `defs/WeaponDefinitionClass.kt` |
| `ExplosionDefinitionClass` | Fully parsed (damageStrength, damageRadius) | `defs/ExplosionDefinitionClass.kt` |
| `ScExplosionEvent` | Done (classId=1014, defId+pos+ownerId) | `net/ScExplosionEvent.kt` |
| `BuildingGameObj.applyDamage()` | Done (marks BIT_OCCASIONAL, calls onDestroyed at 0) | `net/BuildingGameObj.kt` |
| `BaseControllerClass` | Done (tracks buildings, power/radar/destruction) | `net/BaseControllerClass.kt` |
| `DefinitionRegistry` | All typed defs loaded at level load | `level/DefinitionRegistry.kt` |
| `FullDefinitionLoader` | WeaponDef, AmmoDef, ExplosionDef all dispatched | `level/FullDefinitionLoader.kt` |

### C4 Placement Protocol (from C++ research)
C4 uses the **normal weapon fire path** — no special packet:
1. Client sets `BOOLEAN_WEAPON_FIRE_PRIMARY` in `continuousBoolBits` (bit 0 of 4 continuous bits)
2. Server already reads `continuousBoolBits` in `handleFrequentUpdate` and stores on soldier
3. Server checks weapon style: `WeaponDefinitionClass.style == 0` means C4
4. Server creates C4GameObj stuck to nearest building, marks `BIT_RARE` → clients see C4

### Remote C4 Detonation
C++ uses `BOOLEAN_WEAPON_USE` (one-time boolean) which we don't parse. Instead, use `BOOLEAN_WEAPON_FIRE_SECONDARY` (bit 1 of `continuousBoolBits`) — the client sends this when alt-firing a C4 weapon.

### C4 Hierarchy
`NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → C4GameObj`

C4 does NOT extend SmartGameObj — no control inputs, no weapons. Only overrides `exportRare`. Inherits `exportCreation` (position+facing), `exportOccasional` (health/shield), `exportFrequent` (onHostBone) from parent chain.

### C4 Per-Team Limit
30 non-timed C4 per team. Oldest by `age` is defused when exceeded.

## Files to Modify

| File | Changes |
|------|---------|
| `server/.../net/C4GameObj.kt` | `val`→`var`, add `think()`, `detonate()`, `defuse()`, timer/owner/ammo fields |
| `server/.../net/SoldierGameObj.kt` | Add `currentWeaponDefId`, `detonateC4` fields |
| `server/.../God.kt` | `createC4()`, `c4Objects` list, C4 limit, cleanup on disconnect |
| `server/.../GameServer.kt` | Weapon fire detection, C4 weapon/ammo lookup helpers, explosion broadcast |

## Tasks

### Task 1: Make C4GameObj mutable + add think/detonate/defuse
### Task 2: SoldierGameObj weapon tracking + detonation flag
### Task 3: C4 creation + tracking in God
### Task 4: Weapon fire detection + C4 placement in GameServer
