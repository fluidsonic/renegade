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

// WCHAR as wchar_t so L"..." literals are type-compatible (compile-only goal;
// runtime binary layout differs from Windows 2-byte wchar_t)
#include <wchar.h>
#ifndef WCHAR_DEFINED
#define WCHAR_DEFINED
typedef wchar_t  WCHAR;
typedef WCHAR*   PWSTR;
typedef WCHAR*   LPWSTR;
typedef const WCHAR* PCWSTR;
typedef const WCHAR* LPCWSTR;
#endif
typedef WCHAR*   PWCHAR;
typedef WCHAR*   LPWCH;
typedef WCHAR*   PWCH;
typedef const WCHAR* LPCWCH;
typedef const WCHAR* PCWCH;

// Unicode string macros
#define UNICODE_NULL ((WCHAR)0)

// Status codes
#define STATUS_WAIT_0                   ((DWORD)0x00000000L)
#define WAIT_OBJECT_0                   ((STATUS_WAIT_0) + 0)
#define WAIT_TIMEOUT                    ((DWORD)0x00000102L)
#define WAIT_FAILED                     ((DWORD)0xFFFFFFFF)
#define WAIT_ABANDONED                  ((DWORD)0x00000080L)

// PE image types (Windows Portable Executable format)
typedef struct _IMAGE_FILE_HEADER {
    WORD    Machine;
    WORD    NumberOfSections;
    DWORD   TimeDateStamp;
    DWORD   PointerToSymbolTable;
    DWORD   NumberOfSymbols;
    WORD    SizeOfOptionalHeader;
    WORD    Characteristics;
} IMAGE_FILE_HEADER, *PIMAGE_FILE_HEADER;

#define IMAGE_SIZEOF_FILE_HEADER    20
#define IMAGE_FILE_MACHINE_I386     0x014c

typedef struct _IMAGE_DATA_DIRECTORY {
    DWORD   VirtualAddress;
    DWORD   Size;
} IMAGE_DATA_DIRECTORY, *PIMAGE_DATA_DIRECTORY;

#define IMAGE_NUMBEROF_DIRECTORY_ENTRIES 16

typedef struct _IMAGE_OPTIONAL_HEADER {
    WORD    Magic;
    BYTE    MajorLinkerVersion;
    BYTE    MinorLinkerVersion;
    DWORD   SizeOfCode;
    DWORD   SizeOfInitializedData;
    DWORD   SizeOfUninitializedData;
    DWORD   AddressOfEntryPoint;
    DWORD   BaseOfCode;
    DWORD   BaseOfData;
    DWORD   ImageBase;
    DWORD   SectionAlignment;
    DWORD   FileAlignment;
    WORD    MajorOperatingSystemVersion;
    WORD    MinorOperatingSystemVersion;
    WORD    MajorImageVersion;
    WORD    MinorImageVersion;
    WORD    MajorSubsystemVersion;
    WORD    MinorSubsystemVersion;
    DWORD   Win32VersionValue;
    DWORD   SizeOfImage;
    DWORD   SizeOfHeaders;
    DWORD   CheckSum;
    WORD    Subsystem;
    WORD    DllCharacteristics;
    DWORD   SizeOfStackReserve;
    DWORD   SizeOfStackCommit;
    DWORD   SizeOfHeapReserve;
    DWORD   SizeOfHeapCommit;
    DWORD   LoaderFlags;
    DWORD   NumberOfRvaAndSizes;
    IMAGE_DATA_DIRECTORY DataDirectory[IMAGE_NUMBEROF_DIRECTORY_ENTRIES];
} IMAGE_OPTIONAL_HEADER, *PIMAGE_OPTIONAL_HEADER;

typedef struct _IMAGE_NT_HEADERS {
    DWORD Signature;
    IMAGE_FILE_HEADER FileHeader;
    IMAGE_OPTIONAL_HEADER OptionalHeader;
} IMAGE_NT_HEADERS, *PIMAGE_NT_HEADERS;

typedef struct _IMAGE_DOS_HEADER {
    WORD e_magic;
    WORD e_cblp;
    WORD e_cp;
    WORD e_crnc;
    WORD e_cparhdr;
    WORD e_minalloc;
    WORD e_maxalloc;
    WORD e_ss;
    WORD e_sp;
    WORD e_csum;
    WORD e_ip;
    WORD e_cs;
    WORD e_lfarlc;
    WORD e_ovno;
    WORD e_res[4];
    WORD e_oemid;
    WORD e_oeminfo;
    WORD e_res2[10];
    LONG e_lfanew;
} IMAGE_DOS_HEADER, *PIMAGE_DOS_HEADER;

#define IMAGE_DOS_SIGNATURE  0x5A4D      // MZ
#define IMAGE_NT_SIGNATURE   0x00004550  // PE\0\0

#endif // WINNT_H_COMPAT
