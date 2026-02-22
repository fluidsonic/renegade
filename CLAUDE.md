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

## C++ Port — Workflow Rules
- **All compiler AND linker errors are your problem to fix** — never leave a build broken; fix them before moving on
- **macOS-only codebase — no platform ifdefs**: This port targets macOS only; write clean macOS/POSIX code directly; no need to preserve `#ifdef _WIN32` / `#ifdef _UNIX` branches

## Workflow Meta-Rules
- When the user writes **"remember X"**, immediately update `CLAUDE.md` with X — no need to ask for confirmation
- **Never use hacks or workarounds** — always find and fix the root cause; never add iteration limits, fallback stubs, or band-aids over real bugs

## C++ Port — Known Patterns & Pitfalls

### Primitive type rules (enforced by `ccr-primitive-type` clang-tidy plugin)
- **Forbidden** raw built-ins: `char`, `signed char`, `unsigned char`, `short`, `unsigned short`, `int`, `unsigned int`, `long`, `unsigned long`, `long long`, `unsigned long long`, `long double`, `wchar_t`
- **Allowed** raw built-ins: `bool`, `void`, `nullptr_t`, `char8_t`, `char16_t`, `char32_t`, `float`, `double`
- **Required replacements**: use `(u)intN_t` (e.g. `int32_t`, `uint8_t`) for integers; `float`/`double` are fine as-is
- Typedefs like `int32_t`, `size_t`, `ptrdiff_t` are fine — the check only fires on raw un-typedef'd built-ins
- `float` and `double` sizes are guaranteed by `static_assert` in `original/global.h` (4 and 8 bytes)
- Run lint: `cmake --build /Users/marc/Documents/ccr/original/build --target lint`
- Narrowing/conversion warnings: `-Wconversion`, `-Wsign-conversion`, `-Wdouble-promotion` (warnings, not errors, until each module is clean)

### global.h — project-wide preamble
- `original/global.h` is the single project-wide preamble — must be included first in all source files
- Contains: platform defines (_UNIX, NOMINMAX), Windows types (BYTE/WORD/DWORD/HANDLE etc.), char16_t string functions, IEEE 754 asserts, engine macros (MIN/MAX/WWINLINE), debug stubs, MSVC compat
- `compat/windef.h`, `compat/c16string.h`, `Code/wwlib/always.h` are thin forwarding headers to `global.h` — files that include them still work
- `compat/clangcompat.h`, `compat/floattypes.h`, `Code/wwlib/bittype.h` are DELETED — absorbed into `global.h`
- No `-include` CLI magic — files include `global.h` explicitly (or via a forwarder)
- Source root `original/` is in the include path so `#include "global.h"` resolves from any TU

### wchar_t → char16_t conversion (COMPLETE)
- All `wchar_t` in Code/ and compat/ converted to `char16_t` (macOS wchar_t=4 bytes; protocol needs 2-byte UTF-16)
- `WCHAR` typedef is `char16_t`; all derived types (LPWSTR, LPCWSTR, etc.) auto-update
- Use `u"..."` and `u'...'` string/char literals (not `L"..."` / `L'...'`)
- `global.h` defines char16_t equivalents of all `wcs*` functions (`c16slen`, `c16scpy`, etc.); `compat/c16string.h` is a forwarder to `global.h`
- `global.h` provides inline `wcslen(char16_t*)`, `wcscpy(char16_t*)`, etc. overloads (previously in `windef.h`)
- Two intentional `wchar_t` exceptions: `winbase.h` internal `_vsnwprintf` bridge buffer; `LOGFONTW` struct in wingdi.h
- `const WCHAR*` (not `WCHAR*`) for string-literal arrays — `u"..."` is `const char16_t[]`

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

