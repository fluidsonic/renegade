# Project: C&C Renegade Kotlin Server

## Project Overview
C&C Renegade (2002 FPS) server reimplementation and macOS port.

- **`original/`** — C++ macOS port of the original Windows game (builds and runs the Commando executable)
- **`original-untouched/`** — pristine Windows/DirectX reference source; consult to understand original GDI/D3D8/Win32 behavior
- **`kotlin-server/`** — from-scratch Kotlin server reimplementation
- Kotlin modules: `math`, `net`, `physics`, `server`
- Key constraint: all wire formats must match the original Renegade client exactly

## Build Commands

### Kotlin Server
- NEVER set JAVA_HOME or GRADLE_USER_HOME — user's shell has these configured
- Run: `kotlin-server/gradlew -p kotlin-server <task>`

### C++ Port (original/)
- Source root: `original/`
- Original untouched Windows/DirectX source for reference: `original-untouched/` — use this to understand original GDI/D3D8/Win32 behavior
- Build dir: `original/build/`
- Build: `cmake --build original/build --target Commando -j8`
- Configure (if build.ninja missing/stale or files deleted): `cmake -S original -B original/build -G Ninja`
- Run: `MallocNanoZone=0 original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
- Build type: Debug with ASan (`-fsanitize=address` in root CMakeLists.txt); `MallocNanoZone=0` required on macOS

### Tests
- Run all Kotlin tests: `kotlin-server/gradlew -p kotlin-server test`
- Individual modules: `:server:test`, `:net:test`, `:math:test`, `:physics:test`
- **Do NOT run Kotlin tests when working on C++ changes** — they are unrelated; C++ verification is build + visual run only

## C++ Port — Workflow Rules
- **All compiler AND linker errors are your problem to fix** — never leave a build broken; fix them before moving on
- **macOS-only codebase — no platform ifdefs**: This port targets macOS only; write clean macOS/POSIX code directly; no need to preserve `#ifdef _WIN32` / `#ifdef _UNIX` branches
- **Visual/runtime verification is required for C++ fixes** — a successful build is NOT sufficient to call a fix complete; the game must be run interactively and the fix confirmed visually before declaring done or merging

## Workflow Meta-Rules
- **Use agents for code exploration and implementation** — keep the main conversation free from code and focused on high-level decisions; delegate code reading, searching, and writing to Task agents
- **Never use the `commit-commands:commit` skill** — commit directly with git commands instead
- When the user writes **"remember X"**, immediately update `CLAUDE.md` with X — no need to ask for confirmation
- **Minimize work for the user** — always run build commands, tests, and verifications yourself rather than giving the user commands to paste; only hand off commands that require interactive UI (e.g. running the game for visual confirmation)
- **Never use hacks or workarounds** — always find and fix the root cause; never add iteration limits, fallback stubs, or band-aids over real bugs
- **Git merges must use `--ff-only`** — always rebase the feature branch onto the base branch first, then FF-merge
- **Always create worktrees from the latest local `main`** — run `git pull` (or at least `git fetch`) on main before branching; stale branch points cause unnecessary rebase conflicts
- **Worktree merge sequence**: (1) check for uncommitted changes in both the worktree AND main repo (`git status` in both), (2) commit all changes in the worktree, (3) rebase the worktree branch onto local `main`, (4) `git checkout main && git merge --ff-only <branch>`, (5) `git worktree remove` the worktree, (6) delete the branch — in that order; you cannot delete a branch while its worktree still exists
- Write all documentation to `/docs/*.md` — update immediately when new information is discovered
- Network protocol documentation must be maintained in real-time at `/docs/network.md` — update whenever new packet formats, field meanings, or wire format details are discovered
- Write every accepted plan to `/plans/*.md` immediately once approved; make that behavior part of each plan itself
- Automatically update `/CLAUDE.md` as you learn new things in every conversation
- `/CLAUDE.md`, `/docs/*.md`, `/plans/*.md` can be written/edited in plan mode
- Always use `<project root>/.tmp/` for temporary files (scripts, scratch files, etc.) — never use `/tmp` or `$TMPDIR`
- Use `<project root>/.worktrees/` for git worktrees (not `.claude/worktrees/`)
- **Always provide copyable debugging instructions** — when the user needs to help debug, give ready-to-paste commands (e.g. how to run the C++ binary with output to a log file, LLDB commands to debug a crash with debugger attached, etc.)

