// windef.h compat shim for macOS/clang
#pragma once
#ifndef WINDEF_H_COMPAT
#define WINDEF_H_COMPAT

#include <stdint.h>
#include <stddef.h>
#include <string.h>   // strlen, strcpy, strcmp, memcpy, etc.
#include <strings.h>  // strcasecmp, strncasecmp

// Calling conventions (no-ops on macOS)
#define WINAPI
#define APIENTRY
#define CALLBACK
#define PASCAL
#define FAR
#define NEAR
#define __cdecl
#define __fastcall
#define __forceinline inline
#define _cdecl
#define _stdcall
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

// CONST is used in old D3D8 headers as a keyword alias
#define CONST const

// Primitive types
typedef unsigned char       BYTE;
typedef unsigned short      WORD;
typedef unsigned int        UINT;
typedef unsigned int        DWORD;
typedef int                 INT;
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
typedef float               FLOAT;
typedef void*               LPVOID;
typedef void*               PVOID;
typedef const void*         LPCVOID;
typedef size_t              SIZE_T;
typedef char*               LPSTR;
typedef const char*         LPCSTR;
typedef char*               LPTSTR;
typedef const char*         LPCTSTR;

#include "c16string.h"
#ifndef WCHAR_DEFINED
#define WCHAR_DEFINED
typedef char16_t            WCHAR;
typedef WCHAR*              PWSTR;
typedef WCHAR*              LPWSTR;
typedef const WCHAR*        PCWSTR;
typedef const WCHAR*        LPCWSTR;
#endif
// wcs* overloads for char16_t* — existing call sites compile without renaming.
// WCHAR = char16_t throughout; no wchar_t in this codebase.
inline size_t   wcslen (const char16_t* s)                           { return c16slen(s); }
inline char16_t* wcscpy (char16_t* d, const char16_t* s)             { return c16scpy(d, s); }
inline char16_t* wcsncpy(char16_t* d, const char16_t* s, size_t n)   { return c16sncpy(d, s, n); }
inline char16_t* wcscat (char16_t* d, const char16_t* s)             { return c16scat(d, s); }
inline int       wcscmp (const char16_t* a, const char16_t* b)       { return c16scmp(a, b); }
inline int       wcsncmp(const char16_t* a, const char16_t* b, size_t n) { return c16sncmp(a, b, n); }
inline int       wcscasecmp(const char16_t* a, const char16_t* b)    { return c16sicmp(a, b); }
inline const char16_t* wcschr (const char16_t* s, char16_t c)        { return c16schr(s, c); }
inline char16_t* wcschr (char16_t* s, char16_t c)                    { return c16schr(s, c); }
inline const char16_t* wcsrchr(const char16_t* s, char16_t c)        { return c16srchr(s, c); }
inline char16_t* wcsrchr(char16_t* s, char16_t c)                    { return c16srchr(s, c); }
inline const char16_t* wcsstr (const char16_t* h, const char16_t* n) { return c16sstr(h, n); }
inline char16_t* wcsstr (char16_t* h, const char16_t* n)             { return c16sstr(h, n); }
inline char16_t* wcsupr (char16_t* s)                                { return c16supr(s); }
inline char16_t* wcslwr (char16_t* s)                                { return c16slwr(s); }
inline const char16_t* wcspbrk(const char16_t* s, const char16_t* a) { return c16spbrk(s, a); }
inline char16_t* wcspbrk(char16_t* s, const char16_t* a)             { return c16spbrk(s, a); }
inline unsigned long wcstoul(const char16_t* s, char16_t** e, int b) { return c16stoul(s, e, b); }
inline long      wcstol (const char16_t* s, char16_t** e, int b)     { return c16stol(s, e, b); }

typedef DWORD*              LPDWORD;
typedef BYTE*               LPBYTE;
typedef WORD*               LPWORD;
typedef LONG*               LPLONG;
typedef DWORD               COLORREF;

// Handle types (opaque pointers on macOS)
typedef void*               HANDLE;
typedef void*               HWND;
typedef void*               HDC;
typedef void*               HINSTANCE;
typedef void*               HMODULE;
typedef void*               HKEY;
typedef void*               HBITMAP;
typedef void*               HFONT;
typedef void*               HGLOBAL;
typedef void*               HBRUSH;
typedef void*               HMENU;
typedef void*               HMONITOR;
typedef void*               HPALETTE;
typedef void*               HICON;
typedef void*               HCURSOR;
typedef void*               HACCEL;
typedef void*               HKL;

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
// C++ COM convention: REFGUID/REFIID/REFCLSID are const references, not pointers
typedef const GUID& REFGUID;
typedef const GUID& REFIID;
typedef const GUID& REFCLSID;
typedef GUID IID;
typedef GUID CLSID;
#endif
#define DEFINE_GUID(name, l, w1, w2, b1,b2,b3,b4,b5,b6,b7,b8) \
    extern const GUID name

// strupr/strlwr — not in POSIX
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
} RECT, *LPRECT;

typedef struct tagPOINT {
    LONG x;
    LONG y;
} POINT, *LPPOINT;

typedef struct tagSIZE {
    LONG cx;
    LONG cy;
} SIZE;

// Macros
#define MAKEWORD(a, b)      ((WORD)(((BYTE)(a)) | ((WORD)((BYTE)(b))) << 8))
#define MAKELONG(a, b)      ((LONG)(((WORD)(a)) | ((DWORD)((WORD)(b))) << 16))
#define LOWORD(l)           ((WORD)(l))
#define HIWORD(l)           ((WORD)(((DWORD)(l) >> 16) & 0xFFFF))

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

// COM result type and common codes
typedef long HRESULT;
#define S_OK                0
#define S_FALSE             1
#define E_FAIL              0x80004005L
#define E_NOTIMPL           0x80004001L
#define E_NOINTERFACE       0x80004002L
#define E_POINTER           0x80000005L
#define SUCCEEDED(hr)       ((HRESULT)(hr) >= 0)
#define FAILED(hr)          ((HRESULT)(hr) < 0)

#define INFINITE            0xFFFFFFFF

// File access flags
#define GENERIC_READ        0x80000000
#define GENERIC_WRITE       0x40000000

#define INVALID_HANDLE_VALUE ((HANDLE)(intptr_t)(-1))

#include "floattypes.h"

#endif // WINDEF_H_COMPAT
