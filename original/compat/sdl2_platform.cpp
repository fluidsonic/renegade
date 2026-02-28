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
    fprintf(stderr, "[SDL2] SDL2_Platform_Shutdown — begin\n");
    if (s_glcontext) {
        SDL_GL_DeleteContext(s_glcontext);
        s_glcontext = NULL;
    }
    if (s_window) {
        SDL_DestroyWindow(s_window);
        s_window = NULL;
    }
    SDL_Quit();
    fprintf(stderr, "[SDL2] SDL2_Platform_Shutdown — done\n");
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
        if (SDL2_ShouldRender) {
            SDL_GL_SwapWindow(s_window);
        } else {
            SDL_Delay(16); // ~60fps throttle when not rendering
        }
    }
}

// Map an SDL_Keycode to a Windows VK_ virtual-key code.
// Returns 0 for keys that have no meaningful VK mapping.
static uint32_t sdl_keycode_to_vk(SDL_Keycode sym)
{
    // ASCII control chars where SDL value == VK value
    if (sym == SDLK_BACKSPACE) return 0x08; // VK_BACK
    if (sym == SDLK_TAB)       return 0x09; // VK_TAB
    if (sym == SDLK_RETURN)    return 0x0D; // VK_RETURN
    if (sym == SDLK_ESCAPE)    return 0x1B; // VK_ESCAPE
    if (sym == SDLK_SPACE)     return 0x20; // VK_SPACE

    // Digits 0-9: SDL value == ASCII == VK value
    if (sym >= SDLK_0 && sym <= SDLK_9) return (uint32_t)sym;

    // Letters a-z: SDL value is lowercase ASCII; VK is uppercase ASCII
    if (sym >= SDLK_a && sym <= SDLK_z) return (uint32_t)(sym - 32);

    // Extended keys encoded as SDLK_SCANCODE_MASK | scancode
    switch (sym) {
        case SDLK_F1:       return 0x70; // VK_F1
        case SDLK_F2:       return 0x71;
        case SDLK_F3:       return 0x72;
        case SDLK_F4:       return 0x73;
        case SDLK_F5:       return 0x74;
        case SDLK_F6:       return 0x75;
        case SDLK_F7:       return 0x76;
        case SDLK_F8:       return 0x77;
        case SDLK_F9:       return 0x78;
        case SDLK_F10:      return 0x79;
        case SDLK_F11:      return 0x7A;
        case SDLK_F12:      return 0x7B;
        case SDLK_UP:       return 0x26; // VK_UP
        case SDLK_DOWN:     return 0x28; // VK_DOWN
        case SDLK_LEFT:     return 0x25; // VK_LEFT
        case SDLK_RIGHT:    return 0x27; // VK_RIGHT
        case SDLK_HOME:     return 0x24; // VK_HOME
        case SDLK_END:      return 0x23; // VK_END
        case SDLK_PAGEUP:   return 0x21; // VK_PRIOR
        case SDLK_PAGEDOWN: return 0x22; // VK_NEXT
        case SDLK_DELETE:   return 0x2E; // VK_DELETE
        case SDLK_LSHIFT:
        case SDLK_RSHIFT:   return 0x10; // VK_SHIFT
        case SDLK_LCTRL:
        case SDLK_RCTRL:    return 0x11; // VK_CONTROL
        case SDLK_LALT:
        case SDLK_RALT:     return 0x12; // VK_MENU
        default:            return 0;
    }
}

