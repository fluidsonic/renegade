#pragma once

#include "global.h"

// Windows API compatibility shim for macOS/clang

// Prevent multiple conflicting definitions
#define _WINDOWS_

#include "winnt.h"
#include "winbase.h"
#include "winuser.h"
#include "wingdi.h"
#include "mmsystem.h"
#include "winsock.h"

// Undefine macros from system headers that conflict with enum/class member names
// PASS_MAX is defined in <limits.h> as 128 (max password length), conflicts with shader.h enum
#ifdef PASS_MAX
#undef PASS_MAX
#endif
// DEPTH_MAX is sometimes defined in system headers
#ifdef DEPTH_MAX
#undef DEPTH_MAX
#endif
