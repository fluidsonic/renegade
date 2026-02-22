#pragma once

#include "global.h"

// sdl2_platform.h — SDL2 platform layer for macOS port
// Owns the SDL2 lifecycle: window, GL context, event dispatch.
// Pure C declarations (no SDL2 headers) so this can be included widely.

#ifdef __cplusplus
extern "C" {
#endif

// Opaque SDL types (avoid including SDL.h here)
typedef void SDL2_Window;
typedef void* SDL2_GLContext;

// Mouse wheel accumulator — read and cleared by DirectInput each frame
extern int SDL2_MouseWheelDelta;

// Quit flag — set by SDL_QUIT event; game loop polls this
extern int SDL2_QuitRequested;

// Focus state — updated by window focus events
extern int SDL2_HasFocus;

// Initialise SDL2, create window (800x600) and OpenGL 2.1 context.
// Returns 0 on success, -1 on failure.
int  SDL2_Platform_Init(const char* title, int w, int h);

// Tear down SDL2 context, window, and SDL itself.
void SDL2_Platform_Shutdown(void);

// Return the SDL_Window* (as void* to avoid SDL.h dependency here).
SDL2_Window* SDL2_Platform_GetWindow(void);

// Return the SDL_GLContext.
SDL2_GLContext SDL2_Platform_GetGLContext(void);

// Swap the OpenGL front/back buffers.
void SDL2_Platform_SwapWindow(void);

// Drain the SDL event queue.  Updates SDL2_QuitRequested, SDL2_HasFocus,
// SDL2_MouseWheelDelta, and calls the focus callbacks declared below.
void SDL2_Platform_PollEvents(void);

// Focus callbacks — implemented in WINMAIN.CPP; called from PollEvents.
// Declared as weak so translation units that don't define them link cleanly.
void On_Focus_Loss(void);
void On_Focus_Restore(void);

// macOS CoreAnalytics ARM64 alignment-fault workaround (macos_fix.mm).
// Call install before SDL_Init; call remove after SDL_Init returns.
// These are no-ops when SIGBUS is already handled (forward to prev handler).
// NOTE: The SIGBUS handler does NOT fire under lldb — the ObjC swizzles in
// macos_fix.mm cover the two known crash paths and work under lldb.
void macos_install_alignment_fault_workaround(void);
void macos_remove_alignment_fault_workaround(void);
// Bring the app to the foreground after window creation.
// Terminal-launched apps are not automatically focused on macOS.
void macos_activate_app(void);

#ifdef __cplusplus
} // extern "C"
#endif
