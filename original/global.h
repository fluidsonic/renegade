// global.h — single universal include for the C&C Renegade macOS/clang port
// Absorbs: compat/windef.h, compat/c16string.h, compat/clangcompat.h,
//          compat/floattypes.h, Code/wwlib/always.h
#pragma once
#ifndef GLOBAL_H
#define GLOBAL_H

// ─────────────────────────────────────────────────────────────────────────────
// Section 1: Platform defines
// ─────────────────────────────────────────────────────────────────────────────
#ifndef _UNIX
#define _UNIX
#endif
#define NOMINMAX

// ─────────────────────────────────────────────────────────────────────────────
// Section 2: Standard C headers
// ─────────────────────────────────────────────────────────────────────────────
#include <stdint.h>
#include <stddef.h>
#include <string.h>   // strlen, strcpy, strcmp, memcpy, etc.
#include <strings.h>  // strcasecmp, strncasecmp
#include <ctype.h>

// ─────────────────────────────────────────────────────────────────────────────
// Section 3: MSVC compiler compat (Clang/macOS only)
// ─────────────────────────────────────────────────────────────────────────────
#if defined(__clang__) && !defined(_MSC_VER)

#pragma clang diagnostic ignored "-Wpragmas"
#pragma clang diagnostic ignored "-Wunknown-pragmas"

// __int64 / _int64 aliases (MSVC types)
#ifndef __int64
typedef long long __int64;
#endif
#ifndef _int64
typedef long long _int64;
#endif
typedef unsigned long long __uint64;

// MSVC __assume hint is no-op
#define __assume(x) ((void)0)

// MSVC-specific struct/member alignment pragmas (ignore)
#define _pragma(x)

// Function attributes
#define __pascal

// Fix 64-bit type sizes: MSVC Win32 uses 'unsigned long' for 32-bit,
// but on macOS/LP64, 'unsigned long' is 64 bits.
// Redefine the problematic types from bittype.h here:
#undef uint32
#undef sint32
typedef unsigned int    uint32;
typedef signed int      sint32;

// Bit rotation (MSVC intrinsics)
static inline unsigned long _lrotl(unsigned long val, int shift) {
    shift &= 31;
    return (val << shift) | (val >> (32 - shift));
}
static inline unsigned long _lrotr(unsigned long val, int shift) {
    shift &= 31;
    return (val >> shift) | (val << (32 - shift));
}

// _countof (MSVC macro, define if not present)
#ifndef _countof
#define _countof(arr) (sizeof(arr)/sizeof((arr)[0]))
#endif

#endif // defined(__clang__) && !defined(_MSC_VER)

// ─────────────────────────────────────────────────────────────────────────────
// Section 4: Calling convention macros (no-ops on macOS)
// ─────────────────────────────────────────────────────────────────────────────
#define WINAPI
#define APIENTRY
#define CALLBACK
#define PASCAL
#define FAR
#define NEAR
#ifndef __cdecl
#define __cdecl
#endif
#ifndef __fastcall
#define __fastcall
#endif
#define __forceinline inline
#ifndef _cdecl
#define _cdecl
#endif
#ifndef _stdcall
#define _stdcall
#endif
#ifndef __declspec
#define __declspec(x)
#endif
#ifndef _declspec
#define _declspec(x)
#endif

// CONST is used in old D3D8 headers as a keyword alias
#define CONST const

// ─────────────────────────────────────────────────────────────────────────────
// Section 5: String compat macros (Windows names → POSIX)
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Section 6: Windows primitive types
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Section 7: Pointer/handle types
// ─────────────────────────────────────────────────────────────────────────────
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

// ULONG must be 32-bit on all platforms (unsigned long is 64-bit on macOS/LP64)
#if defined(__clang__) && !defined(_MSC_VER)
typedef unsigned int        ULONG;
#else
typedef unsigned long       ULONG;
#endif
typedef intptr_t            INT_PTR;
typedef intptr_t            LONG_PTR;
typedef uintptr_t           ULONG_PTR;

// ─────────────────────────────────────────────────────────────────────────────
// Section 8: GUID/COM types
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Section 9: Structs and string helpers
// ─────────────────────────────────────────────────────────────────────────────

// strupr/strlwr — not in POSIX
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

// ─────────────────────────────────────────────────────────────────────────────
// Section 10: Utility macros
// ─────────────────────────────────────────────────────────────────────────────
#define MAKEWORD(a, b)      ((WORD)(((BYTE)(a)) | ((WORD)((BYTE)(b))) << 8))
#define MAKELONG(a, b)      ((LONG)(((WORD)(a)) | ((DWORD)((WORD)(b))) << 16))
#define LOWORD(l)           ((WORD)(l))
#define HIWORD(l)           ((WORD)(((DWORD)(l) >> 16) & 0xFFFF))

#define RGB(r,g,b)          ((COLORREF)(((BYTE)(r)|((WORD)((BYTE)(g))<<8))|(((DWORD)(BYTE)(b))<<16)))
#define GetRValue(rgb)      ((BYTE)(rgb))
#define GetGValue(rgb)      ((BYTE)(((WORD)(rgb)) >> 8))
#define GetBValue(rgb)      ((BYTE)((rgb)>>16))