## C++ Port — Known Patterns & Pitfalls

### Primitive type rules (enforced by `ccr-primitive-type` clang-tidy plugin)
- **Forbidden** raw built-ins: `char`, `signed char`, `unsigned char`, `short`, `unsigned short`, `int`, `signed`, `unsigned int`, `unsigned`, `long`, `unsigned long`, `long long`, `unsigned long long`, `long double`, `char32_t`, `wchar_t` (use `char16_t` instead) — naked `signed`/`unsigned` are just aliases for `signed int`/`unsigned int` and equally forbidden
- **Allowed** raw built-ins: `bool`, `void`, `nullptr_t`, `char8_t`, `char16_t`, `float`, `double`
- **Required replacements**: use `(u)intN_t` (e.g. `int32_t`, `uint8_t`) for integers; `float`/`double` are fine as-is
- Typedefs like `int32_t`, `size_t`, `ptrdiff_t` are fine — the check only fires on raw un-typedef'd built-ins
- `float` and `double` sizes are guaranteed by `static_assert` in `original/global.h` (4 and 8 bytes)
- Run lint: `cmake --build original/build --target lint`
- Narrowing/conversion warnings: `-Wconversion`, `-Wsign-conversion`, `-Wdouble-promotion` (warnings, not errors, until each module is clean)

### Active compiler warnings — what to avoid in new code

**Hard errors (`-Werror=`)** — these break the build immediately:
- `non-pod-varargs`: don't pass `StringClass`/`WideStringClass` through `...`; cast to `const char*`/`const WCHAR*` first
- `return-type`: every non-void function must return a value on all paths
- `delete-incomplete`: never `delete ptr` when the pointed-to type is forward-declared only; include the full definition
- `null-conversion`: don't assign `NULL`/`0` to a non-pointer (e.g. `bool x = NULL` — use `false`)
- `array-bounds`: no out-of-bounds constant array indexing

**Active warnings (not errors yet — avoid adding new instances):**
- `-Wconversion` / `-Wsign-conversion`: avoid implicit signed↔unsigned or narrowing conversions; use explicit casts (`static_cast<uint32_t>(x)`)
- `-Wdouble-promotion`: avoid passing `float` to functions that take `double` (e.g. `printf`-family `%f`); use explicit `(double)` cast or `%f` with a float literal
- `-Wunused-function`: don't write static/internal functions that are never called; remove or `#if 0` dead helpers
- `-Wunused-private-field`: remove private fields that are never read
- `-Wunneeded-internal-declaration`: don't declare functions/vars in anonymous namespaces unless they're actually used
- `-Wunreachable-code` / `-Wunreachable-code-return`: no dead code after `return`/`break`/`throw`; no `return x;` after unconditional `return`

**Intentional no-op pattern:** to silence an unused-value on a parameter that must be named, use `(void)param;` — not a bare `param;`

### float32_t / float64_t
- NOT in the macOS SDK — typedef'd as `float`/`double` in `original/compat/winnt.h` just before `#include "typesizes.h"`
- Never replace with raw `float`/`double` in `typesizes.h` itself

