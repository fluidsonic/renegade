// wingdi.h compat shim for macOS/clang
#pragma once
#ifndef WINGDI_H_COMPAT
#define WINGDI_H_COMPAT

#include "windef.h"

typedef void* HGDIOBJ;
typedef void* HFONT;

// PALETTEENTRY
typedef struct tagPALETTEENTRY {
    BYTE peRed;
    BYTE peGreen;
    BYTE peBlue;
    BYTE peFlags;
} PALETTEENTRY, *LPPALETTEENTRY;

// LOGPALETTE
typedef struct tagLOGPALETTE {
    WORD         palVersion;
    WORD         palNumEntries;
    PALETTEENTRY palPalEntry[1];
} LOGPALETTE, *LPLOGPALETTE;

// RGBQUAD
typedef struct tagRGBQUAD {
    BYTE rgbBlue;
    BYTE rgbGreen;
    BYTE rgbRed;
    BYTE rgbReserved;
} RGBQUAD, *LPRGBQUAD;

// BITMAPINFOHEADER
typedef struct tagBITMAPINFOHEADER {
    DWORD biSize;
    LONG  biWidth;
    LONG  biHeight;
    WORD  biPlanes;
    WORD  biBitCount;
    DWORD biCompression;
    DWORD biSizeImage;
    LONG  biXPelsPerMeter;
    LONG  biYPelsPerMeter;
    DWORD biClrUsed;
    DWORD biClrImportant;
} BITMAPINFOHEADER, *LPBITMAPINFOHEADER;

typedef struct tagBITMAPINFO {
    BITMAPINFOHEADER bmiHeader;
    RGBQUAD          bmiColors[1];
} BITMAPINFO, *LPBITMAPINFO;

// DC operations stubs
inline HGDIOBJ SelectObject(HDC dc, HGDIOBJ obj) { return NULL; }
inline BOOL DeleteObject(HGDIOBJ obj) { return FALSE; }
inline HPALETTE CreatePalette(const LOGPALETTE* lp) { return NULL; }
inline HPALETTE SelectPalette(HDC dc, HPALETTE pal, BOOL force) { return NULL; }
inline UINT RealizePalette(HDC dc) { return 0; }
inline BOOL DeleteDC(HDC dc) { return FALSE; }
inline HDC CreateCompatibleDC(HDC dc) { return NULL; }
inline HBITMAP CreateCompatibleBitmap(HDC dc, int w, int h) { return NULL; }
inline BOOL BitBlt(HDC dst, int dx, int dy, int w, int h, HDC src, int sx, int sy, DWORD rop) { return FALSE; }
inline BOOL StretchBlt(HDC dst, int dx, int dy, int dw, int dh, HDC src, int sx, int sy, int sw, int sh, DWORD rop) { return FALSE; }

// Raster ops
#define SRCCOPY  0x00CC0020
#define SRCPAINT 0x00EE0086
#define SRCAND   0x008800C6
#define SRCINVERT 0x00660046

// Text
inline BOOL TextOut(HDC dc, int x, int y, LPCSTR str, int len) { return FALSE; }
inline COLORREF SetBkColor(HDC dc, COLORREF c) { return 0; }
inline COLORREF SetTextColor(HDC dc, COLORREF c) { return 0; }
inline int SetBkMode(HDC dc, int mode) { return 0; }
#define TRANSPARENT 1
#define OPAQUE      2

#endif // WINGDI_H_COMPAT