// Map a Windows VK_ virtual-key code to an SDL_Scancode.
// Returns SDL_SCANCODE_UNKNOWN for unmapped keys.
static SDL_Scancode vk_to_sdl_scancode(int32_t vk)
{
    switch (vk) {
        case 0x08: return SDL_SCANCODE_BACKSPACE;
        case 0x09: return SDL_SCANCODE_TAB;
        case 0x0D: return SDL_SCANCODE_RETURN;
        case 0x10: return SDL_SCANCODE_LSHIFT;    // VK_SHIFT — generic → left variant
        case 0x11: return SDL_SCANCODE_LCTRL;     // VK_CONTROL
        case 0x12: return SDL_SCANCODE_LALT;      // VK_MENU
        case 0x13: return SDL_SCANCODE_PAUSE;
        case 0x14: return SDL_SCANCODE_CAPSLOCK;
        case 0x1B: return SDL_SCANCODE_ESCAPE;
        case 0x20: return SDL_SCANCODE_SPACE;
        case 0x21: return SDL_SCANCODE_PAGEUP;    // VK_PRIOR
        case 0x22: return SDL_SCANCODE_PAGEDOWN;  // VK_NEXT
        case 0x23: return SDL_SCANCODE_END;
        case 0x24: return SDL_SCANCODE_HOME;
        case 0x25: return SDL_SCANCODE_LEFT;
        case 0x26: return SDL_SCANCODE_UP;
        case 0x27: return SDL_SCANCODE_RIGHT;
        case 0x28: return SDL_SCANCODE_DOWN;
        case 0x2D: return SDL_SCANCODE_INSERT;
        case 0x2E: return SDL_SCANCODE_DELETE;
        // '0'-'9'
        case 0x30: return SDL_SCANCODE_0;
        case 0x31: return SDL_SCANCODE_1;
        case 0x32: return SDL_SCANCODE_2;
        case 0x33: return SDL_SCANCODE_3;
        case 0x34: return SDL_SCANCODE_4;
        case 0x35: return SDL_SCANCODE_5;
        case 0x36: return SDL_SCANCODE_6;
        case 0x37: return SDL_SCANCODE_7;
        case 0x38: return SDL_SCANCODE_8;
        case 0x39: return SDL_SCANCODE_9;
        // 'A'-'Z'
        case 0x41: return SDL_SCANCODE_A;
        case 0x42: return SDL_SCANCODE_B;
        case 0x43: return SDL_SCANCODE_C;
        case 0x44: return SDL_SCANCODE_D;
        case 0x45: return SDL_SCANCODE_E;
        case 0x46: return SDL_SCANCODE_F;
        case 0x47: return SDL_SCANCODE_G;
        case 0x48: return SDL_SCANCODE_H;
        case 0x49: return SDL_SCANCODE_I;
        case 0x4A: return SDL_SCANCODE_J;
        case 0x4B: return SDL_SCANCODE_K;
        case 0x4C: return SDL_SCANCODE_L;
        case 0x4D: return SDL_SCANCODE_M;
        case 0x4E: return SDL_SCANCODE_N;
        case 0x4F: return SDL_SCANCODE_O;
        case 0x50: return SDL_SCANCODE_P;
        case 0x51: return SDL_SCANCODE_Q;
        case 0x52: return SDL_SCANCODE_R;
        case 0x53: return SDL_SCANCODE_S;
        case 0x54: return SDL_SCANCODE_T;
        case 0x55: return SDL_SCANCODE_U;
        case 0x56: return SDL_SCANCODE_V;
        case 0x57: return SDL_SCANCODE_W;
        case 0x58: return SDL_SCANCODE_X;
        case 0x59: return SDL_SCANCODE_Y;
        case 0x5A: return SDL_SCANCODE_Z;
        // VK_NUMPAD0-9
        case 0x60: return SDL_SCANCODE_KP_0;
        case 0x61: return SDL_SCANCODE_KP_1;
        case 0x62: return SDL_SCANCODE_KP_2;
        case 0x63: return SDL_SCANCODE_KP_3;
        case 0x64: return SDL_SCANCODE_KP_4;
        case 0x65: return SDL_SCANCODE_KP_5;
        case 0x66: return SDL_SCANCODE_KP_6;
        case 0x67: return SDL_SCANCODE_KP_7;
        case 0x68: return SDL_SCANCODE_KP_8;
        case 0x69: return SDL_SCANCODE_KP_9;
        case 0x6A: return SDL_SCANCODE_KP_MULTIPLY;
        case 0x6B: return SDL_SCANCODE_KP_PLUS;
        case 0x6D: return SDL_SCANCODE_KP_MINUS;
        case 0x6E: return SDL_SCANCODE_KP_PERIOD;
        case 0x6F: return SDL_SCANCODE_KP_DIVIDE;
        // VK_F1-F12
        case 0x70: return SDL_SCANCODE_F1;
        case 0x71: return SDL_SCANCODE_F2;
        case 0x72: return SDL_SCANCODE_F3;
        case 0x73: return SDL_SCANCODE_F4;
        case 0x74: return SDL_SCANCODE_F5;
        case 0x75: return SDL_SCANCODE_F6;
        case 0x76: return SDL_SCANCODE_F7;
        case 0x77: return SDL_SCANCODE_F8;
        case 0x78: return SDL_SCANCODE_F9;
        case 0x79: return SDL_SCANCODE_F10;
        case 0x7A: return SDL_SCANCODE_F11;
        case 0x7B: return SDL_SCANCODE_F12;
        case 0x90: return SDL_SCANCODE_NUMLOCKCLEAR; // VK_NUMLOCK
        case 0x91: return SDL_SCANCODE_SCROLLLOCK;   // VK_SCROLL
        case 0xA0: return SDL_SCANCODE_LSHIFT;       // VK_LSHIFT
        case 0xA1: return SDL_SCANCODE_RSHIFT;       // VK_RSHIFT
        case 0xA2: return SDL_SCANCODE_LCTRL;        // VK_LCONTROL
        case 0xA3: return SDL_SCANCODE_RCTRL;        // VK_RCONTROL
        case 0xA4: return SDL_SCANCODE_LALT;         // VK_LMENU
        case 0xA5: return SDL_SCANCODE_RALT;         // VK_RMENU
        default:   return SDL_SCANCODE_UNKNOWN;
    }
}