// ─────────────────────────────────────────────────────────────────────────────
// Section 11: Constants
// ─────────────────────────────────────────────────────────────────────────────
#define TRUE  1
#define FALSE 0
#ifndef NULL
#define NULL  0
#endif

#define MAX_PATH  260
#define _MAX_PATH 260

#define INFINITE            0xFFFFFFFF

// File access flags
#define GENERIC_READ        0x80000000
#define GENERIC_WRITE       0x40000000

#define INVALID_HANDLE_VALUE ((HANDLE)(intptr_t)(-1))

// ─────────────────────────────────────────────────────────────────────────────
// Section 12: char16_t string functions (c16slen, c16scpy, etc.)
// ─────────────────────────────────────────────────────────────────────────────

// c16slen — equivalent of wcslen
inline size_t c16slen(const char16_t* s) {
    const char16_t* p = s;
    while (*p) p++;
    return (size_t)(p - s);
}

// c16scpy — equivalent of wcscpy
inline char16_t* c16scpy(char16_t* dst, const char16_t* src) {
    char16_t* d = dst;
    while ((*d++ = *src++) != 0) {}
    return dst;
}

// c16sncpy — equivalent of wcsncpy
inline char16_t* c16sncpy(char16_t* dst, const char16_t* src, size_t n) {
    char16_t* d = dst;
    while (n > 0 && *src) { *d++ = *src++; n--; }
    while (n > 0) { *d++ = 0; n--; }
    return dst;
}

// c16scat — equivalent of wcscat
inline char16_t* c16scat(char16_t* dst, const char16_t* src) {
    char16_t* d = dst;
    while (*d) d++;
    while ((*d++ = *src++) != 0) {}
    return dst;
}

// c16scmp — equivalent of wcscmp
inline int c16scmp(const char16_t* a, const char16_t* b) {
    while (*a && *a == *b) { a++; b++; }
    return (int)(unsigned short)*a - (int)(unsigned short)*b;
}

// c16sncmp — equivalent of wcsncmp
inline int c16sncmp(const char16_t* a, const char16_t* b, size_t n) {
    while (n > 0 && *a && *a == *b) { a++; b++; n--; }
    if (n == 0) return 0;
    return (int)(unsigned short)*a - (int)(unsigned short)*b;
}

// c16_tolower / c16_toupper — ASCII-range only (sufficient for game data)
inline char16_t c16_tolower(char16_t c) {
    if (c >= u'A' && c <= u'Z') return (char16_t)(c + 32);
    return c;
}
inline char16_t c16_toupper(char16_t c) {
    if (c >= u'a' && c <= u'z') return (char16_t)(c - 32);
    return c;
}

// c16sicmp — case-insensitive comparison (ASCII range), equivalent of _wcsicmp / wcscasecmp
inline int c16sicmp(const char16_t* a, const char16_t* b) {
    while (*a && c16_tolower(*a) == c16_tolower(*b)) { a++; b++; }
    return (int)c16_tolower(*a) - (int)c16_tolower(*b);
}

// c16snicmp — case-insensitive comparison with length limit, equivalent of _wcsnicmp
inline int c16snicmp(const char16_t* a, const char16_t* b, size_t n) {
    while (n > 0 && *a && c16_tolower(*a) == c16_tolower(*b)) { a++; b++; n--; }
    if (n == 0) return 0;
    return (int)c16_tolower(*a) - (int)c16_tolower(*b);
}

// c16schr — equivalent of wcschr
inline const char16_t* c16schr(const char16_t* s, char16_t c) {
    while (*s) { if (*s == c) return s; s++; }
    return (c == 0) ? s : nullptr;
}
inline char16_t* c16schr(char16_t* s, char16_t c) {
    while (*s) { if (*s == c) return s; s++; }
    return (c == 0) ? s : nullptr;
}

// c16srchr — equivalent of wcsrchr
inline const char16_t* c16srchr(const char16_t* s, char16_t c) {
    const char16_t* last = nullptr;
    while (*s) { if (*s == c) last = s; s++; }
    if (c == 0) return s;
    return last;
}
inline char16_t* c16srchr(char16_t* s, char16_t c) {
    char16_t* last = nullptr;
    while (*s) { if (*s == c) last = s; s++; }
    if (c == 0) return s;
    return last;
}

// c16sstr — equivalent of wcsstr
inline const char16_t* c16sstr(const char16_t* haystack, const char16_t* needle) {
    if (!*needle) return haystack;
    for (; *haystack; haystack++) {
        const char16_t* h = haystack;
        const char16_t* n = needle;
        while (*h && *n && *h == *n) { h++; n++; }
        if (!*n) return haystack;
    }
    return nullptr;
}
inline char16_t* c16sstr(char16_t* haystack, const char16_t* needle) {
    return const_cast<char16_t*>(c16sstr((const char16_t*)haystack, needle));
}

