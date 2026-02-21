# C&C Renegade Multiplayer Connection Sequence

## Overview

This document describes the full network handshake and game-state bootstrap sequence
between a Renegade client and server, as reverse-engineered from the original C++ source.

## Wire Protocol Layers

```
UDP datagram
  [CRC: 4 bytes LE]          ← WrapperCrc (packetmgr.cpp:894-914)
  [PacketCombiner header: 2 bytes]  ← numPackets:5, packetSize:10, morePackets:1
  [Packet wire bytes]
    [Type: 4 bits]            ← BITPACK_PACKET_TYPE (index 23)
    [Id: 28 bits]             ← BITPACK_PACKET_ID (index 24)
    [SenderId: 8 bits]
    [BitLength: 16 bits]
    [Payload: BitLength bits]
```

## Packet Types (PacketType enum)

| Type        | Direction | Description |
|-------------|-----------|-------------|
| CONNECT_CS  | C→S       | Initial connection request |
| ACCEPT_SC   | S→C       | Connection accepted + assigned slot ID |
| REFUSAL_SC  | S→C       | Connection refused |
| ACK         | both      | Acknowledge a reliable packet |
| KEEPALIVE   | both      | Heartbeat (also carries loss/rate stats) |
| RELIABLE    | both      | In-order delivery, must be ACKed |
| UNRELIABLE  | both      | Best-effort delivery (sequenced, no retry) |

## Connection Handshake

```
Client                               Server
  │                                     │
  │──── CONNECT_CS (id=0) ─────────────▶│  nickname + password + exeKey + bbo
  │◀─── ACK (id=0) ────────────────────│  always sent before processing
  │◀─── ACCEPT_SC (id=0) ──────────────│  assigned rhostId (MUST be first)
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
  │──── RELIABLE: CLIENTCONTROL ──────▶│  classId=1018
  │──── RELIABLE: CLIENTFPS ──────────▶│  classId=1032
  │──── RELIABLE: LOADINGEVENT ───────▶│  classId=1027, IsLoading=false
  │                                     │
  │    (client finishes loading)        │
  │──── RELIABLE: BIOEVENT ───────────▶│  classId=1026
  │◀─── RELIABLE: PLAYER (creation) ───│  classId=1011, name + team + stats
  │◀─── RELIABLE: GAMEDATAUPDATEEVENT ─│  classId=1012
  │──── ACK ──────────────────────────▶│
  │                                     │
  │◀───────── game stream ────────────▶│  RELIABLE + UNRELIABLE game objects
```

**Critical ordering**: ACCEPT_SC must be sent before any game objects. Player creation
is deferred to the BIOEVENT handler (after client finishes loading), not sent in
Connection_Handler. See [network.md](network.md) for full protocol reference.

## CONNECT_CS Payload (C→S)

```
[nickname: wideString]      ← non-empty required
[password: wideString]      ← empty permitted
[exeKey: int]               ← CRC32 of game executable
[bbo: int]                  ← bandwidth budget override (read by ConnectionManager)
```

## ACCEPT_SC Payload (S→C)

```
[rhostId: int]              ← assigned slot (1..maxPlayers)
```

## Network Object Payload (RELIABLE/UNRELIABLE carrying a game object)

All game objects (teams, player, events, game entities) use this structure, defined in
`messages.cpp Send_Object_Update`:

```
[networkId: int 32]         ← unique object ID (assigned by server at creation)
[dirtyBits: byte 8]         ← which data tiers are present (see below); C++: BYTE, not int
[isDeletePending: bool 1]   ← object is being destroyed
if (dirtyBits & 0x08):      ← BIT_CREATION
  [classId: int 32]         ← NETCLASSID_xxx (see netclassids.h)
  [factory prep data]       ← class-specific prep (usually empty)
  [Export_Creation data]
if (dirtyBits & 0x04):      ← BIT_RARE
  [Export_Rare data]
if (dirtyBits & 0x02):      ← BIT_OCCASIONAL
  [Export_Occasional data]
if (dirtyBits & 0x01):      ← BIT_FREQUENT
  [Export_Frequent data]
```

