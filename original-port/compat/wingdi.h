// wingdi.h compat shim for macOS/clang
#pragma once
#ifndef WINGDI_H_COMPAT
#define WINGDI_H_COMPAT

#include "windef.h"

typedef void* HGDIOBJ;
// HFONT already typedef'd in windef.h
#ifndef HFONT_DEFINED
#define HFONT_DEFINED
// (HFONT is void* from windef.h)
#endif

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

// Bitmap compression types
#define BI_RGB       0
#define BI_RLE8      1
#define BI_RLE4      2
#define BI_BITFIELDS 3

// CreateDIBSection / GetDIBits usage flags
#define DIB_RGB_COLORS  0
#define DIB_PAL_COLORS  1

// CreateDIBSection stub
inline HBITMAP CreateDIBSection(HDC dc, const BITMAPINFO* bmi, UINT usage, void** ppvBits, HANDLE hSection, DWORD offset) {
    if (ppvBits) *ppvBits = NULL;
    return NULL;
}

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

// Gamma ramp
typedef struct _RAMP {
    WORD red[256];
    WORD green[256];
    WORD blue[256];
} RAMP;
inline BOOL SetDeviceGammaRamp(HDC hdc, LPVOID lpRamp) { return FALSE; }
inline BOOL GetDeviceGammaRamp(HDC hdc, LPVOID lpRamp) { return FALSE; }

// Color macro
#ifndef RGB
#define RGB(r,g,b) ((COLORREF)(((BYTE)(r)|((WORD)((BYTE)(g))<<8))|(((DWORD)(BYTE)(b))<<16)))
#endif
#define GetRValue(rgb) ((BYTE)(rgb))
#define GetGValue(rgb) ((BYTE)((rgb)>>8))
#define GetBValue(rgb) ((BYTE)((rgb)>>16))

// Font weight constants
#define FW_THIN       100
#define FW_NORMAL     400
#define FW_BOLD       700
#define FW_BLACK      900

// Charset constants
#define ANSI_CHARSET          0
#define DEFAULT_CHARSET       1
#define OEM_CHARSET           2
#define SHIFTJIS_CHARSET      128
#define HANGUL_CHARSET        129
#define CHINESEBIG5_CHARSET   136
#define GREEK_CHARSET         161
#define TURKISH_CHARSET       162
#define HEBREW_CHARSET        177
#define ARABIC_CHARSET        178
#define BALTIC_CHARSET        186
#define RUSSIAN_CHARSET       204
#define EASTEUROPE_CHARSET    238

// Precision / quality constants
#define OUT_DEFAULT_PRECIS    0
#define CLIP_DEFAULT_PRECIS   0
#define DEFAULT_QUALITY       0
#define DRAFT_QUALITY         1
#define PROOF_QUALITY         2
#define NONANTIALIASED_QUALITY 3
#define ANTIALIASED_QUALITY   4

// Pitch and family
#define DEFAULT_PITCH         0
#define FIXED_PITCH           1
#define VARIABLE_PITCH        2
#define FF_DONTCARE           0
#define FF_ROMAN              16
#define FF_SWISS              32
#define FF_MODERN             48
#define FF_SCRIPT             64
#define FF_DECORATIVE         80

// GetDeviceCaps indices
#define LOGPIXELSX  88
#define LOGPIXELSY  90
#define HORZRES     8
#define VERTRES     10
#define BITSPIXEL   12

// ExtTextOut flags
#define ETO_OPAQUE   0x0002
#define ETO_CLIPPED  0x0004

// TEXTMETRIC struct
typedef struct tagTEXTMETRIC {
    LONG tmHeight;
    LONG tmAscent;
    LONG tmDescent;
    LONG tmInternalLeading;
    LONG tmExternalLeading;
    LONG tmAveCharWidth;
    LONG tmMaxCharWidth;
    LONG tmWeight;
    LONG tmOverhang;
    LONG tmDigitizedAspectX;
    LONG tmDigitizedAspectY;
    BYTE tmFirstChar;
    BYTE tmLastChar;
    BYTE tmDefaultChar;
    BYTE tmBreakChar;
    BYTE tmItalic;
    BYTE tmUnderlined;
    BYTE tmStruckOut;
    BYTE tmPitchAndFamily;
    BYTE tmCharSet;
} TEXTMETRIC, *LPTEXTMETRIC;

