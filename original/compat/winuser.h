// winuser.h compat shim for macOS/clang
#pragma once
#ifndef WINUSER_H_COMPAT
#define WINUSER_H_COMPAT

#include "windef.h"
#include <stdio.h>
#include <stdarg.h>
#include "sdl2_platform.h"

// Window messages
#define WM_NULL         0x0000
#define WM_CREATE       0x0001
#define WM_DESTROY      0x0002
#define WM_MOVE         0x0003
#define WM_SIZE         0x0005
#define WM_ACTIVATE     0x0006
#define WM_SETFOCUS     0x0007
#define WM_KILLFOCUS    0x0008
#define WM_ENABLE       0x000A
#define WM_PAINT        0x000F
#define WM_CLOSE        0x0010
#define WM_QUIT         0x0012
#define WM_ERASEBKGND   0x0014
#define WM_SHOWWINDOW   0x0018
#define WM_SETTEXT      0x000C
#define WM_GETTEXT      0x000D
#define WM_KEYDOWN      0x0100
#define WM_KEYUP        0x0101
#define WM_CHAR         0x0102
#define WM_SYSKEYDOWN   0x0104
#define WM_SYSKEYUP     0x0105
#define WM_SYSCHAR      0x0106
#define WM_MOUSEMOVE        0x0200
#define WM_LBUTTONDOWN      0x0201
#define WM_LBUTTONUP        0x0202
#define WM_LBUTTONDBLCLK    0x0203
#define WM_RBUTTONDOWN      0x0204
#define WM_RBUTTONUP        0x0205
#define WM_RBUTTONDBLCLK    0x0206
#define WM_MBUTTONDOWN      0x0207
#define WM_MBUTTONUP        0x0208
#define WM_MBUTTONDBLCLK    0x0209
#define WM_MOUSEWHEEL   0x020A
#define WM_TIMER        0x0113
#define WM_ACTIVATEAPP  0x001C
#define WM_SYSCOMMAND   0x0112
#define WM_USER         0x0400
#define WM_APP          0x8000

// System command identifiers
#define SC_SIZE     0xF000
#define SC_MOVE     0xF010
#define SC_MINIMIZE 0xF020
#define SC_MAXIMIZE 0xF030
#define SC_CLOSE    0xF060
#define SC_KEYMENU  0xF100

// Key flags (from WM_SYSKEYDOWN lParam)
#define KF_EXTENDED 0x0100
#define KF_DLGMODE  0x0800
#define KF_MENUMODE 0x1000
#define KF_ALTDOWN  0x2000
#define KF_REPEAT   0x4000
#define KF_UP       0x8000

// Activate message params
#define WA_INACTIVE     0
#define WA_ACTIVE       1
#define WA_CLICKACTIVE  2

// Show window constants
#define SW_HIDE         0
#define SW_SHOW         5
#define SW_SHOWDEFAULT  10
#define SW_SHOWNORMAL   1
#define SW_SHOWMINIMIZED 2
#define SW_SHOWMAXIMIZED 3
#define SW_RESTORE      9
#define SW_MINIMIZE     6

// MessageBox constants
#define MB_OK               0x00000000L
#define MB_OKCANCEL         0x00000001L
#define MB_ABORTRETRYIGNORE 0x00000002L
#define MB_YESNOCANCEL      0x00000003L
#define MB_YESNO            0x00000004L
#define MB_RETRYCANCEL      0x00000005L
#define MB_ICONHAND         0x00000010L
#define MB_ICONQUESTION     0x00000020L
#define MB_ICONEXCLAMATION  0x00000030L
#define MB_ICONASTERISK     0x00000040L
#define MB_ICONINFORMATION  MB_ICONASTERISK
#define MB_ICONWARNING      MB_ICONEXCLAMATION
#define MB_ICONSTOP         MB_ICONHAND
#define MB_ICONERROR        MB_ICONHAND
#define MB_SETFOREGROUND    0x00010000L
#define MB_TASKMODAL        0x00002000L
#define IDABORT  3
#define IDRETRY  4
#define IDIGNORE 5
#define IDYES    6
#define IDNO     7
#define IDOK     1
#define IDCANCEL 2

// Window style extras
#define WS_TABSTOP          0x00010000L
#define WS_GROUP            0x00020000L
#define WS_HSCROLL          0x00100000L
#define WS_VSCROLL          0x00200000L
#define WS_DLGFRAME         0x00400000L
#define WS_TILED            WS_OVERLAPPED
#define WS_ICONIC           WS_MINIMIZE
#define WS_SIZEBOX          WS_THICKFRAME

// Button notification codes
#define BN_CLICKED          0
#define BN_PAINT            1
#define BN_HILITE           2
#define BN_UNHILITE         3
#define BN_DISABLE          4
#define BN_DOUBLECLICKED    5
#define BN_PUSHED           BN_HILITE
#define BN_UNPUSHED         BN_UNHILITE
#define BN_DBLCLK           BN_DOUBLECLICKED
#define BN_SETFOCUS         6
#define BN_KILLFOCUS        7