### Dirty Bit Constants (networkobject.h)

| Constant        | Value | Meaning |
|----------------|-------|---------|
| BIT_FREQUENT   | 0x01  | Real-time streaming (sent UNRELIABLE) |
| BIT_OCCASIONAL | 0x03  | Semi-frequent state (sent RELIABLE) |
| BIT_RARE       | 0x07  | Slow-changing state (sent RELIABLE) |
| BIT_CREATION   | 0x0F  | Full object creation (all tiers, RELIABLE) |

Each constant is cumulative — BIT_CREATION (0x0F) has all 4 bits set. When checking,
each bit is tested individually: `dirtyBits & 0x08` for creation, `& 0x04` for rare, etc.

## Network Class IDs (netclassids.h)

### Server → Client (1000–1017)

| ID   | Name                     | Notes |
|------|--------------------------|-------|
| 1000 | GAMEOBJ                  | Generic game object |
| 1001 | SCTEXTOBJ                | Server text message |
| 1002 | PLAYERKILL               | Kill notification |
| 1003 | WIN                      | Game over event |
| 1004 | PURCHASERESPONSEEVENT    | Purchase approval |
| 1005 | CONSOLECOMMANDEVENT      | Server console command |
| 1006 | RESETWINSEVENT           | Reset win counters |
| 1007 | SVRGOODBYEEVENT          | Server shutdown |
| 1008 | GAMEOPTIONSEVENT         | Game rules, map CRC |
| 1009 | EVICTIONEVENT            | Player eviction |
| 1010 | TEAM                     | Team state (NOD/GDI) |
| 1011 | PLAYER                   | Player state |
| 1012 | GAMEDATAUPDATEEVENT      | Dynamic game state update |
| 1013 | SCPINGRESPONSEEVENT      | Ping reply |
| 1014 | SCEXPLOSIONEVENT         | Explosion effect |
| 1015 | SCOBELISKEVENT           | Obelisk event |
| 1016 | SCANNOUNCEMENT           | Server announcement |
| 1017 | GAMESPYSCCHALLENGEEVENT  | GameSpy auth challenge |

### Client → Server (1018–1040)

| ID   | Name                           | Notes |
|------|-------------------------------|-------|
| 1018 | CLIENTCONTROL                  | Input state mirror |
| 1019 | CSTEXTOBJ                      | Chat message |
| 1020 | SUICIDEEVENT                   | Player suicide |
| 1021 | CHANGETEAMEVENT                | Team selection |
| 1022 | MONEYEVENT                     | Money transfer |
| 1023 | WARPEVENT                      | Teleport |
| 1024 | PURCHASEREQUESTEVENT           | Purchase request |
| 1025 | CLIENTGOODBYEEVENT             | Client disconnect |
| 1026 | BIOEVENT                       | Bio/special ability |
| 1027 | LOADINGEVENT                   | Map loading complete |
| 1028 | GODMODEEVENT                   | God mode toggle |
| 1029 | VIPMODEEVENT                   | VIP mode |
| 1030 | SCOREEVENT                     | Score update |
| 1031 | CLIENTBBOEVENT                 | Bandwidth event |
| 1032 | CLIENTFPS                      | FPS data |
| 1033 | CSPINGREQUESTEVENT             | Ping request |
| 1034 | CSDAMAGEEVENT                  | Damage notification |
| 1035 | REQUESTKILLEVENT               | Kill request |
| 1036 | CSCONSOLECOMMANDEVENT          | Client console command |
| 1037 | CSHINT                         | Client hint |
| 1038 | CSANNOUNCEMENT                 | Client announcement |
| 1039 | DONATEEVENT                    | Donation |
| 1040 | GAMESPYCSCHALLENGERESPONSEEVENT | GameSpy response |

## cTeam Object (classId=1010)

Sent for each team in Connection_Handler.

