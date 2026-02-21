// dinput.h compat shim for macOS - DirectInput stubs
#pragma once
#ifndef DINPUT_H_COMPAT
#define DINPUT_H_COMPAT

#include "windef.h"
#include "d3d8types.h"

#define DIRECTINPUT_VERSION 0x0800

#define DISCL_EXCLUSIVE     0x00000001
#define DISCL_NONEXCLUSIVE  0x00000002
#define DISCL_FOREGROUND    0x00000004
#define DISCL_BACKGROUND    0x00000008
#define DISCL_NOWINKEY      0x00000010

#define DIDFT_BUTTON    0x0000000C
#define DIDFT_AXIS      0x00000003
#define DIDFT_POV       0x00000010
#define DIDFT_ANYINSTANCE 0x00FFFF00
#define DIDFT_OPTIONAL  0x80000000

// Key codes (DirectInput scan codes)
#define DIK_ESCAPE      0x01
#define DIK_1           0x02
#define DIK_2           0x03
#define DIK_3           0x04
#define DIK_4           0x05
#define DIK_5           0x06
#define DIK_6           0x07
#define DIK_7           0x08
#define DIK_8           0x09
#define DIK_9           0x0A
#define DIK_0           0x0B
#define DIK_MINUS       0x0C
#define DIK_EQUALS      0x0D
#define DIK_BACK        0x0E
#define DIK_TAB         0x0F
#define DIK_Q           0x10
#define DIK_W           0x11
#define DIK_E           0x12
#define DIK_R           0x13
#define DIK_T           0x14
#define DIK_Y           0x15
#define DIK_U           0x16
#define DIK_I           0x17
#define DIK_O           0x18
#define DIK_P           0x19
#define DIK_LBRACKET    0x1A
#define DIK_RBRACKET    0x1B
#define DIK_RETURN      0x1C
#define DIK_LCONTROL    0x1D
#define DIK_A           0x1E
#define DIK_S           0x1F
#define DIK_D           0x20
#define DIK_F           0x21
#define DIK_G           0x22
#define DIK_H           0x23
#define DIK_J           0x24
#define DIK_K           0x25
#define DIK_L           0x26
#define DIK_SEMICOLON   0x27
#define DIK_LSHIFT      0x2A
#define DIK_Z           0x2C
#define DIK_X           0x2D
#define DIK_C           0x2E
#define DIK_V           0x2F
#define DIK_B           0x30
#define DIK_N           0x31
#define DIK_M           0x32
#define DIK_RSHIFT      0x36
#define DIK_SPACE       0x39
#define DIK_F1          0x3B
#define DIK_F2          0x3C
#define DIK_F3          0x3D
#define DIK_F4          0x3E
#define DIK_F5          0x3F
#define DIK_F6          0x40
#define DIK_F7          0x41
#define DIK_F8          0x42
#define DIK_F9          0x43
#define DIK_F10         0x44
#define DIK_F11         0x57
#define DIK_F12         0x58
#define DIK_UP          0xC8
#define DIK_DOWN        0xD0
#define DIK_LEFT        0xCB
#define DIK_RIGHT       0xCD
#define DIK_LALT        0x38
#define DIK_LMENU       0x38  // Left Alt (alias)
#define DIK_RMENU       0xB8  // Right Alt (alias)
#define DIK_RALT        0xB8
#define DIK_DELETE      0xD3
#define DIK_INSERT      0xD2
#define DIK_HOME        0xC7
#define DIK_END         0xCF
#define DIK_PRIOR       0xC9  // Page up
#define DIK_NEXT        0xD1  // Page down
#define DIK_APOSTROPHE  0x28
#define DIK_GRAVE       0x29  // backtick `
#define DIK_BACKSLASH   0x2B
#define DIK_COMMA       0x33
#define DIK_PERIOD      0x34
#define DIK_SLASH       0x35
#define DIK_CAPITAL     0x3A  // Caps Lock
#define DIK_NUMLOCK     0x45
#define DIK_SCROLL      0x46  // Scroll Lock
#define DIK_NUMPAD7     0x47
#define DIK_NUMPAD8     0x48
#define DIK_NUMPAD9     0x49
#define DIK_SUBTRACT    0x4A  // Numpad -
#define DIK_NUMPAD4     0x4B
#define DIK_NUMPAD5     0x4C
#define DIK_NUMPAD6     0x4D
#define DIK_ADD         0x4E  // Numpad +
#define DIK_NUMPAD1     0x4F
#define DIK_NUMPAD2     0x50
#define DIK_NUMPAD3     0x51
#define DIK_NUMPAD0     0x52
#define DIK_DECIMAL     0x53  // Numpad .
#define DIK_MULTIPLY    0x37  // Numpad *
#define DIK_NUMPADENTER 0x9C
#define DIK_RCONTROL    0x9D
#define DIK_DIVIDE      0xB5  // Numpad /
#define DIK_SYSRQ       0xB7  // Print Screen / SysRq
#define DIK_LWIN        0xDB
#define DIK_RWIN        0xDC
#define DIK_APPS        0xDD  // Application / Menu key
// Note: DIK_SHIFT/CONTROL/ALT/WIN are game-defined virtual keys in Combat/directinput.h (DupeKeys enum)

