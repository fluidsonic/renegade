// except.h compat shim for macOS - missing from GPL release
// This header was not included in the EA GPL release.
// Provides stub implementations for exception handling utilities.
#pragma once
#ifndef EXCEPT_H_COMPAT
#define EXCEPT_H_COMPAT

#include "windef.h"
#include <signal.h>
#include <setjmp.h>

// Stub: Is the application trying to exit due to an exception?
inline bool Is_Trying_To_Exit() { return false; }

// Stub: Structured exception filter
inline int Exception_Filter(unsigned int code, void* ep) { return 1; }

// Stub: Get exception info string
inline const char* Get_Exception_String(unsigned int code) { return "unknown exception"; }

// Stub: Crash dump utilities
inline void Write_Mini_Dump() {}
inline void Set_Exception_Handler() {}

// Structured exception handling macros (simplified)
#ifndef __try
#define __try       if(1)
#define __except(x) else if(0)
#define __finally   // no-op
#endif

#endif // EXCEPT_H_COMPAT
