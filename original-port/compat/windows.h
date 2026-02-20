// Windows API compatibility shim for macOS/clang
#pragma once
#ifndef WINDOWS_H_COMPAT
#define WINDOWS_H_COMPAT

// Prevent multiple conflicting definitions
#define _WINDOWS_

#include "windef.h"
#include "winnt.h"
#include "winbase.h"
#include "winuser.h"
#include "wingdi.h"
#include "mmsystem.h"

#endif // WINDOWS_H_COMPAT