// Window styles
#define WS_OVERLAPPED       0x00000000L
#define WS_POPUP            0x80000000L
#define WS_CHILD            0x40000000L
#define WS_VISIBLE          0x10000000L
#define WS_DISABLED         0x08000000L
#define WS_CAPTION          0x00C00000L
#define WS_BORDER           0x00800000L
#define WS_SYSMENU          0x00080000L
#define WS_THICKFRAME       0x00040000L
#define WS_MINIMIZEBOX      0x00020000L
#define WS_MAXIMIZEBOX      0x00010000L
#define WS_OVERLAPPEDWINDOW (WS_OVERLAPPED|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MINIMIZEBOX|WS_MAXIMIZEBOX)
#define WS_CLIPCHILDREN     0x02000000L
#define WS_CLIPSIBLINGS     0x04000000L
#define WS_EX_APPWINDOW     0x00040000L
#define WS_EX_OVERLAPPEDWINDOW 0

// GetSystemMetrics constants
#define SM_CXSCREEN     0
#define SM_CYSCREEN     1

// Key codes
#define VK_LBUTTON  0x01
#define VK_RBUTTON  0x02
#define VK_CANCEL   0x03
#define VK_MBUTTON  0x04
#define VK_BACK     0x08
#define VK_TAB      0x09
#define VK_RETURN   0x0D
#define VK_SHIFT    0x10
#define VK_CONTROL  0x11
#define VK_MENU     0x12  // ALT
#define VK_PAUSE    0x13
#define VK_ESCAPE   0x1B
#define VK_SPACE    0x20
#define VK_PRIOR    0x21  // Page Up
#define VK_NEXT     0x22  // Page Down
#define VK_END      0x23
#define VK_HOME     0x24
#define VK_LEFT     0x25
#define VK_UP       0x26
#define VK_RIGHT    0x27
#define VK_DOWN     0x28
#define VK_DELETE   0x2E
#define VK_F1       0x70
#define VK_F2       0x71
#define VK_F3       0x72
#define VK_F4       0x73
#define VK_F5       0x74
#define VK_F6       0x75
#define VK_F7       0x76
#define VK_F8       0x77
#define VK_F9       0x78
#define VK_F10      0x79
#define VK_F11      0x7A
#define VK_F12      0x7B
#define VK_NUMPAD0  0x60
#define VK_NUMPAD9  0x69
#define VK_MULTIPLY 0x6A
#define VK_ADD      0x6B
#define VK_SUBTRACT 0x6D
#define VK_DECIMAL  0x6E
#define VK_DIVIDE   0x6F

// MSG structure
typedef struct tagMSG {
    HWND   hwnd;
    UINT   message;
    UINT   wParam;    // technically WPARAM but we use UINT
    LONG   lParam;    // technically LPARAM but we use LONG
    DWORD  time;
    POINT  pt;
} MSG, *PMSG, *LPMSG;
typedef UINT   WPARAM;
typedef LONG   LPARAM;
typedef LONG   LRESULT;

// WNDCLASS stubs
typedef LRESULT (*WNDPROC)(HWND, UINT, WPARAM, LPARAM);

typedef struct tagWNDCLASS {
    UINT      style;
    WNDPROC   lpfnWndProc;
    int       cbClsExtra;
    int       cbWndExtra;
    HINSTANCE hInstance;
    HICON     hIcon;
    HCURSOR   hCursor;
    HBRUSH    hbrBackground;
    LPCSTR    lpszMenuName;
    LPCSTR    lpszClassName;
} WNDCLASS, *PWNDCLASS, *LPWNDCLASS;
typedef WNDCLASS WNDCLASSA;

typedef struct tagWNDCLASSEX {
    UINT      cbSize;
    UINT      style;
    WNDPROC   lpfnWndProc;
    int       cbClsExtra;
    int       cbWndExtra;
    HINSTANCE hInstance;
    HICON     hIcon;
    HCURSOR   hCursor;
    HBRUSH    hbrBackground;
    LPCSTR    lpszMenuName;
    LPCSTR    lpszClassName;
    HICON     hIconSm;
} WNDCLASSEX, *PWNDCLASSEX;
typedef WNDCLASSEX WNDCLASSEXA;

// Window function stubs
inline BOOL RegisterClass(const WNDCLASS* wc) { return TRUE; }
inline BOOL RegisterClassA(const WNDCLASS* wc) { return TRUE; }
inline BOOL RegisterClassEx(const WNDCLASSEX* wc) { return TRUE; }
inline BOOL UnregisterClass(LPCSTR name, HINSTANCE inst) { return TRUE; }

