# Plan: Port C&C Renegade Original Client to Compile on macOS

## Context

The original C&C Renegade source code (GPL, EA 2025 release) at `original/Code/` is a ~3,100 file C++ codebase from ~2002. It was built with MSVC 6 targeting Win32 x86, with deep dependencies on DirectX 8, Windows API, Miles Sound System, Bink Video, and Winsock. The goal is to make the **full game client** compile natively on macOS (Apple Silicon) using clang and CMake, working in a git worktree for isolation.

## Approach

**Shim-first strategy**: Create a `compat/` directory with replacement headers (`windows.h`, `d3d8.h`, `mss.h`, etc.) that provide the correct types and stub implementations. This is placed first on the include path so `#include <windows.h>` resolves to our shim. This minimizes changes to original source files.

**CMake build system**: Mirror the .dsp workspace structure with per-library CMake targets referencing original source via absolute path.

**Phased compilation**: Start with smallest/lowest-dependency libraries and work up to the main executable.

## Step 0: Save this plan & create worktree

1. Write this plan to `/plans/macos-client-port.md`
2. Create a git worktree branched from `main` for the macOS port work
3. In the worktree, create the `original-port/` directory structure for CMake + compat layer
4. The CMake files reference original source at `/Users/marc/Documents/ccr/original/Code/` via absolute path

## Step 1: CMake infrastructure + core compat headers

### Files to create

**`original-port/CMakeLists.txt`** (top-level):
- `cmake_minimum_required(VERSION 3.20)`, `project(CnCRenegade CXX C)`
- C++17 standard (for atomics, stdint)
- Define `_UNIX` globally via `add_compile_definitions(_UNIX)`
- `compat/` as FIRST include directory for all targets (intercepts `<windows.h>` etc.)
- Clang warning suppressions: `-Wno-pragma-once-outside-header`, `-Wno-unknown-pragmas`, `-Wno-register`, `-Wno-writable-strings`, `-Wno-invalid-offsetof`, `-Wno-narrowing`, `-Wno-int-to-pointer-cast`, `-Wno-comment`, `-Wno-microsoft-include`, `-Wno-nonportable-include-path`
- `add_subdirectory()` for each library and the main executable

**`original-port/compat/` headers**:

| Header | Purpose |
|--------|---------|
| `windows.h` | Core Windows types (HANDLE, HWND, HINSTANCE, etc.), calling conventions (WINAPI, CALLBACK as no-ops), includes sub-headers |
| `windef.h` | RECT, POINT, SIZE, COLORREF, MAKELONG, etc. |
| `winnt.h` | LARGE_INTEGER, CRITICAL_SECTION (wrapping pthread_mutex_t), LONG, SHORT, etc. |
| `winbase.h` | File I/O stubs, Sleep/GetTickCount, threading (CreateThread/WaitForSingleObject), GetLastError, registry stubs |
| `winuser.h` | Message constants (WM_*), WNDCLASS, MessageBox stub, window function stubs |
| `wingdi.h` | Minimal GDI types (HDC, HBITMAP, LOGPALETTE, etc.) |
| `winsock.h` / `winsock2.h` | Map Winsock types to BSD sockets (SOCKET=int, SOCKADDR_IN=struct sockaddr_in, WSAStartup=no-op, closesocket=close) |
| `mmsystem.h` | timeGetTime() via clock_gettime, MMRESULT, timer stubs |
| `d3d8.h` + `d3d8types.h` + `d3d8caps.h` | D3D8 enums, structs, COM-style interface stubs (IDirect3D8, IDirect3DDevice8, etc.) |
| `d3dx8math.h` | D3DX math function stubs |
| `ddraw.h` | Minimal DirectDraw types (DDSURFACEDESC2, IDirectDraw, etc.) |
| `dinput.h` | DirectInput types and interface stubs |
| `dsound.h` | DirectSound stubs |
| `direct.h` | `_mkdir` -> `mkdir`, `_chdir` -> `chdir`, `_getcwd` -> `getcwd` |
| `process.h` | `_beginthread` -> pthread_create wrapper |
| `tchar.h` | TCHAR=char, _T()=passthrough, _tcslen=strlen, etc. |
| `crtdbg.h` | Empty/no-op debug CRT |
| `io.h` | `_access` -> `access`, `_open`/`_close`/`_read`/`_write` -> POSIX equivalents |
| `osdep.h` | The missing Unix compat header (stricmp=strcasecmp, _snprintf=snprintf, etc.) |
| `mss.h` | Miles Sound System type stubs (HSAMPLE, HDIGDRIVER, etc.) - all functions no-op |
| `bink.h` | Bink video type stubs - all functions no-op |
| `commctrl.h` | Common controls stubs |
| `shlobj.h` | Shell API stubs (SHGetFolderPath, etc.) |