### global.h — project-wide preamble
- `original/global.h` is the single project-wide preamble — must be included FIRST in all source files
- Contains: platform defines (_UNIX, NOMINMAX), Windows types (BYTE/WORD/DWORD/HANDLE etc.), char16_t string functions, IEEE 754 asserts, engine macros (MIN/MAX/WWINLINE), debug stubs, MSVC compat
- **Forwarder headers DELETED**: `compat/windef.h`, `compat/c16string.h`, `Code/wwlib/always.h`, `compat/malloc.h`, `compat/new.h`, `compat/osdep/osdep.h` — all content is in `global.h` or replaced by standard includes
- `compat/clangcompat.h`, `compat/floattypes.h`, `Code/wwlib/bittype.h` are also DELETED — absorbed into `global.h`
- **All .h files use `#pragma once` exclusively** — no `#ifndef`/`#define` include guards anywhere
- **`#include "global.h"` is the first include in every .h and .cpp file** — explicit, not transitive
- PCH enabled: `target_precompile_headers(Commando PRIVATE "global.h")` in Code/Commando/CMakeLists.txt
- `compat/macos_fix.mm` is excluded from PCH (Objective-C++ — `SKIP_PRECOMPILE_HEADERS ON`); **no other file should use `SKIP_PRECOMPILE_HEADERS ON`** — the PCH only pre-compiles `global.h`, so defining implementation macros (e.g. `MINIAUDIO_IMPLEMENTATION`) before a `#include` in a `.cpp` works correctly without it
- Source root `original/` is in the include path so `#include "global.h"` resolves from any TU
- Exclusions (no `#include "global.h"` added): `compat/typesizes.h` (mid-file include in winnt.h), `tools/primitive-type-check/PrimitiveTypeCheck.h`, `tools/primitive-type-check/PrimitiveTypeCheck.cpp` (clang-tidy plugin — LLVM-API-only code, not part of the game codebase), resource compiler headers (`resource.h`, `dialogresource.h`, `afxres.h` — processed by llvm-rc which lacks the include path)

### wchar_t → char16_t convention
- All `wchar_t` in Code/ and compat/ converted to `char16_t` (macOS wchar_t=4 bytes; protocol needs 2-byte UTF-16)
- `WCHAR` typedef is `char16_t`; all derived types (LPWSTR, LPCWSTR, etc.) auto-update
- Use `u"..."` and `u'...'` string/char literals (not `L"..."` / `L'...'`)
- `global.h` defines char16_t equivalents of all `wcs*` functions (`c16slen`, `c16scpy`, etc.); `compat/c16string.h` is DELETED (was a forwarder to `global.h`)
- `global.h` provides inline `wcslen(char16_t*)`, `wcscpy(char16_t*)`, etc. overloads; `compat/windef.h` is DELETED (was a forwarder to `global.h`)
- Two intentional `wchar_t` exceptions: `winbase.h` internal `_vsnwprintf` bridge buffer; `LOGFONTW` struct in wingdi.h
- `const WCHAR*` (not `WCHAR*`) for string-literal arrays — `u"..."` is `const char16_t[]`

### Non-POD varargs (`-Werror=non-pod-varargs`)
- `StringClass` and `WideStringClass` are non-trivially-copyable; passing through `...` is UB — Clang crashes the program even with `-Wno-non-pod-varargs`
- Fix: explicit cast at call site — `(const char*)strVar` or `(const WCHAR*)wideVar`

### `RawFileClass::Error()` is a no-op stub
- `Error(EBADF, ...)` does NOT abort — execution continues, so callers must `return` manually after calling it

### mempool.h — 64-bit pointer arithmetic
- `BlockListHead` must be `uintptr_t*` (not `uint32_t*`) so `+1` skips a full 8-byte pointer width (arm64), not 4 bytes — prevents first-object-slot/block-header overlap on free

### Bink video — bink_impl.cpp
- `compat/bink.h` has `extern` declarations only; implemented in `compat/bink_impl.cpp` via FFmpeg
- FFmpeg (`/opt/homebrew/opt/ffmpeg`) linked dynamically: `-lavformat -lavcodec -lswscale -lswresample -lavutil`; rpath `/opt/homebrew/opt/ffmpeg/lib` set in `Code/Commando/CMakeLists.txt`
- `BinkContext` struct: `BINK` as first member (so `HBINK` casts directly), wraps `AVFormatContext`, `AVCodecContext`, `SwsContext`, `SwrContext`, `ma_device`, `ma_pcm_rb`
- **Pixel format**: `AV_PIX_FMT_BGR565LE` (not RGB) — `d3d8_gl.cpp` uploads D3DFMT_R5G6B5 as `GL_UNSIGNED_SHORT_5_6_5_REV` which has R in low bits, requiring B in high bits
- **Audio**: `BinkDoFrame` drains audio packets (binkaudio_dct/rdft) and pushes decoded float-stereo frames into a `ma_pcm_rb` ring buffer; a `ma_device` started at `BinkOpen` time consumes it in callback; runs independently of the MSS `ma_engine`
- Include `miniaudio.h` WITHOUT `MINIAUDIO_IMPLEMENTATION` — implementation is in `mss_impl.cpp`; declarations suffice for `ma_device` / `ma_pcm_rb` usage

