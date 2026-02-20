// winnt.h compat shim for macOS/clang
#pragma once
#ifndef WINNT_H_COMPAT
#define WINNT_H_COMPAT

#include "windef.h"
#include <pthread.h>

// Integer types with explicit sizes
typedef signed char         INT8;
typedef unsigned char       UINT8;
typedef signed short        INT16;
typedef unsigned short      UINT16;
typedef signed int          INT32;
typedef unsigned int        UINT32;
typedef signed long long    INT64;
typedef unsigned long long  UINT64;

typedef long long           LONGLONG;
typedef unsigned long long  ULONGLONG;

// LARGE_INTEGER union
typedef union _LARGE_INTEGER {
    struct {
        DWORD LowPart;
        LONG  HighPart;
    };
    struct {
        DWORD LowPart;
        LONG  HighPart;
    } u;
    LONGLONG QuadPart;
} LARGE_INTEGER, *PLARGE_INTEGER;

typedef union _ULARGE_INTEGER {
    struct {
        DWORD LowPart;
        DWORD HighPart;
    };
    struct {
        DWORD LowPart;
        DWORD HighPart;
    } u;
    ULONGLONG QuadPart;
} ULARGE_INTEGER, *PULARGE_INTEGER;

// CRITICAL_SECTION wraps pthread_mutex_t
typedef struct _CRITICAL_SECTION {
    pthread_mutex_t mutex;
    pthread_mutexattr_t attr;
    int lock_count;
} CRITICAL_SECTION, *LPCRITICAL_SECTION;

inline void InitializeCriticalSection(LPCRITICAL_SECTION cs) {
    pthread_mutexattr_init(&cs->attr);
    pthread_mutexattr_settype(&cs->attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&cs->mutex, &cs->attr);
    cs->lock_count = 0;
}

inline void DeleteCriticalSection(LPCRITICAL_SECTION cs) {
    pthread_mutex_destroy(&cs->mutex);
    pthread_mutexattr_destroy(&cs->attr);
}

inline void EnterCriticalSection(LPCRITICAL_SECTION cs) {
    pthread_mutex_lock(&cs->mutex);
    cs->lock_count++;
}

inline void LeaveCriticalSection(LPCRITICAL_SECTION cs) {
    cs->lock_count--;
    pthread_mutex_unlock(&cs->mutex);
}

inline BOOL TryEnterCriticalSection(LPCRITICAL_SECTION cs) {
    return pthread_mutex_trylock(&cs->mutex) == 0;
}

// WCHAR as uint16_t to match Windows 2-byte size
typedef uint16_t WCHAR;
typedef WCHAR*   PWCHAR;
typedef WCHAR*   LPWCH;
typedef WCHAR*   PWCH;
typedef const WCHAR* LPCWCH;
typedef const WCHAR* PCWCH;
typedef WCHAR*   LPWSTR;
typedef const WCHAR* LPCWSTR;

// Unicode string macros
#define UNICODE_NULL ((WCHAR)0)

// Status codes
#define STATUS_WAIT_0                   ((DWORD)0x00000000L)
#define WAIT_OBJECT_0                   ((STATUS_WAIT_0) + 0)
#define WAIT_TIMEOUT                    ((DWORD)0x00000102L)
#define WAIT_FAILED                     ((DWORD)0xFFFFFFFF)
#define WAIT_ABANDONED                  ((DWORD)0x00000080L)

#endif // WINNT_H_COMPAT
