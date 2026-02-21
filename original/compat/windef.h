// windef.h compat shim for macOS/clang
#pragma once
#ifndef WINDEF_H_COMPAT
#define WINDEF_H_COMPAT

#include <stdint.h>
#include <stddef.h>

// Calling conventions (no-ops on non-Windows)
#define WINAPI
#define APIENTRY
#define CALLBACK
#define CDECL
#define PASCAL
#define FAR
#define NEAR
#define __cdecl
#define __stdcall
#define __fastcall
#define __forceinline inline
// Single-underscore aliases (MSVC also accepts these)
#define _cdecl
#define _stdcall
#define _fastcall
#define _pascal
// __declspec / _declspec - no-op on non-Windows
#ifndef __declspec
#define __declspec(x)
#endif
#ifndef _declspec
#define _declspec(x)
#endif

// Case-insensitive string comparison (Windows names mapped to POSIX)
#ifndef stricmp
#define stricmp  strcasecmp
#endif
#ifndef strcmpi
#define strcmpi  strcasecmp
#endif
#ifndef strnicmp
#define strnicmp strncasecmp
#endif
#ifndef _stricmp
#define _stricmp strcasecmp
#endif
#ifndef _strnicmp
#define _strnicmp strncasecmp
#endif

// CONST is used in old D3D8/Windows headers as a keyword alias
#define CONST const

// Primitive types
typedef unsigned char       BYTE;
typedef unsigned short      WORD;
typedef unsigned int        UINT;
typedef unsigned int        DWORD;
typedef unsigned int        ULONG;
typedef int                 INT;
typedef unsigned long       ULONG_PTR;
typedef long                LONG;
/* In Objective-C/Objective-C++ mode __OBJC__ is defined, and objc.h defines
   BOOL as 'bool'. Match that here so the later typedef bool BOOL in objc.h
   is a harmless compatible redefinition rather than a conflicting one. */
#ifdef __OBJC__
typedef bool                BOOL;
#else
typedef int                 BOOL;
#endif
typedef unsigned short      USHORT;
typedef short               SHORT;
typedef char                CHAR;
typedef unsigned char       UCHAR;
typedef float               FLOAT;
typedef double              DOUBLE;
typedef void*               LPVOID;
typedef void*               PVOID;
typedef const void*         LPCVOID;
typedef size_t              SIZE_T;
typedef ptrdiff_t           SSIZE_T;
typedef char*               LPSTR;
typedef const char*         LPCSTR;
typedef char*               LPTSTR;
typedef const char*         LPCTSTR;
#include <wchar.h>
#ifndef WCHAR_DEFINED
#define WCHAR_DEFINED
typedef wchar_t             WCHAR;
typedef WCHAR*              PWSTR;
typedef WCHAR*              LPWSTR;
typedef const WCHAR*        PCWSTR;
typedef const WCHAR*        LPCWSTR;
#endif
typedef int*                LPINT;
typedef DWORD*              LPDWORD;
typedef BYTE*               LPBYTE;
typedef WORD*               LPWORD;
typedef LONG*               LPLONG;
typedef BOOL*               LPBOOL;
typedef void                VOID;
typedef DWORD               COLORREF;
typedef DWORD*              LPCOLORREF;

// Handle types (opaque pointers)
typedef void*               HANDLE;
typedef void*               HWND;
typedef void*               HDC;
typedef void*               HINSTANCE;
typedef void*               HMODULE;
typedef void*               HKEY;
typedef void*               HBITMAP;
typedef void*               HBRUSH;
typedef void*               HFONT;
typedef void*               HGLOBAL;
typedef void*               HLOCAL;
typedef void*               HMENU;
typedef void*               HPALETTE;
typedef void*               HPEN;
typedef void*               HRGN;
typedef void*               HRSRC;
typedef void*               HICON;
typedef void*               HCURSOR;
typedef void*               HTASK;
typedef void*               HFILE;
typedef void*               HHOOK;
typedef void*               HMONITOR;
typedef void*               HACCEL;
typedef void*               HKL;