### Audio backend — mss_impl.cpp
- `compat/mss.h` has extern declarations only (no bodies); all ~106 `AIL_*` functions implemented in `compat/mss_impl.cpp` via miniaudio
- `compat/miniaudio.h` is a vendored single-header library (CoreAudio backend); linked with `-framework CoreAudio -framework AudioToolbox`
- Pool sizes: 64 2D samples (`HSAMPLE`), 32 3D samples (`H3DSAMPLE`), 16 streams (`HSTREAM`), 16 timers; handles are 1-based (0 and -1 are invalid sentinels)
- 3D coordinate swizzle `[-Y, Z, X]` lives in `Sound3DClass::Update_Miles_Transform()` in WWAudio — the backend passes coords through as-is, do not add a swizzle there
- `FilteredSoundClass` reverb (`AIL_set_sample_processor`, `AIL_set_filter_sample_preference`) is currently a no-op stub
- `g_lock` is `PTHREAD_RECURSIVE_MUTEX_INITIALIZER` — must be recursive to match Windows `CRITICAL_SECTION`; several call chains nest `MMSLockClass` on the same thread (e.g. `Free_3D_Driver_List` → `Release_3D_Handles`)
- `AIL_set_file_callbacks` open-callback convention: **return = file size (0 = failure), out-param = file handle** — the MSS convention, not return=handle
- File callback and sample user-data types are `uintptr_t` / `void*` (not `U32`) — original 32-bit MSS used `U32` which truncates pointers on arm64
- `AIL_WAV_info(data, info, buf_size)` takes an explicit `buf_size` — callers may pass a partial file read (e.g. 4096-byte stack buffer in `StreamSoundBufferClass::Load_From_File`); without it the parser reads past the buffer using the RIFF header's file-size field
- See `docs/audio-system.md` for full architecture, class hierarchy, MSS function list, and coordinate transform details

### SDL2 input and cursor architecture
- **`SDL2_ShouldRender`** — render gate in `sdl2_platform.h`; true in unfocused windowed mode, false only when minimized or fullscreen+unfocused; use this instead of `GameInFocus` for render gating
- **`SDL2_MouseCaptured`** — click-to-capture flag set by `SDL_MOUSEBUTTONDOWN` in `SDL2_Platform_PollEvents`; pre-capture: absolute mouse + button events via `SDL_GetMouseState`; captured: raw deltas via `SDL_GetRelativeMouseState`
- **SDL cursor hiding is async on macOS**: `SDL_ShowCursor(SDL_DISABLE)` dispatches `invalidateCursorRectsForView:waitUntilDone:NO` — call `SDL_PumpEvents()` immediately after so AppKit processes the cursor rect update before game init runs

### Intro movie startup (movie.cpp)
- `MovieGameModeClass::Start_Movie()` hangs if .BIK file missing and no CD drive (macOS has neither); patched to call `Movie_Done()` directly
- Fix allows skipping both intro movies and proceeding to the main menu when video files aren't present

### ThreadClass — POSIX thread implementation
- Implemented with pthreads: `Execute()` → `pthread_create`/`pthread_detach`; `Switch_Thread()` → `sched_yield()`; `Sleep_Ms()` → `usleep()`
- Warning: if methods revert to `return;` no-ops, background thread never starts → `_BackgroundQueue` never drains → `Flush_Pending_Load_Tasks` loops forever

