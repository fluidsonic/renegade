# Fix: Game hangs in SDL_GL_SwapWindow on quit (SIGTERM)

## Context

When the game is told to quit (Cmd-Q, window close, `kill`), SDL2 pushes `SDL_QUIT`. But the main thread is blocked in `SDL_GL_SwapWindow` (vsync condition wait), and the event isn't processed because `Windows_Message_Handler()` runs AFTER `GameModeManager::Render()` in the main loop. If the display link stops firing (Cocoa tearing down the window), the swap blocks forever and macOS escalates to SIGTERM.

Additionally, `SDL2_Platform_Shutdown()` is never called — the SDL window and GL context leak on exit.

## Fix

### 1. Move event polling before rendering (`mainloop.cpp`)

Move `Windows_Message_Handler()` from after `GameModeManager::Render()` (line 120) to the top of `_Game_Main_Loop_Loop()`, before `TimeManager::Update()`. This ensures `SDL_QUIT` is detected before the frame's render/swap, so `Game_Main_Loop` breaks before entering another swap.

### 2. Call `SDL2_Platform_Shutdown()` in `Game_Shutdown()` (`shutdown.cpp`)

Add `SDL2_Platform_Shutdown()` at the end of `Game_Shutdown()`, after `WW3D::Shutdown()` has released GL resources. This destroys the SDL window and calls `SDL_Quit()`.

### 3. Add shutdown logging

Add `fprintf(stderr, ...)` traces at key points so we can follow the shutdown sequence:

- `sdl2_platform.cpp` — `SDL2_Platform_PollEvents` when `SDL_QUIT` is received
- `sdl2_platform.cpp` — `SDL2_Platform_Shutdown` entry/exit
- `mainloop.cpp` — `Stop_Main_Loop` when called
- `mainloop.cpp` — `Game_Main_Loop` when breaking out due to `SDL2_QuitRequested`
- `shutdown.cpp` — `Game_Shutdown` entry and before/after `SDL2_Platform_Shutdown`

## Files to modify

- `original/Code/Commando/mainloop.cpp` — move `Windows_Message_Handler()` to top of loop, add logging
- `original/Code/Commando/shutdown.cpp` — add `SDL2_Platform_Shutdown()` call, add logging
- `original/compat/sdl2_platform.cpp` — add logging to SDL_QUIT handling and shutdown

## Verification

- Build: `cmake --build original/build --target Commando -j8`
- Run the game, close window or Cmd-Q — should exit cleanly, shutdown trace visible in stderr
- Run the game, `kill <pid>` — should exit cleanly via SDL's SIGTERM → SDL_QUIT → poll → quit
