// Minimal afxres.h stub for llvm-rc compilation on macOS
// Provides Windows constants normally built into rc.exe / winuser.h / winnt.h
#ifndef AFXRES_H
#define AFXRES_H

// Standard dialog button IDs
#ifndef IDOK
#define IDOK        1
#endif
#ifndef IDCANCEL
#define IDCANCEL    2
#endif
#ifndef IDABORT
#define IDABORT     3
#endif
#ifndef IDRETRY
#define IDRETRY     4
#endif
#ifndef IDIGNORE
#define IDIGNORE    5
#endif
#ifndef IDYES
#define IDYES       6
#endif
#ifndef IDNO
#define IDNO        7
#endif
#ifndef IDC_STATIC
#define IDC_STATIC  (-1)
#endif

// Language / sublanguage (winnt.h)
#ifndef LANG_ENGLISH
#define LANG_ENGLISH        0x09
#endif
#ifndef SUBLANG_ENGLISH_US
#define SUBLANG_ENGLISH_US  0x01
#endif

// Virtual key codes (winuser.h)
#ifndef VK_RETURN
#define VK_RETURN   0x0D
#endif
#ifndef VK_ESCAPE
#define VK_ESCAPE   0x1B
#endif
#ifndef VK_TAB
#define VK_TAB      0x09
#endif
#ifndef VK_DELETE
#define VK_DELETE   0x2E
#endif

// Window styles (WS_)
#ifndef WS_POPUP
#define WS_POPUP            0x80000000
#endif
#ifndef WS_CHILD
#define WS_CHILD            0x40000000
#endif
#ifndef WS_CLIPSIBLINGS
#define WS_CLIPSIBLINGS     0x04000000
#endif
#ifndef WS_CLIPCHILDREN
#define WS_CLIPCHILDREN     0x02000000
#endif
#ifndef WS_VISIBLE
#define WS_VISIBLE          0x10000000
#endif
#ifndef WS_DISABLED
#define WS_DISABLED         0x08000000
#endif
#ifndef WS_CAPTION
#define WS_CAPTION          0x00C00000
#endif
#ifndef WS_BORDER
#define WS_BORDER           0x00800000
#endif
#ifndef WS_SYSMENU
#define WS_SYSMENU          0x00080000
#endif
#ifndef WS_HSCROLL
#define WS_HSCROLL          0x00100000
#endif
#ifndef WS_VSCROLL
#define WS_VSCROLL          0x00200000
#endif
#ifndef WS_TABSTOP
#define WS_TABSTOP          0x00010000
#endif
#ifndef WS_GROUP
#define WS_GROUP            0x00020000
#endif
#ifndef WS_MINIMIZEBOX
#define WS_MINIMIZEBOX      0x00020000
#endif
#ifndef WS_MAXIMIZEBOX
#define WS_MAXIMIZEBOX      0x00010000
#endif
#ifndef WS_THICKFRAME
#define WS_THICKFRAME       0x00040000
#endif

// Dialog styles (DS_)
#ifndef DS_MODALFRAME
#define DS_MODALFRAME       0x00000080
#endif
#ifndef DS_SETFONT
#define DS_SETFONT          0x00000040
#endif
#ifndef DS_CENTER
#define DS_CENTER           0x00000800
#endif
#ifndef DS_ABSALIGN
#define DS_ABSALIGN         0x00000001
#endif
#ifndef DS_SYSMODAL
#define DS_SYSMODAL         0x00000002
#endif
#ifndef DS_3DLOOK
#define DS_3DLOOK           0x00000004
#endif
#ifndef DS_FIXEDSYS
#define DS_FIXEDSYS         0x00000008
#endif
#ifndef DS_NOFAILCREATE
#define DS_NOFAILCREATE     0x00000010
#endif
#ifndef DS_CONTROL
#define DS_CONTROL          0x00000400
#endif
#ifndef DS_CENTERMOUSE
#define DS_CENTERMOUSE      0x00001000
#endif

