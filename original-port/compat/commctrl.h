// commctrl.h compat shim for macOS - Common Controls stubs
#pragma once
#ifndef COMMCTRL_H_COMPAT
#define COMMCTRL_H_COMPAT

#include "windef.h"
#include "winuser.h"

// InitCommonControls stub
inline void InitCommonControls() {}

// Common control styles
#define ICC_WIN95_CLASSES   0x000000FF
#define ICC_COOL_CLASSES    0x00000400
#define ICC_INTERNET_CLASSES 0x00000800

typedef struct _INITCOMMONCONTROLSEX {
    DWORD dwSize;
    DWORD dwICC;
} INITCOMMONCONTROLSEX, *LPINITCOMMONCONTROLSEX;

inline BOOL InitCommonControlsEx(LPINITCOMMONCONTROLSEX p) { return TRUE; }

// Trackbar (slider) control
#define TRACKBAR_CLASS "msctls_trackbar32"
#define TBS_HORZ    0x0000
#define TBS_VERT    0x0002
#define TBS_AUTOTICKS 0x0001
#define TBM_SETRANGE (WM_USER+6)
#define TBM_SETPOS   (WM_USER+5)
#define TBM_GETPOS   (WM_USER+0)

// Tooltip control
#define TOOLTIPS_CLASS "tooltips_class32"
#define TTM_ADDTOOL (WM_USER+4)

// ListView control
#define WC_LISTVIEW "SysListView32"

// Edit control constants
#define EM_SETLIMITTEXT (WM_USER+21)
#define EM_GETLIMITTEXT (WM_USER+37)
#define EM_SETREADONLY  (WM_USER+31)

#endif // COMMCTRL_H_COMPAT