### ThreadClass — POSIX thread implementation (COMPLETE)
- `Execute()`: `pthread_create` + `pthread_detach`; stores `pthread_t` cast to `unsigned long` in `handle`
- `Stop(ms)`: sets `running=false`, spin-waits on `handle==0` (set by `Internal_Thread_Function` on exit), `pthread_cancel` on timeout
- `Switch_Thread()`: `sched_yield()`; `Sleep_Ms(ms)`: `usleep(ms*1000)`; `_Get_Current_Thread_ID()`: `(unsigned)(uintptr_t)pthread_self()`
- Root cause of texture-load exit hang: all `ThreadClass` methods were `return;` no-ops on macOS → background thread never started → `_BackgroundQueue` never drained → `Flush_Pending_Load_Tasks` looped forever

### On-disk / network struct fields must be fixed-width
- `void*`, `long`, `unsigned long` in binary format structs are bugs on 64-bit macOS (8 bytes vs Windows 32-bit 4 bytes)
- Use `uint32_t`/`int32_t` for every field that is read/written from disk or a network wire format
- Example caught: `LegacyDDSURFACEDESC2::Surface` was `void*` → struct became 132 bytes instead of 124 → DDS loading silently failed

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
- CLIENTCONTROL=1017, BIOEVENT=1025, CLIENTFPS=1031 — these are the header values
- **GameServer.kt handlers use +1 from the header for ALL C→S events** (e.g. BIOEVENT handled at 1026, not 1025)
- This is intentional — the actual Renegade binary sends IDs at +1 offset; don't "fix" GameServer.kt to match the header
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

### SpawnerClass save format
- SpawnManager::Save wraps each SpawnerClass in CHUNKID_SPAWNER_DATA=1014991133 (inside CHUNKID_SPAWNERS); SpawnManager's own timer is CHUNKID_SPAWNER_VARIABLES=1014991134 (skipped)
- SpawnerClass writes CHUNKID_PARENT=1014991053 + CHUNKID_VARIABLES=1014991054 inside the wrapper; micro 1=ID, 2=TM(48b), 3=defId, 6=enabled, 7=spawnPoint(repeats)
- Position = TM column[3]: bytes 12–15(X), 28–31(Y), 44–47(Z) within 48-byte Matrix3D
- SpawnerLoader now handles this: `ccr.server.level.ldd.SpawnerLoader.load(chunk)`

### Map loading — LevelLoader
- Orchestrator: `LevelLoader(alwaysMix, mapMix, baseName).load()` (suspend) — returns `LoadedLevel`
- Package structure: `ccr.server.level` (root), `lsd/` (static), `ldd/` (dynamic), `pathfind/`, `w3d/`
- `FullDefinitionLoader.load(ddbData, registry)` — dispatches by classId to 30+ typed parsers
- `SoldierGameObjDef` has no CLASS_ID constant; use `0x3001u` (CLASSID_GAME_OBJECTS + 1)
- `MixReader.readFile()` is case-insensitive (crcStringi) — no need for .lsd/.LSD case fallbacks

### LDD format (combatsaveload.cpp)
- Top-level chunks: CHUNKID_LEVEL_INFO (micro 1=mapFilename, 2=missionDescId, 3=description) + CHUNKID_LEVEL_DATA
- Inside LEVEL_DATA: CHUNKID_GAMEOBJMANAGER + CHUNKID_SPAWNERS + CHUNKID_SCRIPTS
- BaseGameObj CHUNKID_VARIABLES = 910991407; definitionId = micro 2, instanceId = micro 3

### LSD chunk IDs — corrections vs original docs
- WWAudio subsystem in LSD: `CHUNKID_STATIC_SAVELOAD = 0x00030005` (NOT 0x00030000 which is just the range start)
- BackgroundMgr: `CHUNKID_BACKGROUND_MGR = 0x00040126u`; WeatherMgr: `CHUNKID_WEATHER_MGR = 0x00040800u`
- Pathfind corrected: HEIGHTDB=0x0106063Du, ACTION_PORTAL=0x0106063Eu, WAYPATH_PORTAL=0x0106063Fu