```
BIT_CREATION:
  [TeamNumber: int]   ← 0 = NOD, 1 = GDI

BIT_RARE:
  [Kills: int]
  [Deaths: int]

BIT_OCCASIONAL:
  [Score: float]
```

## cGameOptionsEvent (classId=1008)

One-time creation event sent in Connection_Handler. Object self-deletes after import.

```
BIT_CREATION:
  # Export_Tier_1_Data (gamedata.cpp):
  [IpAddress: int]
  [Owner: wideString]
  [GameTitle: wideString]
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
  [MapNameCRC: int]
  [ModNameCRC: int]
  # Export_Tier_2_Data (gamedata.cpp):
  [TimeLimitMinutes: int]
  [RadarMode: int]
  [IntermissionTimeSeconds: int]
  [MinQualifyingTimeMinutes: int]
  [IsFriendlyFirePermitted: bool]
  [IsFreeWeapons: bool]
  [IsClientTrusted: bool]
  [RemixTeams: bool]
  [CanRepairBuildings: bool]     ← only if Is_Cnc() = true
  [DriverIsAlwaysGunner: bool]   ← only if Is_Cnc() = true
  [SpawnWeapons: bool]           ← only if Is_Cnc() = true
  [Motd: wideString]
  # cGameOptionsEvent-specific:
  [TimeRemainingSeconds: float]
  [HostedGameNumber: int]
  [ModNameCRC: int]              ← duplicate of Tier1 value
  [MapNameCRC: int]              ← duplicate of Tier1 value
```

## cPlayer Object (classId=1011)

Sent per connected player. In the original engine these are sent via Server_Think (the object
update loop). Our server sends them in the connection handler for simplicity until Server_Think
is implemented.

```
BIT_CREATION:
  [Name: wideString]

BIT_RARE:
  [Id: int]
  [LadderPoints: int]
  [PlayerType: int]        ← team number, -1 = unassigned
  [DamageScaleFactor: int]
  [Ping: int]
  [IsInGame: bool]
  [Invulnerable: bool]
  [IsActive: bool]
  [WolRank: int]
  [NumWolGames: int]

BIT_OCCASIONAL:
  [Score: float]           ← from PlayerDataClass::Export_Occasional
  [Money: float]           ← from PlayerDataClass::Export_Occasional
  [Kills: int]
  [Deaths: int]
```

## Post-Connection Client State Machine

After receiving the GameOptionsEvent, the client:

1. Reads game rules and map/mod CRCs
2. Looks up the map by CRC (`ModPackageMgrClass::Get_Mod_Map_Name_From_CRC`)
3. If map not found → shows error dialog, disconnects
4. If map found → starts loading (`CombatManager::Load_Level`)
5. When loaded → sends LOADINGEVENT (classId=1027) with `IsLoading=false`
6. Server sets player as "in game", may show team selection

## What the Kotlin Server Sends

### On connection (Connection_Handler):
1. ACK for CONNECT_CS
2. ACCEPT_SC (reliable id=0) — slot assignment
3. TEAM NOD (reliable id=1) — networkId=100001, teamNumber=0
4. TEAM GDI (reliable id=2) — networkId=100002, teamNumber=1
5. GAMEOPTIONSEVENT (reliable id=3) — networkId=100003, game rules + real CRC values

### After BIOEVENT (post-load):
6. PLAYER (BIT_CREATION) — networkId=rhostId, name + team + isInGame=true
7. GAMEDATAUPDATEEVENT — timeRemaining + hostedGameNumber

MapNameCRC and ModNameCRC are computed using `crcStringi(mapName)` — C++ `CRC_Stringi` —
which is standard CRC32 over the uppercase bytes of the filename (e.g., `"C&C_Under.mix"`).
Known value: `crcStringi("C&C_Under.mix") == 0x2AFE0E38`.

**Known gaps:**
- Other connected players' PLAYER objects are not broadcast to new clients
- Server_Think object update loop not implemented (buildings, vehicles, etc. not streamed)
- SoldierGameObj spawn disabled while debugging encoding issues
