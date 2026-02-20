// ddraw.h compat shim for macOS
#pragma once
#ifndef DDRAW_H_COMPAT
#define DDRAW_H_COMPAT

#include "windef.h"
#include "wingdi.h"
#include "d3d8types.h"

typedef struct _DDPIXELFORMAT {
    DWORD dwSize, dwFlags, dwFourCC;
    union { DWORD dwRGBBitCount; DWORD dwYUVBitCount; DWORD dwZBufferBitDepth; DWORD dwAlphaBitDepth; };
    union { DWORD dwRBitMask;    DWORD dwYBitMask;    };
    union { DWORD dwGBitMask;    DWORD dwUBitMask;    };
    union { DWORD dwBBitMask;    DWORD dwVBitMask;    };
    union { DWORD dwRGBAlphaBitMask; DWORD dwYUVAlphaBitMask; DWORD dwRGBZBitMask; DWORD dwYUVZBitMask; };
} DDPIXELFORMAT, *LPDDPIXELFORMAT;

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

struct IDirectDrawSurface7 : public IUnknown {
    virtual HRESULT GetSurfaceDesc(LPDDSURFACEDESC2 lpDesc) { return E_NOTIMPL; }
    virtual HRESULT Lock(LPRECT lpRect, LPDDSURFACEDESC2 lpDesc, DWORD flags, HANDLE hEvent) { return E_NOTIMPL; }
    virtual HRESULT Unlock(LPRECT lpRect) { return E_NOTIMPL; }
    virtual HRESULT Blt(LPRECT lpDest, IDirectDrawSurface7* lpSrc, LPRECT lpSrc2, DWORD flags, void* lpDDBltFx) { return E_NOTIMPL; }
    virtual HRESULT GetPixelFormat(LPDDPIXELFORMAT lpFmt) { return E_NOTIMPL; }
    virtual HRESULT GetDC(HDC* lphDC) { return E_NOTIMPL; }
    virtual HRESULT ReleaseDC(HDC hDC) { return E_NOTIMPL; }
};
typedef IDirectDrawSurface7* LPDIRECTDRAWSURFACE7;

struct IDirectDraw7 : public IUnknown {
    virtual HRESULT CreateSurface(LPDDSURFACEDESC2 lpDesc, IDirectDrawSurface7** lplpSurf, IUnknown* pUnk) { return E_NOTIMPL; }
    virtual HRESULT GetDisplayMode(LPDDSURFACEDESC2 lpDesc) { return E_NOTIMPL; }
    virtual HRESULT SetDisplayMode(DWORD w, DWORD h, DWORD bpp, DWORD refresh, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT SetCooperativeLevel(HWND hwnd, DWORD flags) { return E_NOTIMPL; }
    virtual HRESULT RestoreDisplayMode() { return E_NOTIMPL; }
};
typedef IDirectDraw7* LPDIRECTDRAW7;

#define DDCL_NORMAL    0x00000008L
#define DDCL_EXCLUSIVE 0x00000010L
#define DDCL_FULLSCREEN 0x00000020L

#define DDLOCK_WAIT    0x00000001L
#define DDLOCK_READONLY 0x00000010L
#define DDLOCK_WRITEONLY 0x00000020L
#define DDLOCK_NOSYSLOCK 0x00000800L

#endif // DDRAW_H_COMPAT