### On-disk / network struct fields must be fixed-width
- `void*`, `long`, `unsigned long` in binary format structs are bugs on 64-bit macOS (8 bytes vs Windows 32-bit 4 bytes)
- Use `uint32_t`/`int32_t` for every field read/written from disk or a network wire format
- `HRESULT` is `typedef long HRESULT` — on arm64 `long` = 8 bytes, but D3D return values fit in 32 bits; use `static_cast<uint32_t>(hr)` when storing into `uint32_t` variables (~42 sites in ww3d2)
- `TIMEGETTIME()` returns `unsigned long` — logically wraps at 32 bits on arm64; cast with `static_cast<uint32_t>(TIMEGETTIME())`
- `fseek`/`ftell` return `long` on macOS — cast to `int32_t` at call sites; `fpos_t` is `long long`, also needs cast

### Cross-thread GL resource cleanup — d3d8_gl.cpp
- Background threads (e.g. audio delayed-release thread) can destroy `RefCountClass` objects whose destructor chain reaches `D3D8Texture_GL::~D3D8Texture_GL()` — calling `glDeleteTextures` without a GL context → SEGV
- Fix: deferred deletion queue (`g_pendingTexDeletes` + `g_texDeleteMutex`) at file scope; destructor pushes `glTexId` onto the queue; `Present()` drains it on the render thread before `SDL2_Platform_SwapWindow()`
- Only textures need this — vertex/index buffers are CPU-side `BYTE*` arrays

### GL rendering layer — d3d8_gl.cpp
- `original/compat/d3d8_gl.cpp` — D3D8→OpenGL translation layer; texture stages ~line 920, lighting ~line 877, transforms ~line 840
- **Texture coordinate generation**: `D3DTSS_TCI_CAMERASPACENORMAL` → `GL_TEXTURE_GEN_S/T` with `GL_NORMAL_MAP` (implemented); other TCI modes not yet implemented
- **Texture transform matrices**: `D3DTS_TEXTURE0..7` stored in `texMatrix[8]`; applied via `glMatrixMode(GL_TEXTURE)` when `D3DTSS_TEXTURETRANSFORMFLAGS != 0`
- **`d3d_to_gl_matrix`**: straight memcpy of 16 floats — game pre-transposes before calling SetTransform, so this function transposes back to GL column-major
- **ClassicEnvironmentMapperClass** (`original-untouched/Code/ww3d2/mapper.cpp` ~line 593): sets `D3DTSS_TCI_CAMERASPACENORMAL` + `D3DTS_TEXTURE0` scale+bias matrix; meshes using it have FVF `0x12` (XYZ+NORMAL only, no stored UVs) — if such a mesh appears dark/invisible with screen blend, the texgen path is the suspect
- **`Clear()` vs `glDepthMask`**: D3D8 `Clear()` ignores `D3DRS_ZWRITEENABLE`; OpenGL `glClear(GL_DEPTH_BUFFER_BIT)` respects `glDepthMask()` — combat transparent rendering can leave depth writes disabled, silently breaking the backdrop's depth clear; fix: force `glDepthMask(GL_TRUE)` before `glClear` then restore tracked D3D state
- **`ScreenUVBias` must be 0 for GL**: `console.cpp` loads `ScreenUVBias` from `renegade_registry.ini` (default was 1); this D3D8 half-texel correction shifts all 2D vertices by -0.5px, causing maximum bilinear blur in GL (sample lands exactly on texel boundary); `console.cpp` default changed to 0, and `renegade_registry.ini` set to `ScreenUVBias=0`
- **A4R4G4B4 → GL upload**: `Upload_To_GL` expands 16-bit A4R4G4B4 pixels to BGRA8 bytes (B=bits3:0, G=bits7:4, R=bits11:8, A=bits15:12, each nibble×17); uploaded with `GL_BGRA_EXT` / `GL_UNSIGNED_BYTE` / internal `GL_RGBA8`
- **`D3DRS_CULLMODE` (index 22)** mapped to `DIRTY_CULL` flag; `Apply_GL_State` handles it: `D3DCULL_NONE` → `glDisable(GL_CULL_FACE)`; `D3DCULL_CW` → `glEnable + glCullFace(GL_BACK)`; `D3DCULL_CCW` → `glEnable + glCullFace(GL_FRONT)`; initial `rs[22] = 3` (D3DCULL_CCW) set in constructor

