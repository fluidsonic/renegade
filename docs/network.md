# C&C Renegade Network Protocol Reference

Reverse-engineered from original C++ source. Kept up to date as the Kotlin server evolves.

## Wire Protocol Stack

```
UDP datagram
  ┌─ WrapperCrc (4 bytes LE)          packetmgr.cpp:894-914
  ├─ PacketCombiner header (2 bytes)   numPackets:5, packetSize:10, morePackets:1
  └─ Packet wire bytes
       ┌─ Type (4 bits)               BITPACK_PACKET_TYPE (index 23)
       ├─ Id (28 bits)                BITPACK_PACKET_ID (index 24)
       ├─ SenderId (8 bits)
       ├─ BitLength (16 bits)
       └─ Payload (BitLength bits)
```

**Wire header**: 4 + 28 + 8 + 16 = 56 bits = 7 bytes (before payload).

### WrapperCrc

Every datagram has a 4-byte CRC32 prefix (little-endian, byte-swapped). The CRC covers the
remaining bytes after the prefix. On receive, verify CRC before parsing.

### PacketCombiner

Multiple packets can be combined in a single datagram:
- Header: `[numPackets:5][packetSize:10][morePackets:1]` = 16 bits
- `packetSize` is in bytes, applies to the first packet
- If `morePackets=1`, another header follows for the next chunk
- **Delta encoding** (C++ server): when multiple packets of the same size are combined,
  subsequent packets are XOR-delta encoded against the previous (saves bandwidth for teams).
  The Kotlin server currently sends each packet individually without combining.

## Packet Types

| Value | Name         | Direction | Reliable | Description |
|-------|-------------|-----------|----------|-------------|
| 0     | UNRELIABLE  | both      | No       | Sequenced best-effort delivery |
| 1     | RELIABLE    | both      | Yes      | In-order delivery, must be ACKed |
| 2     | ACK         | both      | —        | Acknowledges a reliable/keepalive/accept packet |
| 3     | KEEPALIVE   | both      | Yes      | Heartbeat with loss/rate stats (64-bit payload) |
| 4     | CONNECT_CS  | C→S       | —        | Initial connection request |
| 5     | ACCEPT_SC   | S→C       | Yes      | Connection accepted, assigns slot ID |
| 6     | REFUSAL_SC  | S→C       | —        | Connection refused with reason code |

**Reliable ID space**: ACCEPT_SC, RELIABLE, and KEEPALIVE share a single per-connection
reliable ID counter. ACCEPT_SC occupies id=0, then RELIABLE starts at id=1.

## Connection Sequence

```
Client                               Server
  │                                     │
  │──── CONNECT_CS (id=0) ─────────────▶│  nickname + password + exeKey + bbo
  │◀─── ACK (id=0) ────────────────────│  always sent before processing
  │◀─── ACCEPT_SC (id=0) ──────────────│  assigned rhostId (MUST be sent first)
  │──── ACK (id=0) ───────────────────▶│
  │                                     │
  │  ┌─ Connection_Handler ────────┐    │
  │◀─│─ RELIABLE id=1: TEAM(NOD) ──│───│  classId=1010, teamNumber=0
  │◀─│─ RELIABLE id=2: TEAM(GDI) ──│───│  classId=1010, teamNumber=1
  │◀─│─ RELIABLE id=3: GAMEOPTIONSEVENT │  classId=1008, game rules + CRCs
  │  └─────────────────────────────┘    │
  │──── ACK (id=1..3) ────────────────▶│
  │                                     │
  │    (client loads map)               │
  │──── RELIABLE: CLIENTCONTROL ──────▶│  classId=1017
  │──── RELIABLE: CLIENTFPS ──────────▶│  classId=1031
  │──── RELIABLE: LOADINGEVENT ───────▶│  classId=1026, IsLoading=false
  │                                     │
  │    (client finishes loading)        │
  │──── RELIABLE: BIOEVENT ───────────▶│  classId=1025
  │                                     │  server: restoreDirtyBits(clientId)
  │                                     │  server: createPlayer() → BIT_CREATION dirty
  │◀─── RELIABLE: GAMEDATAUPDATEEVENT ─│  classId=1012 (one-shot, sent immediately)
  │                                     │
  │  ┌─ next replicationTick() ────┐    │
  │◀─│─ RELIABLE: BUILDING×N ──────│───│  classId=1000, writeCreation per building
  │◀─│─ RELIABLE: BASE_CTRL(NOD) ──│───│  classId=0, writeOccasionalUpdate only
  │◀─│─ RELIABLE: BASE_CTRL(GDI) ──│───│  classId=0, writeOccasionalUpdate only
  │◀─│─ UNRELIABLE: SERVERFPS ─────│───│  classId=0, writeFrequentUpdate only
  │◀─│─ RELIABLE: PLAYER×N+1 ──────│───│  classId=1011, writeCreation per player
  │◀─│─ RELIABLE: SOLDIER×N ───────│───│  classId=1000, writeCreation per soldier
  │  └─────────────────────────────┘    │
  │──── ACK ──────────────────────────▶│
  │                                     │
  │◀───────── game stream ────────────▶│  RELIABLE + UNRELIABLE game objects
```