// Button styles (BS_)
#ifndef BS_PUSHBUTTON
#define BS_PUSHBUTTON       0x00000000
#endif
#ifndef BS_DEFPUSHBUTTON
#define BS_DEFPUSHBUTTON    0x00000001
#endif
#ifndef BS_CHECKBOX
#define BS_CHECKBOX         0x00000002
#endif
#ifndef BS_AUTOCHECKBOX
#define BS_AUTOCHECKBOX     0x00000003
#endif
#ifndef BS_RADIOBUTTON
#define BS_RADIOBUTTON      0x00000004
#endif
#ifndef BS_3STATE
#define BS_3STATE           0x00000005
#endif
#ifndef BS_AUTO3STATE
#define BS_AUTO3STATE       0x00000006
#endif
#ifndef BS_GROUPBOX
#define BS_GROUPBOX         0x00000007
#endif
#ifndef BS_USERBUTTON
#define BS_USERBUTTON       0x00000008
#endif
#ifndef BS_AUTORADIOBUTTON
#define BS_AUTORADIOBUTTON  0x00000009
#endif
#ifndef BS_OWNERDRAW
#define BS_OWNERDRAW        0x0000000B
#endif
#ifndef BS_LEFT
#define BS_LEFT             0x00000100
#endif
#ifndef BS_RIGHT
#define BS_RIGHT            0x00000200
#endif
#ifndef BS_CENTER
#define BS_CENTER           0x00000300
#endif
#ifndef BS_TOP
#define BS_TOP              0x00000400
#endif
#ifndef BS_BOTTOM
#define BS_BOTTOM           0x00000800
#endif
#ifndef BS_VCENTER
#define BS_VCENTER          0x00000C00
#endif
#ifndef BS_PUSHLIKE
#define BS_PUSHLIKE         0x00001000
#endif
#ifndef BS_MULTILINE
#define BS_MULTILINE        0x00002000
#endif
#ifndef BS_NOTIFY
#define BS_NOTIFY           0x00004000
#endif
#ifndef BS_FLAT
#define BS_FLAT             0x00008000
#endif
#ifndef BS_ICON
#define BS_ICON             0x00000040
#endif
#ifndef BS_BITMAP
#define BS_BITMAP           0x00000080
#endif
#ifndef BS_TEXT
#define BS_TEXT             0x00000000
#endif

// Edit styles (ES_)
#ifndef ES_LEFT
#define ES_LEFT             0x00000000
#endif
#ifndef ES_CENTER
#define ES_CENTER           0x00000001
#endif
#ifndef ES_RIGHT
#define ES_RIGHT            0x00000002
#endif
#ifndef ES_MULTILINE
#define ES_MULTILINE        0x00000004
#endif
#ifndef ES_UPPERCASE
#define ES_UPPERCASE        0x00000008
#endif
#ifndef ES_LOWERCASE
#define ES_LOWERCASE        0x00000010
#endif
#ifndef ES_PASSWORD
#define ES_PASSWORD         0x00000020
#endif
#ifndef ES_AUTOVSCROLL
#define ES_AUTOVSCROLL      0x00000040
#endif
#ifndef ES_AUTOHSCROLL
#define ES_AUTOHSCROLL      0x00000080
#endif
#ifndef ES_NOHIDESEL
#define ES_NOHIDESEL        0x00000100
#endif
#ifndef ES_OEMCONVERT
#define ES_OEMCONVERT       0x00000400
#endif
#ifndef ES_READONLY
#define ES_READONLY         0x00000800
#endif
#ifndef ES_WANTRETURN
#define ES_WANTRETURN       0x00001000
#endif
#ifndef ES_NUMBER
#define ES_NUMBER           0x00002000
#endif

