// directinput.cpp — SDL2-backed input layer (replaces DirectInput8)
// Keeps the exact same public API as the original but uses SDL2 internally.
#define SDL_MAIN_HANDLED
#include <SDL2/SDL.h>

#include "directinput.h"
#include "win.h"
#include "timemgr.h"
#include "sdl2_platform.h"

// ---------------------------------------------------------------------------
// Static member data
// ---------------------------------------------------------------------------
char    DirectInput::DIKeyboardButtons[NUM_KEYBOARD_BUTTONS];
char    DirectInput::DIMouseButtons[NUM_MOUSE_BUTTONS];
long    DirectInput::DIMouseAxis[NUM_MOUSE_AXIS];
char    DirectInput::DIJoystickButtons[NUM_MOUSE_BUTTONS];
float   DirectInput::ButtonLastHitTime[NUM_KEYBOARD_BUTTONS];
Vector3 DirectInput::CursorPos(0, 0, 0);
bool    DirectInput::EatMouseHeld  = false;
bool    DirectInput::Captured      = false;
void*   DirectInput::DirectInputLibrary = NULL;
int     DirectInput::LastKeyPressed     = 0;

// Previous-frame keyboard state (for HIT/RELEASED transition detection)
static Uint8 s_prevKeyState[SDL_NUM_SCANCODES];

