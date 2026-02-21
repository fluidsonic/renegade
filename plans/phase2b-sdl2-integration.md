# Plan: SDL2 Integration — Window, Input, Rendering

## Context

The C&C Renegade macOS port compiles and links (814 source files, arm64 Mach-O). The binary runs but exits immediately because `WW3D::Init()` fails — `Direct3DCreate8()` returns NULL, so DX8Wrapper initialization fails, and `Game_Init()` returns false.

The game currently uses no-op stubs for: D3D8 (rendering), DirectInput (keyboard/mouse), and Win32 windowing (CreateWindowEx returns fake handle). The goal is to replace all three with real SDL2-backed implementations so the game opens a window, accepts input, and renders.

**Key architectural insight:** `DX8Wrapper` (in `ww3d2/dx8wrapper.cpp`) is the sole abstraction over D3D8 — game code never calls D3D8 directly. `DirectInput` (in `Combat/directinput.cpp`) is the sole abstraction over DirectInput8. Both are contained to single files. The replacement strategy is to rewrite the *internals* of these wrappers to use SDL2+OpenGL, keeping their public APIs unchanged.

## Step 0: Save this plan

Write this plan to `/plans/phase2b-sdl2-integration.md`.

## Step 1: Add SDL2 via FetchContent

**File: `original/CMakeLists.txt`**

Add after `find_package(Threads REQUIRED)`:
```cmake
include(FetchContent)
FetchContent_Declare(SDL2
    GIT_REPOSITORY https://github.com/libsdl-org/SDL.git
    GIT_TAG        release-2.30.11
    GIT_SHALLOW    TRUE
)
set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(SDL2)
```

**File: `original/Code/Commando/CMakeLists.txt`**

Add `SDL2::SDL2-static` to `target_link_libraries`.

Also add OpenGL framework link: `-framework OpenGL`.

**Verify:** `cmake --build build --target Commando` succeeds.

## Step 2: SDL2 platform layer

Create **`original/compat/sdl2_platform.h`** and **`original/compat/sdl2_platform.cpp`**:

A thin wrapper that owns the SDL2 lifecycle:
- `SDL2_Platform_Init()` — calls `SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS)`, creates the `SDL_Window*` with `SDL_WINDOW_OPENGL | SDL_WINDOW_SHOWN`, creates `SDL_GLContext` (OpenGL 2.1 compatibility profile)
- `SDL2_Platform_Shutdown()` — destroys context, window, `SDL_Quit()`
- `SDL2_Platform_GetWindow()` — returns `SDL_Window*`
- `SDL2_Platform_GetGLContext()` — returns `SDL_GLContext`
- `SDL2_Platform_SwapWindow()` — calls `SDL_GL_SwapWindow`
- `SDL2_Platform_PollEvents()` — calls `SDL_PollEvent` in a loop, dispatches to callbacks for input/focus/quit

This is compiled as part of the Commando target (or a new small static lib).

**Update `compat/winuser.h`:**
- `CreateWindowEx` → calls `SDL2_Platform_Init()`, returns `(HWND)SDL2_Platform_GetWindow()`
- The game stores this in `MainWindow` which gets passed to `WW3D::Init(MainWindow, ...)`

## Step 3: D3D8 → OpenGL translation in compat layer

**Replace `compat/d3d8.h`** stubs with real OpenGL-backed implementations. The D3D8 COM interfaces (`IDirect3D8`, `IDirect3DDevice8`, etc.) become real C++ classes that call OpenGL.

Split into files:
- **`compat/d3d8.h`** — interface declarations (keep existing struct layout)
- **`compat/d3d8_gl.cpp`** — OpenGL implementations

### IDirect3D8 (minimal):
| Method | Implementation |
|--------|---------------|
| `GetAdapterCount()` | Return `SDL_GetNumVideoDisplays()` |
| `GetAdapterDisplayMode()` | Use `SDL_GetCurrentDisplayMode()` |
| `GetAdapterIdentifier()` | Fill with SDL renderer info |
| `GetDeviceCaps()` | Fill D3DCAPS8 with reasonable defaults from GL caps |
| `CreateDevice()` | GL context already exists from Step 2. Create `IDirect3DDevice8` backed by it, return `S_OK` |
| `CheckDeviceFormat/Type/MultiSampleType/DepthStencilMatch` | Return `S_OK` (permissive) |
| `EnumAdapterModes/GetAdapterModeCount` | Use `SDL_GetNumDisplayModes/SDL_GetDisplayMode` |