// c16strim — trim leading and trailing whitespace in-place (equivalent of wcstrim)
inline char16_t* c16strim(char16_t* s) {
    if (!s) return s;
    size_t len = c16slen(s);
    while (len > 0 && s[len-1] <= 32) len--;
    s[len] = 0;
    size_t start = 0;
    while (s[start] && s[start] <= 32) start++;
    if (start > 0) {
        size_t i;
        for (i = 0; s[start + i]; i++) s[i] = s[start + i];
        s[i] = 0;
    }
    return s;
}

// c16supr — convert to uppercase in-place (ASCII range), equivalent of _wcsupr
inline char16_t* c16supr(char16_t* s) {
    char16_t* p = s;
    while (*p) { *p = c16_toupper(*p); p++; }
    return s;
}

// c16slwr — convert to lowercase in-place (ASCII range), equivalent of _wcslwr
inline char16_t* c16slwr(char16_t* s) {
    char16_t* p = s;
    while (*p) { *p = c16_tolower(*p); p++; }
    return s;
}

// c16spbrk — equivalent of wcspbrk (find first char from accept set)
inline const char16_t* c16spbrk(const char16_t* s, const char16_t* accept) {
    for (; *s; s++) {
        for (const char16_t* a = accept; *a; a++) {
            if (*s == *a) return s;
        }
    }
    return nullptr;
}
inline char16_t* c16spbrk(char16_t* s, const char16_t* accept) {
    return const_cast<char16_t*>(c16spbrk((const char16_t*)s, accept));
}

// c16stoul — equivalent of wcstoul for ASCII digit strings
inline unsigned long c16stoul(const char16_t* s, char16_t** endptr, int base) {
    while (*s == u' ' || *s == u'\t') s++;
    if (base == 0) {
        if (s[0] == u'0' && (s[1] == u'x' || s[1] == u'X')) { base = 16; s += 2; }
        else { base = 10; }
    } else if (base == 16 && s[0] == u'0' && (s[1] == u'x' || s[1] == u'X')) {
        s += 2;
    }
    unsigned long result = 0;
    const char16_t* p = s;
    while (*p) {
        int digit = -1;
        if (*p >= u'0' && *p <= u'9') digit = *p - u'0';
        else if (base == 16 && *p >= u'a' && *p <= u'f') digit = *p - u'a' + 10;
        else if (base == 16 && *p >= u'A' && *p <= u'F') digit = *p - u'A' + 10;
        if (digit < 0 || digit >= base) break;
        result = result * (unsigned long)base + (unsigned long)digit;
        p++;
    }
    if (endptr) *endptr = (char16_t*)p;
    return result;
}

// c16stol — equivalent of wcstol for ASCII digit strings
inline long c16stol(const char16_t* s, char16_t** endptr, int base) {
    while (*s == u' ' || *s == u'\t') s++;
    int sign = 1;
    if (*s == u'-') { sign = -1; s++; }
    else if (*s == u'+') { s++; }
    return sign * (long)c16stoul(s, endptr, base);
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 13: char16_t wcs* overloads + WCHAR typedef
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// Section 14: IEEE 754 float size guarantees
// ─────────────────────────────────────────────────────────────────────────────
static_assert(sizeof(float)  == 4, "float must be 32-bit IEEE 754");
static_assert(sizeof(double) == 8, "double must be 64-bit IEEE 754");

// ─────────────────────────────────────────────────────────────────────────────
// Section 15: Engine macros (from always.h)
// ─────────────────────────────────────────────────────────────────────────────

// Disable warning about exception handling not being enabled (MSVC 4530)
#pragma warning(disable : 4530)

#define WWINLINE inline

#ifndef MAX
#define MAX(a,b)            (((a) > (b)) ? (a) : (b))
#endif
#ifndef MIN
#define MIN(a,b)            (((a) < (b)) ? (a) : (b))
#endif

#ifdef min
#undef min
#endif
#ifdef max
#undef max
#endif

template <class T> T min(T a, T b) { if (a < b) { return a; } else { return b; } }
template <class T> T max(T a, T b) { if (a > b) { return a; } else { return b; } }

#ifndef ARRAY_SIZE
#define ARRAY_SIZE(x) int(sizeof(x)/sizeof(x[0]))
#endif

#ifndef size_of
#define size_of(typ,id) sizeof(((typ*)0)->id)
#endif

// ─────────────────────────────────────────────────────────────────────────────
// Section 16: Debug
// ─────────────────────────────────────────────────────────────────────────────
#include <cassert>
#define Debug_Say(...) ((void)0)

// ─────────────────────────────────────────────────────────────────────────────
// Section 17: Exception stubs
// ─────────────────────────────────────────────────────────────────────────────
inline void Set_Exit_On_Exception(bool set) { (void)set; }
#ifndef IS_TRYING_TO_EXIT_DEFINED
#define IS_TRYING_TO_EXIT_DEFINED
inline bool Is_Trying_To_Exit(void) { return false; }
#endif

#endif // GLOBAL_H