// Static styles (SS_)
#ifndef SS_LEFT
#define SS_LEFT             0x00000000
#endif
#ifndef SS_CENTER
#define SS_CENTER           0x00000001
#endif
#ifndef SS_RIGHT
#define SS_RIGHT            0x00000002
#endif
#ifndef SS_ICON
#define SS_ICON             0x00000003
#endif
#ifndef SS_BLACKRECT
#define SS_BLACKRECT        0x00000004
#endif
#ifndef SS_GRAYRECT
#define SS_GRAYRECT         0x00000005
#endif
#ifndef SS_WHITERECT
#define SS_WHITERECT        0x00000006
#endif
#ifndef SS_BLACKFRAME
#define SS_BLACKFRAME       0x00000007
#endif
#ifndef SS_GRAYFRAME
#define SS_GRAYFRAME        0x00000008
#endif
#ifndef SS_WHITEFRAME
#define SS_WHITEFRAME       0x00000009
#endif
#ifndef SS_SIMPLE
#define SS_SIMPLE           0x0000000B
#endif
#ifndef SS_LEFTNOWORDWRAP
#define SS_LEFTNOWORDWRAP   0x0000000C
#endif
#ifndef SS_BITMAP
#define SS_BITMAP           0x0000000E
#endif
#ifndef SS_ETCHEDHORZ
#define SS_ETCHEDHORZ       0x00000010
#endif
#ifndef SS_ETCHEDVERT
#define SS_ETCHEDVERT       0x00000011
#endif
#ifndef SS_ETCHEDFRAME
#define SS_ETCHEDFRAME      0x00000012
#endif
#ifndef SS_NOPREFIX
#define SS_NOPREFIX         0x00000080
#endif
#ifndef SS_NOTIFY
#define SS_NOTIFY           0x00000100
#endif
#ifndef SS_CENTERIMAGE
#define SS_CENTERIMAGE      0x00000200
#endif
#ifndef SS_RIGHTJUST
#define SS_RIGHTJUST        0x00000400
#endif
#ifndef SS_REALSIZEIMAGE
#define SS_REALSIZEIMAGE    0x00000800
#endif
#ifndef SS_SUNKEN
#define SS_SUNKEN           0x00001000
#endif
#ifndef SS_TYPEMASK
#define SS_TYPEMASK         0x0000001F
#endif

// List box styles (LBS_)
#ifndef LBS_NOTIFY
#define LBS_NOTIFY          0x0001
#endif
#ifndef LBS_SORT
#define LBS_SORT            0x0002
#endif
#ifndef LBS_NOREDRAW
#define LBS_NOREDRAW        0x0004
#endif
#ifndef LBS_MULTIPLESEL
#define LBS_MULTIPLESEL     0x0008
#endif
#ifndef LBS_HASSTRINGS
#define LBS_HASSTRINGS      0x0040
#endif
#ifndef LBS_USETABSTOPS
#define LBS_USETABSTOPS     0x0080
#endif
#ifndef LBS_NOINTEGRALHEIGHT
#define LBS_NOINTEGRALHEIGHT 0x0100
#endif
#ifndef LBS_MULTICOLUMN
#define LBS_MULTICOLUMN     0x0200
#endif
#ifndef LBS_WANTKEYBOARDINPUT
#define LBS_WANTKEYBOARDINPUT 0x0400
#endif
#ifndef LBS_EXTENDEDSEL
#define LBS_EXTENDEDSEL     0x0800
#endif
#ifndef LBS_DISABLENOSCROLL
#define LBS_DISABLENOSCROLL 0x1000
#endif
#ifndef LBS_NODATA
#define LBS_NODATA          0x2000
#endif
#ifndef LBS_NOSEL
#define LBS_NOSEL           0x4000
#endif
#ifndef LBS_OWNERDRAWFIXED
#define LBS_OWNERDRAWFIXED  0x0010
#endif
#ifndef LBS_OWNERDRAWVARIABLE
#define LBS_OWNERDRAWVARIABLE 0x0020
#endif
#ifndef LBS_STANDARD
#define LBS_STANDARD        (LBS_NOTIFY | LBS_SORT | WS_VSCROLL | WS_BORDER)
#endif