typedef struct _DIOBJECTDATAFORMAT {
    const GUID* pguid;
    DWORD       dwOfs;
    DWORD       dwType;
    DWORD       dwFlags;
} DIOBJECTDATAFORMAT, *LPDIOBJECTDATAFORMAT;

typedef struct _DIDATAFORMAT {
    DWORD                dwSize, dwObjSize, dwFlags, dwDataSize, dwNumObjs;
    LPDIOBJECTDATAFORMAT rgodf;
} DIDATAFORMAT, *LPDIDATAFORMAT;

typedef struct _DIMOUSESTATE  { LONG lX, lY, lZ; BYTE rgbButtons[4];  } DIMOUSESTATE;
typedef struct _DIMOUSESTATE2 { LONG lX, lY, lZ; BYTE rgbButtons[8];  } DIMOUSESTATE2;

typedef struct _DIJOYSTATE {
    LONG lX, lY, lZ, lRx, lRy, lRz;
    LONG rglSlider[2];
    DWORD rgdwPOV[4];
    BYTE rgbButtons[32];
} DIJOYSTATE;

#define DIJOFS_BUTTON(n)    (offsetof(DIJOYSTATE,rgbButtons)+(n))
#define DIJOFS_X            offsetof(DIJOYSTATE,lX)
#define DIJOFS_Y            offsetof(DIJOYSTATE,lY)
#define DIJOFS_Z            offsetof(DIJOYSTATE,lZ)

// Mouse state offsets (into DIMOUSESTATE)
#define DIMOFS_X        offsetof(DIMOUSESTATE,lX)
#define DIMOFS_Y        offsetof(DIMOUSESTATE,lY)
#define DIMOFS_Z        offsetof(DIMOUSESTATE,lZ)
#define DIMOFS_BUTTON0  (offsetof(DIMOUSESTATE,rgbButtons)+0)
#define DIMOFS_BUTTON1  (offsetof(DIMOUSESTATE,rgbButtons)+1)
#define DIMOFS_BUTTON2  (offsetof(DIMOUSESTATE,rgbButtons)+2)
#define DIMOFS_BUTTON3  (offsetof(DIMOUSESTATE,rgbButtons)+3)

typedef struct {
    DWORD dwSize;
    GUID  guidType;
    DWORD dwOfs, dwType, dwFlags;
    char  tszName[260];
    DWORD dwFFMaxForce, dwFFForceResolution;
    WORD  wCollectionNumber, wDesignatorIndex, wUsagePage, wUsage;
    DWORD dwDimension;
    WORD  wExponent, wReportId;
} DIDEVICEOBJECTINSTANCEA, *LPDIDEVICEOBJECTINSTANCEA;
typedef DIDEVICEOBJECTINSTANCEA DIDEVICEOBJECTINSTANCE;

// Buffered input data element
typedef struct _DIDEVICEOBJECTDATA {
    DWORD dwOfs;
    DWORD dwData;
    DWORD dwTimeStamp;
    DWORD dwSequence;
} DIDEVICEOBJECTDATA, *LPDIDEVICEOBJECTDATA;

// Device instance info (forward declared here so callback typedef works)
struct _DIDEVICEINSTANCEA;
typedef const _DIDEVICEINSTANCEA* LPCDIDEVICEINSTANCEA_FWD;

