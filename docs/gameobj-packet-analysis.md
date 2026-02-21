# GAMEOBJ Packet Analysis from Real C++ Server

Analysis of all `classId=1000 (GAMEOBJ)` creation packets captured in `.tmp/real-server.log`.

Date: 2026-02-21

## Methodology

1. Parsed all 131 GAMEOBJ packets from the log (lines with `classId=1000(GAMEOBJ)`)
2. Extracted hex bytes from the hex dump lines preceding each payload line
3. Parsed the bitstream payload after the 7-byte wire header:
   - networkId: 32 bits
   - dirtyBits: 8 bits (0x0F for creation)
   - isDeletePending: 1 bit
   - classId: 32 bits (verified = 1000 / 0x3E8)
   - definitionId: 32 bits (first field of Export_Creation)
4. Resolved each definitionId to a name via `Objects.DDB` (parsed from `always.dat` using `DefinitionDbReader`)

All 131 packets parsed successfully with zero errors.

## Results: GAMEOBJ by definitionId

### Summary Table (31 unique definitionIds, 131 total objects)

| Count | defId | defId (hex) | classId (hex) | Name | Category |
|------:|------:|------------:|:-------------:|------|----------|
| 15 | 81960131 | 0x04E29CC3 | 0x0004010B | pct_zone_gdi | Script zone (purchase terminal) |
| 12 | 81960135 | 0x04E29CC7 | 0x0004010B | pct_zone_nod | Script zone (purchase terminal) |
| 11 | 81960209 | 0x04E29D11 | 0x0004010B | SFX.Arrows_Nod_Refinery | Cinematic/SFX arrows |
| 11 | 81960242 | 0x04E29D32 | 0x0004010B | SFX.Arrows_GDI_Barracks | Cinematic/SFX arrows |
| 10 | 81960241 | 0x04E29D31 | 0x0004010B | SFX.Arrows_GDI_Refinery | Cinematic/SFX arrows |
| 9 | 81960203 | 0x04E29D0B | 0x0004010B | SFX.Arrows_Nod_PowerPlant | Cinematic/SFX arrows |
| 9 | 81960240 | 0x04E29D30 | 0x0004010B | SFX.Arrows_GDI_PowerPlant | Cinematic/SFX arrows |
| 9 | 81960243 | 0x04E29D33 | 0x0004010B | SFX.Arrows_GDI_War_Factory | Cinematic/SFX arrows |
| 6 | 81960033 | 0x04E29C61 | 0x0004010B | Invisible_Object | Invisible/utility |
| 6 | 81960226 | 0x04E29D22 | 0x0004010B | SFX.Arrows_Hand_Of_NOD | Cinematic/SFX arrows |
| 6 | 81960233 | 0x04E29D29 | 0x0004010B | SFX.Arrows_Obelisk | Cinematic/SFX arrows |
| 4 | 81960239 | 0x04E29D2F | 0x0004010B | SFX.Arrows_Nod_Con_Yard | Cinematic/SFX arrows |
| 4 | 82080122 | 0x04E4717A | 0x00040129 | GDI_Ceiling_Gun_AGT | Vehicle (defense turret) |
| 2 | 82080123 | 0x04E4717B | 0x00040129 | Nod_Turret_MP | Vehicle (defense turret) |
| 1 | 81930257 | 0x04E22811 | 0x0004010F | CnC_Nod_Minigunner_0 | Soldier (Nod basic infantry) |
| 1 | 81950138 | 0x04E275BA | 0x00040107 | CnC_Ammo_Crate | PowerUp (ammo crate) |
| 1 | 81960139 | 0x04E29CCB | 0x0004010B | Obelisk Effect | Cinematic/SFX |
| 1 | 82080086 | 0x04E47156 | 0x00040129 | Nod_Obelisk | Vehicle (base defense) |
| 1 | 82080125 | 0x04E4717D | 0x00040129 | GDI_AGT | Vehicle (base defense) |
| 1 | 82080148 | 0x04E47194 | 0x00040129 | CnC_GDI_Harvester | Vehicle (harvester) |
| 1 | 82080149 | 0x04E47195 | 0x00040129 | CnC_Nod_Harvester | Vehicle (harvester) |
| 1 | 491530012 | 0x1D4C271C | 0x00040134 | mp_GDI_Advanced_Guard_Tower | Building (AGT) |
| 1 | 491530021 | 0x1D4C2725 | 0x00040134 | mp_Nod_Obelisk | Building (Obelisk) |
| 1 | 491540001 | 0x1D4C4E21 | 0x00040138 | mp_GDI_Refinery | Building (Refinery) |
| 1 | 491540002 | 0x1D4C4E22 | 0x00040138 | mp_Nod_Refinery | Building (Refinery) |
| 1 | 491550001 | 0x1D4C7531 | 0x0004013A | mp_GDI_Power_Plant | Building (PowerPlant) |
| 1 | 491550002 | 0x1D4C7532 | 0x0004013A | mp_Nod_Power_Plant | Building (PowerPlant) |
| 1 | 491560001 | 0x1D4C9C41 | 0x0004013C | mp_GDI_Barracks | Building (SoldierFactory) |
| 1 | 491560002 | 0x1D4C9C42 | 0x0004013C | mp_Hand_of_Nod | Building (SoldierFactory) |
| 1 | 491580001 | 0x1D4CEA61 | 0x00040140 | mp_Nod_Airstrip | Building (AirStrip) |
| 1 | 491590001 | 0x1D4D1171 | 0x00040142 | mp_GDI_War_Factory | Building (WarFactory) |

### Grouped by Object Category

#### Buildings (10 objects)
All from map data (`C&C_Under.mix`). One of each building per team.