// ---------------------------------------------------------------------------
// SDL_SCANCODE → DIK translation table
// Index = SDL_Scancode value (0..511), value = DIK code (0 = unmapped).
// ---------------------------------------------------------------------------
static const unsigned char s_sdlToDik[SDL_NUM_SCANCODES] = {
    [SDL_SCANCODE_A]           = 0x1E,  // DIK_A
    [SDL_SCANCODE_B]           = 0x30,  // DIK_B
    [SDL_SCANCODE_C]           = 0x2E,  // DIK_C
    [SDL_SCANCODE_D]           = 0x20,  // DIK_D
    [SDL_SCANCODE_E]           = 0x12,  // DIK_E
    [SDL_SCANCODE_F]           = 0x21,  // DIK_F
    [SDL_SCANCODE_G]           = 0x22,  // DIK_G
    [SDL_SCANCODE_H]           = 0x23,  // DIK_H
    [SDL_SCANCODE_I]           = 0x17,  // DIK_I
    [SDL_SCANCODE_J]           = 0x24,  // DIK_J
    [SDL_SCANCODE_K]           = 0x25,  // DIK_K
    [SDL_SCANCODE_L]           = 0x26,  // DIK_L
    [SDL_SCANCODE_M]           = 0x32,  // DIK_M
    [SDL_SCANCODE_N]           = 0x31,  // DIK_N
    [SDL_SCANCODE_O]           = 0x18,  // DIK_O
    [SDL_SCANCODE_P]           = 0x19,  // DIK_P
    [SDL_SCANCODE_Q]           = 0x10,  // DIK_Q
    [SDL_SCANCODE_R]           = 0x13,  // DIK_R
    [SDL_SCANCODE_S]           = 0x1F,  // DIK_S
    [SDL_SCANCODE_T]           = 0x14,  // DIK_T
    [SDL_SCANCODE_U]           = 0x16,  // DIK_U
    [SDL_SCANCODE_V]           = 0x2F,  // DIK_V
    [SDL_SCANCODE_W]           = 0x11,  // DIK_W
    [SDL_SCANCODE_X]           = 0x2D,  // DIK_X
    [SDL_SCANCODE_Y]           = 0x15,  // DIK_Y
    [SDL_SCANCODE_Z]           = 0x2C,  // DIK_Z

    [SDL_SCANCODE_1]           = 0x02,
    [SDL_SCANCODE_2]           = 0x03,
    [SDL_SCANCODE_3]           = 0x04,
    [SDL_SCANCODE_4]           = 0x05,
    [SDL_SCANCODE_5]           = 0x06,
    [SDL_SCANCODE_6]           = 0x07,
    [SDL_SCANCODE_7]           = 0x08,
    [SDL_SCANCODE_8]           = 0x09,
    [SDL_SCANCODE_9]           = 0x0A,
    [SDL_SCANCODE_0]           = 0x0B,

    [SDL_SCANCODE_RETURN]      = 0x1C,  // DIK_RETURN
    [SDL_SCANCODE_ESCAPE]      = 0x01,  // DIK_ESCAPE
    [SDL_SCANCODE_BACKSPACE]   = 0x0E,  // DIK_BACK
    [SDL_SCANCODE_TAB]         = 0x0F,  // DIK_TAB
    [SDL_SCANCODE_SPACE]       = 0x39,  // DIK_SPACE
    [SDL_SCANCODE_MINUS]       = 0x0C,  // DIK_MINUS
    [SDL_SCANCODE_EQUALS]      = 0x0D,  // DIK_EQUALS
    [SDL_SCANCODE_LEFTBRACKET] = 0x1A,  // DIK_LBRACKET
    [SDL_SCANCODE_RIGHTBRACKET]= 0x1B,  // DIK_RBRACKET
    [SDL_SCANCODE_BACKSLASH]   = 0x2B,  // DIK_BACKSLASH
    [SDL_SCANCODE_SEMICOLON]   = 0x27,  // DIK_SEMICOLON
    [SDL_SCANCODE_APOSTROPHE]  = 0x28,  // DIK_APOSTROPHE
    [SDL_SCANCODE_GRAVE]       = 0x29,  // DIK_GRAVE
    [SDL_SCANCODE_COMMA]       = 0x33,  // DIK_COMMA
    [SDL_SCANCODE_PERIOD]      = 0x34,  // DIK_PERIOD
    [SDL_SCANCODE_SLASH]       = 0x35,  // DIK_SLASH

    [SDL_SCANCODE_CAPSLOCK]    = 0x3A,  // DIK_CAPITAL

    [SDL_SCANCODE_F1]          = 0x3B,
    [SDL_SCANCODE_F2]          = 0x3C,
    [SDL_SCANCODE_F3]          = 0x3D,
    [SDL_SCANCODE_F4]          = 0x3E,
    [SDL_SCANCODE_F5]          = 0x3F,
    [SDL_SCANCODE_F6]          = 0x40,
    [SDL_SCANCODE_F7]          = 0x41,
    [SDL_SCANCODE_F8]          = 0x42,
    [SDL_SCANCODE_F9]          = 0x43,
    [SDL_SCANCODE_F10]         = 0x44,
    [SDL_SCANCODE_F11]         = 0x57,
    [SDL_SCANCODE_F12]         = 0x58,

    [SDL_SCANCODE_PRINTSCREEN] = 0xB7,  // DIK_SYSRQ
    [SDL_SCANCODE_SCROLLLOCK]  = 0x46,  // DIK_SCROLL
    [SDL_SCANCODE_INSERT]      = 0xD2,  // DIK_INSERT
    [SDL_SCANCODE_HOME]        = 0xC7,  // DIK_HOME
    [SDL_SCANCODE_PAGEUP]      = 0xC9,  // DIK_PRIOR
    [SDL_SCANCODE_DELETE]      = 0xD3,  // DIK_DELETE
    [SDL_SCANCODE_END]         = 0xCF,  // DIK_END
    [SDL_SCANCODE_PAGEDOWN]    = 0xD1,  // DIK_NEXT
    [SDL_SCANCODE_RIGHT]       = 0xCD,  // DIK_RIGHT
    [SDL_SCANCODE_LEFT]        = 0xCB,  // DIK_LEFT
    [SDL_SCANCODE_DOWN]        = 0xD0,  // DIK_DOWN
    [SDL_SCANCODE_UP]          = 0xC8,  // DIK_UP

    [SDL_SCANCODE_NUMLOCKCLEAR]= 0x45,  // DIK_NUMLOCK
    [SDL_SCANCODE_KP_DIVIDE]   = 0xB5,  // DIK_DIVIDE
    [SDL_SCANCODE_KP_MULTIPLY] = 0x37,  // DIK_MULTIPLY
    [SDL_SCANCODE_KP_MINUS]    = 0x4A,  // DIK_SUBTRACT
    [SDL_SCANCODE_KP_PLUS]     = 0x4E,  // DIK_ADD
    [SDL_SCANCODE_KP_ENTER]    = 0x9C,  // DIK_NUMPADENTER
    [SDL_SCANCODE_KP_1]        = 0x4F,  // DIK_NUMPAD1
    [SDL_SCANCODE_KP_2]        = 0x50,
    [SDL_SCANCODE_KP_3]        = 0x51,
    [SDL_SCANCODE_KP_4]        = 0x4B,
    [SDL_SCANCODE_KP_5]        = 0x4C,
    [SDL_SCANCODE_KP_6]        = 0x4D,
    [SDL_SCANCODE_KP_7]        = 0x47,
    [SDL_SCANCODE_KP_8]        = 0x48,
    [SDL_SCANCODE_KP_9]        = 0x49,
    [SDL_SCANCODE_KP_0]        = 0x52,  // DIK_NUMPAD0
    [SDL_SCANCODE_KP_PERIOD]   = 0x53,  // DIK_DECIMAL

    [SDL_SCANCODE_LCTRL]       = 0x1D,  // DIK_LCONTROL
    [SDL_SCANCODE_LSHIFT]      = 0x2A,  // DIK_LSHIFT
    [SDL_SCANCODE_LALT]        = 0x38,  // DIK_LALT
    [SDL_SCANCODE_LGUI]        = 0xDB,  // DIK_LWIN
    [SDL_SCANCODE_RCTRL]       = 0x9D,  // DIK_RCONTROL
    [SDL_SCANCODE_RSHIFT]      = 0x36,  // DIK_RSHIFT
    [SDL_SCANCODE_RALT]        = 0xB8,  // DIK_RALT
    [SDL_SCANCODE_RGUI]        = 0xDC,  // DIK_RWIN
    [SDL_SCANCODE_APPLICATION] = 0xDD,  // DIK_APPS
};

