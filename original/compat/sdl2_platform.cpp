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
                    On_Focus_Restore();
                }
                break;
            case SDL_WINDOWEVENT_FOCUS_LOST:
                if (SDL2_HasFocus) {
                    SDL2_HasFocus = 0;
                    GameInFocus = false;
                    On_Focus_Loss();
                }
                break;
            default:
                break;
            }
            break;

        case SDL_MOUSEWHEEL:
            SDL2_MouseWheelDelta += event.wheel.y;
            break;

        default:
            break;
        }
    }
}
