# Soldier Packet Wire Format

Analysis of the SoldierGameObj creation packet sent from server to client.

Date: 2026-02-21

## Class Hierarchy (Export Order)

The soldier inherits through this chain. Each level adds fields to the packet:

```
NetworkObject (base — empty export)
  └→ BaseGameObj (classId=1000)
       └→ DamageableGameObj (health, shield)
            └→ PhysicalGameObj (position, model, animation)
                 └→ ArmedGameObj (targeting)
                      └→ SmartGameObj (controlOwner, analog control)
                           └→ SoldierGameObj (weapon bag, human state)
```

Export methods are called in C++ super→sub order within each tier, but **SoldierGameObj::Export_Frequent writes its own fields BEFORE calling super** (position, humanState, etc. come before on_host_bone/targeting/control).

## Full Creation Packet Layout

A creation packet has `dirtyBits = 0x0F` which triggers all 4 export tiers.

### Header (NetworkObjectPacketWriter)

| Bit offset | Width | Field | Notes |
|------------|-------|-------|-------|
| 0 | 32 | networkId | Unique object ID |
| 32 | 8 | dirtyBits | 0x0F for creation |
| 40 | 1 | isDeletePending | Always false for creation |
| 41 | 32 | classId | 1000 (NETCLASSID_GAMEOBJ) |

**Header total: 73 bits**

### Export_Creation

Written by: PhysicalGameObj (via factory) + SmartGameObj

| Bit offset | Width | Field | Source |
|------------|-------|-------|--------|
| 73 | 32 | definitionId | C++ factory `Prep_Packet`; Kotlin: PhysicalGameObj |
| 105 | X_BITS | position.x | BITPACK_WORLD_POSITION_X (map-dependent) |
| 105+X | Y_BITS | position.y | BITPACK_WORLD_POSITION_Y (map-dependent) |
| ... | Z_BITS | position.z | BITPACK_WORLD_POSITION_Z (map-dependent) |
| ... | 32 | facing | Raw IEEE 754 float |
| ... | 32 | controlOwner | SmartGameObj: client ID that owns this soldier |

**Export_Creation total: 32 + X_BITS + Y_BITS + Z_BITS + 32 + 32**

### Export_Rare

Written by: PhysicalGameObj + SoldierGameObj

| Offset from rare start | Width | Field | Notes |
|------------------------|-------|-------|-------|
| 0 | 16 + len×8 | modelName | Terminated string (len=USHORT, then chars) |
| varies | 16 + len×8 | animName | Terminated string (often empty on creation) |
| varies | 32 | curr_frame | Animation current frame |
| varies | 32 | target_frame | Animation target frame |
| varies | 32 | anim_mode | 0=TARGET, 1=MANUAL, ... |
| varies | 32 | host_model_id | 0 if not hosted on another object |
| varies | 32 | host_bone | 0 if not hosted |
| varies | 32 | playerType | Team: 0=GDI, 1=Nod (via Get_Player_Type virtual) |
| varies | 1 | hud_pokable | HUD indicator flag |
| varies | 32 | soldier.definitionId | SoldierGameObj repeats the defId |

Note: `hidden` bool is only written for VehicleGameObj, not soldiers.

### Export_Occasional

Written by: DamageableGameObj (DefenseObject) + SoldierGameObj (weapon bag)

| Offset from occ start | Width | Field | Notes |
|------------------------|-------|-------|-------|
| 0 | 1 | isDead | `Health == 0` |
| 1 | 11 | health | BITPACK_HEALTH (0–2000, 11 bits) |
| 12 | 11 | shieldStrength | BITPACK_SHIELD_STRENGTH (0–2000, 11 bits) |
| 23 | 4 | shieldType | BITPACK_SHIELD_TYPE (0–armorTypeCount) |
| 27 | 32 | weaponCount | Number of REAL weapons to follow (see below) |
| 59 | 64×count | weapons[] | For each: weaponDefId(32) + totalRounds(32) |

**Weapon List Format** (from `WeaponBag::Export_Weapon_List`):
- Writes `WeaponList.Count() - 1` as int — this is the count of real weapons (slot 0 is always the empty/fist weapon and is skipped)
- Client reads this value directly as the loop count (no +1 adjustment)
- 0 = no real weapons to follow (soldier only has slot 0)
- 1 = one real weapon follows (64 bits of data)
- A typical Renegade soldier has 1–2 real weapons