// Combo box styles (CBS_)
#ifndef CBS_SIMPLE
#define CBS_SIMPLE          0x0001
#endif
#ifndef CBS_DROPDOWN
#define CBS_DROPDOWN        0x0002
#endif
#ifndef CBS_DROPDOWNLIST
#define CBS_DROPDOWNLIST    0x0003
#endif
#ifndef CBS_OWNERDRAWFIXED
#define CBS_OWNERDRAWFIXED  0x0010
#endif
#ifndef CBS_OWNERDRAWVARIABLE
#define CBS_OWNERDRAWVARIABLE 0x0020
#endif
#ifndef CBS_AUTOHSCROLL
#define CBS_AUTOHSCROLL     0x0040
#endif
#ifndef CBS_OEMCONVERT
#define CBS_OEMCONVERT      0x0080
#endif
#ifndef CBS_SORT
#define CBS_SORT            0x0100
#endif
#ifndef CBS_HASSTRINGS
#define CBS_HASSTRINGS      0x0200
#endif
#ifndef CBS_NOINTEGRALHEIGHT
#define CBS_NOINTEGRALHEIGHT 0x0400
#endif
#ifndef CBS_DISABLENOSCROLL
#define CBS_DISABLENOSCROLL 0x0800
#endif
#ifndef CBS_UPPERCASE
#define CBS_UPPERCASE       0x2000
#endif
#ifndef CBS_LOWERCASE
#define CBS_LOWERCASE       0x4000
#endif

// Trackbar styles (TBS_)
#ifndef TBS_AUTOTICKS
#define TBS_AUTOTICKS       0x0001
#endif
#ifndef TBS_VERT
#define TBS_VERT            0x0002
#endif
#ifndef TBS_HORZ
#define TBS_HORZ            0x0000
#endif
#ifndef TBS_TOP
#define TBS_TOP             0x0004
#endif
#ifndef TBS_BOTTOM
#define TBS_BOTTOM          0x0000
#endif
#ifndef TBS_LEFT
#define TBS_LEFT            0x0004
#endif
#ifndef TBS_RIGHT
#define TBS_RIGHT           0x0000
#endif
#ifndef TBS_BOTH
#define TBS_BOTH            0x0008
#endif
#ifndef TBS_NOTICKS
#define TBS_NOTICKS         0x0010
#endif
#ifndef TBS_ENABLESELRANGE
#define TBS_ENABLESELRANGE  0x0020
#endif
#ifndef TBS_FIXEDLENGTH
#define TBS_FIXEDLENGTH     0x0040
#endif
#ifndef TBS_NOTHUMB
#define TBS_NOTHUMB         0x0080
#endif
#ifndef TBS_TOOLTIPS
#define TBS_TOOLTIPS        0x0100
#endif
#ifndef TBS_REVERSED
#define TBS_REVERSED        0x0200
#endif