**Critical ordering**: ACCEPT_SC MUST be sent before any RELIABLE game objects.
The client needs its rhostId to process subsequent packets. If ACCEPT_SC arrives late,
the client drops all earlier reliable packets (no LocalId → can't ACK), forcing a
full resend cycle.

Connection_Handler sends ONLY Teams and GameOptionsEvent — matching the C++
`cNetwork::Connection_Handler`. Buildings, base controllers, ServerFps, players, and
soldiers are NOT sent here; they reach the client via `replicationTick()` after
`restoreDirtyBits()` is called in the BIOEVENT handler.

## Payload Formats

### CONNECT_CS (C→S)

```
[nickname: wideString]      non-empty required
[password: wideString]      empty permitted
[exeKey: int]               CRC32 of game executable
[bbo: int]                  bandwidth budget override (read by ConnectionManager after app handler)
```

### ACCEPT_SC (S→C)

```
[rhostId: int]              assigned slot (1..maxPlayers)
```

### REFUSAL_SC (S→C)

```
[refusalCode: int]          see RefusalCode enum
```

Refusal codes: `CLIENT_ACCEPTED=0`, `GAME_FULL=1`, `VERSION_MISMATCH=2`, `BAD_PASSWORD=3`, `GAME_NOT_FOUND=4`

### KEEPALIVE (both)

```
[packetLossPc: float]       packet loss percentage (0.0–1.0)
[serviceRate: int]          packets serviced per second
```

### ACK

No payload. The `id` field in the packet header identifies which packet is being acknowledged.

## Network Object Envelope

All game objects (teams, players, events, entities) use this structure inside RELIABLE/UNRELIABLE payloads. Defined in `messages.cpp Send_Object_Update`:

```
[networkId: int 32]            unique object ID
[dirtyBits: byte 8]            which data tiers are present (BYTE, not int!)
[isDeletePending: bool 1]      compressed bool (1 bit)
if (dirtyBits & 0x08):         BIT_CREATION
  [classId: int 32]            NETCLASSID_xxx
  [Export_Creation data]
if (dirtyBits & 0x04):         BIT_RARE
  [Export_Rare data]
if (dirtyBits & 0x02):         BIT_OCCASIONAL
  [Export_Occasional data]
if (dirtyBits & 0x01):         BIT_FREQUENT
  [Export_Frequent data]
```

**Envelope size**: 32 + 8 + 1 = 41 bits minimum; 41 + 32 = 73 bits with BIT_CREATION.

### Dirty Bit Constants (networkobject.h)

| Constant       | Value | Bits set     | Usage |
|---------------|-------|-------------|-------|
| BIT_FREQUENT   | 0x01  | `....0001`  | Real-time streaming (UNRELIABLE) |
| BIT_OCCASIONAL | 0x03  | `....0011`  | Semi-frequent state (RELIABLE) |
| BIT_RARE       | 0x07  | `....0111`  | Slow-changing state (RELIABLE) |
| BIT_CREATION   | 0x0F  | `....1111`  | Full creation (all tiers, RELIABLE) |

Each constant is cumulative. BIT_CREATION has all 4 bits set. Each bit is tested individually.

## Network Class IDs

### Server → Client (1000–1016)

| ID   | Name                    | Description |
|------|------------------------|-------------|
| 1000 | GAMEOBJ                | Soldier/vehicle/building game object |
| 1001 | SCTEXTOBJ              | Server text message |
| 1002 | PLAYERKILL             | Kill notification |
| 1003 | WIN                    | Game over event |
| 1004 | PURCHASERESPONSEEVENT  | Purchase approval |
| 1005 | CONSOLECOMMANDEVENT    | Server console command |
| 1006 | RESETWINSEVENT         | Reset win counters |
| 1007 | SVRGOODBYEEVENT        | Server shutdown |
| 1008 | GAMEOPTIONSEVENT       | Game rules, map CRC (one-time creation) |
| 1009 | EVICTIONEVENT          | Player eviction |
| 1010 | TEAM                   | Team state (NOD/GDI) |
| 1011 | PLAYER                 | Player state |
| 1012 | GAMEDATAUPDATEEVENT    | Time remaining + hosted game number |
| 1013 | SCPINGRESPONSEEVENT    | Ping reply |
| 1014 | SCEXPLOSIONEVENT       | Explosion effect |
| 1015 | SCOBELISKEVENT         | Obelisk event |
| 1016 | SCANNOUNCEMENT         | Server announcement |

### Client → Server (1017–1038)

| ID   | Name                    | Description |
|------|------------------------|-------------|
| 1017 | CLIENTCONTROL          | Input state mirror |
| 1018 | CSTEXTOBJ              | Chat message |
| 1019 | SUICIDEEVENT           | Player suicide |
| 1020 | CHANGETEAMEVENT        | Team toggle NOD↔GDI |
| 1021 | MONEYEVENT             | Money transfer |
| 1022 | WARPEVENT              | Teleport |
| 1023 | PURCHASEREQUESTEVENT   | Purchase request |
| 1024 | CLIENTGOODBYEEVENT     | Client disconnect |
| 1025 | BIOEVENT               | Post-load trigger |
| 1026 | LOADINGEVENT           | Map loading state |
| 1027 | GODMODEEVENT           | God mode toggle |
| 1028 | VIPMODEEVENT           | VIP mode |
| 1029 | SCOREEVENT             | Score update |
| 1030 | CLIENTBBOEVENT         | Bandwidth event |
| 1031 | CLIENTFPS              | FPS data |
| 1032 | CSPINGREQUESTEVENT     | Ping request |
| 1033 | CSDAMAGEEVENT          | Damage notification |
| 1034 | REQUESTKILLEVENT       | Kill request |
| 1035 | CSCONSOLECOMMANDEVENT  | Client console command |
| 1036 | CSHINT                 | Client hint |
| 1037 | CSANNOUNCEMENT         | Client announcement |
| 1038 | DONATEEVENT            | Donation |

## Game Object Formats

### cTeam (classId=1010)

Sent for each team in Connection_Handler.

```
BIT_CREATION:  [TeamNumber: int]        0=NOD, 1=GDI
BIT_RARE:      [Kills: int] [Deaths: int]
BIT_OCCASIONAL:[Score: float]
BIT_FREQUENT:  (empty)
```

**Total bits (BIT_CREATION)**: 73 (header) + 32 (team) + 64 (kills+deaths) + 32 (score) = **201 bits**

### cGameOptionsEvent (classId=1008)

One-time creation event. Self-deletes after import.
C++ source: `gameoptionsevent.cpp:62-85`, `gamedata.cpp:715-742`, `gamedata.cpp:820-839`, `gdcnc.cpp:160-167`

```
BIT_CREATION:
  # cNetEvent::Export_Creation (empty)
  # cGameData::Export_Tier_1_Data:
  [IpAddress: int]
  [Owner: wideString]
  [GameTitle: wideString]            permitEmpty
  [Port: int]
  [CurrentPlayers: int]
  [MaxPlayers: int]
  [VersionNumber: int]
  [ExeCRC: int]
  [StringsCRC: int]
  [IsDedicated: bool]
  [IsTeamChangingAllowed: bool]
  [IsPassworded: bool]
  [IsLaddered: bool]
  [IsClanGame: bool]
  [MapNameCRC: int]                  CRC_Stringi(MapName)
  [ModNameCRC: int]                  CRC_Stringi(ModName)
  # cGameData::Export_Tier_2_Data:
  [TimeLimitMinutes: int]
  [RadarMode: int]
  [IntermissionTimeSeconds: int]
  [MinQualifyingTimeMinutes: int]
  [IsFriendlyFirePermitted: bool]
  [IsFreeWeapons: bool]
  [IsClientTrusted: bool]
  [RemixTeams: bool]
  [CanRepairBuildings: bool]         only if Is_Cnc()
  [DriverIsAlwaysGunner: bool]       only if Is_Cnc()
  [SpawnWeapons: bool]               only if Is_Cnc()
  [Motd: wideString]                 permitEmpty
  # cGameDataCnc::Export_Tier_2_Data (after parent):
  [BaseDestructionEndsGame: bool]
  [BeaconPlacementEndsGame: bool]
  [StartingCredits: int]
  # cGameOptionsEvent-specific:
  [TimeRemainingSeconds: float]
  [HostedGameNumber: int]
  [ModNameCRC: int]                  duplicate of Tier1 value
  [MapNameCRC: int]                  duplicate of Tier1 value
BIT_RARE:       (empty — cNetEvent)
BIT_OCCASIONAL: (empty — cNetEvent)
BIT_FREQUENT:   (empty — cNetEvent)
```

**Bit count** (with owner="Server", gameTitle="", motd=""):
- Header: 73
- Tier1: 32 + 112(owner) + 16(title) + 32*6 + 5(bools) + 32*2 = 421
- Tier2: 32*4 + 4(bools) + 3(cnc bools) + 16(motd) + 2(cnc bools) + 32(credits) = 185
- Event: 32*4 = 128
- **Total: 807 bits**

Note: C++ servers with longer owner/title/motd strings produce larger packets (e.g., 1255 bits).

### cPlayer (classId=1011)

```
BIT_CREATION:  [Name: wideString]
BIT_RARE:
  [Id: int]
  [LadderPoints: int]
  [PlayerType: int]            team number (0=NOD, 1=GDI, -1=unassigned)
  [DamageScaleFactor: int]
  [Ping: int]
  [IsInGame: bool]
  [Invulnerable: bool]
  [IsActive: bool]
  [WolRank: int]
  [NumWolGames: int]
BIT_OCCASIONAL:
  [Score: float]               from PlayerDataClass
  [Money: float]               from PlayerDataClass
  [Kills: int]
  [Deaths: int]
BIT_FREQUENT:  (empty)
```

### cGameDataUpdateEvent (classId=1012)

Sent after BIOEVENT to signal gameplay can proceed.

```
BIT_CREATION:
  [TimeRemainingSeconds: int]    NOTE: int, not float!
  [HostedGameNumber: int]
```

**Total bits**: 73 (header) + 64 (data) = **137 bits**

### cBioEvent (classId=1025, C→S)

Post-load trigger sent by client after `Load_Level()` completes.
C++ source: `bioevent.cpp`

```
BIT_CREATION:
  [SenderId: int]
  [Nickname: wideString]
  [TeamChoice: int]
  [ClanID: uint32]
  [MapName: string]           terminated string (char, not wide)
```

### cScPingResponseEvent (classId=1013, S→C)

Server's reply to CSPINGREQUESTEVENT.

```
BIT_CREATION:  [PingNumber: int]
```

### cChangeteamEvent (classId=1020, C→S)

Toggles player's team NOD↔GDI. `Act()` reads `SenderId` and flips the team.

```
BIT_CREATION:  [SenderId: int]
```

### cLoadingEvent (classId=1026, C→S)

```
BIT_CREATION:  [SenderId: int] [IsLoading: bool]
```

### cCsPingRequestEvent (classId=1032, C→S)

```
BIT_CREATION:  [SenderId: int] [PingNumber: int]
```

### cClientControl (classId=1017, C→S)

```
BIT_CREATION:   [ClientId: int]
BIT_FREQUENT:   (input state — not yet documented)
```

### cClientFps (classId=1031, C→S)

```
BIT_CREATION:   [ClientId: int]
BIT_FREQUENT:   [Fps: byte]
```

### cScTextObj (classId=1001, S→C)

```
BIT_CREATION:
  [Type: byte]                TextMessageEnum
  [SenderId: int]
  [RecipientId: int]
  [IsHostAdminMessage: bool]
  [Text: wideString]
```

### cPlayerKill (classId=1002, S→C)

```
BIT_CREATION:  [KillerId: int] [VictimId: int]
```

### cPurchaseRequestEvent (classId=1023, C→S)

```
BIT_CREATION:
  [SenderId: int]
  [PurchaseType: int]         VendorClass::PURCHASE_TYPE enum
  [ItemIndex: int]
  [AltSkinIndex: int]
```

### cPurchaseResponseEvent (classId=1004, S→C)

```
BIT_CREATION:  [PurchaserId: int] [ResponseId: int]
```

### cEvictionEvent (classId=1009, S→C)

```
BIT_CREATION:  [EvictionCode: int]
```

### cClientGoodbyeEvent (classId=1024, C→S)

```
BIT_CREATION:  [SenderId: int]
```

### cSvrGoodbyeEvent (classId=1007, S→C)

```
BIT_CREATION:  [IsQuickFullExitRequested: bool]
```

## Data Types in BitStream

| Type | Bits | Encoding |
|------|------|----------|
| int | 32 | Big-endian |
| float | 32 | IEEE 754, big-endian bit order |
| byte | 8 | Single byte |
| bool | 1 | Compressed boolean (1 bit) |
| wideString | 16 + N*16 | Length prefix (uint16) + N wide chars (uint16 each) |
| wideString(permitEmpty) | same | Length=0 allowed |

## Client State Machine

After receiving GameOptionsEvent, the client:
1. Reads game rules and map/mod CRCs
2. Looks up map by CRC (`ModPackageMgrClass::Get_Mod_Map_Name_From_CRC`)
3. If not found → error dialog, disconnect
4. If found → loads map (`CombatManager::Load_Level`)
5. Sends CLIENTCONTROL (classId=1017) and CLIENTFPS (classId=1031)
6. Sends LOADINGEVENT (classId=1026, IsLoading=false)
7. Sends BIOEVENT (classId=1025) — post-load trigger
8. Server: restores dirty bits, creates Player, sends GAMEDATAUPDATEEVENT immediately;
   next replicationTick() delivers buildings, base controllers, ServerFps, players, soldiers
9. Client enters gameplay, sends CHANGETEAMEVENT for team selection

## Kotlin Server Replication Architecture

### Single replication path: dirty bits → replicationTick()

The Kotlin server replication model mirrors C++ `Tell_Client_About_Dynamic_Objects`:

- `replicationTick()` iterates ALL registered `NetworkObject`s
- For each object and each connected client, it checks per-client dirty bits
- Sends the appropriate packet tier (creation → rare → occasional → frequent)
- Clears the dirty bits for that client after sending
- Reliable packets (BIT_CREATION / BIT_RARE / BIT_OCCASIONAL) go via `sendGameNetObj`
- Unreliable packets (BIT_FREQUENT only) go via `sendUnreliable`
- A soldier's BIT_FREQUENT update is never sent to its own `controlOwner` (client is authoritative)

Objects set their dirty bits at registration time, ensuring the first `replicationTick()`
after a new client joins sends everything that client needs.

### creationDirtyBit — per-class maximum dirty level for new clients

Each `NetworkObject` subclass declares a `creationDirtyBit` property. When
`NetworkObjectManager.restoreDirtyBits(clientId)` is called (in the BIOEVENT handler),
each object is marked dirty at its `creationDirtyBit` level for that client.

| Object type          | classId | creationDirtyBit  | Packet sent by replicationTick() |
|----------------------|---------|-------------------|---------------------------------|
| BuildingGameObj      | 1000    | BIT_CREATION      | writeCreation (RELIABLE) |
| Player               | 1011    | BIT_CREATION      | writeCreation (RELIABLE) |
| SoldierGameObj       | 1000    | BIT_CREATION      | writeCreation (RELIABLE) |
| BaseControllerClass  | 0       | BIT_OCCASIONAL    | writeOccasionalUpdate (RELIABLE) |
| ServerFps            | 0       | BIT_FREQUENT      | writeFrequentUpdate (UNRELIABLE) |

**classId=0 objects must never receive a creation packet.** The client has no factory for
classId=0 — `Create_Network_Object(0)` returns null, causing a crash. `BaseControllerClass`
and `ServerFps` are instantiated by the client locally during level load and only need state
updates. Their `creationDirtyBit` is set to `BIT_OCCASIONAL` / `BIT_FREQUENT` so
`replicationTick()` never emits a BIT_CREATION packet for them.

### BIOEVENT handler flow

When the server receives BIOEVENT from a client:
1. Client is added to `playerInGame`
2. `NetworkObjectManager.restoreDirtyBits(clientId)` — marks all registered objects dirty at their `creationDirtyBit` level for this client
3. Team dirty bits are cleared (Teams were already sent in Connection_Handler)
4. `createPlayer()` is called — registers a new `Player` object with `BIT_CREATION` dirty for ALL clients
5. `GameDataUpdateEvent` is sent immediately (one-shot)
6. Next `replicationTick()` sends all objects to the new client (and the new Player to all clients)

### Deletion via setDeletePending()

`deleteSoldier()` and similar calls use `setDeletePending()`. The centralized delete-pending
loop in `networkTickLoop` broadcasts a deletion packet to all in-game clients and then
unregisters the object. Manual `writeDeletion` calls are not used.

### On connection (Connection_Handler):
1. ACCEPT_SC (reliable id=0) — slot assignment
2. TEAM NOD (reliable id=1) — networkId=100001, teamNumber=0
3. TEAM GDI (reliable id=2) — networkId=100002, teamNumber=1
4. GAMEOPTIONSEVENT (reliable id=3) — networkId=100003, game rules + CRCs

### After BIOEVENT (immediate, before replicationTick):
5. GAMEDATAUPDATEEVENT — timeRemaining + hostedGameNumber

### After BIOEVENT (next replicationTick, dirty-bit-driven):
6. BUILDING×N (BIT_CREATION, RELIABLE) — one per building in the level
7. BASE_CTRL NOD + GDI (BIT_OCCASIONAL, RELIABLE) — state update only, no creation
8. SERVERFPS (BIT_FREQUENT, UNRELIABLE) — frequent update only, no creation
9. PLAYER for new client + all existing players (BIT_CREATION, RELIABLE)
10. SOLDIER×N for all existing soldiers (BIT_CREATION, RELIABLE)

### Network IDs:
- NOD Team: 100001
- GDI Team: 100002
- GameOptionsEvent: 100003
- Events: 100010+ (incrementing)
- Players: rhostId
- Soldiers: 200000 + rhostId
- ServerFps: NET_ID_SERVER_FPS = 2,100,000,006

MapNameCRC and ModNameCRC use `crcStringi()` — CRC32 over uppercase bytes.
Known: `crcStringi("C&C_Under.mix") == 0x2AFE0E38 == 721292856`.

## Verified Against C++ Server

Packet capture comparison (MiniClient → C++ server at 192.168.1.210:4848):

| Object | C++ bits | Kotlin bits | Match |
|--------|----------|-------------|-------|
| Team NOD | 201 | 201 | Field layout identical (networkId differs) |
| Team GDI | 201 | 201 | Field layout identical |
| GameOptionsEvent | 1255 | 807 | Layout identical; size diff = string lengths |
| GameDataUpdateEvent | — | 137 | Not sent by C++ to MiniClient |

The C++ server's longer GameOptionsEvent (1255 bits vs 807) is due to a longer server
owner name. All fields match in type and order.

### cServerFps (classId=0, S→C)

No `Get_Network_Class_ID()` override — returns 0 (base class default).
Identified by static `networkId=NETID_SERVER_FPS` and `AppPacketType=APPPACKETTYPE_SERVERFPS`.
Singleton — `setDeletePending()` is a no-op; persists the entire game session.

**`creationDirtyBit = BIT_FREQUENT`** — `replicationTick()` only ever sends a frequent
update for this object (UNRELIABLE). No creation packet is sent; the client instantiates
its `CServerFps` object locally during level load.

```
BIT_FREQUENT: [Fps: int]
```

### BaseControllerClass (classId=0, S→C)

No `Get_Network_Class_ID()` override — returns 0.
Identified by static networkId (`NETID_GDI_BASE_CONTROLLER` or `NETID_NOD_BASE_CONTROLLER`)
and `AppPacketType=APPPACKETTYPE_BASECONTROLLER`. One instance per team. Singleton.

**`creationDirtyBit = BIT_OCCASIONAL`** — `replicationTick()` only ever sends an occasional
update for this object (RELIABLE). No creation packet is sent; the client instantiates its
`BaseControllerClass` objects locally during level load.

```
BIT_OCCASIONAL:
  [OperationTimeFactor: float]
  [IsBasePowered: bool]
  [CanGenerateSoldiers: bool]
  [CanGenerateVehicles: bool]
  [IsBaseDestroyed: bool]
  [DidBeaconDestroyBase: bool]
  [IsRadarEnabled: bool]
```

### BuildingGameObj (classId=1000/NETCLASSID_GAMEOBJ, S→C)

C++ hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj.
Does NOT extend PhysicalGameObj — no model name, animation, facing, or control ownership.
`definitionId` is normally written by `NetworkGameObjectFactoryClass::Prep_Packet` before
`Export_Creation` in C++; in Kotlin it is written at the start of `exportCreation`.

```
BIT_CREATION:
  [definitionId: int]                           written first (factory Prep_Packet in C++)
  [Position.X: float(BITPACK_WORLD_POSITION_X)]
  [Position.Y: float(BITPACK_WORLD_POSITION_Y)]
  [Position.Z: float(BITPACK_WORLD_POSITION_Z)]
  [CollectionSphere.Center.X: float(BITPACK_WORLD_POSITION_X)]
  [CollectionSphere.Center.Y: float(BITPACK_WORLD_POSITION_Y)]
  [CollectionSphere.Center.Z: float(BITPACK_WORLD_POSITION_Z)]
  [CollectionSphere.Radius: float(BITPACK_BUILDING_RADIUS)]  range (0, 50, 0.1) → 9 bits

BIT_RARE:
  [IsDestroyed: bool]
  [IsPowerOn: bool]
  [CurrentState: int(BITPACK_BUILDING_STATE)]   range (-1, 10, 1.0) → 4 bits
  # BuildingState enum: 0=HEALTH100_POWERON … 4=DESTROYED_POWERON, 5-9 POWEROFF variants

BIT_OCCASIONAL: (from DamageableGameObj)
  [IsDead: bool]
  [Health: int(BITPACK_HEALTH)]
  [ShieldStrength: int(BITPACK_SHIELD_STRENGTH)]
  [ShieldType: int(BITPACK_SHIELD_TYPE)]

BIT_FREQUENT: (empty)
```

**Bit counts** (default ±500/0.2 world position encoder = 13 bits/axis):
- Creation: 32 + 3×13 + 3×13 + 9 = **119 bits**
- Rare: 1 + 1 + 4 = **6 bits**
- Occasional: 1 + 11 + 11 + 4 = **27 bits**
- Full creation packet: 73 (header) + 119 + 6 + 27 = **225 bits**

**Encoder precision** (from C++ `Set_Precision` calls):
- `BITPACK_BUILDING_RADIUS`: (0.0, 50.0, 0.1) — set in `combatgmode.cpp`
- `BITPACK_BUILDING_STATE`: (-1, 10, 1.0) — set in `BuildingGameObj::Set_Precision`

### VehicleGameObj (classId=1000/NETCLASSID_GAMEOBJ, S→C)

C++ hierarchy: NetworkObject → BaseGameObj → PhysicalGameObj → DamageableGameObj
                             → ArmedGameObj → SmartGameObj → VehicleGameObj

**Vehicle types**: CAR=0, TANK=1, BIKE=2, FLYING=3, TURRET=4

**Encoder precision** (from C++ `VehicleGameObj::Set_Precision`):
- `BITPACK_VEHICLE_VELOCITY`: (-90, 90, 0.01) → 15 bits/axis
- `BITPACK_VEHICLE_ANGULAR_VELOCITY`: (-20, 20, 0.01) → 12 bits/axis
- `BITPACK_VEHICLE_QUATERNION`: (-1, 1, 0.0005) → 12 bits/component
- `BITPACK_VEHICLE_LOCK_TIMER`: (0, 16, 0.25) → 7 bits

```
BIT_CREATION: (SmartGameObj chain first)
  [definitionId: int]
  [Position.xyz: float(BITPACK_WORLD_POSITION_X/Y/Z)]   13 bits each
  [Facing: float]                                         32 bits
  [ControlOwner: int]
  # VehicleGameObj appends:
  [LockOwnerId: int]
  if LockOwnerId != 0: [LockTimer: float(BITPACK_VEHICLE_LOCK_TIMER)]  7 bits

BIT_RARE: (PhysicalGameObj chain first)
  [ModelName: terminatedString]                  16 + len*8 bits
  [AnimName: terminatedString]                   16 + len*8 bits
  [CurrFrame: int]
  [TargetFrame: int]
  [AnimMode: int]
  [HostModelId: int]
  [HostBone: int]
  [PlayerType: int]                              team via SmartGameObj.Get_Player_Type()
  [HudPokable: bool]
  [Hidden: bool]                                 written by PhysicalGameObj only for vehicles
  # VehicleGameObj appends:
  [SeatOccupantIds[0..seatCount-1]: int each]   -1 if empty
  [VehicleDelivered: bool]

BIT_FREQUENT:
  [TotalRounds: int]
  if vehicleType in [CAR, TANK, BIKE, FLYING]:
    [IsEngineOn: bool]
    [Position.xyz: float(BITPACK_WORLD_POSITION_X/Y/Z)]  13 bits each
    [Quaternion.xyzw: float(BITPACK_VEHICLE_QUATERNION)]  12 bits each = 48 bits
    [Velocity.xyz: float(BITPACK_VEHICLE_VELOCITY)]       15 bits each = 45 bits
    [AngularVelocity.xyz: float(BITPACK_VEHICLE_ANGULAR_VELOCITY)]  12 bits each = 36 bits
  [DriverIsGunner: bool]
  # SmartGameObj chain:
  [OnHostBone: bool]                             from PhysicalGameObj
  [Targeting.xyz: float(BITPACK_WORLD_POSITION_X/Y/Z)]   39 bits — from ArmedGameObj
  [ContinuousBoolBits: 4 bits]                   from SmartGameObj
  [4× AnalogValue: byte each]                    4×8 = 32 bits — from SmartGameObj
```

**Frequent bit totals**:
- Physics vehicle (CAR/TANK/BIKE/FLYING): 32 + 1 + 39 + 48 + 45 + 36 + 1 + 76 = **278 bits**
- Turret: 32 + 1 + 76 = **109 bits**

**Creation bit totals** (default encoders):
- Unlocked: 32 + 39 + 32 + 32 + 32 = **167 bits**
- Locked: 167 + 7 = **174 bits**

### StaticNetworkObject Hierarchy (classId=0, S→C)

All static network objects return `networkClassId=0` (no C++ override). They are identified by
well-known `networkId` values (not classId). `setDeletePending()` is a no-op for all.

#### StaticNetworkObject (abstract base)
```
BIT_RARE:
  [AnimationMode: int]
  [LoopStart: float]
  [LoopEnd: float]
  [CurrFrame: float]
  [TargetFrame: float]
```

#### DoorNetworkObject (classId=0)
Well-known networkId. Does NOT call super.exportRare (state-based).
Precision: `BITPACK_DOOR_STATE` (0, 5, 1.0) → 3 bits.
Door states: CLOSED=0, OPENED=1, OPENING=2, CLOSING=3, ACCESS_DENIED=4.
```
BIT_RARE:
  [DoorState: int(BITPACK_DOOR_STATE)]   3 bits
```

#### ElevatorNetworkObject (classId=0)
Does NOT call super.exportRare.
```
BIT_RARE:
  [State: int]
  [DoorStateTop: int]
  [DoorStateBottom: int]
```

#### DsapoNetworkObject (classId=0)
DSAPONetworkObjectClass — Destructible Static Anim Physics Object.
DOES call super.exportRare, then appends defense fields.
```
BIT_RARE: (StaticNetworkObject fields first, then DefenseObject.Export)
  [AnimationMode: int]
  [LoopStart: float]
  [LoopEnd: float]
  [CurrFrame: float]
  [TargetFrame: float]
  [IsDead: bool]
  [Health: int(BITPACK_HEALTH)]
  [ShieldStrength: int(BITPACK_SHIELD_STRENGTH)]
  [ShieldType: int(BITPACK_SHIELD_TYPE)]
```

### BackgroundMgr (classId=0, S→C)

C++ class: `BackgroundMgrClass`. Well-known `networkId=NETID_SERVER_BACKGROUND`
(`NETID_STATIC_OBJECT_MIN + 3`). Singleton.

```
BIT_RARE: (8 parameters × 4 floats + 3 override counts = 1120 bits total)
  # Parameters in order: SKY_TINT_FACTOR, CLOUD_COVER, CLOUD_GLOOMINESS,
  #   LIGHTNING_INTENSITY, LIGHTNING_START_DISTANCE, LIGHTNING_END_DISTANCE,
  #   LIGHTNING_HEADING, LIGHTNING_DISTRIBUTION
  # For each parameter:
  [NormalTarget: float]
  [NormalDuration: float]
  [OverrideTarget: float]
  [OverrideDuration: float]
  # After all 8 parameters:
  [CloudOverrideCount: int]       uint in C++
  [LightningOverrideCount: int]   uint in C++
  [SkyTintOverrideCount: int]     uint in C++
```

**Total rare bits**: 8 × 4 × 32 + 3 × 32 = **1120 bits**

### WeatherMgr (classId=0, S→C)

C++ class: `WeatherMgrClass`. Well-known `networkId=NETID_SERVER_WEATHER`
(`NETID_STATIC_OBJECT_MIN + 2`). Singleton. Reuses `BackgroundParameter` structure.

```
BIT_RARE: (6 parameters × 4 floats + 2 override counts = 832 bits total)
  # Parameters in order: WIND_HEADING, WIND_SPEED, WIND_VARIABILITY,
  #   RAIN_DENSITY, SNOW_DENSITY, ASH_DENSITY
  # For each parameter: 4 floats (NormalTarget, NormalDuration, OverrideTarget, OverrideDuration)
  [WindOverrideCount: int]          uint in C++
  [PrecipitationOverrideCount: int] uint in C++
```

**Total rare bits**: 6 × 4 × 32 + 2 × 32 = **832 bits**

### cScExplosionEvent (classId=1014, S→C)

One-shot event. Extends `NetEvent`.

```
BIT_CREATION:
  [DefId: int]                                  explosion definition ID
  [Position.X: float(BITPACK_WORLD_POSITION_X)]
  [Position.Y: float(BITPACK_WORLD_POSITION_Y)]
  [Position.Z: float(BITPACK_WORLD_POSITION_Z)]
  [OwnerId: int]                                networkId of entity that caused explosion
```

### cScObeliskEvent (classId=1015, S→C)

Identical wire format to `cScExplosionEvent`. One-shot event. Extends `NetEvent`.

```
BIT_CREATION:
  [DefId: int]                                  obelisk beam definition ID
  [Position.X: float(BITPACK_WORLD_POSITION_X)]
  [Position.Y: float(BITPACK_WORLD_POSITION_Y)]
  [Position.Z: float(BITPACK_WORLD_POSITION_Z)]
  [OwnerId: int]                                networkId of entity that fired the obelisk
```

---

## NETCLASSID Table (from netclassids.h)

Sequential enum starting at 1000:

| ID   | Constant                      | Class                         |
|------|-------------------------------|-------------------------------|
| 1000 | NETCLASSID_GAMEOBJ            | BaseGameObj (soldier/vehicle/building) |
| 1001 | NETCLASSID_SCTEXTOBJ          | cScTextObj                    |
| 1002 | NETCLASSID_PLAYERKILL         | cPlayerKill                   |
| 1003 | NETCLASSID_WIN                | cWinEvent                     |
| 1004 | NETCLASSID_PURCHASERESPONSEEVENT | cPurchaseResponseEvent      |
| 1005 | NETCLASSID_CONSOLECOMMANDEVENT | cConsoleCommandEvent          |
| 1006 | NETCLASSID_RESETWINSEVENT     | — (no implementing class)     |
| 1007 | NETCLASSID_SVRGOODBYEEVENT    | cSvrGoodbyeEvent              |
| 1008 | NETCLASSID_GAMEOPTIONSEVENT   | cGameOptionsEvent             |
| 1009 | NETCLASSID_EVICTIONEVENT      | cEvictionEvent                |
| 1010 | NETCLASSID_TEAM               | cTeam                         |
| 1011 | NETCLASSID_PLAYER             | cPlayer                       |
| 1012 | NETCLASSID_GAMEDATAUPDATEEVENT | cGameDataUpdateEvent         |
| 1013 | NETCLASSID_SCPINGRESPONSEEVENT | cScPingResponseEvent         |
| 1014 | NETCLASSID_SCEXPLOSIONEVENT   | cScExplosionEvent             |
| 1015 | NETCLASSID_SCOBELISKEVENT     | cScObeliskEvent               |
| 1016 | NETCLASSID_SCANNOUNCEMENT     | SCAnnouncement                |
| 1017 | NETCLASSID_CLIENTCONTROL      | CClientControl                |
| 1018 | NETCLASSID_CSTEXTOBJ          | cCsTextObj                    |
| 1019 | NETCLASSID_SUICIDEEVENT       | cSuicideEvent                 |
| 1020 | NETCLASSID_CHANGETEAMEVENT    | cChangeTeamEvent              |
| 1021 | NETCLASSID_MONEYEVENT         | cMoneyEvent                   |
| 1022 | NETCLASSID_WARPEVENT          | cWarpEvent                    |
| 1023 | NETCLASSID_PURCHASEREQUESTEVENT | cPurchaseRequestEvent       |
| 1024 | NETCLASSID_CLIENTGOODBYEEVENT | cClientGoodbyeEvent           |
| 1025 | NETCLASSID_BIOEVENT           | cBioEvent                     |
| 1026 | NETCLASSID_LOADINGEVENT       | cLoadingEvent                 |
| 1027 | NETCLASSID_GODMODEEVENT       | cGodModeEvent                 |
| 1028 | NETCLASSID_VIPMODEEVENT       | cVipModeEvent                 |
| 1029 | NETCLASSID_SCOREEVENT         | cScoreEvent                   |
| 1030 | NETCLASSID_CLIENTBBOEVENT     | cClientBboEvent               |
| 1031 | NETCLASSID_CLIENTFPS          | CClientFps                    |
| 1032 | NETCLASSID_CSPINGREQUESTEVENT | cCsPingRequestEvent           |
| 1033 | NETCLASSID_CSDAMAGEEVENT      | cCsDamageEvent                |
| 1034 | NETCLASSID_REQUESTKILLEVENT   | cRequestKillEvent             |
| 1035 | NETCLASSID_CSCONSOLECOMMANDEVENT | cCsConsoleCommandEvent     |
| 1036 | NETCLASSID_CSHINT             | cCsHint                       |
| 1037 | NETCLASSID_CSANNOUNCEMENT     | CSAnnouncement                |
| 1038 | NETCLASSID_DONATEEVENT        | cDonateEvent                  |

Static objects (DoorNetworkObject, ElevatorNetworkObject, DsapoNetworkObject,
BackgroundMgrClass, WeatherMgrClass, cServerFps, BaseControllerClass) return
classId=0 and are identified by well-known `networkId` values.

## Known Gaps

- PacketCombiner delta encoding not used for outgoing packets
- VehicleGameObj BITPACK_VEHICLE_* encoder precision not yet registered at server startup
- BuildingGameObj BITPACK_BUILDING_RADIUS/STATE encoder precision not yet registered at startup
- DOOR_STATE C++↔Kotlin encoding mismatch: C++ uses quantized scaled mapping, Kotlin uses raw 3-bit (Kotlin-only tests pass but C++ client interop unverified)
- C4GameObj importRare round-trip only tested Kotlin→Kotlin; C++ client interop unverified
- CinematicGameObj is singleplayer-only; network wire format not verified against live traffic
