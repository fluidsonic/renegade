#pragma once

#include "global.h"

// DataSafe system removed — replaced with plain types.
// The encryption / anti-cheat machinery was written for 32-bit Windows and
// incompatible with 64-bit ARM64 (unsigned long is 8 bytes, not 4).
// For the macOS port we use plain primitives everywhere.

// Safe type aliases — now just the underlying plain types.
#define safe_int            int
#define safe_unsigned_int   unsigned int
#define safe_long           int
#define safe_unsigned_long  unsigned int
#define safe_float          float
#define safe_double         double

typedef unsigned int SafeArmorType;
typedef unsigned int SafeWarheadType;

// ds_assert used internally — map to nothing.
#define ds_assert(x) ((void)0)

// Stub GenericDataSafeClass so callers in combat.cpp / combatgmode.cpp /
// gameinitmgr.cpp / consolefunction.cpp keep compiling without changes.
class GenericDataSafeClass {
public:
    static void Reset(void) {}
    static void Reset_Timers(void) {}
    static void Shutdown(void) {}
    static void Set_Preferred_Thread(unsigned int) {}
    static void Dump_Safe_Stats(char *buf, int size) { if (buf && size > 0) buf[0] = '\0'; }
    static void Security_Check(void) {}
    static void Say_Security_Fault(void) {}
};