### Building to find all issues
- **All worktree C++ commands need absolute paths** — `cmake --build`, `cmake -S/-B`, and run commands all resolve relative to CWD; if CWD is the main repo dir they target the main repo's build, not the worktree's; always use absolute paths for configure, build, and run when working in a worktree (e.g. `cmake -S /abs/path/.worktrees/feat/original -B /abs/path/.worktrees/feat/original/build -G Ninja`)
- Use `cmake --build ... -- -k 0` (Ninja keep-going) to see ALL errors, not just the first one
- After merging a worktree feature branch, reconfigure: `cmake -S original -B original/build -G Ninja` — regenerates `compile_commands.json` with updated flags/includes so clangd stays accurate
- Batch find-and-replace across source files: use Python3 `re` module or the Edit tool — **never use shell commands (`sed`, `echo`, heredocs) containing `!`** — zsh history expansion corrupts `!=` to `\!=`, `!x` to `\!x`, etc. silently; this applies to Perl, sed scripts, and any heredoc
- Case-insensitive `#include` search: use `grep -ril '"header.h"'` — include paths can have mixed case (e.g. `"BitType.H"`)
- Running the game from a background bash subprocess fails with "SDL_Init: no displays" — always run interactively
- **Keep diagnostic `fprintf` logging until visual confirmation** — remove only after the user confirms the fix works; removing before confirmation means a rebuild just to re-add it

## Kotlin Server — Design Philosophy
- **Stay close to the C++ code** — class names, function names, field names, and code structure should mirror the C++ original as closely as possible; Kotlin idioms are fine but don't invent abstractions that don't exist in C++
- **Always check the C++ original (`original/`) BEFORE fixing any Kotlin bug** — never reason from first principles about what the correct behavior should be; find the equivalent C++ code and mirror it exactly; this applies to every fix, no matter how obvious it seems
- **Before writing or modifying any Kotlin function, find the exact C++ counterpart in `original/` and use its name and call structure** — no invented names, no invented abstractions; if a Kotlin function exists whose name has no C++ counterpart, rename it; if a C++ function has no Kotlin counterpart, it is missing and must be added
- **ALL C++ lookups must search `original/` only — never `original-untouched/`**; agents must be explicitly told: "your search path is `original/` only, never search under `original-untouched/`"

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
- `UnreliableChannel.nextOutgoingId()` allocates the next unreliable packet ID (do NOT read `nextSendId` directly)

### Replication architecture — dirty-bit-driven, matching C++ Tell_Client_About_Dynamic_Objects
- **Single replication path**: `replicationTick()` iterates ALL registered NetworkObjects, checks per-client dirty bits, sends appropriate update, clears bits. No manual `writeCreation`/`writeDeletion` calls for persistent objects.
- **Objects set dirty bits at registration**: `BaseGameObj`-derived objects (buildings, soldiers, players) set `BIT_CREATION` for all clients at construction. Singletons (BaseControllerClass, ServerFps) set their appropriate level (`BIT_OCCASIONAL`, `BIT_FREQUENT`).
- **New clients**: call `NetworkObjectManager.restoreDirtyBits(clientId)` in BIOEVENT handler — sets each object's `creationDirtyBit` for the new client. Next `replicationTick()` sends everything.
- **classId=0 objects MUST NEVER be sent with writeCreation**: BaseControllerClass, ServerFps, and all StaticNetworkObjects have classId=0. The client has no factory for classId=0 — `Create_Network_Object` → `Find_Factory(0)` → NULL → crash. These objects are created locally by the client during level load and only need state updates (occasional/frequent).
- **`creationDirtyBit` property**: each NetworkObject subclass declares its max dirty level for new-client replication. Default is `BIT_CREATION`; classId=0 singletons override to `BIT_OCCASIONAL` or `BIT_FREQUENT`.
- **Connection_Handler sends ONLY Teams + GameOptionsEvent**: matches C++ `cNetwork::Connection_Handler`. Everything else (buildings, base controllers, ServerFps, players) goes through `replicationTick()` after BIOEVENT.
- **Deletion via `setDeletePending()`**: centralized delete-pending loop in `networkTickLoop` broadcasts deletion to all in-game clients and unregisters. Don't manually send `writeDeletion` packets.
- Reliable: BIT_CREATION / BIT_RARE / BIT_OCCASIONAL updates → `sendGameNetObj` (reliable channel)
- Unreliable: BIT_FREQUENT-only updates → `sendUnreliable` (no ACK, no retry)
- Never send a soldier's BIT_FREQUENT update to its own `controlOwner` — client is authoritative for its own position

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
- `god.createPlayer(rhostId, name)` — creates/registers Player object, sets `BIT_CREATION` dirty; `replicationTick()` handles sending
- `god.createCommando(rhostId, playerType)` — creates/registers SoldierGameObj, sets `BIT_CREATION` dirty; `replicationTick()` handles sending to all in-game hosts
- `god.deleteSoldier(rhostId)` — calls `setDeletePending()`; centralized delete-pending loop broadcasts deletion
- `god.removePlayer(rhostId)` — full disconnect cleanup: deleteSoldier + player object removal