// List view styles (LVS_)
#ifndef LVS_ICON
#define LVS_ICON            0x0000
#endif
#ifndef LVS_REPORT
#define LVS_REPORT          0x0001
#endif
#ifndef LVS_SMALLICON
#define LVS_SMALLICON       0x0002
#endif
#ifndef LVS_LIST
#define LVS_LIST            0x0003
#endif
#ifndef LVS_TYPEMASK
#define LVS_TYPEMASK        0x0003
#endif
#ifndef LVS_SINGLESEL
#define LVS_SINGLESEL       0x0004
#endif
#ifndef LVS_SHOWSELALWAYS
#define LVS_SHOWSELALWAYS   0x0008
#endif
#ifndef LVS_SORTASCENDING
#define LVS_SORTASCENDING   0x0010
#endif
#ifndef LVS_SORTDESCENDING
#define LVS_SORTDESCENDING  0x0020
#endif
#ifndef LVS_SHAREIMAGELISTS
#define LVS_SHAREIMAGELISTS 0x0040
#endif
#ifndef LVS_NOLABELWRAP
#define LVS_NOLABELWRAP     0x0080
#endif
#ifndef LVS_AUTOARRANGE
#define LVS_AUTOARRANGE     0x0100
#endif
#ifndef LVS_EDITLABELS
#define LVS_EDITLABELS      0x0200
#endif
#ifndef LVS_OWNERDATA
#define LVS_OWNERDATA       0x1000
#endif
#ifndef LVS_NOSCROLL
#define LVS_NOSCROLL        0x2000
#endif
#ifndef LVS_ALIGNTOP
#define LVS_ALIGNTOP        0x0000
#endif
#ifndef LVS_ALIGNLEFT
#define LVS_ALIGNLEFT       0x0800
#endif
#ifndef LVS_OWNERDRAWFIXED
#define LVS_OWNERDRAWFIXED  0x0400
#endif
#ifndef LVS_NOCOLUMNHEADER
#define LVS_NOCOLUMNHEADER  0x4000
#endif
#ifndef LVS_NOSORTHEADER
#define LVS_NOSORTHEADER    0x8000
#endif

// Tab control styles (TCS_)
#ifndef TCS_TABS
#define TCS_TABS            0x0000
#endif
#ifndef TCS_BUTTONS
#define TCS_BUTTONS         0x0100
#endif
#ifndef TCS_SINGLELINE
#define TCS_SINGLELINE      0x0000
#endif
#ifndef TCS_MULTILINE
#define TCS_MULTILINE       0x0200
#endif
#ifndef TCS_RIGHTJUSTIFY
#define TCS_RIGHTJUSTIFY    0x0000
#endif
#ifndef TCS_FIXEDWIDTH
#define TCS_FIXEDWIDTH      0x0400
#endif
#ifndef TCS_RAGGEDRIGHT
#define TCS_RAGGEDRIGHT     0x0800
#endif
#ifndef TCS_FOCUSONBUTTONDOWN
#define TCS_FOCUSONBUTTONDOWN 0x1000
#endif
#ifndef TCS_OWNERDRAWFIXED
#define TCS_OWNERDRAWFIXED  0x2000
#endif
#ifndef TCS_TOOLTIPS
#define TCS_TOOLTIPS        0x4000
#endif
#ifndef TCS_FOCUSNEVER
#define TCS_FOCUSNEVER      0x8000
#endif
#ifndef TCS_BOTTOM
#define TCS_BOTTOM          0x0002
#endif
#ifndef TCS_RIGHT
#define TCS_RIGHT           0x0002
#endif
#ifndef TCS_FLATBUTTONS
#define TCS_FLATBUTTONS     0x0008
#endif
#ifndef TCS_FORCELABELLEFT
#define TCS_FORCELABELLEFT  0x0020
#endif
#ifndef TCS_HOTTRACK
#define TCS_HOTTRACK        0x0040
#endif
#ifndef TCS_VERTICAL
#define TCS_VERTICAL        0x0080
#endif

// Extended window styles (WS_EX_)
#ifndef WS_EX_STATICEDGE
#define WS_EX_STATICEDGE    0x00020000
#endif
#ifndef WS_EX_CLIENTEDGE
#define WS_EX_CLIENTEDGE    0x00000200
#endif
#ifndef WS_EX_WINDOWEDGE
#define WS_EX_WINDOWEDGE    0x00000100
#endif
#ifndef WS_EX_DLGMODALFRAME
#define WS_EX_DLGMODALFRAME 0x00000001
#endif

// Scroll bar styles (SBS_)
#ifndef SBS_HORZ
#define SBS_HORZ            0x0000
#endif
#ifndef SBS_VERT
#define SBS_VERT            0x0001
#endif

// Progress bar styles (PBS_)
#ifndef PBS_SMOOTH
#define PBS_SMOOTH          0x01
#endif
#ifndef PBS_VERTICAL
#define PBS_VERTICAL        0x04
#endif

#endif // AFXRES_H