SHORT GetAsyncKeyState(int vKey)
{
    const Uint8* state = SDL_GetKeyboardState(NULL);
    bool pressed = false;
    if (vKey == 0x10 /*VK_SHIFT*/) {
        pressed = state[SDL_SCANCODE_LSHIFT] || state[SDL_SCANCODE_RSHIFT];
    } else if (vKey == 0x11 /*VK_CONTROL*/) {
        pressed = state[SDL_SCANCODE_LCTRL] || state[SDL_SCANCODE_RCTRL];
    } else if (vKey == 0x12 /*VK_MENU*/) {
        pressed = state[SDL_SCANCODE_LALT] || state[SDL_SCANCODE_RALT];
    } else {
        SDL_Scancode sc = vk_to_sdl_scancode(vKey);
        pressed = (sc != SDL_SCANCODE_UNKNOWN) && (state[sc] != 0);
    }
    return pressed ? (SHORT)0x8000 : 0;
}

SHORT GetKeyState(int nVirtKey)
{
    SHORT result = GetAsyncKeyState(nVirtKey);
    SDL_Keymod mod = SDL_GetModState();
    if (nVirtKey == 0x14 /*VK_CAPITAL*/ && (mod & KMOD_CAPS)) result |= 0x0001;
    if (nVirtKey == 0x90 /*VK_NUMLOCK*/ && (mod & KMOD_NUM))  result |= 0x0001;
    return result;
}

BOOL GetKeyboardState(PBYTE lpKeyState)
{
    const Uint8* state = SDL_GetKeyboardState(NULL);
    SDL_Keymod   mod   = SDL_GetModState();
    memset(lpKeyState, 0, 256);
    for (int32_t vk = 0; vk < 256; vk++) {
        SDL_Scancode sc = vk_to_sdl_scancode(vk);
        if (sc != SDL_SCANCODE_UNKNOWN && state[sc]) {
            lpKeyState[vk] |= 0x80;
        }
    }
    // Generic modifier keys: OR both L+R variants
    if (state[SDL_SCANCODE_LSHIFT] || state[SDL_SCANCODE_RSHIFT])   lpKeyState[0x10] |= 0x80; // VK_SHIFT
    if (state[SDL_SCANCODE_LCTRL]  || state[SDL_SCANCODE_RCTRL])    lpKeyState[0x11] |= 0x80; // VK_CONTROL
    if (state[SDL_SCANCODE_LALT]   || state[SDL_SCANCODE_RALT])     lpKeyState[0x12] |= 0x80; // VK_MENU
    // Toggle bits
    if (mod & KMOD_CAPS) lpKeyState[0x14] |= 0x01; // VK_CAPITAL
    if (mod & KMOD_NUM)  lpKeyState[0x90] |= 0x01; // VK_NUMLOCK
    return TRUE;
}

// Decode the first UTF-8 codepoint in s to a UTF-16 BMP value.
// Returns 0 on failure or if the codepoint is outside the BMP.
static uint32_t utf8_first_codepoint(const char* s)
{
    uint8_t c0 = (uint8_t)s[0];
    if (c0 == 0)              return 0;
    if (c0 < 0x80)            return c0;
    if ((c0 & 0xE0) == 0xC0) {
        uint8_t c1 = (uint8_t)s[1];
        if (!c1) return 0;
        return ((uint32_t)(c0 & 0x1F) << 6) | (c1 & 0x3F);
    }
    if ((c0 & 0xF0) == 0xE0) {
        uint8_t c1 = (uint8_t)s[1];
        uint8_t c2 = (uint8_t)s[2];
        if (!c1 || !c2) return 0;
        return ((uint32_t)(c0 & 0x0F) << 12) | ((uint32_t)(c1 & 0x3F) << 6) | (c2 & 0x3F);
    }
    return 0; // 4-byte sequences are outside the BMP; ignore
}

void SDL2_Platform_PollEvents(void)
{
    SDL_Event event;
    while (SDL_PollEvent(&event)) {
        switch (event.type) {
        case SDL_QUIT:
            fprintf(stderr, "[SDL2] SDL_QUIT received — requesting shutdown\n");
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

        case SDL_KEYDOWN: {
            uint32_t vk = sdl_keycode_to_vk(event.key.keysym.sym);
            if (vk != 0) {
                On_Key_Down(vk);
                SDL_PumpEvents();  // refresh keyboard state after potentially long KEYDOWN handlers
            }
            break;
        }

        case SDL_KEYUP: {
            uint32_t vk = sdl_keycode_to_vk(event.key.keysym.sym);
            if (vk != 0) {
                On_Key_Up(vk);
            }
            break;
        }

        case SDL_TEXTINPUT: {
            uint32_t cp = utf8_first_codepoint(event.text.text);
            if (cp > 0) {
                On_Char(cp);
            }
            break;
        }

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