inline HWND CreateWindowEx(DWORD exStyle, LPCSTR cls, LPCSTR title, DWORD style,
                           int x, int y, int w, int h, HWND parent, HMENU menu,
                           HINSTANCE inst, LPVOID param) {
    // Create the real SDL2 window + OpenGL context
    int ww = (w > 0 && w < 10000) ? w : 800;
    int hh = (h > 0 && h < 10000) ? h : 600;
    if (SDL2_Platform_Init(title ? title : "Renegade", ww, hh) != 0) {
        fprintf(stderr, "[winuser] SDL2_Platform_Init failed\n");
        return NULL;
    }
    return (HWND)SDL2_Platform_GetWindow();
}
inline HWND CreateWindow(LPCSTR cls, LPCSTR title, DWORD style,
                         int x, int y, int w, int h, HWND parent, HMENU menu,
                         HINSTANCE inst, LPVOID param) {
    return CreateWindowEx(0, cls, title, style, x, y, w, h, parent, menu, inst, param);
}
inline int  GetSystemMetrics(int nIndex) { return nIndex == SM_CXSCREEN ? 1920 : nIndex == SM_CYSCREEN ? 1080 : 0; }
inline BOOL ShowWindow(HWND wnd, int cmd) { return FALSE; }
inline BOOL UpdateWindow(HWND wnd) { return FALSE; }
inline BOOL DestroyWindow(HWND wnd) { return FALSE; }
inline BOOL MoveWindow(HWND wnd, int x, int y, int w, int h, BOOL repaint) { return FALSE; }
inline BOOL InvalidateRect(HWND wnd, const RECT* r, BOOL erase) { return FALSE; }
inline HWND GetForegroundWindow() { return NULL; }
inline HWND GetActiveWindow() { return NULL; }
inline HWND GetTopWindow(HWND hwnd) { return NULL; }
inline HWND SetFocus(HWND wnd) { return NULL; }
inline HWND GetDesktopWindow() { return NULL; }
inline BOOL GetWindowRect(HWND wnd, LPRECT r) { return FALSE; }
inline BOOL GetClientRect(HWND wnd, LPRECT r) { if (r) { r->left=r->top=r->right=r->bottom=0; } return FALSE; }
inline LONG GetWindowLong(HWND wnd, int idx) { return 0; }
inline LONG SetWindowLong(HWND wnd, int idx, LONG val) { return 0; }
inline BOOL SetWindowText(HWND wnd, LPCSTR s) { return FALSE; }
inline BOOL IsWindow(HWND wnd) { return FALSE; }
inline BOOL IsWindowVisible(HWND wnd) { return FALSE; }
inline BOOL SetWindowPos(HWND wnd, HWND after, int x, int y, int cx, int cy, UINT flags) { return FALSE; }
inline BOOL SetRect(RECT* r, int x1, int y1, int x2, int y2) { if(r){r->left=x1;r->top=y1;r->right=x2;r->bottom=y2;} return TRUE; }
inline BOOL SetRectEmpty(RECT* r) { if(r){r->left=r->top=r->right=r->bottom=0;} return TRUE; }
inline BOOL CopyRect(RECT* dst, const RECT* src) { if(dst&&src){*dst=*src;} return TRUE; }
inline BOOL IntersectRect(RECT* d, const RECT* a, const RECT* b) { return FALSE; }
inline BOOL EqualRect(const RECT* a, const RECT* b) { return a&&b&&a->left==b->left&&a->top==b->top&&a->right==b->right&&a->bottom==b->bottom; }

// Message loop stubs
inline BOOL GetMessage(LPMSG msg, HWND wnd, UINT min, UINT max) { return FALSE; }
inline BOOL PeekMessage(LPMSG msg, HWND wnd, UINT min, UINT max, UINT remove) { return FALSE; }
inline BOOL TranslateMessage(const MSG* msg) { return FALSE; }
inline LRESULT DispatchMessage(const MSG* msg) { return 0; }
inline void PostQuitMessage(int code) {}
inline BOOL PostMessage(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return FALSE; }
inline LRESULT SendMessage(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return 0; }
inline LRESULT SendMessageA(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return 0; }
inline LRESULT DefWindowProc(HWND wnd, UINT msg, WPARAM wp, LPARAM lp) { return 0; }

// MessageBox stubs
inline int MessageBox(HWND wnd, LPCSTR text, LPCSTR title, UINT type) {
    fprintf(stderr, "[MessageBox] %s: %s\n", title ? title : "", text ? text : "");
    if (type & MB_ABORTRETRYIGNORE) return IDIGNORE;
    return IDOK;
}
inline int MessageBoxA(HWND wnd, LPCSTR text, LPCSTR title, UINT type) {
    return MessageBox(wnd, text, title, type);
}

// Cursor/Icon stubs
inline HCURSOR LoadCursor(HINSTANCE inst, LPCSTR name) { return NULL; }
inline HCURSOR SetCursor(HCURSOR c) { return NULL; }
inline BOOL ShowCursor(BOOL show) { return FALSE; }

// Timer stubs
inline UINT SetTimer(HWND wnd, UINT id, UINT elapse, LPVOID proc) { return 0; }
inline BOOL KillTimer(HWND wnd, UINT id) { return FALSE; }

// Clipboard stubs
inline BOOL OpenClipboard(HWND wnd) { return FALSE; }
inline BOOL CloseClipboard() { return FALSE; }
inline BOOL EmptyClipboard() { return FALSE; }
inline HANDLE GetClipboardData(UINT fmt) { return NULL; }
inline HANDLE SetClipboardData(UINT fmt, HANDLE data) { return NULL; }

// Misc
#define GWL_USERDATA (-21)
#define GWL_STYLE    (-16)
#define GWL_EXSTYLE  (-20)
#define GWL_WNDPROC  (-4)
#define GWL_HWNDPARENT (-8)
#define GWL_ID       (-12)
#define CS_VREDRAW   0x0001
#define CS_HREDRAW   0x0002
#define CS_OWNDC     0x0020
#define SWP_NOSIZE       0x0001
#define SWP_NOMOVE       0x0002
#define SWP_NOZORDER     0x0004
#define SWP_NOACTIVATE   0x0010
#define SWP_SHOWWINDOW   0x0040
#define SWP_HIDEWINDOW   0x0080
#define SWP_NOCOPYBITS   0x0100
#define SWP_FRAMECHANGED 0x0020
#define HWND_TOP         ((HWND)0)
#define HWND_TOPMOST     ((HWND)-1)
#define PM_REMOVE        0x0001
#define PM_NOREMOVE      0x0000