**Critical type-size issue**: `bittype.h` defines `uint32` as `unsigned long` which is 4 bytes on Win32 but **8 bytes on 64-bit macOS**. Fix: create a `compat/bittype_fix.h` that `always.h` includes on clang, redefining `uint32`/`sint32`/`DWORD`/`ULONG` to use `unsigned int`/`signed int` (4 bytes on all platforms).

### Source modifications (minimal, via `#ifdef` guards)

- **`always.h`**: Add `#if defined(__clang__)` block alongside existing Borland/MSVC/Watcom blocks to include `"clangcompat.h"` (new compat header for clang-specific settings and bittype fixes)
- **`bittype.h`**: Wrap `unsigned long` typedefs with `#ifndef __clang__` and provide `unsigned int` alternatives
- **`mutex.h`**: Wrap `FastCriticalSectionClass::Thread_Safe_Set_Flag()` inline asm with `#ifdef _MSC_VER` and provide atomic alternative for clang using `__atomic_test_and_set`
- **`wwdebug.h`**: Add `#elif defined(__clang__)` for `WWDEBUG_BREAK` using `__builtin_debugtrap()`

## Step 2: Compile wwdebug (4 source files - simplest library)

**Source**: `wwdebug/wwdebug.cpp`, `wwdebug/wwmemlog.cpp`, `wwdebug/wwprofile.cpp`

Key fixes:
- `OutputDebugString` -> `fprintf(stderr, ...)`
- `__asm rdtsc` in profiler -> `mach_absolute_time()` or `clock_gettime(CLOCK_MONOTONIC)`
- `_asm int 3` -> `__builtin_debugtrap()`

## Step 3: Compile WWMath (38 source files - pure math)

**Source**: `WWMath/*.cpp`

Key fixes:
- `wwmath.h` already has non-MSVC fallbacks for Sin/Cos/Sqrt - should work
- `vp.cpp` extensive x86 SSE asm -> provide C fallbacks under `#ifndef _MSC_VER`
- Some files include `osdep.h` under `_UNIX` -> our compat provides this

## Step 4: Compile wwbitpack (5 source files - no Windows deps)

**Source**: `wwbitpack/*.cpp`

Should compile with minimal issues once wwdebug and WWMath are available. Depends on `vector.h` from wwlib (just the header, not implementation).

## Step 5: Compile wwlib (largest core library, ~160 files)

**Source**: `wwlib/*.cpp`

Split approach - compile incrementally:

**Batch A - Data structures & strings** (low Windows dependency):
- `stringclass.cpp`, `hashtemplate.cpp`, `vector.cpp`, `multilist.cpp`, `slist.cpp`
- `crc.cpp`, `sha.cpp`, `blowfish.cpp` (crypto)
- `straw.cpp`, `pipe.cpp`, `buffer.cpp` (I/O primitives)
- `random.cpp`, `srandom.cpp`, `strtok_r.cpp`

**Batch B - File I/O** (has `_UNIX` guards):
- `rawfile.cpp` (existing POSIX fallbacks via `_UNIX`), `bufffile.cpp`, `ramfile.cpp`
- `ffactory.cpp`, `filefind.cpp`
- `ini.cpp`, `iniclass.cpp`

**Batch C - Threading** (needs real pthread implementation):
- `thread.cpp`, `mutex.cpp`, `critsection.cpp`
- Replace the `_UNIX` no-op stubs with real pthread calls

**Batch D - Platform** (heavy stubs):
- `registry.cpp` -> file-backed INI replacement
- `win.cpp` -> provide global stubs for `ProgramInstance`, `MainWindow`, `GameInFocus`
- `systimer.cpp` -> `clock_gettime` implementation
- `cpudetect.cpp` -> stub reporting generic CPU
- `keyboard.cpp` -> stub
- `Except.cpp` -> already `#ifdef _MSC_VER`, compiles to empty
- `ddraw.cpp`, `dsurface.cpp` -> need DDraw compat types
- `WWCOMUtil.cpp` -> stub or exclude

