// imagehlp.h compat stub for macOS/clang
// Windows Image Help Library — not available on macOS; stack walk functions are no-ops
#pragma once
#ifndef IMAGEHLP_H_COMPAT
#define IMAGEHLP_H_COMPAT

#include "windef.h"
#include "winbase.h"

// Type aliases needed by imagehlp
#ifndef PSTR
typedef char* PSTR;
#endif
#ifndef PDWORD
typedef DWORD* PDWORD;
#endif

// Stack walking types (no-ops on macOS)
typedef struct _IMAGEHLP_SYMBOL {
    DWORD  SizeOfStruct;
    DWORD  Address;
    DWORD  Size;
    DWORD  Flags;
    DWORD  MaxNameLength;
    CHAR   Name[1];
} IMAGEHLP_SYMBOL, *PIMAGEHLP_SYMBOL;

typedef struct _IMAGEHLP_LINE {
    DWORD SizeOfStruct;
    PVOID Key;
    DWORD LineNumber;
    PSTR  FileName;
    DWORD Address;
} IMAGEHLP_LINE, *PIMAGEHLP_LINE;

typedef struct _IMAGEHLP_MODULE {
    DWORD SizeOfStruct;
    DWORD BaseOfImage;
    DWORD ImageSize;
    DWORD TimeDateStamp;
    DWORD CheckSum;
    DWORD NumSyms;
    int   SymType;
    CHAR  ModuleName[32];
    CHAR  ImageName[256];
    CHAR  LoadedImageName[256];
} IMAGEHLP_MODULE, *PIMAGEHLP_MODULE;

// No-op stubs
inline BOOL SymInitialize(HANDLE, PSTR, BOOL) { return FALSE; }
inline BOOL SymCleanup(HANDLE) { return FALSE; }
inline BOOL SymGetSymFromAddr(HANDLE, DWORD, PDWORD, PIMAGEHLP_SYMBOL) { return FALSE; }
inline BOOL SymGetLineFromAddr(HANDLE, DWORD, PDWORD, PIMAGEHLP_LINE) { return FALSE; }
inline BOOL SymGetModuleInfo(HANDLE, DWORD, PIMAGEHLP_MODULE) { return FALSE; }
inline BOOL StackWalk(DWORD, HANDLE, HANDLE, void*, void*, void*, void*, void*, void*) { return FALSE; }

#endif // IMAGEHLP_H_COMPAT