HDC GetDC(HWND wnd);       // implemented in wingdi_coretext.cpp
int ReleaseDC(HWND wnd, HDC dc);
inline HDC GetWindowDC(HWND wnd) { (void)wnd; return NULL; }
inline BOOL ScreenToClient(HWND wnd, LPPOINT pt) { return FALSE; }
inline BOOL ClientToScreen(HWND wnd, LPPOINT pt) { return FALSE; }
inline BOOL ClipCursor(const RECT* r) { return FALSE; }
inline BOOL GetCursorPos(LPPOINT pt) { if (pt) { pt->x = pt->y = 0; } return FALSE; }
inline BOOL SetCursorPos(int x, int y) { return FALSE; }

// Input language change messages
#define WM_INPUTLANGCHANGEREQUEST   0x0050
#define WM_INPUTLANGCHANGE          0x0051
#define WM_DEADCHAR                 0x0103
#define WM_SYSDEADCHAR              0x0107
#define WM_UNICHAR                  0x0109

// Language/locale macros and constants
typedef DWORD LCID;
typedef DWORD LANGID;
#define MAKELANGID(p, s)        ((((WORD)(s)) << 10) | (WORD)(p))
#define PRIMARYLANGID(lgid)     ((WORD)(lgid) & 0x3ff)
#define SUBLANGID(lgid)         ((WORD)(lgid) >> 10)
#define MAKELCID(lgid, srtid)   ((DWORD)((((DWORD)((WORD)(srtid))) << 16) | ((DWORD)((WORD)(lgid)))))
#define LANGIDFROMLCID(lcid)    ((WORD)(lcid))

#define LANG_NEUTRAL                0x00
#define LANG_ENGLISH                0x09
#define LANG_JAPANESE               0x11
#define LANG_CHINESE                0x04
#define LANG_KOREAN                 0x12
#define SUBLANG_DEFAULT             0x01
#define SUBLANG_NEUTRAL             0x00
#define SORT_DEFAULT                0x0
#define LOCALE_USER_DEFAULT         ((LCID)0x0400)
#define LOCALE_SYSTEM_DEFAULT       ((LCID)0x0800)
#define LOCALE_INVARIANT            ((LCID)0x007f)
#define LOCALE_IDEFAULTANSICODEPAGE 0x1004
#define LOCALE_IDEFAULTCODEPAGE     0x000b
#define LOCALE_SENGLANGUAGE         0x1001

// CompareString constants
#define NORM_IGNORECASE         0x00000001
#define NORM_IGNORENONSPACE     0x00000002
#define NORM_IGNORESYMBOLS      0x00000004
#define NORM_IGNOREKANATYPE     0x00010000
#define NORM_IGNOREWIDTH        0x00020000
#define CSTR_LESS_THAN          1
#define CSTR_EQUAL              2
#define CSTR_GREATER_THAN       3

// Keyboard layout functions
inline HKL GetKeyboardLayout(DWORD idThread) { return NULL; }
inline UINT GetKeyboardLayoutList(UINT nBuff, HKL* lpList) { return 0; }
inline BOOL ActivateKeyboardLayout(HKL hkl, UINT Flags) { return FALSE; }

// GetLocaleInfo stub
inline int GetLocaleInfoA(LCID Locale, DWORD LCType, LPSTR lpLCData, int cchData) {
    if (lpLCData && cchData > 0) lpLCData[0] = 0;
    return 0;
}
inline int GetLocaleInfoW(LCID Locale, DWORD LCType, LPWSTR lpLCData, int cchData) {
    if (lpLCData && cchData > 0) lpLCData[0] = 0;
    return 0;
}
#define GetLocaleInfo GetLocaleInfoA

// CompareString stubs
inline int CompareStringW(LCID Locale, DWORD dwCmpFlags, LPCWSTR lpString1, int cchCount1, LPCWSTR lpString2, int cchCount2) {
    if (!lpString1 || !lpString2) return CSTR_EQUAL;
    int r = (cchCount1 == -1 && cchCount2 == -1)
        ? ((dwCmpFlags & NORM_IGNORECASE) ? wcscasecmp(lpString1, lpString2) : wcscmp(lpString1, lpString2))
        : wcsncmp(lpString1, lpString2, (cchCount1 < cchCount2 ? cchCount1 : cchCount2));
    return r < 0 ? CSTR_LESS_THAN : r > 0 ? CSTR_GREATER_THAN : CSTR_EQUAL;
}
inline int CompareStringA(LCID Locale, DWORD dwCmpFlags, LPCSTR s1, int c1, LPCSTR s2, int c2) {
    if (!s1 || !s2) return CSTR_EQUAL;
    int r = (dwCmpFlags & NORM_IGNORECASE) ? strcasecmp(s1, s2) : strcmp(s1, s2);
    return r < 0 ? CSTR_LESS_THAN : r > 0 ? CSTR_GREATER_THAN : CSTR_EQUAL;
}

// Mouse wheel
#define WHEEL_DELTA             120
#define WM_MOUSEWHEEL_ADD       WM_MOUSEWHEEL
#define MOUSEEVENTF_WHEEL       0x0800