## Step 6: Compile wwutil, wwsaveload, wwtranslatedb

Small libraries, mostly depend on wwlib types. Low Windows-specific code.
- `wwutil/stackdump.cpp` uses `<imagehlp.h>` -> stub entirely

## Step 7: Compile wwnet (Winsock -> BSD sockets)

17 source files. Our `compat/winsock.h` maps:
- `SOCKET` -> `int`, `INVALID_SOCKET` -> `-1`, `SOCKET_ERROR` -> `-1`
- `WSAStartup`/`WSACleanup` -> no-op
- `WSAGetLastError` -> `errno`
- `closesocket` -> `close`
- `ioctlsocket` -> `fcntl`
- `SOCKADDR_IN` -> `struct sockaddr_in`

## Step 8: Compile ww3d2 (222 files - D3D8 stubs)

The rendering engine. All D3D8 calls go through `dx8wrapper.h/cpp`. Our `compat/d3d8.h` provides:
- All D3D8 enums (D3DFORMAT, D3DPOOL, D3DPRIMITIVETYPE, D3DRENDERSTATETYPE, etc.)
- All D3D8 structs (D3DPRESENT_PARAMETERS, D3DCAPS8, D3DVIEWPORT8, D3DLIGHT8, D3DMATERIAL8, etc.)
- COM-style interface class stubs with virtual methods returning `E_NOTIMPL`
- D3DX utility function stubs

This is ~500-800 lines of stub header but is well-defined (D3D8 API is frozen/documented).

## Step 9: Compile remaining libraries

- **wwphys** (195 files): Physics. Depends on WWMath + ww3d2 types. Low direct Windows use.
- **WWAudio** (46 files): Wraps Miles. `compat/mss.h` makes all calls no-ops.
- **BinkMovie** (8 files): Wraps Bink. `compat/bink.h` makes all calls no-ops.
- **wwui** (86 files): UI framework. Uses D3D8 types for rendering.
- **Combat** (265 files): Game logic. DirectInput for input.
- **Scripts** (99 files): Game scripts DLL (compile as static lib).
- **SControl** (4 files): RCON server.
- **BandTest**: Bandwidth test.
- **GameSpy**: Directory missing from GPL release -> stub or exclude.

## Step 10: Compile Commando executable

212 source files. Key changes:
- `WINMAIN.CPP`: `WinMain` -> `main()`, stub window creation, provide event loop skeleton
- Dialog files (`dlg*.cpp`): Many reference Windows dialogs -> stub
- WOL files (`DlgWOL*.cpp`): Westwood Online (dead) -> stub
- WebBrowser COM -> stub

## Key risks and mitigations

| Risk | Mitigation |
|------|-----------|
| `unsigned long` is 8 bytes on macOS (not 4) | Fix in `bittype.h` via `#ifdef`, use `unsigned int` for 32-bit types |
| D3D8 stub header complexity | Build incrementally - add types as compile errors reveal needs |
| Pointer-to-int casts | Original code casts `void*` to `unsigned long` -> needs `uintptr_t` fixes |
| WCHAR is 4 bytes on macOS (not 2) | Define `WCHAR` as `uint16_t` in compat layer |
| GameSpy SDK missing | Exclude from build, provide empty stubs if headers are referenced |
| Inline x86 assembly (~30 files) | Guard with `#ifdef _MSC_VER`, provide C fallbacks |

## Verification

After each step, run:
```bash
cmake -B build -G Ninja -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++
cmake --build build --target <library> 2>&1 | tail -20
```

Track progress by error count per library. The build is "compiling" when `cmake --build build` succeeds with 0 errors.

## Files to modify in original source (kept minimal)

1. `original/Code/wwlib/always.h` - Add clang compiler detection block
2. `original/Code/wwlib/bittype.h` - Fix 64-bit type sizes
3. `original/Code/wwlib/mutex.h` - Replace x86 asm with atomics
4. `original/Code/wwdebug/wwdebug.h` - Replace `_asm int 0x03`
5. `original/Code/wwlib/cpudetect.cpp` - Guard x86 asm, provide stub
6. `original/Code/WWMath/vp.cpp` - Guard x86 SSE asm, provide C fallback
7. Various files with `_UNIX` no-op stubs - Replace with real implementations
