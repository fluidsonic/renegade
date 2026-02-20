# C&C Renegade Kotlin Game Server - Phase 1: Network Foundation

## Context

Command & Conquer Renegade's source code (GPL, 2025 EA release) contains the original C++ multiplayer server. This project creates a new Kotlin+coroutines game server that is **wire-compatible** with the original game client. The server reimplements the custom UDP networking protocol, game object replication, combat, economy, buildings, vehicles, and full server-side physics.

**Key decisions:**
- LAN mode only (custom server list is a separate project)
- Kotlin-native scripting API (no native DLL loading)
- Full server-side physics simulation (120 Hz physics tick)
- Network update rate: configurable 5-30 Hz (original range), decoupled from physics
- Network layer first, then incrementally add game systems
- JDK 23 + Kotlin 2.1
- Multi-module Gradle project (no unnecessary prefixes)
- Idiomatic Kotlin naming (comments reference original C++ names)

---

## Phase 1 Goal

A server that original Renegade clients can discover on LAN, connect to, and maintain connections with.

## Sub-steps

### 1A: Project Setup + Math Primitives

Create the Gradle multi-module project and basic math types.

```
kotlin-server/
  build.gradle.kts              -- Kotlin 2.1, JDK 23, coroutines
  settings.gradle.kts
  math/                         -- Vector3, Quaternion, Matrix3D, OBBox
  net/                          -- Networking (core protocol)
  server/                       -- Server entry point + config
```

**Dependencies:** `kotlinx-coroutines-core`, `kotlin-test`, JUnit 5

**Files:**
- `math/src/main/kotlin/ccr/math/Vector3.kt`
- `math/src/main/kotlin/ccr/math/Quaternion.kt`
- `math/src/main/kotlin/ccr/math/Matrix3D.kt`

### 1B: BitStream (bit-level serialization)

Port `wwbitpack/BitPacker.cpp` exactly for wire compatibility. This is the **#1 risk item** -- bit ordering must match the C++ optimized version (MSB-first, Jani's 02-14-2002 rewrite).

**Package:** `ccr.net.bitstream`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `BitPacker` | `cBitPacker` | Raw bit array read/write on `ByteArray(548)` |
| `BitStream` | `BitStreamClass` | Typed add/get: Bool(1 bit), Byte, Short, Int, Float, WideString(UTF-16LE), TerminatedString, RawData, Vector3, Quaternion |
| `EncoderRegistry` | `cEncoderList` | Maps encoder IDs to precision specs (bits, min, max) for quantized encoding |

**Key constants:** `MAX_BUFFER_SIZE = 548`, bit positions track read/write independently.

**Reference files:**
- `original/Code/wwbitpack/BitPacker.h` / `.cpp`
- `original/Code/wwbitpack/bitstream.h` / `.cpp`
- `original/Code/wwbitpack/bitpackids.h`

### 1C: Wire Protocol

Port the packet header format and packet type definitions.

**Package:** `ccr.net.protocol`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `PacketType` (enum) | `packettype.h` | 8 types: UNRELIABLE(0), RELIABLE(1), ACK(2), KEEPALIVE(3), CONNECT_CS(4), ACCEPT_SC(5), REFUSAL_SC(6), FIREWALL_PROBE(7) |
| `PacketHeader` (data class) | `cPacket::Construct_Full_Packet` | 7-byte header: [Type:4bits][PacketID:28bits][SenderID:1byte][BitLength:2bytes] |
| `RefusalCode` (enum) | `REFUSAL_CODE` | GAME_FULL(1), BAD_PASSWORD(2), VERSION_MISMATCH(3), PLAYER_EXISTS(4), BY_APPLICATION(5) |

**Reference files:**
- `original/Code/wwnet/packettype.h`
- `original/Code/wwnet/wwpacket.h` / `.cpp`
- `original/Code/wwnet/connect.h` (REFUSAL_CODE)

### 1D: Packet Combining

Port `PacketManagerClass` -- combines multiple small packets into single UDP datagrams.

**Package:** `ccr.net.protocol`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `PacketCombiner` | `PacketManagerClass` | Pack: group same-sized packets per destination, 16-bit group header [count:5][size:10][more:1]. Unpack: split received datagrams back into individual packets. |

**Key constants:** MTU = 540 bytes, max 31 packets per group.

**Reference:** `original/Code/wwnet/packetmgr.h` / `.cpp`

### 1E: UDP Transport

Kotlin NIO-based UDP socket with coroutine integration.

**Package:** `ccr.net.transport`

| Kotlin class | Role |
|---|---|
| `UdpTransport` | `DatagramChannel` wrapper. Non-blocking, selector-based I/O loop. Inbound/outbound via `kotlinx.coroutines.channels.Channel`. |

### 1F: Reliable Transport

ACK-based reliability with dynamic resend timeout.

**Package:** `ccr.net.reliability`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `ReliableChannel` | reliable send/rcv lists in `cRemoteHost` | Send queue with ACK tracking, receive queue with in-order delivery. |
| `UnreliableChannel` | unreliable lists in `cRemoteHost` | Sequence tracking, stale packet discard (packets with ID < highest received are dropped). |
| `AckTracker` | ping tracking in `cRemoteHost` | Ping measurement from ACK round-trip (only from non-resent packets). |
| `ResendPolicy` | `Adjust_Resend_Timeout()` | Dynamic timeout: adapts to min(3*avg_ping, 1.3*max_ping), clamped [333, 3000]ms, reset every 2000ms. |

**Reference:** `original/Code/wwnet/rhost.h` / `.cpp`