// SystemParametersInfo constants
#define SPI_GETSCREENSAVERTIME  14
#define SPI_GETBEEP             1
#define SPI_GETBORDER           5
#define SPI_GETKEYBOARDSPEED    10
#define SPI_GETKEYBOARDDELAY    22
#define SPI_GETICONTITLELOGFONT 31
#define SPI_GETWORKAREA         48
#define SPI_GETNONCLIENTMETRICS 41
#define SPI_GETICONMETRICS      45
#define SPI_GETWHEELSCROLLLINES 104

inline BOOL SystemParametersInfoA(UINT uiAction, UINT uiParam, PVOID pvParam, UINT fWinIni) {
    if (uiAction == SPI_GETWHEELSCROLLLINES && pvParam) { *(UINT*)pvParam = 3; return TRUE; }
    return FALSE;
}
#define SystemParametersInfo SystemParametersInfoA

// wsprintf is in winbase.h
inline int wvsprintf(LPSTR buf, LPCSTR fmt, va_list va) { return vsprintf(buf, fmt, va); }

// LoadString stub
inline int LoadString(HINSTANCE inst, UINT id, LPSTR buf, int size) {
    if (buf && size > 0) buf[0] = 0;
    return 0;
}

// Keyboard state stubs
typedef BYTE* PBYTE;
inline SHORT GetKeyState(int nVirtKey) { return 0; }
inline SHORT GetAsyncKeyState(int vKey) { return 0; }
inline BOOL  GetKeyboardState(PBYTE lpKeyState) { return FALSE; }
inline UINT  MapVirtualKey(UINT uCode, UINT uMapType) { return 0; }
inline UINT  MapVirtualKeyA(UINT uCode, UINT uMapType) { return 0; }

// ToAscii stub
inline int ToAscii(UINT vk, UINT sc, const BYTE* ks, LPWORD buf, UINT flags) {
    if (buf) *buf = 0;
    if (vk >= '0' && vk <= '9') { if (buf) *buf = (WORD)vk; return 1; }
    if (vk >= 'A' && vk <= 'Z') { if (buf) *buf = (WORD)(vk + 32); return 1; }
    return 0;
}

// Static control styles (SS_*)
#define SS_LEFT             0x00000000L
#define SS_CENTER           0x00000001L
#define SS_RIGHT            0x00000002L
#define SS_ICON             0x00000003L
#define SS_BLACKRECT        0x00000004L
#define SS_GRAYRECT         0x00000005L
#define SS_WHITERECT        0x00000006L
#define SS_BLACKFRAME       0x00000007L
#define SS_GRAYFRAME        0x00000008L
#define SS_WHITEFRAME       0x00000009L
#define SS_USERITEM         0x0000000AL
#define SS_SIMPLE           0x0000000BL
#define SS_LEFTNOWORDWRAP   0x0000000CL
#define SS_OWNERDRAW        0x0000000DL
#define SS_BITMAP           0x0000000EL
#define SS_ENHMETAFILE      0x0000000FL
#define SS_ETCHEDHORZ       0x00000010L
#define SS_ETCHEDVERT       0x00000011L
#define SS_ETCHEDFRAME      0x00000012L
#define SS_TYPEMASK         0x0000001FL
#define SS_NOTIFY           0x00000100L
#define SS_CENTERIMAGE      0x00000200L
#define SS_RIGHTJUST        0x00000400L
#define SS_REALSIZEIMAGE    0x00000800L
#define SS_SUNKEN           0x00001000L
#define SS_NOPREFIX         0x00000080L
#define SS_ENDELLIPSIS      0x00004000L
#define SS_PATHELLIPSIS     0x00008000L
#define SS_WORDELLIPSIS     0x0000C000L
#define SS_ELLIPSISMASK     0x0000C000L

// Dialog styles
#define DS_SETFONT          0x0040L
#define DS_MODALFRAME       0x0080L
#define DS_NOIDLEMSG        0x0100L
#define DS_SETFOREGROUND    0x0200L
#define DS_3DLOOK           0x0004L
#define DS_FIXEDSYS         0x0008L
#define DS_NOFAILCREATE     0x0010L
#define DS_CONTROL          0x0400L
#define DS_CENTER           0x0800L
#define DS_CENTERMOUSE      0x1000L
#define DS_CONTEXTHELP      0x2000L
#define DS_SHELLFONT        (DS_SETFONT | DS_FIXEDSYS)
#define DS_SYSMODAL         0x0002L
#define DS_ABSALIGN         0x0001L
#define DS_LOCALEDIT        0x0020L

// Dialog template structs
#pragma pack(push, 2)
typedef struct tagDLGTEMPLATE {
    DWORD style;
    DWORD dwExtendedStyle;
    WORD  cdit;
    short x;
    short y;
    short cx;
    short cy;
} DLGTEMPLATE, *LPDLGTEMPLATE, *LPCDLGTEMPLATE;

typedef struct tagDLGITEMTEMPLATE {
    DWORD style;
    DWORD dwExtendedStyle;
    short x;
    short y;
    short cx;
    short cy;
    WORD  id;
} DLGITEMTEMPLATE, *LPDLGITEMTEMPLATE;
#pragma pack(pop)

// Resource macros
typedef LPTSTR LPCTSTR_RESOURCE;
#define MAKEINTRESOURCE(i)  ((LPTSTR)((ULONG_PTR)((WORD)(i))))
#define MAKEINTRESOURCEA(i) ((LPSTR)((ULONG_PTR)((WORD)(i))))
#define MAKEINTRESOURCEW(i) ((LPWSTR)((ULONG_PTR)((WORD)(i))))
#define IS_INTRESOURCE(r)   ((((ULONG_PTR)(r)) >> 16) == 0)