### IDirect3DDevice8 (phased):

**Phase A — enough to pass init + run game loop (clear screen):**
| Method | Implementation |
|--------|---------------|
| `BeginScene()` | No-op, return `S_OK` |
| `EndScene()` | No-op, return `S_OK` |
| `Present()` | `SDL2_Platform_SwapWindow()`, return `S_OK` |
| `Clear()` | `glClearColor` + `glClearDepth` + `glClear(GL_COLOR_BUFFER_BIT \| GL_DEPTH_BUFFER_BIT)` |
| `SetViewport()` | `glViewport()` + `glDepthRange()` |
| `SetRenderState()` | Store state, return `S_OK` (implement mapping later) |
| `SetTransform()` | Store matrix, return `S_OK` (implement GL matrix later) |
| `SetTexture()` | Store ref, return `S_OK` |
| `SetTextureStageState()` | Store state, return `S_OK` |
| `SetMaterial()` | Store, return `S_OK` |
| `SetLight/LightEnable()` | Store, return `S_OK` |
| `SetStreamSource/SetIndices/SetVertexShader()` | Store, return `S_OK` |
| `DrawIndexedPrimitive()` | No-op, return `S_OK` (no geometry yet) |
| `CreateTexture()` | Allocate stub texture object, return `S_OK` |
| `CreateVertexBuffer/CreateIndexBuffer()` | Allocate stub buffer, return `S_OK` |
| `GetBackBuffer/GetFrontBuffer/CreateImageSurface` | Return stub surface |
| `TestCooperativeLevel()` | Return `S_OK` |
| `Reset()` | Resize SDL window, return `S_OK` |
| `GetDisplayMode()` | Use SDL to query, return `S_OK` |
| `SetGammaRamp()` | `SDL_SetWindowGammaRamp()` or no-op |

**Phase B — actual geometry rendering (follow-up plan):**
Implement `SetRenderState`, `SetTransform`, `SetTexture`, `DrawIndexedPrimitive` with real GL calls. Use OpenGL 2.1 fixed-function pipeline which maps 1:1 to D3D8's fixed-function model.

### `Direct3DCreate8()`:
Instead of returning NULL, allocate and return a real `IDirect3D8` instance backed by the SDL2/GL platform.

### `DX8Wrapper::Init()` flow after changes:
1. `LoadLibrary("D3D8.DLL")` → returns non-NULL (already stubbed)
2. `GetProcAddress(lib, "Direct3DCreate8")` → returns pointer to our function
3. `Direct3DCreate8(D3D_SDK_VERSION)` → returns real `IDirect3D8*` backed by SDL2/GL
4. Enumerates adapters (delegates to SDL display enumeration)
5. `D3DInterface->CreateDevice(...)` → returns real `IDirect3DDevice8*` backed by GL context
6. WW3D init succeeds → `Game_Init()` continues

## Step 4: Replace DirectInput with SDL2 input

**Rewrite `original/Code/Combat/directinput.cpp`:**

Keep the public API identical (`DirectInput::Init/Shutdown/Read/Acquire/Unacquire`, `DIKeyboardButtons[256]`, `DIMouseButtons[3]`, `DIMouseAxis[3]`). Replace the internals:

### Init:
- No DLL loading needed. Just set `Captured = true`, enable SDL relative mouse mode.

### ReadKeyboard:
- Call `SDL_GetKeyboardState()` to get current key state
- Map `SDL_SCANCODE_*` → `DIK_*` via a translation table (both are scan codes, mostly the same layout)
- Synthesize HIT/HELD/RELEASED bit transitions by comparing current vs previous frame state

### ReadMouse:
- Call `SDL_GetRelativeMouseState(&dx, &dy)` for axis deltas
- Query mouse button state from SDL, map to `DIMouseButtons[]` with HIT/HELD/RELEASED transitions
- Mouse wheel: track via `SDL_MOUSEWHEEL` events accumulated during `SDL2_Platform_PollEvents()`

### ReadJoystick:
- Use `SDL_JoystickOpen` / `SDL_JoystickGetAxis` / `SDL_JoystickGetButton` or skip initially (keyboard/mouse is priority)