### Export_Frequent

Written by: SoldierGameObj + PhysicalGameObj + ArmedGameObj + SmartGameObj

| Offset from freq start | Width | Field | Source |
|------------------------|-------|-------|--------|
| 0 | 1 | in_vehicle | SoldierGameObj |
| 1 | 1 | has_weapon | (only if !in_vehicle) |
| 2 | 64 | weapon id+rounds | (only if has_weapon) |
| varies | X_BITS | position.x | (only if !in_vehicle) |
| varies | Y_BITS | position.y | |
| varies | Z_BITS | position.z | |
| varies | 4 | humanState | BITPACK_HUMAN_STATE (0–12) |
| varies | 9 | humanSubState | BITPACK_HUMAN_SUB_STATE (0–511) |
| varies | 96 | velocity x,y,z | (only if humanState == AIRBORNE=3) |
| varies | 16+len×8 | animName | (only if humanState == TRANSITION=7 or ANIMATION=9) |
| varies | 1 | is_special_damage | |
| varies | 32 | damage_mode | (only if is_special_damage) |
| varies | 1 | on_host_bone | PhysicalGameObj |
| varies | X_BITS | targeting.x | ArmedGameObj |
| varies | Y_BITS | targeting.y | |
| varies | Z_BITS | targeting.z | |
| varies | 4 | ContinuousBooleanBits | SmartGameObj (Export_Control_Sc) |
| varies | 8 | analog_forward | BITPACK_ANALOG_VALUES (-1 to 1) |
| varies | 8 | analog_left | |
| varies | 8 | analog_up | |
| varies | 8 | analog_turn | |

## Encoder Precision (Map-Dependent)

Position encoders derive bit widths from the map's world extents (loaded from the LSD file in the map MIX). Each axis can have a **different** bit width.

### C&C_Under Map

| Encoder | Min | Max | Resolution | Bits |
|---------|-----|-----|------------|------|
| WORLD_POSITION_X | -578.52 | 517.15 | — | 13 |
| WORLD_POSITION_Y | -255.34 | 471.86 | — | 12 |
| WORLD_POSITION_Z | -67.00 | 71.46 | — | 10 |

World extents from LSD (with 1.0 margin applied):
- X: [-577.5166, 516.15454] → with margin → [-578.52, 517.15]
- Y: [-254.34427, 470.85605] → with margin → [-255.34, 471.86]
- Z: [-66.0001, 70.46397] → with margin → [-67.00, 71.46]

### Default (Fallback, No LSD)

All axes: min=-500, max=500, resolution=0.2 → 13 bits each.

### Fixed Encoders (Same on All Maps)

| Encoder | Min | Max | Resolution | Bits |
|---------|-----|-----|------------|------|
| HEALTH | 0 | 2000 | — | 11 |
| SHIELD_STRENGTH | 0 | 2000 | — | 11 |
| SHIELD_TYPE | 0 | armorTypeCount | — | 4 (for count ≤ 15) |
| HUMAN_STATE | 0 | 12 | — | 4 |
| HUMAN_SUB_STATE | 0 | 511 | — | 9 |
| CONTINUOUS_BOOLEAN_BITS | — | — | — | 4 (raw) |
| ANALOG_VALUES | -1 | 1 | 0.01 | 8 |

## Bit Count Examples

### Kotlin Soldier (Pre-fix, C&C_Under, 13+12+10 bits)

**Before fix**: modelName="s_a_human", animName="", 1 weapon (pistol)

| Section | Bits | Cumulative |
|---------|------|-----------|
| Header | 73 | 73 |
| Export_Creation | 131 (32+13+12+10+32+32) | 204 |
| Export_Rare | 329 (88+16+192+1+32) | 533 |
| Export_Occasional | 123 (1+11+11+4+32+64) | 656 |
| Export_Frequent | 187 (2+64+13+12+10+4+9+1+1+13+12+10+4+32) | **843** |

Note: Export_Occasional = 123 (1 weapon), Export_Frequent = 187 (has_weapon=true, adds 64 bits for weapon id+rounds).

### Kotlin Soldier (Post-fix, C&C_Under, 13+12+10 bits)

**After fix**: modelName="c_ag_nod_mg" (11 chars), animName="S_A_HUMAN.H_A_AINM" (18 chars), 1 weapon (pistol)