// ---------------------------------------------------------------------------
// Init / Shutdown
// ---------------------------------------------------------------------------
void DirectInput::Init(void)
{

    memset(s_prevKeyState, 0, sizeof(s_prevKeyState));
    Flush();

    for (int i = 0; i < NUM_KEYBOARD_BUTTONS; i++) {
        ButtonLastHitTime[i] = 1000;
    }

    // SDL is already initialised by SDL2_Platform_Init (called from CreateWindowEx).
    // Enable relative mouse mode so we get relative deltas.
    SDL_SetRelativeMouseMode(SDL_TRUE);
    SDL_ShowCursor(SDL_DISABLE);

    Captured = true;
}

// ---------------------------------------------------------------------------
void DirectInput::Shutdown(void)
{
    SDL_SetRelativeMouseMode(SDL_FALSE);
    SDL_ShowCursor(SDL_ENABLE);
    Captured = false;
}

// ---------------------------------------------------------------------------
void DirectInput::Flush(void)
{
    memset(DIKeyboardButtons, 0, sizeof(DIKeyboardButtons));
    memset(DIMouseButtons,    0, sizeof(DIMouseButtons));
    memset(DIMouseAxis,       0, sizeof(DIMouseAxis));
    memset(DIJoystickButtons, 0, sizeof(DIJoystickButtons));
}

// ---------------------------------------------------------------------------
void DirectInput::Acquire(void)
{
    if (!Captured) {
        Flush();
        SDL_SetRelativeMouseMode(SDL_TRUE);
        SDL_ShowCursor(SDL_DISABLE);
        Captured = true;
    }
}

// ---------------------------------------------------------------------------
void DirectInput::Unacquire(void)
{
    if (Captured) {
        SDL_SetRelativeMouseMode(SDL_FALSE);
        SDL_ShowCursor(SDL_ENABLE);
        Captured = false;
    }
}

// ---------------------------------------------------------------------------
void DirectInput::ReadKeyboard(void)
{
    int numKeys = 0;
    const Uint8* cur = SDL_GetKeyboardState(&numKeys);
    if (!cur) return;

    int limit = numKeys < SDL_NUM_SCANCODES ? numKeys : SDL_NUM_SCANCODES;

    for (int sc = 0; sc < limit; sc++) {
        unsigned char dik = s_sdlToDik[sc];
        if (dik == 0) continue;

        bool wasDown = (s_prevKeyState[sc] != 0);
        bool isDown  = (cur[sc] != 0);

        // Preserve HELD bit, clear transition bits
        DIKeyboardButtons[dik] &= DI_BUTTON_HELD;

        if (isDown && !wasDown) {
            DIKeyboardButtons[dik] |= DI_BUTTON_HIT | DI_BUTTON_HELD;
            LastKeyPressed = dik;
        } else if (isDown) {
            DIKeyboardButtons[dik] |= DI_BUTTON_HELD;
        } else if (!isDown && wasDown) {
            DIKeyboardButtons[dik] |= DI_BUTTON_RELEASED;
            DIKeyboardButtons[dik] &= ~DI_BUTTON_HELD;
        }
    }

    // Save state for next frame
    if (limit > 0) {
        memcpy(s_prevKeyState, cur, limit);
    }

    // Synthetic combo keys
    DIKeyboardButtons[DIK_CONTROL] = DIKeyboardButtons[0x1D] | DIKeyboardButtons[0x9D]; // L+R ctrl
    DIKeyboardButtons[DIK_SHIFT]   = DIKeyboardButtons[0x2A] | DIKeyboardButtons[0x36]; // L+R shift
    DIKeyboardButtons[DIK_ALT]     = DIKeyboardButtons[0x38] | DIKeyboardButtons[0xB8]; // L+R alt
    DIKeyboardButtons[DIK_WIN]     = DIKeyboardButtons[0xDB] | DIKeyboardButtons[0xDC]; // L+R win
}

