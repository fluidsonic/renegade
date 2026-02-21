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
#define EM_GETFIRSTVISIBLELINE (WM_USER+30)
#define EM_LINESCROLL   (WM_USER+6)

// ListView styles
#define LVS_ICON            0x0000
#define LVS_REPORT          0x0001
#define LVS_SMALLICON       0x0002
#define LVS_LIST            0x0003
#define LVS_TYPEMASK        0x0003
#define LVS_SINGLESEL       0x0004
#define LVS_SHOWSELALWAYS   0x0008
#define LVS_SORTASCENDING   0x0010
#define LVS_SORTDESCENDING  0x0020
#define LVS_SHAREIMAGELISTS 0x0040
#define LVS_NOLABELWRAP     0x0080
#define LVS_AUTOARRANGE     0x0100
#define LVS_EDITLABELS      0x0200
#define LVS_OWNERDATA       0x1000
#define LVS_NOSCROLL        0x2000
#define LVS_TYPESTYLEMASK   0xfc00
#define LVS_ALIGNTOP        0x0000
#define LVS_ALIGNLEFT       0x0800
#define LVS_ALIGNMASK       0x0c00
#define LVS_OWNERDRAWFIXED  0x0400
#define LVS_NOCOLUMNHEADER  0x4000
#define LVS_NOSORTHEADER    0x8000

// Progress bar messages
#define PBM_SETRANGE        (WM_USER+1)
#define PBM_SETPOS          (WM_USER+2)
#define PBM_DELTAPOS        (WM_USER+3)
#define PBM_SETSTEP         (WM_USER+4)
#define PBM_STEPIT          (WM_USER+5)
#define PBM_SETRANGE32      (WM_USER+6)
#define PBM_GETRANGE        (WM_USER+7)
#define PBM_GETPOS          (WM_USER+8)
#define PBM_SETBARCOLOR     (WM_USER+9)
#define PBM_SETBKCOLOR      (WM_USER+10)
#define PBS_SMOOTH          0x01
#define PBS_VERTICAL        0x04

#endif // COMMCTRL_H_COMPAT
