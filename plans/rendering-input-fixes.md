# Plan: Fix Rendering & Input (Always Render, Cursor Offset, Click-to-Capture)

## Context

The macOS SDL2 port has three rendering/input issues:
1. The game stops rendering when the window loses focus (`GameInFocus` gates in the render path)
2. The in-game cursor is offset from the OS cursor (a `*2` multiplier on mouse deltas)
3. There is no click-to-capture model — input is gated only on window focus, and `SDL_SetRelativeMouseMode(SDL_TRUE)` is never called

## Approach

Use a worktree for isolation. Introduce a new `SDL2_MouseCaptured` state that is set by clicking into the window and cleared on focus loss. Remove all `GameInFocus` gates from rendering. Gate input on capture state instead of focus. Fix the cursor multiplier and seed cursor position from the capture click.

## Step 0: Create worktree

Create a worktree at `.worktrees/` for this feature branch.

## Step 1: Add capture state to SDL platform layer

**`original/compat/sdl2_platform.h`** — Add after `SDL2_HasFocus` extern (line 23):
```cpp
extern int SDL2_MouseCaptured;
extern int SDL2_CaptureClickX;
extern int SDL2_CaptureClickY;
```

**`original/compat/sdl2_platform.cpp`** — Add globals after line 14:
```cpp
int SDL2_MouseCaptured   = 0;
int SDL2_CaptureClickX   = 0;
int SDL2_CaptureClickY   = 0;
int SDL2_ShouldRender    = 1;   // 0 when minimized or fullscreen+unfocused
```

Add static minimized tracker and `update_should_render()` helper.
Call `update_should_render()` in FOCUS_GAINED, FOCUS_LOST handlers.
Add `SDL_WINDOWEVENT_MINIMIZED` (set flag → update) and `SDL_WINDOWEVENT_RESTORED` (clear flag → update) handlers.

Add `SDL_MOUSEBUTTONDOWN` handler in the event loop (before `default:` at line 137):
```cpp
case SDL_MOUSEBUTTONDOWN:
    if (!SDL2_MouseCaptured && SDL2_HasFocus) {
        SDL2_CaptureClickX = event.button.x;
        SDL2_CaptureClickY = event.button.y;
        SDL_SetRelativeMouseMode(SDL_TRUE);
        SDL2_MouseCaptured = 1;
    }
    break;
```

In `SDL_WINDOWEVENT_FOCUS_LOST` handler (lines 121-127), release capture before calling `On_Focus_Loss`:
```cpp
case SDL_WINDOWEVENT_FOCUS_LOST:
    if (SDL2_HasFocus) {
        SDL2_HasFocus = 0;
        GameInFocus = false;
        if (SDL2_MouseCaptured) {
            SDL_SetRelativeMouseMode(SDL_FALSE);
            SDL2_MouseCaptured = 0;
        }
        On_Focus_Loss();
    }
    break;
```

## Step 2: Fix DirectInput cursor tracking

**`original/Code/Combat/directinput.cpp`**

Remove `*2` multiplier in `ReadMouse()` (lines 272-273):
```cpp
// OLD: CursorPos.X += (float)(dx * 2);  CursorPos.Y += (float)(dy * 2);
// NEW:
CursorPos.X += (float)dx;
CursorPos.Y += (float)dy;
```

Update `Acquire()` (lines 186-195) to seed cursor from click position:
```cpp
void DirectInput::Acquire(void)
{
    if (!Captured) {
        Flush();
        CursorPos.X = (float)SDL2_CaptureClickX;
        CursorPos.Y = (float)SDL2_CaptureClickY;
        SDL_GetRelativeMouseState(nullptr, nullptr); // flush stale deltas
        Captured = true;
        EatMouseHeld = true; // suppress the capture click
    }
}
```

## Step 3: Gate input on capture state

**`original/Code/Combat/input.cpp`** — Add `#include "sdl2_platform.h"` near top.

Replace lines 793-796:
```cpp
// OLD:
if (!GameInFocus) {
    DirectInput::Unacquire();
    return;
}
// NEW:
if (!SDL2_MouseCaptured) {
    DirectInput::Unacquire();
    return;
}
DirectInput::Acquire();
```

## Step 4: Replace rendering gates with `SDL2_ShouldRender`

`SDL2_ShouldRender` is 0 when minimized or (fullscreen + no focus); 1 otherwise.
Windowed+unfocused still renders.

**`original/Code/Commando/gamemode.cpp`** — add `#include "sdl2_platform.h"`

- Replace `if (!GameInFocus) do_pscene=false;` → `if (!SDL2_ShouldRender) do_pscene=false;`
- Wrap game mode render loop with `if (SDL2_ShouldRender) { ... }`
- Wrap `BINKMovie::Render()` with `if (SDL2_ShouldRender) { ... }`

## Step 5: Fix dialog manager rendering

**`original/Code/wwui/dialogmgr.cpp`** — Add `#include "sdl2_platform.h"` near top.
Remove unused `static bool GameWasInFocus;`.

Replace the `GameInFocus`/`GameWasInFocus` block with:
```cpp
if (!SDL2_ShouldRender) {
    return;
}
{
    static bool WasCaptured = false;
    bool isCaptured = (SDL2_MouseCaptured != 0);
    if (isCaptured && !WasCaptured) {
        DialogMgrClass::Reset();
    }
    WasCaptured = isCaptured;
}
```

## Step 6: Remove auto-acquire on focus restore

**`original/Code/Commando/WINMAIN.CPP`** — In `On_Focus_Restore()` (lines 728-733), remove `DirectInput::Acquire();`. Keep `GameModeManager::Hide_Render_Frames(1);`.

## Files Modified

| File | Change |
|------|--------|
| `original/compat/sdl2_platform.h` | Export `SDL2_MouseCaptured`, `SDL2_CaptureClickX/Y` |
| `original/compat/sdl2_platform.cpp` | Add capture globals, MOUSEBUTTONDOWN handler, focus-lost uncapture |
| `original/Code/Combat/directinput.cpp` | Remove `*2`, seed CursorPos in Acquire(), flush stale deltas |
| `original/Code/Combat/input.cpp` | Gate on `SDL2_MouseCaptured` instead of `GameInFocus` |
| `original/Code/Commando/gamemode.cpp` | Remove 3 `GameInFocus` rendering gates |
| `original/Code/wwui/dialogmgr.cpp` | Replace focus gate with capture-transition Reset |
| `original/Code/Commando/WINMAIN.CPP` | Remove `Acquire()` from `On_Focus_Restore()` |

## Notes

- `GameInFocus` is preserved — still used by `combatgmode.cpp:152` for singleplayer auto-menu on focus loss
- The capture click is "eaten" via `EatMouseHeld = true` so it doesn't trigger in-game actions
- Cursor position maps 1:1 (window 800x600 = game resolution 800x600)
- No escape-to-uncapture is included — only focus loss releases capture (can be added as follow-up)

## Verification

1. `cmake --build original/build --target Commando -j8`
2. `MallocNanoZone=0 original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
3. **Always render:** Cmd+Tab away — game window should keep rendering
4. **Cursor offset:** Click to capture, move mouse in menus — cursor tracks 1:1
5. **Click-to-capture:** OS cursor visible over unfocused window; click captures (OS cursor hides); Cmd+Tab releases; click again re-captures