// ---------------------------------------------------------------------------
void DirectInput::ReadMouse(void)
{
    // Clear transition bits, keep HELD state
    for (int i = 0; i < NUM_MOUSE_BUTTONS; i++) {
        DIMouseButtons[i] &= DI_BUTTON_HELD;
    }
    for (int i = 0; i < NUM_MOUSE_AXIS; i++) {
        DIMouseAxis[i] = 0;
    }

    // Relative mouse deltas
    int dx = 0, dy = 0;
    Uint32 sdlButtons = SDL_GetRelativeMouseState(&dx, &dy);

    DIMouseAxis[MOUSE_X_AXIS] = dx;
    DIMouseAxis[MOUSE_Y_AXIS] = dy;

    // Mouse wheel from platform event accumulator
    DIMouseAxis[MOUSE_Z_AXIS] = SDL2_MouseWheelDelta * 120; // match DInput wheel scale
    SDL2_MouseWheelDelta = 0;

    // Cursor position tracking
    CursorPos.X += (float)(dx * 2);
    CursorPos.Y += (float)(dy * 2);

    // Button transitions (SDL uses mask bits: SDL_BUTTON_LMASK, RMASK, MMASK)
    static Uint32 prevButtons = 0;

    struct { Uint32 mask; int idx; } btns[] = {
        { SDL_BUTTON_LMASK, 0 },  // BUTTON_MOUSE_LEFT
        { SDL_BUTTON_RMASK, 1 },  // BUTTON_MOUSE_RIGHT
        { SDL_BUTTON_MMASK, 2 },  // BUTTON_MOUSE_CENTER
    };

    for (int i = 0; i < 3; i++) {
        bool wasDown = (prevButtons & btns[i].mask) != 0;
        bool isDown  = (sdlButtons  & btns[i].mask) != 0;

        if (isDown && !wasDown) {
            DIMouseButtons[btns[i].idx] |= DI_BUTTON_HIT | DI_BUTTON_HELD;
        } else if (isDown) {
            DIMouseButtons[btns[i].idx] |= DI_BUTTON_HELD;
        } else if (!isDown && wasDown) {
            DIMouseButtons[btns[i].idx] |= DI_BUTTON_RELEASED;
            DIMouseButtons[btns[i].idx] &= ~DI_BUTTON_HELD;
            EatMouseHeld = false;
        }
    }
    prevButtons = sdlButtons;

    // "Eat" left mouse button if requested
    if (EatMouseHeld) {
        DIMouseButtons[BUTTON_MOUSE_LEFT & 0xFF] &= ~(DI_BUTTON_HELD | DI_BUTTON_HIT);
        DIMouseButtons[BUTTON_MOUSE_LEFT & 0xFF] |= DI_BUTTON_RELEASED;
    }
}

// ---------------------------------------------------------------------------
void DirectInput::ReadJoystick(void)
{
    // Joystick not implemented in Phase A — zero out state
    memset(DIJoystickButtons, 0, sizeof(DIJoystickButtons));
}

// ---------------------------------------------------------------------------
void DirectInput::Read(void)
{
    if (Captured) {
        ReadKeyboard();
        ReadMouse();
        ReadJoystick();
        Update_Double_Clicks();
    }
}

// ---------------------------------------------------------------------------
void DirectInput::Eat_Mouse_Held_States(void)
{
    if ((DIMouseButtons[BUTTON_MOUSE_LEFT & 0xFF] & DI_BUTTON_HELD) ||
        (DIMouseButtons[BUTTON_MOUSE_LEFT & 0xFF] & DI_BUTTON_HIT))
    {
        EatMouseHeld = true;
    }
}

// ---------------------------------------------------------------------------
long DirectInput::Get_Joystick_Axis_State(JoystickAxis axis)
{
    return 0;
}

// ---------------------------------------------------------------------------
void DirectInput::Update_Double_Clicks(void)
{
    float time_delta = TimeManager::Get_Frame_Real_Seconds();
    for (int index = 0; index < NUM_KEYBOARD_BUTTONS; index++) {
        ButtonLastHitTime[index] += time_delta;

        if (DIKeyboardButtons[index] & DI_BUTTON_HIT) {
            if (ButtonLastHitTime[index] <= 0.25f) {
                DIKeyboardButtons[index] |= 8; // BUTTON_BIT_DOUBLE
            }
            ButtonLastHitTime[index] = 0;
        }
    }
}
