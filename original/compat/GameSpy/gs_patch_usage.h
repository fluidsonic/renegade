// GameSpy/gs_patch_usage.h compat stub for macOS - GameSpy patching (not used)
#pragma once
#ifndef GS_PATCH_USAGE_H
#define GS_PATCH_USAGE_H

// Stub - GameSpy patch functionality not available
inline int gsPatchQuery(void) { return 0; }
// ptTrackUsage(int type, int prodid, const char* build, int sku, bool beta)
inline void ptTrackUsage(int type, int prodid, const char* build, int sku, bool beta) {}

#endif // GS_PATCH_USAGE_H