// Resource type constants
#define RT_CURSOR           MAKEINTRESOURCE(1)
#define RT_BITMAP           MAKEINTRESOURCE(2)
#define RT_ICON             MAKEINTRESOURCE(3)
#define RT_MENU             MAKEINTRESOURCE(4)
#define RT_DIALOG           MAKEINTRESOURCE(5)
#define RT_STRING           MAKEINTRESOURCE(6)
#define RT_FONTDIR          MAKEINTRESOURCE(7)
#define RT_FONT             MAKEINTRESOURCE(8)
#define RT_ACCELERATOR      MAKEINTRESOURCE(9)
#define RT_RCDATA           MAKEINTRESOURCE(10)
#define RT_MESSAGETABLE     MAKEINTRESOURCE(11)

// Common control class names
#define PROGRESS_CLASSA     "msctls_progress32"
#define PROGRESS_CLASSW     L"msctls_progress32"
#define PROGRESS_CLASS      PROGRESS_CLASSA

// Edit control notifications
#define EN_SETFOCUS     0x0100
#define EN_KILLFOCUS    0x0200
#define EN_CHANGE       0x0300
#define EN_UPDATE       0x0400
#define EN_ERRSPACE     0x0500
#define EN_MAXTEXT      0x0501
#define EN_HSCROLL      0x0601
#define EN_VSCROLL      0x0602

// Button styles
#define BS_PUSHBUTTON       0x00000000L
#define BS_DEFPUSHBUTTON    0x00000001L
#define BS_CHECKBOX         0x00000002L
#define BS_AUTOCHECKBOX     0x00000003L
#define BS_RADIOBUTTON      0x00000004L
#define BS_3STATE           0x00000005L
#define BS_AUTO3STATE       0x00000006L
#define BS_GROUPBOX         0x00000007L
#define BS_USERBUTTON       0x00000008L
#define BS_AUTORADIOBUTTON  0x00000009L
#define BS_OWNERDRAW        0x0000000BL
#define BS_LEFTTEXT         0x00000020L
#define BS_NOTIFY           0x00004000L
#define BS_FLAT             0x00008000L
#define BS_LEFT             0x00000100L
#define BS_RIGHT            0x00000200L
#define BS_CENTER           0x00000300L
#define BS_TOP              0x00000400L
#define BS_BOTTOM           0x00000800L
#define BS_VCENTER          0x00000C00L
#define BS_BITMAP           0x00000080L
#define BS_ICON             0x00000040L
#define BS_TEXT             0x00000000L
#define BS_MULTILINE        0x00002000L

// Edit styles
#define ES_LEFT         0x0000L
#define ES_CENTER       0x0001L
#define ES_RIGHT        0x0002L
#define ES_MULTILINE    0x0004L
#define ES_UPPERCASE    0x0008L
#define ES_LOWERCASE    0x0010L
#define ES_PASSWORD     0x0020L
#define ES_AUTOVSCROLL  0x0040L
#define ES_AUTOHSCROLL  0x0080L
#define ES_NOHIDESEL    0x0100L
#define ES_OEMCONVERT   0x0400L
#define ES_READONLY     0x0800L
#define ES_WANTRETURN   0x1000L
#define ES_NUMBER       0x2000L

// Listbox styles
#define LBS_NOTIFY          0x0001L
#define LBS_SORT            0x0002L
#define LBS_NOREDRAW        0x0004L
#define LBS_MULTIPLESEL     0x0008L
#define LBS_OWNERDRAWFIXED  0x0010L
#define LBS_OWNERDRAWVARIABLE 0x0020L
#define LBS_HASSTRINGS      0x0040L
#define LBS_USETABSTOPS     0x0080L
#define LBS_NOINTEGRALHEIGHT 0x0100L
#define LBS_MULTICOLUMN     0x0200L
#define LBS_WANTKEYBOARDINPUT 0x0400L
#define LBS_EXTENDEDSEL     0x0800L
#define LBS_DISABLENOSCROLL 0x1000L
#define LBS_NODATA          0x2000L
#define LBS_NOSEL           0x4000L
#define LBS_STANDARD        (LBS_NOTIFY | LBS_SORT | WS_VSCROLL | WS_BORDER)

// Combobox styles
#define CBS_SIMPLE          0x0001L
#define CBS_DROPDOWN        0x0002L
#define CBS_DROPDOWNLIST    0x0003L
#define CBS_OWNERDRAWFIXED  0x0010L
#define CBS_OWNERDRAWVARIABLE 0x0020L
#define CBS_AUTOHSCROLL     0x0040L
#define CBS_OEMCONVERT      0x0080L
#define CBS_SORT            0x0100L
#define CBS_HASSTRINGS      0x0200L
#define CBS_NOINTEGRALHEIGHT 0x0400L
#define CBS_DISABLENOSCROLL 0x0800L
#define CBS_UPPERCASE       0x2000L
#define CBS_LOWERCASE       0x4000L

// Scrollbar styles
#define SBS_HORZ            0x0000L
#define SBS_VERT            0x0001L
#define SBS_TOPALIGN        0x0002L
#define SBS_LEFTALIGN       0x0002L
#define SBS_BOTTOMALIGN     0x0004L
#define SBS_RIGHTALIGN      0x0004L
#define SBS_SIZEBOXTOPLEFTALIGN     0x0002L
#define SBS_SIZEBOXBOTTOMRIGHTALIGN 0x0004L
#define SBS_SIZEBOX         0x0008L
#define SBS_SIZEGRIP        0x0010L