typedef uintptr_t           UINT_PTR;
typedef intptr_t            INT_PTR;
typedef intptr_t            LONG_PTR;

// GUID type
#ifndef GUID_DEFINED
#define GUID_DEFINED
typedef struct _GUID {
    unsigned long  Data1;
    unsigned short Data2;
    unsigned short Data3;
    unsigned char  Data4[8];
} GUID, *LPGUID;
// C++ COM convention: REFGUID/REFIID are const references, not pointers
typedef const GUID& REFGUID;
typedef const GUID& REFIID;
typedef const GUID& REFCLSID;
typedef const GUID& REFFMTID;
typedef GUID IID;
typedef GUID CLSID;
#endif
#define DEFINE_GUID(name, l, w1, w2, b1,b2,b3,b4,b5,b6,b7,b8) \
    extern const GUID name

// strupr/strlwr - not in POSIX, commonly used in Windows code
#include <ctype.h>
#ifndef _STRUPR_DEFINED
#define _STRUPR_DEFINED
inline char* strupr(char* s) {
    char* p = s;
    while (*p) { *p = (char)toupper((unsigned char)*p); p++; }
    return s;
}
inline char* strlwr(char* s) {
    char* p = s;
    while (*p) { *p = (char)tolower((unsigned char)*p); p++; }
    return s;
}
#define _strupr strupr
#define _strlwr strlwr
#endif

// Common struct types
typedef struct tagRECT {
    LONG left;
    LONG top;
    LONG right;
    LONG bottom;
} RECT, *PRECT, *LPRECT;

typedef struct tagPOINT {
    LONG x;
    LONG y;
} POINT, *PPOINT, *LPPOINT;

typedef struct tagSIZE {
    LONG cx;
    LONG cy;
} SIZE, *PSIZE, *LPSIZE;

typedef struct tagPOINTS {
    SHORT x;
    SHORT y;
} POINTS, *PPOINTS;

// Macros
#define MAKEWORD(a, b)      ((WORD)(((BYTE)(a)) | ((WORD)((BYTE)(b))) << 8))
#define MAKELONG(a, b)      ((LONG)(((WORD)(a)) | ((DWORD)((WORD)(b))) << 16))
#define LOWORD(l)           ((WORD)(l))
#define HIWORD(l)           ((WORD)(((DWORD)(l) >> 16) & 0xFFFF))
#define LOBYTE(w)           ((BYTE)(w))
#define HIBYTE(w)           ((BYTE)(((WORD)(w) >> 8) & 0xFF))

#define RGB(r,g,b)          ((COLORREF)(((BYTE)(r)|((WORD)((BYTE)(g))<<8))|(((DWORD)(BYTE)(b))<<16)))
#define GetRValue(rgb)      ((BYTE)(rgb))
#define GetGValue(rgb)      ((BYTE)(((WORD)(rgb)) >> 8))
#define GetBValue(rgb)      ((BYTE)((rgb)>>16))

#define TRUE  1
#define FALSE 0
#ifndef NULL
#define NULL  0
#endif

#define MAX_PATH  260
#define _MAX_PATH 260

// Common return values
#define S_OK                0
#define S_FALSE             1
#define E_FAIL              0x80004005L
#define E_NOTIMPL           0x80004001L
#define E_NOINTERFACE       0x80004002L
#define E_POINTER           0x80000005L  // fixed: was 0x80004003L
#define E_OUTOFMEMORY       0x8007000EL
#define E_INVALIDARG        0x80070057L
#define SUCCEEDED(hr)       ((HRESULT)(hr) >= 0)
#define FAILED(hr)          ((HRESULT)(hr) < 0)
typedef long HRESULT;

// INFINITE wait value
#define INFINITE            0xFFFFFFFF

// File access
#define GENERIC_READ        0x80000000
#define GENERIC_WRITE       0x40000000
#define GENERIC_EXECUTE     0x20000000
#define GENERIC_ALL         0x10000000

#define INVALID_HANDLE_VALUE ((HANDLE)(intptr_t)(-1))

#endif // WINDEF_H_COMPAT