| netId range | Name | classId |
|-------------|------|---------|
| 1563414-1563419 | mp_GDI_Advanced_Guard_Tower, mp_Nod_Obelisk, mp_GDI_Refinery, mp_Nod_Refinery, mp_GDI_Power_Plant, mp_Nod_Power_Plant, mp_GDI_Barracks, mp_Hand_of_Nod, mp_Nod_Airstrip, mp_GDI_War_Factory | 0x00040134..0x00040142 |

#### Cinematic/SFX Objects (81 objects)
These are `CinematicGameObj` instances (defClassId `0x0004010B`). They represent visual effects like directional arrows pointing to buildings and the Obelisk beam effect.

- 15x `pct_zone_gdi` -- purchase terminal zones for GDI
- 12x `pct_zone_nod` -- purchase terminal zones for Nod
- 11x `SFX.Arrows_Nod_Refinery`
- 11x `SFX.Arrows_GDI_Barracks`
- 10x `SFX.Arrows_GDI_Refinery`
- 9x `SFX.Arrows_Nod_PowerPlant`
- 9x `SFX.Arrows_GDI_PowerPlant`
- 9x `SFX.Arrows_GDI_War_Factory`
- 6x `Invisible_Object`
- 6x `SFX.Arrows_Hand_Of_NOD`
- 6x `SFX.Arrows_Obelisk`
- 4x `SFX.Arrows_Nod_Con_Yard`
- 1x `Obelisk Effect`

Note: `pct_zone_*` are purchase terminal interaction zones. `Invisible_Object` instances are likely script-controller objects.

#### Vehicles / Defense Turrets (10 objects)
All VehicleGameObj (defClassId `0x00040129`).

- 4x `GDI_Ceiling_Gun_AGT` -- turrets inside the AGT
- 2x `Nod_Turret_MP` -- turrets on Nod buildings
- 1x `Nod_Obelisk` -- the Obelisk weapon vehicle-entity
- 1x `GDI_AGT` -- the AGT weapon vehicle-entity
- 1x `CnC_GDI_Harvester`
- 1x `CnC_Nod_Harvester`

#### PowerUps (1 object)
- 1x `CnC_Ammo_Crate` (defClassId `0x00040107`)

#### Soldiers (1 object)
- 1x `CnC_Nod_Minigunner_0` (defClassId `0x0004010F`) -- the player's own soldier, spawned in response to the player joining

### netId Ranges

| netId range | Count | Description |
|-------------|-------|-------------|
| 100118-100274 | 75 | Static map objects (SFX arrows, zones, invisible objects) |
| 1500007-1500283 | 30 | More static objects + defense turrets |
| 1559644-1559647 | 4 | Buildings (first batch) |
| 1563414-1563419 | 6 | Buildings (second batch) |
| 1500000090-1500001957 | 15 | Dynamic objects (vehicles, harvesters, AGT, soldier) |

### Packet Size Patterns

| bitLength | Object types | Notes |
|-----------|-------------|-------|
| 218-219 | Buildings | Minimal payload, just position + building state |
| 458 | Cinematic/SFX, Invisible_Object, pct_zone_* | Standard creation payload |
| 490 | Cinematic/SFX (variant) | Slightly larger than 458 |
| 602 | CnC_Ammo_Crate | PowerUp with extra fields |
| 676-700 | Defense turrets (GDI_Ceiling_Gun_AGT, etc.) | Vehicle with weapon data |
| 698 | Obelisk Effect | Vehicle definition |
| 889 | Harvesters | Vehicle with full state |
| 1133 | CnC_Nod_Minigunner_0 (soldier) | Largest -- soldier has full state including weapon/animation/name |

## classId to Definition Class Mapping

The `classId` field in `Objects.DDB` (not to be confused with the network `classId=1000` for GAMEOBJ) identifies the C++ definition class:

| DDB classId | C++ Definition Class | Objects in capture |
|-------------|---------------------|-------------------|
| 0x0004010B | CinematicGameObjDef | SFX arrows, pct zones, Invisible_Object, Obelisk Effect |
| 0x0004010F | SoldierGameObjDef | CnC_Nod_Minigunner_0 |
| 0x00040107 | SimpleGameObjDef (PowerUp) | CnC_Ammo_Crate |
| 0x00040129 | VehicleGameObjDef | Turrets, harvesters, Obelisk, AGT |
| 0x00040134 | SAMSiteGameObjDef (BuildingAggregate) | mp_GDI_Advanced_Guard_Tower, mp_Nod_Obelisk |
| 0x00040138 | RefineryGameObjDef | mp_GDI_Refinery, mp_Nod_Refinery |
| 0x0004013A | PowerPlantGameObjDef | mp_GDI_Power_Plant, mp_Nod_Power_Plant |
| 0x0004013C | SoldierFactoryGameObjDef | mp_GDI_Barracks, mp_Hand_of_Nod |
| 0x00040140 | AirStripGameObjDef | mp_Nod_Airstrip |
| 0x00040142 | WarFactoryGameObjDef | mp_GDI_War_Factory |

## Implications for Kotlin Server

The C++ server creates these objects on map load for the `C&C_Under` map:

1. **Must implement**: Building spawning (10 buildings with correct definitionIds from Objects.DDB)
2. **Must implement**: Vehicle spawning for defense turrets and harvesters
3. **Must implement**: Cinematic/SFX object spawning (arrows, zones, invisible objects)
4. **Must implement**: PowerUp spawning (ammo crates)
5. **Already implemented**: Soldier spawning (CnC_Nod_Minigunner_0 / CnC_GDI_MiniGunner_0)

The SFX.Arrows_* objects are the directional arrows visible in the HUD pointing players toward buildings. The `pct_zone_*` objects are the purchase terminal interaction zones (walk-up menus). Both are `CinematicGameObjDef` instances that need `Export_Creation` support.
