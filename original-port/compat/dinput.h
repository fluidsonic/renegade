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
#define DIK_RALT        0xB8
#define DIK_DELETE      0xD3
#define DIK_INSERT      0xD2
#define DIK_HOME        0xC7
#define DIK_END         0xCF
#define DIK_PRIOR       0xC9  // Page up
#define DIK_NEXT        0xD1  // Page down

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
    virtual HRESULT EnumDevices(DWORD type, void* cb, LPVOID ref, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT GetDeviceStatus(REFGUID g) { return E_NOTIMPL; }
    virtual HRESULT RunControlPanel(HWND hwnd, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT Initialize(HINSTANCE hinst, DWORD ver) { return E_NOTIMPL; }
    virtual HRESULT FindDevice(REFGUID cls, LPCSTR name, LPGUID pguid) { return E_NOTIMPL; }
};
typedef IDirectInput8A IDirectInput8;
typedef IDirectInput8A* LPDIRECTINPUT8;
typedef IDirectInput8A* LPDIRECTINPUT8A;

inline HRESULT DirectInput8Create(HINSTANCE hinst, DWORD ver, REFIID riid, LPVOID* ppvOut, IUnknown* pUnk) {
    if (ppvOut) *ppvOut = NULL; return E_NOTIMPL;
}

#endif // DINPUT_H_COMPAT
