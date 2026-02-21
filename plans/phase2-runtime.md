# Plan: Port C&C Renegade Original Client to macOS — Phase 2: Runtime

## Status

**Compilation: COMPLETE.** 814 source files compile and link to a 12MB arm64 Mach-O binary. Zero `REMOVE_ITEM` exclusions. Every `.cpp` in `original/Code/` either compiles or has been deleted and replaced with a compat stub.

## Context

The binary links but is entirely non-functional — all Windows, D3D8, Miles, Bink, and DirectInput APIs are no-op stubs. The next step is to run the binary, observe what happens, and fix compat stubs so the startup flow progresses further.

## Step 1: Delete `BandTest/`

Delete `original/Code/BandTest/` directory entirely. `BandTest.h` is already in `compat/` (via `commando_stubs.cpp` include), and `Detect_Bandwidth()` is already stubbed.

## Step 2: Run the binary and observe

Run `./build/Commando/Commando` and capture output (stdout/stderr). The binary will likely crash or exit early. Record the exact failure.

## Step 3: Fix compat stubs for full graphical startup

The game must run with full UI — no dedicated server / headless shortcut.

Based on the WinMain startup analysis, these are the failure points in order:

| # | Call site | Stub behavior needed |
|---|-----------|---------------------|
| 1 | `CreateMutex()` in `singletoninstancekeeper.cpp:77` | Return non-NULL HANDLE |
| 2 | `SearchPath("data", "always.dat", ...)` in `WINMAIN.CPP:436` | Return non-zero (or game exits with "Set working folder") |
| 3 | `RegisterClass()` + `CreateWindowEx()` in `WINMAIN.CPP:719-728` | Return non-zero / non-NULL |
| 4 | `LoadLibrary("D3D8.DLL")` in `dx8wrapper.cpp:209` | Return non-NULL |
| 5 | `Direct3DCreate8()` via function pointer in `dx8wrapper.cpp:220` | Return valid IDirect3D8* COM stub |
| 6 | `IDirect3D8::CreateDevice()` in `dx8wrapper.cpp` | Return valid IDirect3DDevice8* COM stub |

### Specific compat fixes:
- `compat/winbase.h`: `CreateMutex` returns `(HANDLE)1`, `SearchPath` returns `1`, `GetModuleFileName` uses `_NSGetExecutablePath` on macOS
- `compat/winuser.h`: `RegisterClass` returns `1`, `CreateWindowEx` returns `(HWND)1`
- `compat/winbase.h`: `LoadLibrary` returns `(HMODULE)1`, `GetProcAddress` returns function pointers for known DLLs (D3D8)
- `compat/d3d8.h`: IDirect3D8/IDirect3DDevice8 stubs must return `S_OK` from `CreateDevice`, `BeginScene`, `EndScene`, `Present`, etc. — not just `E_NOTIMPL`

## Step 4: Iterate on runtime crashes

Run the binary after each fix, observe the next crash point, fix it, repeat. Likely areas:
- File factory setup (MIX file loading — needs game data files)
- `FindFirstFile`/`FindNextFile` enumeration stubs
- Registry access returning sensible defaults
- D3D8 device methods called during rendering setup
- DirectInput initialization for keyboard/mouse
- WWAudio / Miles initialization (should be safe with no-op stubs)

## Verification

```bash
cd /Users/marc/Documents/ccr/.worktrees/macos-port/original-port
./build/Commando/Commando 2>&1 | head -50
```

Progress is measured by how far the startup flow gets before failing.