### Acquire/Unacquire:
- `Acquire()`: `SDL_SetRelativeMouseMode(SDL_TRUE)`, `SDL_ShowCursor(SDL_DISABLE)`
- `Unacquire()`: `SDL_SetRelativeMouseMode(SDL_FALSE)`, `SDL_ShowCursor(SDL_ENABLE)`

### Scancode translation table (`SDL_SCANCODE` → `DIK_*`):
Both use hardware scan codes so most map directly. Key differences to handle:
- `SDL_SCANCODE_A (4)` → `DIK_A (0x1E)` (SDL uses USB HID codes, DirectInput uses AT scan codes)
- Need a ~120-entry lookup table

## Step 5: Replace Win32 message loop with SDL2 events

**Modify `original/Code/wwlib/msgloop.cpp`:**

Replace `Windows_Message_Handler()`:
```cpp
void Windows_Message_Handler(void) {
    SDL2_Platform_PollEvents(); // drains SDL event queue
}
```

**In `SDL2_Platform_PollEvents()`**, dispatch SDL events:
| SDL Event | Action |
|-----------|--------|
| `SDL_QUIT` | Set `RunMainLoop = false` |
| `SDL_WINDOWEVENT_FOCUS_GAINED` | Set `GameInFocus = true`, call `On_Focus_Restore()` |
| `SDL_WINDOWEVENT_FOCUS_LOST` | Set `GameInFocus = false`, call `On_Focus_Loss()` |
| `SDL_TEXTINPUT` | Feed each char to `Input::Console_Add_Key()` |
| `SDL_KEYDOWN` (Alt+Enter) | Call `WW3D::Toggle_Windowed()` |
| `SDL_MOUSEWHEEL` | Accumulate for DirectInput mouse Z axis |

**The `_TheWWUIInput->ProcessMessage()` call** in `Main_Window_Proc` needs investigation — the UI system may need SDL events translated to WM_ messages, or we may need to feed it directly. Initially skip this (UI won't work until we bridge it).

## Step 6: Wire up WinMain entry point

The current `WinMain` is the entry point. On macOS with SDL2, `SDL_main` redefines `main`. Options:
- Use `#define SDL_MAIN_HANDLED` before including SDL.h, then call `SDL_SetMainReady()` in WinMain
- Or rename WinMain to main on macOS (it already compiles via the compat `PASCAL`/`WINAPI` macros)

The cleanest approach: add `#define SDL_MAIN_HANDLED` in the compat layer, call `SDL_SetMainReady()` at the top of WinMain.

## Files Modified

| File | Change |
|------|--------|
| `original/CMakeLists.txt` | Add SDL2 FetchContent |
| `original/Code/Commando/CMakeLists.txt` | Link SDL2, OpenGL framework |
| **NEW** `original/compat/sdl2_platform.h` | SDL2 platform wrapper declarations |
| **NEW** `original/compat/sdl2_platform.cpp` | SDL2 window, GL context, event loop |
| **NEW** `original/compat/d3d8_gl.cpp` | D3D8 → OpenGL translation implementation |
| `original/compat/d3d8.h` | Change stubs to real class declarations backed by GL |
| `original/Code/Combat/directinput.cpp` | Rewrite internals with SDL2 input |
| `original/Code/wwlib/msgloop.cpp` | Replace Win32 message pump with SDL2 |
| `original/Code/Commando/WINMAIN.CPP` | SDL_MAIN_HANDLED, minor adjustments |
| `original/compat/winuser.h` | CreateWindowEx delegates to SDL2 platform |

## Verification

```bash
cd /Users/marc/Documents/ccr/original && cmake --build build --target Commando -j8
cd /Users/marc/Documents/ccr && ./original/build/Code/Commando/Commando 2>&1
```

**Phase A success criteria:**
- SDL2 window opens (800x600, titled "Renegade")
- Game loop runs (trace shows `Game_Init() returned true`, `Entering main loop`)
- Screen clears to black each frame (OpenGL glClear working)
- Keyboard/mouse input is read (no crashes in Input::Update)
- Window close (X button or Cmd-Q) cleanly exits

**Phase B (follow-up):** Actual geometry rendering — D3D8 render states, transforms, textures, and draw calls mapped to OpenGL fixed-function pipeline.
