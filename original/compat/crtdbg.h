// crtdbg.h compat shim for macOS - no-op debug CRT
#pragma once
#ifndef CRTDBG_H_COMPAT
#define CRTDBG_H_COMPAT

// All CRT debug macros are no-ops on non-Windows
#define _ASSERT(expr)           ((void)0)
#define _ASSERTE(expr)          ((void)0)
#define _RPT0(rptno, msg)       ((void)0)
#define _RPT1(rptno, fmt, a)    ((void)0)
#define _RPT2(rptno, fmt, a, b) ((void)0)
#define _NORMAL_BLOCK           1
#define _CRT_WARN               0
#define _CRT_ERROR              1
#define _CRT_ASSERT             2

#define _CrtDbgReport(...)      0
#define _CrtCheckMemory()       1
#define _CrtSetDbgFlag(x)       0
#define _CrtSetReportMode(t,m)  0
#define _CrtDumpMemoryLeaks()   0

#define _CRTDBG_ALLOC_MEM_DF    0x01
#define _CRTDBG_LEAK_CHECK_DF   0x20
#define _CRTDBG_MODE_DEBUG      0x02

#endif // CRTDBG_H_COMPAT