### Spawning — SpawnManager
- `SpawnManager(level)` resolves spawners at init; call `getMultiplayerSpawnLocation(team)` to get a team-filtered spawn position
- Multiplayer soldier filter: `!isPrimary && isSoldierStartup && playerType == team`
- PLAYERTYPE_NEUTRAL (-2) is remapped to PLAYERTYPE_RENEGADE (-1) before filtering
- **Spawn position uses spawner TM** (`spawner.transform.position`) — NOT `spawnPoints` which are relative/empty in most MP maps (C++ uses `SpawnerList[i]->Get_TM()`)

### Spawning — God (cGod port)
- `God(server)` owns the player/soldier lifecycle — port of `cGod` (god.cpp)
- `GameServer.god` — initialized after `loadLevel()`, accessed as `god.*` throughout
- Player/soldier state maps live in God: `playersByHost`, `soldiersByHost`, `playerTeams`, `playerInGame`, `playerNetIds`
- `god.think()` — called each network tick; spawns soldiers for soldierless in-game players
- `god.createPlayer(rhostId, name)` — creates/registers Player object; caller sends the creation packet
- `god.createCommando(rhostId, playerType)` — creates/registers/sends SoldierGameObj to all in-game hosts
- `god.deleteSoldier(rhostId)` — sends deletion to peers, unregisters; use for suicide/team-change/disconnect
- `god.removePlayer(rhostId)` — full disconnect cleanup: deleteSoldier + player object removal
- `god.choosePlayerType()` — auto-balance team assignment (0=NOD, 1=GDI)

### Physics module (ccr.physics)
- Gradle module: `kotlin-server/physics/`, depends on `:math`. Added to `settings.gradle.kts` and `server/build.gradle.kts`
- Package layout mirrors C++ wwphys: `ode/` (integrators), `collision/` (CollisionMath, CastResult, CollisionGroupMatrix), `spatial/` (StaticAABTree, PhysGrid), `scene/` (PhysicsScene), `static/`, `dynamic/`, `moveable/`, `rigidbody/`, `vehicle/`
- `PhysClass.scene: PhysicsScene?` — set automatically by `PhysicsScene.addDynamicObject()`; used by `timestep()` implementations for collision queries
- `PhysicsScene.update(dt)` sub-steps at MAX_TIMESTEP=1/15f; `castRay/castAABox` query both static tree and dynamic grid
- `CollisionMath` object — all geometry algorithms: Möller-Trumbore (ray/tri), slab method (ray/AABB), SAT (OBB/OBB, OBB/tri), swept AABB/OBB collide methods
- `Integrator.midpointIntegrate(sys, 0f, dt)` — default for `RigidBodyClass`; all 4 methods (Euler/Midpoint/RK4/RK5) in `ccr.physics.ode.Integrator` object
- `Vector3` is a data class with `var` fields — direct mutation works: `force.x += gravX`
- `ccr.physics.static` package — `static` is a Java reserved word but valid Kotlin package name

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

### Game loop architecture (God, GameState, BuildingManager)
- `God(server)` owns player/soldier lifecycle; all player state maps live in God (`playersByHost`, `soldiersByHost`, `playerTeams`, `playerInGame`, `playerNetIds`)
- `GameState(config)` handles timer countdown, intermission, game-over detection; `gameState.think(deltaMs)` called every tick before `god.think()`
- `BuildingManager(server, level)` instantiates building subtypes from `LoadedBuildingGameObj`, registers `BaseControllerClass` singletons at `NET_ID_BASE_CONTROLLER_NOD/GDI`
- `GameServer.config` is `internal`; `GameServer.sendGameNetObj()` is `internal` — God and BuildingManager access these directly
- Player reconnect: `God.createPlayer()` checks for existing inactive player by same name, reactivates instead of creating new
- `ServerFps` registered with `NET_ID_SERVER_FPS = 2_100_000_006`; sent in `sendConnectionObjects()` and BIOEVENT handler
- `Player.replaceMoney()` named that way (not `setMoney`) because Kotlin generates a `setMoney` JVM setter that clashes with the `var money` property
