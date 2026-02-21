# Phase 1 Completion: Steps 1J (RCON) + 1K (Server Entry Point + LAN Discovery)

## Context

Steps 1A-1I are complete with 60 passing tests. Two steps remain to complete Phase 1, which will produce a runnable server that Renegade clients can discover on LAN and connect to.

---

## Step 1J: RCON (Remote Console)

### 1J.1: `net/src/main/kotlin/ccr/net/rcon/RconCrypto.kt`

Pure functions for RCON encryption, decryption, and CRC. Must match C++ `servercontrolsocket.cpp` byte-for-byte.

**Encrypt** (C++ lines 987-1013): Stream cipher with 8-byte key and ciphertext feedback.
```
for each byte i:
    a = plaintext[i] + (i.toByte() - 50)   // signed byte arithmetic, wraps at ±128
    a = a xor key[i % 8]
    ciphertext[i] = a
    key[i % 8] ^= a                         // key mutates with ciphertext
```

**Decrypt**: Exact inverse — save ciphertext byte, XOR with key, subtract offset, feedback with saved ciphertext.

**CRC** (C++ `Add_CRC`, lines 935-948): Rolling CRC over 4-byte LE chunks:
```
for each 4-byte LE uint chunk:
    hibit = if (crc & 0x80000000) 1 else 0
    crc = (crc << 1) + chunk + hibit
Handle trailing 1-3 bytes as partial LE uint.
```

**Key derivation**: First 8 bytes of password, zero-padded if shorter.

### 1J.2: `net/src/test/kotlin/ccr/net/rcon/RconCryptoTest.kt`

- Encrypt-then-decrypt round-trips (various lengths)
- CRC with full and partial trailing bytes
- Key derivation (short password, long password, exact 8)
- Ciphertext feedback verification (same byte produces different output)

### 1J.3: `net/src/main/kotlin/ccr/net/rcon/RconSession.kt`

Per-controller state: `InetSocketAddress`, `isAuthenticated`, `lastActivityMs`, 60s timeout check.

### 1J.4: `net/src/main/kotlin/ccr/net/rcon/RconServer.kt`

UDP listener with NIO Selector (100ms poll). Manages sessions, handles the RCON protocol.

**Wire format**: `[CRC: 4 bytes LE][encrypted payload]`
Payload after decryption: `[Type: 4 bytes LE int (0=REQUEST, 1=RESPONSE)][Message: null-terminated, max 500 bytes]`

**Protocol state machine**:
```
CONNECT → "Password required:" (if password set, else immediate auth)
<password> → "Password accepted.\n" + welcome message
<command> → dispatch to commandHandler, chunk response into 499-byte packets
BYE → "Goodbye!\n", remove session
60s inactivity → "** Connection timed out - Bye! **\n", remove session
```

**API**:
```kotlin
class RconServer(
    val port: Int = 63999,
    val password: String = "",
    val remoteAdminAllowed: Boolean = false,
    val welcomeMessage: String = "",
    val commandHandler: (String) -> String = { "" },
) {
    suspend fun run()   // cancellation-aware main loop
}
```

Loopback-only by default (`remoteAdminAllowed = false` → reject non-127.0.0.1).

### 1J.5: `net/src/test/kotlin/ccr/net/rcon/RconServerTest.kt`

Real UDP on localhost with `runBlocking`:
- Full auth flow (CONNECT → password → accepted)
- Command dispatch and response
- Response chunking (>499 bytes)
- BYE disconnection
- No-password mode (immediate auth)

---

## Step 1K: Server Entry Point + LAN Discovery

### 1K.1: `server/src/main/kotlin/ccr/server/IniParser.kt`

Minimal read-only INI parser. Sections `[Name]`, key=value pairs, `;`/`#` comments.
```kotlin
class IniParser(sections: Map<String, Map<String, String>>) {
    fun getString(section: String, key: String, default: String = ""): String
    fun getInt(section: String, key: String, default: Int = 0): Int
    fun getBool(section: String, key: String, default: Boolean = false): Boolean
    // getBool: "yes"/"true"/"1" = true (case-insensitive), matching C++ Get_Bool
}
```

### 1K.2: `server/src/main/kotlin/ccr/server/ServerConfig.kt`