// Dialog/child window messages
#define DM_GETDEFID    (WM_USER+0)
#define DM_SETDEFID    (WM_USER+1)
#define DM_REPOSITION  (WM_USER+2)

// Dialog stubs
typedef INT_PTR (*DLGPROC)(HWND, UINT, WPARAM, LPARAM);
inline HWND CreateDialog(HINSTANCE inst, LPCSTR tmpl, HWND parent, DLGPROC dlgProc) { return NULL; }
inline HWND CreateDialogParam(HINSTANCE inst, LPCSTR tmpl, HWND parent, DLGPROC dlgProc, LPARAM init) { return NULL; }
inline INT_PTR DialogBox(HINSTANCE inst, LPCSTR tmpl, HWND parent, DLGPROC dlgProc) { return -1; }
inline INT_PTR DialogBoxParam(HINSTANCE inst, LPCSTR tmpl, HWND parent, DLGPROC dlgProc, LPARAM init) { return -1; }
inline BOOL EndDialog(HWND dlg, INT_PTR result) { return FALSE; }
inline HWND GetDlgItem(HWND dlg, int nIDDlgItem) { return NULL; }
inline BOOL EnableWindow(HWND wnd, BOOL enable) { return FALSE; }
inline BOOL SetDlgItemText(HWND dlg, int nIDDlgItem, LPCSTR text) { return FALSE; }
inline UINT GetDlgItemText(HWND dlg, int nIDDlgItem, LPSTR text, int max) { if(text&&max>0)text[0]=0; return 0; }
inline INT  GetDlgItemInt(HWND dlg, int nIDDlgItem, BOOL* ok, BOOL sign) { return 0; }
inline BOOL SetDlgItemInt(HWND dlg, int nIDDlgItem, UINT val, BOOL sign) { return FALSE; }
inline BOOL SendDlgItemMessage(HWND dlg, int id, UINT msg, WPARAM wp, LPARAM lp) { return FALSE; }
inline HWND GetParent(HWND wnd) { return NULL; }
inline HWND GetWindow(HWND wnd, UINT cmd) { return NULL; }
inline LONG_PTR GetWindowLongPtr(HWND wnd, int idx) { return 0; }
inline LONG_PTR SetWindowLongPtr(HWND wnd, int idx, LONG_PTR val) { return 0; }
inline BOOL CheckDlgButton(HWND dlg, int id, UINT check) { return FALSE; }
inline UINT IsDlgButtonChecked(HWND dlg, int id) { return 0; }
inline BOOL CheckRadioButton(HWND dlg, int first, int last, int check) { return FALSE; }
inline BOOL SetWindowTextA(HWND wnd, LPCSTR s) { return FALSE; }
inline BOOL SetWindowTextW(HWND wnd, LPCWSTR s) { return FALSE; }

// FindWindow stubs
inline HWND FindWindow(LPCSTR cls, LPCSTR wnd) { return NULL; }
inline HWND FindWindowA(LPCSTR cls, LPCSTR wnd) { return NULL; }

// More window constants
#define DLGC_WANTALLKEYS    0x0004
#define DLGC_WANTCHARS      0x0080
#define DLGC_HASSETSEL      0x0008
#define DLGC_DEFPUSHBUTTON  0x0010
#define DLGC_UNDEFPUSHBUTTON 0x0020
#define DLGC_RADIOBUTTON    0x0040
#define DLGC_WANTTAB        0x0002
#define DLGC_BUTTON         0x2000
#define DLGC_STATIC         0x0100
#define DLGC_WANTARROWS     0x0001
#define DLGC_WANTMESSAGE    (DLGC_WANTALLKEYS | DLGC_WANTCHARS | DLGC_WANTTAB | DLGC_WANTARROWS)

// Accelerator / dialog stubs
inline BOOL TranslateAccelerator(HWND hwnd, HACCEL hacc, LPMSG msg) { return FALSE; }
inline BOOL IsDialogMessage(HWND hwnd, LPMSG msg) { return FALSE; }

// Virtual key codes
#define VK_LBUTTON   0x01
#define VK_RBUTTON   0x02
#define VK_MBUTTON   0x04
#define VK_BACK      0x08
#define VK_TAB       0x09
#define VK_RETURN    0x0D
#define VK_SHIFT     0x10
#define VK_CONTROL   0x11
#define VK_MENU      0x12
#define VK_PAUSE     0x13
#define VK_CAPITAL   0x14
#define VK_ESCAPE    0x1B
#define VK_SPACE     0x20
#define VK_PRIOR     0x21
#define VK_NEXT      0x22
#define VK_END       0x23
#define VK_HOME      0x24
#define VK_LEFT      0x25
#define VK_UP        0x26
#define VK_RIGHT     0x27
#define VK_DOWN      0x28
#define VK_INSERT    0x2D
#define VK_DELETE    0x2E
#define VK_F1        0x70
#define VK_F2        0x71
#define VK_F3        0x72
#define VK_F4        0x73
#define VK_F5        0x74
#define VK_F6        0x75
#define VK_F7        0x76
#define VK_F8        0x77
#define VK_F9        0x78
#define VK_F10       0x79
#define VK_F11       0x7A
#define VK_F12       0x7B
#define VK_NUMPAD0   0x60
#define VK_NUMPAD1   0x61
#define VK_NUMPAD2   0x62
#define VK_NUMPAD3   0x63
#define VK_NUMPAD4   0x64
#define VK_NUMPAD5   0x65
#define VK_NUMPAD6   0x66
#define VK_NUMPAD7   0x67
#define VK_NUMPAD8   0x68
#define VK_NUMPAD9   0x69
#define VK_MULTIPLY  0x6A
#define VK_ADD       0x6B
#define VK_SUBTRACT  0x6D
#define VK_DECIMAL   0x6E
#define VK_DIVIDE    0x6F
#define VK_LWIN      0x5B
#define VK_RWIN      0x5C
#define VK_LSHIFT    0xA0
#define VK_RSHIFT    0xA1
#define VK_LCONTROL  0xA2
#define VK_RCONTROL  0xA3
#define VK_LMENU     0xA4
#define VK_RMENU     0xA5
#define MAPVK_VK_TO_VSC 0
#define MAPVK_VSC_TO_VK 1
#define MAPVK_VK_TO_CHAR 2

