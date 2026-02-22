#pragma once

#include "global.h"

// ddraw.h compat shim for macOS

#include "wingdi.h"
#include "d3d8types.h"

// Forward declarations
struct IDirectDrawClipper;
struct IDirectDrawPalette;

typedef struct _DDCOLORKEY {
    DWORD dwColorSpaceLowValue;
    DWORD dwColorSpaceHighValue;
} DDCOLORKEY, *LPDDCOLORKEY;

typedef struct _DDPIXELFORMAT {
    DWORD dwSize, dwFlags, dwFourCC;
    union { DWORD dwRGBBitCount; DWORD dwYUVBitCount; DWORD dwZBufferBitDepth; DWORD dwAlphaBitDepth; };
    union { DWORD dwRBitMask;    DWORD dwYBitMask;    };
    union { DWORD dwGBitMask;    DWORD dwUBitMask;    };
    union { DWORD dwBBitMask;    DWORD dwVBitMask;    };
    union { DWORD dwRGBAlphaBitMask; DWORD dwYUVAlphaBitMask; DWORD dwRGBZBitMask; DWORD dwYUVZBitMask; };
} DDPIXELFORMAT, *LPDDPIXELFORMAT;

typedef struct _DDSCAPS {
    DWORD dwCaps;
} DDSCAPS, *LPDDSCAPS;

typedef struct _DDSCAPS2 {
    DWORD dwCaps, dwCaps2, dwCaps3, dwCaps4;
} DDSCAPS2, *LPDDSCAPS2;

typedef struct _DDSURFACEDESC2 {
    DWORD    dwSize, dwFlags, dwHeight, dwWidth;
    union { LONG lPitch; DWORD dwLinearSize; };
    DWORD    dwBackBufferCount;
    union { DWORD dwMipMapCount; DWORD dwZBufferBitDepth; DWORD dwRefreshRate; };
    DWORD    dwAlphaBitDepth, dwReserved;
    LPVOID   lpSurface;
    DDPIXELFORMAT ddpfPixelFormat;
    DDSCAPS2 ddsCaps;
    DWORD    dwTextureStage;
} DDSURFACEDESC2, *LPDDSURFACEDESC2;

// DDSURFACEDESC (v1) - alias to v2
typedef DDSURFACEDESC2 DDSURFACEDESC;
typedef DDSURFACEDESC2* LPDDSURFACEDESC;

// DDCAPS (hardware capabilities)
typedef struct _DDCAPS {
    DWORD dwSize;
    DWORD dwCaps, dwCaps2;
    DWORD dwVidMemTotal, dwVidMemFree;
    // (many more fields omitted for stub)
    DWORD _pad[64];
} DDCAPS, *LPDDCAPS;

// DDBLTFX
typedef struct _DDBLTFX {
    DWORD dwSize;
    DWORD dwDDFX;
    DWORD dwROP;
    DWORD dwDDROP;
    DWORD dwRotationAngle;
    DWORD dwZBufferOpCode;
    DWORD dwZBufferLow, dwZBufferHigh;
    DWORD dwZBufferBaseDest;
    DWORD dwZDestConstBitDepth;
    union { DWORD dwZDestConst; void* lpDDSZBufferDest; };
    DWORD dwZSrcConstBitDepth;
    union { DWORD dwZSrcConst;  void* lpDDSZBufferSrc; };
    DWORD dwAlphaEdgeBlendBitDepth;
    DWORD dwAlphaEdgeBlend;
    DWORD dwReserved;
    DWORD dwAlphaDestConstBitDepth;
    union { DWORD dwAlphaDestConst; void* lpDDSAlphaDest; };
    DWORD dwAlphaSrcConstBitDepth;
    union { DWORD dwAlphaSrcConst;  void* lpDDSAlphaSrc; };
    union { DWORD dwFillColor; DWORD dwFillDepth; DWORD dwFillPixel; void* lpDDSPattern; };
    DDCOLORKEY ddckDestColorkey;
    DDCOLORKEY ddckSrcColorkey;
} DDBLTFX, *LPDDBLTFX;

// DDSCAPS flags
#define DDSCAPS_PRIMARYSURFACE   0x00000200L
#define DDSCAPS_BACKBUFFER       0x00000004L
#define DDSCAPS_3DDEVICE         0x00002000L
#define DDSCAPS_TEXTURE          0x00001000L
#define DDSCAPS_FLIP             0x00000010L
#define DDSCAPS_COMPLEX          0x00000008L
#define DDSCAPS_OFFSCREENPLAIN   0x00000040L
#define DDSCAPS_VIDEOMEMORY      0x00004000L
#define DDSCAPS_SYSTEMMEMORY     0x00000800L
#define DDSCAPS_ZBUFFER          0x00000400L
#define DDSCAPS2_CUBEMAP         0x00000200L
#define DDSCAPS2_CUBEMAP_ALLFACES 0x0000FC00L

