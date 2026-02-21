# Project: C&C Renegade Kotlin Server

## Build Commands

### Kotlin Server
- NEVER set JAVA_HOME or GRADLE_USER_HOME — user's shell has these configured
- Run: `/Users/marc/Documents/ccr/kotlin-server/gradlew -p /Users/marc/Documents/ccr/kotlin-server <task>`

### C++ Port (original/)
- Source root: `/Users/marc/Documents/ccr/original/`
- Build dir: `/Users/marc/Documents/ccr/original/build/`
- Build: `cmake --build /Users/marc/Documents/ccr/original/build --target Commando -j8`
- Configure (if build.ninja missing/stale or files deleted): `cmake -S /Users/marc/Documents/ccr/original -B /Users/marc/Documents/ccr/original/build -G Ninja`
- Run: `MallocNanoZone=0 /Users/marc/Documents/ccr/original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
- Build type: Debug with ASan (`-fsanitize=address` in root CMakeLists.txt); `MallocNanoZone=0` required on macOS

## C++ Port — Known Patterns & Pitfalls

### Non-POD varargs (`-Werror=non-pod-varargs`)
- `StringClass` and `WideStringClass` are non-trivially-copyable; passing them through `...` is UB
- Clang inserts `__builtin_trap()` at the call site even when the warning is suppressed with `-Wno-non-pod-varargs`
- CMakeLists.txt uses `-Werror=non-pod-varargs` to catch these at compile time
- Fix: explicit cast at call site — `(const char*)strVar` or `(const WCHAR*)wideVar`

### `RawFileClass::Error()` is a no-op stub
- After calling `Error(EBADF, ...)`, execution continues — callers must `return` manually
- All code paths that call `Error()` then fall through to the faulting operation without the explicit return

### mempool.h — 64-bit pointer arithmetic
- `BlockListHead` must be `uintptr_t*` (not `uint32_t*`) so that `+1` skips a full pointer width (8 bytes on arm64)
- Original Windows code used `uint32*`; first object slot overlapped the block header on 64-bit → corruption on free

### Intro movie startup (movie.cpp)
- `MovieGameModeClass::Start_Movie()` hangs forever if the .BIK file is missing AND no CD drive (macOS has neither)
- Fixed: `else if (force_cd)` branch now calls `Movie_Done()` directly instead of `CDVerifier.Display_UI()`
- Allows the game to skip both intro movies and proceed to the main menu when video files aren't present

### Building to find all issues
- Use `cmake --build ... -- -k 0` (Ninja keep-going) to see ALL errors, not just the first one
- Running the game from a background bash subprocess fails with "SDL_Init: no displays" — always run interactively

## Kotlin Server — Known Patterns & Pitfalls

### Test framework is kotlin.test (not AssertJ)
- `kotlin.test` is the only test dependency: `assertEquals`, `assertTrue`, `assertNotNull`, etc.
- AssertJ (`org.assertj.core.api.Assertions`) is NOT available — don't use it

### BitStream constraints
- `getBits(n)` only supports 1..32 bits — use `getBits(32)` + `getBits(8)` + `getBits(1)` to skip the 41-bit envelope header
- Write position property is `bitWritePosition` (not `bitCount`)

### NETCLASSID source
- Authoritative source: `Code/Combat/netclassids.h` — sequential enum starting at 1000
- S→C IDs: 1000–1016 (no 1017 GameSpy — removed); C→S IDs: 1017–1038
- CLIENTCONTROL=1017, BIOEVENT=1025, CLIENTFPS=1031 (old docs/code had off-by-one +1 errors — corrected)
- Many singletons have classId=0 (no `Get_Network_Class_ID()` override): ServerFps, BaseControllerClass, all StaticNetworkObject subclasses, BackgroundMgr, WeatherMgr
- These are identified by well-known `networkId` values at runtime, not classId
- `NetClassIds.kt` at `server/src/main/kotlin/ccr/server/NetClassIds.kt` — maps ID→name for logging

### BuildingGameObj hierarchy
- Does NOT extend PhysicalGameObj (unlike SoldierGameObj/VehicleGameObj)
- Extends DamageableGameObj directly — no model, animation, facing, or controlOwner fields

### VehicleGameObj.exportFrequent ordering
- Writes vehicle-specific data (totalRounds, physics block, driverIsGunner) BEFORE calling super.exportFrequent
- PhysicalGameObj writes `isHidden` bool in exportRare only for VehicleGameObj instances (`As_VehicleGameObj() != NULL` check)

### Soldier packet — animName must be non-empty
- Client calls `AnimControl->Set_Animation(name)` unconditionally — empty string freezes the client
- Client never completes `Import_Rare`, never ACKs any subsequent reliable packets → infinite resend loop
- Production values: `modelName = "c_ag_nod_mg"` (NOD) or `"c_ag_gdi_mg"` (GDI), `animName = "S_A_HUMAN.H_A_AINM"`
- `animName` threads through: `PhysicalGameObj` → `ArmedGameObj` → `SmartGameObj` → `SoldierGameObj`

### NetworkObject module boundary — registering with networkId
- `NetworkObject.networkId` has `internal set` (net module only); server module cannot set it directly
- Use `NetworkObjectManager.registerObject(obj, networkId)` overload to assign ID at registration time
- Objects start with all dirty bits = 0 after creation; only set dirty bits for future state changes
- `UnreliableChannel.nextOutgoingId()` allocates the next unreliable packet ID (do NOT read `nextSendId` directly)

### Replication loop rules
- Reliable: BIT_CREATION / BIT_RARE / BIT_OCCASIONAL updates → `sendGameNetObj` (reliable channel)
- Unreliable: BIT_FREQUENT-only updates → `sendUnreliable` (no ACK, no retry)
- Never send a soldier's BIT_FREQUENT update to its own `controlOwner` — client is authoritative for its own position
- On disconnect: send `writeDeletion` to other in-game hosts BEFORE calling `NetworkObjectManager.unregisterObject`

### CClientControl frequent update wire format (C→S, UNRELIABLE)
- Header: networkId(32) + dirtyBits(8) + isDeletePending(1); no classId when BIT_CREATION not set
- Payload: smartObjId(32); if != -1: in_vehicle(bool), has_weapon(bool), [if hasWeapon: defId+rounds(32×2)],
  position(3×float BITPACK_WORLD), humanState(BITPACK_HUMAN_STATE), humanSubState, isSpecialDamage(bool),
  onHostBone(bool), targeting(3×float BITPACK_WORLD), continuousBoolBits(BITPACK_CONTINUOUS), 4×analog(BITPACK_ANALOG)

### SpawnerClass save format (for future spawn point extraction from map .lsd)
- `CHUNKID_COMBAT_BEGIN = 0x00040000`; SpawnerClass CHUNKID_PARENT=1014991053, CHUNKID_VARIABLES=1014991054
- In VARIABLES micro-chunks: ID=1 (int), TM=2 (Matrix3D: 12 floats = 48 bytes), DEFINITION_ID=3 (int)
- Position = TM columns [3]: bytes 12–15 (X), 28–31 (Y), 44–47 (Z) within the 48-byte Matrix3D

### Live proxy (diagnostic tool)
- `gradlew :server:liveProxy -PlocalPort=4849 -PremoteHost=127.0.0.1 -PremotePort=4848 -PlogFile=.tmp/proxy_log.txt`
- Forwards client↔server traffic and decodes every packet in real-time
- `PacketDecoder` object: `ccr.server.net.PacketDecoder` — reusable decode logic for all packet analysis
- Captured logs go in `server/src/test/resources/proxy_log.txt` for use by `ProxyDecoderTest`

## Workflow Preferences
- Write all documentation to `/docs/*.md`. Update documentation as soon as you learn something new.
- Network protocol documentation must be maintained in real-time at `/docs/network.md`. Update it immediately whenever new packet formats, field meanings, connection sequences, or wire format details are discovered.
- Write every accepted plan to `/plans/*.md`. Write immediately once a plan was approved. Make that behavior part of each plan itself.
- Automatically update `/CLAUDE.md` as you learn new things to remember in every conversation.
- `/CLAUDE.md`, `/docs/*.md`, `/plans/*.md` can be written/edited in plan mode.
- Always use `<project root>/.tmp/` for temporary files (scripts, scratch files, etc.). Never use `/tmp` or `$TMPDIR`.