// ValidateRect - marks window area as valid (no repaint needed)
inline BOOL ValidateRect(HWND hwnd, const RECT* lpRect) { return TRUE; }

// ReleaseCapture - releases mouse capture
inline BOOL ReleaseCapture(void) { return TRUE; }

// SC_SCREENSAVE and WM_COMMAND (missing from earlier additions)
#ifndef SC_SCREENSAVE
#define SC_SCREENSAVE   0xF140
#endif
#define WM_COMMAND      0x0111

// Window class styles
#define CS_DBLCLKS      0x0008
#define CS_NOCLOSE      0x0200
#define CS_SAVEBITS     0x0800
#define CS_BYTEALIGNWINDOW 0x2000
#define CS_BYTEALIGNCLIENT 0x1000
#define CS_GLOBALCLASS  0x4000

// LoadIcon/LoadCursor standard IDs
#define IDI_APPLICATION ((LPCSTR)32512)
#define IDI_ASTERISK    ((LPCSTR)32516)
#define IDI_ERROR       ((LPCSTR)32513)
#define IDI_EXCLAMATION ((LPCSTR)32515)
#define IDI_HAND        IDI_ERROR
#define IDI_INFORMATION IDI_ASTERISK
#define IDI_QUESTION    ((LPCSTR)32514)
#define IDI_WARNING     IDI_EXCLAMATION
#define IDI_WINLOGO     ((LPCSTR)32517)

#define IDC_ARROW       ((LPCSTR)32512)
#define IDC_IBEAM       ((LPCSTR)32513)
#define IDC_WAIT        ((LPCSTR)32514)
#define IDC_CROSS       ((LPCSTR)32515)
#define IDC_UPARROW     ((LPCSTR)32516)
#define IDC_SIZENWSE    ((LPCSTR)32642)
#define IDC_SIZENESW    ((LPCSTR)32643)
#define IDC_SIZEWE      ((LPCSTR)32644)
#define IDC_SIZENS      ((LPCSTR)32645)
#define IDC_SIZEALL     ((LPCSTR)32646)
#define IDC_NO          ((LPCSTR)32648)
#define IDC_HAND        ((LPCSTR)32649)
#define IDC_APPSTARTING ((LPCSTR)32650)
#define IDC_HELP        ((LPCSTR)32651)

// LoadIcon/LoadCursor stubs
inline HICON   LoadIcon(HINSTANCE inst, LPCSTR name) { return NULL; }
inline HICON   LoadIconA(HINSTANCE inst, LPCSTR name) { return NULL; }

// GetStockObject - returns a stock brush/pen/font handle
#define BLACK_BRUSH     4
#define WHITE_BRUSH     0
#define GRAY_BRUSH      2
#define DKGRAY_BRUSH    3
#define LTGRAY_BRUSH    1
#define NULL_BRUSH      5
#define HOLLOW_BRUSH    NULL_BRUSH
#define BLACK_PEN       7
#define WHITE_PEN       8
#define NULL_PEN        9
#define OEM_FIXED_FONT  10
#define ANSI_FIXED_FONT 11
#define ANSI_VAR_FONT   12
#define SYSTEM_FONT     13
#define DEVICE_DEFAULT_FONT 14
#define DEFAULT_PALETTE 15
#define SYSTEM_FIXED_FONT 16
#define DEFAULT_GUI_FONT 17
#define DC_BRUSH        18
#define DC_PEN          19

inline HANDLE GetStockObject(int fnObject) { return NULL; }

// Window/Process functions
inline DWORD GetWindowThreadProcessId(HWND hWnd, LPDWORD lpdwProcessId) {
    (void)hWnd; if (lpdwProcessId) *lpdwProcessId = 0; return 0;
}

// Accelerator table stubs
typedef void* HACCEL;
#ifndef MAKEINTRESOURCE
#define MAKEINTRESOURCE(i) ((LPSTR)((uintptr_t)((WORD)(i))))
#endif
inline HACCEL LoadAccelerators(HINSTANCE hInst, LPCSTR lpTableName) { (void)hInst; (void)lpTableName; return nullptr; }
inline BOOL   TranslateAccelerator(HWND hwnd, HACCEL hAccTable, void* lpMsg) { (void)hwnd; (void)hAccTable; (void)lpMsg; return FALSE; }

#endif // WINUSER_H_COMPAT