### 1G: Connection Management

Handshake, keepalive, timeout, disconnect.

**Package:** `ccr.net.connection`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `RemoteHost` | `cRemoteHost` | Per-client state: reliable/unreliable channels, ack tracker, flow controller, stats, last contact time, loading flag. |
| `ConnectionManager` | `cConnection` | Server-side orchestrator: slot allocation (max 128 clients), packet dispatch by type, handshake processing, keepalive sending, timeout detection. |
| `Handshake` | `Process_Connection_Request()` | CONNECT_CS -> validate -> ACCEPT_SC (assigns client ID) or REFUSAL_SC. |

**Handshake wire format:**
- CONNECT_CS payload: nickname (wide string), password (wide string), exe_key (int), bandwidth (uint32)
- ACCEPT_SC payload: assigned client_id (int)
- REFUSAL_SC payload: refusal_code (int)

**Keepalive:** Reliable packet sent every `KEEPALIVE_TIMEOUT_MS`, contains packetloss% (float) + service_count (int).

**Timeout:** Connection broken after 15s silence (+ 45s allowance during map loading).

**Reference:** `original/Code/wwnet/connect.h` / `.cpp`

### 1H: Flow Control

Per-host bandwidth management.

**Package:** `ccr.net.flow`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `FlowController` | `Adjust_Flow_If_Necessary()` | Per-host threshold priority + bandwidth multiplier, flood detection. |
| `BandwidthBudget` | bandwidth in `cConnection` | Divide total server bandwidth among connected hosts. |

**Reference:** `original/Code/wwnet/rhost.cpp` (lines 336-486), `original/Code/wwnet/BWBalance.h`

### 1I: Network Object Replication Framework

Base classes and manager for the dirty-bit replication system.

**Package:** `ccr.net.replication`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `NetworkObject` | `NetworkObjectClass` | Abstract base. 4 dirty tiers (CREATION/RARE/OCCASIONAL/FREQUENT) with per-client tracking. Export/Import methods per tier. Cascading: CREATION includes all lower tiers. |
| `NetworkObjectManager` | `NetworkObjectMgrClass` | Object registry, ID allocation (dynamic: 1.5B-2.1B, static: 2.1B-2.11B, client: 2.11B+, 100K per client). Think loop. |
| `NetworkObjectFactory` | `NetworkObjectFactoryClass` | Factory registry: classId -> factory. Creates objects from incoming packets. |
| `DirtyBits` | per-client dirty arrays | `ByteArray(128)` tracking BIT_FREQUENT(0x01), BIT_OCCASIONAL(0x03), BIT_RARE(0x07), BIT_CREATION(0x0F). |
| `ReplicationPriority` | `cPriority` | Distance + facing + object type + tier weighting. Objects below threshold not sent. |

**Network class IDs** (from `netclassids.h`):
- Server-created: GAMEOBJ(1000), SCTEXTOBJ, PLAYERKILL, WIN, PURCHASERESPONSEEVENT, CONSOLECOMMANDEVENT, SVRGOODBYEEVENT, GAMEOPTIONSEVENT, EVICTIONEVENT, TEAM, PLAYER, etc.
- Client-created: CLIENTCONTROL, CSTEXTOBJ, SUICIDEEVENT, CHANGETEAMEVENT, PURCHASEREQUESTEVENT, CLIENTGOODBYEEVENT, etc.

**Reference:**
- `original/Code/wwnet/networkobject.h`
- `original/Code/wwnet/networkobjectmgr.h`
- `original/Code/wwnet/networkobjectfactory.h`
- `original/Code/Combat/netclassids.h`

### 1J: RCON (Remote Console)

Separate UDP admin interface.

**Package:** `ccr.net.rcon`

| Kotlin class | C++ origin | Role |
|---|---|---|
| `RconServer` | `ServerControlClass` | UDP listener on port 63999, password auth, 500-byte max messages, 60s timeout. Forward commands to console system. |
| `RconSession` | per-controller state | Tracks auth state and activity timer. |

**Reference:** `original/Code/SControl/servercontrol.h`

### 1K: Server Entry Point + LAN Discovery

**Package:** `ccr.server`

| Kotlin class | Role |
|---|---|
| `GameServer` | Main coroutine scope: launches UDP transport, connection manager, RCON, tick loops. |
| `ServerConfig` | INI-based config: port, max players, server name, password, bandwidth, RCON settings. |
| `LanBroadcastResponder` | Responds to LAN server browser queries. |

### Phase 1 Coroutine Architecture

```
coroutineScope {
    launch(Dispatchers.IO)     { udpTransport.ioLoop() }       // selector-based UDP I/O
    launch(singleThread)       { processInbound(transport) }    // packet dispatch (confined)
    launch(singleThread)       { networkTickLoop() }            // keepalive, resend, replication (configurable Hz)
    launch(singleThread)       { physicsTickLoop() }            // 120 Hz physics (future, stub for Phase 1)
    launch(Dispatchers.IO)     { rconServer.run() }             // RCON on separate socket
}
```

All `RemoteHost` and `ConnectionManager` state confined to single-thread dispatcher. IO loop communicates via coroutine channels. No shared mutable state, no mutexes.

### Phase 1 Verification

1. Unit tests: BitStream round-trip for all types, verify byte sequences against C++ output
2. Unit tests: PacketHeader encode/decode, PacketCombiner pack/unpack
3. Unit tests: ReliableChannel ACK/resend with simulated packet loss
4. Integration test: Start server, connect original Renegade client, verify handshake completes
5. Integration test: Client stays connected (keepalive exchange), clean disconnect
