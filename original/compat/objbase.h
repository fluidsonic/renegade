// objbase.h - COM base stub for macOS port
#pragma once
#ifndef OBJBASE_H_COMPAT
#define OBJBASE_H_COMPAT

#include "windows.h"
#include <stdlib.h>

// COM init flags
#define COINIT_MULTITHREADED      0x0
#define COINIT_APARTMENTTHREADED  0x2
#define COINIT_DISABLE_OLE1DDE    0x4
#define COINIT_SPEED_OVER_MEMORY  0x8

typedef LONG HRESULT;

#define S_OK        ((HRESULT)0L)
#define S_FALSE     ((HRESULT)1L)
#define E_NOTIMPL   ((HRESULT)0x80004001L)
#define E_NOINTERFACE ((HRESULT)0x80004002L)
#define E_POINTER   ((HRESULT)0x80004003L)
#define E_FAIL      ((HRESULT)0x80004005L)
#define CO_E_NOTINITIALIZED ((HRESULT)0x800401F0L)

#define SUCCEEDED(hr) ((HRESULT)(hr) >= 0)
#define FAILED(hr)    ((HRESULT)(hr) < 0)

inline HRESULT CoInitialize(void* pvReserved) { return S_OK; }
inline HRESULT CoInitializeEx(void* pvReserved, DWORD dwCoInit) { return S_OK; }
inline void    CoUninitialize(void) {}

#endif // OBJBASE_H_COMPAT