// Enum callback types
typedef int (PASCAL *LPDIENUMDEVICESCALLBACKA)(const _DIDEVICEINSTANCEA*, LPVOID);

struct IDirectInputDevice8A : public IUnknown {
    virtual HRESULT GetCapabilities(void* p) { return E_NOTIMPL; }
    virtual HRESULT EnumObjects(void* cb, LPVOID ref, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT GetProperty(REFGUID g, void* p) { return E_NOTIMPL; }
    virtual HRESULT SetProperty(REFGUID g, const void* p) { return E_NOTIMPL; }
    virtual HRESULT Acquire() { return E_NOTIMPL; }
    virtual HRESULT Unacquire() { return E_NOTIMPL; }
    virtual HRESULT GetDeviceState(DWORD cb, LPVOID data) { return E_NOTIMPL; }
    virtual HRESULT GetDeviceData(DWORD cbObj, void* rgdod, LPDWORD pdw, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT SetDataFormat(const DIDATAFORMAT* lpdf) { return E_NOTIMPL; }
    virtual HRESULT SetEventNotification(HANDLE h) { return E_NOTIMPL; }
    virtual HRESULT SetCooperativeLevel(HWND hwnd, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT GetObjectInfo(LPDIDEVICEOBJECTINSTANCEA p, DWORD obj, DWORD how) { return E_NOTIMPL; }
    virtual HRESULT GetDeviceInfo(void* p) { return E_NOTIMPL; }
    virtual HRESULT RunControlPanel(HWND hwnd, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT Initialize(HINSTANCE hinst, DWORD ver, REFGUID guid) { return E_NOTIMPL; }
    virtual HRESULT CreateEffect(REFGUID g, const void* eff, void** ppdeff, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT EnumEffects(void* cb, LPVOID ref, DWORD type) { return E_NOTIMPL; }
    virtual HRESULT GetEffectInfo(void* p, REFGUID g) { return E_NOTIMPL; }
    virtual HRESULT GetForceFeedbackState(LPDWORD pdw) { return E_NOTIMPL; }
    virtual HRESULT SendForceFeedbackCommand(DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT EnumCreatedEffectObjects(void* cb, LPVOID ref, DWORD fl) { return E_NOTIMPL; }
    virtual HRESULT Escape(void* p) { return E_NOTIMPL; }
    virtual HRESULT Poll() { return E_NOTIMPL; }
    virtual HRESULT SendDeviceData(DWORD cb, const void* d, LPDWORD pdw, DWORD fl) { return E_NOTIMPL; }
};
typedef IDirectInputDevice8A IDirectInputDevice8;
typedef IDirectInputDevice8A* LPDIRECTINPUTDEVICE8;
typedef IDirectInputDevice8A* LPDIRECTINPUTDEVICE8A;

struct IDirectInput8A : public IUnknown {
    virtual HRESULT CreateDevice(REFGUID g, IDirectInputDevice8A** ppdev, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT EnumDevices(DWORD type, LPDIENUMDEVICESCALLBACKA cb, LPVOID ref, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT GetDeviceStatus(REFGUID g) { return E_NOTIMPL; }
    virtual HRESULT RunControlPanel(HWND hwnd, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT Initialize(HINSTANCE hinst, DWORD ver) { return E_NOTIMPL; }
    virtual HRESULT FindDevice(REFGUID cls, LPCSTR name, LPGUID pguid) { return E_NOTIMPL; }
};
typedef IDirectInput8A IDirectInput8;
typedef IDirectInput8A* LPDIRECTINPUT8;
typedef IDirectInput8A* LPDIRECTINPUT8A;

// v1/v2 aliases (this code uses DInput8 but refers to older type names)
typedef IDirectInput8A* LPDIRECTINPUT;
typedef IDirectInputDevice8A* LPDIRECTINPUTDEVICE;
typedef IDirectInputDevice8A* LPDIRECTINPUTDEVICE2;
typedef IUnknown* LPUNKNOWN;

inline HRESULT DirectInput8Create(HINSTANCE hinst, DWORD ver, REFIID riid, LPVOID* ppvOut, IUnknown* pUnk) {
    if (ppvOut) *ppvOut = NULL; return E_NOTIMPL;
}

// Device instance info (full definition of forward-declared struct)
typedef struct _DIDEVICEINSTANCEA {
    DWORD dwSize;
    GUID  guidInstance;
    GUID  guidProduct;
    DWORD dwDevType;
    CHAR  tszInstanceName[260];
    CHAR  tszProductName[260];
    GUID  guidFFDriver;
    WORD  wUsagePage;
    WORD  wUsage;
} DIDEVICEINSTANCEA, *LPDIDEVICEINSTANCEA;
typedef const DIDEVICEINSTANCEA* LPCDIDEVICEINSTANCEA;
typedef DIDEVICEINSTANCEA DIDEVICEINSTANCE;
typedef LPCDIDEVICEINSTANCEA LPCDIDEVICEINSTANCE;

// Property header and DIPROPDWORD
typedef struct _DIPROPHEADER {
    DWORD dwSize;
    DWORD dwHeaderSize;
    DWORD dwObj;
    DWORD dwHow;
} DIPROPHEADER, *LPDIPROPHEADER;
typedef const DIPROPHEADER* LPCDIPROPHEADER;

typedef struct _DIPROPDWORD {
    DIPROPHEADER diph;
    DWORD        dwData;
} DIPROPDWORD, *LPDIPROPDWORD;

// Property constants
#define DIPH_DEVICE      0
#define DIPH_BYOFFSET    1
#define DIPH_BYID        2

// Property GUIDs — REFGUID is const GUID& in C++
static const GUID _DIPROP_BUFFERSIZE = {0x1};
static const GUID _DIPROP_RANGE      = {0x4};
static const GUID _DIPROP_DEADZONE   = {0x5};
#define DIPROP_BUFFERSIZE _DIPROP_BUFFERSIZE
#define DIPROP_RANGE      _DIPROP_RANGE
#define DIPROP_DEADZONE   _DIPROP_DEADZONE

// Property range struct (for axis min/max)
typedef struct _DIPROPRANGE {
    DIPROPHEADER diph;
    LONG         lMin;
    LONG         lMax;
} DIPROPRANGE, *LPDIPROPRANGE;

// DInput GUIDs
static const GUID GUID_SysKeyboard  = {0x6F1D2B61, 0xD5A0, 0x11CF, {0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00}};
static const GUID GUID_SysMouse     = {0x6F1D2B60, 0xD5A0, 0x11CF, {0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00}};
static const GUID IID_IDirectInput8 = {0xBF798031, 0x483A, 0x4DA2, {0xAA,0x99,0x5D,0x64,0xED,0x36,0x97,0x00}};
static const GUID IID_IDirectInputDevice2 = {0x5944E682, 0xC92E, 0x11CF, {0xBF,0xC7,0x44,0x45,0x53,0x54,0x00,0x00}};

// Data format globals (stub instances — const needed since REFGUID is const&)
static const DIDATAFORMAT c_dfDIKeyboard  = {sizeof(DIDATAFORMAT), sizeof(DIOBJECTDATAFORMAT), 0x2, 256, 0, NULL};
static const DIDATAFORMAT c_dfDIMouse     = {sizeof(DIDATAFORMAT), sizeof(DIOBJECTDATAFORMAT), 0x2, sizeof(DIMOUSESTATE), 0, NULL};
static const DIDATAFORMAT c_dfDIJoystick  = {sizeof(DIDATAFORMAT), sizeof(DIOBJECTDATAFORMAT), 0x1, sizeof(DIJOYSTATE), 0, NULL};

// Enumeration constants
#define DI8DEVCLASS_GAMECTRL  4
#define DIEDFL_ATTACHEDONLY   0x00000001
#define DIENUM_STOP           0
#define DIENUM_CONTINUE       1

// IDirectInputDevice_Release macro (C interface compat)
#define IDirectInputDevice_Release(p)  ((p)->Release())

// Error codes
#define DIERR_INVALIDPARAM      0x80070057L
#define DIERR_NOTINITIALIZED    0x80040001L
#define DIERR_OTHERAPPHASPRIO   0x8007000AL
#define DIERR_ACQUIRED          0x8007001EL
#define DIERR_NOTACQUIRED       0x8007001FL
#define DIERR_INPUTLOST         0x8007001FL

#endif // DINPUT_H_COMPAT
