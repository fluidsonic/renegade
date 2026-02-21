// WTYPES.H compat stub for macOS/clang — Windows COM/OLE types
#pragma once
#ifndef WTYPES_H_COMPAT
#define WTYPES_H_COMPAT

#include "winbase.h"  // Includes SYSTEMTIME, basic types

// OLE/COM character types
typedef wchar_t OLECHAR;
typedef OLECHAR *LPOLESTR;
typedef const OLECHAR *LPCOLESTR;
typedef OLECHAR *BSTR;

// VARIANT_BOOL
typedef short VARIANT_BOOL;
#define VARIANT_TRUE  ((VARIANT_BOOL)-1)
#define VARIANT_FALSE ((VARIANT_BOOL)0)

// FILETIME (64-bit Windows time) — guard against redefinition
#ifndef _FILETIME_
#define _FILETIME_
typedef struct _FILETIME {
    DWORD dwLowDateTime;
    DWORD dwHighDateTime;
} FILETIME, *PFILETIME, *LPFILETIME;
#endif

// DATE type
typedef double DATE;

#endif // WTYPES_H_COMPAT
