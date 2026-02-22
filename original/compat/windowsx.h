#pragma once

#include "global.h"

// windowsx.h compat shim for macOS - Windows message crackers and macros stub
#define _INC_WINDOWSX

#include "windows.h"

// Common message crackers / helper macros used from windowsx.h
#ifndef GET_X_LPARAM
#define GET_X_LPARAM(lp) ((int)(short)LOWORD(lp))
#endif
#ifndef GET_Y_LPARAM
#define GET_Y_LPARAM(lp) ((int)(short)HIWORD(lp))
#endif

// SelectObject / DeleteObject helpers
#ifndef SelectBitmap
#define SelectBitmap(hdc, hbm)  ((HBITMAP)SelectObject((hdc), (HGDIOBJ)(HBITMAP)(hbm)))
#endif
#ifndef DeleteBitmap
#define DeleteBitmap(hbm)       (DeleteObject((HGDIOBJ)(HBITMAP)(hbm)) != 0)
#endif