Immutable data class loaded from INI `[Settings]` section. Key fields:
- `gamePort` (4848), `maxPlayers` (16), `serverName`, `ownerName`, `gameTitle`
- `mapName`, `modName`, `password`, `isDedicated`, `isPassworded`
- `isTeamChangingAllowed`, `isLaddered`, `isClanGame`, `timeLimitMinutes`
- `versionNumber`, `exeCrc`, `stringsCrc`
- `rconPort` (63999), `rconPassword`, `remoteAdminAllowed`
- `netUpdateRate` (8 Hz), `bandwidthBps`

### 1K.3: `server/src/main/kotlin/ccr/server/LanBroadcastResponder.kt`

Broadcasts server presence every 1s to `255.255.255.255:3373` via raw BitStream (no Packet header, no PacketCombiner).

**Broadcast packet format** (BitStream with compression ON → bools are 1 bit):
```
byte:        0 (LAN_MESSAGE_POSITION)
wideString:  server nickname
int:         broadcast_number (incrementing)
bool:        true (is_hosting)
int:         2 (GAME_TYPE_CNC)
--- Tier 1 data ---
int:         IP address (local LAN IPv4 as 32-bit)
wideString:  owner name
wideString:  game title (permitEmpty=true)
int:         game port
int:         current players
int:         max players
int:         version number
int:         exe CRC
int:         strings CRC
bool:        is_dedicated
bool:        is_team_changing_allowed
bool:        is_passworded
bool:        is_laddered
bool:        is_clan_game
int:         map name CRC (0 stub for Phase 1)
int:         mod name CRC (0 stub for Phase 1)
```

Socket: `DatagramChannel` with `SO_BROADCAST` enabled, ephemeral send port.

IP detection: Enumerate `NetworkInterface`s, find first non-loopback IPv4 address.

### 1K.4: `server/src/main/kotlin/ccr/server/GameServer.kt`

Main orchestrator. Launches all coroutines in a `coroutineScope`:

```kotlin
coroutineScope {
    launch(Dispatchers.IO)   { transport.ioLoop() }           // game UDP I/O
    launch(gameThread)       { processInbound() }              // packet dispatch
    launch(gameThread)       { networkTickLoop() }             // keepalive, resend
    launch(gameThread)       { physicsTickLoop() }             // 120Hz stub
    launch(Dispatchers.IO)   { rconServer.run() }             // RCON
    launch(Dispatchers.IO)   { lanResponder.broadcastLoop() } // LAN discovery
}
```

**Thread safety**: All ConnectionManager/RemoteHost state confined to `gameThread` (single-thread dispatcher). UdpTransport communicates via coroutine channels. RconServer and LanBroadcastResponder have their own sockets.

**Packet dispatch** (`processInbound`): Read from `transport.receiveChannel`, split via `PacketCombiner.split`, parse via `Packet.parseWirePacket`, dispatch by `PacketType`:
- `CONNECT_CS` → `connectionManager.processConnectionRequest` → send ACCEPT/REFUSAL
- `ACK` → `connectionManager.processAck`
- `KEEPALIVE` / `RELIABLE` → touch host, ACK back, deliver to handler
- `UNRELIABLE` → touch host, sequence check, deliver to handler

**Network tick loop**: Keepalives, resends, timeout checks at `netUpdateRate` Hz.

**RCON command handler**: Phase 1 stub with `help`, `status`, `players` commands.

**Application acceptance handler**: Check password match, extract nickname/password/exe_key from CONNECT_CS payload.

### 1K.5: `server/src/main/kotlin/ccr/server/Main.kt`

Entry point: parse optional config file path from args, load `ServerConfig`, create and run `GameServer`.

---

## Implementation Order

| # | File | Module | Depends on |
|---|------|--------|------------|
| 1 | `RconCrypto.kt` | net | nothing |
| 2 | `RconCryptoTest.kt` | net | RconCrypto |
| 3 | `RconSession.kt` | net | nothing |
| 4 | `RconServer.kt` | net | RconCrypto, RconSession |
| 5 | `RconServerTest.kt` | net | RconServer, RconCrypto |
| 6 | `IniParser.kt` | server | nothing |
| 7 | `ServerConfig.kt` | server | IniParser |
| 8 | `LanBroadcastResponder.kt` | server | ServerConfig, BitStream, ConnectionManager |
| 9 | `GameServer.kt` | server | all of the above + UdpTransport, ConnectionManager, PacketCombiner, Packet |
| 10 | `Main.kt` | server | GameServer, ServerConfig |

Run `./gradlew :net:test` after steps 1-5, then `./gradlew test` after all steps.
