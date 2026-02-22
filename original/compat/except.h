#pragma once

#include "global.h"

// except.h compat shim for macOS - missing from GPL release
// This header was not included in the EA GPL release.
// Provides stub implementations for exception handling utilities.

#include <signal.h>
#include <setjmp.h>

// Stub: Is the application trying to exit due to an exception?
// (also defined in clangcompat.h for non-Except.h code paths)
#ifndef IS_TRYING_TO_EXIT_DEFINED
#define IS_TRYING_TO_EXIT_DEFINED
inline bool Is_Trying_To_Exit() { return false; }
#endif

// Stub: Structured exception filter
inline int Exception_Filter(unsigned int code, void* ep) { return 1; }

// Stub: Get exception info string
inline const char* Get_Exception_String(unsigned int code) { return "unknown exception"; }

// Stub: Crash dump utilities
inline void Write_Mini_Dump() {}
inline void Set_Exception_Handler() {}

// Thread tracking stubs (MSVC-only in original Except.h)
// 3-arg version: (thread_id, name, is_main_thread)
inline void Register_Thread_ID(unsigned long thread_id, const char* name, bool is_main = false) {}
inline void Unregister_Thread_ID(unsigned long thread_id, const char* name, bool is_main = false) {}

// Structured exception handling macros (simplified)
#ifndef __try
#define __try       if(1)
#define __except(x) else if(0)
#define __finally   // no-op
#endif