### GameObjManager — ticking game objects
- `gameObjManager.add(obj)` registers any `BaseGameObj` for `think()` ticks; `GameObjManager.think(deltaSeconds)` iterates `gameObjList` and calls `obj.think()` on every registered object
- New ticking objects must `override fun think(deltaSeconds: Float)` (declared `open` in `BaseGameObj`)

### Game loop architecture (God, GameState, BuildingManager)
- `God(server)` owns player/soldier lifecycle; all player state maps live in God (`playersByHost`, `soldiersByHost`, `playerTeams`, `playerInGame`, `playerNetIds`)
- `GameState(config)` handles timer countdown, intermission, game-over detection; `gameState.think(deltaMs)` called every tick before `god.think()`
- `BuildingManager(server, level)` instantiates building subtypes from `LoadedBuildingGameObj`, registers `BaseControllerClass` singletons at `NET_ID_BASE_CONTROLLER_NOD/GDI`
- `GameServer.config` is `internal`; `GameServer.sendGameNetObj()` is `internal` — God and BuildingManager access these directly
- Player reconnect: `God.createPlayer()` checks for existing inactive player by same name, reactivates instead of creating new
- `ServerFps` registered with `NET_ID_SERVER_FPS = 2_100_000_006`; sent via `replicationTick()` dirty bits (BIT_FREQUENT), never manually
- `Player.replaceMoney()` named that way (not `setMoney`) because Kotlin generates a `setMoney` JVM setter that clashes with the `var money` property

