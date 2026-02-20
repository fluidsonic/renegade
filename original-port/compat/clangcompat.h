// clangcompat.h - Clang/macOS specific compatibility for C&C Renegade
// Included from always.h when __clang__ is defined
#pragma once
#ifndef CLANGCOMPAT_H
#define CLANGCOMPAT_H

#include <stdint.h>

// Disable MSVC-specific pragmas and attributes
#pragma clang diagnostic ignored "-Wpragmas"
#pragma clang diagnostic ignored "-Wunknown-pragmas"

// __int64 / _int64 aliases (MSVC types)
#ifndef __int64
typedef long long __int64;
#endif
#ifndef _int64
typedef long long _int64;
#endif
typedef long long          __int64;
typedef unsigned long long __uint64;

// MSVC __forceinline -> inline
#define __forceinline inline

// MSVC __cdecl and __stdcall are no-ops on clang/macOS
#ifndef __cdecl
#define __cdecl
#endif
#ifndef __stdcall
#define __stdcall
#endif
#ifndef __fastcall
#define __fastcall
#endif

// _declspec is no-op
#define __declspec(x)
#define _declspec(x)

// MSVC-specific struct/member alignment pragmas (ignore)
#define _pragma(x)

// __assume hint is no-op
#define __assume(x) ((void)0)

// Function attributes
#define __cdecl
#define __pascal

// Fix 64-bit type sizes: MSVC Win32 uses 'unsigned long' for 32-bit,
// but on macOS/LP64, 'unsigned long' is 64 bits.
// Redefine the problematic types from bittype.h here:
#undef uint32
#undef sint32
typedef unsigned int    uint32;
typedef signed int      sint32;

// Also fix DWORD if needed (it's defined as unsigned int in windef.h so it's fine)

// __FUNCTION__ is standard in clang, no change needed

// _countof (MSVC macro, define if not present)
#ifndef _countof
#define _countof(arr) (sizeof(arr)/sizeof((arr)[0]))
#endif

// Disable MSVC-specific warning pragmas that clang doesn't understand
// (These are already handled by CMake flags but add here for safety)

// offsetof is in stddef.h
#include <stddef.h>

// min/max via templates - ensure they don't conflict
// (already in always.h)

#endif // CLANGCOMPAT_H
