// sdl2_platform.cpp — SDL2 platform layer implementation
#define SDL_MAIN_HANDLED
#include "global.h"
#include <SDL2/SDL.h>
#include <stdio.h>
#include "sdl2_platform.h"
#include "win.h"   // GameInFocus

static SDL_Window*    s_window    = NULL;
static SDL_GLContext  s_glcontext = NULL;

int SDL2_MouseWheelDelta = 0;
int SDL2_QuitRequested   = 0;
int SDL2_HasFocus        = 1;   // window starts focused
int SDL2_MouseCaptured   = 0;
int SDL2_CaptureClickX   = 0;
int SDL2_CaptureClickY   = 0;
int SDL2_ShouldRender    = 1;   // 0 when minimized or fullscreen+unfocused

static int s_minimized = 0;

// Recompute SDL2_ShouldRender from current state.
// Must be called whenever minimized or focus state changes.
static void update_should_render(void)
{
    int fullscreen = s_window
        ? (int)(SDL_GetWindowFlags(s_window) & (SDL_WINDOW_FULLSCREEN | SDL_WINDOW_FULLSCREEN_DESKTOP))
        : 0;
    SDL2_ShouldRender = !s_minimized && (!fullscreen || SDL2_HasFocus);
}

int SDL2_Platform_Init(const char* title, int w, int h)
{
    // Install SIGBUS handler before SDL_Init so that if any CoreAnalytics
    // alignment-fault paths survive the ObjC swizzles in macos_fix.mm,
    // they are silently skipped rather than crashing.
    // (Does not fire under lldb — swizzles in macos_fix.mm cover lldb case.)
    macos_install_alignment_fault_workaround();

    int sdl_rc = SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS | SDL_INIT_JOYSTICK);
    macos_remove_alignment_fault_workaround();

    if (sdl_rc != 0) {
        fprintf(stderr, "[SDL2] SDL_Init failed: %s\n", SDL_GetError());
        return -1;
    }

    // Request OpenGL 2.1 compatibility profile (matches D3D8 fixed-function)
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 2);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 1);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_COMPATIBILITY);
    SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
    SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);
    SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8);

    s_window = SDL_CreateWindow(
        title ? title : "Renegade",
        SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED,
        w > 0 ? w : 800, h > 0 ? h : 600,
        SDL_WINDOW_OPENGL | SDL_WINDOW_SHOWN
    );
    if (!s_window) {
        fprintf(stderr, "[SDL2] SDL_CreateWindow failed: %s\n", SDL_GetError());
        SDL_Quit();
        return -1;
    }

    s_glcontext = SDL_GL_CreateContext(s_window);
    if (!s_glcontext) {
        fprintf(stderr, "[SDL2] SDL_GL_CreateContext failed: %s\n", SDL_GetError());
        SDL_DestroyWindow(s_window);
        s_window = NULL;
        SDL_Quit();
        return -1;
    }

    SDL_GL_SetSwapInterval(1); // vsync

    // Bring the window to the foreground. When launched from the terminal,
    // macOS does not focus the app automatically.
    macos_activate_app();
    SDL_RaiseWindow(s_window);

    fprintf(stderr, "[SDL2] Window created %dx%d, GL context ready\n", w, h);
    GameInFocus = true;   // window starts with focus; DialogMgrClass::Render guards on this

    SDL_ShowCursor(SDL_DISABLE);
    // The invisible cursor rect is dispatched asynchronously; pump events now so
    // AppKit rebuilds cursor rects before the first frame — avoids a visible cursor
    // during the (potentially long) game initialization phase.
    SDL_PumpEvents();
    return 0;
}

void SDL2_Platform_Shutdown(void)
{
    if (s_glcontext) {
        SDL_GL_DeleteContext(s_glcontext);
        s_glcontext = NULL;
    }
    if (s_window) {
        SDL_DestroyWindow(s_window);
        s_window = NULL;
    }
    SDL_Quit();
}

SDL2_Window* SDL2_Platform_GetWindow(void)
{
    return (SDL2_Window*)s_window;
}

SDL2_GLContext SDL2_Platform_GetGLContext(void)
{
    return (SDL2_GLContext)s_glcontext;
}

void SDL2_Platform_SwapWindow(void)
{
    if (s_window) {
        SDL_GL_SwapWindow(s_window);
    }
}

void SDL2_Platform_PollEvents(void)
{
    SDL_Event event;
    while (SDL_PollEvent(&event)) {
        switch (event.type) {
        case SDL_QUIT:
            SDL2_QuitRequested = 1;
            break;

        case SDL_WINDOWEVENT:
            switch (event.window.event) {
            case SDL_WINDOWEVENT_FOCUS_GAINED:
                if (!SDL2_HasFocus) {
                    SDL2_HasFocus = 1;
                    GameInFocus = true;
                    SDL_ShowCursor(SDL_DISABLE);
                    update_should_render();
                    On_Focus_Restore();
                }
                break;
            case SDL_WINDOWEVENT_FOCUS_LOST:
                if (SDL2_HasFocus) {
                    SDL2_HasFocus = 0;
                    GameInFocus = false;
                    if (SDL2_MouseCaptured) {
                        SDL_SetRelativeMouseMode(SDL_FALSE);
                        SDL2_MouseCaptured = 0;
                    }
                    SDL_ShowCursor(SDL_ENABLE);
                    update_should_render();
                    On_Focus_Loss();
                }
                break;
            case SDL_WINDOWEVENT_MINIMIZED:
                s_minimized = 1;
                update_should_render();
                break;
            case SDL_WINDOWEVENT_RESTORED:
                s_minimized = 0;
                update_should_render();
                break;
            default:
                break;
            }
            break;

        case SDL_MOUSEWHEEL:
            SDL2_MouseWheelDelta += event.wheel.y;
            break;

        case SDL_MOUSEBUTTONDOWN:
            fprintf(stderr, "[SDL2] MOUSEBUTTONDOWN btn=%d pos=(%d,%d) captured=%d focus=%d\n",
                event.button.button, event.button.x, event.button.y,
                SDL2_MouseCaptured, SDL2_HasFocus);
            if (!SDL2_MouseCaptured && SDL2_HasFocus) {
                SDL2_CaptureClickX = event.button.x;
                SDL2_CaptureClickY = event.button.y;
                int rc = SDL_SetRelativeMouseMode(SDL_TRUE);
                if (rc == 0) {
                    SDL2_MouseCaptured = 1;
                } else {
                    fprintf(stderr, "[SDL2] SDL_SetRelativeMouseMode failed: %s\n", SDL_GetError());
                }
            }
            break;

        default:
            break;
        }
    }
}