### Physics module (ccr.physics)
- Gradle module: `kotlin-server/physics/`, depends on `:math`. Added to `settings.gradle.kts` and `server/build.gradle.kts`
- Package layout mirrors C++ wwphys: `ode/` (integrators), `collision/` (CollisionMath, CastResult, CollisionGroupMatrix), `spatial/` (StaticAABTree, PhysGrid), `scene/` (PhysicsScene), `static/`, `dynamic/`, `moveable/`, `rigidbody/`, `vehicle/`
- `PhysClass.scene: PhysicsScene?` — set automatically by `PhysicsScene.addDynamicObject()`; used by `timestep()` implementations for collision queries
- `PhysicsScene.update(dt)` sub-steps at MAX_TIMESTEP=1/15f; `castRay/castAABox` query both static tree and dynamic grid
- `CollisionMath` object — all geometry algorithms: Möller-Trumbore (ray/tri), slab method (ray/AABB), SAT (OBB/OBB, OBB/tri), swept AABB/OBB collide methods
- `Integrator.midpointIntegrate(sys, 0f, dt)` — default for `RigidBodyClass`; all 4 methods (Euler/Midpoint/RK4/RK5) in `ccr.physics.ode.Integrator` object
- `Vector3` is a data class with `var` fields — direct mutation works: `force.x += gravX`
- `ccr.physics.static` package — `static` is a Java reserved word but valid Kotlin package name
- `StaticPhysClass` is `open class` (not abstract) — directly instantiable; set `triangles` and `transform` then call `scene.addStaticObject()`
- Static collision geometry loaded by `PhysicsSceneBuilder.build(staticObjects, mapMix, alwaysMix)` → `PhysicsScene`; called inside `GameServer.loadLevel()` after `LevelLoader`; result stored in `GameServer.physicsScene`
- W3D collision flag: `(mesh.attributes and W3D_MESH_FLAG_COLLISION_TYPE_PHYSICAL) != 0u` — only meshes with bit 0x10 set should be added to the scene
- StaticPhysClass save format: `STATICPHYS_CHUNK_PHYS(0xDC2F94)` → `PHYS_CHUNK_VARIABLES(0x660055)` (micro 0x06=definitionId) + `PHYS_CHUNK_MODEL(0x660056)` → `WW3D_PERSIST_CHUNKID_RENDEROBJ(0x10000)` → `RENDOBJFACTORY_CHUNKID_VARIABLES(0x555040)` (micro 0x01=modelName string, 0x02=transform 48 bytes row-major Matrix3D)

### ArmorWarheadManager — armor × warhead damage table
- `ArmorWarheadManager` is no longer a stub — loaded from `armor.ini` inside `always.dbs` during `loadLevel()` / `loadDefinitions()`
- Call: `ArmorWarheadManager.scaleDamage(rawDamage, warheadSaveId, armorSaveId)` — returns 1.0× if table not yet loaded
- `AmmoDefinitionClass.warhead` is the warhead save ID; `BuildingGameObj.shieldType` is the normal armor save ID; `BuildingGameObj.mctSkinSaveId` is the MCT alternate armor save ID (from `BuildingGameObjDef.mctSkin`)
- `IniFile` (ccr.server.mix) — simple section-based INI parser: `getString/getInt/getFloat/entryCount/getEntry`, case-insensitive, `;` comments

### Network Object Factory Pipeline
- `NetworkObjectFactory` interface in `ccr.net.replication` — matches C++ `NetworkObjectFactoryClass`; has `classId`, `prepPacket(obj, bs)`, `create(bs)`
- `NetworkObjectFactoryManager` object in `ccr.net.replication` — singleton registry mapping classId → factory; use `register()`, `getFactory()`
- `NetworkGameObjectFactory` (classId=1000) in `ccr.server.net` — `prepPacket` writes `definitionId:32`; `create` consumes it (server-only, returns null)
- `SimpleNetworkObjectFactory(classId)` — no-op prepPacket; used for event classIds 1001–1038 and classId=0 singletons
- `NetworkObjectFactories.register()` — call at server startup (already called in `GameServer.run()`); idempotent
- Wire format: `[networkId:32][dirtyBits:8][isDeletePending:1][classId:32][factory_data][exportCreation][exportRare][exportOccasional][exportFrequent]`
  - For classId=1000 (game objects): factory_data = `[definitionId:32]`
  - For classId=0, 1001–1038: factory_data = (empty)
- `NetworkFactoriesExtension` (JUnit5 auto-discovery) — ensures factories are registered for all server tests automatically; no per-test `@ExtendWith` needed
- Factory writes definitionId BETWEEN classId header and Export_Creation — matches C++ `messages.cpp:987–999`; `basegameobj.cpp:54–106`

### Live proxy (diagnostic tool)
- `gradlew :server:liveProxy -PlocalPort=4849 -PremoteHost=127.0.0.1 -PremotePort=4848 -PlogFile=.tmp/proxy_log.txt`
- Forwards client↔server traffic and decodes every packet in real-time
- `PacketDecoder` object: `ccr.server.net.PacketDecoder` — reusable decode logic for all packet analysis
- Captured logs go in `server/src/test/resources/proxy_log.txt` for use by `ProxyDecoderTest`