// LOGFONT
typedef struct tagLOGFONT {
    LONG  lfHeight;
    LONG  lfWidth;
    LONG  lfEscapement;
    LONG  lfOrientation;
    LONG  lfWeight;
    BYTE  lfItalic;
    BYTE  lfUnderline;
    BYTE  lfStrikeOut;
    BYTE  lfCharSet;
    BYTE  lfOutPrecision;
    BYTE  lfClipPrecision;
    BYTE  lfQuality;
    BYTE  lfPitchAndFamily;
    char  lfFaceName[32];
} LOGFONT, LOGFONTA, *LPLOGFONT, *LPLOGFONTA;

typedef struct tagLOGFONTW {
    LONG  lfHeight;
    LONG  lfWidth;
    LONG  lfEscapement;
    LONG  lfOrientation;
    LONG  lfWeight;
    BYTE  lfItalic;
    BYTE  lfUnderline;
    BYTE  lfStrikeOut;
    BYTE  lfCharSet;
    BYTE  lfOutPrecision;
    BYTE  lfClipPrecision;
    BYTE  lfQuality;
    BYTE  lfPitchAndFamily;
    wchar_t lfFaceName[32];
} LOGFONTW, *LPLOGFONTW;

// Font resource functions (Windows loads fonts from .fon/.ttf files)
inline int AddFontResourceA(LPCSTR lpFilename) { return 0; }
inline int AddFontResourceW(LPCWSTR lpFilename) { return 0; }
inline BOOL RemoveFontResourceA(LPCSTR lpFilename) { return FALSE; }
inline BOOL RemoveFontResourceW(LPCWSTR lpFilename) { return FALSE; }
#define AddFontResource    AddFontResourceA
#define RemoveFontResource RemoveFontResourceA

// MulDiv
inline int MulDiv(int nNumber, int nNumerator, int nDenominator) {
    if (nDenominator == 0) return -1;
    return (int)(((long long)nNumber * nNumerator + nDenominator/2) / nDenominator);
}

// GetACP - returns US English code page
inline UINT GetACP(void) { return 1252; }

// DC capabilities stubs
inline int GetDeviceCaps(HDC hdc, int nIndex) {
    if (nIndex == LOGPIXELSX || nIndex == LOGPIXELSY) return 96;
    if (nIndex == HORZRES) return 1920;
    if (nIndex == VERTRES) return 1080;
    if (nIndex == BITSPIXEL) return 32;
    return 0;
}

// Font stubs
inline HFONT CreateFontA(int cHeight, int cWidth, int cEscapement, int cOrientation,
    int cWeight, DWORD bItalic, DWORD bUnderline, DWORD bStrikeOut,
    DWORD iCharSet, DWORD iOutPrecision, DWORD iClipPrecision,
    DWORD iQuality, DWORD iPitchAndFamily, LPCSTR pszFaceName) { return NULL; }
#define CreateFont CreateFontA

inline BOOL GetTextMetricsA(HDC hdc, TEXTMETRIC* lptm) {
    if (lptm) { memset(lptm, 0, sizeof(TEXTMETRIC)); lptm->tmHeight=16; }
    return TRUE;
}
#define GetTextMetrics GetTextMetricsA

inline BOOL GetTextExtentPoint32A(HDC hdc, LPCSTR lpString, int c, SIZE* psizl) {
    if (psizl) { psizl->cx = c*8; psizl->cy = 16; }
    return TRUE;
}
inline BOOL GetTextExtentPoint32W(HDC hdc, const WCHAR* lpString, int c, SIZE* psizl) {
    if (psizl) { psizl->cx = c*8; psizl->cy = 16; }
    return TRUE;
}

inline BOOL ExtTextOutA(HDC hdc, int x, int y, UINT options, const RECT* lprect,
    LPCSTR lpString, UINT c, const INT* lpDx) { return TRUE; }
inline BOOL ExtTextOutW(HDC hdc, int x, int y, UINT options, const RECT* lprect,
    const WCHAR* lpString, UINT c, const INT* lpDx) { return TRUE; }

// Text
inline BOOL TextOut(HDC dc, int x, int y, LPCSTR str, int len) { return FALSE; }
inline COLORREF SetBkColor(HDC dc, COLORREF c) { return 0; }
inline COLORREF SetTextColor(HDC dc, COLORREF c) { return 0; }
inline int SetBkMode(HDC dc, int mode) { return 0; }
#define TRANSPARENT 1
#define OPAQUE      2

#endif // WINGDI_H_COMPAT