// DDSD flags
#define DDSD_CAPS                0x00000001L
#define DDSD_HEIGHT              0x00000002L
#define DDSD_WIDTH               0x00000004L
#define DDSD_PITCH               0x00000008L
#define DDSD_PIXELFORMAT         0x00001000L
#define DDSD_BACKBUFFERCOUNT     0x00000020L
#define DDSD_MIPMAPCOUNT         0x00020000L
#define DDSD_LINEARSIZE          0x00080000L
#define DDSD_ALL                 0x003F8FE3L

// DDPF flags
#define DDPF_ALPHAPIXELS         0x00000001L
#define DDPF_ALPHA               0x00000002L
#define DDPF_FOURCC              0x00000004L
#define DDPF_RGB                 0x00000040L
#define DDPF_PALETTEINDEXED8     0x00000020L
#define DDPF_LUMINANCE           0x00020000L
#define DDPF_BUMPDUDV            0x00080000L
#define DDPF_ZBUFFER             0x00000400L

// DDSCL (SetCooperativeLevel flags)
#define DDSCL_NORMAL             0x00000008L
#define DDSCL_EXCLUSIVE          0x00000010L
#define DDSCL_FULLSCREEN         0x00000020L
#define DDSCL_ALLOWREBOOT        0x00000040L
#define DDSCL_NOWINDOWCHANGES    0x00000400L
// Aliases
#define DDCL_NORMAL    DDSCL_NORMAL
#define DDCL_EXCLUSIVE DDSCL_EXCLUSIVE
#define DDCL_FULLSCREEN DDSCL_FULLSCREEN

// DDLOCK flags
#define DDLOCK_WAIT              0x00000001L
#define DDLOCK_READONLY          0x00000010L
#define DDLOCK_WRITEONLY         0x00000020L
#define DDLOCK_NOSYSLOCK         0x00000800L
#define DDLOCK_SURFACEMEMORYPTR  0x00000000L  // default, no flag needed

// DDBLT flags
#define DDBLT_WAIT               0x01000000L
#define DDBLT_COLORFILL          0x00000400L
#define DDBLT_ASYNC              0x00000200L
#define DDBLT_DDFX               0x00000800L

// Palette caps
#define DDPCAPS_8BIT             0x00000002L
#define DDPCAPS_ALLOW256         0x00000010L
#define DDPCAPS_INITIALIZE       0x00000008L

// Return codes
#define DD_OK                    0
#define DDERR_GENERIC            0x80004005L
#define DDERR_SURFACELOST        0x887601C2L
#define DDERR_INVALIDPARAMS      0x80070057L
#define DDERR_OUTOFMEMORY        0x8007000EL
#define DDERR_UNSUPPORTED        0x80004001L