| Section | Bits | Notes |
|---------|------|-------|
| Header | 73 | Same |
| Export_Creation | 131 | Same position encoders |
| Export_Rare | 489 (104+160+192+1+32) | model 11 chars + anim 18 chars |
| Export_Occasional | 123 | 1 weapon (64 bits) |
| Export_Frequent | 187 | has_weapon=true |
| **Total** | **1003** | |

### C++ Soldier (Full, C&C_Under, CnC_Nod_Minigunner_0)

Real server data: modelName="c_ag_nod_mg" (11 chars), animName="S_A_HUMAN.H_A_C3A0" (18 chars), 2 weapons.

| Section | Bits | Notes |
|---------|------|-------|
| Header | 73 | Same |
| Export_Creation | 131 | Same position encoders |
| Export_Rare | 489 | model 11 chars + anim 18 chars |
| Export_Occasional | 187 | 2 weapons (128 bits) |
| Export_Frequent | 187 | has_weapon=true |
| **Total** | **1067** | Computed; actual captured = 1133 (66 bits unaccounted) |

**Bit count discrepancy note** (updated 2026-02-21): The original context claimed 843 vs 1133 = 290 bits difference. Accounting for known differences (model name +16, anim in Rare +144, 2 weapons vs 1 in Occasional +64) gives only 224 bits explained, leaving 66 bits unaccounted. Possible sources of the remaining 66 bits:

1. The C++ server may have had 2 weapons when the 1133-bit capture was made (vs the Kotlin server's 1 weapon). If the C++ server sent 3 real weapons (not 2), that would add another 64 bits.
2. Some encoder may have used a wider precision on the reference C++ server.
3. The 66 bits may be from a conditional path not exercised by the Kotlin soldier (e.g., different human state).

The root cause of the client ACK failure was not the bit count mismatch per se, but the **empty animName** sent in Export_Rare. The C++ client reads the animName and calls `AnimControl->Set_Animation()` on it. An empty string causes the animation system to fail, leaving the soldier in an uninitialized state. The client never sends an ACK because it cannot complete Import_Rare processing.

## C++ Source References

- `physicalgameobj.cpp:935-967` — Export_Creation: position + facing (NOT definitionId; factory writes that)
- `physicalgameobj.cpp:1007-1076` — Export_Rare: model, anim, frames, host, playerType, hudPokable, [hidden if vehicle]
- `damageablegameobj.cpp:312-320` — Export_Occasional: calls DefenseObject.Export
- `damage.cpp:1021-1041` — DefenseObjectClass::Export: isDead + health + shieldStr + shieldType
- `soldier.cpp:880-903` — Export_Rare: super + definitionId
- `soldier.cpp:935-975` — Export_Occasional: super + WeaponBag->Export_Weapon_List
- `soldier.cpp:853-878` — Export_Creation: just calls super (SmartGameObj → PhysicalGameObj)
- `soldier.cpp:977-1088` — Export_Frequent: in_vehicle, [has_weapon + weapon], position, humanState, [velocity], [animName], special_damage, then super
- `weaponbag.cpp:369-376` — Export_Weapon_List: (Count()-1) + weapons from slot 1 onward
- `weaponbag.cpp:347-365` — Import_Weapon_List: reads count, loops count times
- `smartgameobj.cpp:432-447` — Export_Frequent: super + Export_Control_Sc
- `smartgameobj.cpp:896-905` — Export_Creation: super + controlOwner
- `control.cpp:280-318` — Export_Sc: `#if 01` branch = ContinuousBooleanBits + 4 analog floats
- `armedgameobj.cpp:326-333` — Export_Frequent: super + targeting XYZ
- `physicalgameobj.cpp:1158-1168` — Export_Frequent: on_host_bone

## Encoder Precision Algorithm

Both C++ (`cEncoderTypeEntry::Calc_Bit_Precision`) and Kotlin (`EncoderRegistry.setPrecision`) use the same algorithm:

```
f_units = ceil((max - min) / resolution - EPSILON) + 1
bitPrecision = 0; max_units = 0
while max_units < f_units:
    max_units += 1 << bitPrecision
    bitPrecision++
    if bitPrecision == 1: max_units++  // 1 bit → 2 values
resolution = (max - min) / (max_units - 1)
```

The final resolution is recalculated to evenly span the range. Scaling: `round((value - min) / resolution)`. Unscaling: `min + scaled * resolution`.