struct IDirectDrawSurface7 : public IUnknown {
    virtual HRESULT GetSurfaceDesc(LPDDSURFACEDESC2 lpDesc) { return E_NOTIMPL; }
    virtual HRESULT Lock(LPRECT lpRect, LPDDSURFACEDESC2 lpDesc, DWORD flags, HANDLE hEvent) { return E_NOTIMPL; }
    virtual HRESULT Unlock(LPVOID lpSurfaceData) { return E_NOTIMPL; }
    virtual HRESULT Blt(LPRECT lpDest, IDirectDrawSurface7* lpSrc, LPRECT lpSrc2, DWORD flags, LPDDBLTFX lpDDBltFx) { return E_NOTIMPL; }
    virtual HRESULT GetPixelFormat(LPDDPIXELFORMAT lpFmt) { return E_NOTIMPL; }
    virtual HRESULT GetDC(HDC* lphDC) { return E_NOTIMPL; }
    virtual HRESULT ReleaseDC(HDC hDC) { return E_NOTIMPL; }
    virtual HRESULT SetClipper(IDirectDrawClipper* lpClipper) { return E_NOTIMPL; }
    virtual HRESULT GetAttachedSurface(LPDDSCAPS lpDDSCaps, IDirectDrawSurface7** lplpDDAttachedSurface) { return E_NOTIMPL; }
    virtual HRESULT IsLost() { return DDERR_SURFACELOST; }
    virtual HRESULT Restore() { return E_NOTIMPL; }
    virtual HRESULT SetColorKey(DWORD dwFlags, DDCOLORKEY* lpDDColorKey) { return E_NOTIMPL; }
    virtual HRESULT BltFast(DWORD x, DWORD y, IDirectDrawSurface7* lpSrc, LPRECT lpSrcRect, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT SetPalette(IDirectDrawPalette* lpDDPalette) { return E_NOTIMPL; }
    virtual HRESULT GetBltStatus(DWORD dwFlags) { return E_NOTIMPL; }
    virtual HRESULT Flip(IDirectDrawSurface7* lpDDSurfaceTargetOverride, DWORD dwFlags) { return E_NOTIMPL; }
    virtual HRESULT AddAttachedSurface(IDirectDrawSurface7* lpDDSAttachedSurface) { return E_NOTIMPL; }
};
typedef IDirectDrawSurface7* LPDIRECTDRAWSURFACE7;

struct IDirectDraw7 : public IUnknown {
    virtual HRESULT CreateSurface(LPDDSURFACEDESC2 lpDesc, IDirectDrawSurface7** lplpSurf, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT GetDisplayMode(LPDDSURFACEDESC2 lpDesc) { return E_NOTIMPL; }
    virtual HRESULT SetDisplayMode(DWORD w, DWORD h, DWORD bpp, DWORD refresh = 0, DWORD flags = 0) { return E_NOTIMPL; }
    virtual HRESULT SetCooperativeLevel(HWND hwnd, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT RestoreDisplayMode() { return E_NOTIMPL; }
    virtual HRESULT CreatePalette(DWORD flags, PALETTEENTRY* lpEntries, IDirectDrawPalette** lplpDDPalette, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT GetCaps(LPDDCAPS lpDDDriverCaps, LPDDCAPS lpDDHELCaps) { return E_NOTIMPL; }
    virtual HRESULT CreateClipper(DWORD flags, IDirectDrawClipper** lplpDDClipper, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT WaitForVerticalBlank(DWORD flags, HANDLE hEvent) { return E_NOTIMPL; }
    virtual HRESULT EnumDisplayModes(DWORD flags, LPDDSURFACEDESC2 lpSurfDesc, LPVOID lpContext, void* lpEnumModesCallback) { return E_NOTIMPL; }
};
typedef IDirectDraw7* LPDIRECTDRAW7;

// IDirectDrawClipper stub (forward decl needed for IDirectDrawSurface7::SetClipper above)
struct IDirectDrawClipper : public IUnknown {
    virtual HRESULT GetClipList(LPRECT lpRect, void* lpClipList, LPDWORD lpdwSize) { return E_NOTIMPL; }
    virtual HRESULT SetClipList(void* lpClipList, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT SetHWnd(DWORD flags, HWND hwnd) { return E_NOTIMPL; }
};
typedef IDirectDrawClipper* LPDIRECTDRAWCLIPPER;

// IDirectDrawPalette stub
struct IDirectDrawPalette : public IUnknown {
    virtual HRESULT GetEntries(DWORD flags, DWORD start, DWORD count, void* entries) { return E_NOTIMPL; }
    virtual HRESULT SetEntries(DWORD flags, DWORD start, DWORD count, void* entries) { return E_NOTIMPL; }
};
typedef IDirectDrawPalette* LPDIRECTDRAWPALETTE;

// Earlier DDraw interface aliases (v1, v2, v4 -> v7)
typedef IDirectDrawSurface7  IDirectDrawSurface;
typedef IDirectDrawSurface7  IDirectDrawSurface2;
typedef IDirectDrawSurface7  IDirectDrawSurface4;
typedef IDirectDrawSurface7* LPDIRECTDRAWSURFACE;
typedef IDirectDrawSurface7* LPDIRECTDRAWSURFACE2;
typedef IDirectDrawSurface7* LPDIRECTDRAWSURFACE4;

typedef IDirectDraw7  IDirectDraw;
typedef IDirectDraw7  IDirectDraw2;
typedef IDirectDraw7  IDirectDraw4;
typedef IDirectDraw7* LPDIRECTDRAW;
typedef IDirectDraw7* LPDIRECTDRAW2;
typedef IDirectDraw7* LPDIRECTDRAW4;

// DirectDrawCreate function stubs (after aliases are defined)
inline HRESULT DirectDrawCreate(void* lpGUID, LPDIRECTDRAW* lplpDD, IUnknown* pUnkOuter) {
    if (lplpDD) *lplpDD = NULL;
    return E_NOTIMPL;
}
inline HRESULT DirectDrawCreateEx(void* lpGUID, void** lplpDD, void* iid, IUnknown* pUnkOuter) {
    if (lplpDD) *lplpDD = NULL;
    return E_NOTIMPL;
}

// DDCAPS hardware capability flags
#define DDCAPS_BLT               0x00000200L
#define DDCAPS_BLTQUEUE          0x00000800L
#define DDCAPS_BLTCOLORFILL      0x00004000L
#define DDCAPS_PALETTEVSYNC      0x00200000L
#define DDCAPS_BANKSWITCHED      0x00400000L
#define DDCAPS_NOHARDWARE        0x02000000L
#define DDCAPS_COLORKEY          0x00000400L

// WaitForVerticalBlank flags
#define DDWAITVB_BLOCKBEGIN      0x00000001L
#define DDWAITVB_BLOCKBEGINEVENT 0x00000002L
#define DDWAITVB_BLOCKEND        0x00000004L

// GetBltStatus flags
#define DDGBS_CANBLT             0x00000001L
#define DDGBS_ISBLTDONE          0x00000002L

// DDFLIP flags
#define DDFLIP_WAIT              0x00000001L
#define DDFLIP_DONOTWAIT         0x00000020L
